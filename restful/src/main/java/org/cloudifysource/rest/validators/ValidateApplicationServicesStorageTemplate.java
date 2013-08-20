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

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.StorageDetails;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 * validate storage templates for all of the services in an application.
 * 
 * @author adaml
 *
 */
@Component
public class ValidateApplicationServicesStorageTemplate implements InstallApplicationValidator {

	@Override
	public void validate(final InstallApplicationValidationContext validationContext)
			throws RestErrorException {
		final Cloud cloud = validationContext.getCloud();
		for (Service service : validationContext.getApplication().getServices()) {
			final StorageDetails storage = service.getStorage();
			if (storage != null) {
				final String serviceTemplateName = storage.getTemplate();
				final ValidateStorageTemplateExists validator = new ValidateStorageTemplateExists();
				validator.validateStorageTemplateExists(serviceTemplateName, cloud);
			}
		}
	}
}
