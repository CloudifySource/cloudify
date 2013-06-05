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
 ******************************************************************************/
package org.cloudifysource.rest.repo;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

/**
 * Cleaning the upload directory.
 * @author yael
 *
 */
public class CleanUploadDirRunnable implements Runnable {
	private static final Logger logger = Logger.getLogger(CleanUploadDirRunnable.class.getName());

	private File restUploadDir;
	private long cleanupTimeoutMillis;
	
	public CleanUploadDirRunnable(final File restUploadDir, final long cleanupTimeoutMillis) {
		this.restUploadDir = restUploadDir;
		this.cleanupTimeoutMillis = cleanupTimeoutMillis;
	}
	
	@Override
	public void run() {
		logger.finest("cleaning all the folders in upload directory that are in the system more than " 
				+ cleanupTimeoutMillis + " millis.");
		if (restUploadDir != null) {
			File[] listFiles = restUploadDir.listFiles();
			for (File file : listFiles) {
				if (System.currentTimeMillis() - file.lastModified() >= cleanupTimeoutMillis) {
					logger.finer("delete folder " + file.getName());
					FileUtils.deleteQuietly(file);
				} else {
					logger.finest("Folder " + file.getName() + " was not deleted (in the system for " 
							+ (System.currentTimeMillis() - file.lastModified()) + "millis).");
				}
			}
		}

	}

}
