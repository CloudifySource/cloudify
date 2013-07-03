package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.OrchestratorComponent;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class OrchestratorComponentValidator extends GridComponentValidator implements DSLValidator {

	private OrchestratorComponent entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		super.setDSLEntity(dslEntity);
		this.entity = (OrchestratorComponent) dslEntity;
	}

	@DSLValidation
	void validatePort(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validatePort(this.entity.getPort());
	}
	
	@DSLValidation
	void validateMemory(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validateMemorySyntax();
	}
}
