package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.ServiceNetwork;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class ServiceNetworkValidator implements DSLValidator {

	private ServiceNetwork entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (ServiceNetwork) dslEntity;
	}
	
	@DSLValidation
	public void checkPortValue(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.entity.getPort() <= 0) {
			throw new DSLValidationException("The port value of the network block must be a positive integer.");
		}
	}

	@DSLValidation
	public void checkDescription(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.entity.getProtocolDescription() == null) {
			throw new DSLValidationException("The protocol description can't be an empty value");
		}

		
	}

}
