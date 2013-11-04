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

import org.cloudifysource.domain.ExecutableEntriesMap;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;
/**
 * used to validate the service does not define any custom command with the prefix '.cloudify'.
 * 
 * @author adaml
 *
 */
@Component
public class ValidateCustomCommandName implements InstallServiceValidator {
	
	private static final Logger logger = Logger.getLogger(ValidateCustomCommandName.class.getName());

	private String reservedPrefix = CloudifyConstants.BUILT_IN_COMMAND_PREFIX;
	
	@Override
	public void validate(final InstallServiceValidationContext validationContext)
			throws RestErrorException {
		logger.info("Validating custom command name");
		ExecutableEntriesMap customCommands = validationContext.getService().getCustomCommands();
		for (String key : customCommands.keySet()) {
			if (key.startsWith(reservedPrefix)) {
				throw new RestErrorException(CloudifyErrorMessages.ILLEGAL_CUSTOM_COMMAND_PREFIX.getName(), 
						key, reservedPrefix);
			}
		}
	}
}
