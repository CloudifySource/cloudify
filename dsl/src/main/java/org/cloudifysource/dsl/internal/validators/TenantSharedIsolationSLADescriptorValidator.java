package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.TenantSharedIsolationSLADescriptor;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class TenantSharedIsolationSLADescriptorValidator extends SharedIsolationSLADescriptorValidator implements DSLValidator {

	private TenantSharedIsolationSLADescriptor entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		super.setDSLEntity(dslEntity);
		this.entity = (TenantSharedIsolationSLADescriptor) dslEntity;
	}
	
	@Override
	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {

		super.validateDefaultValues(validationContext);

		if (entity.isUseManagement()) {
			throw new DSLValidationException("isUseManagement can only be true for isolationSLA of type 'global'");
		}

	}

}
