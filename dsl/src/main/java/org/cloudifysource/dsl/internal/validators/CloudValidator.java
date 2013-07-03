package org.cloudifysource.dsl.internal.validators;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudConfiguration;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

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
