package org.cloudifysource.rest.validators;

import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * An interface for uninstall-service validator classes. Each validator must implement the validate method.
 * 
 * @author noak
 */
public interface UninstallServiceValidator {
	
	/**
	 * @param validationContext .
	 * @throws org.cloudifysource.rest.controllers.RestErrorException .
	 */
	void validate(final UninstallServiceValidationContext validationContext) throws RestErrorException;
}
