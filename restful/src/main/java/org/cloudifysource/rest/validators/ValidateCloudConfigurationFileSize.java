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
package org.cloudifysource.rest.validators;

import java.io.File;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 * 
 * @author yael
 * 
 */
@Component
public class ValidateCloudConfigurationFileSize implements InstallServiceValidator, InstallApplicationValidator {

	private static final long DEFAULT_CLOUD_CONFIGURATION_FILE_LENGTH_LIMIT_BYTES =
			CloudifyConstants.CLOUD_CONFIGURATION_FILE_LENGTH_LIMIT_BYTES;
	private long cloudConfigurationFileSizeLimit = DEFAULT_CLOUD_CONFIGURATION_FILE_LENGTH_LIMIT_BYTES;

	@Override
	public void validate(final InstallApplicationValidationContext validationContext) throws RestErrorException {
		validateCloudConfigurationFileSize(validationContext.getCloudConfigurationFile());
	}

	@Override
	public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
		validateCloudConfigurationFileSize(validationContext.getCloudConfigurationFile());
	}

	private void validateCloudConfigurationFileSize(final File cloudConfigurationFile) throws RestErrorException {
		if (cloudConfigurationFile != null) {
			if (cloudConfigurationFile.length() > cloudConfigurationFileSizeLimit) {
				throw new RestErrorException(CloudifyMessageKeys.CLOUD_CONFIGURATION_SIZE_LIMIT_EXCEEDED.getName(),
						cloudConfigurationFile.getAbsolutePath());
			}
		}
	}

	public long getCloudConfigurationFileSizeLimit() {
		return cloudConfigurationFileSizeLimit;
	}

	public void setCloudConfigurationFileSizeLimit(final long cloudConfigurationFileSizeLimit) {
		this.cloudConfigurationFileSizeLimit = cloudConfigurationFileSizeLimit;
	}

}
