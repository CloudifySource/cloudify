package org.cloudifysource.esc.driver.provisioning.azure;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.esc.driver.provisioning.CloudDriverSupport;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.azure.client.CreatePersistentVMRoleDeploymentDescriptor;
import org.cloudifysource.esc.driver.provisioning.azure.client.DeletePersistentRoleVMDeploymentDetails;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureException;
import org.cloudifysource.esc.driver.provisioning.azure.client.MicrosoftAzureRestClient;
import org.cloudifysource.esc.driver.provisioning.azure.client.RoleAddressDetails;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoint;
import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;

import com.j_spaces.kernel.Environment;

/****************************************************************************
 * A custom Cloud Driver implementation for provisioning machines on Azure. * *
 * 
 * @author elip *
 */
public class MicrosoftAzureCloudDriver extends CloudDriverSupport implements
		ProvisioningDriver {

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
	private static final String AZURE_NETWORK_NAME = "azure.network.name";
	private static final String AZURE_STORAGE_ACCOUNT = "azure.storage.account";
	private static final String AZURE_AVAILABILITY_SET = "azure.availability.set";

	private String serverNamePrefix;

	// Azure Credentials
	private String subscriptionId;

	// Arguments for all machines
	private String location;
	private String addressSpace;
	private String networkName;
	private String affinityGroup;
	private String storageAccountName;

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

	private MicrosoftAzureRestClient azureClient;

	public MicrosoftAzureCloudDriver() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setConfig(final Cloud cloud, final String templateName,
			final boolean management) {
		super.setConfig(cloud, templateName, management);

		// Per template properties
		this.availabilitySet = (String) this.template.getCustom().get(
				AZURE_AVAILABILITY_SET);
		String pfxFile = (String) this.template.getCustom().get(AZURE_PFX_FILE);
		if (pfxFile == null && management) {
			throw new IllegalArgumentException("Custom field '"
					+ AZURE_PFX_FILE + "' must be set");
		}
		if (this.management) {
			this.pathToPfxFile = Environment.getHomeDirectory()
					+ File.separator + this.template.getLocalDirectory()
					+ File.separator + pfxFile;
		} else {
			this.pathToPfxFile = this.template.getRemoteDirectory()
					+ File.separator + pfxFile;
		}

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

		logger.fine("Initializing Azure REST Client");
		this.azureClient = new MicrosoftAzureRestClient(this.subscriptionId,
				this.pathToPfxFile, this.pfxPassword);

		final String wireLog = (String) this.cloud.getCustom().get(
				AZURE_WIRE_LOG);
		if (wireLog != null) {
			if (Boolean.parseBoolean(wireLog)) {
				this.azureClient.setLoggingFilter(logger);
			}
		}
	}

	@Override
	public MachineDetails startMachine(final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {

		MachineDetails machineDetails = new MachineDetails();
		String cloudServiceName = null;
		CreatePersistentVMRoleDeploymentDescriptor desc = null;
		try {
			logger.fine("Creating Cloud Service");

			cloudServiceName = azureClient.createCloudService(affinityGroup,
					duration, unit);

			logger.fine("Cloud Service Created : " + cloudServiceName);

			desc = new CreatePersistentVMRoleDeploymentDescriptor();
			desc.setRoleName(serverNamePrefix + "_role");
			desc.setDeploymentName(cloudServiceName);
			desc.setDeploymentSlot(deploymentSlot);
			desc.setImageName(imageName);
			desc.setAvailabilitySetName(availabilitySet);

			InputEndpoints inputEndpoints = new InputEndpoints();

			// Add End Point for each port

			if (this.endpoints != null) {
				for (Map<String, String> endpointPair : this.endpoints) {
					String name = endpointPair.get("name");
					int port = Integer.parseInt(endpointPair.get("port"));
					InputEndpoint endpoint = new InputEndpoint();
					endpoint.setLocalPort(port);
					endpoint.setPort(port);
					endpoint.setName(name);
					endpoint.setProtocol("TCP");
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

			desc.setInputEndpoints(inputEndpoints);
			desc.setNetworkName(networkName);
			desc.setPassword(password);
			desc.setSize(size);
			desc.setStorageAccountName(storageAccountName);
			desc.setUserName(userName);

			logger.fine("Launching Virtual Machine...");

			RoleAddressDetails roleAddressDetails = azureClient
					.createVirtualMachineDeployment(desc, cloudServiceName,
							duration, unit);

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

			logger.fine("Virtual Machine Started : " + machineDetails);

			return machineDetails;
		} catch (final MicrosoftAzureException e) {
			logger.warning("Failed Starting Virtual Machine properly. "
					+ "trying to delete it and any services that were pre dedicated for this instance");
			if (desc != null) {
				logger.warning("deleting role " + desc.getRoleName());
				try {
					azureClient.deleteRole(cloudServiceName,
							desc.getDeploymentName(), desc.getRoleName(),
							duration, unit);
				} catch (MicrosoftAzureException e1) {
					logger.log(Level.WARNING,
							"Failed deleting role " + desc.getRoleName(), e1);
				}
			}

			if (cloudServiceName != null) {
				logger.warning("the Cloud Service " + cloudServiceName
						+ " was created, deleting it...");
				try {
					azureClient.deleteCloudService(cloudServiceName, duration,
							unit);
				} catch (MicrosoftAzureException e1) {
					logger.log(Level.WARNING, "Failed deleting Cloud Service "
							+ cloudServiceName, e1);
				}
			}

			throw new CloudProvisioningException(e);
		}
	}

	@Override
	public MachineDetails[] startManagementMachines(final long duration,
			final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {

		try {
			logger.fine("Creating Affinity Group : " + affinityGroup);

			azureClient.createAffinityGroup(affinityGroup, location, duration,
					unit);

			logger.fine("Creating Virtual Network : " + networkName);

			azureClient.createVirtualNetwork(addressSpace, affinityGroup,
					networkName, duration, unit);

			logger.fine("Creating a Storage Account : " + storageAccountName);

			azureClient.createStorageAccount(affinityGroup, storageAccountName,
					duration, unit);

		} catch (final MicrosoftAzureException e) {
			throw new CloudProvisioningException(e);
		}

		int numberOfManagementMachines = this.cloud.getProvider()
				.getNumberOfManagementMachines();

		final ExecutorService executorService = Executors
				.newFixedThreadPool(numberOfManagementMachines);
		final List<Future<MachineDetails>> results = new ArrayList<Future<MachineDetails>>(
				numberOfManagementMachines);

		for (int i = 0; i < numberOfManagementMachines; i++) {
			StartMachineCallable task = new StartMachineCallable(duration, unit);
			Future<MachineDetails> future = executorService.submit(task);
			results.add(future);
		}

		// block until all machines are ready
		List<MachineDetails> managementMachinesDetails = new ArrayList<MachineDetails>();
		for (Future<MachineDetails> future : results) {
			try {
				managementMachinesDetails.add(future.get());
			} catch (final Exception e) {
				throw new CloudProvisioningException(e);
			}
		}

		return managementMachinesDetails
				.toArray(new MachineDetails[numberOfManagementMachines]);

	}

	@Override
	public boolean stopMachine(final String machineIp, final long duration,
			final TimeUnit unit) throws InterruptedException, TimeoutException,
			CloudProvisioningException {

		DeletePersistentRoleVMDeploymentDetails details = getDeletePersistentRoleVMDeploymentDetails(machineIp);

		try {
			logger.fine("Deleting Virtual Machine : " + details.getRoleName());

			azureClient.deleteRole(details.getHostedServiceName(),
					details.getDeploymentName(), details.getRoleName(),
					duration, unit);

			logger.fine("Deleting OS Disk : " + details.getOsDisk());

			azureClient.deleteOSDisk(details.getOsDisk(), duration, unit);

			logger.fine("Deleting Storage Account : "
					+ details.getStorageAccountName());

			azureClient.deleteStorageAccount(details.getStorageAccountName(),
					duration, unit);

			logger.fine("Deleteing Cloud Service : "
					+ details.getHostedServiceName());

			azureClient.deleteCloudService(details.getHostedServiceName(),
					duration, unit);

		} catch (MicrosoftAzureException e) {
			return false;
		}

		return true;
	}

	/**
	 * @param machineIp
	 * @return
	 */
	private DeletePersistentRoleVMDeploymentDetails getDeletePersistentRoleVMDeploymentDetails(
			final String machineIp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stopManagementMachines() throws TimeoutException,
			CloudProvisioningException {
		// TODO Auto-generated method stub

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

		private long duration;
		private TimeUnit unit;

		public StartMachineCallable(final long duration, final TimeUnit unit) {
			this.duration = duration;
			this.unit = unit;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public MachineDetails call() throws Exception {
			final MachineDetails machineDetails = startMachine(duration, unit);
			return machineDetails;
		}
	}
}
