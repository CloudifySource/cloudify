package org.cloudifysource.esc.installer.remoteExec;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/*********
 * Wrapper for command line calls to Windows powershell.
 * 
 * @author barakme
 * @since 2.5.0
 * 
 */
public class PowershellClient {

	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(PowershellClient.class
			.getName());

	/*******
	 * Interface for clients that want to get the powershell output line-by-line
	 * rather then wait for the process to terminate and only then get the
	 * entire output.
	 * 
	 * @author barakme
	 * 
	 */
	public interface PowerShellOutputListener {
		/******
		 * Called when a new line is read from the powershell output.
		 * 
		 * @param line
		 *            the new line.
		 */
		void onPowerShellOutput(final String line);
	}

	private final List<PowerShellOutputListener> outputListeners =
			new java.util.LinkedList<PowershellClient.PowerShellOutputListener>();

	private static volatile Boolean powerShellInstalled;

	private static final String[] POWERSHELL_INSTALLED_COMMAND = new String[] { "powershell.exe", "-inputformat",
			"none", "-?" };

	private static final String POWERSHELL_CLIENT_SCRIPT = "bootstrap-client.ps1";

	/********
	 * Adds an output listener.
	 * 
	 * @param listener
	 *            the listener.
	 */
	public void addOutputListener(final PowerShellOutputListener listener) {
		this.outputListeners.add(listener);
	}

	private void notifyListeners(final String line) {
		for (final PowerShellOutputListener listener : this.outputListeners) {
			listener.onPowerShellOutput(line);
		}
	}

	// TODO: add timeout
	private String readProcessOutput(final Process p, final boolean notifyOnOutput) throws PowershellClientException {
		final StringBuilder sb = new StringBuilder();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		try {
			final String newline = System.getProperty("line.separator");
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				// this.publishEvent("powershell_output_line", line);
				if (notifyOnOutput) {
					notifyListeners(line);
				}
				logger.fine(line);
				sb.append(line).append(newline);
			}

		} catch (final IOException e) {
			throw new PowershellClientException("Failed to read output of powershell process", e);
		} finally {
			try {
				reader.close();
			} catch (final IOException e) {
				logger.log(Level.SEVERE, "Error while closing process input stream: " + e.getMessage(), e);

			}

		}
		return sb.toString();
	}

	private List<String> getPowershellCommandLine(final String target, final String username, final String password,
			final String command, final String localDir) throws PowershellClientException {

		final File clientScriptFile = new File(localDir, POWERSHELL_CLIENT_SCRIPT);
		if (!clientScriptFile.exists()) {
			throw new PowershellClientException(
					"Could not find expected powershell client script in local directory. Was expecting file: "
							+ clientScriptFile.getAbsolutePath());
		}
		final String[] commandLineParts = { "powershell.exe", "-inputformat", "none", "-File",
				clientScriptFile.getAbsolutePath(), "-target", target, "-password", quoteString(password), "-username",
				quoteString(username), "-command", quoteString(command) };

		return Arrays.asList(commandLineParts);
	}

	private String quoteString(final String input) {
		return "\"" + input + "\"";
	}

	// TODO - add timeout. Use commons exec package to add watchdog
	/********
	 * Executes a remote powershell command. using the client script.
	 * 
	 * @param targetHost
	 *            ip or host name of target machine.
	 * @param command
	 *            the command to execute.
	 * @param username
	 *            the username for the remote machine.
	 * @param password
	 *            the password for the remote machine.
	 * @param localDir
	 *            the local directory where the client script can be found.
	 * @return the output of the powershell process.
	 * @throws PowershellClientException
	 *             if there was a problem executing the script.
	 * @throws InterruptedException .
	 */
	public String invokeRemotePowershellCommand(final String targetHost, final String command, final String username,
			final String password, final String localDir) throws PowershellClientException, InterruptedException {

		checkPowershellInstalled();
		final List<String> fullCommand = getPowershellCommandLine(targetHost, username, password, command, localDir);
	
		final ProcessBuilder pb = new ProcessBuilder(fullCommand);
		pb.redirectErrorStream(true);

		try {
			final Process p = pb.start();

			final String output = readProcessOutput(p, true);
			final int exitCode = p.waitFor();
			if (exitCode != 0) {
				throw new PowershellClientException("Remote installation failed with exit code: " + exitCode
						+ ". Execution output: " + output);
			}
			return output;
		} catch (final IOException e) {
			throw new PowershellClientException("Failed to invoke remote installation: " + e.getMessage(), e);
		}
	}

	private void checkPowershellInstalled() throws PowershellClientException, InterruptedException {
		if (powerShellInstalled != null) {
			if (powerShellInstalled.booleanValue()) {
				return;
			}
			throw new PowershellClientException(
					"powershell.exe is not on installed, or is not available on the system path. "
							+ "Powershell is required on both client and server for Cloudify to work on Windows. ");
		}

		logger.fine("Checking if powershell is installed using: " + Arrays.toString(POWERSHELL_INSTALLED_COMMAND));
		final ProcessBuilder pb = new ProcessBuilder(Arrays.asList(POWERSHELL_INSTALLED_COMMAND));
		pb.redirectErrorStream(true);

		Process p;
		try {
			p = pb.start();
		} catch (final IOException e) {
			throw new PowershellClientException("Failed to start powershell - it may not be installed", e);
		}

		final String output = readProcessOutput(p, false);

		logger.fine("Finished reading output");
		final int retval = p.waitFor();
		logger.fine("Powershell installed command exit value: " + retval);
		if (retval != 0) {
			throw new PowershellClientException(
					"powershell.exe is not on installed, or is not available on the system path. "
							+ "Powershell is required on both client and server for Cloudify to work on Windows. "
							+ "Execution result: " + output);
		}

		powerShellInstalled = Boolean.TRUE;
	}
}
