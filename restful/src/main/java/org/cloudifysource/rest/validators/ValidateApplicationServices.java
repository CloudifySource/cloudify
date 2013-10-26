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

import org.cloudifysource.domain.Service;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validates all application's services.
 * Uses all service's validators to validate each service.
 * Does not perform validations that allready performed for the application:
 * 	1. debug settings
 * 	2. cloud configuration size limit
 * 	3. cloud overrides size limit
 * 	4. application overrides file size limit
 * 	5. ESM exist
 * 	6. GSM state
 * 
 * @author yael
 * @since 2.7
 * 
 */
@Component
public class ValidateApplicationServices implements InstallApplicationValidator {

	@Autowired
	private InstallServiceValidator[] installServiceValidators = new InstallServiceValidator[0];
	
	@Override
	public void validate(final InstallApplicationValidationContext validationContext) throws RestErrorException {
		
		List<Service> services = validationContext.getApplication().getServices();
		for (Service service : services) {
			InstallServiceValidationContext serviceValidationContext = new InstallServiceValidationContext();
			serviceValidationContext.setCloud(validationContext.getCloud());
			serviceValidationContext.setService(service);
			// for each install service validator, perform service's validations.
			for (InstallServiceValidator validator : installServiceValidators) {
				validator.validate(serviceValidationContext);
			}
		}
	}

}
