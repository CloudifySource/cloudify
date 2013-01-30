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
package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.discovery.LookupLocator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.cloud.storage.StorageTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.ComputeTemplatesReader;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.esc.driver.provisioning.context.DefaultProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.driver.provisioning.events.MachineStartRequestedCloudifyEvent;
import org.cloudifysource.esc.driver.provisioning.events.MachineStartedCloudifyEvent;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.util.CalcUtils;
import org.cloudifysource.esc.util.Utils;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.bean.BeanConfigurationException;
import org.openspaces.admin.gsa.GSAReservationId;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.ElasticGridServiceAgentProvisioningProgressChangedEventListener;
import org.openspaces.admin.machine.events.ElasticMachineProvisioningProgressChangedEventListener;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.openspaces.admin.zone.config.ExactZonesConfigurer;
import org.openspaces.admin.zone.config.ZonesConfig;
import org.openspaces.core.bean.Bean;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.CpuCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;
import org.openspaces.grid.gsm.machines.isolation.ElasticProcessingUnitMachineIsolation;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioning;
import org.openspaces.grid.gsm.machines.plugins.events.GridServiceAgentStartRequestedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.GridServiceAgentStartedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.GridServiceAgentStopRequestedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.GridServiceAgentStoppedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStopRequestedEvent;
import org.openspaces.grid.gsm.machines.plugins.events.MachineStoppedEvent;
import org.openspaces.grid.gsm.machines.plugins.exceptions.ElasticGridServiceAgentProvisioningException;
import org.openspaces.grid.gsm.machines.plugins.exceptions.ElasticMachineProvisioningException;

/****************************
 * An ESM machine provisioning implementation used by the Cloudify cloud driver. All calls to start/stop a machine are
 * delegated to the CloudifyProvisioning implementation. If the started machine does not have an agent running, this
 * class will install gigaspaces and start the agent using the Agent-less Installer process.
 * 
 * @author barakme
 * 
 */
public class ElasticMachineProvisioningCloudifyAdapter implements ElasticMachineProvisioning, Bean {

	private static final String REMOTE_ADMIN_SHARE_CHAR = "$";

	private static final String BACK_SLASH = "\\";

	private static final String FORWARD_SLASH = "/";

	private static final int MILLISECONDS_IN_SECOND = 1000;

	private static final int DEFAULT_SHUTDOWN_TIMEOUT_AFTER_PROVISION_FAILURE = 5;

	/**********
	 * .
	 */
	public static final String CLOUD_ZONE_PREFIX = "__cloud.zone.";

	// TODO: Store this object inside ElasticMachineProvisioningContext instead of a static variable
	private static final Map<String, ProvisioningDriverClassContext> PROVISIONING_DRIVER_CONTEXT_PER_DRIVER_CLASSNAME =
			new HashMap<String, ProvisioningDriverClassContext>();

	private static Admin globalAdminInstance = null;
	private static final Object GLOBAL_ADMIN_MUTEX = new Object();

	private static final long DEFAULT_AGENT_DISCOVERY_INTERVAL = 1000L;

	private static Admin getGlobalAdminInstance(final Admin esmAdminInstance) {
		synchronized (GLOBAL_ADMIN_MUTEX) {
			if (globalAdminInstance == null) {
				// create admin clone from esm instance
				final AdminFactory factory = new AdminFactory();
				for (final String group : esmAdminInstance.getGroups()) {
					factory.addGroup(group);
				}
				for (final LookupLocator locator : esmAdminInstance.getLocators()) {
					factory.addLocator(locator.getHost() + ":" + locator.getPort());
				}
				globalAdminInstance = factory.createAdmin();
			}

			return globalAdminInstance;
		}
	}

	private ProvisioningDriver cloudifyProvisioning;
	private StorageProvisioningDriver storageProvisioning;
	private Admin originalESMAdmin;
	private Cloud cloud;
	private Map<String, String> properties;
	private String cloudTemplateName;
	private String storageTemplateName;
	private String lookupLocatorsString;
	private CloudifyMachineProvisioningConfig config;
	private java.util.logging.Logger logger;

	private String serviceName;

	private ElasticMachineProvisioningProgressChangedEventListener machineEventListener;
	private ElasticGridServiceAgentProvisioningProgressChangedEventListener agentEventListener;


	@Override
	public boolean isStartMachineSupported() {
		return true;
	}

	@Override
	public GridServiceAgent[] getDiscoveredMachines(final long duration, final TimeUnit unit)
			throws InterruptedException, TimeoutException {

		final List<GridServiceAgent> result = new ArrayList<GridServiceAgent>();
		final GridServiceAgent[] agents = this.originalESMAdmin.getGridServiceAgents().getAgents();
		for (final GridServiceAgent agent : agents) {
			final String template = agent.getVirtualMachine().getDetails()
					.getEnvironmentVariables().get(CloudifyConstants.GIGASPACES_CLOUD_TEMPLATE_NAME);
			if (template != null) { // management machines don't have this variable attached
				if (template.equals(this.cloudTemplateName)) {
					result.add(agent);
				}
			} else {
				logger.fine("in getDiscoveredMachines() --> agent on host " + agent.getMachine().getHostAddress()
						+ " does not have a template name attached to its env variables");
			}
		}

		return result.toArray(new GridServiceAgent[result.size()]);
	}

	private InstallationDetails createInstallationDetails(final Cloud cloud, final MachineDetails md,
			final GSAReservationId reservationId) throws FileNotFoundException {
		final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);

		// Start with the default zone that are also used for discovering agents
		// By default cloudify puts the service-name as the zone.
		// We then add the location of the machine to the zone, so if it fails the ESM starts it with these zones
		// and this adapter can look for the CLOUD_ZONE_PREFIX and start a machine with the same location.
		// TODO Fix GS-9484 and then remove the service name from the machine zone and remove the CLOUD_ZONE_PREFIX.

		final ExactZonesConfigurer configurer = new ExactZonesConfigurer()
				.addZones(config.getGridServiceAgentZones().getZones());

		if (!StringUtils.isBlank(md.getLocationId())) {
			configurer.addZone(CLOUD_ZONE_PREFIX + md.getLocationId());
		}

		final ExactZonesConfig zones = configurer.create();

		final InstallationDetails details =
				Utils.createInstallationDetails(md, cloud, template, zones, lookupLocatorsString,
						this.originalESMAdmin, false,
						null, reservationId, cloudTemplateName, ""/* securityProfile */, ""/* keystorePassword */,
						config.getAuthGroups());

		logger.info("Created new Installation Details: " + details);
		return details;

	}

	@Override
	public GridServiceAgent startMachine(final long duration, final TimeUnit unit)
			throws ElasticMachineProvisioningException,
			ElasticGridServiceAgentProvisioningException, InterruptedException, TimeoutException {
		final GSAReservationId reservationId = null;
		final ExactZonesConfig zones = new ExactZonesConfig();
		return startMachine(zones, reservationId, duration, unit);
	}

	@Override
	public GridServiceAgent startMachine(final ExactZonesConfig zones, final GSAReservationId reservationId,
			final long duration, final TimeUnit unit)
			throws ElasticMachineProvisioningException,
			ElasticGridServiceAgentProvisioningException, InterruptedException, TimeoutException {

		logger.info("Cloudify Adapter is starting a new machine with zones " + zones.getZones());

		// calculate timeout
		final long end = System.currentTimeMillis() + unit.toMillis(duration);

		// provision the machine
		logger.info("Calling provisioning implementation for new machine");
		MachineDetails machineDetails;
		cloudifyProvisioning.setAdmin(getGlobalAdminInstance(originalESMAdmin));

		final ZonesConfig defaultZones = config.getGridServiceAgentZones();
		logger.fine("default zones = " + defaultZones.getZones());
		if (!defaultZones.isSatisfiedBy(zones)) {
			throw new IllegalArgumentException("The specified zones " + zones
					+ " does not satisfy the configuration zones " + defaultZones);
		}

		String locationId = null;

		logger.fine("searching for cloud specific zone");
		for (final String zone : zones.getZones()) {
			logger.fine("current zone = " + zone);
			if (zone.startsWith(CLOUD_ZONE_PREFIX)) {
				logger.fine("found a zone with " + CLOUD_ZONE_PREFIX + " prefix : " + zone);
				if (locationId == null) {
					locationId = zone.substring(CLOUD_ZONE_PREFIX.length());
					logger.fine("passing locationId to machine provisioning as " + locationId);
				} else {
					throw new IllegalArgumentException("The specified zones " + zones
							+ " should include only one zone with the " + CLOUD_ZONE_PREFIX + " prefix:" + locationId);
				}
			}
		}

		final MachineStartRequestedCloudifyEvent machineStartEvent = new MachineStartRequestedCloudifyEvent();
		machineStartEvent.setTemplateName(cloudTemplateName);
		machineStartEvent.setLocationId(locationId);
		machineEventListener.elasticMachineProvisioningProgressChanged(machineStartEvent);

		try {
			final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);
			if (locationId == null) {
				final ComputeTemplate template = cloud.getTemplates().get(this.cloudTemplateName);
				locationId = template.getLocationId();
			}

			// This is the call to the actual cloud driver implementation!
			machineDetails = provisionMachine(locationId, duration, unit);

			// Auto populate installer configuration with values set in template if they were not previously set.
			if (machineDetails != null && machineDetails.getInstallerConfigutation() == null) {
				machineDetails.setInstallerConfigutation(template.getInstaller());
			}

		} catch (final Exception e) {
			throw new ElasticMachineProvisioningException("Failed to provisiong machine: " + e.getMessage(), e);
		}

		logger.info("Machine was provisioned by implementation. Machine is: " + machineDetails);

		// which IP should be used in the cluster
		String machineIp;
		if (cloud.getConfiguration().isConnectToPrivateIp()) {
			machineIp = machineDetails.getPrivateAddress();
		} else {
			machineIp = machineDetails.getPublicAddress();
		}
		if (machineIp == null) {
			throw new IllegalStateException("The IP of the new machine is null! Machine Details are: "
					+ machineDetails + " .");
		}

		final MachineStartedCloudifyEvent machineStartedEvent = new MachineStartedCloudifyEvent();
		machineStartedEvent.setMachineDetails(machineDetails);
		machineStartedEvent.setHostAddress(machineIp);
		machineEventListener.elasticMachineProvisioningProgressChanged(machineStartedEvent);

		final GridServiceAgentStartRequestedEvent agentStartEvent = new GridServiceAgentStartRequestedEvent();
		agentStartEvent.setHostAddress(machineIp);
		agentEventListener.elasticGridServiceAgentProvisioningProgressChanged(agentStartEvent);

		try {
			// check for timeout
			checkForProvisioningTimeout(end, machineDetails);
			
			if (isStorageTemplateUsed()) {
				String machineLocation = machineDetails.getLocationId();
				
				VolumeDetails volumeDetails = startNewStorageVolume(machineLocation, end);
				try {
					attachStorageVolumeToMachine(machineIp, volumeDetails , end);
				} catch (Exception e) {
					logger.log(Level.WARNING, "Failed attaching volume. Error was " + e.getMessage(), e);
					handleVolumeAttachException(volumeDetails);
					throw new StorageProvisioningException(e);
				}
				StorageTemplate storageTemplate = this.cloud.getCloudStorage().getTemplates()
								.get(this.storageTemplateName);
				String formatType = storageTemplate.getFileSystemType();
				String mountPath = storageTemplate.getPath();
				String deviceName = storageTemplate.getDeviceName();

				machineDetails.setStorageVolumeAttached(true);
				machineDetails.setStorageDeviceName(deviceName);
				machineDetails.setStorageFormatType(formatType);
				machineDetails.setStorageMountPath(mountPath);
			}
			if (machineDetails.isAgentRunning()) {
				logger.info("Machine provisioning provided a machine and indicated that an agent is already running");
			} else {
				// install gigaspaces and start agent
				logger.info("Cloudify Adapter is installing Cloudify on new machine");
				installAndStartAgent(machineDetails, reservationId, end);
				// check for timeout again - the installation step can also take a
				// while to complete.
				checkForProvisioningTimeout(end, machineDetails);
			}

			// There is another case here, that we do not handle - where the started image
			// has the cloudify distro, but the agent is not running. This means we should
			// run a modified remote execution script. Not sure if we really need this, though,
			// as this scenario really does not offer a better experience. If required, handling will
			// be added here.

			// wait for GSA to become available
			logger.info("Cloudify adapter is waiting for GSA to become available");
			final GridServiceAgent gsa = waitForGsa(machineIp, end);
			if (gsa == null) {
				// GSA did not start correctly or on time - shutdown the machine
				// handleGSANotFound(machineIp);
				throw new TimeoutException("New machine was provisioned and Cloudify was installed, "
						+ "but a GSA was not discovered on the new machine: " + machineDetails);
			}
			// TODO: Derive cloudify specific event and include more event details as specified in CLOUDIFY-10651
			agentEventListener.elasticGridServiceAgentProvisioningProgressChanged(
					new GridServiceAgentStartedEvent(machineIp, gsa.getUid()));

			// check that the agent is really started with the expected env variable of the template
			// we inject this variable earlier on to the bootstrap-management.sh script
			if (gsa.getVirtualMachine().getDetails().getEnvironmentVariables().
					get(CloudifyConstants.GIGASPACES_CLOUD_TEMPLATE_NAME) == null) {
				throw new ElasticGridServiceAgentProvisioningException("an agent was started. but the property "
						+ CloudifyConstants.GIGASPACES_CLOUD_TEMPLATE_NAME
						+ " was missing from its environment variables.");
			}

			return gsa;
		} catch (final ElasticMachineProvisioningException e) {
			logger.info("ElasticMachineProvisioningException occurred, " + e.getMessage());
			logger.info(ExceptionUtils.getFullStackTrace(e));
			handleExceptionAfterMachineCreated(machineIp, machineDetails, end);
			throw e;
		} catch (final ElasticGridServiceAgentProvisioningException e) {
			logger.info("ElasticGridServiceAgentProvisioningException occurred, " + e.getMessage());
			logger.info(ExceptionUtils.getFullStackTrace(e));
			handleExceptionAfterMachineCreated(machineIp, machineDetails, end);
			throw e;
		} catch (final TimeoutException e) {
			logger.info("TimeoutException occurred, " + e.getMessage());
			logger.info(ExceptionUtils.getFullStackTrace(e));
			handleExceptionAfterMachineCreated(machineIp, machineDetails, end);
			throw e;
		} catch (final InterruptedException e) {
			logger.info("InterruptedException occurred, " + e.getMessage());
			logger.info(ExceptionUtils.getFullStackTrace(e));
			handleExceptionAfterMachineCreated(machineIp, machineDetails, end);
			throw e;
		} catch (StorageProvisioningException e) {
			logger.info("StorageProvisioningException occurred, " + e.getMessage());
			logger.info(ExceptionUtils.getFullStackTrace(e));
			handleExceptionAfterMachineCreated(machineIp, machineDetails, end);
			//temporary, this will be replaced when a throws is added to the interface on openspaces.
			throw new ElasticMachineProvisioningException("Storage exception thrown", e);
		} 
	}

	void handleVolumeAttachException(VolumeDetails volumeDetails) {
		try {
			this.storageProvisioning.deleteVolume(volumeDetails.getId(), 1, TimeUnit.MINUTES);
		}catch (Exception e) {
			logger.log(
					Level.WARNING,
					"Volume attachment failed. "
							+ "Failed cleaning volume after attachment failure. ( "
							+ volumeDetails.getId() + "). Error was: " + e.getMessage(), e);
		}
	}

	boolean isStorageTemplateUsed() {
		return !StringUtils.isEmpty(this.storageTemplateName);
	}

	void attachStorageVolumeToMachine(final String machineIp, final VolumeDetails volumeDetails, final long end)
			throws TimeoutException, StorageProvisioningException {
		long timeout;
		timeout = end - System.currentTimeMillis();
		String volumeId = volumeDetails.getId();
		this.storageProvisioning.attachVolume(volumeId, machineIp, timeout, TimeUnit.MILLISECONDS);
	}

	// in-case creation fails, the provisioning driver will clean the new volume.
	VolumeDetails startNewStorageVolume(final String machineLocation, final long end) throws TimeoutException,
			StorageProvisioningException {
		long timeout = end - System.currentTimeMillis();
		ComputeTemplate computeTemplate = this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);
		VolumeDetails volumeDetails = this.storageProvisioning
				.createVolume(machineLocation, timeout, TimeUnit.MILLISECONDS);
		return volumeDetails;
	}

	private void handleExceptionAfterMachineCreated(final String machineIp, final MachineDetails machineDetails,
			final long end) {
		try {
			// if an agent is found (not supposed to, we got here after it wasn't found earlier) - shut it down
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
			try {
				if (isStorageTemplateUsed()) {
					handleVolumeOnStopMachine(machineIp, end, TimeUnit.MILLISECONDS);
				}
			} catch (StorageProvisioningException e) {
				logger.log(Level.SEVERE, "Failed detaching volume from machine with ip: " 
								+ machineIp + ". There may still be leaking volumes.", e);
			}
			this.cloudifyProvisioning.stopMachine(machineDetails.getPrivateAddress(),
					DEFAULT_SHUTDOWN_TIMEOUT_AFTER_PROVISION_FAILURE, TimeUnit.MINUTES);
		} catch (final Exception e) {
			logger.log(
					Level.WARNING,
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

	private void installAndStartAgent(final MachineDetails machineDetails,
			final GSAReservationId reservationId, final long end)
			throws TimeoutException, InterruptedException,
			ElasticMachineProvisioningException, ElasticGridServiceAgentProvisioningException {
		final AgentlessInstaller installer = new AgentlessInstaller();

		InstallationDetails installationDetails;
		try {
			// since only agents are started by this method, server-security is set to false
			installationDetails = createInstallationDetails(cloud, machineDetails, reservationId);
		} catch (final FileNotFoundException e) {
			throw new ElasticGridServiceAgentProvisioningException("Failed to create installation details for agent: "
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
			throw new ElasticGridServiceAgentProvisioningException(
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

	private MachineDetails provisionMachine(final String locationId, final long duration, final TimeUnit unit)
			throws TimeoutException, ElasticMachineProvisioningException {

		MachineDetails machineDetails;
		try {
			machineDetails = cloudifyProvisioning.startMachine(locationId, duration, unit);
		} catch (final CloudProvisioningException e) {
			throw new ElasticMachineProvisioningException("Failed to start machine: " + e.getMessage(), e);
		}

		if (machineDetails == null) {
			throw new IllegalStateException("Provisioning provider: " + cloudifyProvisioning.getClass().getName()
					+ " returned null when calling startMachine");
		}

		logger.info("New machine was provisioned. Machine details: " + machineDetails);
		return machineDetails;
	}

	private GridServiceAgent waitForGsa(final String machineIp, final long end)
			throws InterruptedException, TimeoutException {

		while (CalcUtils.millisUntil(end) > 0) {
			final GridServiceAgent gsa = getGSAByIpOrHost(machineIp);
			if (gsa != null) {
				return gsa;
			}

			Thread.sleep(MILLISECONDS_IN_SECOND);

		}
		return null;

	}

	private GridServiceAgent getGSAByIpOrHost(final String machineIp) {
		GridServiceAgent gsa = originalESMAdmin.getGridServiceAgents().getHostAddress().get(machineIp);
		if (gsa != null) {
			return gsa;
		}

		gsa = originalESMAdmin.getGridServiceAgents().getHostNames().get(machineIp);
		if (gsa != null) {
			return gsa;
		}
		return null;
	}

	@Override
	public CapacityRequirements getCapacityOfSingleMachine() {
		final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);
		final CapacityRequirements capacityRequirements =
				new CapacityRequirements(new MemoryCapacityRequirement((long) template.getMachineMemoryMB()),
						new CpuCapacityRequirement(template.getNumberOfCores()));
		logger.info("Capacity requirements for a single machine are: " + capacityRequirements);
		return capacityRequirements;

	}

	@Override
	public boolean stopMachine(final GridServiceAgent agent, final long duration, final TimeUnit unit)
			throws ElasticMachineProvisioningException,
			ElasticGridServiceAgentProvisioningException,
			InterruptedException, TimeoutException {
		
		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);
		final String machineIp = agent.getMachine().getHostAddress();
		try {
			if (isStorageTemplateUsed()) {
				handleVolumeOnStopMachine(machineIp, duration, unit);
			}
		} catch (StorageProvisioningException e) {
			logger.log(Level.SEVERE, "Failed detaching volume from machine with ip: " 
							+ machineIp + ". There may still be leaking volumes.", e);
		}
		
		Exception failedToShutdownAgentException = null;
		final GridServiceAgentStopRequestedEvent agentStopEvent = new GridServiceAgentStopRequestedEvent();
		agentStopEvent.setHostAddress(machineIp);
		agentStopEvent.setAgentUid(agent.getUid());
		agentEventListener.elasticGridServiceAgentProvisioningProgressChanged(agentStopEvent);

		logger.fine("Shutting down agent: " + agent + " on host: " + machineIp);
		try {
			agent.shutdown();
			logger.fine("Agent on host: " + machineIp + " successfully shut down");

			final GridServiceAgentStoppedEvent agentStoppedEvent = new GridServiceAgentStoppedEvent();
			agentStoppedEvent.setHostAddress(machineIp);
			agentStoppedEvent.setAgentUid(agent.getUid());
			agentEventListener.elasticGridServiceAgentProvisioningProgressChanged(agentStoppedEvent);
		} catch (final Exception e) {
			failedToShutdownAgentException = e;
			logger.log(Level.FINE, "Failed to shutdown agent on host: " + machineIp
					+ ". Continuing with shutdown of machine.", e);
		}

		try {

			final MachineStopRequestedEvent machineStopEvent = new MachineStopRequestedEvent();
			machineStopEvent.setHostAddress(machineIp);
			machineEventListener.elasticMachineProvisioningProgressChanged(machineStopEvent);

			logger.fine("Cloudify Adapter is shutting down machine with ip: " + machineIp);
			final boolean shutdownSuccessful = this.cloudifyProvisioning.stopMachine(machineIp, duration, unit);
			logger.fine("Shutdown result of machine: " + machineIp + " was: " + shutdownSuccessful);

			if (shutdownSuccessful) {
				final MachineStoppedEvent machineStoppedEvent = new MachineStoppedEvent();
				machineStoppedEvent.setHostAddress(machineIp);
				machineEventListener.elasticMachineProvisioningProgressChanged(machineStoppedEvent);

				// machine was shutdown, but an error happened while shutting down agent.
				// lets wait for the agent to not be discovered until we reach the timeout.

				while (agent.isDiscovered()) {
					Thread.sleep(DEFAULT_AGENT_DISCOVERY_INTERVAL);
					if (System.currentTimeMillis() > endTime && agent.isDiscovered()) {
						if (failedToShutdownAgentException != null) {
							throw new ElasticGridServiceAgentProvisioningException(
									"Machine is stopped but agent [" + agent.getUid() + "] is still discovered."
											+ "Failed to shutdown agent:" + failedToShutdownAgentException.getMessage(),
									failedToShutdownAgentException);
						}
						throw new ElasticGridServiceAgentProvisioningException(
								"Machine is stopped but agent[" + agent.getUid()
										+ "] is still discovered.");

					}
				}

			}

			return shutdownSuccessful;

		} catch (final CloudProvisioningException e) {
			throw new ElasticMachineProvisioningException("Attempt to shutdown machine with IP: " + machineIp
					+ " for agent with UID: " + agent.getUid() + " has failed with error: " + e.getMessage(), e);
		}
	}

	void handleVolumeOnStopMachine(final String machineIp, final long duration, 
			final TimeUnit unit) throws TimeoutException,
			StorageProvisioningException {
		logger.info("detaching all volumes attachet to machine with ip: " + machineIp);
		Set<VolumeDetails> attachedVolumes = this.storageProvisioning.listVolumes(machineIp, duration, unit);
		StorageTemplate storageTemplate = this.cloud.getCloudStorage().getTemplates().get(this.storageTemplateName);
		for (VolumeDetails volumeDetails : attachedVolumes) {
			String id = volumeDetails.getId();
			logger.info("getting volume name using volume id " + id);
			String volumeName = this.storageProvisioning.getVolumeName(id);
			String namePrefix = storageTemplate.getNamePrefix();
			if (!StringUtils.isEmpty(volumeName) && volumeName.startsWith(namePrefix)) {
				logger.info("detaching volume with id " + id + " from machine with ip: " + machineIp);
				this.storageProvisioning.detachVolume(id, machineIp, duration, unit);
				boolean deleteOnExit = storageTemplate.isDeleteOnExit();
				if (deleteOnExit) {
					logger.info("deleting volume with id " + id);
					this.storageProvisioning.deleteVolume(id, duration, unit);
				}
			}
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
		this.originalESMAdmin = admin;

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

		final String cloudConfigDirectoryPath =
				properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_CONFIGURATION_DIRECTORY);
		if (cloudConfigDirectoryPath == null) {
			logger.severe("Missing cloud configuration property. Properties are: " + this.properties);
			throw new IllegalArgumentException("Cloud configuration directory was not set!");
		}

		try {
			final String cloudOverridesPerService = config.getCloudOverridesPerService();
			this.cloud = ServiceReader.readCloudFromDirectory(cloudConfigDirectoryPath,
					cloudOverridesPerService);
			this.cloudTemplateName = properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME);

			if (this.cloudTemplateName == null) {
				throw new BeanConfigurationException("Cloud template was not set!");
			}
			// add additional templates from cloudConfigDirectory.
			addTemplatesToCloud(new File(cloudConfigDirectoryPath));
			final ComputeTemplate computeTemplate = 
					this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);
			if (computeTemplate == null) {
				throw new BeanConfigurationException("The provided cloud template name: " + this.cloudTemplateName
						+ " was not found in the cloud configuration");
			}

			// This code runs on the ESM in the remote machine,
			// so set the local directory to the value of the remote directory
			logger.info("Remote Directory is: " + computeTemplate.getRemoteDirectory());
			// if running a windows server.
			if (computeTemplate.getFileTransfer() == FileTransferModes.CIFS) {
				logger.info("Windows machine - modifying local directory location");
				final String remoteDirName = computeTemplate.getRemoteDirectory();
				final String windowsLocalDirPath =
						getWindowsLocalDirPath(remoteDirName, computeTemplate.getLocalDirectory());
				logger.info("Modified local dir name is: " + windowsLocalDirPath);

				computeTemplate.setLocalDirectory(windowsLocalDirPath);
			} else {
				computeTemplate.setLocalDirectory(computeTemplate.getRemoteDirectory());
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

				// checks if a service level configuration exists. If so, save the configuration to local file and pass
				// to cloud driver.
				handleServiceCloudConfiguration();
				this.cloudifyProvisioning.setConfig(cloud, cloudTemplateName, false, serviceName);
				this.storageTemplateName = config.getStorageTemplateName();
				if (isStorageTemplateUsed()) {
					boolean privileged = computeTemplate.isPrivileged();
					// mounting the volume will not be possible if not running in privileged mode. 
					if (!privileged) {
						logger.warning("Storage template defined but not running in privileged mode.");
						throw new StorageProvisioningException("Storage mounting requires running in privileged mode."
								+ " This should be defined in the cloud's compute template.");
					}
					logger.info("creating storage provisioning driver.");
					this.storageProvisioning = 
							(StorageProvisioningDriver) Class.forName(this.cloud.getConfiguration()
									.getStorageClassName()).newInstance();
					this.storageProvisioning.setComputeContext(cloudifyProvisioning.getComputeContext());
					this.storageProvisioning.setConfig(cloud, this.cloudTemplateName, this.storageTemplateName);
					logger.info("storage provisioning driver created successfully.");
				}
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
			logger.severe("Could not parse the provided cloud configuration from : " + cloudConfigDirectoryPath + ": "
					+ e.getMessage());
			throw new BeanConfigurationException("Could not parse the prvided cloud configuration: "
					+ cloudConfigDirectoryPath
					+ ": " + e.getMessage(), e);
		}

	}

	private void addTemplatesToCloud(final File cloudConfigDirectory) {
		logger.info("addTemplatesToCloud - adding templates from directory " + cloudConfigDirectory.getAbsolutePath());
		final File additionalTemplatesFolder = new File(cloudConfigDirectory,
				CloudifyConstants.ADDITIONAL_TEMPLATES_FOLDER_NAME);
		if (!additionalTemplatesFolder.exists()) {
			logger.info("addTemplatesToCloud - no additional templates to add from directory "
					+ cloudConfigDirectory.getAbsolutePath());
			return;
		}
		final File[] listFiles = additionalTemplatesFolder.listFiles();
		logger.info("addTemplatesToCloud - found files: " + Arrays.toString(listFiles));
		ComputeTemplatesReader reader = new ComputeTemplatesReader();
		List<ComputeTemplate> addedTemplates = reader.addAdditionalTemplates(cloud, listFiles);
		logger.info("addTemplatesToCloud - Added " + addedTemplates.size() + " templates to the cloud: " + addedTemplates);
	}

	private String getWindowsLocalDirPath(final String remoteDirectoryPath, final String localDirName) {
		final String homeDirectoryName = getWindowsRemoteDirPath(remoteDirectoryPath);
		final File localDirectory = new File(homeDirectoryName, localDirName);
		return localDirectory.getAbsolutePath();
	}

	private String getWindowsRemoteDirPath(final String remoteDirectoryPath) {
		String homeDirectoryName = remoteDirectoryPath;
		homeDirectoryName = homeDirectoryName.replace(REMOTE_ADMIN_SHARE_CHAR, "");
		if (homeDirectoryName.startsWith(FORWARD_SLASH)) {
			homeDirectoryName = homeDirectoryName.substring(1);
		}
		if (homeDirectoryName.charAt(1) == FORWARD_SLASH.charAt(0)) {
			homeDirectoryName = homeDirectoryName.substring(0, 1) + ":" + homeDirectoryName.substring(1);
		}
		homeDirectoryName = homeDirectoryName.replace(FORWARD_SLASH, BACK_SLASH);
		return homeDirectoryName;
	}

	private void handleServiceCloudConfiguration()
			throws IOException {
		final byte[] serviceCloudConfigurationContents = this.config.getServiceCloudConfiguration();
		if (serviceCloudConfigurationContents != null) {
			logger.info("Found service cloud configuration - saving to file");
			final File tempZipFile = File.createTempFile("__CLOUD_DRIVER_SERVICE_CONFIGURATION_FILE", ".zip");
			FileUtils.writeByteArrayToFile(tempZipFile, serviceCloudConfigurationContents);
			logger.info("Wrote file: " + tempZipFile);

			final File tempServiceConfigurationDirectory =
					File.createTempFile("__CLOUD_DRIVER_SERVICE_CONFIGURATION_DIRECTORY", ".tmp");
			logger.info("Unzipping file to: " + tempServiceConfigurationDirectory);
			FileUtils.forceDelete(tempServiceConfigurationDirectory);
			tempServiceConfigurationDirectory.mkdirs();

			ZipUtils.unzip(tempZipFile, tempServiceConfigurationDirectory);

			final File[] childFiles = tempServiceConfigurationDirectory.listFiles();

			logger.info("Unzipped configuration contained top-level entries: " + Arrays.toString(childFiles));
			if (childFiles.length != 1) {
				throw new BeanConfigurationException(
						"Received a service cloud configuration file, "
								+ "but root of zip file had more then one entry!");
			}

			final File serviceCloudConfigurationFile = childFiles[0];

			if (this.cloudifyProvisioning instanceof CustomServiceDataAware) {
				logger.info("Setting service cloud configuration in cloud driver to: " + serviceCloudConfigurationFile);
				final CustomServiceDataAware custom = (CustomServiceDataAware) this.cloudifyProvisioning;
				custom.setCustomDataFile(serviceCloudConfigurationFile);
			} else {
				throw new BeanConfigurationException(
						"Cloud driver configuration inclouded a service cloud configuration file,"
								+ " but the cloud driver "
								+ this.cloudifyProvisioning.getClass().getName() + " does not implement the "
								+ CustomServiceDataAware.class.getName() + " interface");
			}
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
		final LookupLocator[] locators = this.originalESMAdmin.getLocators();
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
		if (isStorageTemplateUsed()) {
			this.storageProvisioning.close();
		}
		// not closing globalAdminMutex, it's a static object, and this is intentional.
	}

	/**
	 * @param isolation
	 *            - describes the relation between different service instances on the same machine Assuming each service
	 *            has a dedicated machine {@link org.openspaces.grid.gsm.machines.isolation.DedicatedMachineIsolation;},
	 *            the machine isolation name is the service name. This would change when instances from different
	 *            services would be installed on the same machine using
	 *            {@link org.openspaces.grid.gsm.machines.isolation.SharedMachineIsolation}.
	 */
	@Override
	public void setElasticProcessingUnitMachineIsolation(final ElasticProcessingUnitMachineIsolation isolation) {

		this.serviceName = isolation.getName();
	}

	@Override
	public void setElasticMachineProvisioningProgressChangedEventListener(
			final ElasticMachineProvisioningProgressChangedEventListener machineEventListener) {
		this.machineEventListener = machineEventListener;
	}

	@Override
	public void setElasticGridServiceAgentProvisioningProgressEventListener(
			final ElasticGridServiceAgentProvisioningProgressChangedEventListener agentEventListener) {
		this.agentEventListener = agentEventListener;
	}
}
