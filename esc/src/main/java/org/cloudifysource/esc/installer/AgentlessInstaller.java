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
package org.cloudifysource.esc.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.taskdefs.optional.testing.BuildTimeoutException;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.util.ShellCommandBuilder;
import org.cloudifysource.esc.util.Utils;

/************
 * The agentless installer class is responsible for installing Cloudify on a
 * remote machine, using only SSH. It will upload all relevant files and start
 * the Cloudify agent.
 * 
 * File transfer is handled using Apache commons vfs.
 * 
 * @author barakme
 * 
 */
public class AgentlessInstaller {

	private static final int MACHINE_ACCESS_NUMBER_OF_RETRIES = 3;

	private static final int TIMEOUT_BETWEEN_MACHINE_ACCESS_ATTEMPTS_MILLIS = 5000;

	private static final String POWERSHELL_CLIENT_SCRIPT = "bootstrap-client.ps1";

	private static final int POWERSHELL_PORT = 5985;

	private static final int CIFS_PORT = 445;

	private static final int DEFAULT_ROUTE_RESOLUTION_TIMEOUT = 2 * 60 * 1000; // 2
																				// minutes

	private static final String LINUX_STARTUP_SCRIPT_NAME = "bootstrap-management.sh";
	private static final String POWERSHELL_STARTUP_SCRIPT_NAME = "bootstrap-management.bat";

	private static final String CLOUDIFY_LINK_ENV = "GIGASPACES_LINK";
	private static final String CLOUDIFY_OVERRIDES_LINK_ENV = "GIGASPACES_OVERRIDES_LINK";

	private static final String MACHINE_ZONES_ENV = "MACHINE_ZONES";

	private static final String MACHINE_IP_ADDRESS_ENV = "MACHINE_IP_ADDRESS";

	private static final String GSA_MODE_ENV = "GSA_MODE";

	private static final String NO_WEB_SERVICES_ENV = "NO_WEB_SERVICES";

	private static final String LUS_IP_ADDRESS_ENV = "LUS_IP_ADDRESS";

	private static final String WORKING_HOME_DIRECTORY_ENV = "WORKING_HOME_DIRECTORY";
	
	private static final String GSA_RESERVATION_ID_ENV = "GSA_RESERVATION_ID";

	private static final String CLOUD_FILE = "CLOUD_FILE";

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(AgentlessInstaller.class.getName());

	// tries opening a socket to port 22, and waits for the specified connection
	// timeout
	// between retry attempts it sleeps the specified diration
	private static final int CONNECTION_TEST_SOCKET_CONNECT_TIMEOUT_MILLIS = 10000;

	private static final int CONNECTION_TEST_SLEEP_BEFORE_RETRY_MILLIS = 5 * 1000;

	private static final int SSH_PORT = 22;

	// TODO check if this is the proper timeout
	// timeout of uploading a file over SFTP
	private static final int SFTP_DISCONNECT_DETECTION_TIMEOUT_MILLIS = 10 * 1000;

	private final List<AgentlessInstallerListener> eventsListenersList = new LinkedList<AgentlessInstallerListener>();

	private static final String[] POWERSHELL_INSTALLED_COMMAND = new String[] {
			"powershell.exe", "-inputformat", "none", "-?" };

	// indicates if powershell is installed on this host. If null, installation
	// test was not performed.
	private static volatile Boolean powerShellInstalled = null;
	/******
	 * Name of the logger used for piping out ssh output.
	 */
	public static final String SSH_OUTPUT_LOGGER_NAME = AgentlessInstaller.class
			.getName() + ".ssh.output";

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

	private static InetAddress waitForRoute(final String ip, final long endTime)
			throws InstallerException, InterruptedException {
		Exception lastException = null;
		while (System.currentTimeMillis() < endTime) {

			try {
				return InetAddress.getByName(ip);
			} catch (final IOException e) {
				lastException = e;
			}
			Thread.sleep(CONNECTION_TEST_SLEEP_BEFORE_RETRY_MILLIS);

		}

		throw new InstallerException("Failed to resolve installation target: "
				+ ip, lastException);
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
	 * @throws InstallerException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 * @throws ElasticMachineProvisioningException
	 */
	public static void checkConnection(final String ip, final int port,
			final long timeout, final TimeUnit unit) throws TimeoutException,
			InterruptedException, InstallerException {

		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		final InetAddress inetAddress = waitForRoute(
				ip,
				Math.min(end, System.currentTimeMillis()
						+ DEFAULT_ROUTE_RESOLUTION_TIMEOUT));
		final InetSocketAddress socketAddress = new InetSocketAddress(
				inetAddress, port);

		logger.fine("Checking connection to: " + socketAddress);
		while (System.currentTimeMillis()
				+ CONNECTION_TEST_SLEEP_BEFORE_RETRY_MILLIS < end) {

			// need to sleep since sock.connect may return immediately, and
			// server may take time to start
			Thread.sleep(CONNECTION_TEST_SLEEP_BEFORE_RETRY_MILLIS);

			final Socket sock = new Socket();
			try {
				sock.connect(socketAddress,
						CONNECTION_TEST_SOCKET_CONNECT_TIMEOUT_MILLIS);
				return;
			} catch (final IOException e) {
				logger.log(Level.FINE, "Checking connection to: "
						+ socketAddress, e);
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

	/****
	 * Copies files from local dir to remote dir.
	 * 
	 * @param host
	 *            host name or ip address of remote machine.
	 * @param username
	 *            ssh username of remote machine.
	 * @param password
	 *            ssh password of remote machine.
	 * @param srcDir
	 *            local directory.
	 * @param toDir
	 *            remote directory.
	 * @param keyFile
	 *            The key file of the remote machine, if used. private key file.
	 * @param excludedFile
	 *            Files that should not be copied.
	 * @param cloudFile
	 *            The cloud file.
	 * @param timeout
	 *            Time before timeout is thrown.
	 * @param unit
	 *            Time unit, relevant to timeout parameter.
	 * @param fileTransferMode
	 *            Remote file system type.
	 * @throws IOException
	 *             in case of an error during file transfer.
	 * @throws TimeoutException
	 * @throws URISyntaxException
	 * @throws InstallerException
	 * @throws InterruptedException
	 */
	private void copyFiles(final InstallationDetails details,
			final String host, final Set<String> excludedFiles,
			final long timeout, final TimeUnit unit) throws IOException,
			TimeoutException, URISyntaxException, InterruptedException,
			InstallerException {

		if (timeout < 0) {
			throw new TimeoutException("Uploading files to host " + host
					+ " timed out");
		}
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		final FileSystemOptions opts = new FileSystemOptions();

		FileSystemManager createdManager = null;
		String target = null;
		switch (details.getFileTransferMode()) {
		case SCP:
			createdManager = createRemoteSSHFileSystem(details.getKeyFile(),
					opts, details);

			final String userDetails;
			if (details.getPassword() != null
					&& details.getPassword().length() > 0) {
				userDetails = details.getUsername() + ":"
						+ details.getPassword();
			} else {
				userDetails = details.getUsername();
			}
			target = new java.net.URI("sftp", userDetails, host, SSH_PORT,
					details.getRemoteDir(), null, null).toASCIIString();
			break;
		case CIFS:
			checkConnection(host, CIFS_PORT, timeout, unit);
			createdManager = VFS.getManager();

			target = new java.net.URI("smb", details.getUsername() + ":"
					+ details.getPassword(), host, CIFS_PORT,
					details.getRemoteDir(), null, null).toASCIIString();

			break;
		default:
			throw new UnsupportedOperationException(
					"Unsupported Remote File System: "
							+ details.getFileTransferMode().toString());
		}

		// when bootstrapping a management machine, pass all of the cloud
		// configuration, including all template
		// for an agent machine, just pass the upload dir fot the specific
		// template.
		String localDirPath = details.getLocalDir();
		if (details.isLus()) {
			if (details.getCloudFile() == null) {
				throw new IllegalArgumentException(
						"While bootstrapping a management machine, cloud file is null");
			}

			localDirPath = details.getCloudFile().getParentFile()
					.getAbsolutePath();

		}

		logger.fine("Setting local directory for file upload to: "
				+ localDirPath);
		final FileSystemManager mng = createdManager;

		mng.setLogger(org.apache.commons.logging.LogFactory.getLog(logger
				.getName()));
		final FileObject localDir = mng.resolveFile("file:" + localDirPath);

		final FileObject remoteDir = resolveTargetDirectory(opts, target, mng);

		logger.fine("Copying files to: " + target + " from local dir: "
				+ localDir.getName().getPath() + " excluding "
				+ excludedFiles.toString());

		try {
			remoteDir.copyFrom(localDir, new FileSelector() {

				@Override
				public boolean includeFile(final FileSelectInfo fileInfo)
						throws Exception {
					if (excludedFiles.contains(fileInfo.getFile().getName()
							.getBaseName())) {
						logger.fine(fileInfo.getFile().getName().getBaseName()
								+ " excluded");
						return false;

					}
					//The key file shouldn't be copied to any machine aside from the management machine.
					if (details.getKeyFile() != null) {
						if (fileInfo.getFile().getType() == FileType.FILE) {
							String fileName = new File(fileInfo.getFile().getURL().toString()).getName();
							String keyFileName =  new File(details.getKeyFile()).getName();
							if (StringUtils.equals(keyFileName, fileName)) {
								if (!details.isLus()) {
									return false;
								}
							}
						}
					}
					
					final FileObject remoteFile = mng.resolveFile(
							remoteDir,
							localDir.getName().getRelativeName(
									fileInfo.getFile().getName()));

					if (!remoteFile.exists()) {
						logger.fine(fileInfo.getFile().getName().getBaseName()
								+ " missing on server");
						return true;
					}

					if (fileInfo.getFile().getType() == FileType.FILE) {
						final long remoteSize = remoteFile.getContent()
								.getSize();
						final long localSize = fileInfo.getFile().getContent()
								.getSize();
						final boolean res = localSize != remoteSize;
						if (res) {
							logger.fine(fileInfo.getFile().getName()
									.getBaseName()
									+ " different on server");
						}
						return res;
					}
					return false;

				}

				@Override
				public boolean traverseDescendents(final FileSelectInfo fileInfo)
						throws Exception {
					return true;
				}
			});

			if (details.getCloudFile() != null) {
				// copy cloud file too TODO - remote this
				final FileObject cloudFileParentObject = mng
						.resolveFile(details.getCloudFile().getParentFile()
								.getAbsolutePath());
				final FileObject cloudFileObject = mng.resolveFile(details
						.getCloudFile().getAbsolutePath());
				remoteDir.copyFrom(cloudFileParentObject, new FileSelector() {

					@Override
					public boolean traverseDescendents(
							final FileSelectInfo fileInfo) throws Exception {
						return true;
					}

					@Override
					public boolean includeFile(final FileSelectInfo fileInfo)
							throws Exception {
						return fileInfo.getFile().equals(cloudFileObject);

					}
				});
			}

			logger.fine("Copying files to: " + target + " completed.");
		} finally {
			mng.closeFileSystem(remoteDir.getFileSystem());
			mng.closeFileSystem(localDir.getFileSystem());
		}

		if (end < System.currentTimeMillis()) {
			throw new TimeoutException("Uploading files to host " + host
					+ " timed out");
		}
	}

	private FileObject resolveTargetDirectory(final FileSystemOptions opts,
			String target, final FileSystemManager mng)
			throws FileSystemException {
		FileSystemException lastException = null;
		// TODO - move these constants to an external configuration file
		for (int i = 0; i < MACHINE_ACCESS_NUMBER_OF_RETRIES; ++i) {
			try {
				FileObject targetDirectory = mng.resolveFile(target, opts);
				logger.fine("Remote directory resolved successfully.");
				return targetDirectory;
			} catch (FileSystemException fse) {
				logger.fine("Attempt number: "
						+ (i + 1)
						+ " to reslve remote directory failed."
						+ " This may be a temporary issue while remote machine is starting up.");
				try {
					Thread.sleep(TIMEOUT_BETWEEN_MACHINE_ACCESS_ATTEMPTS_MILLIS);
				} catch (InterruptedException e) {
					// ignore
				}
				lastException = fse;
			}
		}
		throw lastException;
	}

	private FileSystemManager createRemoteSSHFileSystem(final String keyFile,
			final FileSystemOptions opts, final InstallationDetails details)
			throws FileSystemException, FileNotFoundException {
		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(
				opts, "no");

		final Object preferredAuthenticationMethods = details
				.getCustomData()
				.get(CloudifyConstants.INSTALLER_CUSTOM_DATA_SFTP_PREFERRED_AUTHENTICATION_METHODS_KEY);

		if (preferredAuthenticationMethods != null
				&& String.class.isInstance(preferredAuthenticationMethods)) {
			SftpFileSystemConfigBuilder.getInstance()
					.setPreferredAuthentications(opts,
							(String) preferredAuthenticationMethods);
		}

		SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);

		if (keyFile != null && keyFile.length() > 0) {
			final File temp = new File(keyFile);
			if (!temp.exists()) {
				throw new FileNotFoundException(
						"Could not find key file: "
								+ temp
								+ ". KeyFile "
								+ keyFile
								+ " that was passed in the installation Details does not exist");
			}
			SftpFileSystemConfigBuilder.getInstance().setIdentities(opts,
					new File[] { temp });
		}

		SftpFileSystemConfigBuilder.getInstance().setTimeout(opts,
				SFTP_DISCONNECT_DETECTION_TIMEOUT_MILLIS);
		return VFS.getManager();
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
	public void installOnMachineWithIP(final InstallationDetails details,
			final long timeout, final TimeUnit unit) throws TimeoutException,
			InterruptedException, InstallerException {

		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		if (details.getLocator() == null) {
			// We are installing the lus now
			details.setLocator(details.getPrivateIp());
		}

		logger.fine("Executing agentless installer with the following details:\n"
				+ details.toString());

		final String targetHost = details.isConnectedToPrivateIp() ? details
				.getPrivateIp() : details.getPublicIp();

		int port = 0;
		switch (details.getFileTransferMode()) {
		case CIFS:
			port = CIFS_PORT;
			break;
		case SCP:
			port = SSH_PORT;
			break;
		default:
			throw new UnsupportedOperationException("File Transfer Mode: "
					+ details.getFileTransferMode() + " not supported");
		}

		publishEvent("attempting_to_access_vm", targetHost);
		checkConnection(targetHost, port, Utils.millisUntil(end),
				TimeUnit.MILLISECONDS);

		// upload bootstrap files
		publishEvent("uploading_files_to_node", targetHost);
		uploadFilesToServer(details, end, targetHost);

		// launch the cloudify agent
		publishEvent("launching_agent_on_node", targetHost);
		remoteExecuteAgentOnServer(details, end, targetHost);

		publishEvent("install_completed_on_node", targetHost);

	}

	private void remoteExecuteAgentOnServer(final InstallationDetails details,
			final long end, final String targetHost) throws InstallerException,
			TimeoutException, InterruptedException {

		// get script for execution mode
		final String scriptFileName = getScriptFileName(details);

		String remoteDirectory = details.getRemoteDir();
		if (remoteDirectory.endsWith("/")) {
			remoteDirectory = remoteDirectory.substring(0,
					remoteDirectory.length() - 1);
		}
		if (details.isLus()) { // TODO - fix - use local dir of template
			// add the relative path to the cloud file location
			remoteDirectory = remoteDirectory + "/"
					+ details.getRelativeLocalDir();
		}

		final String scriptPath = remoteDirectory + "/" + scriptFileName;

		final ShellCommandBuilder scb = new ShellCommandBuilder(
				details.getRemoteExecutionMode())
				.exportVar(LUS_IP_ADDRESS_ENV, details.getLocator())
				.exportVar(GSA_MODE_ENV, details.isLus() ? "lus" : "agent")
				.exportVar(NO_WEB_SERVICES_ENV,
						details.isNoWebServices() ? "true" : "false")
				.exportVar(
						MACHINE_IP_ADDRESS_ENV,
						details.isBindToPrivateIp() ? details.getPrivateIp()
								: details.getPublicIp())
				.exportVar(MACHINE_ZONES_ENV, details.getZones())
				.exportVar(
						CLOUDIFY_LINK_ENV,
						details.getCloudifyUrl() != null ? "\""
								+ details.getCloudifyUrl() + "\"" : "")
				.exportVar(
						CLOUDIFY_OVERRIDES_LINK_ENV,
						details.getOverridesUrl() != null ? "\""
								+ details.getOverridesUrl() + "\"" : "")
				.exportVar(WORKING_HOME_DIRECTORY_ENV, remoteDirectory)
				.exportVar(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP,
						details.getPrivateIp())
				.exportVar(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP,
						details.getPublicIp())
				.exportVar(CloudifyConstants.CLOUDIFY_CLOUD_TEMPLATE_NAME,
						details.getTemplateName());

		if (details.getReservationId() != null) {
			scb.exportVar(GSA_RESERVATION_ID_ENV, details.getReservationId().toString());
		}
		
		if (details.isLus()) {
			String remotePath = details.getRemoteDir();
			if (!remotePath.endsWith("/")) {
				remotePath += "/";
			}
			scb.exportVar(CLOUD_FILE, remotePath
					+ details.getCloudFile().getName());
		}

		if (details.getUsername() != null) {
			scb.exportVar("USERNAME", details.getUsername());
		}
		if (details.getPassword() != null) {
			scb.exportVar("PASSWORD", details.getPassword());
		}

		final Set<Entry<String, String>> entries = details
				.getExtraRemoteEnvironmentVariables().entrySet();
		for (final Entry<String, String> entry : entries) {
			scb.exportVar(entry.getKey(), entry.getValue());
		}

		scb.chmodExecutable(scriptPath).call(scriptPath);

		final String command = scb.toString();

		logger.fine("Calling startup script on target: " + targetHost
				+ " with LOCATOR=" + details.getLocator()
				+ "\nThis may take a few minutes");

		switch (details.getRemoteExecutionMode()) {
		case SSH:
			sshCommand(targetHost, command, details.getUsername(),
					details.getPassword(), details.getKeyFile(),
					Utils.millisUntil(end), TimeUnit.MILLISECONDS);

			break;
		case WINRM:
			powershellCommand(targetHost, command, details.getUsername(),
					details.getPassword(), details.getKeyFile(),
					Utils.millisUntil(end), TimeUnit.MILLISECONDS,
					details.getLocalDir());
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	private String getScriptFileName(final InstallationDetails details) {
		final String scriptFileName;

		switch (details.getRemoteExecutionMode()) {
		case WINRM:
			scriptFileName = POWERSHELL_STARTUP_SCRIPT_NAME + "";
			// scriptFileName = "bootstrap-management.ps1";
			break;
		case SSH:
			scriptFileName = LINUX_STARTUP_SCRIPT_NAME;
			break;
		default:
			throw new UnsupportedOperationException("Remote Execution Mode: "
					+ details.getRemoteExecutionMode() + " not supported");
		}
		return scriptFileName;
	}

	private void uploadFilesToServer(final InstallationDetails details,
			final long end, final String targetHost) throws TimeoutException,
			InstallerException, InterruptedException {
		try {
			final Set<String> excludedFiles = new HashSet<String>();
			if (!details.isLus() && details.getManagementOnlyFiles() != null) {
				excludedFiles.addAll(Arrays.asList(details
						.getManagementOnlyFiles()));
			}
			copyFiles(details, targetHost, excludedFiles,
					Utils.millisUntil(end), TimeUnit.MILLISECONDS);
		} catch (final FileSystemException e) {
			throw new InstallerException(
					"Uploading files to remote server failed.", e);
		} catch (final IOException e) {
			throw new InstallerException(
					"Uploading files to remote server failed.", e);
		} catch (final URISyntaxException e) {
			throw new InstallerException(
					"Uploading files to remote server failed.", e);
		}
	}

	private List<String> getPowershellCommandLine(final String target,
			final String username, final String password, final String command,
			final String localDir) throws FileNotFoundException {

		final File clientScriptFile = new File(localDir,
				POWERSHELL_CLIENT_SCRIPT);
		if (!clientScriptFile.exists()) {
			throw new FileNotFoundException(
					"Could not find expected powershell client script in local directory. Was expecting file: "
							+ clientScriptFile.getAbsolutePath());
		}
		final String[] commandLineParts = { "powershell.exe", "-inputformat",
				"none", "-File", clientScriptFile.getAbsolutePath(), "-target",
				target, "-password", quoteString(password), "-username",
				quoteString(username), "-command", quoteString(command) };

		return Arrays.asList(commandLineParts);
	}

	private String quoteString(final String input) {
		return "\"" + input + "\"";
	}

	private void powershellCommand(final String targetHost,
			final String command, final String username, final String password,
			final String keyFile, final long millisUntil,
			final TimeUnit milliseconds, final String localDir)
			throws InstallerException, InterruptedException, TimeoutException {
		logger.fine("Executing: " + command + " on: " + targetHost);

		logger.fine("Checking if powershell is installed");
		try {
			checkPowershellInstalled();
		} catch (final IOException e) {
			throw new InstallerException(
					"Error while trying to find powershell.exe", e);
		}

		logger.fine("Checking WinRM Connection");
		checkConnection(targetHost, POWERSHELL_PORT, millisUntil, milliseconds);

		logger.fine("Executing remote command");
		try {
			invokeRemotePowershellCommand(targetHost, command, username,
					password, localDir);
		} catch (final FileNotFoundException e) {
			throw new InstallerException(
					"Failed to invoke remote powershell command", e);
		}

	}

	private String invokeRemotePowershellCommand(final String targetHost,
			final String command, final String username, final String password,
			final String localDir) throws InstallerException,
			InterruptedException, FileNotFoundException {

		final List<String> fullCommand = getPowershellCommandLine(targetHost,
				username, password, command, localDir);

		final ProcessBuilder pb = new ProcessBuilder(fullCommand);
		pb.redirectErrorStream(true);

		try {
			final Process p = pb.start();

			final String output = readProcessOutput(p);
			final int exitCode = p.waitFor();
			if (exitCode != 0) {
				throw new InstallerException(
						"Remote installation failed with exit code: "
								+ exitCode + ". Execution output: " + output);
			}
			return output;
		} catch (final IOException e) {
			throw new InstallerException(
					"Failed to invoke remote installation: " + e.getMessage(),
					e);
		}
	}

	private void checkPowershellInstalled() throws IOException,
			InterruptedException, InstallerException {
		if (powerShellInstalled != null) {
			if (powerShellInstalled.booleanValue()) {
				return;
			} else {
				throw new InstallerException(
						"powershell.exe is not on installed, or is not available on the system path. "
								+ "Powershell is required on both client and server for Cloudify to work on Windows. ");
			}
		}

		logger.fine("Checking if powershell is installed using: "
				+ Arrays.toString(POWERSHELL_INSTALLED_COMMAND));
		final ProcessBuilder pb = new ProcessBuilder(
				Arrays.asList(POWERSHELL_INSTALLED_COMMAND));
		pb.redirectErrorStream(true);

		final Process p = pb.start();

		final String output = readProcessOutput(p);

		logger.fine("Finished reading output");
		final int retval = p.waitFor();
		logger.fine("Powershell installed command exit value: " + retval);
		if (retval != 0) {
			throw new InstallerException(
					"powershell.exe is not on installed, or is not available on the system path. "
							+ "Powershell is required on both client and server for Cloudify to work on Windows. "
							+ "Execution result: " + output);
		}

		powerShellInstalled = Boolean.TRUE;
	}

	private String readProcessOutput(final Process p) throws IOException {
		final StringBuilder sb = new StringBuilder();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		try {
			final String newline = System.getProperty("line.separator");
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				this.publishEvent("powershell_output_line", line);
				logger.fine(line);
				sb.append(line).append(newline);
			}

		} finally {
			try {
				reader.close();
			} catch (final IOException e) {
				logger.log(
						Level.SEVERE,
						"Error while closing process input stream: "
								+ e.getMessage(), e);

			}

		}
		return sb.toString();
	}

	private void sshCommand(final String host, final String command,
			final String username, final String password, final String keyFile,
			final long timeout, final TimeUnit unit) throws InstallerException,
			TimeoutException {

		try {
			Utils.executeSSHCommand(host, command, username, password, keyFile,
					timeout, unit);
		} catch (final BuildException e) {
			// There really should be a better way to check that this is a
			// timeout
			logger.log(
					Level.FINE,
					"The remote boostrap command failed with error: "
							+ e.getMessage()
							+ ". The command that failed to execute is : "
							+ command, e);

			if (e instanceof BuildTimeoutException) {
				final TimeoutException ex = new TimeoutException(
						"Remote bootstrap command failed to execute: "
								+ e.getMessage());
				ex.initCause(e);
				throw ex;
			} else if (e instanceof ExitStatusException) {
				final ExitStatusException ex = (ExitStatusException) e;
				final int ec = ex.getStatus();
				throw new InstallerException(
						"Remote bootstrap command failed with exit code: " + ec,
						e);
			} else {
				throw new InstallerException(
						"Remote bootstrap command failed to execute.", e);
			}
		}

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
	 * This method is public so that implementation classes for file copy and
	 * remote execution can publish events.
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
				logger.log(
						Level.FINE,
						"Exception in listener while publishing event: "
								+ eventName + " with arguments: "
								+ Arrays.toString(args), e);
			}
		}
	}

}
