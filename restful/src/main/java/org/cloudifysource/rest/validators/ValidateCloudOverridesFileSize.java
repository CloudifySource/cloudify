/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.validators;

import java.io.File;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 * Validate the size of cloud overrides file.
 * @author yael
 * @since 2.7.0 
 */
@Component
public class ValidateCloudOverridesFileSize implements InstallServiceValidator , InstallApplicationValidator {

	private static final Logger logger = Logger.getLogger(ValidateCloudOverridesFileSize.class.getName());

    private static final long DEFAULT_CLOUD_OVERRIDES_FILE_LENGTH_LIMIT_BYTES =
            CloudifyConstants.CLOUD_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;
    private long cloudOverridesFileSizeLimit = DEFAULT_CLOUD_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;

    @Override
    public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
    	validateCloudOverridesFileSize(validationContext.getCloudOverridesFile());
    }

    @Override
    public void validate(final InstallApplicationValidationContext validationContext) throws RestErrorException {
    	validateCloudOverridesFileSize(validationContext.getCloudOverridesFile());
    }
    
    private void validateCloudOverridesFileSize(final File cloudOverridesFile) throws RestErrorException {
    	logger.info("Validating cloud overrides file size");
    	if (cloudOverridesFile != null) {
    		if (cloudOverridesFile.length() > cloudOverridesFileSizeLimit) {
    			throw new RestErrorException(CloudifyMessageKeys.CLOUD_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName(),
    					cloudOverridesFile.getAbsolutePath());
    		}
    	}
    }

    public long getCloudOverridesFileSizeLimit() {
        return cloudOverridesFileSizeLimit;
    }

    public void setCloudOverridesFileSizeLimit(final long cloudOverridesFileSizeLimit) {
        this.cloudOverridesFileSizeLimit = cloudOverridesFileSizeLimit;
    }


}
