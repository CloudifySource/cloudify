package org.cloudifysource.esc.driver.provisioning.validation;

/**
 * Types of validation results.
 *
 * @author noak
 * @since 2.6
 */
public enum ValidationResultType {
	
	/*********
	 * Indicates the validation ended successfully.
	 */
	OK,
	
	/*********
	 * Indicates the validation ended with a warning.
	 */
	WARNING,
	
	/*********
	 * Indicates the validation failed.
	 */
	ERROR
}
