/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.context;

import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;


/**
 * Writes validation messages to the shell and log.
 * @author noak
 * @since 2.6.0
 */
public interface ValidationContext {
	
	/**
	 * Publish a message to the shell and log it.
	 * The ValidationMessageType determines the message display: 
	 * TOP_LEVEL_VALIDATION_MESSAGE -standard display,
	 * GROUP_VALIDATION_MESSAGE - the message text will be prefixed with "- > ",
	 * ENTRY_VALIDATION_MESSAGE - the message text will be prefixed with "-    > "
	 *
	 * @param messageType
	 *           The type of the validation message (Top, Group, Entry).
	 * @param message
	 *            The message text
	 */
	void validationEvent(final ValidationMessageType messageType, final String message);
	
	/**
	 * Publish a message about an ongoing validation to the shell and log it.
	 * The ValidationMessageType determines the message display: 
	 * TOP_LEVEL_VALIDATION_MESSAGE -standard display,
	 * GROUP_VALIDATION_MESSAGE - the message text will be prefixed with "- > ",
	 * ENTRY_VALIDATION_MESSAGE - the message text will be prefixed with "-    > "
	 *
	 * @param messageType
	 *           The type of the validation message (Top, Group, Entry).
	 * @param message
	 *            The message text
	 */
	void validationOngoingEvent(final ValidationMessageType messageType, final String message);
	
	/**
	 * Publish the end result of an ongoing validation to the shell and log it.
	 * If the validation ended successfully - the message will be "[OK]", colored green.
	 * If the validation ended with a warning - the message will be "[WARNING]", colored yellow.
	 * if the validation failed - it will be "[ERROR]" colored red.
	 *
	 * @param validationResultType
	 *            indicates the validation end result (OK, Warning, Error)
	 */
	void validationEventEnd(final ValidationResultType validationResultType);
	
}
