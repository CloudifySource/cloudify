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

import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_SERVICE;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.rest.controllers.RestErrorException;
import org.openspaces.admin.pu.ProcessingUnit;
import org.springframework.stereotype.Component;

/**
 * 
 * @author yael
 * @since 2.7.0 
 */
@Component
public class ValidateServiceExists implements UninstallServiceValidator {

	private static final Logger logger = Logger.getLogger(ValidateServiceExists.class.getName());

	private static final long TIMEOUT_SECONDS = 10;
	
	@Override
	public void validate(final UninstallServiceValidationContext validationContext) throws RestErrorException {
		logger.info("Validating that service exists");
		final String puName = validationContext.getPuName();
		ProcessingUnit pu = validationContext.getAdmin().getProcessingUnits()
				.waitFor(puName, TIMEOUT_SECONDS, TimeUnit.SECONDS);
		if (pu == null) {
			logger.log(Level.INFO, "Cannot uninstall service " 
					+ puName + " since it has not been discovered yet.");
			throw new RestErrorException(FAILED_TO_LOCATE_SERVICE, puName);
		}

	}

}
