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

import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.Applications;
import org.springframework.stereotype.Component;

/**
 * Validate application is not already installed.
 * 
 * @author adaml
 *
 */
@Component
public class ValidateApplicationNotAlreadyDeployed implements InstallApplicationValidator {
	
	private static final Logger logger = Logger.getLogger(ValidateApplicationNotAlreadyDeployed.class.getName());

	@Override
	public void validate(final InstallApplicationValidationContext validationContext)
			throws RestErrorException {
		logger.info("Validating that application is not already deployed");
		final String appName = validationContext.getApplication().getName();
		final Applications apps = validationContext.getAdmin().getApplications();
		for (Application application : apps) {
			if (application.getName().equals(appName)) {
				throw new RestErrorException(CloudifyMessageKeys.APPLICATION_NAME_IS_ALREADY_IN_USE.getName(), appName);
			}
		}
	}

	
}
