package org.cloudifysource.esc.shell.listner;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.cloudifysource.shell.ShellUtils;

public class AbstractEventListener {
	
	private ResourceBundle messages = ShellUtils.getMessageBundle();
	private Logger logger = Logger.getLogger(AbstractEventListener.class.getName());
	
	protected String getFormattedMessage(String msgName, Object... arguments) {
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
