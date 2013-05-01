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

import org.cloudifysource.dsl.ComputeDetails;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.util.IsolationUtils;
import org.cloudifysource.restclient.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ValidateTemplate implements InstallServiceValidator {

	@Override
	public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
		Cloud cloud = validationContext.getCloud();
		if (cloud == null) {
			// no template validation for local cloud
			return;
		}
		String templateName = validationContext.getTemplateName();
		if (templateName == null) {
			throw new RestErrorException(CloudifyMessageKeys.VALIDATOR_TEMPLATE_NAME_MISSING.getName());
		}
		Service service = validationContext.getService();
		if (service == null) {
			throw new RestErrorException(CloudifyMessageKeys.VALIDATOR_SERVICE_MISSING.getName());
		}
		final ComputeDetails compute = service.getCompute();
		templateName = compute.getTemplate();
		
		if (IsolationUtils.isGlobal(service) && IsolationUtils.isUseManagement(service)) {
			final String managementTemplateName = cloud.getConfiguration().getManagementMachineTemplate();
			if (compute != null) {
				if (StringUtils.notEmpty(templateName)) {
					if (!templateName.equals(managementTemplateName)) {
						// this is just a clarification log.
						// the service wont be installed on a management machine(even if there is enough memory)
						// because the management machine template does not match the desired template
						logger.warning("Installation of service " + service.getName() + " on a management machine "
								+ "will not be attempted since the specified template(" + templateName + ")"
								+ " is different than the management machine template(" + managementTemplateName + ")");
					}
				}
			}
		}
		// validate that the template exist at cloud's template list
		final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(templateName);
		if (template == null) {
			throw new RestErrorException(CloudifyMessageKeys.MISSING_TEMPLATE.getName(), templateName);
		}
		
	}

}
