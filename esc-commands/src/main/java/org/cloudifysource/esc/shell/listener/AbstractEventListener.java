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
package org.cloudifysource.esc.shell.listener;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.cloudifysource.shell.ShellUtils;

public class AbstractEventListener {
	
	private final ResourceBundle messages = ShellUtils.getMessageBundle();
	private static final Logger logger = Logger.getLogger(AbstractEventListener.class.getName());
	
	/**
	 * Formats a message based on the given message name and arguments.
	 * @param msgName The name of the message
	 * @param arguments The arguments to embed in the message
	 * @return a formatted message with embedded arguments
	 */
	protected String getFormattedMessage(final String msgName, final Object... arguments) {
		if (messages == null) {
			logger.warning("Messages resource bundle was not initialized! Message: "
					+ msgName + " could not be displayed.");
			return msgName;
		}
		//TODO:Handle MissingResourceException
		String message = messages.getString(msgName);
		if (message == null) {
			logger.warning("Missing resource in messages resource bundle: "
					+ msgName);
			return msgName;
		}
		try {
			return MessageFormat.format(message, arguments);
		} catch (IllegalArgumentException e) {
			logger.warning("Failed to format message: " + msgName
					+ " with format: " + message + " and arguments: "
					+ Arrays.toString(arguments));
			return msgName;
		}
	}
}
