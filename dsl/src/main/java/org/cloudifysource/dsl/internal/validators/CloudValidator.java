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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.DSLValidation;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.CloudConfiguration;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * 
 * @author adaml
 *
 */
public class CloudValidator implements DSLValidator {

	private Cloud entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (Cloud) dslEntity;
	}
	
	@DSLValidation
	void validateManagementTemplateName(final DSLValidationContext validationContext)
			throws DSLValidationException {

		final CloudConfiguration configuration = entity.getConfiguration();
		final Map<String, ComputeTemplate> templates = entity.getCloudCompute().getTemplates();

		final String managementTemplateName = configuration.getManagementMachineTemplate();

		if (StringUtils.isBlank(managementTemplateName)) {
			throw new DSLValidationException("managementMachineTemplate may not be empty");
		}

		if (!templates.containsKey(managementTemplateName)) {
			throw new DSLValidationException("The management machine template \"" + managementTemplateName + "\" is "
					+ "not listed in the cloud's templates section");
		}

	}

	/**
	 * Validate that a tenant id was assigned if required. The tenant id property is required only in openstack based
	 * clouds.
	 *
	 * @throws DSLValidationException
	 */
	@DSLValidation
	void validateTenantId(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.entity.getCustom().containsKey("openstack.tenant")) {
			final String tenantId = (String) entity.getCustom().get("openstack.tenant");
			if (tenantId.equalsIgnoreCase("ENTER_TENANT")) {
				throw new DSLValidationException("The tenant id property must be set");
			}
		}
	}

	/**
	 * Validations for dynamic-byon cloud only.
	 * Validates that each template contains startMachine and stopMachine closures in its custom closure.
	 * Validates that the management machine's template contains the
	 * 									startManagementMachines closure in its custom closure.
	 * @param validationContext .
	 * @throws DSLValidationException .
	 */
	@DSLValidation
	public void validateDynamicNodesClosures(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (CloudifyConstants.DYNAMIC_BYON_NAME.equals(entity.getName())) {
			String managementMachineTemplateName = entity.getConfiguration().getManagementMachineTemplate();
			ComputeTemplate managementMachineTemplate = entity.getCloudCompute().
					getTemplates().get(managementMachineTemplateName);
			Map<String, Object> mngTemplateCustom = managementMachineTemplate.getCustom();
			validateClosureExists(mngTemplateCustom, CloudifyConstants.DYNAMIC_BYON_START_MNG_MACHINES_KEY,
					managementMachineTemplateName);
			validateClosureExists(mngTemplateCustom, CloudifyConstants.DYNAMIC_BYON_STOP_MNG_MACHINES_KEY,
					managementMachineTemplateName);
			for (Entry<String, ComputeTemplate> templateEntry : entity.getCloudCompute().getTemplates().entrySet()) {
				final String templateName = templateEntry.getKey();
				Map<String, Object> templateCustom = templateEntry.getValue().getCustom();
				validateClosureExists(templateCustom, CloudifyConstants.DYNAMIC_BYON_START_MACHINE_KEY, templateName);
				validateClosureExists(templateCustom, CloudifyConstants.DYNAMIC_BYON_STOP_MACHINE_KEY, templateName);
			}
		}
	}

	private void validateClosureExists(final Map<String, Object> customMap, final String key,
			final String templateName) throws DSLValidationException {
		Object closure = customMap.get(key);
		if (closure == null) {
			throw new DSLValidationException("The " + key + " closure is missing in template "
					+ templateName + ".");
		}
	}

}
