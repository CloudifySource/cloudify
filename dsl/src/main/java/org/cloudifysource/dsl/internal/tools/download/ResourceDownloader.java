/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl.internal.tools.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * This class enables resource download and resource verification using the VerifyChecksum class
 * {@link org.cloudifysource.dsl.internal.tools.download.ChecksumVerifier} Supported checksum algorithms include md5,
 * sha1, sha256, sha384 and sha512, See enum
 * {@link org.cloudifysource.dsl.internal.tools.download.ChecksumVerifier.ChecksumAlgorithm} The default hash message
 * format used to extract the hash message from the hash file is of the form {0} *{1} i.e 'hash string *some string'.
 * The file hash output will be compared against the {0} index.
 *
 * @author adaml
 * @since 2.6.0
 *
 */
public class ResourceDownloader {

	private static final int TEMPORARY_FILE_CREATION_RETY_LIMIT = 100;

	// big buffer
	private static final int BUFFER_SIZE = 100 * 1024;

	private static final long DEFAULT_DOWNLOAD_TIMEOUT_MILLIS = 600000;

	private static final int DEFAULT_NUMBER_OF_RETRIES = 3;

	private static final Logger logger = Logger
			.getLogger(ResourceDownloadFacadeImpl.class.getName());

	private URL resourceUrl;

	private URL hashUrl;

	// destination where the resource file will be saved
	private File resourceDest;

	private long timeoutInMillis = DEFAULT_DOWNLOAD_TIMEOUT_MILLIS;

	private String userName;

	private String password;

	private int numberOfRetries = DEFAULT_NUMBER_OF_RETRIES;

	private boolean skipExisting;

	// the hash message format.
	private MessageFormat format = new MessageFormat("{0} *{1}");

	public void setUrl(final URL urlString) {
		this.resourceUrl = urlString;
	}

	public URL getUrl() {
		return this.resourceUrl;
	}

	public void setResourceDest(final File resourceDest) {
		this.resourceDest = resourceDest;

	}

	public File getResourceDest() {
		return this.resourceDest;
	}

	public void setHashUrl(final URL hashUrl) {
		this.hashUrl = hashUrl;

	}

	public URL getHashUrl() {
		return this.hashUrl;
	}

	public void setTimeoutInMillis(final long timeout) {
		this.timeoutInMillis = timeout;
	}

	public long getTimeoutInMillis() {
		return this.timeoutInMillis;
	}

	public void setUserName(final String userName) {
		this.userName = userName;

	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public void setNumberOfRetries(final int numberOfRetries) {
		this.numberOfRetries = numberOfRetries;

	}

	public int getNumberOfRetries() {
		return this.numberOfRetries;
	}

	public void setSkipExisting(final boolean skipExisting) {
		this.skipExisting = skipExisting;
	}

	public boolean getSkipExisting() {
		return this.skipExisting;
	}

	public void setFormat(final MessageFormat format) {
		this.format = format;
	}

	public MessageFormat getFormat() {
		return this.format;
	}

	/**
	 * Use this method to verify resource-file's integrity using a checksum file containing the file hash. The checksum
	 * file extension determines the hashing algorithm used.
	 *
	 * @param checksumFile
	 *            A file containing the hash code.
	 * @throws ResourceDownloadException
	 *             if hashing algorithm does not exist, or other exception occurs.
	 */
	public void verifyResourceChecksum(final File checksumFile)
			throws ResourceDownloadException {
		final ChecksumVerifier cv = new ChecksumVerifier();
		cv.setFile(this.resourceDest);
		cv.setHashFile(checksumFile);
		cv.setFormat(this.format);
		try {
			boolean result = cv.evaluate();
			if (!result) {
				throw new ResourceDownloadException("Failed verifing checksum.");
			}
		} catch (ChecksumVerifierException e) {
			logger.warning("Failed verifing resource checksum. Reason: " + e.getMessage());
			throw new ResourceDownloadException("Failed validating checksum.", e);
		}
	}

	private String getResourceName(final URL url) {
		final String urlAsString = url.toString();
		final int slashIndex = urlAsString.lastIndexOf('/');
		final String filename = urlAsString.substring(slashIndex + 1);
		return filename;
	}

	/**
	 *
	 * @throws ResourceDownloadException
	 *             if download fails.
	 * @throws TimeoutException
	 *             if timeout exceeded.
	 */
	public void download()
			throws ResourceDownloadException, TimeoutException {

		if (this.resourceDest.exists() && this.skipExisting) {
			logger.log(Level.INFO, "File already exists. "
					+ this.resourceDest.getAbsolutePath() + " Skipping download.");
			return;
		}

		createDestinationDirectories();

		for (int attempt = 1; attempt <= this.numberOfRetries; attempt++) {
			try {
				getResource(this.resourceUrl, this.resourceDest);
				if (this.hashUrl != null) {
					// create checksum file destination.
					// The checksum file extension determines the hashing algorithm used.
					String resourceName = getResourceName(this.hashUrl);
					File checksumFile = new File(this.resourceDest.getParent(), resourceName);

					getResource(this.hashUrl, checksumFile);
					logger.log(Level.FINE, "Verifying resource checksum using checksum file "
							+ checksumFile.getAbsolutePath());
					verifyResourceChecksum(checksumFile);
				}
				return;
			} catch (ResourceDownloadException e) {
				logger.log(Level.WARNING, "Failed downloading resource on attempt " + attempt
						+ ". Reason was " + e.getMessage());
				if (attempt == numberOfRetries) {
					throw e;
				}
			}
		}
	}

	private void createDestinationDirectories() throws ResourceDownloadException {
		File destinationParent = this.resourceDest.getParentFile();
		if (!destinationParent.exists()) {
			if (!destinationParent.mkdirs()) {
				throw new ResourceDownloadException("Failed to create the required directories for destination: "
						+ this.resourceDest);
			}
		}
	}

	private void getResource(final URL downloadURL, final File destination)
			throws ResourceDownloadException, TimeoutException {

		final long end = System.currentTimeMillis() + this.timeoutInMillis;
		final InputStream is = openConnectionInputStream(downloadURL);
		if (is == null) {
			logger.log(Level.WARNING, "connection input stream failed to initialize");
			throw new ResourceDownloadException(
					"Failed getting " + this.resourceUrl + " to " + destination.getAbsolutePath());
		}

		final File temporaryDestination = createTemporaryDestinationFile(destination);

		final OutputStream os = getFileOutputString(temporaryDestination);
		boolean finished = false;
		try {
			final byte[] buffer = new byte[BUFFER_SIZE];
			int length;
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Downloading " + downloadURL.toString() + " to " + this.resourceDest);
			}
			while ((length = is.read(buffer)) >= 0) {
				os.write(buffer, 0, length);
				if (end < System.currentTimeMillis()) {
					throw new TimeoutException();
				}
			}
			finished = true;

		} catch (IOException e) {
			logger.warning("Failed downloading resource from " + downloadURL.toString()
					+ ". Reason was: " + e.getMessage());
			throw new ResourceDownloadException("Failed downloading resource. Reason was: "
					+ e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(os);
			IOUtils.closeQuietly(is);

			if (!finished) {
				logger.log(Level.WARNING, "Download did not complete successfully. deleting file.");
				FileUtils.deleteQuietly(temporaryDestination);
				FileUtils.deleteQuietly(destination);

			}
		}
		if (finished) {
			try {

				FileUtils.copyFile(temporaryDestination, destination);

			} catch (IOException e) {
				if (destination.exists()) {
					logger.warning("Failed to write downloaded file to destination: "
							+ destination
							+ ". Destination file already exists. "
							+ "This probably indicates a concurrent download of the same file.");
				} else {
					throw new ResourceDownloadException("Failed to copy downloaded file to target location: "
							+ e.getMessage(), e);
				}

			} finally {
				FileUtils.deleteQuietly(temporaryDestination);
			}
		}

	}

	private File createTemporaryDestinationFile(final File destination) throws ResourceDownloadException {
		Exception lastException = null;
		String temporaryFileName = null;
		for (int i = 0; i < TEMPORARY_FILE_CREATION_RETY_LIMIT; ++i) {
			temporaryFileName = destination.getName() + ".part." + System.nanoTime();
			if (i > 0) {
				temporaryFileName += "_" + i;
			}

			final File file = new File(destination.getParentFile(), temporaryFileName);
			try {
				if (file.createNewFile()) {
					return file;
				}
			} catch (IOException e) {
				// probably concurrent access
				lastException = e;
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, "Failed to create new file: " + file + ". Error was: " + e.getMessage(), e);
				}
			}
		}

		if (lastException == null) {
			throw new ResourceDownloadException("Failed to create temporary file : " + temporaryFileName);
		} else {
			throw new ResourceDownloadException("Failed to create temporary file : " + temporaryFileName
					+ ", last error was: " + lastException.getMessage(), lastException);
		}

	}

	private OutputStream getFileOutputString(final File destination)
			throws ResourceDownloadException {
		destination.getParentFile().mkdirs();
		try {
			// if (!destination.createNewFile()) {
			// throw new IllegalStateException("Failed to create a new file called " + destination.getAbsolutePath()
			// + ": file already exists");
			// }
			return new FileOutputStream(destination);
		} catch (final IOException e) {
			throw new ResourceDownloadException("Failed opening stream to dest file "
					+ destination.getAbsolutePath(), e);
		}
	}

	private InputStream openConnectionInputStream(final URL url) throws ResourceDownloadException {

		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpHead httpMethod = new HttpHead(url.toString());

		HttpResponse response;
		try {
			logger.fine("validating url");
			response = httpClient.execute(httpMethod);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				logger.warning("Failed to validate Resource URL: " + url.toString());
				throw new ResourceDownloadException("Invalid resource URL: " + url.toString());
			}
			final URLConnection connection = url.openConnection();
			if (this.userName != null || this.password != null) {
				logger.fine("Setting connection credentials");
				String up = this.userName + ":" + this.password;
				String encoding = new String(
						Base64.encodeBase64(up.getBytes()));
				connection.setRequestProperty("Authorization", "Basic " + encoding);
			}
			return connection.getInputStream();
		} catch (ClientProtocolException e) {
			throw new ResourceDownloadException("Invalid connection protocol " + url.toString(), e);
		} catch (IOException e) {
			throw new ResourceDownloadException("Invalid resource URL: " + url.toString(), e);
		}
	}
}
