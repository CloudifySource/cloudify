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
package org.cloudifysource.esc.driver.provisioning.jclouds;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.cloudifysource.esc.util.Utils;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;

import com.google.common.base.Predicate;

/**************
 * A jclouds-based CloudifyProvisioning implementation. Uses the JClouds Compute Context API to provision an
 * image with linux installed and ssh available. If GigaSpaces is not already installed on the new machine,
 * this class will install gigaspaces and run the agent.
 * 
 * @author barakme, noak
 * @since 2.0.0
 */
public class DefaultProvisioningDriver extends BaseProvisioningDriver implements ProvisioningDriver,
		ProvisioningDriverClassContextAware {

	private JCloudsDeployer deployer;

	@Override
	protected void initDeployer(final Cloud cloud) {
		try {
			// TODO - jcloudsUniqueId should be unique per cloud configuration.
			this.deployer = (JCloudsDeployer) context.getOrCreate("UNIQUE_JCLOUDS_DEPLOYER_ID",
					new Callable<Object>() {

						@Override
						public Object call() throws Exception {
							logger.fine("Creating JClouds context deployer with user: " + cloud.getUser().getUser());
							final CloudTemplate cloudTemplate = cloud.getTemplates().get(cloudTemplateName);

							final Properties props = new Properties();
							props.putAll(cloudTemplate.getOverrides());

							deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser()
									.getUser(), cloud.getUser().getApiKey(), props);

							deployer.setImageId(cloudTemplate.getImageId());
							deployer.setMinRamMegabytes(cloudTemplate.getMachineMemoryMB());
							deployer.setHardwareId(cloudTemplate.getHardwareId());
							deployer.setLocationId(cloudTemplate.getLocationId());
							deployer.setExtraOptions(cloudTemplate.getOptions());
							return deployer;
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
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);
		// TO DO : is this really necessary? maybe we can remove this method?
		initDeployer(cloud);

		// initDeployed can take a while on some clouds, so checking the timeout
		if (System.currentTimeMillis() > end) {
			throw new TimeoutException("Starting a new machine timed out");
		}

		try {
			final MachineDetails md = doStartMachine(end);
			return md;
		} catch (final Exception e) {
			throw new CloudProvisioningException("Failed to start cloud machine", e);
		}
	}

	private MachineDetails doStartMachine(final long end) throws Exception {

		final String groupName = createNewServerName();
		logger.info("Starting a new cloud server with group: " + groupName);
		return createServer(end, groupName);
	}

	private MachineDetails createServer(final long end, final String groupName) throws CloudProvisioningException
			{

		final NodeMetadata node;
		final MachineDetails machineDetails;
		try {
			logger.info("Cloudify Deployer is creating a new server with tag: " + groupName
					+ ". This may take a few minutes");
			node = deployer.createServer(groupName);
		} catch (final InstallerException e) {
			throw new CloudProvisioningException("Failed to create cloud server", e);
		}
		logger.info("New node is allocated, group name: " + groupName);

		machineDetails = createMachineDetailsFromNode(node);

		// At this point the machine is starting. Any error beyond this point
		// must clean up the machine
		try {
			handleServerCredentials(machineDetails);
			waitUntilServerIsActive(machineDetails.getMachineId(), Utils.millisUntil(end), TimeUnit.MILLISECONDS);
		} catch (final Exception e) {
			// catch any exception - to prevent a cloud machine leaking.
			logger.log(Level.SEVERE,
					"Cloud machine was started but an error occured during initialization. Shutting down machine", e);
			deployer.shutdownMachine(node.getId());
			throw new CloudProvisioningException(e);
		}

		return machineDetails;
	}

	/*********
	 * Periodically gets the server status from the cloud, until the server's status changes to ACTIVE, or a
	 * timeout expires.
	 * 
	 * @param serverId
	 *            The server ID.
	 * @param milliseconds
	 * @param l
	 * @return The server status - should always be ACTIVE.
	 */
	private void waitUntilServerIsActive(final String serverId, final long timeout, final TimeUnit unit)
			throws TimeoutException, InterruptedException {
		final long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
		NodeMetadata server;
		while (true) {
			server = deployer.getServerByID(serverId);
			if (server != null && server.getState() == NodeState.RUNNING) {
				break;
			}

			if (System.currentTimeMillis() > endTime) {
				throw new TimeoutException("Server [ " + serverId + " ] has been starting up for more more than "
						+ TimeUnit.MINUTES.convert(WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS) + " minutes!");
			}

			if (logger.isLoggable(Level.FINE)) {
				final String serverName = server != null ? server.getState().name() : serverId;
				logger.fine("Server Status (" + serverName + ") still not active, please wait...");
			}
			Thread.sleep(WAIT_THREAD_SLEEP_MILLIS);
		}
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
			final NodeMetadata existingNode = deployer.getServerByID(serverName);
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

			// In case of a partial error, shutdown all servers that did start up
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
			final NodeMetadata server = deployer.getServerWithIP(serverIp);
			if (server != null) {
				logger.info("Found server: " + server.getId()
						+ ". Shutting it down and waiting for shutdown to complete");
				deployer.shutdownMachineAndWait(server.getId(), unit, duration);
				logger.info("Server: " + server.getId() + " shutdown has finished.");
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

		initDeployer(this.cloud);
		final MachineDetails[] managementServers = getExistingManagementServers(this.serverNamePrefix);

		if (managementServers.length == 0) {
			throw new CloudProvisioningException(
					"Could not find any management machines for this cloud (management machine prefix is: "
							+ this.serverNamePrefix + ")");
		}

		final Set<String> machineIps = new HashSet<String>();
		for (final MachineDetails machineDetails : managementServers) {
			machineIps.add(machineDetails.getPrivateAddress());
		}

		this.deployer.shutdownMachinesWithIPs(machineIps);
	}

	private MachineDetails[] getExistingManagementServers(final String managementMachinePrefix) {
		final Set<? extends NodeMetadata> existingManagementServers = this.deployer
				.getServers(new Predicate<ComputeMetadata>() {

					@Override
					public boolean apply(final ComputeMetadata input) {
						final NodeMetadata node = (NodeMetadata) input;
						if (node.getGroup() == null) {
							return false;
						}
						// only running or pending nodes are interesting
						if (!(node.getState() == NodeState.RUNNING || node.getState() == NodeState.PENDING)) {
							return false;
						}

						return node.getGroup().startsWith(managementMachinePrefix);

					}
				});

		final MachineDetails[] result = new MachineDetails[existingManagementServers.size()];
		int i = 0;
		for (final NodeMetadata node : existingManagementServers) {
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
					this.deployer.shutdownMachine(machineDetails.getMachineId());
				}
			}
		}

		throw new CloudProvisioningException(
				"One or more managememnt machines failed. The first encountered error was: "
						+ firstCreationException.getMessage(), firstCreationException);
	}

	private MachineDetails createMachineDetailsFromNode(final NodeMetadata node) {
		final MachineDetails md = new MachineDetails();
		md.setAgentRunning(false);
		md.setCloudifyInstalled(false);
		md.setInstallationDirectory(null);
		md.setMachineId(node.getId());
		if (node.getPrivateAddresses().size() > 0) {
			md.setPrivateAddress(node.getPrivateAddresses().iterator().next());
		}
		if (node.getPublicAddresses().size() > 0) {
			md.setPublicAddress(node.getPublicAddresses().iterator().next());
		}

		if (node.getCredentials() == null) {
			md.setRemoteUsername(cloud.getConfiguration().getRemoteUsername());
		} else {
			final String serverIdentity = node.getCredentials().identity;
			if (serverIdentity != null) {
				md.setRemoteUsername(serverIdentity);
			} else {
				md.setRemoteUsername(cloud.getConfiguration().getRemoteUsername());
			}
		}
	
		md.setUsePrivateAddress(this.cloud.getConfiguration().isConnectToPrivateIp());
		return md;
	}

	@Override
	public void close() {
		deployer.close();
	}

}
