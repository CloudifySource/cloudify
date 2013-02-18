/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
import java.net.URISyntaxException;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.util.Utils;

/*******
 * An sftp based file transfer implementation.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
public class SftpFileTransfer extends VfsFileTransfer implements FileTransfer {

	@Override
	protected void initVFSManager(final InstallationDetails details, final long endTimeMillis)
			throws InstallerException {
		try {
			this.opts = new FileSystemOptions();

			SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");

			SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);

			final Object preferredAuthenticationMethods =
					details.getCustomData().get(
							CloudifyConstants.INSTALLER_CUSTOM_DATA_SFTP_PREFERRED_AUTHENTICATION_METHODS_KEY);

			if (preferredAuthenticationMethods != null) {
				if (String.class.isInstance(preferredAuthenticationMethods)) {

					SftpFileSystemConfigBuilder.getInstance().setPreferredAuthentications(opts,
							(String) preferredAuthenticationMethods);
				} else {
					throw new IllegalArgumentException("Was expecti`ng a string value for custom data field '"
							+ CloudifyConstants.INSTALLER_CUSTOM_DATA_SFTP_PREFERRED_AUTHENTICATION_METHODS_KEY
							+ "', got a; " + preferredAuthenticationMethods.getClass().getName());
				}
			}

			final String keyFile = details.getKeyFile();

			if (keyFile != null && !keyFile.isEmpty()) {
				final File temp = new File(keyFile);
				if (!temp.exists()) {
					throw new InstallerException("Could not find key file: " + temp + ". KeyFile " + keyFile
							+ " that was passed in the installation Details does not exist");
				}
				SftpFileSystemConfigBuilder.getInstance().setIdentities(opts, new File[] { temp });
			}

			SftpFileSystemConfigBuilder.getInstance().setTimeout(opts,
					installerConfiguration.getFileTransferConnectionTimeoutMillis());
			this.fileSystemManager = VFS.getManager();
		} catch (final FileSystemException e) {
			throw new InstallerException("Failed to set up file transfer: " + e.getMessage(), e);

		}
	}

	@Override
	protected void createTargetURI(final InstallationDetails details)
			throws InstallerException {
		final String userDetails;
		if (details.getPassword() != null && !details.getPassword().isEmpty()) {
			userDetails = details.getUsername() + ":" + details.getPassword();
		} else {
			userDetails = details.getUsername();
		}

		try {
			final int port = Utils.getFileTransferPort(this.installerConfiguration, FileTransferModes.SFTP);
			targetURI =
					new java.net.URI("sftp", userDetails, host, port,
							details.getRemoteDir(), null, null)
							.toASCIIString();
		} catch (final URISyntaxException e) {
			throw new InstallerException("Failed to set up file transfer: " + e.getMessage(), e);

		}
	}
}
