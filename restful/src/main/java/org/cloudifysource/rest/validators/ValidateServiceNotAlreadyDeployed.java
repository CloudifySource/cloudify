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

import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnits;
import org.springframework.stereotype.Component;

/**
 * 
 * @author yael
 * @since 2.7.0 
 */
@Component
public class ValidateServiceNotAlreadyDeployed implements InstallServiceValidator {

    private static final Logger logger = Logger.getLogger(ValidateServiceNotAlreadyDeployed.class.getName());

	@Override
	public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
		logger.info("Validating that service is not already deployed");
		final String puName = validationContext.getPuName();
		final ProcessingUnits pus = validationContext.getAdmin().getProcessingUnits();
		for (ProcessingUnit pu : pus) {
			if (pu.getName().equals(puName)) {
				throw new RestErrorException(CloudifyErrorMessages.SERVICE_ALREADY_INSTALLED.getName(), puName);
			}
		}
	}

}
