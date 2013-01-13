/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.installer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.installer.filetransfer.FileTransfer;
import org.cloudifysource.esc.installer.filetransfer.FileTransferFactory;
import org.cloudifysource.esc.installer.remoteExec.RemoteExecutor;
import org.cloudifysource.esc.installer.remoteExec.RemoteExecutorFactory;
import org.cloudifysource.esc.util.CalcUtils;
import org.cloudifysource.esc.util.Utils;

/************
 * The agentless installer class is responsible for installing Cloudify on a remote machine, using only SSH. It will
 * upload all relevant files and start the Cloudify agent.
 *
 * File transfer is handled using Apache commons vfs.
 *
 * @author barakme
 *
 */
public class AgentlessInstaller {

	@Override
	public String toString() {
		return "NewAgentlessInstaller [eventsListenersList=" + eventsListenersList + "]";
	}

	private static final int DEFAULT_ROUTE_RESOLUTION_TIMEOUT = 2 * 60 * 1000; // 2
	// minutes

	private static final String LINUX_STARTUP_SCRIPT_NAME = "bootstrap-management.sh";
	private static final String POWERSHELL_STARTUP_SCRIPT_NAME = "bootstrap-management.bat";

	private static final String MACHINE_ZONES_ENV = "MACHINE_ZONES";

	private static final String MACHINE_IP_ADDRESS_ENV = "MACHINE_IP_ADDRESS";

	private static final String GSA_MODE_ENV = "GSA_MODE";

	private static final String NO_WEB_SERVICES_ENV = "NO_WEB_SERVICES";

	private static final String LUS_IP_ADDRESS_ENV = "LUS_IP_ADDRESS";

	private static final String WORKING_HOME_DIRECTORY_ENV = "WORKING_HOME_DIRECTORY";

	private static final String GSA_RESERVATION_ID_ENV = "GSA_RESERVATION_ID";

	private static final String CLOUD_FILE = "CLOUD_FILE";

	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AgentlessInstaller.class
			.getName());

	// tries opening a socket to port 22, and waits for the specified connection
	// timeout
	// between retry attempts it sleeps the specified diration
	private static final int CONNECTION_TEST_SOCKET_CONNECT_TIMEOUT_MILLIS = 10000;

	private static final int CONNECTION_TEST_SLEEP_BEFORE_RETRY_MILLIS = 5 * 1000;

	private final List<AgentlessInstallerListener> eventsListenersList = new LinkedList<AgentlessInstallerListener>();

	/******
	 * Name of the logger used for piping out ssh output.
	 */
	public static final String SSH_OUTPUT_LOGGER_NAME = AgentlessInstaller.class.getName() + ".ssh.output";

	/********
	 * Name of the internal logger used by the ssh component.
	 */
	public static final String SSH_LOGGER_NAME = "com.jcraft.jsch";

	/***********
	 * Constructor.
	 */
	public AgentlessInstaller() {
		final Logger sshLogger = Logger.getLogger(SSH_LOGGER_NAME);
		com.jcraft.jsch.JSch.setLogger(new JschJdkLogger(sshLogger));
	}

	private static InetAddress waitForRoute(final String ip, final long endTime) throws InstallerException,
			InterruptedException {
		Exception lastException = null;
		while (System.currentTimeMillis() < endTime) {

			try {
				return InetAddress.getByName(ip);
			} catch (final IOException e) {
				lastException = e;
			}
			Thread.sleep(CONNECTION_TEST_SLEEP_BEFORE_RETRY_MILLIS);

		}

		throw new InstallerException("Failed to resolve installation target: " + ip, lastException);
	}

	/*******
	 * Checks if a TCP connection to a remote machine and port is possible.
	 *
	 * @param ip
	 *            remote machine ip.
	 * @param port
	 *            remote machine port.
	 * @param timeout
	 *            duration to wait for successful connection.
	 * @param unit
	 *            time unit to wait.
	 * @throws InstallerException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 */
	public static void checkConnection(final String ip, final int port, final long timeout, final TimeUnit unit)
			throws TimeoutException, InterruptedException, InstallerException {

		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		final InetAddress inetAddress = waitForRoute(ip,
				Math.min(end, System.currentTimeMillis() + DEFAULT_ROUTE_RESOLUTION_TIMEOUT));
		final InetSocketAddress socketAddress = new InetSocketAddress(inetAddress, port);

		logger.fine("Checking connection to: " + socketAddress);
		while (System.currentTimeMillis() + CONNECTION_TEST_SLEEP_BEFORE_RETRY_MILLIS < end) {

			// need to sleep since sock.connect may return immediately, and
			// server may take time to start
			Thread.sleep(CONNECTION_TEST_SLEEP_BEFORE_RETRY_MILLIS);

			final Socket sock = new Socket();
			try {
				sock.connect(socketAddress, CONNECTION_TEST_SOCKET_CONNECT_TIMEOUT_MILLIS);
				return;
			} catch (final IOException e) {
				logger.log(Level.FINE, "Checking connection to: " + socketAddress, e);
				// retry
			} finally {
				if (sock != null) {
					try {
						sock.close();
					} catch (final IOException e) {
						logger.fine("Failed to close socket");
					}
				}
			}
		}
		throw new TimeoutException("Failed connecting to " + ip + ":" + port);

	}

	/******
	 * Performs installation on a remote machine with a known IP.
	 *
	 * @param details
	 *            the installation details.
	 * @param timeout
	 *            the timeout duration.
	 * @param unit
	 *            the timeout unit.
	 * @throws InterruptedException .
	 * @throws TimeoutException .
	 * @throws InstallerException .
	 */
	public void installOnMachineWithIP(final InstallationDetails details, final long timeout, final TimeUnit unit)
			throws TimeoutException, InterruptedException, InstallerException {

		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		if (details.getLocator() == null) {
			// We are installing the lus now
			details.setLocator(details.getPrivateIp());
		}

		logger.fine("Executing agentless installer with the following details:\n" + details.toString());

		// this is the right way to get the target, but the naming is off.
		final String targetHost = details.isConnectedToPrivateIp() ? details.getPrivateIp() : details.getPublicIp();

		final int port = details.getFileTransferMode().getDefaultPort();

		publishEvent("attempting_to_access_vm", targetHost);
		checkConnection(targetHost, port, CalcUtils.millisUntil(end), TimeUnit.MILLISECONDS);

		File environmentFile = null;
		// create the environment file
		try {
			environmentFile = createEnvironmentFile(details);
			// upload bootstrap files
			publishEvent("uploading_files_to_node", targetHost);
			uploadFilesToServer(details, environmentFile, end, targetHost);

		} catch (final IOException e) {
			throw new InstallerException("Failed to create environment file", e);
		} finally {
			// delete the temp directory and temp env file.
			if (environmentFile != null) {
				FileUtils.deleteQuietly(environmentFile.getParentFile());
			}
		}

		// launch the cloudify agent
		publishEvent("launching_agent_on_node", targetHost);
		remoteExecuteAgentOnServer(details, end, targetHost);

		publishEvent("install_completed_on_node", targetHost);

	}

	private File createEnvironmentFile(final InstallationDetails details) throws IOException {

		String remoteDirectory = details.getRemoteDir();
		if (remoteDirectory.endsWith("/")) {
			remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
		}
		if (details.isLus()) {
			// add the relative path to the cloud file location
			remoteDirectory = remoteDirectory + "/" + details.getRelativeLocalDir();
		}

		String authGroups = null;
		if (details.getAuthGroups() != null) {
			// authgroups should be a strongly typed object convertible into a
			// String
			authGroups = details.getAuthGroups();
		}

		final EnvironmentFileBuilder builder = new EnvironmentFileBuilder(details.getScriptLanguage())
				.exportVar(LUS_IP_ADDRESS_ENV, details.getLocator())
				.exportVar(GSA_MODE_ENV, details.isLus() ? "lus" : "agent")
				.exportVar(CloudifyConstants.SPRING_ACTIVE_PROFILE_ENV_VAR, details.getSecurityProfile())
				.exportVar(NO_WEB_SERVICES_ENV,
						details.isNoWebServices() ? "true" : "false")
				.exportVar(
						MACHINE_IP_ADDRESS_ENV,
						details.isBindToPrivateIp() ? details.getPrivateIp()
								: details.getPublicIp())
				.exportVar(MACHINE_ZONES_ENV, details.getZones())
				.exportVarWithQuotes(CloudifyConstants.CLOUDIFY_LINK_ENV,
						details.getCloudifyUrl())
				.exportVarWithQuotes(CloudifyConstants.CLOUDIFY_OVERRIDES_LINK_ENV,
						details.getOverridesUrl())
				.exportVar(WORKING_HOME_DIRECTORY_ENV, remoteDirectory)
				.exportVar(CloudifyConstants.GIGASPACES_AUTH_GROUPS, authGroups)
				.exportVar(CloudifyConstants.GIGASPACES_AGENT_ENV_PRIVATE_IP, details.getPrivateIp())
				.exportVar(CloudifyConstants.GIGASPACES_AGENT_ENV_PUBLIC_IP, details.getPublicIp())
				.exportVar(CloudifyConstants.GIGASPACES_CLOUD_TEMPLATE_NAME, details.getTemplateName())
				.exportVar(CloudifyConstants.GIGASPACES_CLOUD_MACHINE_ID, details.getMachineId())
				.exportVar(CloudifyConstants.CLOUDIFY_CLOUD_MACHINE_ID, details.getMachineId())

				// maintain backwards compatibility for pre 2.3.0
				.exportVar(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP, details.getPrivateIp())
				.exportVar(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP, details.getPublicIp());

		if (details.getReservationId() != null) {
			builder.exportVar(GSA_RESERVATION_ID_ENV, details.getReservationId().toString());
		}

		if (details.isLus()) {
			String remotePath = details.getRemoteDir();
			if (!remotePath.endsWith("/")) {
				remotePath += "/";
			}
			builder.exportVar(CLOUD_FILE, remotePath + details.getCloudFile().getName());

			logger.log(Level.FINE, "Setting ESM/GSM/LUS/GSA/GSC java options");

			builder.exportVar("ESM_JAVA_OPTIONS", details.getEsmCommandlineArgs());
			builder.exportVar("LUS_JAVA_OPTIONS", details.getLusCommandlineArgs());
			builder.exportVar("GSM_JAVA_OPTIONS", details.getGsmCommandlineArgs());
			builder.exportVar("GSA_JAVA_OPTIONS", details.getGsaCommandlineArgs());
			builder.exportVar("GSC_JAVA_OPTIONS", details.getGscCommandlineArgs());

			if (details.getRestPort() != null) {
				builder.exportVar(CloudifyConstants.REST_PORT_ENV_VAR, details.getRestPort().toString());
			}
			if (details.getRestMaxMemory() != null) {
				builder.exportVar(CloudifyConstants.REST_MAX_MEMORY_ENVIRONMENT_VAR, details.getRestMaxMemory());
			}
			if (details.getWebuiPort() != null) {
				builder.exportVar(CloudifyConstants.WEBUI_PORT_ENV_VAR, details.getWebuiPort().toString());
			}
			if (details.getWebuiMaxMemory() != null) {
				builder.exportVar(CloudifyConstants.WEBUI_MAX_MEMORY_ENVIRONMENT_VAR, details.getWebuiMaxMemory());
			}
		}

		if (details.getUsername() != null) {
			builder.exportVar("USERNAME", details.getUsername());
		}
		if (details.getPassword() != null) {
			builder.exportVar("PASSWORD", details.getPassword());
		}

		builder.exportVar(CloudifyConstants.SPRING_SECURITY_CONFIG_FILE_ENV_VAR, details.getRemoteDir()
				+ "/" + CloudifyConstants.SECURITY_FILE_NAME);
		if (StringUtils.isNotBlank(details.getKeystorePassword())) {
			builder.exportVar(CloudifyConstants.KEYSTORE_FILE_ENV_VAR, details.getRemoteDir()
					+ "/" + CloudifyConstants.KEYSTORE_FILE_NAME);
			builder.exportVar(CloudifyConstants.KEYSTORE_PASSWORD_ENV_VAR, details.getKeystorePassword());
		}

		final Set<Entry<String, String>> entries = details.getExtraRemoteEnvironmentVariables().entrySet();
		for (final Entry<String, String> entry : entries) {
			builder.exportVar(entry.getKey(), entry.getValue());
		}

		final String fileContents = builder.toString();

		final File tempFolder = Utils.createTempFolder();
		final File tempFile = new File(tempFolder, builder.getEnvironmentFileName());
		tempFile.deleteOnExit();
		FileUtils.writeStringToFile(tempFile, fileContents);

		return tempFile;
	}

	private void remoteExecuteAgentOnServer(final InstallationDetails details, final long end, final String targetHost)
			throws InstallerException, TimeoutException, InterruptedException {

		// get script for execution mode
		final String scriptFileName = getScriptFileName(details);

		String remoteDirectory = details.getRemoteDir();
		if (remoteDirectory.endsWith("/")) {
			remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
		}
		if (details.isLus()) {
			// add the relative path to the cloud file location
			remoteDirectory = remoteDirectory + "/" + details.getRelativeLocalDir();
		}
		final String scriptPath = remoteDirectory + "/" + scriptFileName;

		final RemoteExecutor remoteExecutor =
				RemoteExecutorFactory.createRemoteExecutorProvider(details.getRemoteExecutionMode());
		remoteExecutor.initialize(this, details);
		remoteExecutor.execute(targetHost, details, scriptPath, end);

		return;

	}

	private String getScriptFileName(final InstallationDetails details) {
		final String scriptFileName;

		switch (details.getRemoteExecutionMode()) {
		case WINRM:
			scriptFileName = POWERSHELL_STARTUP_SCRIPT_NAME;
			break;
		case SSH:
			scriptFileName = LINUX_STARTUP_SCRIPT_NAME;
			break;
		default:
			throw new UnsupportedOperationException("Remote Execution Mode: " + details.getRemoteExecutionMode()
					+ " not supported");
		}
		return scriptFileName;
	}

	private void uploadFilesToServer(final InstallationDetails details, final File environmentFile, final long end,
			final String targetHost)
			throws TimeoutException, InstallerException, InterruptedException {

		final Set<String> excludedFiles = new HashSet<String>();
		if (!details.isLus() && details.getManagementOnlyFiles() != null) {
			excludedFiles.addAll(Arrays.asList(details.getManagementOnlyFiles()));
		}

		final FileTransfer fileTransfer = FileTransferFactory.getFileTrasnferProvider(details.getFileTransferMode());
		fileTransfer.initialize(details, end);
		fileTransfer.copyFiles(details, excludedFiles, Arrays.asList(environmentFile), end);

	}

	/**********
	 * Registers an event listener for installation events.
	 *
	 * @param listener
	 *            the listener.
	 */
	public void addListener(final AgentlessInstallerListener listener) {
		this.eventsListenersList.add(listener);
	}

	/*********
	 * This method is public so that implementation classes for file copy and remote execution can publish events.
	 *
	 * @param eventName
	 *            .
	 * @param args
	 *            .
	 */
	public void publishEvent(final String eventName, final Object... args) {
		for (final AgentlessInstallerListener listner : this.eventsListenersList) {
			try {
				listner.onInstallerEvent(eventName, args);
			} catch (final Exception e) {
				logger.log(Level.FINE, "Exception in listener while publishing event: " + eventName
						+ " with arguments: " + Arrays.toString(args), e);
			}
		}
	}

}
