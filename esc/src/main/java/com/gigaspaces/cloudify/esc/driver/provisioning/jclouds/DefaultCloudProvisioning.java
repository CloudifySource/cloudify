package com.gigaspaces.cloudify.esc.driver.provisioning.jclouds;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

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
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeState;

import org.openspaces.admin.Admin;
import com.gigaspaces.cloudify.dsl.cloud.Cloud2;
import com.gigaspaces.cloudify.dsl.cloud.CloudTemplate;
import com.gigaspaces.cloudify.esc.driver.provisioning.CloudProvisioningException;
import com.gigaspaces.cloudify.esc.driver.provisioning.CloudifyProvisioning;
import com.gigaspaces.cloudify.esc.driver.provisioning.MachineDetails;
import com.gigaspaces.cloudify.esc.installer.InstallationDetails;
import com.gigaspaces.cloudify.esc.installer.InstallerException;
import com.gigaspaces.cloudify.esc.jclouds.JCloudsDeployer;
import com.gigaspaces.cloudify.esc.util.Utils;
import com.gigaspaces.internal.utils.StringUtils;
import com.google.common.base.Predicate;

public class DefaultCloudProvisioning implements CloudifyProvisioning {

	private static final int WAIT_THREAD_SLEEP_MILLIS = 10000;
	private static final int WAIT_TIMEOUT_MILLIS = 360000;
	// private boolean verbose;
	// private long bootstrapTimeoutInMinutes = 15;
	// private long teardownTimeoutInMinutes = 15;
	private JCloudsDeployer deployer;

	private String machineNamePrefix;

	private String cloudName;

	private Cloud2 cloud;

	private String cloudTemplateName;
	private boolean management;

	private int counter = 0;
	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(DefaultCloudProvisioning.class.getName());
	private static final int MAX_MACHINE_LIMIT = 200;

	@Override
	public void setConfig(Cloud2 cloud, String cloudTemplateName, boolean management) {

		this.cloud = cloud;
		this.cloudTemplateName = cloudTemplateName;
		this.management = management;

		logger.info("Initializing Cloud Provisioning - management mode: " + management + ". Using template: "
				+ cloudTemplateName + " with cloud: " + cloud);

		this.cloudName = cloud.getName();

		String prefix = (management ? cloud.getProvider().getManagementGroup() : cloud.getProvider()
				.getMachineNamePrefix());

		if ((prefix == null) || (prefix.length() == 0)) {
			if (management) {
				prefix = "cloudify_managememnt_";
			} else {
				prefix = "cloudify_agent_";
			}

			logger.warning("Prefix for machine name was not set. Using: " + prefix);
		}

		// TODO - stop using a random number - it is not safe!
		// Keep a counter and validate on startup that previous values are not
		// used!
		// attach a random number to the prefix to prevent collisions
		this.machineNamePrefix = prefix;

	}

	private void initDeployer(Cloud2 cloud) {
		if (this.deployer != null) {
			return;
		}
		logger.info("Creating jclouds context deployer with user: " + cloud.getUser().getUser());

		CloudTemplate cloudTemplate = cloud.getTemplates().get(cloudTemplateName);

		try {
			this.deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(), cloud
					.getUser().getApiKey());
			this.deployer.setImageId(cloudTemplate.getImageId());
			this.deployer.setMinRamMegabytes(cloudTemplate.getMachineMemoryMB());
			this.deployer.setHardwareId(cloudTemplate.getHardwareId());
			this.deployer.setLocationId(cloudTemplate.getLocationId());
			this.deployer.setExtraOptions(cloudTemplate.getOptions());

		} catch (final IOException e) {
			throw new IllegalStateException("Failed to create cloud Deployer", e);
		}
	}

	// @Override
	// public void bootstrapCloud(final Cloud2 cloud) throws
	// CloudProvisioningException {
	// CloudGridAgentBootstrapper installer = new CloudGridAgentBootstrapper();
	//
	// installer.setProgressInSeconds(10);
	// installer.setVerbose(verbose);
	//
	// try {
	// installer.boostrapCloudAndWait(cloud, bootstrapTimeoutInMinutes,
	// TimeUnit.MINUTES);
	// } catch (Exception e) {
	// throw new CloudProvisioningException(e);
	// }
	//
	// }

	@Override
	public MachineDetails startMachine(long timeout, TimeUnit unit) throws TimeoutException, CloudProvisioningException {
		if (timeout < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}

		logger.info("DefaultCloudProvisioning: startMachine - management == " + management);

		initDeployer(cloud);

		try {
			MachineDetails md = doStartMachine(timeout, unit);
			return md;
		} catch (Exception e) {
			throw new CloudProvisioningException("Failed to start cloud machine", e);
		}

	}

	private static void logServerDetails(NodeMetadata server, File tempFile) {
		if (logger.isLoggable(Level.INFO)) {
			logger.info(nodePrefix(server) + "ESM Server was created.");
			if (tempFile == null) {
				logger.info(nodePrefix(server) + "Password: ***");
			} else {
				logger.info(nodePrefix(server) + "Key File: " + tempFile.getAbsolutePath());
			}

			logger.info(nodePrefix(server) + "Public IP: " + Arrays.toString(server.getPublicAddresses().toArray()));
			logger.info(nodePrefix(server) + "Private IP: " + Arrays.toString(server.getPrivateAddresses().toArray()));
			logger.info(nodePrefix(server) + "Target IP for connection: "
					+ server.getPrivateAddresses().iterator().next());
			if (tempFile == null) {
				logger.info(nodePrefix(server) + "Connect with putty using: putty -pw "
						+ server.getCredentials().credential + " root@" + server.getPublicAddresses().toArray()[0]);
			} else {

				if (server.getPublicAddresses().size() > 0) {
					logger.info(nodePrefix(server) + "Connect with putty using: putty -i " + tempFile.getAbsolutePath()
							+ " " + server.getCredentials().identity + "@" + server.getPublicAddresses().toArray()[0]);
				} else {
					logger.info(nodePrefix(server) + "Server's is starting but its public address is not available.");
				}
			}
		}
	}

	private static String nodePrefix(NodeMetadata node) {
		return "[" + node.getId() + "] ";
	}

	/*********
	 * Looks for a free machine name by appending a counter to the precalculated
	 * machine name prefix. If the max counter value is reached, code will loop
	 * back to 0, so that previously used machine names will be reused.
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
			NodeMetadata existingNode = deployer.getServerByID(machineName);
			if (existingNode == null) {
				return machineName;
			}
		}
		throw new CloudProvisioningException("Number of machines has exceeded allowed machine limit ("
				+ MAX_MACHINE_LIMIT + ")");

	}

	private MachineDetails doStartMachine(long timeout, TimeUnit unit) throws Exception {
		if (timeout < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}

		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		final String machineName = createNewMachineName();
		logger.info("Starting a new cloud machines with group: " + machineName);

		return createServer(end, machineName);

	}

	private MachineDetails createServer(final long end, final String machineName) throws CloudProvisioningException,
			Exception {
		NodeMetadata node;
		try {
			node = deployer.createServer(machineName);
		} catch (InstallerException e) {
			throw new CloudProvisioningException("Failed to create cloud machine", e);
		}

		logger.info("New machine is starting");

		// At this point the machine is starting. Any error beyond this point
		// must clean up the machine
		try {
			MachineDetails md = createMachineDetailsFromNode(node);

			handleServerCredentials(node);

			waitUntilServerIsActive(node.getId(), Utils.millisUntil(end), TimeUnit.MILLISECONDS);
			return md;
		} catch (Exception e) {
			logger.log(Level.SEVERE,
					"Cloud machine was started but an error occured during initialization. Shutting down machine", e);
			this.deployer.shutdownMachine(node.getId());
			throw e;
		}
	}

	private MachineDetails createMachineDetailsFromNode(NodeMetadata node) {
		MachineDetails md = new MachineDetails();
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
		return md;
	}

	/*********
	 * Periodically gets the server status from Rackspace, until the server's
	 * status changes to ACTIVE, or a timeout expires.
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

	private void handleServerCredentials(NodeMetadata server) throws CloudProvisioningException {
		final String credential = server.getCredentials().credential;
		File tempFile = null;

		if (credential == null) { // must be using an existing key file or
									// password
			logger.info("Cloud did not provide server credentials, checking in cloud configuration file");
			if ((cloud.getUser().getKeyFile() == null) || (cloud.getUser().getKeyFile().length() == 0)) {
				logger.info("No key file specified in cloud configuration");
				// no key file. Check for password
				if (cloud.getConfiguration().getRemotePassword() == null) {
					logger.severe("No Password specified in cloud configuration - connection to the new machine is not possible.");
					throw new CloudProvisioningException(
							"Cloud did not provider server credentials, and no credentials (password or key file) supplied with cloud configuration file");
				}
			}
			tempFile = new File(cloud.getUser().getKeyFile());
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
			logger.info("Cloud has provided password for remote connection to new machine");
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

		logger.info("Created new Installation Details: " + details);
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

	// @Override
	// public void teardownCloud(final Cloud2 cloud) throws
	// CloudProvisioningException {
	// CloudGridAgentBootstrapper installer = new CloudGridAgentBootstrapper();
	//
	//
	// installer.setProgressInSeconds(10);
	// installer.setVerbose(verbose);
	// installer.setForce(true);
	//
	// try {
	// installer.teardownCloudAndWait(teardownTimeoutInMinutes,
	// TimeUnit.MINUTES, cloud);
	// } catch (InstallerException e) {
	// throw new CloudProvisioningException(e);
	// } catch (TimeoutException e) {
	// throw new CloudProvisioningException(e);
	// } catch (InterruptedException e) {
	// throw new CloudProvisioningException(e);
	// }
	//
	// }

	private NodeMetadata getServerWithIP(final String ip) {
		return deployer.getServerWithIP(ip);
	}

	private final Map<String, Long> stoppingMachines = new ConcurrentHashMap<String, Long>();
	private Admin admin;
	private static final int MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT = 120000;

	@Override
	public boolean stopMachine(String machineIp) throws CloudProvisioningException {
		logger.info("Stop Machine - machineIp: " + machineIp);

		// logger.info("Check that we are not shutting down LUS or ESM - lusIP  in locators: "
		// + config.getLocator());
		// TODO - the adapter should do this.
		// if (config.getLocator() != null && config.getLocator().contains(ip))
		// {
		// logger.info("Recieved scale in request for LUS/ESM server. Ignoring.");
		// return false;
		// }

		// TODO - move this stuff to the adapter class
		// ignore duplicate shutdown requests for same machine
		final Long previousRequest = stoppingMachines.get(machineIp);
		if ((previousRequest != null)
				&& (System.currentTimeMillis() - previousRequest < MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT)) {
			return true;
		}

		// TODO - add a task that cleans up this map
		stoppingMachines.put(machineIp, System.currentTimeMillis());
		logger.info("Scale IN -- " + machineIp + " --");

		logger.info("Looking Up Cloud server with this IP");
		final NodeMetadata server = getServerWithIP(machineIp);
		if (server != null) {
			logger.info("Found server: " + server.getId() + ". Shutting it down");
			deployer.shutdownMachine(server.getId());

			logger.info("Server: " + server.getId() + " shutdown has started.");
			return true;

		} else {
			logger.log(Level.SEVERE, "Recieved scale in request for machine with ip " + machineIp
					+ " but this IP could not be found in the Cloud server list");
		}
		return false;

	}

	@Override
	public void setAdmin(Admin admin) {
		this.admin = admin;

	}

	@Override
	public MachineDetails[] startManagementMachines(final long duration, final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {
		if (duration < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}
		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		logger.info("DefaultCloudProvisioning: startMachine - management == " + management);

		initDeployer(cloud);
		// force the creation of the jclouds template - otherwise, it may be
		// created multiple times, once for each management machine
		this.deployer.getTemplate();

		final String managementMachinePrefix = this.machineNamePrefix;

		// first check if management already exists
		MachineDetails[] existingManagementServers = getExistingManagementServers(managementMachinePrefix);
		if (existingManagementServers.length > 0) {
			logger.info("Found existing servers matching the name: " + managementMachinePrefix);
			return existingManagementServers;
		}

		// launch the management machines
		int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		MachineDetails[] createdMachines = doStartManagementMachines(endTime, numberOfManagementMachines);
		return createdMachines;

	}

	private MachineDetails[] doStartManagementMachines(final long endTime, int numberOfManagementMachines)
			throws TimeoutException, CloudProvisioningException {
		ExecutorService executors = Executors.newFixedThreadPool(numberOfManagementMachines);

		@SuppressWarnings("unchecked")
		Future<MachineDetails>[] futures = (Future<MachineDetails>[]) new Future<?>[numberOfManagementMachines];

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
			MachineDetails[] createdManagementMachines = new MachineDetails[numberOfManagementMachines];
			for (int i = 0; i < createdManagementMachines.length; i++) {
				try {
					createdManagementMachines[i] = futures[i].get(endTime - System.currentTimeMillis(),
							TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					++numberOfErrors;
					logger.log(Level.SEVERE, "Failed to start a management machine", e);
					if (firstCreationException == null) {
						firstCreationException = e;
					}

				} catch (ExecutionException e) {
					++numberOfErrors;
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

	private void handleProvisioningFailure(int numberOfManagementMachines, int numberOfErrors,
			Exception firstCreationException, MachineDetails[] createdManagementMachines)
			throws CloudProvisioningException {
		logger.severe("Of the required " + numberOfManagementMachines + " management machines, " + numberOfErrors
				+ " failed to start.");
		if (numberOfManagementMachines > numberOfErrors) {
			logger.severe("Shutting down the other managememnt machines");
			for (MachineDetails machineDetails : createdManagementMachines) {
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
		Set<? extends NodeMetadata> existingManagementServers = this.deployer
				.getServers(new Predicate<ComputeMetadata>() {

					@Override
					public boolean apply(ComputeMetadata input) {
						final NodeMetadata node = (NodeMetadata) input;
						if (node.getGroup() == null) {
							return false;
						}
						// only running or pending nodes are interesting
						if (!(node.getState().equals(NodeState.RUNNING) || node.getState().equals(NodeState.PENDING))) {

							return false;
						}

						return node.getGroup().startsWith(managementMachinePrefix);

					}
				});

		MachineDetails[] result = new MachineDetails[existingManagementServers.size()];
		int i = 0;
		for (NodeMetadata node : existingManagementServers) {
			result[i] = createMachineDetailsFromNode(node);
			result[i].setAgentRunning(true);
			result[i].setCloudifyInstalled(true);

		}
		return result;

	}

	@Override
	public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {

		initDeployer(this.cloud);
		MachineDetails[] managementServers = getExistingManagementServers(this.machineNamePrefix);

		if (managementServers.length == 0) {
			throw new CloudProvisioningException(
					"Could not find any management machines for this cloud (management machine prefix is: "
							+ this.machineNamePrefix + ")");
		}

		Set<String> machineIps = new HashSet<String>();
		for (MachineDetails machineDetails : managementServers) {
			machineIps.add(machineDetails.getPrivateAddress());
		}

		this.deployer.shutdownMachinesWithIPs(machineIps);

	}
}
