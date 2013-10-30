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

import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 * 
 * @author yael
 * @since 2.7.0 
 */
@Component
public class ValidateServiceName implements InstallServiceValidator {

    private static final Logger logger = Logger.getLogger(ValidateServiceName.class.getName());

	@Override
	public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
		logger.info("Validating service name");
		final String serviceName = validationContext.getService().getName();
		if (serviceName.startsWith(CloudifyConstants.ILlEGAL_SERVICE_NAME_PREFIX)) {
			throw new RestErrorException(CloudifyErrorMessages.ILLEGAL_SERVICE_NAME.getName(), 
					serviceName, 
					"starts with " + CloudifyConstants.ILlEGAL_SERVICE_NAME_PREFIX);	
		}
		if (serviceName.endsWith(CloudifyConstants.ILlEGAL_SERVICE_NAME_SUFFIX)) {
			throw new RestErrorException(CloudifyErrorMessages.ILLEGAL_SERVICE_NAME.getName(), 
					serviceName, 
					"ends with " + CloudifyConstants.ILlEGAL_SERVICE_NAME_SUFFIX);
		}
	}		
	

}
