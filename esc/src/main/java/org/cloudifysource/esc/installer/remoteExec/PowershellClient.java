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
import java.util.ArrayList;
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

	private static final String[] POWERSHELL_TRUST_HOST_WINRM =  new String [] {"cmd.exe","/c","winrm","set","winrm/config/client"};

	private static final String POWERSHELL_CLIENT_SCRIPT = "bootstrap-client.ps1";
	private static final String POWERSHELL_CHECK_WINRM_CONNECTION_SCRIPT = "check-winrm-connection.ps1";

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
	
	private enum IoStreamType {
		OUTPUT,
		ERROR;
	}

	// TODO: add timeout
	private String readProcessStream(final Process p, final boolean notifyOnOutput, final IoStreamType streamType) throws PowershellClientException {
		final StringBuilder sb = new StringBuilder();
		InputStreamReader streamReader;
		if (streamType.equals(IoStreamType.ERROR)) {
			streamReader = new InputStreamReader(p.getErrorStream());
		} else {
			streamReader = new InputStreamReader(p.getInputStream());
		}
		
		final BufferedReader reader = new BufferedReader(streamReader);		
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
	
	private List<String> getPowershellConnectionCheckCommandLine(final String target, final String username, 
			final String password, final String localDir) {

		final File clientScriptFile = new File(localDir, POWERSHELL_CHECK_WINRM_CONNECTION_SCRIPT);
		if (!clientScriptFile.exists()) {
			throw new IllegalArgumentException(
					"Could not find expected powershell client script in local directory. Was expecting file: "
							+ clientScriptFile.getAbsolutePath());
		}
		final String[] commandLineParts = { "powershell.exe", "-inputformat", "none", "-File",
				clientScriptFile.getAbsolutePath(), "-target", target, "-password", quoteString(password), "-username",
				quoteString(username) };

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

		// Put the target in trusted mode (!!! you've to be administrator for this command !!!)
		enableTrustedHost(targetHost);

		try {
			final Process p = pb.start();

			final String output = readProcessStream(p, true, IoStreamType.OUTPUT);
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
	

	/**
	 * Checks that a WinRM connection to the given host can be established.
	 * @param targetHost The target host to connect to
	 * @param username A username for authenticating to the target host
	 * @param password A password for authentication to the target host
	 * @param localDir The directory in which the "check-winrm-connection" script resides
	 * @param testInterval The time to wait between each test interval, in milliseconds
	 * @param endTime The end time by which this check will time out, in milliseconds
	 * @throws PowershellClientException Indicates the WinRM connection test failed
	 * @throws InterruptedException Indicates the WinRM connection test failed
	 */
	public void checkWinrmConnection(final String targetHost, final String username, final String password,
			final String localDir, final int testInterval, final long endTime)
					throws PowershellClientException, InterruptedException {
		
		logger.info("Checking WinRM connectivity to " + targetHost + " ...");
		List<String> commandLine = getPowershellConnectionCheckCommandLine(targetHost, username, password, localDir);
		final ProcessBuilder pb = new ProcessBuilder(commandLine);
		
		// Put the target in trusted mode (!!! you've to be administrator for this command !!!)
		logger.fine("Enabling trusted host: " + targetHost);
		enableTrustedHost(targetHost);
		
		PowershellClientException lastException = null;
		boolean connected = false;
		int attemptIndex = 1;
		while (System.currentTimeMillis() < endTime) {
			try {
				logger.fine("Attempting WinRM connection, attempt #" + attemptIndex + "...");
				winrmConnect(pb);
				connected = true;
				break;
			} catch (final PowershellClientException e) {
				// retry and don't throw an exception, the connection might still not be ready
				logger.fine("Connection attempt #" + attemptIndex + " failed, retrying...");
				lastException = e;
				Thread.sleep(testInterval);
			}
			attemptIndex++;
		}
		
		if (connected) {
			logger.info("WinRM connection established");
		} else {
			if (lastException == null) {
				// connection wasn't attempted even once, therefore the exception is null
				throw new PowershellClientException("WinRM connection check not performed, "
						+ "increase the timeout set by \"remoteExecutionConnectionTimeoutMillis\""
						+ " in the cloud configuration file");				
			} else {
				throw new PowershellClientException("WinRM connection attempts failed. Review the logs and consider"
						+ " increasing the timeout set by \"remoteExecutionConnectionTimeoutMillis\" in the cloud"
						+ " configuration file", lastException);
			}
		}
		
	}
	
	private void winrmConnect(final ProcessBuilder pb) throws PowershellClientException, InterruptedException {

		try {
			final Process p = pb.start();
			final String errorStr = readProcessStream(p, true, IoStreamType.ERROR);
			final int exitCode = p.waitFor();
			
			// if the exit code != 0 or the error stream is not empty - the process failed
			if (exitCode != 0) {
				throw new PowershellClientException("WinRM connection failed with exit code: " + exitCode
						+ ". Execution error: " + errorStr);
			}
			
			if (errorStr != null && errorStr.trim().length() > 0) {
				// FAILED
				throw new PowershellClientException("WinRM connection failed with error: \n" 
						+ errorStr);
			}
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

		final String output = readProcessStream(p, false, IoStreamType.OUTPUT);

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
	
	private String enableTrustedHost(final String ipTarget) throws PowershellClientException, InterruptedException {
			
		List<String> cmd = new ArrayList<String>();
		for ( String cmdElement : POWERSHELL_TRUST_HOST_WINRM)
			cmd .add(cmdElement);

		cmd.add("@{TrustedHosts=\""+ipTarget+"\"}"); 
	
		final ProcessBuilder trusCommand = new ProcessBuilder(cmd);
	   
		try {
			final Process trusCmdResult = trusCommand.start();
			final String output = readProcessStream(trusCmdResult, true, IoStreamType.OUTPUT);
			return output;
		} catch (final IOException e) {
			throw new PowershellClientException("Failed to enable trusted host for winrm command: " + e.getMessage(), e);
		}
		   
	}

}
