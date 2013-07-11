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
package org.cloudifysource.dsl.internal.validators;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.DSLValidation;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author adaml
 *
 */
public class ApplicationValidator implements DSLValidator {

	private Application application;
	
	
	@Override
	public void setDSLEntity(final Object dslEntity) {
		this.application = (Application) dslEntity;
	}
	
	/**
	 * Validates that the name property exists and is not empty or invalid.
	 * @param validationContext
	 * @throws DSLValidationException
	 */
	@DSLValidation
	public void validateName(final DSLValidationContext validationContext) 
			throws DSLValidationException {
		if (StringUtils.isBlank(application.getName())) {
			throw new DSLValidationException("Application.validateName: The application's name " 
					+ (application.getName() == null ? "is missing" : "is empty"));
		}
		DSLUtils.validateRecipeName(application.getName());
	}

}
