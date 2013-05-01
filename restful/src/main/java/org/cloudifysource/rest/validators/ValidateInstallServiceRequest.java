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

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.restclient.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Validate all fields of {@link InstallServiceRequest}.
 * @author yael
 *
 */
@Component
public class ValidateInstallServiceRequest implements InstallServiceValidator {

	/**
	 * 
	 */
	private static final int OVERRIDES_LENGTH_KB = 20;
	private static final long DEFAULT_OVERRIDES_MAX_LENGTH = (OVERRIDES_LENGTH_KB * FileUtils.ONE_KB) / Character.SIZE;
	private long overrdiesMaxLength = DEFAULT_OVERRIDES_MAX_LENGTH;


	@Override
	public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
		String absolutePuName = validationContext.getAbsolutePuName();
		InstallServiceRequest request = validationContext.getRequest();
		if (request == null) {
			throw new RestErrorException(CloudifyMessageKeys.VALIDATOR_REQUEST_MISSING.getName(), absolutePuName);
		}
		String uploadKey = request.getUploadKey();
		if (!StringUtils.notEmpty(uploadKey)) {
			throw new RestErrorException(CloudifyMessageKeys.UPLOAD_KEY_PARAMETER_MISSING.getName(), absolutePuName);
		}

		String cloudOverrides = request.getCloudOverrides();
		if (cloudOverrides.length() > overrdiesMaxLength) {
			throw new RestErrorException(CloudifyMessageKeys.OVERRIDES_LENGTH_LIMIT_EXCEEDED.getName(), absolutePuName);
		}
		String serviceOverrides = request.getServiceOverrides();
		if (serviceOverrides.length() > overrdiesMaxLength) {
			throw new RestErrorException(CloudifyMessageKeys.OVERRIDES_LENGTH_LIMIT_EXCEEDED.getName(), absolutePuName);
		}

		String authGroups = request.getAuthGroups();
		
	}


	public void setOverrdiesMaxLength(final long overrdiesMaxLength) {
		this.overrdiesMaxLength = overrdiesMaxLength;
	}

}
