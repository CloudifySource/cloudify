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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.springframework.stereotype.Component;

/**
 * 
 * @author yael
 * @since 2.7.0 
 */
@Component
public class ValidateEsmExists implements InstallServiceValidator, InstallApplicationValidator {

	private static final Logger logger = Logger.getLogger(ValidateEsmExists.class.getName());

	private static final int TIMEOUT = 5000;

	@Override
	public void validate(final InstallApplicationValidationContext validationContext) throws RestErrorException {
		validateEsmExists(validationContext.getAdmin());
	}

	@Override
	public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
		validateEsmExists(validationContext.getAdmin());
	}

	private void validateEsmExists(final Admin admin) throws RestErrorException {
		logger.info("Validating that Esm exists");
		if (admin != null) {
			final ElasticServiceManager esm = admin.getElasticServiceManagers().waitForAtLeastOne(TIMEOUT,
					TimeUnit.MILLISECONDS);
			if (esm == null) {
				throw new RestErrorException(CloudifyMessageKeys.ESM_MISSING.getName(), 
						Arrays.toString(admin.getGroups()));
			}
		}
	}

}
