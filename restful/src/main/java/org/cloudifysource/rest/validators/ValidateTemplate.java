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

<<<<<<< HEAD
<<<<<<< HEAD
=======
import org.cloudifysource.domain.Application;
>>>>>>> CLOUDIFY-2164 rearranged validations.
=======
import java.util.logging.Logger;

>>>>>>> CLOUDIFY-2164 Added missing validations and tests.
import org.cloudifysource.domain.ComputeDetails;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 *
 * @author yael
 * @since 2.7.0 
 */
@Component
public class ValidateTemplate implements InstallServiceValidator {
    
	private static final Logger logger = Logger.getLogger(ValidateTemplateOperation.class.getName());

    @Override
    public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
    	logger.info("Validating tempalte");
        Cloud cloud = validationContext.getCloud();
        Service service = validationContext.getService();
        String templateName = null;
        if (service != null) {
        	ComputeDetails compute = service.getCompute();
        	if (compute != null) {
        		templateName = compute.getTemplate();
        	}
<<<<<<< HEAD
=======
        }
        validateTemplate(templateName, cloud);
    }

    @Override
    public void validate(final InstallApplicationValidationContext validationContext)
            throws RestErrorException {
        final Application application = validationContext.getApplication();
        final Cloud cloud = validationContext.getCloud();
        for (Service service : application.getServices()) {
            if (service.getCompute() != null) {
                validateTemplate(service.getCompute().getTemplate(), cloud);
            }
>>>>>>> CLOUDIFY-2164 rearranged validations.
        }
        validateTemplate(templateName, cloud);
    }

    private void validateTemplate(final String templateName, final Cloud cloud) throws RestErrorException {
        if (cloud == null || templateName == null) {
            // no template validation for local cloud
            // if template not defined then it will automatically use the management template.
            return;
        }
        // validate that the template exist at cloud's template list
        final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(templateName);
        if (template == null) {
            throw new RestErrorException(CloudifyErrorMessages.MISSING_TEMPLATE.getName(), templateName);
        }
    }
}
