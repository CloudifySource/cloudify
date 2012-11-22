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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
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
	// logger
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Utils.class.getName());

	private Utils() {
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
	 * @param reservationId A unique identifier of the new agent to be created
	 * @param templateName The template of the machines to be created
	 * @param isSecurityOn Indicates whether security should be activated
	 * @return the installation details.
	 * @throws FileNotFoundException if a key file is specified and is not found.
	 */
	public static InstallationDetails createInstallationDetails(final MachineDetails md,
			final Cloud cloud, final CloudTemplate template, final ExactZonesConfig zones,
			final String lookupLocatorsString, final Admin admin, 
			final boolean isManagement, 
			final File cloudFile, 
			final GSAReservationId reservationId,
			final String templateName,
			final boolean isSecurityOn)
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
		details.setSecurityOn(isSecurityOn);
		
		details.setMachineId(md.getMachineId());
		logger.fine("Created InstallationDetails: " + details);
		return details;

	}
}
