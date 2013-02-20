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
package org.cloudifysource.shell;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Properties;

import jline.Terminal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.karaf.shell.console.jline.Console;
import org.cloudifysource.shell.rest.RestAdminFacade;

/**
 * @author uri
 * @since 2.0.0
 *        <p/>
 *        Extends the karaf framework Console Object. This class adds some
 *        branding and functionality on top of the base class, e.g., adds a
 *        default adminFacade to the session, overrides the get prompt method,
 *        set a default application and more.
 */
public class ConsoleWithProps extends Console {

	private static final String DEFAULT_APP_NAME = "default";

	private String currentAppName = DEFAULT_APP_NAME;
	private final ConsoleWithPropsActions consoleActions;

	ConsoleWithProps(final CommandProcessor commandProcessor, final InputStream input, final PrintStream output,
			final PrintStream err, final Terminal terminal, final CloseCallback callback, final boolean isInteractive)
			throws Exception {
		// super(commandProcessor, input, output, err, terminal, null, callback);
		super(commandProcessor, input, output, err, terminal, callback);

		consoleActions = isInteractive ? new ConsoleWithPropsInteractive() : new ConsoleWithPropsNonInteractive();

		callback.setSession(session);
		// TODO choose default admin or make it configurable
		final AdminFacade adminFacade = new RestAdminFacade();
		session.put(Constants.ADMIN_FACADE, adminFacade);
		session.put(Constants.RECIPES, new HashMap<String, File>());
		session.put(Constants.ACTIVE_APP, DEFAULT_APP_NAME);
		session.put(Constants.INTERACTIVE_MODE, isInteractive);
	}

	@Override
	protected void welcome() {
		final Properties props = loadBrandingProperties();
		final String welcome = props.getProperty("welcome");

		if (welcome != null && welcome.length() > 0) {
			session.getConsole().println(welcome);
		}
		if (ShellUtils.shouldDoVersionCheck(session)) {
			session.getConsole().println("Checking version...");
			ShellUtils.doVersionCheck(session);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPrompt() {
		return consoleActions.getPromptInternal(currentAppName);
	}

	/**
	 * Sets the application name.
	 * 
	 * @param currentAppName
	 *            The application name to set
	 */
	public void setCurrentApplicationName(final String currentAppName) {
		this.currentAppName = currentAppName;
	}

	/**
	 * Gets the application name.
	 * 
	 * @return The current application name
	 */
	public String getCurrentApplicationName() {
		return currentAppName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setSessionProperties() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Properties loadBrandingProperties() {
		final Properties props = new Properties();
		loadProps(props, consoleActions.getBrandingPropertiesResourcePath());
		return props;
	}

//	protected static void loadProps(Properties props, String resource) {
//		InputStream is = null;
//		try {
//			is = Branding.class.getClassLoader().getResourceAsStream(resource);
//			if (is != null) {
//				props.load(is);
//			}
//		} catch (IOException e) {
//			// ignore
//		} finally {
//			if (is != null) {
//				try {
//					is.close();
//				} catch (IOException e) {
//					// Ignore
//				}
//			}
//		}
//	}
}