package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.AgentComponent;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class AgentComponentValidator extends GridComponentValidator implements DSLValidator {

	private AgentComponent entity; 
	@Override
	public void setDSLEntity(Object dslEntity) {
		super.setDSLEntity(dslEntity);
		this.entity = (AgentComponent) dslEntity;
	}

	@DSLValidation
	public void validatePort(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validatePort(this.entity.getPort());
	}
	
	@DSLValidation
	public void validateMemory(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validateMemorySyntax();
	}
	
}
