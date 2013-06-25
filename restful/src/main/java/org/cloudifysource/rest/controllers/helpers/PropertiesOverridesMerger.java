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

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.packaging.FileAppender;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * 
 * @author yael
 * @since 2.6.0
 * 
 */
public class PropertiesOverridesMerger {

	private static final String DEFAULT_MERGED_FILE_NAME = "mergedPropertiesFile.properties";
	
	/**
	 * The merge destination file.
	 * This field is mandatory.
	 */
	private File destMergeFile;
	/**
	 * The name of the updated zip file.
	 */
	private String rePackFileName;
	/**
	 * The folder to re-pack after merging.
	 * This field is mandatory.
	 */
	private File rePackFolder;
	/**
	 * The original packed file, return from merge if no merge was needed. 
	 */
	private File originPackedFile;
	private File applicationPropertiesFile;
	private File servicePropertiesFile;
	private File overridesFile;

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
		// check if merge is necessary
		if (applicationPropertiesFile == null && overridesFile == null) {
			return originPackedFile;
		}
		validateValues(originPackedFile);
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

	private void validateValues(final File originPackedFile) throws RestErrorException {
		if (destMergeFile == null) {
			throw new RestErrorException(CloudifyMessageKeys.DEST_MERGE_FILE_MISSING.getName());
		}
		if (rePackFileName == null) {
			String originName = originPackedFile.getName();
			String shortOriginName = originName;
			if (originName.endsWith(".zip")) {
				shortOriginName = originName.split("//.zip")[0];
			}
			rePackFileName = shortOriginName + "_repack.zip";
		}
		if (rePackFolder == null) {
			throw new RestErrorException(CloudifyMessageKeys.REPACKED_MERGE_FOLDER_MISSING.getName());
		}
	}

	public void setDestMergeFile(final File destMergeFile) {
		this.destMergeFile = destMergeFile;
	}

}
