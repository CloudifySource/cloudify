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

package org.cloudifysource.esc.installer.filetransfer;

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.vfs2.VFS;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;

/*********
 * A windows CIFS file transfer implementation.
 * @author barakme
 * @since 2.5.0
 *
 */
public class CifsFileTransfer extends VfsFileTransfer {

	private static final int CIFS_PORT = 445;

	@Override
	protected void initVFSManager(final InstallationDetails details, final long endTimeMillis)
			throws InstallerException {
		try {
			AgentlessInstaller.checkConnection(host, CIFS_PORT, endTimeMillis - System.currentTimeMillis(),
					TimeUnit.MILLISECONDS);
			fileSystemManager = VFS.getManager();
		} catch (final Exception e) {
			throw new InstallerException("Failed to set up file transfer: " + e.getMessage(), e);

		}

	}

	@Override
	protected void createTargetURI(final InstallationDetails details)
			throws InstallerException {
		try {
			targetURI =
					new java.net.URI("smb", details.getUsername() + ":" + details.getPassword(), host, CIFS_PORT,
							details.getRemoteDir(), null, null)
			.toASCIIString();
		} catch (final URISyntaxException e) {
			throw new InstallerException("Failed to initialize file transfer: " + e.getMessage(), e);
		}

	}

}
