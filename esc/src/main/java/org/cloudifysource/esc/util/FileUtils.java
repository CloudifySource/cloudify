package org.cloudifysource.esc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;

/**
 * A utility class for file system handling.
 * 
 * @author noak
 * @since 2.3.1
 */
public class FileUtils {
	
	// timeout for SFTP connections
	private static final Integer SFTP_DISCONNECT_DETECTION_TIMEOUT_MILLIS = Integer.valueOf(10 * 1000);

	/**
	 * Checks whether the files or folders exist on a remote host. The returned value depends on the last parameter -
	 * "allMustExist". If allMustExist is True the returned value is True only if all listed objects exist. If
	 * allMustExist is False, the returned value is True if at least one object exists.
	 * 
	 * @param host The host to connect to
	 * @param username The name of the user that deletes the file/folder
	 * @param password The password of the above user
	 * @param keyFile The key file, if used
	 * @param fileSystemObjects The files or folders to delete
	 * @param fileTransferMode SCP for secure copy in Linux, or CIFS for windows file sharing
	 * @param allMustExist If set to True the function will return True only if all listed objects exist. If set to
	 *        False, the function will return True if at least one object exists.
	 * @return depends on allMustExist
	 * @throws IOException Indicates the deletion failed
	 */
	public static boolean fileSystemObjectsExist(final String host, final String username, final String password,
			final String keyFile, final List<String> fileSystemObjects, final FileTransferModes fileTransferMode,
			final boolean allMustExist)
			throws IOException {

		boolean objectsExist = allMustExist;

		if (!(fileTransferMode == FileTransferModes.SFTP)) {
			// TODO Support get with CIFS as well
			throw new IOException("File resolving is currently not supported for this file transfer protocol ("
					+ fileTransferMode + ")");
		}

		final FileSystemOptions opts = new FileSystemOptions();
		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
		SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
		if (keyFile != null && !keyFile.isEmpty()) {
			final File temp = new File(keyFile);
			if (!temp.isFile()) {
				throw new FileNotFoundException("Could not find key file: " + temp);
			}
			SftpFileSystemConfigBuilder.getInstance().setIdentities(opts, new File[] { temp });
		}

		SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, SFTP_DISCONNECT_DETECTION_TIMEOUT_MILLIS);
		final FileSystemManager mng = VFS.getManager();

		String scpTargetBase, scpTarget;
		if (password != null && !password.isEmpty()) {
			scpTargetBase = "sftp://" + username + ':' + password + '@' + host;
		} else {
			scpTargetBase = "sftp://" + username + '@' + host;
		}

		FileObject remoteDir = null;
		try {
			for (final String fileSystemObject : fileSystemObjects) {
				scpTarget = scpTargetBase + fileSystemObject;
				remoteDir = mng.resolveFile(scpTarget, opts);
				if (remoteDir.exists()) {
					if (!allMustExist) {
						objectsExist = true;
						break;
					}
				} else {
					if (allMustExist) {
						objectsExist = false;
						break;
					}
				}
			}
		} finally {
			if (remoteDir != null) {
				mng.closeFileSystem(remoteDir.getFileSystem());
			}
		}

		return objectsExist;
	}
	
	/**
	 * Deletes files or folders on a remote host.
	 * 
	 * @param host The host to connect to
	 * @param username The name of the user that deletes the file/folder
	 * @param password The password of the above user
	 * @param keyFile The key file, if used
	 * @param fileSystemObjects The files or folders to delete
	 * @param fileTransferMode SCP for secure copy in Linux, or CIFS for windows file sharing
	 * @throws IOException Indicates the deletion failed
	 */
	public static void deleteFileSystemObjects(final String host, final String username, final String password,
			final String keyFile, final List<String> fileSystemObjects, final FileTransferModes fileTransferMode)
			throws IOException {

		if (!(fileTransferMode == FileTransferModes.SFTP)) {
			throw new IOException("File deletion is currently not supported for this file transfer protocol ("
					+ fileTransferMode + ")");
		}

		final FileSystemOptions opts = new FileSystemOptions();
		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
		SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
		if (keyFile != null && !keyFile.isEmpty()) {
			final File temp = new File(keyFile);
			if (!temp.isFile()) {
				throw new FileNotFoundException("Could not find key file: " + temp);
			}
			SftpFileSystemConfigBuilder.getInstance().setIdentities(opts, new File[] { temp });
		}

		SftpFileSystemConfigBuilder.getInstance().setTimeout(opts, SFTP_DISCONNECT_DETECTION_TIMEOUT_MILLIS);
		final FileSystemManager mng = VFS.getManager();

		String scpTargetBase, scpTarget;
		if (password != null && !password.isEmpty()) {
			scpTargetBase = "sftp://" + username + ':' + password + '@' + host;
		} else {
			scpTargetBase = "sftp://" + username + '@' + host;
		}

		FileObject remoteDir = null;
		try {
			for (final String fileSystemObject : fileSystemObjects) {
				scpTarget = scpTargetBase + fileSystemObject;
				remoteDir = mng.resolveFile(scpTarget, opts);

				remoteDir.delete(new FileSelector() {

					@Override
					public boolean includeFile(final FileSelectInfo fileInfo)
							throws Exception {
						return true;
					}

					@Override
					public boolean traverseDescendents(final FileSelectInfo fileInfo)
							throws Exception {
						return true;
					}
				});
			}
		} finally {
			if (remoteDir != null) {
				mng.closeFileSystem(remoteDir.getFileSystem());
			}
		}
	}
	

	/**
	 * Creates a folder a unique name based on the given basicFolderName, inside the specified parent folder.
	 * If the folder by that name already exists in the parent folder - a number is appended to that name, until
	 * a unique name is found. e.g.: "myfolder1", "myfolder2" ... "myfolder99" (max index is set by maxAppender).
	 * If all those names are already in use (meaning there are existing folders with these names) -
	 * we create a completely new random name using "File.createTempFile".
	 * 
	 * @param parentFolder The folder to contain the new folder created by this method.
	 * @param basicFolderName The base name to be used for the new folder. Numbers might be appended to this name to 
	 * reach uniqueness. 
	 * @param maxAppender The maximum number appended to the basic folder name to reach uniqueness. If a unique name
	 * is not found for the folder and the maximum is reached, a new random name using "File.createTempFile".
	 * @return The unique name
	 * @throws IOException Indicates a failure to generate a unique folder name
	 */
	public static String createUniqueFolderName(final File parentFolder, final String basicFolderName, 
			final int maxAppender) throws IOException {
    	
    	int index = 0;
    	boolean uniqueNameFound = false;
    	String folderName = basicFolderName;
    	
    	while (!uniqueNameFound && index < maxAppender) {
			//create a new name (temp1, temp2... temp99)
    		folderName = basicFolderName + (++index);
    		
        	File restTempFolder = new File(parentFolder, folderName);
    		if (!restTempFolder.exists()) {
    			uniqueNameFound = true;
    		}
    	}    	
    	
    	if (!uniqueNameFound) {
    		//create folder with a new unique name
   			File tempFile = File.createTempFile(folderName, ".tmp");
   			tempFile.deleteOnExit();
   			folderName = StringUtils.substringBeforeLast(tempFile.getName(), ".tmp");
   			uniqueNameFound = true;
    	}
    	
    	if (uniqueNameFound) {
    		return folderName;
    	} else {
    		throw new IOException(CloudifyMessageKeys.UPLOAD_DIRECTORY_CREATION_FAILED.getName());
    	}
    }
    

}
