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
package org.cloudifysource.rest.repo;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * A class for uploading files and getting uploaded files.
 * 
 * @author yael
 * 
 */
@Component
public class UploadRepo {
	private static final Logger logger = Logger.getLogger(UploadRepo.class.getName());

	private int uploadSizeLimitBytes = CloudifyConstants.DEFAULT_UPLOAD_SIZE_LIMIT_BYTES;
	private int cleanupTimeoutMillis = CloudifyConstants.DEFAULT_UPLOAD_TIMEOUT_MILLIS;
	private File baseDir;
	private ScheduledExecutorService executor;
	private File restUploadDir;

	/**
	 * Initializing scheduled thread.
	 * @throws RestErrorException
	 *             If failed to create upload directory.  
	 */
	public void init() throws RestErrorException {
		log(Level.INFO, "Initializing upload repo.");
		createUploadDir();
		createScheduledExecutor();
	}

	private void createScheduledExecutor() {
		String absolutePath = null;
		if (restUploadDir != null) {	
			absolutePath = restUploadDir.getAbsolutePath();
		}
		log(Level.FINE, "[createScheduledExecutor] - " 
				+ "creating cleanup thread that will clean all files from rest upload directory [" 
						+ absolutePath + "] every " + cleanupTimeoutMillis + " millis.");
		final CleanUploadDirRunnable cleanupThread =
				new CleanUploadDirRunnable(restUploadDir, cleanupTimeoutMillis);
		executor = Executors.newSingleThreadScheduledExecutor();
		try {
			executor.scheduleAtFixedRate(cleanupThread, 0, cleanupTimeoutMillis, TimeUnit.MILLISECONDS);
		} catch (final RejectedExecutionException e) {
			log(Level.WARNING, "failed to scheduled for execution - " + e.getMessage());
			throw e;
		}
	}

	/**
	 * destroy.
	 * @throws IOException .
	 */
	@PreDestroy
	public void destroy() throws IOException {
		executor.shutdown();
		FileUtils.deleteDirectory(restUploadDir);
	}

	private void reset() {
		executor.shutdownNow();
		createScheduledExecutor();
	}

	/**
	 * Creating the upload directory. 
	 * 
	 * @throws RestErrorException
	 *             If failed to create upload directory.
	 */
	public void createUploadDir()
			throws RestErrorException {
		log(Level.FINER, "[createUploadDir] - creating rest uploads directory in - " + baseDir);
		restUploadDir = new File(baseDir, CloudifyConstants.UPLOADS_FOLDER_NAME);
		final String absolutePath = restUploadDir.getAbsolutePath();
		log(Level.FINER, "[createUploadDir] - setting restUploadDir to: " + absolutePath);
		restUploadDir.deleteOnExit();
		if (restUploadDir.exists()) {
			try {
				FileUtils.deleteDirectory(restUploadDir);
			} catch (IOException e) {
				log(Level.WARNING, "[createUploadDir] - failed to delete uploads directory [" + absolutePath + "].");
				throw new RestErrorException(
						CloudifyMessageKeys.UPLOAD_DIRECTORY_CREATION_FAILED.getName(), absolutePath);
			}
		}
		final boolean mkdirs = restUploadDir.mkdirs();
		if (mkdirs) {
			log(Level.FINE, "[createUploadDir] - created rest uploads directory - " + absolutePath);
		} else {
			log(Level.WARNING, "[createUploadDir] - failed to create rest uploads directory at " + absolutePath);
			throw new RestErrorException(
					CloudifyMessageKeys.UPLOAD_DIRECTORY_CREATION_FAILED.getName(), absolutePath);
		}
	}

	private void copyMultipartFileToLocalFile(final MultipartFile srcFile, final File storedFile)
			throws IOException {
		if (srcFile == null) {
			return;
		}
		srcFile.transferTo(storedFile);
		storedFile.deleteOnExit();
	}

	/**
	 * Creates a new folder with a randomly generated name (using the UUID class) which holds the uploaded file. The
	 * folder located at the main upload folder in {@link #baseDir}. This uploaded file and its folder will be deleted
	 * after {@link #cleanupTimeoutMillis} millis.
	 * 
	 * @param fileName
	 *            The name of the uploaded file. If null, the multipartFile's original file name will be used as the
	 *            file's name.
	 * @param multipartFile
	 *            The file to upload.
	 * @return the uploaded key.
	 * @throws RestErrorException
	 *             if the file doesn't end with zip.
	 * @throws IOException .
	 */
	public String put(final String fileName, final MultipartFile multipartFile)
			throws IOException, RestErrorException {
		final String name = fileName == null ? multipartFile.getOriginalFilename() : fileName;
		// enforce size limit
		log(Level.FINER, "uploading file " + name);
		final long fileSize = multipartFile.getSize();
		if (fileSize > getUploadSizeLimitBytes()) {
			log(Level.FINER, "Upload file [" + name + "] size ("
						+ fileSize + ") exceeded the permitted size limit (" + getUploadSizeLimitBytes() + ").");
			throw new RestErrorException(
					CloudifyMessageKeys.UPLOAD_FILE_SIZE_LIMIT_EXCEEDED.getName(),
					name, fileSize, getUploadSizeLimitBytes());
		}
		final String dirName = UUID.randomUUID().toString();
		final File srcDir = new File(restUploadDir, dirName);
		srcDir.mkdirs();
		final File storedFile = new File(srcDir, name);
		log(Level.FINER, "Uploading file to " + storedFile.getAbsolutePath());
		copyMultipartFileToLocalFile(multipartFile, storedFile);
		log(Level.FINER, "File [" + storedFile.getAbsolutePath() + "] uploaded successfully.");
		return dirName;
	}

	/**
	 * Gets the file stored in a directory with the given name (uploadDirName).
	 * 
	 * @param key
	 *            - the name of the upload file's directory.
	 * @return the suitable file or null if a file with that name doesn't exist.
	 */
	public File get(final String key) {
		if (key == null) {
			log(Level.WARNING, "failed to get uploaded file, key is null.");
			return null;
		}
		log(Level.FINE, "Getting uploaded file with key " + key);
		if (restUploadDir == null) {
			log(Level.WARNING, "failed to get uploaded file, key is " + key + ", upload directory is null.");
			return null;
		}
		if (!restUploadDir.exists()) {
			log(Level.WARNING, "failed to get uploaded file. key is " + key
						+ ", upload directory [" + restUploadDir.getAbsolutePath() + "] does not exist.");
			return null;
		}
		log(Level.FINER, "Trying to get the uploaded file stored in a directory named - " + key
					+ " (under " + restUploadDir.getAbsolutePath() + ").");
		final File dir = new File(restUploadDir, key);
		if (dir.exists()) {
			if (!dir.isDirectory()) {
				log(Level.WARNING, "The file found is not a directory [" + dir.getAbsolutePath() + "].");
				return null;
			}
			final File[] listFiles = dir.listFiles();
			if (listFiles.length > 0) {
				final File uploadedFile = listFiles[0];
				log(Level.FINE, "Returning the found uploaded file [" + uploadedFile.getAbsolutePath() + "].");
				return uploadedFile;
			}
			log(Level.WARNING, "The directory [" + dir.getAbsolutePath() + "] is empty.");
		} else {
			log(Level.WARNING, "No directory with name " + key + " was found at " + restUploadDir.getAbsolutePath());
		}
		return null;
	}

	public File getRestUploadDir() {
		return restUploadDir;
	}

	/**
	 * Sets the cleanup timeout and reset the scheduled thread.
	 * 
	 * @param cleanupTimeoutMillis
	 *            .
	 */
	public void resetTimeout(final int cleanupTimeoutMillis) {
		log(Level.INFO, "reset timeout to " + cleanupTimeoutMillis + " milliseconds.");
		this.setCleanupTimeoutMillis(cleanupTimeoutMillis);
		reset();
	}

	public File getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(final File baseDir) {
		this.baseDir = baseDir;
	}

	public int getCleanupTimeoutMillis() {
		return cleanupTimeoutMillis;
	}

	public void setCleanupTimeoutMillis(final int cleanupTimeoutMillis) {
		this.cleanupTimeoutMillis = cleanupTimeoutMillis;
	}

	public int getUploadSizeLimitBytes() {
		return uploadSizeLimitBytes;
	}

	public void setUploadSizeLimitBytes(final int uploadSizeLimitBytes) {
		this.uploadSizeLimitBytes = uploadSizeLimitBytes;
	}
	
	private void log(final Level level, final String message) {
		if (logger.isLoggable(level)) {
			logger.log(level, message);
		}
	}
}
