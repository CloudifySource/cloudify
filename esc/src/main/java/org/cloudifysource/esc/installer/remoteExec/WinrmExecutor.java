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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.taskdefs.optional.testing.BuildTimeoutException;
import org.cloudifysource.dsl.cloud.RemoteExecutionModes;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.util.Utils;

public class WinrmExecutor implements RemoteExecutor {

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(SshExecutor.class.getName());

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

	// indicates if powershell is installed on this host. If null, installation test was not performed.
	private static volatile Boolean powerShellInstalled = null;

	@Override
	public void initialize(final AgentlessInstaller installer, final InstallationDetails details) {
		this.installer = installer;
	}

	/****************
	 * Given a path of the type /C$/PATH - indicating an absolute cifs path, returns /PATH. If the string does not
	 * match, returns the original unmodified string.
	 * 
	 * @param str the input path.
	 * @return the input path, adjusted to remove the cifs drive letter, if it exists, or the original path if the drive
	 *         letter is not present.
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
	 * @param str the command to add.
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
		sb.append(this.SEPARATOR);
		return this;
	}

	/*********
	 * Adds an environment variable to the command line.
	 * 
	 * @param name variable name.
	 * @param value variable value.
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
	public void execute(final InstallationDetails details, final String command,
			final long endTimeMillis)
			throws InstallerException, TimeoutException {

		String host = null;
		if (details.isConnectedToPrivateIp()) {
			host = details.getPrivateIp();
		} else {
			host = details.getPublicIp();
		}

		try {
			Utils.executeSSHCommand(host, command, details.getUsername(), details.getPassword(), details.getKeyFile(),
					endTimeMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		} catch (final BuildException e) {
			// There really should be a better way to check that this is a
			// timeout
			logger.log(Level.FINE, "The remote boostrap command failed with error: " + e.getMessage()
					+ ". The command that failed to execute is : " + command, e);

			if (e instanceof BuildTimeoutException) {
				final TimeoutException ex =
						new TimeoutException("Remote bootstrap command failed to execute: " + e.getMessage());
				ex.initCause(e);
				throw ex;
			} else if (e instanceof ExitStatusException) {
				final ExitStatusException ex = (ExitStatusException) e;
				final int ec = ex.getStatus();
				throw new InstallerException("Remote bootstrap command failed with exit code: " + ec, e);
			} else {
				throw new InstallerException("Remote bootstrap command failed to execute.", e);
			}
		}

	}

	private void powershellCommand(final String targetHost, final String command, final String username,
			final String password, final String keyFile, final long millisUntil, final TimeUnit milliseconds,
			final String localDir)
			throws InstallerException, InterruptedException, TimeoutException {
		logger.fine("Executing: " + command + " on: " + targetHost);

		logger.fine("Checking if powershell is installed");
		try {
			checkPowershellInstalled();
		} catch (final IOException e) {
			throw new InstallerException("Error while trying to find powershell.exe", e);
		}

		logger.fine("Checking WinRM Connection");
		AgentlessInstaller.checkConnection(targetHost, RemoteExecutionModes.WINRM.getPort(), millisUntil, milliseconds);

		logger.fine("Executing remote command");
		try {
			invokeRemotePowershellCommand(targetHost, command, username, password, localDir);
		} catch (final FileNotFoundException e) {
			throw new InstallerException("Failed to invoke remote powershell command", e);
		}

	}

	private String invokeRemotePowershellCommand(final String targetHost, final String command, final String username,
			final String password, final String localDir)
			throws InstallerException, InterruptedException, FileNotFoundException {

		final List<String> fullCommand = getPowershellCommandLine(targetHost, username, password, command, localDir);

		final ProcessBuilder pb = new ProcessBuilder(fullCommand);
		pb.redirectErrorStream(true);

		try {
			final Process p = pb.start();

			final String output = readProcessOutput(p);
			final int exitCode = p.waitFor();
			if (exitCode != 0) {
				throw new InstallerException("Remote installation failed with exit code: " + exitCode
						+ ". Execution output: " + output);
			}
			return output;
		} catch (final IOException e) {
			throw new InstallerException("Failed to invoke remote installation: " + e.getMessage(), e);
		}
	}

	private void checkPowershellInstalled()
			throws IOException, InterruptedException, InstallerException {
		if (powerShellInstalled != null) {
			if (powerShellInstalled.booleanValue()) {
				return;
			}
			throw new InstallerException(
					"powershell.exe is not on installed, or is not available on the system path. "
					+ "Powershell is required on both client and server for Cloudify to work on Windows. ");
		}

		logger.fine("Checking if powershell is installed using: " + Arrays.toString(POWERSHELL_INSTALLED_COMMAND));
		final ProcessBuilder pb = new ProcessBuilder(Arrays.asList(POWERSHELL_INSTALLED_COMMAND));
		pb.redirectErrorStream(true);

		final Process p = pb.start();

		final String output = readProcessOutput(p);

		logger.fine("Finished reading output");
		final int retval = p.waitFor();
		logger.fine("Powershell installed command exit value: " + retval);
		if (retval != 0) {
			throw new InstallerException("powershell.exe is not on installed, or is not available on the system path. "
					+ "Powershell is required on both client and server for Cloudify to work on Windows. "
					+ "Execution result: " + output);
		}

		powerShellInstalled = Boolean.TRUE;
	}

	private String readProcessOutput(final Process p)
			throws IOException {
		final StringBuilder sb = new StringBuilder();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		try {
			final String newline = System.getProperty("line.separator");
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				installer.publishEvent("powershell_output_line", line);
				logger.fine(line);
				sb.append(line).append(newline);
			}

		} finally {
			try {
				reader.close();
			} catch (final IOException e) {
				logger.log(Level.SEVERE, "Error while closingprocess input stream: " + e.getMessage(), e);

			}

		}
		return sb.toString();
	}

	private List<String> getPowershellCommandLine(final String target, final String username, final String password,
			final String command, final String localDir)
			throws FileNotFoundException {

		final File clientScriptFile = new File(localDir, POWERSHELL_CLIENT_SCRIPT);
		if (!clientScriptFile.exists()) {
			throw new FileNotFoundException(
					"Could not find expected powershell client script in local directory. Was expecting file: "
							+ clientScriptFile.getAbsolutePath());
		}
		final String[] commandLineParts =
		{ "powershell.exe", "-inputformat", "none", "-File", clientScriptFile.getAbsolutePath(), "-target",
				target, "-password", quoteString(password), "-username", quoteString(username), "-command",
				quoteString(command) };

		return Arrays.asList(commandLineParts);
	}

	private String quoteString(final String input) {
		return "\"" + input + "\"";
	}

}
