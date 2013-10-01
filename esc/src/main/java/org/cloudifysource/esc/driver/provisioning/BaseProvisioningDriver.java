/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.Admin;

/**
 * @author noak
 * @since 2.0.1
 */
public abstract class BaseProvisioningDriver extends BaseComputeDriver {

	private static final String PRIVATE_KEY_PREFIX = "-----BEGIN RSA PRIVATE KEY-----";
	protected static final int MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT = 120000;
	protected static final int WAIT_THREAD_SLEEP_MILLIS = 10000;
	protected static final int WAIT_TIMEOUT_MILLIS = 360000;
	// TODO - make this a configuration option
	protected static final int MAX_SERVERS_LIMIT = 200;

	protected static final String EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API = "try_to_connect_to_cloud_api";
	protected static final String EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API = "connection_to_cloud_api_succeeded";
	protected static final String EVENT_ATTEMPT_START_MGMT_VMS = "attempting_to_create_management_vms";
	protected static final String EVENT_MGMT_VMS_STARTED = "management_started_successfully";
	protected static final String AGENT_MACHINE_PREFIX = "cloudify-agent-";
	protected static final String MANAGMENT_MACHINE_PREFIX = "cloudify-managememnt-";

	protected boolean management;
	protected static AtomicInteger counter = new AtomicInteger();
	protected String serverNamePrefix;
	protected String cloudName;
	protected String cloudTemplateName;
	protected Admin admin;
	protected Cloud cloud;

	protected final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(this.getClass().getName());

	protected final List<ProvisioningDriverListener> eventsListenersList = new LinkedList<ProvisioningDriverListener>();
	protected Boolean cleanRemoteDirectoryOnStart = false;
	protected boolean isVerboseValidation = true;

	/**
	 * @param cloud
	 *            Cloud object to use
	 */
	protected abstract void initDeployer(final Cloud cloud);

	@Override
	public String getCloudName() {
		return this.cloudName;
	}

	@Override
	public void setConfig(final ComputeDriverConfiguration configuration) throws CloudProvisioningException {
		// TODO Auto-generated method stub
		super.setConfig(configuration);

		this.cloud = configuration.getCloud();
		this.cloudTemplateName = configuration.getCloudTemplate();
		this.management = configuration.isManagement();
		this.cloudName = cloud.getName();
		this.admin = configuration.getAdmin();
		
		Object bol = cloud.getCustom().get(CloudifyConstants.CUSTOM_PROPERTY_VERBOSE_VALIDATION);
		if (bol == null) {
			this.isVerboseValidation = true;
		} else if (bol instanceof String) {
			this.isVerboseValidation = Boolean.parseBoolean((String) bol);
		} else if (bol instanceof Boolean) {
			this.isVerboseValidation = 
					(Boolean) cloud.getCustom().get(CloudifyConstants.CUSTOM_PROPERTY_VERBOSE_VALIDATION);
		}
		publishEvent(EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API, cloud.getProvider().getProvider());
		initDeployer(cloud);
		publishEvent(EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API, cloud.getProvider().getProvider());

		logger.fine("Initializing Cloud Provisioning - management mode: " + management + ". Using template: "
				+ cloudTemplateName + " with cloud: " + cloudName);

		String prefix =
				management ? cloud.getProvider().getManagementGroup() : cloud.getProvider().getMachineNamePrefix();

		if (StringUtils.isBlank(prefix)) {
			if (management) {
				prefix = MANAGMENT_MACHINE_PREFIX;
			} else {
				prefix = AGENT_MACHINE_PREFIX;
			}

			logger.warning("Prefix for machine name was not set. Using: " + prefix);
		}

		this.serverNamePrefix = prefix;

		initCleanRemoteOnStart(cloud);
	}

	private void initCleanRemoteOnStart(final Cloud cloud) {
		// set custom settings
		final Map<String, Object> customSettings = cloud.getCustom();
		if (customSettings != null) {
			// clean GS files on shutdown
			if (customSettings.containsKey(CloudifyConstants.CUSTOM_PROPERTY_CLEAN_REMOTE_DIR_ON_START)) {
				final Object cleanRemoteDirValue =
						customSettings.get(CloudifyConstants.CUSTOM_PROPERTY_CLEAN_REMOTE_DIR_ON_START);
				if (cleanRemoteDirValue instanceof Boolean) {
					this.cleanRemoteDirectoryOnStart = (Boolean) cleanRemoteDirValue;
				} else if (cleanRemoteDirValue instanceof String) {
					this.cleanRemoteDirectoryOnStart = Boolean.parseBoolean((String) cleanRemoteDirValue);
				} else {
					throw new IllegalArgumentException("Unexpected value for BYON property: "
							+ CloudifyConstants.CUSTOM_PROPERTY_CLEAN_REMOTE_DIR_ON_START
							+ ". Was expecting a boolean or String, got: "
							+ cleanRemoteDirValue.getClass().getName());
				}
			}
		}
	}

	private void logServerDetails(final MachineDetails machineDetails, final File tempFile) {
		if (logger.isLoggable(Level.FINE)) {
			final String nodePrefix = "[" + machineDetails.getMachineId() + "] ";
			logger.fine(nodePrefix + "Cloud Server is allocated.");
			if (tempFile == null) {
				logger.fine(nodePrefix + "Password: ***");
			} else {
				logger.fine(nodePrefix + "Key File: " + tempFile.getAbsolutePath());
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Private IP: " + machineDetails.getPrivateAddress());
				logger.fine("Public IP: " + machineDetails.getPublicAddress());
			}
		}
	}

	/**
	 * @param machineDetails
	 *            The MachineDetails object that represents this server
	 * @param template
	 *            the cloud template.
	 * @throws CloudProvisioningException
	 */
	protected void handleServerCredentials(final MachineDetails machineDetails, final ComputeTemplate template)
			throws CloudProvisioningException {

		File keyFile = null;
		// using a key (pem) file
		if (machineDetails.getKeyFile() != null) {
			keyFile = machineDetails.getKeyFile();
			if (!keyFile.isFile()) {
				throw new CloudProvisioningException("The specified key file could not be found: "
						+ keyFile.getAbsolutePath());
			}
		} else if (StringUtils.isNotBlank(template.getKeyFile())) {
			final String keyFileStr = template.getKeyFile();
			// fixConfigRelativePaths(cloud, template);
			keyFile = new File(keyFileStr);
			if (!keyFile.isAbsolute()) {
				keyFile = new File(template.getAbsoluteUploadDir(), keyFileStr);
			}
			if (keyFile != null && !keyFile.exists()) {
				throw new CloudProvisioningException("The specified key file could not be found: "
						+ keyFile.getAbsolutePath());
			}
		} else {
			// using a password
			final String remotePassword = machineDetails.getRemotePassword();
			if (StringUtils.isNotBlank(remotePassword)) {
				// is this actually a private key file?
				if (remotePassword.startsWith(PRIVATE_KEY_PREFIX)) {
					logger.fine("Cloud has provided a key file for connections to new machines");
					try {
						keyFile = File.createTempFile("gs-esm-key", ".pem");
						keyFile.deleteOnExit();
						FileUtils.write(keyFile, remotePassword);

						// template.setKeyFile(keyFile.getAbsolutePath());
						machineDetails.setKeyFile(keyFile);
					} catch (final IOException e) {
						throw new CloudProvisioningException("Failed to create a temporary "
								+ "file for cloud server's key file", e);
					}
				} else {
					// this is a password
					logger.fine("Cloud has provided a password for remote connections to new machines");
				}
			} else {
				// if we got here - there is no key file or password on the
				// cloud or node.
				logger.severe("No Password or key file specified in the cloud configuration file - connection to"
						+ " the new machine is not possible.");
				throw new CloudProvisioningException(
						"No credentials (password or key file) supplied with the cloud configuration file");
			}
		}

		logServerDetails(machineDetails, keyFile);
	}

	/**
	 * @param eventName
	 *            The name of the event (must be in the message bundle)
	 * @param args
	 *            Arguments that complement the event message
	 */
	protected void publishEvent(final String eventName, final Object... args) {
		for (final ProvisioningDriverListener listener : this.eventsListenersList) {
			listener.onProvisioningEvent(eventName, args);
		}
	}

	/*********
	 * @param template
	 *            the cloud template.
	 * @return the newly created machine details.
	 */
	protected MachineDetails createMachineDetailsForTemplate(final ComputeTemplate template) {

		final MachineDetails md = new MachineDetails();
		md.setAgentRunning(false);
		md.setCloudifyInstalled(false);
		md.setInstallationDirectory(null);

		md.setRemoteUsername(template.getUsername());
		md.setRemotePassword(template.getPassword());

		md.setRemoteExecutionMode(template.getRemoteExecution());
		md.setFileTransferMode(template.getFileTransfer());
		md.setScriptLangeuage(template.getScriptLanguage());
		md.setCleanRemoteDirectoryOnStart(this.cleanRemoteDirectoryOnStart);
		return md;

	}

	/*********
	 * .
	 * 
	 * @param endTime
	 *            .
	 * @param numberOfManagementMachines
	 *            .
	 * @return .
	 * @throws TimeoutException .
	 * @throws CloudProvisioningException .
	 */
	protected MachineDetails[] doStartManagementMachines(final long endTime, final int numberOfManagementMachines)
			throws TimeoutException, CloudProvisioningException {
		final ExecutorService executors = Executors.newFixedThreadPool(numberOfManagementMachines);

		@SuppressWarnings("unchecked")
		final Future<MachineDetails>[] futures = (Future<MachineDetails>[]) new Future<?>[numberOfManagementMachines];

		final ComputeTemplate managementTemplate =
				this.cloud.getCloudCompute().getTemplates().get(
						this.cloud.getConfiguration().getManagementMachineTemplate());
		try {
			// Call startMachine asynchronously once for each management machine
			for (int i = 0; i < numberOfManagementMachines; i++) {
				final int index = i + 1;
				futures[i] = executors.submit(new Callable<MachineDetails>() {

					@Override
					public MachineDetails call()
							throws Exception {
						return createServer(serverNamePrefix + index, endTime, managementTemplate);
					}
				});

			}

			// Wait for each of the async calls to terminate.
			int numberOfErrors = 0;
			Exception firstCreationException = null;
			final MachineDetails[] createdManagementMachines = new MachineDetails[numberOfManagementMachines];
			for (int i = 0; i < createdManagementMachines.length; i++) {
				try {
					createdManagementMachines[i] = futures[i].get(endTime - System.currentTimeMillis(),
							TimeUnit.MILLISECONDS);
				} catch (final InterruptedException e) {
					++numberOfErrors;
					publishEvent("failed_to_create_management_vm", e.getMessage());
					logger.log(Level.SEVERE, "Failed to start a management machine", e);
					if (firstCreationException == null) {
						firstCreationException = e;
					}

				} catch (final ExecutionException e) {
					++numberOfErrors;
					publishEvent("failed_to_create_management_vm", e.getMessage());
					logger.log(Level.SEVERE, "Failed to start a management machine", e);
					if (firstCreationException == null) {
						firstCreationException = e;
					}
				}
			}

			// In case of a partial error, shutdown all servers that did start up
			if (numberOfErrors > 0) {
				handleProvisioningFailure(numberOfManagementMachines, numberOfErrors, firstCreationException,
						createdManagementMachines);
			}

			return createdManagementMachines;
		} finally {
			if (executors != null) {
				executors.shutdownNow();
			}
		}
	}

	/**
	 * returns the message as it appears in the DefaultProvisioningDriver message bundle.
	 * 
	 * @param messageBundle
	 *            The message bundle containing the specified message
	 * @param msgName
	 *            the message key as it is defined in the message bundle.
	 * @param arguments
	 *            the message arguments
	 * @return the formatted message according to the message key.
	 */
	protected String getFormattedMessage(final ResourceBundle messageBundle, final String msgName,
			final Object... arguments) {
		final String message = messageBundle.getString(msgName);
		if (message == null) {
			logger.warning("Missing resource in messages resource bundle: " + msgName);
			return msgName;
		}
		try {
			return MessageFormat.format(message, arguments);
		} catch (final IllegalArgumentException e) {
			logger.fine("Failed to format message: " + msgName + " with format: "
					+ message + " and arguments: " + Arrays.toString(arguments));
			return msgName;
		}
	}

	protected abstract MachineDetails createServer(String serverName, long endTime,
			ComputeTemplate template) throws CloudProvisioningException, TimeoutException;

	protected abstract void handleProvisioningFailure(int numberOfManagementMachines,
			int numberOfErrors, Exception firstCreationException, MachineDetails[] createdManagementMachines)
			throws CloudProvisioningException;

	@Override
	public void onServiceUninstalled(final long duration, final TimeUnit unit)
			throws InterruptedException, TimeoutException, CloudProvisioningException {

	}
}