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

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.StorageDetails;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 * validates the storage template exists.
 * 
 * @author adaml
 *
 */
@Component
public class ValidateStorageTemplateExists implements InstallServiceValidator {

    private static final Logger logger = Logger.getLogger(ValidateStorageTemplateExists.class.getName());

	@Override
	public void validate(final InstallServiceValidationContext validationContext)
			throws RestErrorException {
		logger.info("Validating storage tempalte");
		final Service service = validationContext.getService();
		final StorageDetails storage = service.getStorage();
		if (storage != null) {
			String serviceTemplateName = storage.getTemplate();
			Cloud cloud = validationContext.getCloud();
			validateStorageTemplateExists(serviceTemplateName, cloud);
		}
	}

	//this is public since used by the application storage validator.
	/**
	 * Validate storage template exists in cloud.
	 * @param serviceTemplateName .
	 * @param cloud .
	 * @throws RestErrorException .
	 */
	public void validateStorageTemplateExists(final String serviceTemplateName,
			final Cloud cloud) throws RestErrorException {
		if (cloud != null && !StringUtils.isEmpty(serviceTemplateName)) {
			final Map<String, StorageTemplate> templates = cloud.getCloudStorage().getTemplates();
			final StorageTemplate storageTemplate = templates.get(serviceTemplateName);
			if (storageTemplate == null) {
				throw new RestErrorException(CloudifyErrorMessages.MISSING_TEMPLATE.getName(), serviceTemplateName);
			}
		}
	}
}
