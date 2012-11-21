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
package org.cloudifysource.esc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GSAReservationId;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.zone.config.ExactZonesConfig;

import com.gigaspaces.internal.utils.StringUtils;

/**
 * Utilities class.
 * 
 * @author noak
 * @since 2.0.0
 */
public final class Utils {

	// timeout in seconds, waiting for the admin API to load.
	private static final int ADMIN_API_TIMEOUT = 90;
	// timeout in seconds, waiting for a socket to connect
	private static final int DEFAULT_CONNECTION_TIMEOUT = 10;
	// timeout for SFTP connection
	private static final Integer SFTP_DISCONNECT_DETECTION_TIMEOUT_MILLIS = Integer.valueOf(10 * 1000);
	// logger
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Utils.class.getName());

	private Utils() {
	}

	/**
	 * Calculates the milliseconds remaining until the given end time.
	 * 
	 * @param end The end time, in milliseconds
	 * @return Number of milliseconds remaining until the given end time
	 * @throws TimeoutException Thrown when the end time is in the past
	 */
	public static long millisUntil(final long end)
			throws TimeoutException {
		final long millisUntilEnd = end - System.currentTimeMillis();
		if (millisUntilEnd < 0) {
			throw new TimeoutException("Cloud operation timed out");
		}
		return millisUntilEnd;
	}

	/**
	 * Safely casts long to int.
	 * 
	 * @param longValue The long to cast
	 * @param roundIfNeeded Indicating whether to change the value of the number if it exceeds int's max/min values. If
	 *        set to false and the long is too large/small, an {@link IllegalArgumentException} is thrown.
	 * @return int representing of the given long.
	 */
	public static int safeLongToInt(final long longValue, final boolean roundIfNeeded) {
		int intValue;
		if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
			if (roundIfNeeded) {
				if (longValue < Integer.MIN_VALUE) {
					intValue = Integer.MIN_VALUE;
				} else {
					intValue = Integer.MAX_VALUE;
				}
			} else {
				throw new IllegalArgumentException(longValue + " cannot be cast to int without changing its value.");
			}
		} else {
			intValue = (int) longValue;
		}
		return intValue;
	}
	
	/**
	 * Splits the given string by the given delimiter, and trimming the resulted tokens. 
	 * @param stringOfTokens The string to split 
	 * @param delimiter The delimiter to split by
	 * @return A Collection of trimmed String tokens
	 */
	public static Collection<String> splitAndTrimString(String stringOfTokens, String delimiter) {
    	Collection<String> values = new HashSet<String>();
		StringTokenizer tokenizer = new StringTokenizer(stringOfTokens, delimiter);
		while (tokenizer.hasMoreTokens()) {
			values.add(tokenizer.nextToken().trim());
		}
		
		return values;
    }

	/**
	 * Validates a connection can be made to the given address and port, within the given time limit.
	 * 
	 * @param ipAddress The IP address to connect to
	 * @param port The port number to use
	 * @throws IOException Reports a failure to connect or resolve the given address.
	 */
	public static void validateConnection(final String ipAddress, final int port)
			throws IOException {
		validateConnection(ipAddress, port, DEFAULT_CONNECTION_TIMEOUT);
	}

	/**
	 * Validates a connection can be made to the given address and port, within the given time limit.
	 * 
	 * @param ipAddress The IP address to connect to
	 * @param port The port number to use
	 * @param timeout The time to wait before timing out, in seconds
	 * @throws IOException Reports a failure to connect or resolve the given address.
	 */
	public static void validateConnection(final String ipAddress, final int port, final int timeout)
			throws IOException {

		final Socket socket = new Socket();

		try {
			final InetSocketAddress endPoint = new InetSocketAddress(ipAddress, port);
			if (endPoint.isUnresolved()) {
				throw new UnknownHostException(ipAddress);
			}

			socket.connect(endPoint, Utils.safeLongToInt(TimeUnit.SECONDS.toMillis(timeout), true));
		} finally {
			try {
				socket.close();
			} catch (final IOException ioe) {
				// ignore
			}
		}
	}

	/**
	 * Returns the content of a given input stream, as a String object.
	 * 
	 * @param is the input stream to read.
	 * @return the content of the given input stream
	 * @throws IOException Reporting failure to read from the InputStream
	 */
	public static String getStringFromStream(final InputStream is)
			throws IOException {
		final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
		final StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

	/**
	 * Converts a json String to a Map<String, Object>.
	 * 
	 * @param response a json-format String to convert to a map
	 * @return a Map<String, Object> based on the given String
	 * @throws IOException Reporting failure to read or map the String
	 */
	public static Map<String, Object> jsonToMap(final String response)
			throws IOException {
		@SuppressWarnings("deprecation")
		final JavaType javaType = TypeFactory.type(Map.class);
		return new ObjectMapper().readValue(response, javaType);
	}

	/**
	 * Gets a "full" admin object. The function waits until all GridServiceManagers are found before returning the
	 * object.
	 * 
	 * @param managementIP The IP of the management machine to connect to (through the default LUS port)
	 * @param expectedGsmCount The number of GridServiceManager objects that are expected to be found. Only when this
	 *        number is reached, the admin object is considered loaded and can be returned
	 * @return An updated admin object
	 * @throws TimeoutException Indicates the timeout (default is 90 seconds) was reached before the admin object was
	 *         fully loaded
	 * @throws InterruptedException Indicated the thread was interrupted while waiting
	 */
	public static Admin getAdminObject(final String managementIP, final int expectedGsmCount)
			throws TimeoutException,
			InterruptedException {
		final AdminFactory adminFactory = new AdminFactory();
		adminFactory.addLocator(managementIP + ":" + CloudifyConstants.DEFAULT_LUS_PORT);
		final Admin admin = adminFactory.createAdmin();
		GridServiceManagers gsms = admin.getGridServiceManagers();
		final long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ADMIN_API_TIMEOUT);
		while (admin.getLookupServices() == null || gsms == null || expectedGsmCount > 0
				&& gsms.getSize() < expectedGsmCount) {
			if (System.currentTimeMillis() > end) {
				throw new TimeoutException("Admin API timed out");
			}
			Thread.sleep(TimeUnit.SECONDS.toMillis(1));
			gsms = admin.getGridServiceManagers();
		}

		return admin;
	}

	/**
	 * Executes a SSH command. An Ant BuildException is thrown in case of an error.
	 * 
	 * @param host The host to run the command on
	 * @param command The command to execute
	 * @param username The name of the user executing the command
	 * @param password The password for the executing user, if used
	 * @param keyFile The key file, if used
	 * @param timeout The number of time-units to wait before throwing a TimeoutException
	 * @param unit The units (e.g. seconds)
	 * @throws TimeoutException Indicates the timeout was reached before the command completed
	 */
	public static void executeSSHCommand(final String host, final String command, final String username,
			final String password, final String keyFile, final long timeout, final TimeUnit unit)
			throws TimeoutException {

		final LoggerOutputStream loggerOutputStream = new LoggerOutputStream(
				Logger.getLogger(AgentlessInstaller.SSH_OUTPUT_LOGGER_NAME));
		loggerOutputStream.setPrefix("[" + host + "] ");

		final org.cloudifysource.esc.util.SSHExec task = new org.cloudifysource.esc.util.SSHExec();
		task.setCommand(command);
		task.setHost(host);
		task.setTrust(true);
		task.setUsername(username);
		task.setTimeout(unit.toMillis(timeout));
		task.setFailonerror(true);
		task.setOutputStream(loggerOutputStream);
		task.setUsePty(true);

		if (keyFile != null) {
			task.setKeyfile(keyFile);
		}
		if (password != null) {
			task.setPassword(password);
		}

		task.execute();
		loggerOutputStream.close();
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

		if (!(fileTransferMode == FileTransferModes.SCP)) {
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

		boolean objectsExist;
		if (allMustExist) {
			objectsExist = true;
		} else {
			objectsExist = false;
		}

		if (!(fileTransferMode == FileTransferModes.SCP)) {
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

	/*************************
	 * Creates an Agentless Installer's InstallationDetails input object from a machine details object returned from a
	 * provisioning implementation.
	 * 
	 * @param md the machine details.
	 * @param cloud The cloud configuration.
	 * @param template the cloud template used for this machine.
	 * @param zones the zones that the new machine should start in.
	 * @param lookupLocatorsString the lookup locators string to pass to the new machine.
	 * @param admin an admin object, may be null.
	 * @param isManagement true if this machine will be installed as a cloudify controller, false otherwise.
	 * @param cloudFile the cloud file, required only when isManagement == true.
	 * @return the installation details.
	 * @throws FileNotFoundException if a key file is specified and is not found.
	 */
	public static InstallationDetails createInstallationDetails(final MachineDetails md,
			final Cloud cloud, final CloudTemplate template, final ExactZonesConfig zones,
			final String lookupLocatorsString, final Admin admin, 
			final boolean isManagement, 
			final File cloudFile, 
			final GSAReservationId reservationId,
			final String templateName)
			throws FileNotFoundException {

		final InstallationDetails details = new InstallationDetails();

		details.setBindToPrivateIp(cloud.getConfiguration().isConnectToPrivateIp());
		details.setLocalDir(template.getAbsoluteUploadDir());
		details.setRelativeLocalDir(template.getLocalDirectory());

		final String remoteDir = template.getRemoteDirectory();
		details.setRemoteDir(remoteDir);

		// Create a copy of managementOnly files and mutate
		final List<String> managementOnlyFiles = new ArrayList<String>(cloud.getProvider().getManagementOnlyFiles());
		if (template.getKeyFile() != null && isManagement) {
			// keyFile, if used, is always a management file.
			managementOnlyFiles.add(template.getKeyFile());
		}
		details.setManagementOnlyFiles(managementOnlyFiles);

		details.setZones(StringUtils.collectionToCommaDelimitedString(zones.getZones()));

		details.setPrivateIp(md.getPrivateAddress());
		details.setPublicIp(md.getPublicAddress());

		details.setLocator(lookupLocatorsString);

		details.setCloudifyUrl(cloud.getProvider().getCloudifyUrl());
		details.setOverridesUrl(cloud.getProvider().getCloudifyOverridesUrl());

		details.setConnectedToPrivateIp(cloud.getConfiguration().isConnectToPrivateIp());
		details.setAdmin(admin);

		details.setUsername(md.getRemoteUsername());
		details.setPassword(md.getRemotePassword());
		details.setRemoteExecutionMode(md.getRemoteExecutionMode());
		details.setFileTransferMode(md.getFileTransferMode());

		details.setCloudFile(cloudFile);
		details.setLus(isManagement);
		if (isManagement) {
			details.setConnectedToPrivateIp(!cloud.getConfiguration().isBootstrapManagementOnPublicIp());
		} else {
			details.setConnectedToPrivateIp(cloud.getConfiguration().isConnectToPrivateIp());
		}

		// Add all template custom data fields starting with 'installer.' to the installation details
		final Set<Entry<String, Object>> customEntries = template.getCustom().entrySet();
		for (final Entry<String, Object> entry : customEntries) {
			if (entry.getKey().startsWith("installer.")) {
				details.getCustomData().put(entry.getKey(), entry.getValue());
			}
		}

		final String keyFileName = template.getKeyFile();
		if (keyFileName != null && !keyFileName.isEmpty()) {
			File keyFile = new File(keyFileName);
			if (!keyFile.isAbsolute()) {
				keyFile = new File(details.getLocalDir(), keyFileName);
			}
			if (!keyFile.isFile()) {
				throw new FileNotFoundException(
						"Could not find key file matching specified cloud configuration key file: "
								+ template.getKeyFile() + ". Tried: " + keyFile + " but file does not exist");
			}
			details.setKeyFile(keyFile.getAbsolutePath());
		}

		if (template.getHardwareId() != null) {
			details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.GIGASPACES_CLOUD_HARDWARE_ID,
					template.getHardwareId());
			// maintain backwards for pre 2.3.0
			details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.CLOUDIFY_CLOUD_HARDWARE_ID,
					template.getHardwareId());

		}

		if (template.getImageId() != null) {
			details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.GIGASPACES_CLOUD_IMAGE_ID,
					template.getImageId());
			// maintain backwards for pre 2.3.0
			details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.CLOUDIFY_CLOUD_IMAGE_ID,
					template.getImageId());			
		}

		// Add the template privileged mode flag
		details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.GIGASPACES_AGENT_ENV_PRIVILEGED,
				Boolean.toString(template.isPrivileged()));

		// Add the template initialization command
		if (!org.apache.commons.lang.StringUtils.isBlank(template.getInitializationCommand())) {
			details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.GIGASPACES_AGENT_ENV_INIT_COMMAND,
					template.getInitializationCommand());
		}

		// Add the template custom environment
		final Set<Entry<String, String>> entries = template.getEnv().entrySet();
		for (Entry<String, String> entry : entries) {
			details.getExtraRemoteEnvironmentVariables().put(entry.getKey(), entry.getValue());
		}

		if (!org.apache.commons.lang.StringUtils.isBlank(template.getJavaUrl())) {
			details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.GIGASPACES_AGENT_ENV_JAVA_URL,
					template.getJavaUrl());
		}

		details.setReservationId(reservationId);
		details.setTemplateName(templateName);
		
		details.setMachineId(md.getMachineId());
		logger.fine("Created InstallationDetails: " + details);
		return details;

	}
}
