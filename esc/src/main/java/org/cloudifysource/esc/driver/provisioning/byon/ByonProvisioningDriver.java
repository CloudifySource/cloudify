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

import java.util.Arrays;
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
import org.cloudifysource.esc.byon.ByonDeployer;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomNode;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.util.Utils;

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

	private static final String CLOUD_NODES_POOL = "nodesPool";

	private ByonDeployer deployer;

	@Override
	protected void initDeployer(final Cloud cloud) {
		try {
			deployer = (ByonDeployer) context.getOrCreate("UNIQUE_BYON_DEPLOYER_ID", new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					logger.info("Creating BYON context deployer for cloud: " + cloud.getName());
					final List<Map<String, String>> nodesList = (List<Map<String, String>>) cloud.getCustom().get(
							CLOUD_NODES_POOL);
					return new ByonDeployer(nodesList);
				}
			});
		} catch (final Exception e) {
			publishEvent("connection_to_cloud_api_failed", cloud.getProvider().getProvider());
			throw new IllegalStateException("Failed to create cloud Deployer", e);
		}
	}

	@Override
	public MachineDetails startMachine(final long timeout, final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {

		logger.info(this.getClass().getName() + ": startMachine, management mode: " + management);
		
		try {
			Set<String> ipAddresses = admin.getMachines().getHostsByAddress().keySet();
			deployer.setAllocated(ipAddresses);
			logger.info("Verifying the active machines are not in the free pool (active machines: " + Arrays.toString(ipAddresses.toArray()) + ")");
			final String name = createNewServerName();
			logger.info("Starting a new cloud machine with name: " + name);
			return createServer(System.currentTimeMillis() + unit.toMillis(timeout), name);
		} catch (final Exception e) {
			throw new CloudProvisioningException("Failed to start cloud machine", e);
		}
	}

	private MachineDetails createServer(final long end, final String name) throws CloudProvisioningException {

		final CustomNode node;
		final MachineDetails machineDetails;
		try {
			logger.info("Cloudify Deployer is creating a new server with name: " + name
					+ ". This may take a few minutes");
			node = deployer.createServer(name);
		} catch (final InstallerException e) {
			throw new CloudProvisioningException("Failed to create cloud server", e);
		}
		logger.info("New node is allocated, name: " + name);

		machineDetails = createMachineDetailsFromNode(node);

		// At this point the machine is starting. Any error beyond this point
		// must clean up the machine.
		try {
			handleServerCredentials(machineDetails);
			// check that a SSH connection can be made
			Utils.validateConnection(machineDetails.getIp(), SSH_PORT, Utils.millisUntil(end));
		} catch (final Exception e) {
			// catch any exception - to prevent a cloud machine leaking.
			logger.log(Level.SEVERE,
					"Cloud machine was started but an error occured during initialization. Shutting down machine", e);
			// TODO ? The node should be freed or invalidated ?
			deployer.shutdownServer(node);
			deployer.invalidateServer(node);
			throw new CloudProvisioningException(e);
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
	 *             if no free server name could be found.
	 */
	private String createNewServerName() throws CloudProvisioningException {

		String serverName = null;
		int attempts = 0;
		boolean foundFreeName = false;

		while (attempts < MAX_SERVERS_LIMIT) {
			counter = (counter + 1) % MAX_SERVERS_LIMIT;
			++attempts;
			serverName = serverNamePrefix + this.counter;
			// verifying this server name is not already used
			// TODO : DefaultProvisioningDriver uses deployer.getServerByID instead of getServerByName,
			// resolve that.
			// is this the NAME OR ID ?
			final CustomNode existingNode = deployer.getServerByID(serverName);
			if (existingNode == null) {
				foundFreeName = true;
				break;
			}
		}

		if (!foundFreeName) {
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

		final String managementMachinePrefix = this.serverNamePrefix;

		// first check if management already exists
		final MachineDetails[] existingManagementServers = getExistingManagementServers(managementMachinePrefix);
		if (existingManagementServers.length > 0) {
			logger.info("Found existing servers matching the name: " + managementMachinePrefix);
			return existingManagementServers;
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
						return createServer(endTime, serverNamePrefix + index);
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
			final CustomNode cloudNode = deployer.getServerByIp(serverIp);
			if (cloudNode != null) {
				logger.info("Found server: " + cloudNode.getId()
						+ ". Shutting it down and waiting for shutdown to complete");
				deployer.shutdownServerById(cloudNode.getId());
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

		//initDeployer(this.cloud);
		final MachineDetails[] managementServers = getExistingManagementServers(this.serverNamePrefix);

		if (managementServers.length == 0) {
			throw new CloudProvisioningException(
					"Could not find any management machines for this cloud (management machine prefix is: "
							+ this.serverNamePrefix + ")");
		}

		for (final MachineDetails machineDetails : managementServers) {
			deployer.shutdownServerById(machineDetails.getMachineId());
		}
	}

	private MachineDetails[] getExistingManagementServers(final String managementMachinePrefix) {
		final Set<CustomNode> existingManagementServers = deployer.getServersByPrefix(managementMachinePrefix);

		final MachineDetails[] result = new MachineDetails[existingManagementServers.size()];
		int i = 0;
		for (final CustomNode node : existingManagementServers) {
			result[i] = createMachineDetailsFromNode(node);
			result[i].setAgentRunning(true);
			result[i].setCloudifyInstalled(true);
			i++;

		}
		return result;
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
					deployer.shutdownServerById(machineDetails.getMachineId());
				}
			}
		}

		throw new CloudProvisioningException(
				"One or more managememnt machines failed. The first encountered error was: "
						+ firstCreationException.getMessage(), firstCreationException);
	}

	private MachineDetails createMachineDetailsFromNode(final CustomNode node) {
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

		// if the node has user/pwd - use it. Otherwise - take them from the cloud settings.
		if (!StringUtils.isBlank(node.getUsername()) && !StringUtils.isBlank(node.getCredential())) {
			md.setRemoteUsername(node.getUsername());
			md.setRemotePassword(node.getCredential());
		} else {
			md.setRemoteUsername(cloud.getConfiguration().getRemoteUsername());
			md.setRemotePassword(cloud.getConfiguration().getRemotePassword());
		}

		return md;
	}

	@Override
	public void close() {
		deployer.close();
	}

}
