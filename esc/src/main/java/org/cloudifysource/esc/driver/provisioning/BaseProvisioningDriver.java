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
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.cloud.Cloud;
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
	protected static final int MAX_SERVERS_LIMIT = 200;
	protected static final int SSH_PORT = 22;

	protected static final String EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API = "try_to_connect_to_cloud_api";
	protected static final String EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API = "connection_to_cloud_api_succeeded";
	protected static final String EVENT_ATTEMPT_START_MGMT_VMS = "attempting_to_create_management_vms";
	protected static final String EVENT_MGMT_VMS_STARTED = "management_started_successfully";
	protected static final String AGENT_MACHINE_PREFIX = "cloudify_agent_";
	protected static final String MANAGMENT_MACHINE_PREFIX = "cloudify_managememnt_";
	
	protected boolean management;
	protected int counter = 0;
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
	public void setConfig(final Cloud cloud, final String cloudTemplateName, final boolean management) {

		this.cloud = cloud;
		this.cloudTemplateName = cloudTemplateName;
		this.management = management;
		this.cloudName = cloud.getName();
		publishEvent(EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API, cloud.getProvider().getProvider());
		initDeployer(cloud);
		publishEvent(EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API, cloud.getProvider().getProvider());

		logger.fine("Initializing Cloud Provisioning - management mode: " + management + ". Using template: "
				+ cloudTemplateName + " with cloud: " + cloudName);

		String prefix = management ? cloud.getProvider().getManagementGroup() : cloud.getProvider()
				.getMachineNamePrefix();

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

	protected static void logServerDetails(final MachineDetails machineDetails, final File tempFile) {
		if (logger.isLoggable(Level.FINE)) {
			final String nodePrefix = "[" + machineDetails.getMachineId() + "] ";
			logger.fine(nodePrefix + "Cloud Server was allocated.");
			if (tempFile == null) {
				logger.fine(nodePrefix + "Password: ***");
			} else {
				logger.fine(nodePrefix + "Key File: " + tempFile.getAbsolutePath());
			}

			logger.fine(nodePrefix
					+ (machineDetails.isUsePrivateAddress() ? "Using Private IP: " : "Using Public IP: ")
					+ machineDetails.getIp());
		}
	}


	protected void handleServerCredentials(final MachineDetails machineDetails) throws CloudProvisioningException {
		File tempFile = null;

		if (org.apache.commons.lang.StringUtils.isBlank(machineDetails.getRemotePassword())) {
			if (cloud.getUser().getKeyFile() == null || cloud.getUser().getKeyFile().length() == 0) {
				logger.fine("No key file specified in cloud configuration");
				// no key file. Check for password
				if (cloud.getConfiguration().getRemotePassword() == null) {
					logger.severe("No Password or key file specified in cloud configuration - connection to the new machine is not possible.");
					throw new CloudProvisioningException(
							"No credentials (password or key file) supplied with the cloud configuration file");
				}
				machineDetails.setRemotePassword(cloud.getConfiguration().getRemotePassword());
			} else {
				tempFile = new File(cloud.getUser().getKeyFile());
			}
		} else if (CredentialUtils.isPrivateKeyCredential(machineDetails.getRemotePassword())) {
			// using a key file
			logger.fine("Cloud has provided a key file remote connections to new machine");
			try {
				tempFile = File.createTempFile("gs-esm-key", ".pem");
				FileUtils.write(tempFile, machineDetails.getRemotePassword());
				cloud.getUser().setKeyFile(tempFile.getAbsolutePath());
			} catch (final IOException e) {
				throw new CloudProvisioningException("Failed to create a temporary file for cloud server's key file",
						e);
			}

		} else {
			// using a password
			logger.fine("Cloud has provided a password for remote connections to new machine");
			cloud.getConfiguration().setRemotePassword(machineDetails.getRemotePassword());
		}

		final File keyFile = tempFile;
		logServerDetails(machineDetails, keyFile);
	}

	protected void publishEvent(final String eventName, final Object... args) {
		for (final ProvisioningDriverListener listener : this.eventsListenersList) {
			listener.onProvisioningEvent(eventName, args);
		}
	}
}
