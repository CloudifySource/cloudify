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
package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.discovery.LookupLocator;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.esm.CloudMachineProvisioningConfig;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.bean.BeanConfigurationException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.core.bean.Bean;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.CpuCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioning;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioningException;

import com.gigaspaces.internal.utils.StringUtils;

/****************************
 * An ESM machine provisioning implementation used by the Cloudify cloud driver. All calls to start/stop a machine are
 * delegated to the CloudifyProvisioning implementation. If the started machine does not have an agent running, this
 * class will install gigaspaces and start the agent using the Agent-less Installer process.
 * 
 * @author barakme
 * 
 */
public class ElasticMachineProvisioningCloudifyAdapter implements ElasticMachineProvisioning, Bean {

	private static final int DEFAULT_GSA_LOOKUP_TIMEOUT_SECONDS = 15;
	private CloudifyProvisioning cloudifyProvisioning;
	private Admin admin;
	private Map<String, String> properties;
	private Cloud cloud;
	private String cloudTemplate;
	private String lookupLocatorsString;
	private CloudMachineProvisioningConfig config;
	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ElasticMachineProvisioningCloudifyAdapter.class.getName());

	@Override
	public boolean isStartMachineSupported() {
		return true;
	}

	@Override
	public GridServiceAgent[] getDiscoveredMachines(long duration, TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {
		// TODO - query the cloud and cross reference with the admin
		return this.admin.getGridServiceAgents().getAgents();
	}

	protected InstallationDetails createInstallationDetails(final Cloud cloud, final MachineDetails md)
			throws FileNotFoundException {
		final InstallationDetails details = new InstallationDetails();

		details.setLocalDir(cloud.getProvider().getLocalDirectory());
		details.setRemoteDir(cloud.getProvider().getRemoteDirectory());
		details.setManagementOnlyFiles(cloud.getProvider().getManagementOnlyFiles());
		details.setZones(StringUtils.join(cloud.getProvider().getZones().toArray(new String[0]), ",", 0, cloud
				.getProvider().getZones().size()));

		if (cloud.getUser().getKeyFile() != null) {
			logger.info("Key file has been specified in cloud configuration: " + cloud.getUser().getKeyFile());
			File keyFile = new File(cloud.getProvider().getLocalDirectory(), cloud.getUser().getKeyFile());
			if (keyFile.exists()) {
				details.setKeyFile(keyFile.getAbsolutePath());
				logger.info("Using key file: " + keyFile);
			} else {
				throw new FileNotFoundException(
						"Could not find key file matching specified cloud configuration key file: "
								+ cloud.getUser().getKeyFile() + ". Tried: " + keyFile + " but file does not exist");
			}

		}

		details.setPrivateIp(md.getPrivateAddress());
		details.setPublicIp(md.getPublicAddress());

		details.setLocator(this.lookupLocatorsString);
		details.setLus(false);
		details.setCloudifyUrl(cloud.getProvider().getCloudifyUrl());
		details.setConnectedToPrivateIp(cloud.getConfiguration().isConnectToPrivateIp());
		details.setAdmin(this.admin);

		details.setUsername(md.getRemoteUsername());
		details.setPassword(md.getRemotePassword());
		logger.info("Created new Installation Details: " + details);
		return details;

	}

	@Override
	public GridServiceAgent startMachine(long duration, TimeUnit unit) throws ElasticMachineProvisioningException,
			InterruptedException, TimeoutException {

		logger.info("Cloudify Adapter is starting a new machine");
		// calculate timeout
		final long end = System.currentTimeMillis() + unit.toMillis(duration);

		// provision the machine
		logger.info("Calling provisioning implementation for new machine");
		MachineDetails machineDetails = provisionMachine(duration, unit);

		logger.info("Machine was provisioned by implementation. Machine is: " + machineDetails);

		try {
			// check for timeout
			checkForProvisioningTimeout(end, machineDetails);

			// TODO - finish this section - support picking up existing
			// installations and agent. - i.e. machineDetails.cloudifyInstalled == true

			// install gigaspaces and start agent
			logger.info("Cloudify Adapter is installing Cloudify on new machine");
			installAndStartAgent(machineDetails, end);

			// check for timeout again - the installation step can also take a
			// while to complete.
			checkForProvisioningTimeout(end, machineDetails);

			// which IP should be used in the cluster
			String machineIp = null;
			if (machineDetails.isUsePrivateAddress()) {
				machineIp = machineDetails.getPrivateAddress();
			} else {
				machineIp = machineDetails.getPublicAddress();
			}

			// wait for GSA to become available
			logger.info("Cloudify adapter is waiting for GSA to become available");
			GridServiceAgent gsa = waitForGsa(machineIp, DEFAULT_GSA_LOOKUP_TIMEOUT_SECONDS);
			if (gsa == null) {
				// GSA did not start correctly or on time - shutdown the machine

				// handleGSANotFound(machineIp);
				throw new TimeoutException(
						"New machine was provisioned and Cloudify was installed, but a GSA was not discovered on the new machine: "
								+ machineDetails);
			}
			return gsa;
		} catch (ElasticMachineProvisioningException e) {
			handleExceptionAfterMachineCreated(machineDetails);
			throw e;
		} catch (TimeoutException e) {
			handleExceptionAfterMachineCreated(machineDetails);
			throw e;
		} catch (InterruptedException e) {
			handleExceptionAfterMachineCreated(machineDetails);
			throw e;
		}

	}

	private void handleExceptionAfterMachineCreated(final MachineDetails machineDetails) {
		try {
			this.cloudifyProvisioning.stopMachine(machineDetails.getPrivateAddress(), 5, TimeUnit.MINUTES);
		} catch (Exception e) {
			logger.log(
					Level.SEVERE,
					"Machine Provisioning failed. "
							+ "An error was encountered while trying to shutdown the new machine ( "
							+ machineDetails.toString() + "). Error was: " + e.getMessage(), e);
		}
	}

	private void checkForProvisioningTimeout(final long end, MachineDetails machineDetails) throws TimeoutException,
			ElasticMachineProvisioningException, InterruptedException {
		if (System.currentTimeMillis() > end) {
			logger.warning("Provisioning of new machine exceeded the required timeout. Shutting down the new machine ("
					+ machineDetails.toString() + ")");
			// creating the new machine took too long! clean up and throw a
			// timeout
			throw new TimeoutException("New machine provisioning exceeded the required timeout");

		}
	}

	private void installAndStartAgent(MachineDetails machineDetails, long end) throws TimeoutException,
			InterruptedException, ElasticMachineProvisioningException {
		final AgentlessInstaller installer = new AgentlessInstaller();

		InstallationDetails installationDetails;
		try {
			installationDetails = createInstallationDetails(cloud, machineDetails);
		} catch (FileNotFoundException e) {
			throw new ElasticMachineProvisioningException("Failed to create installation details for agent: "
					+ e.getMessage(), e);
		}

		logger.info("Starting agentless installation process on started machine with installation details: "
				+ installationDetails);
		// Update the logging level of jsch used by the AgentlessInstaller
		Logger.getLogger(AgentlessInstaller.SSH_LOGGER_NAME).setLevel(
				Level.parse(cloud.getProvider().getSshLoggingLevel()));

		// Execute agentless installation on the remote machine
		try {
			installer.installOnMachineWithIP(installationDetails, remainingTimeTill(end), TimeUnit.MILLISECONDS);
		} catch (InstallerException e) {
			throw new ElasticMachineProvisioningException(
					"Failed to install Cloudify Agent on newly provisioned machine: " + e.getMessage(), e);
		}
	}

	private long remainingTimeTill(long end) throws TimeoutException {
		long remaining = end - System.currentTimeMillis();
		if (remaining <= 0) {
			throw new TimeoutException("Passed target end time " + new Date(end));
		}
		return remaining;
	}

	private MachineDetails provisionMachine(long duration, TimeUnit unit) throws TimeoutException,
			ElasticMachineProvisioningException {
		MachineDetails machineDetails;
		try {
			// delegate provisioning to the cloud driver implementation
			machineDetails = cloudifyProvisioning.startMachine(duration, unit);
		} catch (CloudProvisioningException e) {
			throw new ElasticMachineProvisioningException("Failed to start machine: " + e.getMessage());
		}
		if (machineDetails == null) {
			throw new IllegalStateException("Provisioning provider: " + cloudifyProvisioning.getClass().getName()
					+ " returned a null when calling startMachine");
		}

		logger.info("New machine was provisioned. Machine details: " + machineDetails);
		return machineDetails;
	}

	private GridServiceAgent waitForGsa(String machineIp, int timeoutInSeconds) throws InterruptedException {

		long endTime = System.currentTimeMillis() + (timeoutInSeconds * 1000);

		while (System.currentTimeMillis() < endTime) {
			GridServiceAgent gsa = admin.getGridServiceAgents().getHostAddress().get(machineIp);
			if (gsa != null) {
				return gsa;
			}

			gsa = admin.getGridServiceAgents().getHostNames().get(machineIp);
			if (gsa != null) {
				return gsa;
			}

			Thread.sleep(1000);

		}
		return null;

	}

	@Override
	public CapacityRequirements getCapacityOfSingleMachine() {
		CloudTemplate template = cloud.getTemplates().get(this.cloudTemplate);
		CapacityRequirements capacityRequirements = new CapacityRequirements(new MemoryCapacityRequirement(
				(long) template.getMachineMemoryMB()), new CpuCapacityRequirement(template.getNumberOfCores()));
		logger.info("Capacity requirements for a single machine are: " + capacityRequirements);
		return capacityRequirements;

	}

	@Override
	public boolean stopMachine(GridServiceAgent agent, long duration, TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {

		final String machineIp = agent.getMachine().getHostAddress();
		try {
			logger.fine("Cloudify Adapter is shutting down machine with ip: " + machineIp);
			final boolean shutdownResult = this.cloudifyProvisioning.stopMachine(machineIp, duration, unit);
			logger.fine("Shutdown result of machine: " + machineIp + " was: " + shutdownResult);

			return shutdownResult;

		} catch (CloudProvisioningException e) {
			throw new ElasticMachineProvisioningException("Attempt to shutdown machine with IP: " + machineIp
					+ " for agent with UID: " + agent.getUid() + " has failed with error: " + e.getMessage(), e);
		}

	}

	@Override
	public ElasticMachineProvisioningConfig getConfig() {
		return this.config;
	}

	// //////////////////////////////////
	// OpenSpaces Bean Implementation //
	// //////////////////////////////////
	@Override
	public void setAdmin(Admin admin) {
		this.admin = admin;

	}

	@Override
	public void setProperties(final Map<String, String> properties) {
		this.properties = properties;
		this.config = new CloudMachineProvisioningConfig(properties);

	}

	@Override
	public Map<String, String> getProperties() {
		return this.properties;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final String cloudContents = properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_CONFIGURATION);
		if (cloudContents == null) {
			throw new IllegalArgumentException("Cloud configuration was not set!");
		}

		try {
			this.cloud = ServiceReader.readCloud(cloudContents);
			this.cloudTemplate = properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME);

			if (this.cloudTemplate == null) {
				throw new BeanConfigurationException("Cloud template was not set!");
			}

			// This code runs on the ESM in the remote machine,
			// so set the local directory to the value of the remote directory
			cloud.getProvider().setLocalDirectory(cloud.getProvider().getRemoteDirectory());

			// load the provisioning class and set it up
			try {
				this.cloudifyProvisioning = (CloudifyProvisioning) Class.forName(
						this.cloud.getConfiguration().getClassName()).newInstance();
				this.cloudifyProvisioning.setConfig(cloud, cloudTemplate, false);

			} catch (Exception e) {
				throw new BeanConfigurationException("Failed to load provisioning class from cloud: " + this.cloud, e);
			}

			this.lookupLocatorsString = createLocatorsString();

			logger.info("Locators string used for new instances will be: " + this.lookupLocatorsString);

		} catch (DSLException e) {
			logger.severe("Could not parse the provided cloud configuration: " + cloudContents + ": " + e.getMessage());
			throw new BeanConfigurationException("Could not parse the prvided cloud configuration: " + cloudContents
					+ ": " + e.getMessage());
		}

	}

	private String createLocatorsString() {
		LookupLocator[] locators = this.admin.getLocators();
		StringBuilder sb = new StringBuilder();
		for (LookupLocator lookupLocator : locators) {
			sb.append(lookupLocator.getHost()).append(":").append(lookupLocator.getPort()).append(",");
		}
		if (sb.toString().length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	@Override
	public void destroy() throws Exception {
		this.cloudifyProvisioning.close();
	}

}
