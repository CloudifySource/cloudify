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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * This class implements the {@link org.cloudifysource.dsl.internal.tools.download.ResourceDownloader}
 * The class enables download of resources and resource validation using 
 * {@link org.cloudifysource.dsl.internal.tools.download.ChecksumVerifier}
 * 
 * @author adaml
 * @since 2.6.0
 *
 */
public class ResourceDownloader {
	
	//big buffer
	private final int BUFFER_SIZE = 100 * 1024;

	private final int DEFAULT_NUMBER_OF_RETRIES = 3;
	
	private static final Logger logger = Logger
			.getLogger(ResourceDownloadFacadeImpl.class.getName());

	private URL resourceUrl;

	private URL hashUrl;
	
	//destination where the resource file will be saved
	private File resourceDest;
	
	private long timeoutInMillis;

	private String userName;

	private String password;

	private int numberOfRetries = DEFAULT_NUMBER_OF_RETRIES;
	
	private boolean skipExisting;

	private MessageFormat format = new MessageFormat("{0} *{1}");

	public void setUrl(final URL urlString) {
		this.resourceUrl = urlString;
	}

	public URL getUrl() {
		return this.resourceUrl;
	}

	public void setDestFile(final File resourceDest) {
		this.resourceDest = resourceDest;
		
	}

	public File getDestFile() {
		return this.resourceDest;
	}
	
	public void setHashUrl(final URL hashUrl) {
		this.hashUrl = hashUrl;
		
	}

	public URL getHashUrl() {
		return this.hashUrl;
	}

	public void setTimeout(final long timeout, final TimeUnit unit) {
		this.timeoutInMillis = unit.toMillis(timeout);
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
	
	public void setNumRetries(final int numberOfRetries) {
		this.numberOfRetries = numberOfRetries;
		
	}

	public int getNumRetries() {
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
	 * Use this method to verify resource-file's integrity using 
	 * a checksum file containing the file hash. The checksum file extension
	 * determines the hashing algorithm used.  
	 * 
	 * @param checksumFile
	 * 			A file containing the hash code.
	 * @throws ResourceDownloadException
	 * 			if hashing algorithm does not exist, or other exception occurs. 
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
	 * @throws TimeoutException
	 */
    public void download()
    			throws ResourceDownloadException, TimeoutException {
    	
    	if (this.resourceDest.exists() && this.skipExisting) {
    		logger.log(Level.INFO, "File already exists. "
    				+ this.resourceDest.getAbsolutePath() + " Skipping download.");
    		return;
    	}
    	for (int attempt = 1; attempt <= this.numberOfRetries; attempt++) {
    		try {
    			getResource(this.resourceUrl, this.resourceDest);
    			if (this.hashUrl != null) {
    				//create checksum file destination.
    				//The checksum file extension determines the hashing algorithm used.
    				String resourceName = getResourceName(this.hashUrl);
					File checksumFile = new File(this.resourceDest.getParent(), resourceName);
					
    				getResource(this.hashUrl, checksumFile);
    				logger.log(Level.FINE, "Verifying resource checksum using checksum file " 
    						+ checksumFile.getAbsolutePath());
    				verifyResourceChecksum(checksumFile);
    				return;
    			}
    		} catch (ResourceDownloadException e) {
    			logger.log(Level.WARNING, "Failed downloading resource on attempt " + attempt
    					+ ". Reason was " + e.getMessage(), e);
    			if (attempt == numberOfRetries) {
    				throw e;
    			}
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
        final OutputStream os = getFileOutputString(destination);
        boolean finished = false;
        try {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            logger.info("Downloading " + downloadURL.toString() + " to " + this.resourceDest);
            while ((length = is.read(buffer)) >= 0) {
                os.write(buffer, 0, length);
                if (end < System.currentTimeMillis()) {
                	throw new TimeoutException();
                }
            }
            finished = true;
        } catch (IOException e) {
        	logger.warning("Failed downloading resource from " + downloadURL.toString());
        	throw new ResourceDownloadException("Failed downloading resource.", e);
        } finally {
        	IOUtils.closeQuietly(os);
        	IOUtils.closeQuietly(is);
        	
            if (!finished) {
            	logger.log(Level.WARNING, "Download did not complete successfully. deleting file.");
            	destination.delete();
            }
        }
	}

	private OutputStream getFileOutputString(final File destination)
			throws ResourceDownloadException {
		destination.getParentFile().mkdirs(); 
        try {
        	destination.createNewFile();
        	return new FileOutputStream(destination);
        } catch (Exception e) {
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
