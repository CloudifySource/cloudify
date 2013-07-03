package org.cloudifysource.dsl.internal.validators;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

public class ApplicationValidator implements DSLValidator {

	private Application application;
	
	
	@Override
	public void setDSLEntity(final Object dslEntity) {
		this.application = (Application) dslEntity;
	}
	
	/**
	 * Validates that the name property exists and is not empty or invalid.
	 * @param validationContext
	 * @throws DSLValidationException
	 */
	@DSLValidation
	public void validateName(final DSLValidationContext validationContext) 
			throws DSLValidationException {
		if (StringUtils.isBlank(application.getName())) {
			throw new DSLValidationException("Application.validateName: The application's name " 
					+ (application.getName() == null ? "is missing" : "is empty"));
		}
		DSLUtils.validateRecipeName(application.getName());
	}

}
