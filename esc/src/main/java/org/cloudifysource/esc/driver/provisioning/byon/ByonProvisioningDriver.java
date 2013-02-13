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

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.esc.byon.ByonDeployer;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomNode;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ManagementLocator;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.util.FileUtils;
import org.cloudifysource.esc.util.IPUtils;
import org.cloudifysource.esc.util.Utils;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.internal.gsa.InternalGridServiceAgent;
import org.openspaces.admin.internal.support.NetworkExceptionHelper;

import com.gigaspaces.grid.gsa.GSA;

/**************
 * A bring-your-own-node (BYON) CloudifyProvisioning implementation. Parses a groovy file as a source of available
 * machines to operate as cloud nodes, assuming the nodes are Linux machines with SSH installed. If GigaSpaces is not
 * already installed on a node, this class will install GigaSpaces and run the agent.
 *
 * @author noak
 * @since 2.0.1
 *
 */
public class ByonProvisioningDriver extends BaseProvisioningDriver implements ProvisioningDriver,
		ProvisioningDriverClassContextAware, ManagementLocator {

	private static final int MANAGEMENT_LOCATION_TIMEOUT = 10;
	private static final int THREAD_WAITING_IDLE_TIME_IN_SECS = 10;
	private static final int AGENT_SHUTDOWN_TIMEOUT_IN_MINUTES = 2;
	private static final String CLOUD_NODES_LIST = "nodesList";
	private static final String CLEAN_GS_FILES_ON_SHUTDOWN = "cleanGsFilesOnShutdown";
	private static final String CLOUDIFY_ITEMS_TO_CLEAN = "itemsToClean";
	private static final String CUSTOM_PROPERTY_CLEAN_REMOTE_DIR_ON_START = "clearRemoteDirectoryOnStart";

	private boolean cleanGsFilesOnShutdown = false;
	private List<String> cloudifyItems;
	private ByonDeployer deployer;
	private Integer restPort;

	private boolean cleanRemoteDirectoryOnStart = false;

	@SuppressWarnings("unchecked")
	private void addTemplatesToDeployer(final ByonDeployer deployer, final Map<String, ComputeTemplate> templatesMap)
			throws Exception {
		logger.info("addTempaltesToDeployer - adding the following tempaltes to the deployer: "
				+ templatesMap.keySet());

		List<Map<String, String>> nodesList = null;
		for (final String templateName : templatesMap.keySet()) {
			final Map<String, Object> customSettings = cloud.getCloudCompute()
					.getTemplates().get(templateName).getCustom();
			if (customSettings != null) {
				final List<Map<Object, Object>> originalNodesList =
						(List<Map<Object, Object>>) customSettings.get(CLOUD_NODES_LIST);

				nodesList = convertToStringMap(originalNodesList);

			}
			if (nodesList == null) {
				publishEvent(CloudifyErrorMessages.MISSING_NODES_LIST.getName(), templateName);
				throw new CloudProvisioningException(
						"Failed to create BYON cloud deployer, invalid configuration for tempalte "
								+ templateName + " - missing nodes list.");
			}
			deployer.addNodesList(templateName, templatesMap.get(templateName), nodesList);
		}
	}

	/*******
	 * It is easy to accidentally create GStrings instead of String in a groovy file. This will auto correct the problem
	 * for byon node definitions by calling the toString() methods for map keys and values.
	 *
	 * @param originalNodesList
	 *            .
	 * @return the
	 */
	private List<Map<String, String>> convertToStringMap(final List<Map<Object, Object>> originalNodesList) {
		List<Map<String, String>> nodesList;
		nodesList = new LinkedList<Map<String, String>>();

		for (final Map<Object, Object> originalMap : originalNodesList) {
			final Map<String, String> newMap = new LinkedHashMap<String, String>();
			final Set<Entry<Object, Object>> entries = originalMap.entrySet();
			for (final Entry<Object, Object> entry : entries) {
				newMap.put(entry.getKey().toString(), entry.getValue().toString());
			}
			nodesList.add(newMap);

		}
		return nodesList;
	}

	@Override
	protected void initDeployer(final Cloud cloud) {
		try {
			deployer = (ByonDeployer) context.getOrCreate("UNIQUE_BYON_DEPLOYER_ID", new Callable<Object>() {

				@Override
				public Object call()
						throws Exception {
					logger.info("Creating BYON context deployer for cloud: " + cloud.getName());
					final ByonDeployer newDeployer = new ByonDeployer();
					addTemplatesToDeployer(newDeployer, cloud.getCloudCompute().getTemplates());
					return newDeployer;
				}
			});
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

	/**********
	 * .
	 *
	 * @param cloud
	 *            .
	 * @throws Exception .
	 */
	public void updateDeployerTemplates(final Cloud cloud) throws Exception {
		final Map<String, ComputeTemplate> cloudTemplatesMap = cloud.getCloudCompute().getTemplates();
		final List<String> cloudTemplateNames = new LinkedList<String>(cloudTemplatesMap.keySet());
		final List<String> deployerTemplateNames = deployer.getTemplatesList();

		final List<String> redundantTemplates = new LinkedList<String>(deployerTemplateNames);
		redundantTemplates.removeAll(cloudTemplateNames);
		if (!redundantTemplates.isEmpty()) {
			logger.info("initDeployer - found redundant templates: " + redundantTemplates);
			deployer.removeTemplates(redundantTemplates);
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
			addTemplatesToDeployer(deployer, templatesMap);
		}
	}

	@SuppressWarnings("unchecked")
	private void setCustomSettings(final Cloud cloud) {
		initRestPort(cloud.getConfiguration().getComponents().getRest().getPort());
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

			if (customSettings.containsKey(CUSTOM_PROPERTY_CLEAN_REMOTE_DIR_ON_START)) {
				final Object cleanRemoteDirValue = customSettings.get(CUSTOM_PROPERTY_CLEAN_REMOTE_DIR_ON_START);
				if (cleanRemoteDirValue instanceof Boolean) {
					this.cleanRemoteDirectoryOnStart = (Boolean) cleanRemoteDirValue;
				} else if (cleanRemoteDirValue instanceof String) {
					this.cleanRemoteDirectoryOnStart = Boolean.parseBoolean((String) cleanRemoteDirValue);
				} else {
					throw new IllegalArgumentException("Unexpected value for BYON property: "
							+ CUSTOM_PROPERTY_CLEAN_REMOTE_DIR_ON_START + ". Was expecting a boolean or String, got: "
							+ cleanRemoteDirValue.getClass().getName());
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
	public MachineDetails startMachine(final String locationId, final long timeout, final TimeUnit timeUnit)
			throws TimeoutException,
			CloudProvisioningException {

		final long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);

		logger.info(this.getClass().getName() + ": startMachine, management mode: " + management);

		final Set<String> activeMachinesIPs = admin.getMachines().getHostsByAddress().keySet();
		deployer.setAllocated(cloudTemplateName, activeMachinesIPs);
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Verifying the active machines are not in the free pool: "
					+ "\n Admin reports the currently used machines are: "
					+ Arrays.toString(activeMachinesIPs.toArray())
					+ "\n Byon deployer reports the free machines for template " + cloudTemplateName + " are: "
					+ Arrays.toString(deployer.getFreeNodesByTemplateName(cloudTemplateName).toArray())
					+ "\n Byon deployer reports the currently used machines for template " + cloudTemplateName
					+ " are:"
					+ Arrays.toString(deployer.getAllocatedNodesByTemplateName(cloudTemplateName).toArray())
					+ "\n Byon deployer reports the invalid used machines for template " + cloudTemplateName + " are: "
					+ Arrays.toString(deployer.getInvalidNodesByTemplateName(cloudTemplateName).toArray()) + ")");
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
		node = deployer.createServer(cloudTemplateName, serverName);

		machineDetails = createMachineDetailsFromNode(node);

		// At this point the machine is starting. Any error beyond this point
		// must clean up the machine.
		try {
			handleServerCredentials(machineDetails, template);
		} catch (final CloudProvisioningException e) {
			try {
				deployer.invalidateServer(cloudTemplateName, node);
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
	 * @throws CloudProvisioningException
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
			final CustomNode existingNode = deployer.getServerByID(cloudTemplateName, serverName);
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
	public MachineDetails[] startManagementMachines(final long duration, final TimeUnit unit)
			throws TimeoutException,
			CloudProvisioningException {
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
		final CustomNode cloudNode = deployer.getServerByIP(cloudTemplateName, serverIp);
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

		Set<CustomNode> managementServers = null;

		try {
			managementServers = getExistingManagementServers(cloud.getProvider().getNumberOfManagementMachines());
			/*
			 * if (managementServers == null || managementServers.isEmpty()) {
			 * publishEvent("prov_management_server_not_found"); throw new CloudProvisioningException
			 * ("Could not find any management machines for this cloud"); }
			 */
		} catch (final Exception e) {
			publishEvent("prov_management_lookup_failed");
			throw new CloudProvisioningException("Failed to lookup existing management servers.", e);
		}

		for (final CustomNode customNode : managementServers) {
			try {
				stopAgentAndWait(cloud.getProvider().getNumberOfManagementMachines(), customNode.getResolvedIP());
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
		final Set<CustomNode> allNodes = deployer.getAllNodesByTemplateName(cloudTemplateName);
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

		// If a management server was found - connect it and get all management
		// machines
		if (StringUtils.isNotBlank(managementIP)) {
			// TODO don't fly if timeout reached because expectedGsmCount wasn't
			// reached
			final Integer discoveryPort = getLusPort();
			final Admin admin = Utils.getAdminObject(managementIP, expectedGsmCount, discoveryPort);
			try {
				final GridServiceManagers gsms = admin.getGridServiceManagers();
				// make sure a GSM is discovered
				gsms.waitForAtLeastOne(MANAGEMENT_LOCATION_TIMEOUT, TimeUnit.SECONDS);
				for (final GridServiceManager gsm : gsms) {
					final CustomNode managementServer = deployer.getServerByIP(cloudTemplateName, gsm.getMachine()
							.getHostAddress());
					if (managementServer != null) {
						existingManagementServers.add(managementServer);
					}
				}
			} finally {
				admin.close();
			}

		}

		return existingManagementServers;
	}

	Integer getLusPort() {
		Integer discoveryPort = cloud.getConfiguration().getComponents().getDiscovery().getDiscoveryPort();
		if (discoveryPort == null) {
			discoveryPort = CloudifyConstants.DEFAULT_LUS_PORT;
		}
		return discoveryPort;
	}

	private void stopAgentAndWait(final int expectedGsmCount, final String ipAddress)
			throws TimeoutException,
			InterruptedException {

		if (admin == null) {
			final Integer discoveryPort = getLusPort();
			admin = Utils.getAdminObject(ipAddress, expectedGsmCount, discoveryPort);
		}

		final Map<String, GridServiceAgent> agentsMap = admin.getGridServiceAgents().getHostAddress();
		// GridServiceAgent agent = agentsMap.get(ipAddress);
		GSA agent = null;
		for (final Entry<String, GridServiceAgent> agentEntry : agentsMap.entrySet()) {
			if (agentEntry.getKey().equalsIgnoreCase(ipAddress)) {
				agent = ((InternalGridServiceAgent) agentEntry.getValue()).getGSA();
			}
		}

		if (agent != null) {
			logger.info("ByonProvisioningDriver: shutting down agent on server: " + ipAddress);
			try {
				admin.close();
				agent.shutdown();
			} catch (final RemoteException e) {
				if (!NetworkExceptionHelper.isConnectOrCloseException(e)) {
					logger.log(Level.FINER, "Failed to shutdown GSA", e);
					throw new AdminException("Failed to shutdown GSA", e);
				}
			}

			final long end = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(AGENT_SHUTDOWN_TIMEOUT_IN_MINUTES);
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
			FileUtils.deleteFileSystemObjects(machineDetails.getPrivateAddress(), machineDetails.getRemoteUsername(),
					machineDetails.getRemotePassword(), null/* key file */, cloudifyItems,
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
					deployer.shutdownServerByIp(cloudTemplateName, machineDetails.getPrivateAddress());
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
		md.setPrivateAddress(node.getPrivateIP());
		md.setPublicAddress(node.getPublicIP());
		md.setCleanRemoteDirectoryOnStart(this.cleanRemoteDirectoryOnStart);
		// if the node has user/pwd - use it. Otherwise - take the use/password
		// from the template's settings.

		if (!StringUtils.isBlank(node.getUsername()) && !StringUtils.isBlank(node.getCredential())) {
			md.setRemoteUsername(node.getUsername());
			md.setRemotePassword(node.getCredential());
		} else if (!StringUtils.isBlank(template.getUsername())
				&& !StringUtils.isBlank(template.getPassword())) {
			md.setRemoteUsername(template.getUsername());
			md.setRemotePassword(template.getPassword());
		} else {
			final String nodeStr = node.toString();
			logger.severe("Cloud node loading failed, missing credentials for server: " + nodeStr);
			publishEvent("prov_node_loading_failed", nodeStr);
			throw new CloudProvisioningException("Cloud node loading failed, missing credentials for server: "
					+ nodeStr);
		}

		return md;
	}

	private void shutdownServerGracefully(final CustomNode cloudNode, final boolean isManagement)
			throws CloudProvisioningException {
		try {
			if (cleanGsFilesOnShutdown) {
				deleteGsFiles(cloudNode);
			}
			deployer.shutdownServer(cloudTemplateName, cloudNode);
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

		if (deployer != null) {
			deployer.close();
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
	public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
		try {
			return findManagementInAdmin();
		} catch (final TimeoutException e) {
			throw new CloudProvisioningException(e);
		}
	}

	@Override
	public MachineDetails[] getExistingManagementServers(final ControllerDetails[] controllers)
			throws CloudProvisioningException, UnsupportedOperationException {
		final Set<String> ips = new HashSet<String>();
		for (final ControllerDetails controllerDetails : controllers) {
			ips.add(controllerDetails.getPrivateIp());
		}

		final Set<CustomNode> allNodesByTemplateName =
				this.deployer.getAllNodesByTemplateName(this.cloud.getConfiguration().getManagementMachineTemplate());
		final Set<CustomNode> managementNodes = new HashSet<CustomNode>();

		for (final CustomNode node : allNodesByTemplateName) {
			final String ip = node.getPrivateIP();
			if (ips.contains(ip)) {
				managementNodes.add(node);
			}
		}

		final MachineDetails[] result = new MachineDetails[managementNodes.size()];
		final int i = 0;
		for (final CustomNode node : managementNodes) {
			result[i] = createMachineDetailsFromNode(node);
		}
		return result;

	}

}