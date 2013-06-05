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
package org.cloudifysource.rest.controllers.helpers;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.packaging.FileAppender;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * 
 * @author yael
 * 
 */
public class PropertiesOverridesMerger {

	private static final String DEFAULT_MERGED_FILE_NAME = "mergedPropertiesFile.properties";
	private String rePackFileName;
	private File rePackFolder;
	private File originPackedFile;
	private File applicationPropertiesFile;
	private File servicePropertiesFile;
	private File overridesFile;
	private File destMergeFile;

	public void setRePackFileName(final String rePackFileName) {
		this.rePackFileName = rePackFileName;
	}

	public void setRePackFolder(final File rePackFolder) {
		this.rePackFolder = rePackFolder;
	}

	public void setOriginPackedFile(final File originPackedFile) {
		this.originPackedFile = originPackedFile;
	}
	
	public void setApplicationPropertiesFile(final File applicationPropertiesFile) {
		this.applicationPropertiesFile = applicationPropertiesFile;
	}

	public void setServicePropertiesFile(final File servicePropertiesFile) {
		this.servicePropertiesFile = servicePropertiesFile;
	}

	public void setOverridesFile(final File overridesFile) {
		this.overridesFile = overridesFile;
	}
	
	/**
	 * Merge application properties file with service properties and overrides files.
	 * 
	 * @return the updated packed file or the original one if no merge needed.
	 * @throws org.cloudifysource.rest.controllers.RestErrorException .
	 */
	public File merge() throws RestErrorException {
		if (destMergeFile == null) {
			throw new RestErrorException(CloudifyMessageKeys.DEST_MERGE_FILE_MISSING.getName());
		}
		updateDefaultValues(originPackedFile);
		// check if merge is necessary
		if (applicationPropertiesFile == null && overridesFile == null) {
			return originPackedFile;
		}
		try {
			// append application properties, service properties and overrides files
			LinkedHashMap<File, String> mergeFilesAndComments = new LinkedHashMap<File, String>();
			mergeFilesAndComments.put(applicationPropertiesFile, "application properties file");
			mergeFilesAndComments.put(servicePropertiesFile, "service properties file");
			mergeFilesAndComments.put(overridesFile, "properties overrides file");
			// use FileAppender to append all files to one file and store it in the service properties file 
			// (creates one if not exist).
			new FileAppender(DEFAULT_MERGED_FILE_NAME).appendAll(destMergeFile, mergeFilesAndComments);
			return Packager.createZipFile(rePackFileName, rePackFolder);
		} catch (final IOException e) {
			throw new RestErrorException(CloudifyMessageKeys.FAILED_TO_MERGE_OVERRIDES.getName(),
					rePackFileName, e.getMessage());

		}
	}

	private void updateDefaultValues(final File originPackedFile) {
		if (rePackFileName == null) {
			rePackFileName = originPackedFile.getName();
		}
		if (rePackFolder == null) {
			rePackFolder = new File(CloudifyConstants.REST_FOLDER + File.separator);
		}
	}

	public void setDestMergeFile(final File destMergeFile) {
		this.destMergeFile = destMergeFile;
	}

}
