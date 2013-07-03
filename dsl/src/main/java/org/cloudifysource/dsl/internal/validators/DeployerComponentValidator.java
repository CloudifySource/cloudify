package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.DeployerComponent;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class DeployerComponentValidator extends GridComponentValidator implements DSLValidator {

	private DeployerComponent entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		super.setDSLEntity(dslEntity);
		this.entity = (DeployerComponent) dslEntity;
	}
	
	@DSLValidation
	void validatePorts(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validatePort(this.entity
				.getPort());
		super.validatePort(this.entity.getWebsterPort());
	}
	
	@DSLValidation
	void validateMemory(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validateMemorySyntax();
	}

}
