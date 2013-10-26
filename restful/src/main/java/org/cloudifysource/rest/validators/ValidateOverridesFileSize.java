/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 * Validate the service overrides file size.
 * @author yael
 *
 */
@Component
public class ValidateOverridesFileSize implements InstallServiceValidator, InstallApplicationValidator {

    private static final long DEFAULT_SERVICE_OVERRIDES_FILE_LENGTH_LIMIT_BYTES =
            CloudifyConstants.SERVICE_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;
    private long serviceOverridesFileSizeLimit = DEFAULT_SERVICE_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;

	private static final long DEFAULT_APPLICATION_OVERRIDES_FILE_LENGTH_LIMIT_BYTES =
			CloudifyConstants.APPLICATION_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;
	private long applicationOverridesFileSizeLimit = DEFAULT_APPLICATION_OVERRIDES_FILE_LENGTH_LIMIT_BYTES;

	@Override
	public void validate(final InstallApplicationValidationContext validationContext) throws RestErrorException {
		final File appOverridesFile = validationContext.getApplicationOverridesFile();
		if (appOverridesFile != null) {
			final long length = appOverridesFile.length();
			if (length > applicationOverridesFileSizeLimit) {
				throw new RestErrorException(CloudifyMessageKeys.APPLICATION_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName(),
						appOverridesFile.getAbsolutePath(), length, applicationOverridesFileSizeLimit);
			}
		}
	}
	
    @Override
    public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
        File serviceOverridesFile = validationContext.getServiceOverridesFile();
        if (serviceOverridesFile != null) {
            long length = serviceOverridesFile.length();
            if (length > serviceOverridesFileSizeLimit) {
                throw new RestErrorException(CloudifyMessageKeys.SERVICE_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName(),
                        serviceOverridesFile.getAbsolutePath(), length, serviceOverridesFileSizeLimit);
            }
        }
    }

    public long getApplicationOverridesFileSizeLimit() {
    	return applicationOverridesFileSizeLimit;
    }
    
    public void setApplicationOverridesFileSizeLimit(final long applicationOverridesFileSizeLimit) {
    	this.applicationOverridesFileSizeLimit = applicationOverridesFileSizeLimit;
    }

    public long getServiceOverridesFileSizeLimit() {
        return serviceOverridesFileSizeLimit;
    }

    public void setServiceOverridesFileSizeLimit(final long serviceOverridesFileSizeLimit) {
        this.serviceOverridesFileSizeLimit = serviceOverridesFileSizeLimit;
    }

}
