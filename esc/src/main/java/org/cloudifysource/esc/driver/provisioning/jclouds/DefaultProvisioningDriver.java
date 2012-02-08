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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriverListener;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverContextAware;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.cloudifysource.esc.util.Utils;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;
import org.openspaces.admin.Admin;

import com.gigaspaces.internal.utils.StringUtils;
import com.google.common.base.Predicate;

/**************
 * A jclouds-based CloudifyProvisioning implementation. Uses the JClouds Compute Context API to provision an image with
 * linux installed and ssh available. If GigaSpaces is not already installed on the new machine, this class will install
 * gigaspaces and run the agent.
 * 
 * @author barakme
 * 
 */
public class DefaultProvisioningDriver implements ProvisioningDriver , ProvisioningDriverContextAware{

	private static final int WAIT_THREAD_SLEEP_MILLIS = 10000;
	private static final int WAIT_TIMEOUT_MILLIS = 360000;

	private JCloudsDeployer deployer;

	private String machineNamePrefix;

	private String cloudName;

	private Cloud cloud;

	private String cloudTemplateName;
	private boolean management;

	private int counter = 0;
	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(DefaultProvisioningDriver.class.getName());
	private static final int MAX_MACHINE_LIMIT = 200;

	private final List<ProvisioningDriverListener> eventsListenersList = new LinkedList<ProvisioningDriverListener>();

	//TODO: Store JCloudsDeployer in the context
	private ProvisioningDriverContext context;
	
	@Override
	public void setProvisioningContext(ProvisioningDriverContext context) {
		this.context = context;
	}
	
	@Override
	public void setConfig(final Cloud cloud, final String cloudTemplateName, final boolean management) {

		this.cloud = cloud;
		this.cloudTemplateName = cloudTemplateName;
		this.management = management;

		logger.fine("Initializing Cloud Provisioning - management mode: " + management + ". Using template: "
				+ cloudTemplateName + " with cloud: " + cloud);

		this.cloudName = cloud.getName();

		String prefix = management ? cloud.getProvider().getManagementGroup() : cloud.getProvider()
				.getMachineNamePrefix();

		if (prefix == null || prefix.length() == 0) {
			if (management) {
				prefix = "cloudify_managememnt_";
			} else {
				prefix = "cloudify_agent_";
			}

			logger.warning("Prefix for machine name was not set. Using: " + prefix);
		}

		this.machineNamePrefix = prefix;

	}

	private void initDeployer(final Cloud cloud) {		
        try {
        	// TODO: jcloudsUniqueId  should have a real value. currently cloud.
            String jcloudsUniqueId = "UNIQUE_JCLOUDS_DEPLOYER_ID";
			this.deployer = (JCloudsDeployer)context.getOrCreate(jcloudsUniqueId, new Callable<Object>() {
	            
				@Override
				public Object call() throws Exception {
	                logger.fine("Creating jclouds context deployer with user: " + cloud.getUser().getUser());
	                CloudTemplate cloudTemplate = cloud.getTemplates().get(cloudTemplateName);
	                
					Properties props = new Properties();
					props.putAll(cloudTemplate.getOverrides());
		
					deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(), cloud
							.getUser().getApiKey(), props);
		
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

		logger.fine("DefaultCloudProvisioning: startMachine - management == " + management);
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		initDeployer(cloud);

		// initializing the jclouds context can take a while on some clouds, so
		// check for timeout
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

	private static void logServerDetails(final NodeMetadata server, final File tempFile) {
		if (logger.isLoggable(Level.INFO)) {
			logger.fine(nodePrefix(server) + "Cloud Server was created.");
			if (tempFile == null) {
				logger.fine(nodePrefix(server) + "Password: ***");
			} else {
				logger.fine(nodePrefix(server) + "Key File: " + tempFile.getAbsolutePath());
			}

			logger.fine(nodePrefix(server) + "Public IP: " + Arrays.toString(server.getPublicAddresses().toArray()));
			logger.fine(nodePrefix(server) + "Private IP: " + Arrays.toString(server.getPrivateAddresses().toArray()));

		}
	}

	private static String nodePrefix(final NodeMetadata node) {
		return "[" + node.getId() + "] ";
	}

	/*********
	 * Looks for a free machine name by appending a counter to the precalculated machine name prefix. If the max counter
	 * value is reached, code will loop back to 0, so that previously used machine names will be reused.
	 * 
	 * @return the machine name.
	 * @throws CloudProvisioningException
	 *             if no free machine name could be found.
	 */
	private String createNewMachineName() throws CloudProvisioningException {
		int attempts = 0;
		while (attempts < MAX_MACHINE_LIMIT) {
			counter = (counter + 1) % MAX_MACHINE_LIMIT;
			++attempts;
			final String machineName = this.machineNamePrefix + this.counter;
			final NodeMetadata existingNode = deployer.getServerByID(machineName);
			if (existingNode == null) {
				return machineName;
			}
		}
		throw new CloudProvisioningException("Number of machines has exceeded allowed machine limit ("
				+ MAX_MACHINE_LIMIT + ")");

	}

	private MachineDetails doStartMachine(final long end) throws Exception {

		final String machineName = createNewMachineName();
		logger.fine("Starting a new cloud machines with group: " + machineName);

		return createServer(end, machineName);

	}

	private MachineDetails createServer(final long end, final String machineName) throws CloudProvisioningException,
			Exception {
		NodeMetadata node;
		try {
			node = deployer.createServer(machineName);
		} catch (final InstallerException e) {
			throw new CloudProvisioningException("Failed to create cloud machine", e);
		}

		logger.fine("New machine is starting");

		// At this point the machine is starting. Any error beyond this point
		// must clean up the machine
		try {
			final MachineDetails md = createMachineDetailsFromNode(node);

			handleServerCredentials(node, md);

			waitUntilServerIsActive(node.getId(), Utils.millisUntil(end), TimeUnit.MILLISECONDS);
			return md;
		} catch (final Exception e) {
			// catch any exception - to prevent a cloud machine leaking.
			logger.log(Level.SEVERE,
					"Cloud machine was started but an error occured during initialization. Shutting down machine", e);
			this.deployer.shutdownMachine(node.getId());
			throw e;
		}
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

		// By default, cloud nodes connect to each other using their private
		// address.
		md.setUsePrivateAddress(true);
		return md;
	}

	/*********
	 * Periodically gets the server status from Rackspace, until the server's status changes to ACTIVE, or a timeout
	 * expires.
	 * 
	 * @param serverId
	 *            The server ID.
	 * @param milliseconds
	 * @param l
	 * @return The server status - should always be ACTIVE.
	 */
	protected void waitUntilServerIsActive(final String serverId, final long timeout, final TimeUnit unit)
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

	private void handleServerCredentials(final NodeMetadata server, final MachineDetails md)
			throws CloudProvisioningException {
		final String credential = server.getCredentials().credential;
		File tempFile = null;

		if (credential == null) { // must be using an existing key file or
									// password
			logger.fine("Cloud did not provide server credentials, checking in cloud configuration file");

			if (cloud.getUser().getKeyFile() == null || cloud.getUser().getKeyFile().length() == 0) {
				logger.fine("No key file specified in cloud configuration");
				// no key file. Check for password
				if (cloud.getConfiguration().getRemotePassword() == null) {
					logger.severe("No Password specified in cloud configuration - connection to the new machine is not possible.");
					throw new CloudProvisioningException(
							"Cloud did not provider server credentials, and no credentials (password or key file) supplied with cloud configuration file");
				}
				md.setRemotePassword(cloud.getConfiguration().getRemotePassword());
			} else {
				tempFile = new File(cloud.getUser().getKeyFile());
			}
		} else if (credential.startsWith("-----BEGIN RSA PRIVATE KEY-----")) {
			// cloud has provided a key file

			try {
				tempFile = File.createTempFile("gs-esm-key", ".pem");
				FileUtils.write(tempFile, credential);
				cloud.getUser().setKeyFile(tempFile.getAbsolutePath());
			} catch (final IOException e) {
				throw new CloudProvisioningException("Failed to create a temporary file for cloud server's key file", e);
			}

		} else {
			logger.fine("Cloud has provided password for remote connection to new machine");
			cloud.getConfiguration().setRemotePassword(server.getCredentials().credential);
		}

		final File keyFile = tempFile;
		logServerDetails(server, keyFile);

	}

	protected InstallationDetails createInstallationDetails() {
		final InstallationDetails details = new InstallationDetails();

		details.setLocalDir(cloud.getProvider().getLocalDirectory());
		details.setRemoteDir(cloud.getProvider().getRemoteDirectory());
		details.setManagementOnlyFiles(cloud.getProvider().getManagementOnlyFiles());
		details.setZones(StringUtils.join(cloud.getProvider().getZones().toArray(new String[0]), ",", 0, cloud
				.getProvider().getZones().size()));

		details.setKeyFile(cloud.getUser().getKeyFile());

		details.setPrivateIp(null);

		// logger.info("Setting LOCATOR for new installation details to: " +
		// config.getLocator());
		// details.setLocator(config.getLocator()); // TODO get actual locators
		// (which could be more than 1 management machine)
		details.setLus(false);
		details.setCloudifyUrl(cloud.getProvider().getCloudifyUrl());
		details.setConnectedToPrivateIp(true);
		details.setAdmin(this.admin);

		logger.fine("Created new Installation Details: " + details);
		return details;

		// if ((config.getKeyPair() != null) && (config.getKeyPair().length() >
		// 0)) {
		// File keyFile = new File(config.getKeyFile());
		// if (!keyFile.isAbsolute()) {
		// keyFile = new File(details.getLocalDir(), config.getKeyFile());
		// }
		// if (!keyFile.isFile()) {
		// throw new FileNotFoundException("keyfile : "
		// + keyFile.getAbsolutePath() + " not found");
		// }
		// details.setKeyFile(keyFile.getAbsolutePath());
		// }
	}

	@Override
	public String getCloudName() {
		return this.cloudName;
	}

	private NodeMetadata getServerWithIP(final String ip) {
		return deployer.getServerWithIP(ip);
	}

	private final Map<String, Long> stoppingMachines = new ConcurrentHashMap<String, Long>();
	private Admin admin;
	private static final int MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT = 120000;

	@Override
	public boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException, InterruptedException {
		logger.fine("Stop Machine - machineIp: " + machineIp);

		final Long previousRequest = stoppingMachines.get(machineIp);
		if (previousRequest != null
				&& System.currentTimeMillis() - previousRequest < MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT) {
			logger.fine("Machine " + machineIp + " is already stopping. Ignoring this shutdown request");
			return false;
		}

		// TODO - add a task that cleans up this map
		stoppingMachines.put(machineIp, System.currentTimeMillis());
		logger.fine("Scale IN -- " + machineIp + " --");

		logger.fine("Looking Up Cloud server with IP: " + machineIp);
		final NodeMetadata server = getServerWithIP(machineIp);
		if (server != null) {
			logger.fine("Found server: " + server.getId() + ". Shutting it down and waiting for shutdown to completes");
			deployer.shutdownMachineAndWait(server.getId(), unit, duration);
			logger.fine("Server: " + server.getId() + " shutdown has finished.");
			return true;

		} else {
			logger.log(Level.SEVERE, "Recieved scale in request for machine with ip " + machineIp
					+ " but this IP could not be found in the Cloud server list");
			return false;
		}

	}

	@Override
	public void setAdmin(final Admin admin) {
		this.admin = admin;

	}

	@Override
	public MachineDetails[] startManagementMachines(final long duration, final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {
		if (duration < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}
		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		logger.fine("DefaultCloudProvisioning: startMachine - management == " + management);

		publishEvent("try_to_connect_to_cloud_api", cloud.getProvider().getProvider());
		initDeployer(cloud);
		publishEvent("connection_to_cloud_api_succeeded", cloud.getProvider().getProvider());

		// force the creation of the jclouds template - otherwise, it may be
		// created multiple times, once for each management machine
		this.deployer.getTemplate();

		final String managementMachinePrefix = this.machineNamePrefix;

		// first check if management already exists
		final MachineDetails[] existingManagementServers = getExistingManagementServers(managementMachinePrefix);
		if (existingManagementServers.length > 0) {
			logger.fine("Found existing servers matching the name: " + managementMachinePrefix);
			return existingManagementServers;
		}

		// launch the management machines
		publishEvent("attempting_to_create_management_vms");
		final int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		final MachineDetails[] createdMachines = doStartManagementMachines(endTime, numberOfManagementMachines);
		publishEvent("management_started_successfully");
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
						return createServer(endTime, machineNamePrefix + index);
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

	@Override
	public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {

		initDeployer(this.cloud);
		final MachineDetails[] managementServers = getExistingManagementServers(this.machineNamePrefix);

		if (managementServers.length == 0) {
			throw new CloudProvisioningException(
					"Could not find any management machines for this cloud (management machine prefix is: "
							+ this.machineNamePrefix + ")");
		}

		final Set<String> machineIps = new HashSet<String>();
		for (final MachineDetails machineDetails : managementServers) {
			machineIps.add(machineDetails.getPrivateAddress());
		}

		this.deployer.shutdownMachinesWithIPs(machineIps);

	}

	@Override
	public void close() {
		this.deployer.close();
	}

	@Override
	public void addListener(final ProvisioningDriverListener pdl) {
		this.eventsListenersList.add(pdl);
	}

	protected void publishEvent(final String eventName, final Object... args) {
		for (final ProvisioningDriverListener listner : this.eventsListenersList) {
			listner.onProvisioningEvent(eventName, args);
		}
	}

}
