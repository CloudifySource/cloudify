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

import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.springframework.stereotype.Component;

/**
 *
 * @author yael
 *
 */
@Component
public class ValidateTemplate implements InstallServiceValidator, InstallApplicationValidator {

    @Override
    public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
        Cloud cloud = validationContext.getCloud();
        String templateName = validationContext.getTemplateName();
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
        }
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
            throw new RestErrorException(CloudifyMessageKeys.MISSING_TEMPLATE.getName(), templateName);
        }
    }
}
