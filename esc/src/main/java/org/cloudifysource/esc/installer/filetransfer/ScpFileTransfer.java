/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.esc.installer.filetransfer;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.method.AuthNone;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.LocalFileFilter;
import net.schmizz.sshj.xfer.LocalSourceFile;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import net.schmizz.sshj.xfer.scp.SCPUploadClient;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;

/********
 * A file transfer implementation using Secure Copy (SCP), based on the sshj library.
 * 
 * @author barakme
 * @since 2.5.0
 * 
 */
public class ScpFileTransfer implements FileTransfer {

	private static final String CREATE_REMOTE_DIRECTORY_WITH_DELETE =
			"if [ -d {0} ]; then rm -rf {0}; fi; mkdir -p {0}";
	private static final String CREATE_REMOTE_DIRECTORY = "if [ ! -d {0} ]; then mkdir -p {0}; fi";

	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ScpFileTransfer.class
			.getName());

	private String host;

	private String localDirPath;

	private boolean deleteRemoteDirectoryContents;

	@Override
	public void copyFiles(final InstallationDetails details, final Set<String> excludedFiles,
			final List<File> additionalFiles,
			final long endTimeMillis) throws TimeoutException, InstallerException {
		final SSHClient ssh = new SSHClient();
		ssh.addHostKeyVerifier(new PromiscuousVerifier());
		try {
			ssh.connect(host);
		} catch (final IOException e) {
			try {
				ssh.close();
			} catch (IOException e1) {
				logger.log(Level.SEVERE, "Failed to close down ssh client after connection to: " + host
						+ " failed. Error was: " + e.getMessage(), e);
			}
			throw new InstallerException("Failed to connect to host: " + host + ": " + e.getMessage(), e);
		}

		try {

			try {
				if (!StringUtils.isEmpty(details.getKeyFile())) {
					final File keyFile = new File(details.getKeyFile());
					if (!keyFile.exists() || !keyFile.isFile()) {
						throw new InstallerException("Expected to find key file at: " + keyFile.getAbsolutePath());
					}
					ssh.authPublickey(details.getUsername(), keyFile.getAbsolutePath());
				} else if (!StringUtils.isEmpty(details.getPassword())) {
					ssh.authPassword(details.getUsername(), details.getPassword());
				} else {
					ssh.auth(details.getUsername(), new AuthNone());
				}
			} catch (final TransportException e) {
				throw new InstallerException("Failed to authenticate to remote server: " + e.getMessage(), e);
			} catch (final UserAuthException e) {
				throw new InstallerException("Failed to authenticate to remote server: " + e.getMessage(), e);
			}

			// First, we need to create the remote directory

			createRemoteDirectory(details, endTimeMillis, ssh);

			// Note: With SCP, we do not delete the contents of the remote directory! Use SFTP if y ou want this
			// feature.
			final SCPFileTransfer transfer = ssh.newSCPFileTransfer();
			try {
				final SCPUploadClient uploadClient = transfer.newSCPUploadClient();
				final File localDirectory = new File(this.localDirPath);
				final String localDirectoryPath = localDirectory.getCanonicalPath();

				// Filter out excluded files
				uploadClient.setUploadFilter(new LocalFileFilter() {

					@Override
					public boolean accept(final LocalSourceFile originalFile) {

						final FileSystemFile file = (FileSystemFile) originalFile;
						final File localFile = file.getFile();
						String localFilePath;
						try {
							localFilePath = localFile.getCanonicalPath();
						} catch (final IOException e) {
							throw new IllegalStateException("Failed to get canonical path of: " + localFile, e);
						}

						if (!localFilePath.startsWith(localDirectoryPath)) {
							throw new IllegalStateException("Upload candidate file: " + localFilePath
									+ " is not a descendant of local directory: " + localDirectoryPath);
						}

						String relativePath = localFilePath.substring(localDirectoryPath.length());

						// remote trailing separator
						if (relativePath.startsWith(File.separator)) {
							relativePath = relativePath.substring(1);
						}

						// convert windows separator to linux
						relativePath = relativePath.replace("\\", "/");
						
						// this condition will never be satisfied and should be removed.
						if (excludedFiles.contains(relativePath)) {
							return false;
						}
						
						return true;
					}
				});

				// uploading local directory to remote directory
				File[] files = localDirectory.listFiles();
				if (files != null) {
					for (File file : files) {
						if (!excludedFiles.contains(file.getName())) {
							uploadClient.copy(new FileSystemFile(file), details.getRemoteDir());
						}
					}
				}

				// uploading additional files to remote directory
				for (File file : additionalFiles) {
					transfer.upload(new FileSystemFile(file), details.getRemoteDir());
				}
			} catch (final IOException e) {
				throw new InstallerException("Failed to upload files to remote server: " + e.getMessage(), e);
			}
		} finally {
			try {
				ssh.disconnect();
			} catch (final IOException e) {
				logger.log(Level.WARNING, "Failed to disconnect ssh session", e);
			}
		}
	}

	private void createRemoteDirectory(final InstallationDetails details, final long endTimeMillis, final SSHClient ssh)
			throws InstallerException {
		Session session = null;
		try {
			session = ssh.startSession();
			session.allocateDefaultPTY();
			final long timeout = endTimeMillis - System.currentTimeMillis();
			String commandString = createCommandString(details);
			Command command = session.exec(commandString);
			final String commandOutput = IOUtils.readFully(command.getInputStream()).toString();
			command.join((int) timeout, TimeUnit.MILLISECONDS);
			Integer exitStatus = command.getExitStatus();
			if (exitStatus == null) {
				throw new InstallerException("Remote command to create directory did not return");
			} else {
				if (exitStatus != 0) {
					throw new InstallerException("Failed to create remote directory: " + details.getRemoteDir()
							+ ". Command output was: " + commandOutput);
				}
			}
		} catch (IOException e) {
			throw new InstallerException("Failed to create remote directory: " + details.getRemoteDir()
					+ ". Error was: " + e.getMessage(), e);
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (TransportException e) {
					logger.log(Level.WARNING, "Failed to close ssh session while creating remote directory", e);
				} catch (ConnectionException e) {
					logger.log(Level.WARNING, "Failed to close ssh session while creating remote directory", e);
				}
			}
		}
	}

	private String createCommandString(final InstallationDetails details) {
		if (this.deleteRemoteDirectoryContents) {
			return MessageFormat.format(CREATE_REMOTE_DIRECTORY_WITH_DELETE,
					details.getRemoteDir());
		} else {
			return MessageFormat.format(CREATE_REMOTE_DIRECTORY,
					details.getRemoteDir());
		}
	}

	@Override
	public void initialize(final InstallationDetails details, final long endTimeMillis) throws TimeoutException,
			InstallerException {
		// TODO - this code should be in the installer, and not copied in each of the file transfer implementations!
		this.deleteRemoteDirectoryContents = details.isDeleteRemoteDirectoryContents();
		if (details.isConnectedToPrivateIp()) {
			host = details.getPrivateIp();
		} else {
			host = details.getPublicIp();
		}

		// when bootstrapping a management machine, pass all of the cloud
		// configuration, including all template
		// for an agent machine, just pass the upload dir fot the specific
		// template.

		localDirPath = details.getLocalDir();
		if (details.isManagement()) {
			if (details.getCloudFile() == null) {
				throw new IllegalArgumentException("While bootstrapping a management machine, cloud file is null");
			}

			localDirPath = details.getCloudFile().getParentFile().getAbsolutePath();

		}
	}

}
