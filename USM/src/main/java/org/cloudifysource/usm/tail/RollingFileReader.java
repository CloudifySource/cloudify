/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.usm.tail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * RollingFileReader was created in-order for an application to be able to access a file and tail it without locking it.
 * whenever lines are added to the file, the RFR will open the file for a brief moment, "grab" the new lines added and
 * close the file. the RFR remembers it's file-pointer and when reopening the file, the RFR will read the lines from the
 * point where it left-off.
 * 
 * @author adaml
 */
public class RollingFileReader {

	private static final int TIMEOUT_BETWEEN_RETRIES = 1000;
	private static final int DEFAULT_NUMBER_OF_RETRIES = 5;
	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(RollingFileReader.class
			.getName());

	private long lastModified;
	private long filePointer;
	private final File file;
	private boolean exists;

	private int retryCounter;
	private long fileLength;

	/**
	 * Constructor.
	 * 
	 * @param file The file to read
	 */
	public RollingFileReader(final File file) {
		this.lastModified = 0;
		this.file = file;
		this.fileLength = file.length();
		this.exists = true;
	}

	/**
	 * checks if the modification time of the file matches the last modification time since the file was last tailed.
	 * 
	 * @return true if the file has been modified since last polled.
	 */
	public boolean wasModified() {
		return this.lastModified != file.lastModified() || this.fileLength != file.length();

	}

	/**
	 * reads the new lines added to the log file. The method supports RollingFileAppender tailing by not keeping the
	 * file open and opening the file only when a changes have been made to it. After reading the changes, the file will
	 * be closed and all relevant pointers and properties such as last modified date will be saved for the next
	 * iteration.
	 * 
	 * note that the file is being closed in-order to enable the RFA to properly roll the file without having lock
	 * issues.
	 * 
	 * @return new lines added to the log file.
	 * @throws IOException Indicates the lines were not read because of an IO exception
	 */
	public String readLines()
			throws IOException {

		RandomAccessFile randomAccessFile = null;

		try {

			randomAccessFile = new RandomAccessFile(this.file, "r");

			if (this.filePointer > randomAccessFile.length()) {
				// the file must have been rolled. Start form the beginning of the new file.
				this.filePointer = 0;
			}

			// set the file pointer in the new RandomFileAccess.
			randomAccessFile.seek(filePointer);

			// allocate buffer size.
			final byte[] buffer = new byte[(int) randomAccessFile.length() - (int) this.filePointer];

			// read all new data into the buffer.
			randomAccessFile.read(buffer);

			// save the last filePointer location before closing the file.
			this.filePointer = randomAccessFile.length();

			randomAccessFile.close();

			this.lastModified = this.file.lastModified();

			retryCounter = 0;

			this.fileLength = file.length();

			return new String(buffer);
		} catch (final FileNotFoundException e) {
			// in-case we try to access the file at the exact time it is being rolled.
			retryCounter++;
			if (retryCounter > DEFAULT_NUMBER_OF_RETRIES) {
				logger.warning("In RollingFileReader: file not found." + DEFAULT_NUMBER_OF_RETRIES
						+ " Retries failed.");
				this.exists = false;
				return "";
			}
			try {
				logger.warning("file not found: " + file.getName() + ". Retring attempt #" + retryCounter);
				Thread.sleep(TIMEOUT_BETWEEN_RETRIES);
			} catch (final InterruptedException e1) {
				e1.printStackTrace();
			}
			return readLines();
		} finally {
			if (randomAccessFile != null) {
				randomAccessFile.close();
			}
		}

	}

	/**
	 * returns false if the file has been removed from the system and was not recreated after a certain time period.
	 * 
	 * @return returns false if the file has been removed from the system and was not recreated after a certain time
	 *         period.
	 */
	public boolean exists() {
		return exists;
	}
}
