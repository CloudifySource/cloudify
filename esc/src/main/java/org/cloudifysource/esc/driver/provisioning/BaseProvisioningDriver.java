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
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.jclouds.util.CredentialUtils;
import org.openspaces.admin.Admin;

/**
 * @author noak
 * @since 2.0.1
 */
public abstract class BaseProvisioningDriver implements ProvisioningDriver, ProvisioningDriverClassContextAware {

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
	protected ProvisioningDriverClassContext context;

	protected static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(BaseProvisioningDriver.class.getName());

	protected final List<ProvisioningDriverListener> eventsListenersList = new LinkedList<ProvisioningDriverListener>();


	/**
	 * Initializing the cloud deployer according to the given cloud
	 * configuration.
	 *
	 * @param cloud
	 *            Cloud object to use
	 */
	protected abstract void initDeployer(final Cloud cloud);

	@Override
	public void setProvisioningDriverClassContext(final ProvisioningDriverClassContext context) {
		this.context = context;
	}

	@Override
	public String getCloudName() {
		return this.cloudName;
	}

	@Override
	public void setAdmin(final Admin admin) {
		this.admin = admin;

	}

	@Override
	public void addListener(final ProvisioningDriverListener pdl) {
		this.eventsListenersList.add(pdl);
	}

	@Override
	public void setConfig(final Cloud cloud, final String cloudTemplateName,
			final boolean management, final String serviceName) {

		this.cloud = cloud;
		this.cloudTemplateName = cloudTemplateName;
		this.management = management;
		this.cloudName = cloud.getName();
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
	}

	private static void logServerDetails(final MachineDetails machineDetails, final File tempFile) {
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
	 * Handles credentials for accessing the server - in this order: 1. pem file
	 * (set as a key file on the user block in the groovy file) 2. machine's
	 * remote password (set previously by the cloud driver)
	 *
	 * @param machineDetails
	 *            The MachineDetails object that represents this server
	 * @param template
	 *            the cloud template.
	 * @throws CloudProvisioningException
	 *             Indicates missing credentials or IOException (when a key file
	 *             is used)
	 */
	protected void handleServerCredentials(final MachineDetails machineDetails, final ComputeTemplate template)
			throws CloudProvisioningException {

		File keyFile = null;
		// using a key (pem) file
		final String keyFileStr = template.getKeyFile();
		if (StringUtils.isNotBlank(keyFileStr)) {
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
				// is this actually a pem file?
				if (CredentialUtils.isPrivateKeyCredential(remotePassword)) {
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
	 * Publish a provisioning event occurred for the listeners registered on
	 * this class.
	 *
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
	 * Created a machine details with basic settings from the given cloud
	 * template.
	 *
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
		return md;

	}

	protected MachineDetails[] doStartManagementMachines(final long endTime, final int numberOfManagementMachines)
			throws TimeoutException, CloudProvisioningException {
		final ExecutorService executors = Executors.newFixedThreadPool(numberOfManagementMachines);

		@SuppressWarnings("unchecked")
		final Future<MachineDetails>[] futures = (Future<MachineDetails>[]) new Future<?>[numberOfManagementMachines];

		final ComputeTemplate managementTemplate =
				this.cloud.getCloudCompute().getTemplates().get(this.cloud.getConfiguration().getManagementMachineTemplate());
		try {
			// Call startMachine asynchronously once for each management
			// machine
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

			// In case of a partial error, shutdown all servers that did start
			// up
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

	protected abstract MachineDetails createServer(String serverName, long endTime,
			ComputeTemplate template) throws CloudProvisioningException, TimeoutException;

	protected abstract void handleProvisioningFailure(int numberOfManagementMachines,
			int numberOfErrors, Exception firstCreationException, MachineDetails[] createdManagementMachines)
					throws CloudProvisioningException;
}
