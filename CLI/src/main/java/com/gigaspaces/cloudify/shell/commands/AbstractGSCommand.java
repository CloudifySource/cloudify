/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.gigaspaces.cloudify.shell.commands;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.CloseShellException;
import org.fusesource.jansi.Ansi.Color;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.shell.AdminFacade;
import com.gigaspaces.cloudify.shell.Constants;
import com.gigaspaces.cloudify.shell.ShellUtils;

/**
 * @author rafi
 * @since 8.0.3
 */
public abstract class AbstractGSCommand implements Action {

	protected static final Logger logger = Logger
			.getLogger(AbstractGSCommand.class.getName());
	//Used for CompleterValues methods that don't have access to the sessions admin facade
	private static AdminFacade restAdminFacade;

	@Option(required = false, name = "--verbose", description = "show detailed execution result including exception stack trace")
	protected boolean verbose;
	protected CommandSession session;
	protected ResourceBundle messages;
	protected boolean adminAware = false;
	protected AdminFacade adminFacade;

	@Override
	public Object execute(CommandSession session) throws Exception {
		this.session = session;
		messages = ShellUtils.getMessageBundle();
		try {
			if (adminAware) {
				adminFacade = (AdminFacade) session.get(Constants.ADMIN_FACADE);

				if (!adminFacade.isConnected()) {
				    throw new CLIStatusException("not_connected");
				}
			}
			Object result = doExecute();
			return result;
		
		} catch (CLIStatusException cse) {
			if (verbose) {
				logger.log(Level.WARNING, getFormattedMessageFromErrorStatusException(cse), cse);
			}
			else {
				logger.log(Level.WARNING, getFormattedMessageFromErrorStatusException(cse));
			}
			raiseCloseShellExceptionIfNonInteractive(session, cse);
		} catch (CLIException e) {
			if (!verbose) {
				e.setStackTrace(new StackTraceElement[] {});
			}
			logger.log(Level.WARNING, "",e);
			raiseCloseShellExceptionIfNonInteractive(session, e);
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "", e);
		    raiseCloseShellExceptionIfNonInteractive(session, e);			
		}
		return getFormattedMessage("op_failed", Color.RED, "");
	}

	private String getFormattedMessageFromErrorStatusException(CLIStatusException e) {
		String message = getFormattedMessage(e.getReasonCode(),(Object[]) null);
		if (message == null) {
			message = e.getReasonCode();
		}

		if (e.getArgs() == null || e.getArgs().length == 0) {
			return message;
		} else {
			return MessageFormat.format(message, e.getArgs());
		}
	}
	
//	private String getFormattedMessageFromErrorStatusException(ErrorStatusException e, Color color) {
//		String message = getFormattedMessageFromErrorStatusException(e);
//		return ShellUtils.getColorMessage(message, color);
//	}
	
	private static void raiseCloseShellExceptionIfNonInteractive(CommandSession session, Throwable t) throws CloseShellException {
	    if (!(Boolean)(session.get(Constants.INTERACTIVE_MODE))) {
            session.put(Constants.LAST_COMMAND_EXCEPTION, t);
            throw new CloseShellException();	        
	    }
	}
	
	protected String getCurrentApplicationName() {
		if(session == null) {
			return null;
		}
		
		return (String) session.get(Constants.ACTIVE_APP);
	}

	protected String getFormattedMessage(String msgName) {
		return getFormattedMessage(msgName, new Object[0]);
	}
	
	protected String getFormattedMessage(String msgName, Color color, Object... arguments){
		String outputMessage = getFormattedMessage(msgName, arguments);
		return ShellUtils.getColorMessage(outputMessage, color); 
	}

	protected String getFormattedMessage(String msgName, Object... arguments) {
		if (messages == null) {
			logger.warning("Messages resource bundle was not initialized! Message: "
					+ msgName + " could not be displayed.");
			return msgName;
		}
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

	protected abstract Object doExecute() throws Exception;

	protected Properties createServiceContextProperties(
			final String serviceNamesString, final Service service) {
		final Properties contextProperties = new Properties();

		// contextProperties.setProperty("com.gs.application.services",
		// serviceNamesString);
		if (service.getDependsOn() != null) {
			contextProperties.setProperty("com.gs.application.depends", service
					.getDependsOn().toString());
		}
		if (service.getType() != null) {
			contextProperties.setProperty("com.gs.service.type",
					service.getType());
		}
		if (service.getIcon() != null) {
			contextProperties.setProperty("com.gs.service.icon",
					service.getIcon());
		}
		if (service.getNetwork() != null) {
			contextProperties.setProperty(
					"com.gs.service.network.protocolDescription", service
							.getNetwork().getProtocolDescription());
		}
		return contextProperties;
	}

	public static void setRestAdminFacade(AdminFacade restAdminFacade) {
		AbstractGSCommand.restAdminFacade = restAdminFacade;
	}

	public static AdminFacade getRestAdminFacade() {
		return restAdminFacade;
	}
}
