package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.WebuiComponent;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class WebuiComponentValidator extends GridComponentValidator implements DSLValidator {

	private WebuiComponent entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (WebuiComponent) dslEntity;
	}
	//Important: this is a special case where if the port is not initialized, we initialize it here. 
	@DSLValidation
	void validatePort(final DSLValidationContext validationContext) throws DSLValidationException {
		if (entity.getPort() == null) {
			entity.setPort(CloudifyConstants.DEFAULT_WEBUI_PORT);
		}
		super.validatePort(this.entity.getPort());
	}
	
	@DSLValidation
	void validateMemory(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validateMemorySyntax();
	}

}
