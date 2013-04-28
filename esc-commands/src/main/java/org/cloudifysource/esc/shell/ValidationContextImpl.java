/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.shell;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;
import org.cloudifysource.shell.ShellUtils;
import org.fusesource.jansi.Ansi.Color;

/**
 * Writes messages to the shell, implementing interface {@link ValidationContext}.
 * 
 * @author noak
 * @since 2.6.0
 */
public class ValidationContextImpl implements ValidationContext {

	private static final Logger logger = Logger.getLogger(ValidationContextImpl.class.getName());
	
	// TODO : What should be static here and what not? is there a reason to use more the 1 shellwriter in the system?
	// Maybe a singleton is best?
	
	
	@Override
	public void validationEvent(final ValidationMessageType messageType, final String message) {
		String formattedMessage = getIndentedMessage(messageType, message);
		System.out.println(formattedMessage);
		System.out.flush();
		logger.log(Level.FINE, formattedMessage);
	}
	

	@Override
	public void validationOngoingEvent(final ValidationMessageType messageType, final String message) {		
		String formattedMessage = getIndentedMessage(messageType, message);
		System.out.print(formattedMessage);
		System.out.flush();
		logger.log(Level.FINE, formattedMessage);
	}
	

	@Override
	public void validationEventEnd(final ValidationResultType validtionResultType) {
		String formattedMessage;
		Color messageColor;
		
		if (validtionResultType == ValidationResultType.OK) {
			messageColor = Color.GREEN;
			formattedMessage = ShellUtils.getFormattedMessage(CloudifyErrorMessages.ONGOING_EVENT_SUCCEEDED.getName());
		} else if (validtionResultType == ValidationResultType.WARNING) {
			messageColor = Color.YELLOW;
			formattedMessage = ShellUtils.getFormattedMessage(CloudifyErrorMessages.ONGOING_EVENT_WARNING.getName());
		} else {
			messageColor = Color.RED;
			formattedMessage = ShellUtils.getFormattedMessage(CloudifyErrorMessages.ONGOING_EVENT_FAILED.getName());
		}
		
		// A space is prepended to separate this message from the previous one, that is on the same line.
		formattedMessage = " " + formattedMessage;
		
		System.out.println(ShellUtils.getColorMessage(formattedMessage, messageColor));
		System.out.flush();
		logger.log(Level.FINE, formattedMessage);
	}
	
	
	/**
	 * Formats a message based on the given message name and arguments.
	 * @param message The validation message
	 * @param messageType The validationMessageType (Top, Group, Entry)
	 * @return a formatted message with the required indentation
	 */
	protected static String getIndentedMessage(final ValidationMessageType messageType, final String message) {
		String indentedMessage = message;
		
		if (ValidationMessageType.GROUP_VALIDATION_MESSAGE == messageType) {
			indentedMessage = "- > " + indentedMessage;
		} else if (ValidationMessageType.ENTRY_VALIDATION_MESSAGE == messageType) {
			indentedMessage = "-    > " + indentedMessage;
		}
		
		return message;
	}

}