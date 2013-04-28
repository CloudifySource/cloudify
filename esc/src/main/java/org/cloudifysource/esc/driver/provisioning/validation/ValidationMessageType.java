package org.cloudifysource.esc.driver.provisioning.validation;

/**
 * Types of validation messages, determines the message display (indentation level).
 *
 * @author noak
 * @since 2.6
 */
public enum ValidationMessageType {

	/*********
	 * Top level validation message, printed without indentation.
	 */
	TOP_LEVEL_VALIDATION_MESSAGE,
	
	/*********
	 * Group message, printed with a small indentation.
	 */
	GROUP_VALIDATION_MESSAGE,
	
	/*********
	 * Entry validation message, printed with a large indentation.
	 */
	ENTRY_VALIDATION_MESSAGE	
}
