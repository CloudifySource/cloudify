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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.SequenceInputStream;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.Terminal;
import jline.console.ConsoleReader;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.Main;
import org.apache.karaf.shell.console.jline.Console;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.logging.ShellErrorManager;
import org.cloudifysource.shell.logging.ShellFormatter;
import org.cloudifysource.shell.proxy.SystemDefaultProxySelector;
import org.fusesource.jansi.Ansi;

/**
 * @author rafi, barakm, adaml, noak
 * @since 2.0.0
 * 
 *        Extends the Karaf framework Main object. This class is used to start the shell/console.
 */
// declared as command so that it can be used in the context of another shell
@Command(name = "cloudify", scope = "cloudify", description = "Executes a cloudify command interpreter")
public final class GigaShellMain extends Main implements Action {

	private static final String EXIT_COMMAND = "exit\n";
	private static GigaShellMain instance;
	private ConsoleWithProps console;
	private final boolean isInteractive;

	@Argument(name = "args", description = "Cloudify sub command arguments", multiValued = true)
	private String[] args;

	/**
	 * This is the shell's main method. It starts the shell, sets the logging configurations, and the proxy if
	 * configured. Arguments, if passed, are expected in 1 of these 2 formats: -f <file_name> OR <command 1>;<command
	 * 2>;<command 3>;.... Passing commands set the interactive mode off.
	 * 
	 * @param args The commands to be executed, either in a file or as a list.
	 * @throws Exception Reporting a failure to start the shell or execute the commands
	 */
	public static void main(final String[] args)
			throws Exception {
		String[] actualArgs = args;
		initializeLogConfiguration();

		initializeProxyConfiguration();
		InputStream is = null;
		SequenceInputStream sis = null;
		final InputStream exitInputStream = new ByteArrayInputStream(EXIT_COMMAND.getBytes());
		boolean isInteractive = true;

		try {
			if (args.length > 0) {

				isInteractive = false;

				if (args[0].startsWith("-f=")) {
					final String filename = args[0].substring("-f=".length());
					final File file = new File(filename);
					if (!file.exists()) {
						throw new IllegalArgumentException(filename + " does not exist");
					}

					is = new FileInputStream(filename);
				} else {
					String commandString = "";
                    for (String arg : args) {
                        commandString = commandString.concat(arg + " ");
                    }
					if (!commandString.endsWith(";")) {
						commandString = commandString.concat(";");
					}
					commandString = commandString.replace(";", System.getProperty("line.separator"));

					is = new ByteArrayInputStream(commandString.getBytes("UTF-8"));
				}

				sis = new SequenceInputStream(is, exitInputStream);
				System.setIn(sis);
				actualArgs = new String[0];

			}

			instance = new GigaShellMain(isInteractive);
			instance.setApplication("cloudify");
			Ansi.ansi();
			
			instance.run(actualArgs);
		} finally {
			if (is != null) {
				is.close();
			}
			if (sis != null) {
				sis.close();
			}
			exitInputStream.close();
		}
	}

	private GigaShellMain(final boolean isInteractive) {
		this.isInteractive = isInteractive;
	}

	private static void initializeLogConfiguration() {

		// Replace the console Handler's formatter.
		final Handler[] handlers = Logger.getLogger("").getHandlers();
		for (final Handler handler : handlers) {
			if (handler.getClass().getName().equals(ConsoleHandler.class.getName())) {
				handler.setFormatter(new ShellFormatter());
				handler.setErrorManager(new ShellErrorManager());
				handler.setLevel(Level.INFO);
				break;
			}
		}
	}

	private static void initializeProxyConfiguration() {

		// SystemDefaultProxySelector has not been tested on different platforms
		if (!ShellUtils.isWindows()) {
			return;
		}

		if (!Boolean.getBoolean(Constants.ENABLE_PROXY_CONFIGURATION_PROPERTY)) {
			return;
		}

		if (isProxySettingsDefined()) {
			return;
		}

		try {
			SystemDefaultProxySelector.setup();
		} catch (final Exception e) {
			Logger.getLogger(GigaShellMain.class.getName()).log(Level.WARNING,
					"Failed using system proxy configuration, falling back to no proxy.", new CLIException(e));
		}

	}

	// TODO this is not very robust and needs much more work to get done right
	private static boolean isProxySettingsDefined() {
		try {
			final URI someURI = new URI("http://www.example.com");
			final ProxySelector defaultSelector = ProxySelector.getDefault();
			final List<Proxy> proxies = defaultSelector.select(someURI);
			return !proxies.isEmpty() && !proxies.get(0).equals(Proxy.NO_PROXY);
		} catch (final URISyntaxException e) {
			// Will not happen
			return false;
		}
	}

	/**
	 * Gets the single instance of this class.
	 * 
	 * @return the single instance of this class
	 */
	public static GigaShellMain getInstance() {
		return instance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object execute(final CommandSession session)
			throws Exception {
		run(session, args);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Console createConsole(final CommandProcessorImpl commandProcessor, final InputStream input,
			final PrintStream output, final PrintStream err, final Terminal terminal)
			throws Exception {
		// Disable PC speaker beep
		System.setProperty(ConsoleReader.JLINE_NOBELL, Boolean.toString(true));
		final CloseCallback callback = new CloseCallback();
		console = new ConsoleWithProps(commandProcessor, input, output, err, terminal, callback, isInteractive);
		return console;
	}

	/**
	 * Sets the current application name.
	 * 
	 * @param applicationName The application name
	 */
	public void setCurrentApplicationName(final String applicationName) {
		console.setCurrentApplicationName(applicationName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDiscoveryResource() {
		return "META-INF/shell/commands";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isMultiScopeMode() {
		return false;
	}
}
