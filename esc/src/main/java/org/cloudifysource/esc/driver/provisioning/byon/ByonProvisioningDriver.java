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
package org.cloudifysource.esc.driver.provisioning.byon;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.esc.byon.ByonDeployer;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomNode;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.util.Utils;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.internal.gsa.InternalGridServiceAgent;
import org.openspaces.admin.internal.support.NetworkExceptionHelper;

import com.gigaspaces.grid.gsa.GSA;

/**************
 * A bring-your-own-node (BYON) CloudifyProvisioning implementation. Parses a groovy file as a source of
 * available machines to operate as cloud nodes, assuming the nodes are Linux machines with SSH installed. If
 * GigaSpaces is not already installed on a node, this class will install GigaSpaces and run the agent.
 * 
 * @author noak
 * @since 2.0.1
 * 
 */
public class ByonProvisioningDriver extends BaseProvisioningDriver implements ProvisioningDriver,
		ProvisioningDriverClassContextAware {

	private ByonDeployer deployer;
	private static final String CLOUD_NODES_LIST = "nodesList";
	private static final int THREAD_WAITING_IDLE_TIME = 10;

	@Override
	protected void initDeployer(final Cloud cloud) {
		try {
			deployer = (ByonDeployer) context.getOrCreate("UNIQUE_BYON_DEPLOYER_ID", new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					logger.info("Creating BYON context deployer for cloud: " + cloud.getName());
					final ByonDeployer deployer = new ByonDeployer();
					List<Map<String, String>> nodesList = null;
					final Map<String, CloudTemplate> templatesMap = cloud.getTemplates();
					for (final String templateName : templatesMap.keySet()) {
						final Map<String, Object> customSettings = cloud.getTemplates().get(templateName).getCustom();
						if (customSettings != null) {
							nodesList = (List<Map<String, String>>) customSettings.get(CLOUD_NODES_LIST);
						}
						if (nodesList == null) {
							publishEvent("prov_invalid_configuration");
							throw new CloudProvisioningException(
									"Failed to create BYON cloud deployer, invalid configuration");
						}
						deployer.addNodesList(templateName, nodesList);
					}

					return deployer;
				}
			});
		} catch (final Exception e) {
			publishEvent("connection_to_cloud_api_failed", cloud.getProvider().getProvider());
			throw new IllegalStateException("Failed to create cloud deployer", e);
		}
	}

	@Override
	public MachineDetails startMachine(final long timeout, final TimeUnit timeUnit) throws TimeoutException,
			CloudProvisioningException {

		final long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);

		logger.info(this.getClass().getName() + ": startMachine, management mode: " + management);

		final Set<String> activeMachinesIPs = admin.getMachines().getHostsByAddress().keySet();
		deployer.setAllocated(cloudTemplateName, activeMachinesIPs);
		logger.info("Verifying the active machines are not in the free pool (active machines: "
				+ Arrays.toString(activeMachinesIPs.toArray()) + ", all machines in pool: "
				+ Arrays.toString(deployer.getAllNodesByTemplateName(cloudTemplateName).toArray()) + ")");
		final String newServerName = createNewServerName();
		logger.info("Starting a new cloud machine with name: " + newServerName);
		return createServer(newServerName, endTime);
	}

	private MachineDetails createServer(final String serverName, final long endTime)
			throws CloudProvisioningException, TimeoutException {

		final CustomNode node;
		final MachineDetails machineDetails;
		logger.info("Cloudify Deployer is creating a new server with name: " + serverName
				+ ". This may take a few minutes");
		node = deployer.createServer(cloudTemplateName, serverName);
		logger.info("New node is allocated, name: " + serverName);

		machineDetails = createMachineDetailsFromNode(node);

		// At this point the machine is starting. Any error beyond this point
		// must clean up the machine.
		try {
			handleServerCredentials(machineDetails);
		} catch (final CloudProvisioningException e) {
			deployer.invalidateServer(cloudTemplateName, node, true);
			throw e;
		}
		
		try {
			// check that a SSH connection can be made
			Utils.validateConnection(machineDetails.getIp(), CloudifyConstants.SSH_PORT);
		} catch (final Exception e) {
			// catch any exception - to prevent a cloud machine leaking.
			logger.log(Level.SEVERE, "Cloud server could not be started on " + machineDetails.getIp()
					+ ", SSH connection failed.", e);
			try {
				deployer.invalidateServer(cloudTemplateName, node, false);
			} catch (final CloudProvisioningException ie) {
				logger.log(Level.SEVERE, "Failed to mark machine " + machineDetails.getIp() + " as Invalid.", ie);
			}
			throw new CloudProvisioningException(e);
		}

		if (System.currentTimeMillis() > endTime) {
			throw new TimeoutException();
		}

		return machineDetails;
	}

	/*********
	 * Looks for a free server name by appending a counter to the pre-calculated server name prefix. If the
	 * max counter value is reached, code will loop back to 0, so that previously used server names will be
	 * reused.
	 * 
	 * @return the server name.
	 * @throws CloudProvisioningException
	 *             Indicated a free server name was not found.
	 */
	private String createNewServerName() throws CloudProvisioningException {

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
	public MachineDetails[] startManagementMachines(final long duration, final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {
		if (duration < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}
		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		logger.info("DefaultCloudProvisioning: startMachine - management == " + management);

		// first check if management already exists
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
		} catch (final InterruptedException e) {
			publishEvent("prov_management_lookup_failed");
			throw new CloudProvisioningException("Failed to lookup existing manahement servers.", e);
		}

		// launch the management machines
		publishEvent(EVENT_ATTEMPT_START_MGMT_VMS);
		final int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		final MachineDetails[] createdMachines = doStartManagementMachines(endTime, numberOfManagementMachines);
		publishEvent(EVENT_MGMT_VMS_STARTED);
		return createdMachines;
	}

	private MachineDetails[] doStartManagementMachines(final long endTime, final int numberOfManagementMachines)
			throws TimeoutException, CloudProvisioningException {
		final ExecutorService executors = Executors.newFixedThreadPool(numberOfManagementMachines);

		@SuppressWarnings("unchecked")
		final Future<MachineDetails>[] futures = (Future<MachineDetails>[]) new Future<?>[numberOfManagementMachines];

		try {
			// Call startMachine asynchronously once for each management
			// machine
			for (int i = 0; i < numberOfManagementMachines; i++) {
				final int index = i + 1;
				futures[i] = executors.submit(new Callable<MachineDetails>() {

					@Override
					public MachineDetails call() throws Exception {
						return createServer(serverNamePrefix + index, endTime);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean stopMachine(final String serverIp, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException, InterruptedException {

		boolean stopResult = false;

		logger.info("Stop Machine - machineIp: " + serverIp);
		final Long previousRequest = stoppingMachines.get(serverIp);
		if (previousRequest != null
				&& System.currentTimeMillis() - previousRequest < MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT) {
			logger.fine("Machine " + serverIp + " is already stopping. Ignoring this shutdown request");
			stopResult = false;
		} else {
			// TODO - add a task that cleans up this map
			stoppingMachines.put(serverIp, System.currentTimeMillis());
			logger.info("Scale IN -- " + serverIp + " --");
			logger.info("Looking up cloud server with IP: " + serverIp);
			final CustomNode cloudNode = deployer.getServerByIP(cloudTemplateName, serverIp);
			if (cloudNode != null) {
				logger.info("Found server: " + cloudNode.getId()
						+ ". Shutting it down and waiting for shutdown to complete");
				MachineDetails machineDetails = null;
				try {
					machineDetails = createMachineDetailsFromNode(cloudNode);
					Utils.deleteFileSystemObject(machineDetails.getPrivateAddress(), machineDetails.getRemoteUsername(),
							machineDetails.getRemotePassword(), "/tmp/gs-files", null, 1, TimeUnit.MINUTES);
				} catch (Exception e) {
					if (machineDetails != null) {
						logger.info("ByonProvisioningDriver: Failed to delete system files on teardown from server: " 
							+ machineDetails.getPrivateAddress());
					} else {
						logger.info("ByonProvisioningDriver: Failed to delete system files on teardown from server.");
					}
				}
				deployer.shutdownServerByIp(cloudTemplateName, cloudNode.getPrivateIP());
				logger.info("Server: " + cloudNode.getId() + " shutdown has finished.");
				stopResult = true;
			} else {
				logger.log(Level.SEVERE, "Recieved scale in request for machine with ip " + serverIp
						+ " but this IP could not be found in the Cloud server list");
				stopResult = false;
			}
		}

		return stopResult;
	}

	@Override
	public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {

		try {
			final Set<CustomNode> managementServers = getExistingManagementServers(cloud.getProvider()
					.getNumberOfManagementMachines());
			if (managementServers == null || managementServers.isEmpty()) {
				throw new CloudProvisioningException("Could not find any management machines for this cloud");
			}

			for (final CustomNode customNode : managementServers) {
				stopManagementServicesAndWait(cloud.getProvider().getNumberOfManagementMachines(),
						customNode.getPrivateIP(), 2, TimeUnit.MINUTES);
				MachineDetails machineDetails = null;
				try {
					machineDetails = createMachineDetailsFromNode(customNode);
					Utils.deleteFileSystemObject(machineDetails.getPrivateAddress(), machineDetails.getRemoteUsername(),
							machineDetails.getRemotePassword(), "/tmp/gs-files", null, 1, TimeUnit.MINUTES);
				} catch (Exception e) {
					if (machineDetails != null) {
						logger.info("ByonProvisioningDriver: Failed to delete system files on teardown from server: " 
							+ machineDetails.getPrivateAddress());
					} else {
						logger.info("ByonProvisioningDriver: Failed to delete system files on teardown from server.");
					}
				}
				deployer.shutdownServer(cloudTemplateName, customNode);
			}
		} catch (final Exception e) {
			publishEvent("prov_agent_shutdown_failed");
			throw new CloudProvisioningException("Failed to shutdown agent.", e);
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
				Utils.validateConnection(server.getPrivateIP(), CloudifyConstants.DEFAULT_REST_PORT);
				managementIP = server.getPrivateIP();
				break;
			} catch (final Exception ex) {
				// the connection to the REST failed because this is not a management server, continue.
			}
		}

		// If a management server was found - connect it and get all management machines
		if (StringUtils.isNotBlank(managementIP)) {
			if (admin == null) {
				admin = Utils.getAdminObject(managementIP, expectedGsmCount);
			}
			final GridServiceManagers gsms = admin.getGridServiceManagers();
			for (final GridServiceManager gsm : gsms) {
				existingManagementServers.add(deployer.getServerByIP(cloudTemplateName, gsm.getMachine()
						.getHostAddress()));
			}
			admin.close();
		}

		return existingManagementServers;
	}

	private void stopManagementServicesAndWait(final int expectedGsmCount, final String ipAddress, final long timeout,
			final TimeUnit timeunit) throws TimeoutException, InterruptedException {

		if (admin == null) {
			admin = Utils.getAdminObject(ipAddress, expectedGsmCount);
		}

		final Map<String, GridServiceAgent> agentsMap = admin.getGridServiceAgents().getHostAddress();
		// GridServiceAgent agent = agentsMap.get(ipAddress);
		GSA agent = null;
		final Set<String> keys = agentsMap.keySet();
		for (final String key : keys) {
			System.out.println("key: " + key + ", value: " + agentsMap.get(key));
			if (key.equalsIgnoreCase(ipAddress)) {
				agent = ((InternalGridServiceAgent) agentsMap.get(key)).getGSA();
			}
		}
		if (agent != null) {
			logger.info("ByonProvisioningDriver: shutting down agent on management server: " + ipAddress);
			try {
				admin.close();
				agent.shutdown();
			} catch (final RemoteException e) {
				if (!NetworkExceptionHelper.isConnectOrCloseException(e)) {
					logger.log(Level.FINER, "Failed to shutdown GSA", e);
					throw new AdminException("Failed to shutdown GSA", e);
				}
			}

			final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);
			boolean agentUp = isAgentUp(agent);
			while (agentUp && System.currentTimeMillis() < end) {
				logger.fine("next check in " + TimeUnit.MILLISECONDS.toSeconds(THREAD_WAITING_IDLE_TIME) + " seconds");
				Thread.sleep(TimeUnit.SECONDS.toMillis(THREAD_WAITING_IDLE_TIME));
				agentUp = isAgentUp(agent);
			}

			if (!agentUp && System.currentTimeMillis() >= end) {
				throw new TimeoutException("Agent shutdown timed out (agent IP: " + ipAddress + ")");
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

	private void handleProvisioningFailure(final int numberOfManagementMachines, final int numberOfErrors,
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

	private MachineDetails createMachineDetailsFromNode(final CustomNode node) throws CloudProvisioningException {
		final MachineDetails md = new MachineDetails();
		md.setAgentRunning(false);
		md.setCloudifyInstalled(false);
		md.setInstallationDirectory(null);
		md.setMachineId(node.getId());
		md.setPrivateAddress(node.getPrivateIP());
		md.setPublicAddress(node.getPublicIP());
		// By default, cloud nodes connect to each other using their private
		// address.
		md.setUsePrivateAddress(true);

		// if the node has user/pwd - use it. Otherwise - take them from the cloud's settings.
		if (!StringUtils.isBlank(node.getUsername()) && !StringUtils.isBlank(node.getCredential())) {
			md.setRemoteUsername(node.getUsername());
			md.setRemotePassword(node.getCredential());
		} else if (!StringUtils.isBlank(cloud.getConfiguration().getRemoteUsername())
				&& !StringUtils.isBlank(cloud.getConfiguration().getRemotePassword())) {
			md.setRemoteUsername(cloud.getConfiguration().getRemoteUsername());
			md.setRemotePassword(cloud.getConfiguration().getRemotePassword());
		} else {
			logger.severe("Cloud node loading failed, missing credentials for server: " + node.toString());
			publishEvent("prov_node_loading_failed", node.toString());
			throw new CloudProvisioningException("Cloud node loading failed, missing credentials for server: "
					+ node.toString());
		}

		return md;
	}

	@Override
	public void close() {
		try {
			if (admin != null) {
				admin.close();
			}
		} catch (Exception ex) {
			logger.info("ByonProvisioningDriver.close() failed to close agent");
		}
		
		if (deployer != null) {
			deployer.close();
		}
	}

}