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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
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
import org.cloudifysource.esc.driver.provisioning.context.DefaultProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.util.Utils;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.bean.BeanConfigurationException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.core.bean.Bean;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.CpuCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;
import org.openspaces.grid.gsm.machines.isolation.ElasticProcessingUnitMachineIsolation;
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

	private static final int MILLISECONDS_IN_SECOND = 1000;

	private static final int DEFAULT_SHUTDOWN_TIMEOUT_AFTER_PROVISION_FAILURE = 5;

	// TODO: Store this object inside ElasticMachineProvisioningContext instead of a static variable
	private static final Map<String, ProvisioningDriverClassContext> PROVISIONING_DRIVER_CONTEXT_PER_DRIVER_CLASSNAME =
			new HashMap<String, ProvisioningDriverClassContext>();

	private ProvisioningDriver cloudifyProvisioning;
	private Admin admin;
	private Map<String, String> properties;
	private Cloud cloud;
	private String cloudTemplate;
	private String lookupLocatorsString;
	private CloudifyMachineProvisioningConfig config;
	private java.util.logging.Logger logger;
	private ElasticProcessingUnitMachineIsolation isolation;

	@Override
	public boolean isStartMachineSupported() {
		return true;
	}

	@Override
	public GridServiceAgent[] getDiscoveredMachines(final long duration, final TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {
		// TODO - query the cloud and cross reference with the admin
		return this.admin.getGridServiceAgents().getAgents();
	}

	private InstallationDetails createInstallationDetails(final Cloud cloud, final MachineDetails md)
			throws FileNotFoundException {
		// TODO - move this to a util package - it is copied in bootstrap-cloud
		final InstallationDetails details = new InstallationDetails();

		details.setLocalDir(cloud.getProvider().getLocalDirectory());
		final String remoteDir = getRemoteDir();
		details.setRemoteDir(remoteDir);
		details.setManagementOnlyFiles(cloud.getProvider().getManagementOnlyFiles());
		final String[] zones = this.config.getGridServiceAgentZones();
		details.setZones(StringUtils.join(zones, ",", 0, zones.length));

		if (org.apache.commons.lang.StringUtils.isNotBlank(cloud.getUser().getKeyFile())) {
			logger.info("Key file has been specified in cloud configuration: " + cloud.getUser().getKeyFile());
			final File keyFile = new File(cloud.getProvider().getLocalDirectory(), cloud.getUser().getKeyFile());
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
		details.setOverridesUrl(cloud.getProvider().getCloudifyOverridesUrl());
		details.setConnectedToPrivateIp(cloud.getConfiguration().isConnectToPrivateIp());
		details.setAdmin(this.admin);

		details.setUsername(md.getRemoteUsername());
		details.setPassword(md.getRemotePassword());
		details.setRemoteExecutionMode(md.getRemoteExecutionMode());
		details.setFileTransferMode(md.getFileTransferMode());
		logger.info("Created new Installation Details: " + details);
		return details;

	}

	private String getRemoteDir() {
		// TODO - there really should be a template field
		final CloudTemplate template = this.cloud.getTemplates().get(this.cloudTemplate);
		if (template.getRemoteDirectory() != null) {
			return template.getRemoteDirectory();
		}

		return this.cloud.getProvider().getRemoteDirectory();
	}

	@Override
	public GridServiceAgent startMachine(final long duration, final TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {

		logger.info("Cloudify Adapter is starting a new machine");
		// calculate timeout
		final long end = System.currentTimeMillis() + unit.toMillis(duration);

		// provision the machine
		logger.info("Calling provisioning implementation for new machine");
		MachineDetails machineDetails = null;
		try {
			machineDetails = provisionMachine(duration, unit);
		} catch (final Exception e) {
			logger.log(Level.SEVERE, "Failed to provisiong machine: " + e.getMessage(), e);
			throw new ElasticMachineProvisioningException("Failed to provisiong machine: " + e.getMessage(), e);
		}

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

			if (machineIp == null) {
				throw new IllegalArgumentException("The IP of the new machine is null! Machine Details are: "
						+ machineDetails + " .");
			}
			// wait for GSA to become available
			logger.info("Cloudify adapter is waiting for GSA to become available");
			final GridServiceAgent gsa = waitForGsa(machineIp, end);
			if (gsa == null) {
				// GSA did not start correctly or on time - shutdown the machine
				// handleGSANotFound(machineIp);
				throw new TimeoutException("New machine was provisioned and Cloudify was installed, "
						+ "but a GSA was not discovered on the new machine: " + machineDetails);
			}
			return gsa;
		} catch (final ElasticMachineProvisioningException e) {
			logger.info("ElasticMachineProvisioningException occurred, " + e.getMessage());
			logger.info(Arrays.toString(e.getStackTrace()));
			handleExceptionAfterMachineCreated(machineDetails, end);
			throw e;
		} catch (final TimeoutException e) {
			logger.info("TimeoutException occurred, " + e.getMessage());
			logger.info(Arrays.toString(e.getStackTrace()));
			handleExceptionAfterMachineCreated(machineDetails, end);
			throw e;
		} catch (final InterruptedException e) {
			logger.info("InterruptedException occurred, " + e.getMessage());
			logger.info(Arrays.toString(e.getStackTrace()));
			handleExceptionAfterMachineCreated(machineDetails, end);
			throw e;
		}

	}

	private void handleExceptionAfterMachineCreated(final MachineDetails machineDetails, final long end) {
		try {
			// if an agent is found (not supposed to, we got here after it wasn't found earlier) - shut it down
			String machineIp = null;
			if (machineDetails.isUsePrivateAddress()) {
				machineIp = machineDetails.getPrivateAddress();
			} else {
				machineIp = machineDetails.getPublicAddress();
			}

			if (machineIp != null && !machineIp.trim().isEmpty()) {
				try {
					final GridServiceAgent agent = getGSAByIpOrHost(machineIp);
					if (agent != null) {
						logger.info("handleExceptionAfterMachineCreated is shutting down agent: " + agent
								+ " on host: " + machineIp);

						agent.shutdown();
						logger.fine("Agent on host: " + machineIp + " successfully shut down");

					}
				} catch (final Exception e) {
					// even if shutting down the agent failed, this node will be shut down later
					logger.log(Level.WARNING, "Failed to shutdown agent on host: " + machineIp
							+ ". Continuing with shutdown of " + "machine.", e);
				}
			}

			logger.info("Stopping machine " + machineDetails.getPrivateAddress()
					+ ", DEFAULT_SHUTDOWN_TIMEOUT_AFTER_PROVISION_FAILURE");
			this.cloudifyProvisioning.stopMachine(machineDetails.getPrivateAddress(),
					DEFAULT_SHUTDOWN_TIMEOUT_AFTER_PROVISION_FAILURE, TimeUnit.MINUTES);
		} catch (final Exception e) {
			logger.log(
					Level.SEVERE,
					"Machine Provisioning failed. "
							+ "An error was encountered while trying to shutdown the new machine ( "
							+ machineDetails.toString() + "). Error was: " + e.getMessage(), e);
		}
	}

	private void checkForProvisioningTimeout(final long end, final MachineDetails machineDetails)
			throws TimeoutException, ElasticMachineProvisioningException, InterruptedException {
		if (System.currentTimeMillis() > end) {
			logger.warning("Provisioning of new machine exceeded the required timeout. Shutting down the new machine ("
					+ machineDetails.toString() + ")");
			// creating the new machine took too long! clean up and throw a
			// timeout
			throw new TimeoutException("New machine provisioning exceeded the required timeout");

		}
	}

	private void installAndStartAgent(final MachineDetails machineDetails, final long end)
			throws TimeoutException, InterruptedException, ElasticMachineProvisioningException {
		final AgentlessInstaller installer = new AgentlessInstaller();

		InstallationDetails installationDetails;
		try {
			installationDetails = createInstallationDetails(cloud, machineDetails);
		} catch (final FileNotFoundException e) {
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
		} catch (final InstallerException e) {
			throw new ElasticMachineProvisioningException(
					"Failed to install Cloudify Agent on newly provisioned machine: " + e.getMessage(), e);
		}
	}

	private long remainingTimeTill(final long end)
			throws TimeoutException {
		final long remaining = end - System.currentTimeMillis();
		if (remaining <= 0) {
			throw new TimeoutException("Passed target end time " + new Date(end));
		}
		return remaining;
	}

	private MachineDetails provisionMachine(final long duration, final TimeUnit unit)
			throws TimeoutException, ElasticMachineProvisioningException {
		MachineDetails machineDetails;
		try {
			// delegate provisioning to the cloud driver implementation
			cloudifyProvisioning.setAdmin(admin);
			machineDetails = cloudifyProvisioning.startMachine(duration, unit);
		} catch (final CloudProvisioningException e) {
			throw new ElasticMachineProvisioningException("Failed to start machine: " + e.getMessage(), e);
		}

		if (machineDetails == null) {
			throw new IllegalStateException("Provisioning provider: " + cloudifyProvisioning.getClass().getName()
					+ " returned a null when calling startMachine");
		}

		logger.info("New machine was provisioned. Machine details: " + machineDetails);
		return machineDetails;
	}

	private GridServiceAgent waitForGsa(final String machineIp, final long end)
			throws InterruptedException, TimeoutException {

		while (Utils.millisUntil(end) > 0) {
			GridServiceAgent gsa = getGSAByIpOrHost(machineIp);
			if (gsa != null) {
				return gsa;
			}

			Thread.sleep(MILLISECONDS_IN_SECOND);

		}
		return null;

	}

	private GridServiceAgent getGSAByIpOrHost(final String machineIp) {
		GridServiceAgent gsa = admin.getGridServiceAgents().getHostAddress().get(machineIp);
		if (gsa != null) {
			return gsa;
		}

		gsa = admin.getGridServiceAgents().getHostNames().get(machineIp);
		if (gsa != null) {
			return gsa;
		}
		return null;
	}

	@Override
	public CapacityRequirements getCapacityOfSingleMachine() {
		final CloudTemplate template = cloud.getTemplates().get(this.cloudTemplate);
		final CapacityRequirements capacityRequirements =
				new CapacityRequirements(new MemoryCapacityRequirement((long) template.getMachineMemoryMB()),
						new CpuCapacityRequirement(template.getNumberOfCores()));
		logger.info("Capacity requirements for a single machine are: " + capacityRequirements);
		return capacityRequirements;

	}

	@Override
	public boolean stopMachine(final GridServiceAgent agent, final long duration, final TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {

		// TODO - move INFO printouts to FINE
		final String machineIp = agent.getMachine().getHostAddress();
		logger.fine("Shutting down agent: " + agent + " on host: " + machineIp);
		try {
			agent.shutdown();
			logger.fine("Agent on host: " + machineIp + " successfully shut down");
		} catch (final Exception e) {
			logger.log(Level.WARNING, "Failed to shutdown agent on host: " + machineIp
					+ ". Continuing with shutdown of machine.", e);
		}

		try {
			logger.fine("Cloudify Adapter is shutting down machine with ip: " + machineIp);

			final boolean shutdownResult = this.cloudifyProvisioning.stopMachine(machineIp, duration, unit);
			logger.info("Shutdown result of machine: " + machineIp + " was: " + shutdownResult);

			return shutdownResult;

		} catch (final CloudProvisioningException e) {
			throw new ElasticMachineProvisioningException("Attempt to shutdown machine with IP: " + machineIp
					+ " for agent with UID: " + agent.getUid() + " has failed with error: " + e.getMessage(), e);
		} catch (final AdminException e) {
			throw new ElasticMachineProvisioningException("Failed to shutdown agent "
					+ agent.getMachine().getHostAddress(), e);
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
	public void setAdmin(final Admin admin) {
		this.admin = admin;

	}

	@Override
	public void setProperties(final Map<String, String> properties) {
		this.properties = properties;
		this.config = new CloudifyMachineProvisioningConfig(properties);

	}

	@Override
	public Map<String, String> getProperties() {
		return this.properties;
	}

	@Override
	public void afterPropertiesSet()
			throws Exception {
		
		 logger = java.util.logging.Logger
					.getLogger(ElasticMachineProvisioningCloudifyAdapter.class.getName());
		
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
			// TODO - change the condition to ServiceUtils.isWindows
			logger.info("Remote Directory is: " + cloud.getProvider().getRemoteDirectory());
			if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
				logger.info("Windows machine - modifying local directory location");
				String localDirectoryName = cloud.getProvider().getRemoteDirectory();
				localDirectoryName = localDirectoryName.replace("$", "");
				if (localDirectoryName.startsWith("/")) {
					localDirectoryName = localDirectoryName.substring(1);
				}
				if (localDirectoryName.charAt(1) == '/') {
					localDirectoryName = localDirectoryName.substring(0, 1) + ":" + localDirectoryName.substring(1);
				}
				logger.info("Modified local dir name is: " + localDirectoryName);

				cloud.getProvider().setLocalDirectory(localDirectoryName);
			} else {
				cloud.getProvider().setLocalDirectory(cloud.getProvider().getRemoteDirectory());
			}

			// load the provisioning class and set it up
			try {
				this.cloudifyProvisioning =
						(ProvisioningDriver) Class.forName(this.cloud.getConfiguration().getClassName()).newInstance();

				if (cloudifyProvisioning instanceof ProvisioningDriverClassContextAware) {
					final ProvisioningDriverClassContext provisioningDriverContext =
							lazyCreateProvisioningDriverClassContext(cloudifyProvisioning);
					final ProvisioningDriverClassContextAware contextAware =
							(ProvisioningDriverClassContextAware) cloudifyProvisioning;
					contextAware.setProvisioningDriverClassContext(provisioningDriverContext);
				}

				this.cloudifyProvisioning.setConfig(cloud, cloudTemplate, false);

			} catch (final ClassNotFoundException e) {
				throw new BeanConfigurationException("Failed to load provisioning class for cloud: "
						+ this.cloud.getName() + ". Class not found: " + this.cloud.getConfiguration().getClassName(),
						e);
			} catch (final Exception e) {
				throw new BeanConfigurationException("Failed to load provisioning class for cloud: "
						+ this.cloud.getName(), e);
			}

			this.lookupLocatorsString = createLocatorsString();

			logger.info("Locators string used for new instances will be: " + this.lookupLocatorsString);

		} catch (final DSLException e) {
			logger.severe("Could not parse the provided cloud configuration: " + cloudContents + ": " + e.getMessage());
			throw new BeanConfigurationException("Could not parse the prvided cloud configuration: " + cloudContents
					+ ": " + e.getMessage());
		}

	}

	private static ProvisioningDriverClassContext lazyCreateProvisioningDriverClassContext(
			final ProvisioningDriver cloudifyProvisioning) {

		final String cloudDriverUniqueId = cloudifyProvisioning.getClass().getName();
		synchronized (PROVISIONING_DRIVER_CONTEXT_PER_DRIVER_CLASSNAME) {
			if (!PROVISIONING_DRIVER_CONTEXT_PER_DRIVER_CLASSNAME.containsKey(cloudDriverUniqueId)) {
				PROVISIONING_DRIVER_CONTEXT_PER_DRIVER_CLASSNAME.put(cloudDriverUniqueId,
						new DefaultProvisioningDriverClassContext());
			}
		}
		final ProvisioningDriverClassContext provisioningDriverContext =
				PROVISIONING_DRIVER_CONTEXT_PER_DRIVER_CLASSNAME.get(cloudDriverUniqueId);
		return provisioningDriverContext;
	}

	private String createLocatorsString() {
		final LookupLocator[] locators = this.admin.getLocators();
		final StringBuilder sb = new StringBuilder();
		for (final LookupLocator lookupLocator : locators) {
			sb.append(lookupLocator.getHost()).append(':').append(lookupLocator.getPort()).append(',');
		}
		if (!sb.toString().isEmpty()) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	@Override
	public void destroy()
			throws Exception {
		this.cloudifyProvisioning.close();
	}

	@Override
	public void setElasticProcessingUnitMachineIsolation(ElasticProcessingUnitMachineIsolation isolation) {
		this.isolation = isolation;
	}

}
