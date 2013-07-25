package org.cloudifysource.esc.driver.provisioning.azure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.CloudDriverSupport;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.azure.client.CreatePersistentVMRoleDeploymentDescriptor;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.client.RoleDetails;
import org.cloudifysource.esc.driver.provisioning.azure.model.AttachedTo;
import org.cloudifysource.esc.driver.provisioning.azure.model.Deployment;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disk;
import org.cloudifysource.esc.driver.provisioning.azure.model.Disks;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedService;
import org.cloudifysource.esc.driver.provisioning.azure.model.HostedServices;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoint;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;

/***************************************************************************************
 * A custom Cloud Driver implementation for provisioning machines on Azure.
 *
 * @author elip
 ***************************************************************************************/

public class MicrosoftAzureCloudDriver extends CloudDriverSupport implements
		ProvisioningDriver {

	private static final String CLOUDIFY_AFFINITY_PREFIX = "cloudifyaffinity";
	private static final String CLOUDIFY_CLOUD_SERVICE_PREFIX = "cloudifycloudservice";
	private static final String CLOUDIFY_STORAGE_ACCOUNT_PREFIX = "cloudifystorage";

	// Custom template DSL properties
	private static final String AZURE_PFX_FILE = "azure.pfx.file";
	private static final String AZURE_PFX_PASSWORD = "azure.pfx.password";
	private static final String AZURE_ENDPOINTS = "azure.endpoints";

	// Custom cloud DSL properties
	private static final String AZURE_WIRE_LOG = "azure.wireLog";
	private static final String AZURE_DEPLOYMENT_SLOT = "azure.deployment.slot";
	private static final String AZURE_AFFINITY_LOCATION = "azure.affinity.location";
	private static final String AZURE_NETOWRK_ADDRESS_SPACE = "azure.address.space";
	private static final String AZURE_AFFINITY_GROUP = "azure.affinity.group";
	private static final String AZURE_NETWORK_NAME = "azure.networksite.name";
	private static final String AZURE_STORAGE_ACCOUNT = "azure.storage.account";
	private static final String AZURE_AVAILABILITY_SET = "azure.availability.set";
	private static final String AZURE_CLEANUP_ON_TEARDOWN = "azure.cleanup.on.teardown";

	private boolean cleanup;

	private String serverNamePrefix;

	// Azure Credentials
	private String subscriptionId;

	// Arguments for all machines
	private String location;
	private String addressSpace;
	private String networkName;
	private String affinityGroup;
	private String storageAccountName;

	private static final long DEFAULT_SHUTDOWN_DURATION = 15 * 60 * 1000; // 15 minutes
	// minutes

	// Arguments per template
	private String deploymentSlot;
	private String imageName;
	private String userName;
	private String password;
	private String size;
	private String pathToPfxFile;
	private String pfxPassword;
	private String availabilitySet;
	private List<Map<String, String>> endpoints;

	private static final int WEBUI_PORT = 8099;
	private static final int REST_PORT = 8100;
	private static final int SSH_PORT = 22;

	private static final Logger logger = Logger
			.getLogger(MicrosoftAzureCloudDriver.class.getName());
	private static final long CLEANUP_TIMEOUT = 60 * 1000 * 5; // five minutes

	private static MicrosoftAzureRestClient azureClient;

	public MicrosoftAzureCloudDriver() {
	}

	private static synchronized void initRestClient(
			final String subscriptionId, final String pathToPfxFile,
			final String pfxPassword, final boolean enableWireLog) {
		if (azureClient == null) {
			logger.fine("initializing Azure REST client");
			azureClient = new MicrosoftAzureRestClient(subscriptionId,
					pathToPfxFile, pfxPassword, CLOUDIFY_AFFINITY_PREFIX,
					CLOUDIFY_CLOUD_SERVICE_PREFIX,
					CLOUDIFY_STORAGE_ACCOUNT_PREFIX);
			if (enableWireLog) {
				azureClient.setLoggingFilter(logger);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setConfig(final Cloud cloud, final String templateName, final boolean management, 
			final String serviceName) {
		
		super.setConfig(cloud, templateName, management, serviceName);

		// Per template properties
		this.availabilitySet = (String) this.template.getCustom().get(
				AZURE_AVAILABILITY_SET);
		String pfxFile = (String) this.template.getCustom().get(AZURE_PFX_FILE);
		if (pfxFile == null && management) {
			throw new IllegalArgumentException("Custom field '"
					+ AZURE_PFX_FILE + "' must be set");
		}

		this.pathToPfxFile = this.template.getAbsoluteUploadDir() + File.separator + pfxFile;

		this.pfxPassword = (String) this.template.getCustom().get(
				AZURE_PFX_PASSWORD);
		if (pfxPassword == null && management) {
			throw new IllegalArgumentException("Custom field '"
					+ AZURE_PFX_PASSWORD + "' must be set");
		}
		this.deploymentSlot = (String) this.template.getCustom().get(
				AZURE_DEPLOYMENT_SLOT);
		if (deploymentSlot == null) {
			deploymentSlot = "Staging";
		}
		this.endpoints = (List<Map<String, String>>) this.template.getCustom()
				.get(AZURE_ENDPOINTS);

		this.imageName = this.template.getImageId();
		this.userName = this.template.getUsername();
		this.password = this.template.getPassword();
		this.size = this.template.getHardwareId();

		this.subscriptionId = this.cloud.getUser().getUser();

		this.cleanup = Boolean.parseBoolean((String) this.cloud.getCustom()
				.get(AZURE_CLEANUP_ON_TEARDOWN));

		this.location = (String) this.cloud.getCustom().get(
				AZURE_AFFINITY_LOCATION);
		if (location == null) {
			throw new IllegalArgumentException("Custom field '"
					+ AZURE_AFFINITY_LOCATION + "' must be set");
		}
		this.addressSpace = (String) this.cloud.getCustom().get(
				AZURE_NETOWRK_ADDRESS_SPACE);
		if (addressSpace == null) {
			throw new IllegalArgumentException("Custom field '"
					+ AZURE_NETOWRK_ADDRESS_SPACE + "' must be set");
		}
		this.affinityGroup = (String) this.cloud.getCustom().get(
				AZURE_AFFINITY_GROUP);
		if (affinityGroup == null) {
			throw new IllegalArgumentException("Custom field '"
					+ AZURE_AFFINITY_GROUP + "' must be set");
		}
		this.networkName = (String) this.cloud.getCustom().get(
				AZURE_NETWORK_NAME);
		if (networkName == null) {
			throw new IllegalArgumentException("Custom field '"
					+ AZURE_NETWORK_NAME + "' must be set");
		}
		this.storageAccountName = (String) this.cloud.getCustom().get(
				AZURE_STORAGE_ACCOUNT);
		if (storageAccountName == null) {
			throw new IllegalArgumentException("Custom field '"
					+ AZURE_STORAGE_ACCOUNT + "' must be set");
		}

		if (this.management) {
			this.serverNamePrefix = this.cloud.getProvider()
					.getManagementGroup();
		} else {
			this.serverNamePrefix = this.cloud.getProvider()
					.getMachineNamePrefix();
		}

		final String wireLog = (String) this.cloud.getCustom().get(
				AZURE_WIRE_LOG);

		boolean enableWireLog = false;
		if (wireLog != null) {
			enableWireLog = Boolean.parseBoolean(wireLog);
		}
		initRestClient(this.subscriptionId, this.pathToPfxFile,
				this.pfxPassword, enableWireLog);
	}

	@Override
	public MachineDetails startMachine(final String locationId, final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		long endTime = System.currentTimeMillis() + unit.toMillis(duration);
		return startMachine(endTime);

	}

	private MachineDetails startMachine(final long endTime)
			throws TimeoutException, CloudProvisioningException {

		MachineDetails machineDetails = new MachineDetails();
		CreatePersistentVMRoleDeploymentDescriptor desc = null;
		RoleDetails roleAddressDetails = null;
		try {

			desc = new CreatePersistentVMRoleDeploymentDescriptor();
			desc.setRoleName(serverNamePrefix + "_role");
			desc.setDeploymentSlot(deploymentSlot);
			desc.setImageName(imageName);
			desc.setAvailabilitySetName(availabilitySet);
			desc.setAffinityGroup(affinityGroup);

			InputEndpoints inputEndpoints = createInputEndPoints();

			desc.setInputEndpoints(inputEndpoints);
			desc.setNetworkName(networkName);
			desc.setPassword(password);
			desc.setSize(size);
			desc.setStorageAccountName(storageAccountName);
			desc.setUserName(userName);

			logger.info("Launching a new virtual machine");

			roleAddressDetails = azureClient.createVirtualMachineDeployment(
					desc, endTime);

			machineDetails.setPrivateAddress(roleAddressDetails.getPrivateIp());
			machineDetails.setPublicAddress(roleAddressDetails.getPublicIp());
			machineDetails.setMachineId(roleAddressDetails.getId());
			machineDetails.setAgentRunning(false);
			machineDetails.setCloudifyInstalled(false);
			machineDetails.setInstallationDirectory(this.template
					.getRemoteDirectory());
			machineDetails.setRemoteDirectory(this.template
					.getRemoteDirectory());
			machineDetails.setRemotePassword(password);
			machineDetails.setRemoteUsername(userName);
			return machineDetails;
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}

	}

	@Override
	public MachineDetails[] startManagementMachines(final long duration,
			final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {

		long endTime = System.currentTimeMillis() + unit.toMillis(duration);
		try {
			azureClient.createAffinityGroup(affinityGroup, location, endTime);
			azureClient.createVirtualNetworkSite(addressSpace, affinityGroup,
					networkName, endTime);
			azureClient.createStorageAccount(affinityGroup, storageAccountName,
					endTime);
		} catch (final Exception e) {
			logger.warning("Failed creating management services : " + e.getMessage());
			if (cleanup) {
				try {
					logger.info("Cleaning up any services that may have already been started.");
					cleanup();
				} catch (final CloudProvisioningException e1) {
					// we catch this because we want to throw the original exception. not the one that happened on cleanup.
					logger.warning("Failed to cleanup some management services. Please shut them down manually or use the teardown-cloud command.");
					logger.fine(ExceptionUtils.getFullStackTrace(e1));
				}
			}
			throw new CloudProvisioningException(e);
		}

		int numberOfManagementMachines = this.cloud.getProvider()
				.getNumberOfManagementMachines();

		final ExecutorService executorService = Executors
				.newFixedThreadPool(numberOfManagementMachines);

		try {
			return startManagementMachines(endTime, numberOfManagementMachines,
					executorService);
		} finally {
			executorService.shutdown();
		}

	}

	private void cleanup() throws CloudProvisioningException {

		final long endTime = System.currentTimeMillis() + CLEANUP_TIMEOUT;

		boolean deletedNetwork = false;
		boolean deletedStorage = false;
		Exception first = null;

		try {
			deletedNetwork = azureClient.deleteVirtualNetworkSite(networkName, endTime);
		} catch (final Exception e) {
			first = e;
			logger.warning("Failed deleting virtual network site " + networkName + " : " + e.getMessage());
			logger.fine(ExceptionUtils.getFullStackTrace(e));
		}

		try {
			deletedStorage = azureClient.deleteStorageAccount(storageAccountName, endTime);
		} catch (final Exception e) {
			if (first == null) {
				first = e;
			}
			logger.warning("Failed deleting storage account " + storageAccountName + " : " + e.getMessage());
			logger.fine(ExceptionUtils.getFullStackTrace(e));
		}

		if (deletedNetwork && deletedStorage) {
			try {
				azureClient.deleteAffinityGroup(affinityGroup, endTime);
			} catch (final Exception e) {
				if (first == null) {
					first = e;
				}
				logger.warning("Failed deleting affinity group " + affinityGroup + " : " + e.getMessage());
				logger.fine(ExceptionUtils.getFullStackTrace(e));
			}
		} else {
			logger.info("Not trying to delete affinity group since either virtual network " + networkName + " , or storage account " + storageAccountName + " depend on it.");
		}
		if (first != null) {
			throw new CloudProvisioningException(first);
		}
	}

	/**
	 * @param endTime
	 * @param numberOfManagementMachines
	 * @param executorService
	 * @return
	 * @throws CloudProvisioningException
	 * @throws TimeoutException
	 */
	private MachineDetails[] startManagementMachines(final long endTime,
			final int numberOfManagementMachines,
			final ExecutorService executorService)
			throws CloudProvisioningException, TimeoutException {

		final List<Future<MachineDetails>> results = new ArrayList<Future<MachineDetails>>(
				numberOfManagementMachines);

		for (int i = 0; i < numberOfManagementMachines; i++) {
			StartMachineCallable task = new StartMachineCallable(endTime);
			Future<MachineDetails> future = executorService.submit(task);
			results.add(future);
		}

		// block until tasks have stopped execution
		List<Throwable> exceptionsOnManagementStart = new ArrayList<Throwable>();

		List<MachineDetails> managementMachinesDetails = new ArrayList<MachineDetails>();
		for (Future<MachineDetails> future : results) {
			try {
				managementMachinesDetails.add(future.get());
			} catch (final Exception e) {
				if (e instanceof InterruptedException) {
					exceptionsOnManagementStart.add(e);
				} else {
					ExecutionException executionException = (ExecutionException)e;
					Throwable rootCause = ExceptionUtils.getRootCause(executionException);
					// print exception messages to the cli as they happen.
					// otherwise they are only shown in a log file.
					// this serves as a better user experience (users may not be aware of the file).
					logger.warning(rootCause.getMessage());
					exceptionsOnManagementStart.add(rootCause);
				}
			}
		}
		if (exceptionsOnManagementStart.isEmpty()) {
			return managementMachinesDetails
					.toArray(new MachineDetails[numberOfManagementMachines]);
		} else {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Here are all the exception caught from all threads");
				for (Throwable t : exceptionsOnManagementStart) {
					logger.finest(ExceptionUtils.getFullStackTrace(t));
				}
			}

			try {
				logger.warning("Failed to start management machines. cleaning up any services that might have already been started.");
				stopManagementMachines();
			} catch (CloudProvisioningException e) {
				// catch any exceptions here.
				// otherwise they will end up as the exception thrown to the CLI./
				// thats not what we want in this case since we want the exception that failed the bootstrap command.
				logger.warning("Failed to cleanup some management services. Please shut them down manually or use the teardown-cloud command.");
				logger.fine(ExceptionUtils.getFullStackTrace(e));
			}
			throw new CloudProvisioningException(
					exceptionsOnManagementStart.get(0).getMessage(),
					exceptionsOnManagementStart.get(0));

		}
	}

	@Override
	public boolean stopMachine(final String machineIp, final long duration,
			final TimeUnit unit) throws InterruptedException, TimeoutException,
			CloudProvisioningException {

		if (isStopRequestRecent(machineIp)) {
			return false;
		}

		long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		boolean connectToPrivateIp = this.cloud.getConfiguration()
				.isConnectToPrivateIp();
		try {
			azureClient.deleteVirtualMachineByIp(machineIp, connectToPrivateIp,
					endTime);
			return true;
		} catch (MicrosoftAzureException e) {
			throw new CloudProvisioningException(e);
		}
	}

	@Override
	public void stopManagementMachines() throws TimeoutException,
			CloudProvisioningException {

		long endTime = System.currentTimeMillis() + DEFAULT_SHUTDOWN_DURATION;
		boolean success = false;

		ExecutorService service = Executors.newCachedThreadPool();
		try {
			stopManagementMachines(endTime, service);
			success = true;
		} finally {
			if (!success) {
				if (cleanup) {
					logger.warning("Failed to shutdown management machines. no cleanup attempt will be made.");
				}
			}
			service.shutdown();
		}

        scanLeakingNodes();

		if (cleanup) {
            logger.info("Cleaning up management services");
            cleanup();
		}
	}

    private void scanLeakingNodes() {
        try {
            HostedServices hostedServices = azureClient.listHostedServices();
            if (!hostedServices.getHostedServices().isEmpty()) {
                logger.warning("Scanning for leaking nodes...");
                for (HostedService hostedService : hostedServices) {
                    HostedService serviceWithDeployments = azureClient.getHostedService(hostedService.getServiceName(), true);
                    Deployment deployment = serviceWithDeployments.getDeployments().getDeployments().get(0);
                    if (deployment != null) {
                        logger.info("Found : " + deployment.getRoleList().getRoles().get(0).getRoleName());
                    }
                }
                throw new CloudProvisioningException("There are still running instances, please shut them down and try again.");
            }
        } catch (final Exception e) {
            logger.warning("Failed Retrieving running virtual machines : " + e.getMessage());
            // nothing to do here...
        }
    }

    /**
	 * @param endTime
	 * @param service
	 * @throws TimeoutException
	 * @throws CloudProvisioningException
	 */
	private void stopManagementMachines(final long endTime,
			final ExecutorService service) throws TimeoutException,
			CloudProvisioningException {

		List<Future<?>> futures = new ArrayList<Future<?>>();

		Disks disks = null;
		try {
			disks = azureClient.listOSDisks();
		} catch (MicrosoftAzureException e1) {
			throw new CloudProvisioningException(e1);
		}
		for (Disk disk : disks) {
			AttachedTo attachedTo = disk.getAttachedTo();
			if (attachedTo != null) { // protect against zombie disks
				String roleName = attachedTo.getRoleName();
				if (roleName.startsWith(this.serverNamePrefix)) {
					final String diskName = disk.getName();
					final String deploymentName = attachedTo.getDeploymentName();
					final String hostedServiceName = attachedTo
							.getHostedServiceName();
					StopManagementMachineCallable task = new StopManagementMachineCallable(
							deploymentName, hostedServiceName, diskName,
							endTime);
					futures.add(service.submit(task));
				}
			} else {
				if (disk.getName().contains(serverNamePrefix)) {
					try {
						logger.info("Detected a zombie OS Disk with name " + disk.getName() + ", Deleting it.");
						azureClient.deleteOSDisk(disk.getName(), endTime);
					} catch (final Exception e) {
						throw new CloudProvisioningException(e);
					}
				}
			}
		}

		// block until all tasks stop execution
		List<Throwable> exceptionOnStopMachines = new ArrayList<Throwable>();
		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (final Exception e) {
				if (e instanceof InterruptedException) {
					exceptionOnStopMachines.add(e);
				} else {
					ExecutionException executionException = (ExecutionException)e;
					Throwable rootCause = ExceptionUtils.getRootCause(executionException);
					// print exception messages to the cli as they happen.
					// otherwise they are only shown in a log file.
					// this serves as a better user experience (users may not be aware of the file).
					logger.warning(rootCause.getMessage());
					exceptionOnStopMachines.add(rootCause);
				}
			}
		}
		if (!(exceptionOnStopMachines.isEmpty())) {
			if (logger.isLoggable(Level.FINEST)) {
				for (Throwable e : exceptionOnStopMachines) {
					logger.finest(ExceptionUtils.getFullStackTrace(e));
				}
			}
			throw new CloudProvisioningException(
					exceptionOnStopMachines.get(0).getMessage(),
					exceptionOnStopMachines.get(0));
		}

	}

	@Override
	public String getCloudName() {
		return "azure";
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	/**
	 *
	 * @author elip
	 *
	 */
	private class StartMachineCallable implements Callable<MachineDetails> {

		private final long endTime;

		public StartMachineCallable(final long endTime) {
			this.endTime = endTime;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public MachineDetails call() throws Exception {
			final MachineDetails machineDetails = startMachine(endTime);
			return machineDetails;
		}
	}

	/**
	 *
	 * @author elip
	 *
	 */
	private class StopManagementMachineCallable implements Callable<Boolean> {

		private final String deploymentName;
		private final String hostedServiceName;
		private final long endTime;

		public StopManagementMachineCallable(final String deploymentName,
				final String hostedServiceName, final String diskName,
				final long endTime) {
			this.deploymentName = deploymentName;
			this.hostedServiceName = hostedServiceName;
			this.endTime = endTime;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public Boolean call() throws CloudProvisioningException,
				TimeoutException {

			return stopManagementMachine(hostedServiceName, deploymentName,
					endTime);

		}
	}

	private boolean stopManagementMachine(final String hostedServiceName,
			final String deploymentName, final long endTime)
			throws CloudProvisioningException, TimeoutException {

		try {
			azureClient.deleteVirtualMachineByDeploymentName(hostedServiceName,
					deploymentName, endTime);
			return true;
		} catch (MicrosoftAzureException e) {
			throw new CloudProvisioningException(e);
		} catch (InterruptedException e) {
			throw new CloudProvisioningException(e);
		}

	}

	private InputEndpoints createInputEndPoints() {

		InputEndpoints inputEndpoints = new InputEndpoints();

		// Add End Point for each port

		if (this.endpoints != null) {
			for (Map<String, String> endpointMap : this.endpoints) {
				String name = endpointMap.get("name");
				int port = Integer.parseInt(endpointMap.get("port"));
				String protocol = endpointMap.get("protocol");
				InputEndpoint endpoint = new InputEndpoint();
				endpoint.setLocalPort(port);
				endpoint.setPort(port);
				endpoint.setName(name);
				endpoint.setProtocol(protocol);
				inputEndpoints.getInputEndpoints().add(endpoint);

			}
		}

		// open the SSH port on all vm's
		InputEndpoint sshEndpoint = new InputEndpoint();
		sshEndpoint.setLocalPort(SSH_PORT);
		sshEndpoint.setPort(SSH_PORT);
		sshEndpoint.setName("SSH");
		sshEndpoint.setProtocol("TCP");
		inputEndpoints.getInputEndpoints().add(sshEndpoint);

		// open WEBUI and REST ports for management machines
		if (this.management) {

			InputEndpoint webuiEndpoint = new InputEndpoint();
			webuiEndpoint.setLocalPort(WEBUI_PORT);
			webuiEndpoint.setPort(WEBUI_PORT);
			webuiEndpoint.setName("Webui");
			webuiEndpoint.setProtocol("TCP");
			inputEndpoints.getInputEndpoints().add(webuiEndpoint);

			InputEndpoint restEndpoint = new InputEndpoint();
			restEndpoint.setLocalPort(REST_PORT);
			restEndpoint.setPort(REST_PORT);
			restEndpoint.setName("Rest");
			restEndpoint.setProtocol("TCP");
			inputEndpoints.getInputEndpoints().add(restEndpoint);
		}
		return inputEndpoints;
	}

	@Override
	public Object getComputeContext() {
		return null;
	}

	@Override
	public void onServiceUninstalled(long duration, TimeUnit unit) {
		// TODO Auto-generated method stub
	}
}
