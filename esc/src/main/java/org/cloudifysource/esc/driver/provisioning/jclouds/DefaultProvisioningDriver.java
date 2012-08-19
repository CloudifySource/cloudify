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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomServiceDataAware;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.jclouds.domain.LoginCredentials;

import com.google.common.base.Predicate;
import com.j_spaces.kernel.Environment;

/**************
 * A jclouds-based CloudifyProvisioning implementation. Uses the JClouds Compute Context API to provision an image with
 * linux installed and ssh available. If GigaSpaces is not already installed on the new machine, this class will install
 * gigaspaces and run the agent.
 * 
 * @author barakme, noak
 * @since 2.0.0
 */
public class DefaultProvisioningDriver extends BaseProvisioningDriver implements ProvisioningDriver,
		ProvisioningDriverClassContextAware, CustomServiceDataAware {

	private static final String DEFAULT_EC2_WINDOWS_USERNAME = "Administrator";
	private static final int CLOUD_NODE_STATE_POLLING_INTERVAL = 2000;
	private JCloudsDeployer deployer;

	@Override
	protected void initDeployer(final Cloud cloud) {
		if (this.deployer != null) {
			return;
		}

		try {
			// TODO - jcloudsUniqueId should be unique per cloud configuration.
			// TODO - The deployer object should be reusable across templates. The current API is not appropriate.
			// TODO - key should be based on entire cloud configuraion!
			// TODO - this shared context only works if we have reference counting, to check when this item is
			// no longer there. Otherwise, either this context will leak, or it will be shutdown by the first
			// service to by undeployed.
			this.deployer = createDeployer(cloud);
			// (JCloudsDeployer) context.getOrCreate("UNIQUE_JCLOUDS_DEPLOYER_ID_" + this.cloudTemplateName,
			// new Callable<Object>() {
			//
			// @Override
			// public Object call()
			// throws Exception {
			// return createDeplyer(cloud);
			// }
			// });
		} catch (final Exception e) {
			publishEvent("connection_to_cloud_api_failed", cloud.getProvider().getProvider());
			throw new IllegalStateException("Failed to create cloud Deployer", e);
		}
	}

	@Override
	public MachineDetails startMachine(String zone, final long timeout, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {

		logger.fine(this.getClass().getName() + ": startMachine, management mode: " + management);
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		if (System.currentTimeMillis() > end) {
			throw new TimeoutException("Starting a new machine timed out");
		}

		try {
			
			if (zone != null) { // override the default location from the template with the specific availability zone.
				deployer.setLocationId(zone);
			}
			
			final MachineDetails md = doStartMachine(end);
			return md;
		} catch (final Exception e) {

			// Special handling for cloudstack on ALU - for unknown reason, the context throws rejected exception.
			// Looks like the thread pool is exhausted, though not clear why. Does not repro outside their cloud.
			// if (e instanceof RejectedExecutionException
			// && this.cloud.getProvider().getProvider().equalsIgnoreCase("cloudstack")) {
			// logger.warning("Detected Jclouds execution problem. Reseting Jclouds context");
			// try {
			// this.deployer.reset(currentContext);
			// } catch (final Exception e2) {
			// logger.log(Level.WARNING, "Failed to reset jclouds context", e2);
			// }
			// }

			throw new CloudProvisioningException("Failed to start cloud machine", e);
		}
	}

	private MachineDetails doStartMachine(final long end)
			throws Exception {

		final String groupName = createNewServerName();
		logger.fine("Starting a new cloud server with group: " + groupName);
		return createServer(end, groupName);
	}

	private MachineDetails createServer(final long end, final String groupName)
			throws CloudProvisioningException {

		NodeMetadata node;
		final MachineDetails machineDetails;

		try {
			logger.fine("Cloudify Deployer is creating a new server with tag: " + groupName
					+ ". This may take a few minutes");
			node = deployer.createServer(groupName);
		} catch (final InstallerException e) {
			throw new CloudProvisioningException("Failed to create cloud server", e);
		}
		logger.fine("New node is allocated, group name: " + groupName);

		final String nodeId = node.getId();

		// At this point the machine is starting. Any error beyond this point
		// must clean up the machine

		try {
			// wait for node to reach RUNNING state
			node = waitForNodeToBecomeReady(nodeId, end);

			// Create MachineDetails for the node metadata.
			machineDetails = createMachineDetailsFromNode(node);

			final CloudTemplate cloudTemplate = this.cloud.getTemplates().get(this.cloudTemplateName);

			final FileTransferModes fileTransfer = cloudTemplate.getFileTransfer();

			if (this.cloud.getProvider().getProvider().equals("aws-ec2")
					&& fileTransfer.equals(FileTransferModes.CIFS)) {
				// Special password handling for windows on EC2
				if (machineDetails.getRemotePassword() == null) {
					// The template did not specify a password, so we must be using the aws windows password mechanism.
					handleEC2WindowsCredentials(end, node, machineDetails, cloudTemplate);
				}

			} else {
				// Credentials required special handling.
				handleServerCredentials(machineDetails, cloudTemplate);
			}

		} catch (final Exception e) {
			// catch any exception - to prevent a cloud machine leaking.
			logger.log(Level.SEVERE,
					"Cloud machine was started but an error occured during initialization. Shutting down machine", e);
			deployer.shutdownMachine(nodeId);
			throw new CloudProvisioningException(e);
		}

		return machineDetails;
	}

	private void handleEC2WindowsCredentials(final long end, final NodeMetadata node,
			final MachineDetails machineDetails, final CloudTemplate cloudTemplate)
			throws FileNotFoundException, InterruptedException, TimeoutException, CloudProvisioningException {
		File pemFile = null;

		if (this.management) {
			final String baseDirectory = Environment.getHomeDirectory();
			final File localDirectory = new File(baseDirectory, cloudTemplate.getLocalDirectory());

			pemFile = new File(localDirectory, cloudTemplate.getKeyFile());
		} else {
			final String localDirectoryName = cloudTemplate.getLocalDirectory();
			logger.fine("local dir name is: " + localDirectoryName);
			final File localDirectory = new File(localDirectoryName);

			pemFile = new File(localDirectory, cloudTemplate.getKeyFile());
		}

		if (!pemFile.exists()) {
			logger.severe("Could not find pem file: " + pemFile);
			throw new FileNotFoundException("Could not find key file: " + pemFile);
		}

		String password;
		if (cloudTemplate.getPassword() == null) {
			// get the password using Amazon API
			this.publishEvent("waiting_for_ec2_windows_password", node.getId());

			final LoginCredentials credentials =
					new EC2WindowsPasswordHandler().getPassword(node, this.deployer.getContext(), end, pemFile);
			password = credentials.getPassword();

			this.publishEvent("ec2_windows_password_retrieved", node.getId());

		} else {
			password = cloudTemplate.getPassword();
		}

		String username = cloudTemplate.getUsername();

		if (username == null) {
			username = DEFAULT_EC2_WINDOWS_USERNAME;
		}
		machineDetails.setRemoteUsername(username);
		machineDetails.setRemotePassword(password);
		machineDetails.setFileTransferMode(cloudTemplate.getFileTransfer());
		machineDetails.setRemoteExecutionMode(cloudTemplate.getRemoteExecution());
	}

	private NodeMetadata waitForNodeToBecomeReady(final String id, final long end)
			throws CloudProvisioningException, InterruptedException, TimeoutException {
		NodeMetadata node = null;
		while (System.currentTimeMillis() < end) {
			node = deployer.getServerByID(id);

			switch (node.getState()) {
			case RUNNING:
				return node;
			case PENDING:
				logger.fine("Server Status (" + id + ") still PENDING, please wait...");
				Thread.sleep(CLOUD_NODE_STATE_POLLING_INTERVAL);
				break;
			case TERMINATED:
			case ERROR:
			case UNRECOGNIZED:
			case SUSPENDED:
			default:
				throw new CloudProvisioningException("Failed to allocate server - Cloud reported node in "
						+ node.getState().toString() + " state. Node details: " + node);
			}

		}
		throw new TimeoutException("Node failed to reach RUNNING mode in time");
	}

	/*********
	 * Looks for a free server name by appending a counter to the pre-calculated server name prefix. If the max counter
	 * value is reached, code will loop back to 0, so that previously used server names will be reused.
	 * 
	 * @return the server name.
	 * @throws CloudProvisioningException if no free server name could be found.
	 */
	private String createNewServerName()
			throws CloudProvisioningException {

		// TODO - this code breaks cloudstack - not sure why yet
		String serverName = null;
		int attempts = 0;
		boolean foundFreeName = false;

		while (attempts < MAX_SERVERS_LIMIT) {
			// counter = (counter + 1) % MAX_SERVERS_LIMIT;
			++attempts;
			serverName = serverNamePrefix + counter.incrementAndGet();
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
	public MachineDetails[] startManagementMachines(final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {

		if (duration < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}
		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		logger.fine("DefaultCloudProvisioning: startMachine - management == " + management);

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
					public MachineDetails call()
							throws Exception {
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
					createdManagementMachines[i] =
							futures[i].get(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				} catch (final InterruptedException e) {
					++numberOfErrors;
					publishEvent("failed_to_create_management_vm", e.getMessage());
					logger.log(Level.FINE, "Failed to start a management machine", e);
					if (firstCreationException == null) {
						firstCreationException = e;
					}

				} catch (final ExecutionException e) {
					++numberOfErrors;
					publishEvent("failed_to_create_management_vm", e.getMessage());
					logger.log(Level.FINE, "Failed to start a management machine", e);
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
	public void stopManagementMachines()
			throws TimeoutException, CloudProvisioningException {

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
		final Set<? extends NodeMetadata> existingManagementServers =
				this.deployer.getServers(new Predicate<ComputeMetadata>() {

					@Override
					public boolean apply(final ComputeMetadata input) {
						final NodeMetadata node = (NodeMetadata) input;
						if (node.getGroup() == null) {
							return false;
						}
						// only running or pending nodes are interesting
						if (node.getState() == NodeState.RUNNING || node.getState() == NodeState.PENDING) {
							return node.getGroup().toLowerCase().startsWith(managementMachinePrefix.toLowerCase());
						}
						return false;
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

		final CloudTemplate template = this.cloud.getTemplates().get(this.cloudTemplateName);
		final String username = createMachineUsername(node, template);
		final String password = createMachinePassword(node, template);

		md.setRemoteUsername(username);
		md.setRemotePassword(password);
		
		// this will ensure that the availability zone is added to GSA that starts on this machine.
		String locationId = node.getLocation().getId();
		md.setLocationId(locationId);

		return md;
	}

	private String createMachineUsername(final NodeMetadata node, final CloudTemplate template) {

		// Template configuration takes precedence.
		if (template.getUsername() != null) {
			return template.getUsername();
		}

		// Check if node returned a username
		if (node.getCredentials() != null) {
			final String serverIdentity = node.getCredentials().identity;
			if (serverIdentity != null) {
				return serverIdentity;
			}
		}

		return null;
	}

	private String createMachinePassword(final NodeMetadata node, final CloudTemplate template) {

		// Template configuration takes precedence.
		if (template.getPassword() != null) {
			return template.getPassword();
		}

		// Check if node returned a username - some clouds support this (Rackspace, for instance)
		if (node.getCredentials() != null) {
			if (node.getCredentials().getOptionalPassword().isPresent()) {
				return node.getCredentials().getPassword();
			}
		}

		return null;
	}

	@Override
	public void close() {
		deployer.close();
	}

	private JCloudsDeployer createDeployer(final Cloud cloud)
			throws IOException {
		logger.fine("Creating JClouds context deployer with user: "
				+ cloud.getUser().getUser());
		final CloudTemplate cloudTemplate = cloud.getTemplates().get(cloudTemplateName);

		logger.fine("Cloud Template: " + cloudTemplateName + ". Details: " + cloudTemplate);
		final Properties props = new Properties();
		props.putAll(cloudTemplate.getOverrides());

		deployer =
				new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser()
						.getUser(), cloud.getUser().getApiKey(), props);

		deployer.setImageId(cloudTemplate.getImageId());
		deployer.setMinRamMegabytes(cloudTemplate.getMachineMemoryMB());
		deployer.setHardwareId(cloudTemplate.getHardwareId());
		deployer.setLocationId(cloudTemplate.getLocationId());
		deployer.setExtraOptions(cloudTemplate.getOptions());
		return deployer;
	}

	@Override
	public void setCustomDataFile(final File customDataFile) {
		logger.info("Received custom data file: " + customDataFile);
	}

	@Override
	public MachineDetails startMachine(long duration, TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		return startMachine(null, duration, unit);
	}
}
