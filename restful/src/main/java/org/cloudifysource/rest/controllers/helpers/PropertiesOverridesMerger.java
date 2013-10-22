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
	 * The merge destination file. This field is mandatory.
	 */
	private final File destMergeFile;
	
	private final File applicationPropertiesFile;
	private final File servicePropertiesFile;
	private final File overridesFile;

	private boolean isMerged;
	
	public PropertiesOverridesMerger(final File destMergeFile, 
			final File applicationPropertiesFile,
			final File propertiesFile, final File overridesFile) {
		this.destMergeFile = destMergeFile;
		this.applicationPropertiesFile = applicationPropertiesFile;
		this.servicePropertiesFile = propertiesFile;
		this.overridesFile = overridesFile;
		this.isMerged = false;
	}

	/**
	 * Merge application properties file with service properties and overrides files.
	 * The merged file will be written to destMergeFile.
	 * 
	 * @throws org.cloudifysource.rest.controllers.RestErrorException .
	 */
	public void merge() throws RestErrorException {
		// check if merge is necessary
		if (applicationPropertiesFile == null && overridesFile == null) {
			return;
		}
		try {
			// append application properties, service properties and overrides files
			final LinkedHashMap<File, String> mergeFilesAndComments = new LinkedHashMap<File, String>();
			mergeFilesAndComments.put(applicationPropertiesFile, "application properties file");
			mergeFilesAndComments.put(servicePropertiesFile, "service properties file");
			mergeFilesAndComments.put(overridesFile, "properties overrides file");
			// use FileAppender to append all files to one file and store it in the service properties file
			// (creates one if not exist).
			new FileAppender(DEFAULT_MERGED_FILE_NAME).appendAll(destMergeFile, mergeFilesAndComments);
			isMerged = true;
		} catch (final IOException e) {
			throw new RestErrorException(CloudifyMessageKeys.FAILED_TO_MERGE_OVERRIDES.getName(),
					destMergeFile.getAbsolutePath(), e.getMessage());

		}
	}

	public boolean isMerged() {
		return isMerged;
	}

}
