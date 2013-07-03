package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.DiscoveryComponent;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class DiscoveryComponentValidator extends GridComponentValidator implements DSLValidator {

	private DiscoveryComponent entity;

	@Override
	public void setDSLEntity(final Object dslEntity) {
		super.setDSLEntity(dslEntity);
		this.entity = (DiscoveryComponent) dslEntity;
	}
	
	@DSLValidation
	void validatePorts(final DSLValidationContext validationContext) throws DSLValidationException {
		if (this.entity.getDiscoveryPort() == null) {
			entity.setDiscoveryPort(CloudifyConstants.DEFAULT_LUS_PORT);
		}
		super.validatePort(this.entity.getPort());
		super.validatePort(this.entity.getDiscoveryPort());
	}
	
	@DSLValidation
	void validateMemory(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validateMemorySyntax();
	}
}
