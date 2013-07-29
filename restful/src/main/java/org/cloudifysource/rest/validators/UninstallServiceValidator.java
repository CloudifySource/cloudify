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

import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * An interface for uninstall-service validator classes. Each validator must implement the validate method.
 * 
 * @author noak
 */
public interface UninstallServiceValidator {
	
	/**
	 * @param validationContext .
	 * @throws org.cloudifysource.rest.controllers.RestErrorException .
	 */
	void validate(final UninstallServiceValidationContext validationContext) throws RestErrorException;
}
