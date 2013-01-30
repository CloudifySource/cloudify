/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.method.AuthNone;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplateInstallerConfiguration;
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.cloud.CloudTemplateInstallerConfiguration;
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.cloud.GridComponents;
import org.cloudifysource.dsl.cloud.RemoteExecutionModes;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
=======
import org.cloudifysource.dsl.cloud.DeployerComponent;
import org.cloudifysource.dsl.cloud.DiscoveryComponent;
import org.cloudifysource.dsl.cloud.GridComponent;
import org.cloudifysource.dsl.cloud.GridComponents;
import org.cloudifysource.dsl.cloud.UsmComponent;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
>>>>>>> CLOUDIFY-1477 move templates section of cloud to a cloudCompute section under the cloud
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsa.GSAReservationId;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.openspaces.core.util.FileUtils;

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
	 * @param managementIP
	 *            The IP of the management machine to connect to (through the default LUS port)
	 * @param expectedGsmCount
	 *            <<<<<<< HEAD The number of GridServiceManager objects that are expected to be found. Only when this
	 *            number is reached, the admin object is considered loaded and can be returned
	 * @param lusPort
	 *            The lookup service port. ======= The number of GridServiceManager objects that are expected to be
	 *            found. Only when this number is reached, the admin object is considered loaded and can be returned
	 *            >>>>>>> CLOUDIFY-1476 Added configurable installation parameters as optional 'installer' block in each
	 *            template.
	 * @return An updated admin object
	 * @throws TimeoutException
	 *             Indicates the timeout (default is 90 seconds) was reached before the admin object was fully loaded
	 * @throws InterruptedException
	 *             Indicated the thread was interrupted while waiting
	 */
	public static Admin getAdminObject(final String managementIP, final int expectedGsmCount, final Integer lusPort)
			throws TimeoutException,
			InterruptedException {
		final AdminFactory adminFactory = new AdminFactory();
		adminFactory.addLocator(managementIP + ":" + lusPort);
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
	 * @param host
	 *            The host to run the command on
	 * @param command
	 *            The command to execute
	 * @param username
	 *            The name of the user executing the command
	 * @param password
	 *            The password for the executing user, if used
	 * @param keyFile
	 *            The key file, if used
	 * @param timeout
	 *            The number of time-units to wait before throwing a TimeoutException
	 * @param unit
	 *            The units (e.g. seconds)
	 * @throws TimeoutException
	 *             Indicates the timeout was reached before the command completed
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
	 * @param md
	 *            the machine details.
	 * @param cloud
	 *            The cloud configuration.
	 * @param template
	 *            the cloud template used for this machine.
	 * @param zones
	 *            the zones that the new machine should start in.
	 * @param lookupLocatorsString
	 *            the lookup locators string to pass to the new machine.
	 * @param admin
	 *            an admin object, may be null.
	 * @param isManagement
	 *            true if this machine will be installed as a cloudify controller, false otherwise.
	 * @param cloudFile
	 *            the cloud file, required only when isManagement == true.
	 * @param reservationId
	 *            A unique identifier of the new agent to be created
	 * @param templateName
	 *            The template of the machines to be created
	 * @param securityProfile
	 *            set security profile (nonsecure/secure/ssl)
	 * @param keystorePassword
	 *            The password to the keystore set on the rest server
	 * @param authGroups
	 *            The authentication groups attached to the GSA as an environment variable
	 *            {@link CloudifyConstants#GIGASPACES_AUTH_GROUPS}
	 * @return the installation details.
	 * @throws FileNotFoundException
	 *             if a key file is specified and is not found.
	 */
	public static InstallationDetails createInstallationDetails(final MachineDetails md,
			final Cloud cloud, final ComputeTemplate template, final ExactZonesConfig zones,
			final String lookupLocatorsString, final Admin admin,
			final boolean isManagement,
			final File cloudFile,
			final GSAReservationId reservationId,
			final String templateName,
			final String securityProfile,
			final String keystorePassword,
			final String authGroups)
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
		details.setScriptLanguage(md.getScriptLangeuage());

		details.setCloudFile(cloudFile);
		details.setManagement(isManagement);
		final GridComponents componentsConfig = cloud.getConfiguration().getComponents();
		if (isManagement) {
			details.setConnectedToPrivateIp(!cloud.getConfiguration().isBootstrapManagementOnPublicIp());
			details.setSecurityProfile(securityProfile);
			details.setKeystorePassword(keystorePassword);

			// setting management grid components command-line arguments
			final String esmCommandlineArgs = ConfigUtils.getEsmCommandlineArgs(componentsConfig.getOrchestrator());
			final String lusCommandlineArgs = ConfigUtils.getLusCommandlineArgs(componentsConfig.getDiscovery());
			final String gsmCommandlineArgs = ConfigUtils.getGsmCommandlineArgs(componentsConfig.getDeployer(),
					componentsConfig.getDiscovery());
			details.setEsmCommandlineArgs('"' + esmCommandlineArgs + '"');
			details.setLusCommandlineArgs('"' + lusCommandlineArgs + '"');
			details.setGsmCommandlineArgs('"' + gsmCommandlineArgs + '"');

			// setting management services LRMI port range.
			details.setGscLrmiPortRange(componentsConfig.getUsm().getPortRange());
			// setting web service ports and memory allocation
			details.setRestPort(componentsConfig.getRest().getPort());
			details.setWebuiPort(componentsConfig.getWebui().getPort());
			details.setRestMaxMemory(componentsConfig.getRest().getMaxMemory());
			details.setWebuiMaxMemory(componentsConfig.getWebui().getMaxMemory());

		} else {
			details.setConnectedToPrivateIp(cloud.getConfiguration().isConnectToPrivateIp());
		}
		details.setGsaCommandlineArgs('"' + ConfigUtils.getAgentCommandlineArgs(componentsConfig.getAgent()) + '"');

		// Add all template custom data fields starting with 'installer.' to the
		// installation details
		final Set<Entry<String, Object>> customEntries = template.getCustom().entrySet();
		for (final Entry<String, Object> entry : customEntries) {
			if (entry.getKey().startsWith("installer.")) {
				details.getCustomData().put(entry.getKey(), entry.getValue());
			}
		}


		// Handle key file
		if (md.getKeyFile() != null) {
			details.setKeyFile(md.getKeyFile().getAbsolutePath());
		} else {
			final String keyFileName = template.getKeyFile();
			if (!org.apache.commons.lang.StringUtils.isBlank(keyFileName)) {
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
			// the initialization command may include command separators (like
			// ';') so quote it
			final String command = template.getInitializationCommand();
			final String quotedCommand = "\"" + command + "\"";

			details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.GIGASPACES_AGENT_ENV_INIT_COMMAND,
					quotedCommand);
		}

		// Add the template custom environment
		final Set<Entry<String, String>> entries = template.getEnv().entrySet();
		for (final Entry<String, String> entry : entries) {
			details.getExtraRemoteEnvironmentVariables().put(entry.getKey(), entry.getValue());
		}

		if (!org.apache.commons.lang.StringUtils.isBlank(template.getJavaUrl())) {
			details.getExtraRemoteEnvironmentVariables().put(CloudifyConstants.GIGASPACES_AGENT_ENV_JAVA_URL,
					template.getJavaUrl());
		}

		details.setReservationId(reservationId);
		details.setTemplateName(templateName);

		details.setMachineId(md.getMachineId());

		if (authGroups != null) {
			details.setAuthGroups(authGroups);
		}

		details.setDeleteRemoteDirectoryContents(md.isCleanRemoteDirectoryOnStart());
		//add storage props that will be passed down to the bootstrap-management script.
		details.setStorageVolumeAttached(md.isStorageVolumeAttached());
		details.setStorageFormatType(md.getStorageFormatType());
		details.setStorageDeviceName(md.getStorageDeviceName());
		details.setStorageMountPath(md.getStorageMountPath());
		
		details.setInstallerConfiguration(md.getInstallerConfigutation());
		logger.fine("Created InstallationDetails: " + details);
		return details;
	}

	/***********
	 * Created a temporary folder.
	 *
	 * @return the folder.
	 */
	public static File createTempFolder() {
		File tempFile;
		try {
			tempFile = File.createTempFile("cloudify", "tmp");
		} catch (final IOException e) {
			throw new IllegalStateException("Failed to create temp file", e);
		}
		FileUtils.deleteFileOrDirectory(tempFile);

		final boolean created = tempFile.mkdirs();
		if (!created) {
			throw new IllegalStateException("Failed to create temp file " + tempFile);
		}
		return tempFile;
	}

	/**********
	 * Returns the file transfer port that should be used, based on the configuration details and default port.
	 *
	 * @param installerConfiguration
	 *            the installer configuration.
	 * @param mode
	 *            the file transfer mode.
	 *
	 * @return the port.
	 */
	public static int getFileTransferPort(final CloudTemplateInstallerConfiguration installerConfiguration,
			final FileTransferModes mode) {
		if (installerConfiguration.getFileTransferPort() == CloudTemplateInstallerConfiguration.DEFAULT_PORT) {
			return mode.getDefaultPort();
		} else {
			return installerConfiguration.getFileTransferPort();
		}
	}

	/**********
	 * Returns the file transfer port that should be used, based on the configuration details and default port.
	 *
	 * @param installerConfiguration
	 *            the installer configuration.
	 * @param mode
	 *            the file transfer mode.
	 *
	 * @return the port.
	 */
	public static int getRemoteExecutionPort(final CloudTemplateInstallerConfiguration installerConfiguration,
			final RemoteExecutionModes mode) {
		if (installerConfiguration.getRemoteExecutionPort() == CloudTemplateInstallerConfiguration.DEFAULT_PORT) {
			return mode.getDefaultPort();
		} else {
			return installerConfiguration.getRemoteExecutionPort();
		}
	}

	public static SSHClient createSSHClient(final InstallationDetails details, final String host, final int port)
			throws InstallerException {
		final SSHClient ssh = new SSHClient();
		ssh.addHostKeyVerifier(new PromiscuousVerifier());
		try {
			ssh.connect(host, port);
		} catch (final IOException e) {
			throw new InstallerException("Failed to connect to host: " + host + ": " + e.getMessage(), e);
		}

		try {
			if (!org.apache.commons.lang.StringUtils.isEmpty(details.getKeyFile())) {
				final File keyFile = new File(details.getKeyFile());
				if (!keyFile.exists() || !keyFile.isFile()) {
					throw new InstallerException("Expected to find key file at: " + keyFile.getAbsolutePath());
				}
				ssh.authPublickey(details.getUsername(), keyFile.getAbsolutePath());
			} else if (!org.apache.commons.lang.StringUtils.isEmpty(details.getPassword())) {
				ssh.authPassword(details.getUsername(), details.getPassword());
			} else {
				ssh.auth(details.getUsername(), new AuthNone());
			}
		} catch (final IOException e) {
			try {
				if (ssh != null) {
					ssh.close();
				}
			} catch (final IOException e1) {
				logger.log(Level.WARNING,
						"Failed to close ssh client after encountering an exception: " + e.getMessage(), e);
			}
			throw new InstallerException("Failed to authenticate to remote server: " + e.getMessage(), e);
		}

		return ssh;

	}
}
