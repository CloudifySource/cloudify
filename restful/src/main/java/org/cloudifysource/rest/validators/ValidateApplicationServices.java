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

import java.util.List;
import java.util.logging.Logger;

import org.cloudifysource.domain.Service;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validates all application services.
 * Uses all service validators to validate each service.
 * Does not perform validations that were already performed for the application:
 * 	1. cloud configuration size limit
 * 	2. cloud overrides size limit
 * 	3. application overrides file size limit
 * 	4. debug settings
 * 
 * @author yael
 * @since 2.7.0 
 * 
 */
@Component
public class ValidateApplicationServices implements InstallApplicationValidator {

	private static final Logger logger = Logger.getLogger(ValidateApplicationServices.class.getName());
	@Autowired
	private InstallServiceValidator[] installServiceValidators = new InstallServiceValidator[0];
	
	@Override
	public void validate(final InstallApplicationValidationContext validationContext) throws RestErrorException {
		logger.info("Validating application services");
		List<Service> services = validationContext.getApplication().getServices();
		for (Service service : services) {
			InstallServiceValidationContext serviceValidationContext = new InstallServiceValidationContext();
			serviceValidationContext.setAdmin(validationContext.getAdmin());
			serviceValidationContext.setCloud(validationContext.getCloud());
			serviceValidationContext.setService(service);
			logger.info("validating service " + service.getName() 
					+ " for application " + validationContext.getApplication().getName());
			// for each install service validator, perform service's validations.
			for (InstallServiceValidator validator : getInstallServiceValidators()) {
				validator.validate(serviceValidationContext);
			}
		}
	}

	public InstallServiceValidator[] getInstallServiceValidators() {
		return installServiceValidators;
	}

	public void setInstallServiceValidators(final InstallServiceValidator[] installServiceValidators) {
		this.installServiceValidators = installServiceValidators;
	}

}
