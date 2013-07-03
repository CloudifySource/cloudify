package org.cloudifysource.dsl.internal.validators;

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.CloudUser;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class CloudUserValidator implements DSLValidator {

	private CloudUser entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (CloudUser) dslEntity;
	}
	
	@DSLValidation
	public void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if ("ENTER_USER".equals(this.entity.getUser())) {
			throw new DSLValidationException("User field still has default configuration value of ENTER_USER");
		}

		if ("ENTER_KEY".equals(this.entity.getApiKey())) {
			throw new DSLValidationException("Key field still has default configuration value of ENTER_KEY");
		}
	}
}
