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
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileDepthSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.cloudifysource.domain.cloud.CloudTemplateInstallerConfiguration;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;

/*********
 * A base class for commons-vfs based file transfer.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
public abstract class VfsFileTransfer implements FileTransfer {

	protected static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(VfsFileTransfer.class
			.getName());

	protected FileSystemManager fileSystemManager;
	protected FileObject localDir;
	protected FileObject remoteDir;
	protected String host;
	protected String targetURI;
	protected FileSystemOptions opts;

	protected boolean deleteRemoteDirectoryContents = false;

	protected CloudTemplateInstallerConfiguration installerConfiguration;

	/******
	 * Checks if the specified end time has reached.
	 *
	 * @param endTimeMillis
	 *            the end time.
	 * @throws TimeoutException
	 *             if the target time has passed.
	 */
	protected void checkTimeout(final long endTimeMillis)
			throws TimeoutException {
		if (System.currentTimeMillis() > endTimeMillis) {
			throw new TimeoutException("File transfer operation exceeded timeout");
		}
	}

	/****
	 * Closes the local and remote file system.
	 */
	public void shutdown() {
		fileSystemManager.closeFileSystem(remoteDir.getFileSystem());
		fileSystemManager.closeFileSystem(localDir.getFileSystem());

	}

	@Override
	public void copyFiles(final InstallationDetails details,
			final Set<String> excludedFiles, final List<File> additionalFiles, final long endTimeMillis)
			throws TimeoutException, InstallerException {

		logger.fine("Copying files to: " + host + " from local dir: " + localDir.getName().getPath() + " excluding "
				+ excludedFiles.toString());

		try {

			if (remoteDir.exists()) {
				FileType type = remoteDir.getType();
				if (!type.equals(FileType.FOLDER)) {
					throw new InstallerException("The remote location: " + remoteDir.getName().getFriendlyURI()
							+ " exists but is not a directory");
				}

				if (deleteRemoteDirectoryContents) {
					logger.info("Deleting contents of remote directory: " + remoteDir.getName().getFriendlyURI());
					remoteDir.delete(new FileDepthSelector(1, Integer.MAX_VALUE));
				}
				FileObject[] children = remoteDir.getChildren();
				if (children.length > 0) {

					throw new InstallerException("The remote directory: " + remoteDir.getName().getFriendlyURI()
							+ " is not empty");
				}
			}

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

			for (final File file : additionalFiles) {
				logger.fine("copying file: " + file.getAbsolutePath() + " to remote directory");
				final FileObject fileObject =
						fileSystemManager.resolveFile("file:" + file.getAbsolutePath());
				final FileObject remoteFile = remoteDir.resolveFile(file.getName());
				remoteFile.copyFrom(fileObject, new AllFileSelector());
			}

			logger.fine("Copying files to: " + host + " completed.");
		} catch (final FileSystemException e) {
			throw new InstallerException("Failed to copy files to remote host " + host + ": " + e.getMessage(), e);

		}
		checkTimeout(endTimeMillis);

	}

	@Override
	public void initialize(final InstallationDetails details, final long endTimeMillis)
			throws TimeoutException, InstallerException {
		this.installerConfiguration = details.getInstallerConfiguration();
		this.deleteRemoteDirectoryContents = details.isDeleteRemoteDirectoryContents();
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

		// when bootstrapping a management machine, pass all of the cloud
		// configuration, including all template
		// for an agent machine, just pass the upload dir fot the specific
		// template.
		String localDirPath = details.getLocalDir();
		if (details.isManagement()) {
			if (details.getCloudFile() == null) {
				throw new IllegalArgumentException("While bootstrapping a management machine, cloud file is null");
			}

			localDirPath = details.getCloudFile().getParentFile().getAbsolutePath();

		}

		try {
			localDir = mng.resolveFile("file:" + localDirPath);
			remoteDir = resolveTargetDirectory(opts, targetURI, mng);

		} catch (final FileSystemException e) {
			throw new InstallerException("Failed to set up file transfer: " + e.getMessage(), e);
		}

	}


	private FileObject resolveTargetDirectory(final FileSystemOptions opts, final String target,
			final FileSystemManager mng) throws FileSystemException {
		FileSystemException lastException = null;

		for (int i = 0; i < installerConfiguration.getFileTransferRetries(); ++i) {
			try {
				final FileObject targetDirectory = mng.resolveFile(target, opts);
				logger.fine("Remote directory resolved successfully.");
				return targetDirectory;
			} catch (final FileSystemException fse) {
				logger.fine("Attempt number: " + (i + 1) + " to reslve remote directory failed."
						+ " This may be a temporary issue while remote machine is starting up.");
				try {
					Thread.sleep(installerConfiguration.getFileTransferConnectionRetryIntervalMillis());
				} catch (final InterruptedException e) {
					// ignore
				}
				lastException = fse;
			}
		}
		throw lastException;
	}

	/*********
	 * Initialize the VFS manager with the required settings.
	 *
	 * @param details
	 *            the installation details.
	 * @param endTimeMillis
	 *            max end time for this operation.
	 * @throws InstallerException
	 *             if the operation failed.
	 */
	protected abstract void initVFSManager(final InstallationDetails details, final long endTimeMillis)
			throws InstallerException;

	/******
	 * Creates the required URI so it will be available for use later.
	 *
	 * @param details
	 *            the installation details.
	 * @throws InstallerException
	 *             if there was a problem.
	 */
	protected abstract void createTargetURI(InstallationDetails details)
			throws InstallerException;

}
