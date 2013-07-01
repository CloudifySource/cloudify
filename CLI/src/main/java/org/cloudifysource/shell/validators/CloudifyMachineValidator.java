package org.cloudifysource.shell.validators;

import org.cloudifysource.shell.exceptions.CLIValidationException;

/**
 * Machine and connectivity validations called before installing a Cloudify management server or agent on the 
 * local machine.
 */
public interface CloudifyMachineValidator {

	/**
	 * Executes a specific validation on the local machine.
	 * 
	 * @throws CLIValidationException Indicates invalid configuration or a connectivity problem.
	 */
	void validate() throws CLIValidationException;
}
