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

import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;

public abstract class VfsFileTransfer implements FileTransfer {

	protected static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SftpFileTransfer.class
			.getName());
	protected static final int SFTP_DISCONNECT_DETECTION_TIMEOUT_MILLIS = 10 * 1000;
	protected static final int SSH_PORT = 22;
	protected FileSystemManager fileSystemManager;
	protected FileObject localDir;
	protected FileObject remoteDir;
	protected String host;
	protected String targetURI;
	protected FileSystemOptions opts;

	protected void checkTimeout(final long endTimeMillis)
			throws TimeoutException {
		if (System.currentTimeMillis() > endTimeMillis) {
			throw new TimeoutException("File transfer operation exceeded timeout");
		}
	}

	public void shutdown() {
		fileSystemManager.closeFileSystem(remoteDir.getFileSystem());
		fileSystemManager.closeFileSystem(localDir.getFileSystem());

	}

	@Override
	public void copyFiles(final InstallationDetails details, final String sourceFile, final String targetFile,
			final Set<String> excludedFiles, final long endTimeMillis)
			throws TimeoutException, InstallerException {

		logger.fine("Copying files to: " + host + " from local dir: " + localDir.getName().getPath() + " excluding "
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
							fileSystemManager.resolveFile(remoteDir,
									localDir.getName().getRelativeName(fileInfo.getFile().getName()));

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

			// if (cloudFile != null) {
			// // copy cloud file too
			// final FileObject cloudFileParentObject = mng.resolveFile(cloudFile.getParentFile().getAbsolutePath());
			// final FileObject cloudFileObject = mng.resolveFile(cloudFile.getAbsolutePath());
			// remoteDir.copyFrom(cloudFileParentObject, new FileSelector() {
			//
			// @Override
			// public boolean traverseDescendents(final FileSelectInfo fileInfo)
			// throws Exception {
			// return true;
			// }
			//
			// @Override
			// public boolean includeFile(final FileSelectInfo fileInfo)
			// throws Exception {
			// return fileInfo.getFile().equals(cloudFileObject);
			//
			// }
			// });
			// }

			logger.fine("Copying files to: " + host + " completed.");
		} catch (final FileSystemException e) {
			throw new InstallerException("Failed to copy files to remote host " + host + ": " + e.getMessage(), e);

		}
		checkTimeout(endTimeMillis);

	}

	@Override
	public void initialize(final InstallationDetails details, final long endTimeMillis)
			throws TimeoutException, InstallerException {
		if (details.isConnectedToPrivateIp()) {
			host = details.getPrivateIp();
		} else {
			host = details.getPublicIp();
		}

		checkTimeout(endTimeMillis);

		initVFSManager(details, endTimeMillis);

		createTargetURI(details);

		final FileSystemManager mng = fileSystemManager;

		mng.setLogger(org.apache.commons.logging.LogFactory.getLog(logger.getName()));
		try {
			localDir = mng.resolveFile("file:" + details.getLocalDir());
			remoteDir = mng.resolveFile(targetURI, opts);

		} catch (final FileSystemException e) {
			throw new InstallerException("Failed to set up file transfer: " + e.getMessage(), e);
		}

	}

	protected abstract void initVFSManager(final InstallationDetails details, final long endTimeMillis)
			throws InstallerException;

	protected abstract void createTargetURI(InstallationDetails details)
			throws InstallerException;

}
