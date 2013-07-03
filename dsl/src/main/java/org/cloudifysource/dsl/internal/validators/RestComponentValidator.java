package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.RestComponent;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class RestComponentValidator extends GridComponentValidator implements DSLValidator {

	private RestComponent entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		super.setDSLEntity(dslEntity);
		this.entity = (RestComponent) dslEntity;
	}

	//Important: this is a special case where if the port is not initialized, we initialize it here. 
	@DSLValidation
	void validatePort(final DSLValidationContext validationContext) throws DSLValidationException {
		if (entity.getPort() == null) {
			entity.setPort(CloudifyConstants.DEFAULT_REST_PORT);
		}
		super.validatePort(this.entity.getPort());
	}
	
	@DSLValidation
	void validateMemory(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validateMemorySyntax();
	}
}
