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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
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
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.util.ShellCommandBuilder;
import org.cloudifysource.esc.util.Utils;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioningException;

/************
 * The agentless installer class is responsible for installing gigaspaces on a remote machine, using only ssh. It will
 * upload all relevant files and start the gigaspaces agent.
 * 
 * File transfer is handled using apache commons vfs.
 * 
 * @author barakme
 * 
 */
public class AgentlessInstaller {

	// private static final int LOCAL_FILE_BUFFER_SIZE = 1024;

	private static final int CIFS_PORT = 445;

	private static final int DEFAULT_ROUTE_RESOLUTION_TIMEOUT = 2 * 60 * 1000; // 2 minutes

	private static final String LINUX_STARTUP_SCRIPT_NAME = "bootstrap-management.sh";
	private static final String POWERSHELL_STARTUP_SCRIPT_NAME = "bootstrap-management.bat";

	private static final String CLOUDIFY_LINK_ENV = "CLOUDIFY_LINK";
	private static final String CLOUDIFY_OVERRIDES_LINK_ENV = "CLOUDIFY_OVERRIDES_LINK";

	private static final String MACHINE_ZONES_ENV = "MACHINE_ZONES";

	private static final String MACHINE_IP_ADDRESS_ENV = "MACHINE_IP_ADDRESS";

	private static final String GSA_MODE_ENV = "GSA_MODE";

	private static final String NO_WEB_SERVICES_ENV = "NO_WEB_SERVICES";

	private static final String LUS_IP_ADDRESS_ENV = "LUS_IP_ADDRESS";

	private static final String WORKING_HOME_DIRECTORY_ENV = "WORKING_HOME_DIRECTORY";

	private static final String CLOUD_FILE = "CLOUD_FILE";

	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AgentlessInstaller.class
			.getName());

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

	private static InetAddress waitForRoute(final String ip, final long endTime)
			throws InstallerException, InterruptedException {
		Exception lastException = null;
		while (System.currentTimeMillis() < endTime) {

			try {
				final InetAddress inetAddress = InetAddress.getByName(ip);
				return inetAddress;
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
	 * @param ip remote machine ip.
	 * @param port remote machine port.
	 * @param timeout duration to wait for successful connection.
	 * @param unit time unit to wait.
	 * @throws InstallerException .
	 * @throws InstallerException .
	 * @throws TimeoutException .
	 * @throws InterruptedException .
	 * @throws ElasticMachineProvisioningException
	 */
	public static void checkConnection(final String ip, final int port, final long timeout, final TimeUnit unit)
			throws TimeoutException, InterruptedException, InstallerException {

		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		final InetAddress inetAddress =
				waitForRoute(ip, Math.min(end, System.currentTimeMillis() + DEFAULT_ROUTE_RESOLUTION_TIMEOUT));
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

	/****
	 * Copies files from local dir to remote dir.
	 * 
	 * @param host host name or ip address of remote machine.
	 * @param username ssh username of remote machine.
	 * @param password ssh password of remote machine.
	 * @param srcDir local directory.
	 * @param toDir remote directory.
	 * @param keyFile The key file of the remote machine, if used. private key file.
	 * @param excludedFile Files that should not be copied.
	 * @param cloudFile The cloud file.
	 * @param timeout Time before timeout is thrown.
	 * @param unit Time unit, relevant to timeout parameter.
	 * @param fileTransferMode Remote file system type.
	 * @throws IOException in case of an error during file transfer.
	 * @throws TimeoutException
	 * @throws URISyntaxException
	 */
	private void copyFiles(final String host, final String username, final String password, final String srcDir,
			final String toDir, final String keyFile, final Set<String> excludedFiles, final File cloudFile,
			final long timeout, final TimeUnit unit, final FileTransferModes fileTransferMode)
			throws IOException, TimeoutException, URISyntaxException {

		if (timeout < 0) {
			throw new TimeoutException("Uploading files to host " + host + " timed out");
		}
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		final FileSystemOptions opts = new FileSystemOptions();

		FileSystemManager createdManager = null;
		String target = null;
		switch (fileTransferMode) {
		case SCP:
			createdManager = createRemoteSSHFileSystem(keyFile, opts);

			final String userDetails;
			if (password != null && password.length() > 0) {
				userDetails = username + ":" + password;
			} else {
				userDetails = username;
			}
			target = new java.net.URI("sftp", userDetails, host, SSH_PORT, toDir, null, null).toASCIIString();
			break;
		case CIFS:
			createdManager = VFS.getManager();

			target =
					new java.net.URI("smb", username + ":" + password, host, CIFS_PORT, toDir, null, null)
							.toASCIIString();

			break;
		default:
			throw new UnsupportedOperationException("Unsupported Remote File System: " + fileTransferMode.toString());
		}

		final FileSystemManager mng = createdManager;

		mng.setLogger(org.apache.commons.logging.LogFactory.getLog(logger.getName()));
		final FileObject localDir = mng.resolveFile("file:" + srcDir);

		final FileObject remoteDir = mng.resolveFile(target, opts);

		logger.fine("Copying files to: " + target + " from local dir: " + localDir.getName().getPath() + " excluding "
				+ excludedFiles.toString());

		try {
			remoteDir.copyFrom(localDir, new FileSelector() {

				@Override
				public boolean includeFile(final FileSelectInfo fileInfo)
						throws Exception {
					if (excludedFiles.contains(fileInfo.getFile().getName().getBaseName())) {
						logger.fine(fileInfo.getFile().getName().getBaseName() + " excluded");
						return false;

					}
					final FileObject remoteFile =
							mng.resolveFile(remoteDir, localDir.getName().getRelativeName(fileInfo.getFile().getName()));
					if (!remoteFile.exists()) {
						logger.fine(fileInfo.getFile().getName().getBaseName() + " missing on server");
						return true;
					}

					if (fileInfo.getFile().getType() == FileType.FILE) {
						final long remoteSize = remoteFile.getContent().getSize();
						final long localSize = fileInfo.getFile().getContent().getSize();
						final boolean res = localSize != remoteSize;
						if (res) {
							logger.fine(fileInfo.getFile().getName().getBaseName() + " different on server");
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

			if (cloudFile != null) {
				// copy cloud file too
				final FileObject cloudFileParentObject = mng.resolveFile(cloudFile.getParentFile().getAbsolutePath());
				final FileObject cloudFileObject = mng.resolveFile(cloudFile.getAbsolutePath());
				remoteDir.copyFrom(cloudFileParentObject, new FileSelector() {

					@Override
					public boolean traverseDescendents(final FileSelectInfo fileInfo)
							throws Exception {
						return true;
					}

					@Override
					public boolean includeFile(final FileSelectInfo fileInfo)
							throws Exception {
						return fileInfo.getFile().equals(cloudFileObject);

					}
				});
			}
			// publishEvent("access_vm_with_ssh_success");
			logger.fine("Copying files to: " + target + " completed.");
		} finally {
			mng.closeFileSystem(remoteDir.getFileSystem());
			mng.closeFileSystem(localDir.getFileSystem());
		}

		if (end < System.currentTimeMillis()) {
			throw new TimeoutException("Uploading files to host " + host + " timed out");
		}
	}

	private FileSystemManager createRemoteSSHFileSystem(final String keyFile, final FileSystemOptions opts)
			throws FileSystemException, FileNotFoundException {
		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");

		SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);

		if (keyFile != null && keyFile.length() > 0) {
			final File temp = new File(keyFile);
			if (!temp.exists()) {
				throw new FileNotFoundException("Could not find key file: " + temp + ". KeyFile " + keyFile
						+ " that was passed in the installation Details does not exist");
			}
			SftpFileSystemConfigBuilder.getInstance().setIdentities(opts, new File[] { temp });
		}

		SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, SFTP_DISCONNECT_DETECTION_TIMEOUT_MILLIS);
		final FileSystemManager mng = VFS.getManager();
		return mng;
	}

	/******
	 * Performs installation on a remote machine with a known IP.
	 * 
	 * @param details the installation details.
	 * @param timeout the timeout duration.
	 * @param unit the timeout unit.
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

		final String targetHost = details.isConnectedToPrivateIp() ? details.getPrivateIp() : details.getPublicIp();

		publishEvent("attempting_to_access_vm_with_ssh", targetHost);
		int port = 0;
		switch (details.getFileTransferMode()) {
		case CIFS:
			port = CIFS_PORT;
			break;
		case SCP:
			port = SSH_PORT;
			break;
		default:
			throw new UnsupportedOperationException("File Transfer Mode: " + details.getFileTransferMode()
					+ " not supported");
		}

		checkConnection(targetHost, port, Utils.millisUntil(end), TimeUnit.MILLISECONDS);

		// upload bootstrap files
		uploadFilesToServer(details, end, targetHost);

		// launch the cloudify agent
		remoteExecuteAgentOnServer(details, end, targetHost);

		publishEvent("access_vm_with_ssh_success", details.getPublicIp());

	}

	private void remoteExecuteAgentOnServer(final InstallationDetails details, final long end, final String targetHost)
			throws InstallerException, TimeoutException, InterruptedException {

		// get script for execution mode
		final String scriptFileName = getScriptFileName(details);

		String remoteDirectory = details.getRemoteDir();
		if (remoteDirectory.endsWith("/")) {
			remoteDirectory = remoteDirectory.substring(0, remoteDirectory.length() - 1);
		}
		final String scriptPath = remoteDirectory + "/" + scriptFileName;

		final ShellCommandBuilder scb =
				new ShellCommandBuilder(details.getRemoteExecutionMode())
						.exportVar(LUS_IP_ADDRESS_ENV, details.getLocator())
						.exportVar(GSA_MODE_ENV, details.isLus() ? "lus" : "agent")
						.exportVar(NO_WEB_SERVICES_ENV, details.isNoWebServices() ? "true" : "false")
						.exportVar(MACHINE_IP_ADDRESS_ENV,
								details.isBindToPrivateIp() ? details.getPrivateIp() : details.getPublicIp())
						.exportVar(MACHINE_ZONES_ENV, details.getZones())
						.exportVar(CLOUDIFY_LINK_ENV,
								details.getCloudifyUrl() != null ? "\"" + details.getCloudifyUrl() + "\"" : "")
						.exportVar(CLOUDIFY_OVERRIDES_LINK_ENV,
								details.getOverridesUrl() != null ? "\"" + details.getOverridesUrl() + "\"" : "")
						.exportVar(WORKING_HOME_DIRECTORY_ENV, remoteDirectory)
						.exportVar(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP, details.getPrivateIp())
						.exportVar(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP, details.getPublicIp());

		if (details.isLus()) {
			String remotePath = details.getRemoteDir();
			if (!remotePath.endsWith("/")) {
				remotePath += "/";
			}
			scb.exportVar(CLOUD_FILE, remotePath + details.getCloudFile().getName());
		}
		scb.chmodExecutable(scriptPath).call(scriptPath);

		final String command = scb.toString();

		logger.fine("Calling startup script on target: " + targetHost + " with LOCATOR=" + details.getLocator()
				+ "\nThis may take a few minutes");

		switch (details.getRemoteExecutionMode()) {
		case SSH:
			sshCommand(targetHost, command, details.getUsername(), details.getPassword(), details.getKeyFile(),
					Utils.millisUntil(end), TimeUnit.MILLISECONDS);

			break;
		case WINRM:
			powershellCommand(targetHost, command, details.getUsername(), details.getPassword(), details.getKeyFile(),
					Utils.millisUntil(end), TimeUnit.MILLISECONDS);
			break;
		default:
			throw new UnsupportedOperationException();
		}
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

	private void uploadFilesToServer(final InstallationDetails details, final long end, final String targetHost)
			throws TimeoutException, InstallerException {
		try {
			final Set<String> excludedFiles = new HashSet<String>();
			if (!details.isLus() && details.getManagementOnlyFiles() != null) {
				excludedFiles.addAll(Arrays.asList(details.getManagementOnlyFiles()));
			}
			copyFiles(targetHost, details.getUsername(), details.getPassword(), details.getLocalDir(),
					details.getRemoteDir(), details.getKeyFile(), excludedFiles, details.getCloudFile(),
					Utils.millisUntil(end), TimeUnit.MILLISECONDS, details.getFileTransferMode());
		} catch (final FileSystemException e) {
			logger.info(Arrays.toString(e.getStackTrace()));
			throw new InstallerException("Uploading files to remote server failed.", e);
		} catch (final IOException e) {
			logger.info(Arrays.toString(e.getStackTrace()));
			throw new InstallerException("Uploading files to remote server failed.", e);
		} catch (final URISyntaxException e) {
			logger.info(Arrays.toString(e.getStackTrace()));
			throw new InstallerException("Uploading files to remote server failed.", e);
		}
	}

	private static final String POWER_SHELL_COMMAND_TEMPLATE = "$ErrorActionPreference=\"Stop\"\n"
			+ "$securePassword = ConvertTo-SecureString -AsPlainText -Force \"%s\"\n"
			+ "$cred = New-Object System.Management.Automation.PSCredential \"%s\", $securePassword\n"
			+ "Invoke-Command -ComputerName %s -Credential $cred -ScriptBlock { %s }\n"
			+ "Write-Host \"Command finished\"";
	private static final String[] POWER_SHELL_PREFIX = { "powershell.exe", "-inputformat", "none", "-File" };

	private void powershellCommand(final String targetHost, final String command, final String username,
			final String password, final String keyFile, final long millisUntil, final TimeUnit milliseconds)
			throws InstallerException, InterruptedException {
		try {
			checkPowershellInstalled();
		} catch (final IOException e) {
			throw new InstallerException("Error while trying to find powershell.exe", e);
		}

		final String formattedCommand =
				String.format(POWER_SHELL_COMMAND_TEMPLATE, password, username, targetHost, command);

		File tempScriptFile = null;
		try {
			tempScriptFile = createTempScriptFile(formattedCommand);
		} catch (final IOException e) {
			throw new InstallerException("Failed to create script file", e);
		}

		final List<String> fullCommand = new LinkedList<String>();
		fullCommand.addAll(Arrays.asList(POWER_SHELL_PREFIX));
		fullCommand.add(tempScriptFile.getAbsolutePath());

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
		} catch (final IOException e) {
			throw new InstallerException("Failed to invoke remote installation: " + e.getMessage(), e);
		} finally {
			// delete the temp script
			// TODO - instead of writing the password to the file, pass it as an argument to the script.
			tempScriptFile.delete();
		}

	}

	private File createTempScriptFile(final String command)
			throws IOException {
		final File tempFile = File.createTempFile("InstallationScript", ".ps1");
		tempFile.deleteOnExit();
		FileUtils.writeStringToFile(tempFile, command);
		return tempFile;
	}

	private void checkPowershellInstalled()
			throws IOException, InterruptedException, InstallerException {
		final ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-?");
		pb.redirectErrorStream(true);

		final Process p = pb.start();

		final String output = readProcessOutput(p);

		final int retval = p.waitFor();
		if (retval != 0) {
			throw new InstallerException("powershell.exe is not on installed, or is not available on the system path. "
					+ "Powershell is required on both client and server for Cloudify to work on Windows. "
					+ "Execution result: " + output);
		}
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
				logger.fine(line);
				sb.append(line).append(newline);
			}

		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (final IOException e) {
				logger.log(Level.SEVERE, "Error while closingprocess input stream: " + e.getMessage(), e);

			}

		}
		return sb.toString();
	}

	private void sshCommand(final String host, final String command, final String username, final String password,
			final String keyFile, final long timeout, final TimeUnit unit)
			throws InstallerException, TimeoutException {

		try {
			Utils.executeSSHCommand(host, command, username, password, keyFile, timeout, unit);
		} catch (final BuildException e) {
			// There really should be a better way to check that this is a
			// timeout
			if (e instanceof BuildTimeoutException) {
				final TimeoutException ex =
						new TimeoutException("Command " + command + " failed to execute: " + e.getMessage());
				ex.initCause(e);
				throw ex;
			} else if (e instanceof ExitStatusException) {
				final ExitStatusException ex = (ExitStatusException) e;
				final int ec = ex.getStatus();
				throw new InstallerException("Command " + command + " failed with exit code: " + ec, e);
			} else {
				throw new InstallerException("Command " + command + " failed to execute.", e);
			}
		}

		publishEvent("access_vm_with_ssh_success", host);

	}

	/**********
	 * Registers an event listener for installation events.
	 * 
	 * @param listener the listener.
	 */
	public void addListener(final AgentlessInstallerListener listener) {
		this.eventsListenersList.add(listener);
	}

	private void publishEvent(final String eventName, final Object... args) {
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
