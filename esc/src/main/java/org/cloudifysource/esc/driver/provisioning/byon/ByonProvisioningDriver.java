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
package org.cloudifysource.esc.driver.provisioning.byon;

import com.gigaspaces.grid.gsa.GSA;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.esc.byon.ByonDeployer;
import org.cloudifysource.esc.byon.ByonUtils;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomNode;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ManagementProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;
import org.cloudifysource.esc.util.FileUtils;
import org.cloudifysource.esc.util.Utils;
import org.cloudifysource.utilitydomain.openspaces.OpenspacesConstants;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.internal.gsa.InternalGridServiceAgent;
import org.openspaces.admin.internal.support.NetworkExceptionHelper;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**************
 * A bring-your-own-node (BYON) CloudifyProvisioning implementation. Parses a groovy file as a source of available
 * machines to operate as cloud nodes, assuming the nodes are Linux machines with SSH installed. If GigaSpaces is not
 * already installed on a node, this class will install GigaSpaces and run the agent.
 * 
 * @author noak
 * @since 2.0.1
 * 
 */
public class ByonProvisioningDriver extends BaseProvisioningDriver {

	private static final int MANAGEMENT_LOCATION_TIMEOUT = 10;
	private static final int THREAD_WAITING_IDLE_TIME_IN_SECS = 10;
	private static final String CLEAN_GS_FILES_ON_SHUTDOWN = "cleanGsFilesOnShutdown";
	private static final String CLOUDIFY_ITEMS_TO_CLEAN = "itemsToClean";
	private static ResourceBundle byonProvisioningDriverMessageBundle;

	private boolean cleanGsFilesOnShutdown = false;
	private List<String> cloudifyItems;
	private ByonDeployer deployer;
	private Integer restPort;

    private static final int DEFAULT_STOP_MANAGEMENT_TIMEOUT = 4;

    private int stopManagementMachinesTimeoutInMinutes = DEFAULT_STOP_MANAGEMENT_TIMEOUT;

	@Override
	protected void initDeployer(final Cloud cloud) {
		try {
			setDeployer((ByonDeployer) provisioningContext.getOrCreate("UNIQUE_BYON_DEPLOYER_ID",
					new Callable<Object>() {

						@Override
						public Object call()
								throws Exception {
							logger.info("Creating BYON context deployer for cloud: " + cloud.getName());
							final ByonDeployer newDeployer = new ByonDeployer();
							addTemplatesToDeployer(newDeployer, cloud.getCloudCompute().getTemplates());
							return newDeployer;
						}
					}));

            this.stopManagementMachinesTimeoutInMinutes = Utils.getInteger(cloud.getCustom().get(CloudifyConstants
                    .STOP_MANAGEMENT_TIMEOUT_IN_MINUTES), DEFAULT_STOP_MANAGEMENT_TIMEOUT);

		} catch (final Exception e) {
			publishEvent("connection_to_cloud_api_failed", cloud.getProvider().getProvider());
			throw new IllegalStateException("Failed to create cloud deployer", e);
		}
		try {
			updateDeployerTemplates(cloud);
		} catch (final Exception e) {
			logger.log(Level.WARNING, "initDeployer - fialed to add tempaltes to deployer", e);
			throw new IllegalStateException("Failed to update templates", e);
		}
		setCustomSettings(cloud);
	}

	private void addTemplatesToDeployer(final ByonDeployer deployer, final Map<String, ComputeTemplate> templatesMap)
			throws CloudProvisioningException {
		List<Map<String, String>> nodesList = null;

		for (final Entry<String, ComputeTemplate> templateEntry : templatesMap.entrySet()) {
			final String templateName = templateEntry.getKey();
			try {
				ComputeTemplate template = templateEntry.getValue();
				validateTemplate(templateName, template);
				nodesList = ByonUtils.getTemplateNodesList(templateEntry.getValue());				
			} catch (CloudProvisioningException e) {
				publishEvent(CloudifyErrorMessages.MISSING_NODES_LIST.getName(), templateName);
				throw new CloudProvisioningException("Failed to create BYON cloud deployer, invalid configuration for "
						+ "tempalte " + templateName + ", reported error: " + e.getMessage(), e);
			}
			deployer.addNodesList(templateName, templatesMap.get(templateName), nodesList);
		}
	}

	
	// if the hosts list include IPv6 addresses - verify the file transfer protocol is SCP
	private void validateTemplate(final String templateName, final ComputeTemplate template) 
			throws CloudProvisioningException {
		logger.fine("validating template [" + templateName + "]");
		boolean ipv6Used = false;
		
		try {
			List<CustomNode> nodes = ByonUtils.parseCloudNodes(template);
			for (CustomNode node : nodes) {
				if (StringUtils.isNotBlank(node.getPrivateIP()) && IPUtils.isIPv6Address(node.getPrivateIP())) {
					ipv6Used = true;
					break;
				}
			}
			
			if (ipv6Used) {
				//verify file transfer is set to SCP
				FileTransferModes fileTransferMode = template.getFileTransfer();
				if (fileTransferMode == null || !fileTransferMode.equals(FileTransferModes.SCP)) {
					throw new CloudProvisioningException("Invalid file transfer set for template " 
						+ templateName + ". Templates that use IPv6 addresses must use SCP for file transer.");
				}
			}
		} catch (CloudProvisioningException e) {
			throw new CloudProvisioningException("Invalid configuration for template " + templateName 
					+ ", reported error: " + e.getMessage(), e);
		}
	}	
	

	/**********
	 * .
	 * 
	 * @param cloud
	 *            .
	 * @throws Exception .
	 */
	public void updateDeployerTemplates(final Cloud cloud) throws CloudProvisioningException {
		final Map<String, ComputeTemplate> cloudTemplatesMap = cloud.getCloudCompute().getTemplates();
		final List<String> cloudTemplateNames = new LinkedList<String>(cloudTemplatesMap.keySet());
		final List<String> deployerTemplateNames = getDeployer().getTemplatesList();

		final List<String> redundantTemplates = new LinkedList<String>(deployerTemplateNames);
		redundantTemplates.removeAll(cloudTemplateNames);
		if (!redundantTemplates.isEmpty()) {
			logger.info("initDeployer - found redundant templates: " + redundantTemplates);
			getDeployer().removeTemplates(redundantTemplates);
		}
		final List<String> missingTemplates = new LinkedList<String>(cloudTemplateNames);
		missingTemplates.removeAll(deployerTemplateNames);
		if (!missingTemplates.isEmpty()) {
			logger.info("initDeployer - found missing templates: " + missingTemplates);
			final Map<String, ComputeTemplate> templatesMap = new HashMap<String, ComputeTemplate>();
			for (final String templateName : missingTemplates) {
				final ComputeTemplate cloudTemplate = cloudTemplatesMap.get(templateName);
				templatesMap.put(templateName, cloudTemplate);
			}
			addTemplatesToDeployer(getDeployer(), templatesMap);
		}
	}

	@SuppressWarnings("unchecked")
	private void setCustomSettings(final Cloud cloud) {
		initRestPort(cloud.getConfiguration().getComponents().getDiscovery().getPort());
		// set custom settings
		final Map<String, Object> customSettings = cloud.getCustom();
		if (customSettings != null) {
			// clean GS files on shutdown
			if (customSettings.containsKey(CLEAN_GS_FILES_ON_SHUTDOWN)) {
				final Object cleanOnShutdownStr = customSettings.get(CLEAN_GS_FILES_ON_SHUTDOWN);
				if (cleanOnShutdownStr != null && StringUtils.isNotBlank((String) cleanOnShutdownStr)) {
					cleanGsFilesOnShutdown = ((String) cleanOnShutdownStr).equalsIgnoreCase("true");
					if (cleanGsFilesOnShutdown) {
						// Cloudify download directory
						if (customSettings.containsKey(CLOUDIFY_ITEMS_TO_CLEAN)) {
							final Object cloudifyItemsStr = customSettings.get(CLOUDIFY_ITEMS_TO_CLEAN);
							if (cloudifyItemsStr != null) {
								cloudifyItems = (List<String>) cloudifyItemsStr;
							}
						}
					}
				}
			}
		}
	}

	private void initRestPort(final Integer port) {
		if (port != null) {
			this.restPort = port;
		} else {
			this.restPort = CloudifyConstants.DEFAULT_REST_PORT;
		}
	}

	@Override
	public MachineDetails startMachine(final ProvisioningContext context, final long timeout, final TimeUnit timeUnit)
			throws TimeoutException, CloudProvisioningException {

		final long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);

		logger.info(this.getClass().getName() + ": startMachine, management mode: " + management);

		final Set<String> activeMachinesIPs = admin.getMachines().getHostsByAddress().keySet();
		getDeployer().setAllocated(cloudTemplateName, activeMachinesIPs);
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Verifying the active machines are not in the free pool: "
					+ "\n Admin reports the currently used machines are: "
					+ Arrays.toString(activeMachinesIPs.toArray())
					+ "\n Byon deployer reports the free machines for template " + cloudTemplateName + " are: "
					+ Arrays.toString(getDeployer().getFreeNodesByTemplateName(cloudTemplateName).toArray())
					+ "\n Byon deployer reports the currently used machines for template " + cloudTemplateName
					+ " are:"
					+ Arrays.toString(getDeployer().getAllocatedNodesByTemplateName(cloudTemplateName).toArray())
					+ "\n Byon deployer reports the invalid used machines for template " + cloudTemplateName + " are: "
					+ Arrays.toString(getDeployer().getInvalidNodesByTemplateName(cloudTemplateName).toArray()) + ")");
		}
		final String newServerName = createNewServerName();
		logger.info("Attempting to start a new cloud machine");
		final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(cloudTemplateName);

		return createServer(newServerName, endTime, template);
	}

	@Override
	protected MachineDetails createServer(final String serverName, final long endTime, final ComputeTemplate template)
			throws CloudProvisioningException, TimeoutException {

		final CustomNode node;
		final MachineDetails machineDetails;
		logger.info("Cloudify Deployer is creating a machine named: " + serverName + ". This may take a few minutes");
		node = getDeployer().createServer(cloudTemplateName, serverName);

		machineDetails = createMachineDetailsFromNode(node);

		// At this point the machine is starting. Any error beyond this point
		// must clean up the machine.
		try {
			handleServerCredentials(machineDetails, template);
		} catch (final CloudProvisioningException e) {
			try {
				getDeployer().invalidateServer(cloudTemplateName, node);
			} catch (final CloudProvisioningException ie) {
				logger.log(Level.SEVERE, "Failed to mark machine [" + machineDetails.getPublicAddress() + "/"
						+ machineDetails.getPrivateAddress() + "] as Invalid.", ie);
			}
			throw new CloudProvisioningException(e);
		}

		if (System.currentTimeMillis() > endTime) {
			throw new TimeoutException();
		}

		logger.info("Machine successfully allocated");
		return machineDetails;
	}

	/*********
	 * Looks for a free server name by appending a counter to the pre-calculated server name prefix. If the max counter
	 * value is reached, code will loop back to 0, so that previously used server names will be reused.
	 * 
	 * @return the server name.
	 * @throws org.cloudifysource.esc.driver.provisioning.CloudProvisioningException
	 *             Indicated a free server name was not found.
	 */
	private String createNewServerName()
			throws CloudProvisioningException {

		String serverName = null;
		int attempts = 0;
		boolean foundFreeName = false;

		while (attempts < MAX_SERVERS_LIMIT) {
			// counter = (counter + 1) % MAX_SERVERS_LIMIT;
			++attempts;
			serverName = serverNamePrefix + BaseProvisioningDriver.counter.incrementAndGet();
			// verifying this server name is not already used
			final CustomNode existingNode = getDeployer().getServerByID(cloudTemplateName, serverName);
			if (existingNode == null) {
				foundFreeName = true;
				break;
			}
		}

		if (!foundFreeName) {
			publishEvent("prov_servers_limit_reached", MAX_SERVERS_LIMIT);
			throw new CloudProvisioningException("Number of servers has exceeded allowed server limit ("
					+ MAX_SERVERS_LIMIT + ")");
		}

		return serverName;
	}

	@Override
	public MachineDetails[] startManagementMachines(final ManagementProvisioningContext context, final long duration,
			final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {

		if (duration < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}
		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		logger.info("DefaultCloudProvisioning: startMachine - management == " + management);

		// first check if management already exists
		final MachineDetails[] mds = findManagementInAdmin();
		if (mds.length != 0) {
			return mds;
		}

		// launch the management machines
		publishEvent(EVENT_ATTEMPT_START_MGMT_VMS);
		final int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		final MachineDetails[] createdMachines = doStartManagementMachines(endTime, numberOfManagementMachines);
		publishEvent(EVENT_MGMT_VMS_STARTED);
		return createdMachines;
	}

	private MachineDetails[] findManagementInAdmin() throws CloudProvisioningException, TimeoutException {
		try {
			final Set<CustomNode> managementServers = getExistingManagementServers(0);
			if (managementServers != null && !managementServers.isEmpty()) {

				final MachineDetails[] managementMachines = new MachineDetails[managementServers.size()];
				int i = 0;
				for (final CustomNode node : managementServers) {
					managementMachines[i] = createMachineDetailsFromNode(node);
					managementMachines[i].setAgentRunning(true);
					managementMachines[i].setCloudifyInstalled(true);
					i++;
				}

				logger.info("Found existing management servers (" + Arrays.toString(managementMachines) + ")");

				return managementMachines;
			} else {
				logger.warning("Failed locating existing management servers");
			}
			return new MachineDetails[0];
		} catch (final InterruptedException e) {
			publishEvent("prov_management_lookup_failed");
			throw new CloudProvisioningException("Failed to lookup existing manahement servers.", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean stopMachine(final String serverIp, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException, InterruptedException {

		boolean stopResult = false;

		logger.info("Stop Machine - machineIp: " + serverIp);
		logger.info("Scale IN -- " + serverIp + " --");
		logger.info("Looking up cloud server with IP: " + serverIp);
		final CustomNode cloudNode = getDeployer().getServerByIP(cloudTemplateName, serverIp);
		if (cloudNode != null) {
			logger.info("Found server: " + cloudNode.getId()
					+ ". Shutting it down and waiting for shutdown to complete");
			shutdownServerGracefully(cloudNode, false);
			logger.info("Server: " + cloudNode.getId() + " shutdown has finished.");
			stopResult = true;
		} else {
			logger.log(Level.SEVERE, "Recieved scale in request for machine with ip " + serverIp
					+ " but this IP could not be found in the Cloud server list");
			stopResult = false;
		}

		return stopResult;
	}

	@Override
	public void stopManagementMachines()
			throws TimeoutException, CloudProvisioningException {

		Set<CustomNode> managementServers;

        final String managementMachinePrefix = this.cloud.getProvider().getManagementGroup();

        publishEvent(EVENT_RETRIEVE_EXISTING_MANAGEMENT_MACHINES, managementMachinePrefix);

        try {
			managementServers = getExistingManagementServers(cloud.getProvider().getNumberOfManagementMachines());
		} catch (final Exception e) {
			publishEvent("prov_management_lookup_failed");
			throw new CloudProvisioningException("Failed to lookup existing management servers.", e);
		}

        if (managementServers == null || managementServers.size() == 0) {
        	publishEvent("prov_management_lookup_failed");
        	throw new IllegalStateException("No management servers were found");
        }
        
        final int stopTimeoutPerAgent = stopManagementMachinesTimeoutInMinutes / managementServers.size();

        for (final CustomNode customNode : managementServers) {
			try {
				stopAgentAndWait(cloud.getProvider().getNumberOfManagementMachines(),
                        customNode.getPrivateIP(), stopTimeoutPerAgent);
			} catch (final Exception e) {
				publishEvent("prov_failed_to_stop_management_machine");
				throw new CloudProvisioningException(e);
			}

			shutdownServerGracefully(customNode, true);
		}
	}

	private Set<CustomNode> getExistingManagementServers(final int expectedGsmCount)
			throws CloudProvisioningException, TimeoutException, InterruptedException {
		// loop all IPs in the pool to find a mgmt machine - open on port 8100
		final Set<CustomNode> existingManagementServers = new HashSet<CustomNode>();
		final Set<CustomNode> allNodes = getDeployer().getAllNodesByTemplateName(cloudTemplateName);
		String managementIP = null;
		for (final CustomNode server : allNodes) {
			try {
				IPUtils.validateConnection(server.getPrivateIP(), this.restPort);
				managementIP = server.getPrivateIP();
				break;
			} catch (final Exception ex) {
				// the connection to the REST failed because this is not a
				// management server, continue.
			}
		}

		// If a management server was found - connect to it and get all management machines
		if (StringUtils.isNotBlank(managementIP)) {
			logger.fine("found management machine: " + managementIP);
			// TODO don't fly if timeout reached because expectedGsmCount wasn't reached
			final Integer discoveryPort = getLusPort();
			final Admin admin = Utils.getAdminObject(managementIP, expectedGsmCount, discoveryPort);
			try {
				final GridServiceManagers gsms = admin.getGridServiceManagers();
				// make sure a GSM is discovered
				gsms.waitForAtLeastOne(MANAGEMENT_LOCATION_TIMEOUT, TimeUnit.SECONDS);
				for (final GridServiceManager gsm : gsms) {
					final CustomNode managementServer = getDeployer().getServerByIP(cloudTemplateName, gsm.getMachine()
							.getHostAddress());
					if (managementServer != null) {
						existingManagementServers.add(managementServer);
					}
				}
			} catch (Exception e) {
				logger.info("Exception thrown while trying to discover GSMs, reported error: " 
						+ e.getMessage());
			} finally {
				admin.close();
			}

		}

		return existingManagementServers;
	}

	/**
	 * Gets The configured lus port, or the default if no port is configured.
	 * 
	 * @return the lus port.
	 */
	protected Integer getLusPort() {
		Integer discoveryPort = cloud.getConfiguration().getComponents().getDiscovery().getDiscoveryPort();
		if (discoveryPort == null) {
			discoveryPort = OpenspacesConstants.DEFAULT_LUS_PORT;
		}
		return discoveryPort;
	}

	private void stopAgentAndWait(final int expectedGsmCount, final String ipAddress,
                                  final int timeoutInMinutes)
			throws TimeoutException, InterruptedException {

		if (admin == null) {
			final Integer discoveryPort = getLusPort();
			admin = Utils.getAdminObject(ipAddress, expectedGsmCount, discoveryPort);
		}

		final Map<String, GridServiceAgent> agentsMap = admin.getGridServiceAgents().getHostAddress();
		GSA agent = null;
		for (final Entry<String, GridServiceAgent> agentEntry : agentsMap.entrySet()) {
			if (IPUtils.isSameIpAddress(agentEntry.getKey(), ipAddress)
					|| agentEntry.getKey().equalsIgnoreCase(ipAddress)) {
				agent = ((InternalGridServiceAgent) agentEntry.getValue()).getGSA();
			}
		}

		if (agent != null) {
			logger.info("Shutting down agent on server: " + ipAddress);
			try {
				admin.close();
				agent.shutdown();
			} catch (final RemoteException e) {
				if (!NetworkExceptionHelper.isConnectOrCloseException(e)) {
					logger.log(Level.FINER, "Failed to shutdown GSA", e);
					throw new AdminException("Failed to shutdown GSA", e);
				}
			}

			final long end = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeoutInMinutes);
			boolean agentUp = isAgentUp(agent);
			while (agentUp && System.currentTimeMillis() < end) {
				logger.fine("next check in " + TimeUnit.MILLISECONDS.toSeconds(THREAD_WAITING_IDLE_TIME_IN_SECS)
						+ " seconds");
				Thread.sleep(TimeUnit.SECONDS.toMillis(THREAD_WAITING_IDLE_TIME_IN_SECS));
				agentUp = isAgentUp(agent);
			}

			if (!agentUp && System.currentTimeMillis() >= end) {
				throw new TimeoutException("Agent shutdown timed out (agent IP: " + ipAddress + ")");
			}
		}
	}

	private void deleteGsFiles(final CustomNode cloudNode) {
		MachineDetails machineDetails = null;
		try {
			machineDetails = createMachineDetailsFromNode(cloudNode);

			String keyFile = "";
			if (machineDetails.getKeyFile() != null) {
				keyFile = machineDetails.getKeyFile().getAbsolutePath();
			}

			FileUtils.deleteFileSystemObjects(machineDetails.getPrivateAddress(), machineDetails.getRemoteUsername(),
					machineDetails.getRemotePassword(), keyFile, cloudifyItems,
					machineDetails.getFileTransferMode());
		} catch (final Exception e) {
			if (machineDetails != null) {
				logger.info("ByonProvisioningDriver: Failed to delete system files from server: "
						+ machineDetails.getPrivateAddress() + ", error: " + e.getMessage());
			} else {
				logger.info("ByonProvisioningDriver: Failed to delete system files from server, error: "
						+ e.getMessage());
			}
		}
	}

	private boolean isAgentUp(final GSA agent) {
		boolean agentUp = false;
		try {
			agent.ping();
			agentUp = true;
		} catch (final RemoteException e) {
			// Probably NoSuchObjectException meaning the GSA is going down
			agentUp = false;
		}

		return agentUp;
	}

	@Override
	protected void handleProvisioningFailure(final int numberOfManagementMachines, final int numberOfErrors,
			final Exception firstCreationException, final MachineDetails[] createdManagementMachines)
			throws CloudProvisioningException {
		logger.severe("Of the required " + numberOfManagementMachines + " management machines, " + numberOfErrors
				+ " failed to start.");
		if (numberOfManagementMachines > numberOfErrors) {
			logger.severe("Shutting down the other managememnt machines");

			for (final MachineDetails machineDetails : createdManagementMachines) {
				if (machineDetails != null) {
					logger.severe("Shutting down machine: " + machineDetails);
					getDeployer().shutdownServerByIp(cloudTemplateName, machineDetails.getPrivateAddress());
				}
			}
		}

		publishEvent("prov_management_machines_failed", firstCreationException.getMessage());
		throw new CloudProvisioningException(
				"One or more managememnt machines failed. The first encountered error was: "
						+ firstCreationException.getMessage(), firstCreationException);
	}

	private MachineDetails createMachineDetailsFromNode(final CustomNode node)
			throws CloudProvisioningException {
		final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);

		final MachineDetails md = createMachineDetailsForTemplate(template);

		md.setMachineId(node.getId());

//		md.setPrivateAddress(node.getHostName());
		md.setPrivateAddress(node.getPrivateIP());
		md.setPublicAddress(node.getPublicIP());

		// prefer node settings over template setting
		// prefer key file over password
		if (StringUtils.isNotBlank(node.getUsername()) && StringUtils.isNotBlank(node.getKeyFile())) {
			md.setRemoteUsername(node.getUsername());
			setKeyFile(md, node.getKeyFile(), template.getAbsoluteUploadDir());
		} else if (StringUtils.isNotBlank(node.getUsername()) && StringUtils.isNotBlank(node.getCredential())) {
			md.setRemoteUsername(node.getUsername());
			md.setRemotePassword(node.getCredential());
		} else if (StringUtils.isNotBlank(template.getUsername())
				&& StringUtils.isNotBlank(template.getKeyFile())) {
			md.setRemoteUsername(template.getUsername());
			setKeyFile(md, template.getKeyFile(), template.getAbsoluteUploadDir());
		} else if (StringUtils.isNotBlank(template.getUsername())
				&& StringUtils.isNotBlank(template.getPassword())) {
			md.setRemoteUsername(template.getUsername());
			md.setRemotePassword(template.getPassword());
		} else {
			final String nodeStr = node.toString();
			logger.severe("Cloud node loading failed, missing credentials for server: " + nodeStr);
			publishEvent("prov_node_loading_failed", nodeStr);
			throw new CloudProvisioningException("Cloud node loading failed, missing credentials for server: "
					+ nodeStr);
		}
		md.setOpenFilesLimit(template.getOpenFilesLimit());

		return md;
	}

	private void shutdownServerGracefully(final CustomNode cloudNode, final boolean isManagement)
			throws CloudProvisioningException {
		try {
			if (cleanGsFilesOnShutdown) {
				deleteGsFiles(cloudNode);
			}
			getDeployer().shutdownServer(cloudTemplateName, cloudNode);
		} catch (final Exception e) {
			if (isManagement) {
				publishEvent("prov_failed_to_stop_management_machine");
			} else {
				publishEvent("prov_failed_to_stop_machine");
			}
			throw new CloudProvisioningException(e);
		}
		logger.info("Server: " + cloudNode.getId() + " shutdown has finished.");
	}

	@Override
	public void close() {
		/*
		 * try { if (admin != null) { admin.close(); } } catch (final Exception ex) {
		 * logger.info("ByonProvisioningDriver.close() failed to close agent"); }
		 */

		if (getDeployer() != null) {
			getDeployer().close();
		}
	}

	public Cloud getCloud() {
		return this.cloud;
	}

	public ByonDeployer getDeployer() {
		return this.deployer;
	}

	@Override
	public Object getComputeContext() {
		return null;
	}

	@Override
	public void validateCloudConfiguration(final ValidationContext validationContext)
			throws CloudProvisioningException {
		// if the hosts list include IPv6 addresses - verify the file transfer protocol is SCP
		validationContext.validationOngoingEvent(ValidationMessageType.GROUP_VALIDATION_MESSAGE,
				getFormattedMessage("validating_all_templates"));

		ComputeTemplate template = null;
		
		Map<String, ComputeTemplate> templatesMap = cloud.getCloudCompute().getTemplates();
		
		validationContext.validationOngoingEvent(ValidationMessageType.GROUP_VALIDATION_MESSAGE,
				getFormattedMessage("validating_all_templates"));
		
		for (Entry<String, ComputeTemplate> templateEntry : templatesMap.entrySet()) {
			String templateName = templateEntry.getKey();
			template = templateEntry.getValue();
			validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
					getFormattedMessage("validating_template", templateName));
			try {
				validateTemplate(templateName, template);
				validationContext.validationEventEnd(ValidationResultType.OK);
			} catch (final CloudProvisioningException e) {
				validationContext.validationEventEnd(ValidationResultType.ERROR);

				throw e;
			}
		}


	}


	@Override
	public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
		throw new UnsupportedOperationException("Cannot retrieve existing management servers after shutting down"
				+ " agents");
	}

	@Override
	public MachineDetails[] getExistingManagementServers(final ControllerDetails[] controllers)
			throws CloudProvisioningException, UnsupportedOperationException {
		final Set<String> ips = new HashSet<String>();
		for (final ControllerDetails controllerDetails : controllers) {
			ips.add(controllerDetails.getPrivateIp());
		}

		final Set<CustomNode> allNodesByTemplateName =
				this.getDeployer().getAllNodesByTemplateName(this.cloud.getConfiguration().getManagementMachineTemplate());
		final Set<CustomNode> managementNodes = new HashSet<CustomNode>();

		for (final CustomNode node : allNodesByTemplateName) {
			if (ips.contains(node.getPrivateIP()) || ips.contains(node.getHostName())) {
				managementNodes.add(node);
			}
		}

		final MachineDetails[] result = new MachineDetails[managementNodes.size()];
		int i = 0;
		for (final CustomNode node : managementNodes) {
			result[i] = createMachineDetailsFromNode(node);
			++i;
		}
		return result;

	}

	private void setKeyFile(final MachineDetails md, final String keyFileName, final String parentFolder)
			throws CloudProvisioningException {

		File keyFile = new File(keyFileName);
		if (!keyFile.isAbsolute()) {
			keyFile = new File(parentFolder, keyFileName);
		}

		if (!keyFile.isFile()) {
			throw new CloudProvisioningException("The specified key file could not be found: "
					+ keyFile.getAbsolutePath());
		}

		md.setKeyFile(keyFile);
	}

	/**
	 * returns the message as it appears in the ByonProvisioningDriver message bundle.
	 * 
	 * @param msgName
	 *            the message key as it is defined in the message bundle.
	 * @param arguments
	 *            the message arguments
	 * @return the formatted message according to the message key.
	 */
	protected String getFormattedMessage(final String msgName, final Object... arguments) {
		return getFormattedMessage(getByonProvisioningDriverMessageBundle(), msgName, arguments);
	}

	/**
	 * Returns the message bundle of this cloud driver.
	 * 
	 * @return the message bundle of this cloud driver.
	 */
	protected static ResourceBundle getByonProvisioningDriverMessageBundle() {
		if (byonProvisioningDriverMessageBundle == null) {
			byonProvisioningDriverMessageBundle = ResourceBundle.getBundle("ByonProvisioningDriverMessages",
					Locale.getDefault());
		}
		return byonProvisioningDriverMessageBundle;
	}

	public void setDeployer(ByonDeployer deployer) {
		this.deployer = deployer;
	}

}