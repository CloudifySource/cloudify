/*
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
 * *****************************************************************************
 */
package org.cloudifysource.rest.validators;

import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 * 
 * @author yael
 * @since 2.7.0
 */
@Component
public class ValidateAddTemplate extends ValidateTemplateOperation implements AddTemplatesValidator {

	@Override
	public void validate(final AddTemplatesValidationContext validationContext) throws RestErrorException {
		validationContext.setOperationName("add-templates");
		super.validate(validationContext);
		if (validationContext.getRequest().getUploadKey() == null) {
			throw new RestErrorException(CloudifyErrorMessages.UPLOAD_KEY_PARAMETER_MISSING.getName());
		}
	}
}
