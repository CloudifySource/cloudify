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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.jclouds.util.CredentialUtils;
import org.openspaces.admin.Admin;

import com.j_spaces.kernel.Environment;

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
	protected static final String AGENT_MACHINE_PREFIX = "cloudify_agent_";
	protected static final String MANAGMENT_MACHINE_PREFIX = "cloudify_managememnt_";

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

	protected final Map<String, Long> stoppingMachines = new ConcurrentHashMap<String, Long>();

	/**
	 * Initializing the cloud deployer according to the given cloud configuration.
	 * 
	 * @param cloud Cloud object to use
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
			final boolean management) {

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
	 * Handles credentials for accessing the server - in this order: 1. pem file (set as a key file on the user block in
	 * the groovy file) 2. machine's remote password (set previously by the cloud driver)
	 * 
	 * @param machineDetails The MachineDetails object that represents this server
	 * @param template the cloud template.
	 * @throws CloudProvisioningException Indicates missing credentials or IOException (when a key file is used)
	 */
	protected void handleServerCredentials(final MachineDetails machineDetails, final CloudTemplate template)
			throws CloudProvisioningException {

		File keyFile = null;
		// using a key (pem) file
		String keyFileStr = template.getKeyFile();
		if (StringUtils.isNotBlank(keyFileStr)) {
			fixConfigRelativePaths(cloud, template);
			keyFile = new File(keyFileStr);
			if (!keyFile.isAbsolute()) {
				keyFile = new File(template.getLocalDirectory(), keyFileStr);
			}
			if (keyFile != null && !keyFile.exists()) {
				throw new CloudProvisioningException("The specified key file could not be found: "
						+ keyFile.getAbsolutePath());
			}
		} else {
			// using a password
			String remotePassword = machineDetails.getRemotePassword();
			if (StringUtils.isNotBlank(remotePassword)) {
				// is this actually a pem file?
				if (CredentialUtils.isPrivateKeyCredential(remotePassword)) {
					logger.fine("Cloud has provided a key file for connections to new machines");
					try {
						keyFile = File.createTempFile("gs-esm-key", ".pem");
						keyFile.deleteOnExit();
						FileUtils.write(keyFile, remotePassword);
						// TODO : stop settings the machine's private key as the entire cloud's key. This would cause
						// a problem if the cloud (i.e. Rackspace) returns a different key for each machine.
						template.setKeyFile(keyFile.getAbsolutePath());
					} catch (final IOException e) {
						throw new CloudProvisioningException("Failed to create a temporary "
								+ "file for cloud server's key file", e);
					}
				} else {
					// this is a password
					logger.fine("Cloud has provided a password for remote connections to new machines");
				}
			} else {
				// if we got here - there is no key file or password on the cloud or node.
				logger.severe("No Password or key file specified in the cloud configuration file - connection to"
						+ " the new machine is not possible.");
				throw new CloudProvisioningException(
						"No credentials (password or key file) supplied with the cloud configuration file");
			}
		}

		logServerDetails(machineDetails, keyFile);
	}

	/**
	 * Publish a provisioning event occurred for the listeners registered on this class.
	 * 
	 * @param eventName The name of the event (must be in the message bundle)
	 * @param args Arguments that complement the event message
	 */
	protected void publishEvent(final String eventName, final Object... args) {
		for (final ProvisioningDriverListener listener : this.eventsListenersList) {
			listener.onProvisioningEvent(eventName, args);
		}
	}

	/**
	 * Sets the localDirectory setting of the given cloud object to an absolute path, based on the home directory.
	 * 
	 * @param cloud The cloud object to configure
	 * @param template the cloud template
	 */
	protected void fixConfigRelativePaths(final Cloud cloud, final CloudTemplate template) {
		String configLocalDir = template.getLocalDirectory();
		if (configLocalDir != null && !new File(configLocalDir).isAbsolute()) {
			String envHomeDir = Environment.getHomeDirectory();
			logger.fine("Assuming " + configLocalDir + " is in " + envHomeDir);
			template.setLocalDirectory(new File(envHomeDir, configLocalDir).getAbsolutePath());
		}
	}
}
