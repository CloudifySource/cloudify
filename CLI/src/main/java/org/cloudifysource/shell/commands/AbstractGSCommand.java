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
package org.cloudifysource.shell.commands;

import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.CloseShellException;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, barakm
 * @since 2.0.0
 *        <p/>
 *        Abstract implementation of the
 *        {@link org.apache.felix.gogo.commands.Action} interface. This
 *        abstraction can be extended by Cloudify aware commands to get support
 *        for: - Formatted messages from the message bundle - Logging (with
 *        verbose mode - true/false) - AdminFacade such as the RestAdminFacade,
 *        when required by the command (AKA admin-aware commands).
 */
public abstract class AbstractGSCommand implements Action {

	protected static final Logger logger = Logger.getLogger(AbstractGSCommand.class.getName());

	@Option(required = false, name = "--verbose", description = "show detailed execution result including exception"
			+ " stack trace")
	protected boolean verbose;
	protected CommandSession session;
	protected ResourceBundle messages;
	protected boolean adminAware = false;
	protected AdminFacade adminFacade;

	/**
	 * Initializes the messages bundle, and takes the admin facade objects from
	 * the session when command is admin-aware. Calls doExecute (must be
	 * implemented separately in the extending classes).
	 * 
	 * @param session
	 *            The command session to be used.
	 * @return Object The object returned from the call to doExecute
	 * @throws Exception
	 *             Reporting a failure to execute this command
	 */
	@Override
	public Object execute(final CommandSession session)
			throws Exception {
		this.session = session;
		messages = ShellUtils.getMessageBundle();
		try {
			if (adminAware) {
				adminFacade = (AdminFacade) session.get(Constants.ADMIN_FACADE);

				if (!adminFacade.isConnected()) {
					throw new CLIStatusException("not_connected");
				}
			}
			final Object result = doExecute();
			return result;

		} catch (final CLIStatusException cse) {
			if (verbose) {
				if (cse.getVerboseData() == null) {
					logger.log(
							Level.WARNING, getFormattedMessageFromErrorStatusException(cse), cse);
				} else {
					logger.log(
							Level.WARNING, getFormattedMessageFromErrorStatusException(cse, cse.getVerboseData()));
				}
			} else {
				logger.log(
						Level.WARNING, getFormattedMessageFromErrorStatusException(cse));
			}
			raiseCloseShellExceptionIfNonInteractive(
					session, cse);
		} catch (final CLIException e) {
			if (!verbose) {
				e.setStackTrace(new StackTraceElement[] {});
			}
			logger.log(
					Level.WARNING, "", e);
			raiseCloseShellExceptionIfNonInteractive(
					session, e);
		} catch (final InterruptedException e) {
			final String msg = messages.getString("command_interrupted");
			if (verbose) {
				logger.log(
						Level.SEVERE, msg, e);
			} else {
				logger.log(
						Level.SEVERE, msg);
			}
			raiseCloseShellExceptionIfNonInteractive(
					session, e);
		} catch (final Throwable e) {
			if (verbose) {
				logger.log(
						Level.SEVERE, "", e);
			} else {
				logger.log(
						Level.SEVERE, e.getMessage());
			}
			raiseCloseShellExceptionIfNonInteractive(
					session, e);
		}
		return getFormattedMessage(
				"op_failed", Color.RED, "");
	}

	private String getFormattedMessageFromErrorStatusException(final CLIStatusException e) {
		return getFormattedMessageFromErrorStatusException(e, null);
	}

	/**
	 * Gets a formatted message from the given CLIStatusException, using the
	 * exception's reason code as the message name and the exception's args
	 * field, if not null.
	 * 
	 * @param e
	 *            The CLIStatusException to base on
	 * @return The formatted message
	 */
	private String getFormattedMessageFromErrorStatusException(final CLIStatusException e, final String verboseData) {
		String message = getFormattedMessage(e.getReasonCode(), e.getArgs());
		if (message == null) {
			message = e.getReasonCode();
		}
		
		if (verboseData != null) {
			return message + " : " + verboseData;
		} else {
			return message;
		}
	}


	// private String
	// getFormattedMessageFromErrorStatusException(ErrorStatusException e,
	// Color color) {
	// String message = getFormattedMessageFromErrorStatusException(e);
	// return ShellUtils.getColorMessage(message, color);
	// }

	/**
	 * If not using the CLI in interactive mode - the method adds the given
	 * throwable to the session and throws a CloseShellException.
	 * 
	 * @param session
	 *            current command session
	 * @param t
	 *            The throwable to add to the session
	 * @throws CloseShellException
	 *             Indicates the console to close.
	 */
	private static void raiseCloseShellExceptionIfNonInteractive(final CommandSession session, final Throwable t)
			throws CloseShellException {
		if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
			session.put(
					Constants.LAST_COMMAND_EXCEPTION, t);
			throw new CloseShellException();
		}
	}

	/**
	 * Gets the name of the current application.
	 * 
	 * @return Name of the current application, as a String.
	 */
	protected final String getCurrentApplicationName() {
		if (session == null) {
			return null;
		}

		return (String) session.get(Constants.ACTIVE_APP);
	}

	/**
	 * Gets a message from the message bundle, without argument. If the message
	 * cannot be retrieved the message name is returned and the failure is
	 * logged.
	 * 
	 * @param msgName
	 *            the message name used to retrieve from the message bundle
	 * @return formatted message as a String
	 */
	protected final String getFormattedMessage(final String msgName) {
		return getFormattedMessage(
				msgName, new Object[0]);
	}

	/**
	 * Gets a message from the message bundle, embedded with the given arguments
	 * if supplied. If the message cannot be retrieved the message name is
	 * returned and the failure is logged.
	 * 
	 * @param msgName
	 *            the message name used to retrieve from the message bundle
	 * @param color
	 *            The color to be used when displaying the message in the
	 *            console
	 * @param arguments
	 *            arguments to embed in the message text
	 * @return formatted message as a String
	 */
	protected final String getFormattedMessage(final String msgName, final Color color, final Object... arguments) {
		final String outputMessage = getFormattedMessage(
				msgName, arguments);
		return ShellUtils.getColorMessage(
				outputMessage, color);
	}

	/**
	 * Gets a message from the message bundle, embedded with the given arguments
	 * if supplied. If the message cannot be retrieved the message name is
	 * returned and the failure is logged.
	 * 
	 * @param msgName
	 *            the message name used to retrieve from the message bundle
	 * @param arguments
	 *            arguments to embed in the message text
	 * @return formatted message as a String
	 */
	protected final String getFormattedMessage(final String msgName, final Object... arguments) {
		return ShellUtils.getFormattedMessage(msgName, arguments);
	}

	/**
	 * The main method to be implemented by all extending classes.
	 * 
	 * @return Object Return value from the executed command
	 * @throws Exception
	 *             Reporting a failure to execute this command
	 */
	protected abstract Object doExecute()
			throws Exception;

	/**
	 * Creates Properties object for the given service, with value for:
	 * com.gs.application.depends com.gs.service.type com.gs.service.icon
	 * com.gs.service.network.protocolDescription
	 * <p/>
	 * in case the above properties are not null.
	 * 
	 * @param serviceNamesString
	 *            Service name
	 * @param service
	 *            Service object to use for the properties creation
	 * @return Properties object
	 */
	protected final Properties createServiceContextProperties(final String serviceNamesString, final Service service) {
		final Properties contextProperties = new Properties();

		// contextProperties.setProperty("com.gs.application.services",
		// serviceNamesString);
		if (service.getDependsOn() != null) {
			contextProperties.setProperty(
					"com.gs.application.depends", service.getDependsOn().toString());
		}
		if (service.getType() != null) {
			contextProperties.setProperty(
					"com.gs.service.type", service.getType());
		}
		if (service.getIcon() != null) {
			contextProperties.setProperty(
					"com.gs.service.icon", service.getIcon());
		}
		if (service.getNetwork() != null) {
			contextProperties.setProperty(
					"com.gs.service.network.protocolDescription", service.getNetwork().getProtocolDescription());
		}
		return contextProperties;
	}

	/**
	 * Gets the restAdminFacade.
	 * 
	 * @return The AdminFacade object used by rest commands
	 */
	protected AdminFacade getRestAdminFacade() {
		return (AdminFacade) session.get(Constants.ADMIN_FACADE);
	}
}
