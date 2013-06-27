package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.AppSharedIsolationSLADescriptor;
import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;


public class AppSharedIsolationSLADescriptorValidator  implements DSLValidator<AppSharedIsolationSLADescriptor>  {

	private AppSharedIsolationSLADescriptor entity;

	@Override
	public void setDSLEntity(AppSharedIsolationSLADescriptor dslEntity) {
		this.entity = dslEntity;
	}
	
	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {

		super.validateDefaultValues(validationContext);
		
		if (isUseManagement()) {
			throw new DSLValidationException("isUseManagement can only be true for isolationSLA of type 'global'");
		}
		
	}

}
