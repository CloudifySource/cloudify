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

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 * A validator for validate cloud overrides size.
 * @author yael
 *
 */
@Component
public class ValidateCloudOverridesSize implements InstallServiceValidator {
	
	private long cloudOverridesSizeLimitKb = CloudifyConstants.CLOUD_OVERRIDES_FILE_LENGTH_LIMIT_KB;

	@Override
	public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
		File cloudConfiguration = validationContext.getCloudConfiguration();
		if (cloudConfiguration != null) {
			if (cloudConfiguration.length() > getCloudOverridesSizeLimit()) {
				throw new RestErrorException(CloudifyMessageKeys.CLOUD_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName());
			}
		}
	}

	public long getCloudOverridesSizeLimit() {
		return cloudOverridesSizeLimitKb;
	}

	public void setCloudOverridesSizeLimit(final long cloudOverridesSizeLimit) {
		this.cloudOverridesSizeLimitKb = cloudOverridesSizeLimit;
	}

}
