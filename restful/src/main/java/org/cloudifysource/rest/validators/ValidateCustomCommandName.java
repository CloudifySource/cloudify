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

import org.cloudifysource.domain.ExecutableEntriesMap;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.rest.controllers.RestErrorException;
/**
 * used to validate the service does not define any custom command with the prefix '.cloudify'.
 * 
 * @author adaml
 *
 */
public class ValidateCustomCommandName implements InstallServiceValidator {
	
	private String reservedPrefix = CloudifyConstants.BUILT_IN_COMMAND_PREFIX;
	
	@Override
	public void validate(final InstallServiceValidationContext validationContext)
			throws RestErrorException {
		ExecutableEntriesMap customCommands = validationContext.getService().getCustomCommands();
		for (String key : customCommands.keySet()) {
			if (key.startsWith(reservedPrefix)) {
				throw new RestErrorException("custom command name:" 
									+ key + " should not start with " + reservedPrefix);
			}
		}
	}
}
