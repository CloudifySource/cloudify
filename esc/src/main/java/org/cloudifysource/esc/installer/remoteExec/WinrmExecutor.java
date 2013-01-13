/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.esc.installer.remoteExec;

import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.installer.remoteExec.PowershellClient.PowerShellOutputListener;




/********
 * Remote Executor implementation for Windows Remote Management, using the
 * powershell command.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
public class WinrmExecutor implements RemoteExecutor {

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(WinrmExecutor.class.getName());

	private static final String[] POWERSHELL_INSTALLED_COMMAND = new String[] { "powershell.exe", "-inputformat",
		"none", "-?" };
	private static final String POWERSHELL_COMMAND_SEPARATOR = ";"; // System.getProperty("line.separator");
	private static final String SEPARATOR = POWERSHELL_COMMAND_SEPARATOR;
	private static final String CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX = "/[a-zA-Z][$]/.*";
	private static final String POWERSHELL_CLIENT_SCRIPT = "bootstrap-client.ps1";

	private final StringBuilder sb = new StringBuilder();
	private boolean runInBackground = false;
	private AgentlessInstaller installer;

	private static Pattern pattern;

	// indicates if powershell is installed on this host. If null, installation
	// test was not performed.
	private static volatile Boolean powerShellInstalled = null;

	@Override
	public void initialize(final AgentlessInstaller installer, final InstallationDetails details) {
		this.installer = installer;
	}

	/****************
	 * Given a path of the type /C$/PATH - indicating an absolute cifs path,
	 * returns /PATH. If the string does not match, returns the original
	 * unmodified string.
	 *
	 * @param str
	 *            the input path.
	 * @return the input path, adjusted to remove the cifs drive letter, if it
	 *         exists, or the original path if the drive letter is not present.
	 */
	public static String normalizeCifsPath(final String str) {
		final String expression = CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX;
		if (pattern == null) {
			pattern = Pattern.compile(expression);
		}

		if (pattern.matcher(str).matches()) {
			final char drive = str.charAt(1);
			return drive + ":\\" + str.substring("/c$/".length()).replace('/', '\\');
		}
		return str;
	}

	/*******
	 * Adds a command to the command line.
	 *
	 * @param str
	 *            the command to add.
	 * @return this.
	 */
	@Override
	public RemoteExecutor call(final String str) {
		final String normalizedPathCommand = normalizeCifsPath(str);
		sb.append(normalizedPathCommand);

		return this;
	}

	/********
	 * Adds a separator.
	 *
	 * @return this.
	 */
	@Override
	public RemoteExecutor separate() {
		sb.append(WinrmExecutor.SEPARATOR);
		return this;
	}

	/*********
	 * Adds an environment variable to the command line.
	 *
	 * @param name
	 *            variable name.
	 * @param value
	 *            variable value.
	 * @return this.
	 */
	@Override
	public RemoteExecutor exportVar(final String name, final String value) {

		String actualValue = value;
		if (value == null) {
			actualValue = "";
		}

		String normalizedValue = normalizeCifsPath(actualValue);
		if (actualValue.startsWith("\"") && actualValue.endsWith("\"")) {
			normalizedValue = actualValue.replace("\"", "'");
		} else {
			if (!(actualValue.startsWith("\"") && actualValue.endsWith("\""))) {
				normalizedValue = "'" + normalizedValue + "'";
			}
		}

		sb.append("$ENV:").append(name).append("=").append(normalizedValue);

		separate();
		return this;
	}

	@Override
	public RemoteExecutor chmodExecutable(final String path) {
		return this;
	}

	/*****
	 * Marks a command line to be executed in the background.
	 *
	 * @return this.
	 */
	@Override
	public RemoteExecutor runInBackground() {
		this.runInBackground = true;

		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	@Override
	public boolean isRunInBackground() {
		return runInBackground;
	}

	@Override
	public void execute(final String targetHost, final InstallationDetails details, final String scriptPath,
			final long endTimeMillis)
					throws InstallerException, TimeoutException, InterruptedException {

		final String fullCommand = normalizeCifsPath(scriptPath);

		final PowershellClient client = new PowershellClient();
		client.addOutputListener(new PowerShellOutputListener() {

			@Override
			public void onPowerShellOutput(final String line) {
				logger.info(line);

			}
		});
		try {
			client.invokeRemotePowershellCommand(targetHost, fullCommand, details.getUsername(), details.getPassword(),
					details.getLocalDir());
		} catch (final PowershellClientException e) {
			throw new InstallerException("Failed to execute powershell remote command", e);
		}

	}

}
