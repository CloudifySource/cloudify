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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnits;
import org.springframework.stereotype.Component;

/**
 * 
 * @author yael
 * @since 2.7.0
 */
@Component
public class ValidateRemoveTemplate extends ValidateTemplateOperation implements RemoveTemplateValidator {
    private static final Logger logger = Logger.getLogger(ValidateRemoveTemplate.class.getName());
    
	@Override
	public void validate(final RemoveTemplatesValidationContext validationContext) throws RestErrorException {
		
		logger.fine("validating remove-templates");
		
		validationContext.setOperationName("remove-templates");
		super.validate(validationContext);
		
		String templateName = validationContext.getTemplateName();
		
		validateTemplateExist(templateName, validationContext.getCloud());
		
		validateNotDefaultTempalte(templateName, validationContext.getCloudDeclaredTemplates());
		
		validateNotInUse(templateName, validationContext.getAdmin());
	}
	
	private void validateTemplateExist(final String templateName, final Cloud cloud) 
			throws RestErrorException {
		final Map<String, ComputeTemplate> cloudTemplates = cloud.getCloudCompute().getTemplates();
		if (!cloudTemplates.containsKey(templateName)) {
			logger.warning("[validateTemplateExist] - tempalte [" + templateName + "] doesn't exist in cloud's list.");
			throw new RestErrorException(CloudifyErrorMessages.TEMPLATE_NOT_EXIST.getName(), templateName);
		}
	}
	
	private void validateNotDefaultTempalte(final String templateName, 
			final List<String> cloudDeclaredTemplates) 
					throws RestErrorException {
		logger.info("checking template: " + templateName + " in cloudDeclaredTemplates: " + cloudDeclaredTemplates);
		if (cloudDeclaredTemplates.contains(templateName)) {
			logger.warning("cannot remove template that was declared in the cloud's configuration file. "
					+ "This is not an additional template: " + templateName);
			throw new RestErrorException(CloudifyErrorMessages.ILLEGAL_REMOVE_DEFAULT_TEMPLATE.getName(), templateName);
		}
	}
	
	private void validateNotInUse(final String templateName, final Admin admin) 
			throws RestErrorException {
		List<String> templateServices = getTemplateServices(templateName, admin.getProcessingUnits());			
		// check if the template is being used by at least one service, so it cannot be removed.
		if (!templateServices.isEmpty()) {
			logger.warning("[removeTemplateInternal] - failed to remove template [" + templateName
					+ "]. The template is being used by the following services: " + templateServices);
			throw new RestErrorException(CloudifyErrorMessages.TEMPLATE_IN_USE.getName(),
					templateName, templateServices);
		}
	}
	
	private List<String> getTemplateServices(final String templateName, final ProcessingUnits processingUnits) {
		final List<String> services = new LinkedList<String>();
		for (final ProcessingUnit processingUnit : processingUnits) {
			final Properties puProps = processingUnit.getBeanLevelProperties().getContextProperties();
			final String puTemplateName = puProps.getProperty(CloudifyConstants.CONTEXT_PROPERTY_TEMPLATE);
			if (puTemplateName != null && puTemplateName.equals(templateName)) {
				services.add(processingUnit.getName());
			}
		}
		return services;
	}

}
