package com.gigaspaces.cloudify.esc.driver.provisioning;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.discovery.LookupLocator;

import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.core.bean.Bean;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.CpuCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioning;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioningException;

import com.gigaspaces.cloudify.dsl.cloud.Cloud2;
import com.gigaspaces.cloudify.dsl.cloud.CloudTemplate;
import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.internal.DSLException;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.esc.esm.CloudMachineProvisioningConfig;
import com.gigaspaces.cloudify.esc.installer.AgentlessInstaller;
import com.gigaspaces.cloudify.esc.installer.InstallationDetails;
import com.gigaspaces.cloudify.esc.installer.InstallerException;
import com.gigaspaces.internal.utils.StringUtils;

/****************************
 * An ESM machine provisioning implementation used by the Cloudify cloud driver.
 * All calls to start/stop a machine are delegated to the CloudifyProvisioning
 * implementation. If the started machine does not have an agent running, this
 * class will install gigaspaces and start the agent using the Agent-less
 * Installer process.
 * 
 * @author barakme
 * 
 */
public class ElasticMachineProvisioningCloudifyAdapter implements ElasticMachineProvisioning, Bean {

	private static final int DEFAULT_GSA_LOOKUP_TIMEOUT_SECONDS = 15;
	private CloudifyProvisioning cloudifyProvisioning;
	private Admin admin;
	private Map<String, String> properties;
	private Cloud2 cloud;
	private String cloudTemplate;
	private String lookupLocatorsString;
	private CloudMachineProvisioningConfig config;
	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ElasticMachineProvisioningCloudifyAdapter.class.getName());

	@Override
	public boolean isStartMachineSupported() {
		return true;
	}

	@Override
	public GridServiceAgent[] getDiscoveredMachines(long duration, TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {

		return this.admin.getGridServiceAgents().getAgents();
	}

	protected InstallationDetails createInstallationDetails(final Cloud2 cloud, final MachineDetails md) throws FileNotFoundException {
		final InstallationDetails details = new InstallationDetails();

		details.setLocalDir(cloud.getProvider().getLocalDirectory());
		details.setRemoteDir(cloud.getProvider().getRemoteDirectory());
		details.setManagementOnlyFiles(cloud.getProvider().getManagementOnlyFiles());
		details.setZones(StringUtils.join(cloud.getProvider().getZones().toArray(new String[0]), ",", 0, cloud
				.getProvider().getZones().size()));

		if (cloud.getUser().getKeyFile() != null) {
			logger.info("Key file has been specified in cloud configuration: " + cloud.getUser().getKeyFile());
			File keyFile = new File(cloud.getProvider().getLocalDirectory(), cloud.getUser().getKeyFile());
			if (keyFile.exists()) {
				details.setKeyFile(keyFile.getAbsolutePath());
				logger.info("Using key file: " + keyFile);
			} else {
				throw new FileNotFoundException(
						"Could not find key file matching specified cloud configuration key file: "
								+ cloud.getUser().getKeyFile() + ". Tried: " + keyFile + " but file does not exist");
			}

		}
	
		details.setPrivateIp(md.getPrivateAddress());
		details.setPublicIp(md.getPublicAddress());

		details.setLocator(this.lookupLocatorsString);
		details.setLus(false);
		details.setCloudifyUrl(cloud.getProvider().getCloudifyUrl());
		details.setConnectedToPrivateIp(cloud.getConfiguration().isConnectToPrivateIp());
		details.setAdmin(this.admin);

		details.setUsername(md.getRemoteUsername());
		details.setPassword(md.getRemotePassword());
		logger.info("Created new Installation Details: " + details);
		return details;

	}

	@Override
	public GridServiceAgent startMachine(long duration, TimeUnit unit) throws ElasticMachineProvisioningException,
			InterruptedException, TimeoutException {

		// calculate timeout
		final long end = System.currentTimeMillis() + unit.toMillis(duration);

		// provision the machine
		MachineDetails machineDetails = provisionMachine(duration, unit);

		// install gigaspaces and start agent
		installAndStartAgent(machineDetails, end);

		// TODO - finish this section - support picking up existing
		// installations and agent. - i.e. machineDetails.cloudifyInstalled ==
		// true

		// which IP should be used in the cluster
		String machineIp = null;
		if (machineDetails.isUsePrivateAddress()) {
			machineIp = machineDetails.getPrivateAddress();
		} else {
			machineIp = machineDetails.getPublicAddress();
		}

		// wait for GSA to become available
		GridServiceAgent gsa = waitForGsa(machineIp, DEFAULT_GSA_LOOKUP_TIMEOUT_SECONDS);
		if (gsa == null) {
			// GSA did not start correctly or on time - shutdown the machine
			handleGSANotFound(machineIp);

		}
		return gsa;

	}

	private void handleGSANotFound(String machineIp) throws ElasticMachineProvisioningException {
		logger.severe("Failed to look up Grid Service Agent on machine with IP: " + machineIp
				+ ". Attempting to shut it down !");
		boolean shutdownResult = false;
		try {
			shutdownResult = this.cloudifyProvisioning.stopMachine(machineIp);
		} catch (CloudProvisioningException e) {
			logger.log(Level.SEVERE, "During scale out, a new machine with IP: " + machineIp
					+ " was provisioned, but a Cloudfiy Agent was not available on it. "
					+ "Attempt to shut down the machine has failed with the following error: " + e.getMessage()
					+ ". Contact Cloud Adminisrator to manuall shut down this machine", e);
		}

		if (!shutdownResult) {
			final String msg = "During scale out, a new machine with IP: " + machineIp
					+ " was provisioned, but a Cloudfiy Agent was not available on it. "
					+ "Attempt to shut down the machine has also failed. This machine may be leaking, "
					+ "and should be shut down manually by the cloud administrator";
			logger.severe(msg);
			throw new ElasticMachineProvisioningException(msg);
		} else {
			final String msg = "During scale out, a new machine with IP: " + machineIp
					+ " was provisioned, but a Cloudfiy Agent was not available on it. "
					+ "Attempt to shut down the machine was successful. "
					+ "A new provisioning attempt will be attempted automatically. "
					+ "If the problem repeats, contact your cloud administrator";
			logger.severe(msg);
			throw new ElasticMachineProvisioningException(msg);

		}
	}

	private void installAndStartAgent(MachineDetails machineDetails, long end) throws TimeoutException,
			InterruptedException, ElasticMachineProvisioningException {
		final AgentlessInstaller installer = new AgentlessInstaller();

		InstallationDetails installationDetails;
		try {
			installationDetails = createInstallationDetails(cloud, machineDetails);
		} catch (FileNotFoundException e) {
			throw new ElasticMachineProvisioningException("Failed to create installation details for agent: " + e.getMessage(), e);
		}
		
		logger.info("Starting agentless installation process on started machine with installation details: "
				+ installationDetails);
		// Update the logging level of jsch used by the AgentlessInstaller
		Logger.getLogger(AgentlessInstaller.SSH_LOGGER_NAME).setLevel(
				Level.parse(cloud.getProvider().getSshLoggingLevel()));

		// Execute agentless installation on the remote machine
		try {
			installer.installOnMachineWithIP(installationDetails, remainingTimeTill(end), TimeUnit.MILLISECONDS);
		} catch (InstallerException e) {
			throw new ElasticMachineProvisioningException(
					"Failed to install Cloudify Agent on newly provisioned machine: " + e.getMessage(), e);
		}
	}

	private long remainingTimeTill(long end) throws TimeoutException {
		long remaining = end - System.currentTimeMillis();
		if (remaining <= 0) {
			throw new TimeoutException("Passed target end time " + new Date(end));
		}
		return remaining;
	}

	private MachineDetails provisionMachine(long duration, TimeUnit unit) throws TimeoutException,
			ElasticMachineProvisioningException {
		MachineDetails machineDetails;
		try {
			// delegate provisioning to the cloud driver implementation
			machineDetails = cloudifyProvisioning.startMachine(duration, unit);
		} catch (CloudProvisioningException e) {
			throw new ElasticMachineProvisioningException("Failed to start machine: " + e.getMessage());
		}
		if (machineDetails == null) {
			throw new IllegalStateException("Provisioning provider: " + cloudifyProvisioning.getClass().getName()
					+ " returned a null when calling startMachine");
		}

		logger.info("New machine was provisioned. Machine details: " + machineDetails);
		return machineDetails;
	}

	private GridServiceAgent waitForGsa(String machineIp, int timeoutInSeconds) throws InterruptedException {

		long endTime = System.currentTimeMillis() + (timeoutInSeconds * 1000);

		while (System.currentTimeMillis() < endTime) {
			GridServiceAgent gsa = admin.getGridServiceAgents().getHostAddress().get(machineIp);
			if (gsa != null) {
				return gsa;
			}

			gsa = admin.getGridServiceAgents().getHostNames().get(machineIp);
			if (gsa != null) {
				return gsa;
			}

			Thread.sleep(1000);

		}
		return null;

	}

	@Override
	public CapacityRequirements getCapacityOfSingleMachine() {
		CloudTemplate template = cloud.getTemplates().get(this.cloudTemplate);
		CapacityRequirements capacityRequirements = new CapacityRequirements(new MemoryCapacityRequirement(
				(long) template.getMachineMemoryMB()), new CpuCapacityRequirement(template.getNumberOfCores()));
		logger.info("Capacity requirements for a single machine are: " + capacityRequirements);
		return capacityRequirements;

	}

	@Override
	public boolean stopMachine(GridServiceAgent agent, long duration, TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {
		final String machineIp = agent.getMachine().getHostAddress();
		try {
			final boolean shutdownResult = this.cloudifyProvisioning.stopMachine(machineIp);
			if (!shutdownResult) {
				logger.severe("Attempt to shutdown machine with IP: " + machineIp + " for agent with UID: "
						+ agent.getUid() + " has failed");
			}
			return shutdownResult;

		} catch (CloudProvisioningException e) {
			throw new ElasticMachineProvisioningException("Attempt to shutdown machine with IP: " + machineIp
					+ " for agent with UID: " + agent.getUid() + " has failed with error: " + e.getMessage(), e);
		}

	}

	@Override
	public ElasticMachineProvisioningConfig getConfig() {
		return this.config;
	}

	// //////////////////////////////////
	// OpenSpaces Bean Implementation //
	// //////////////////////////////////
	@Override
	public void setAdmin(Admin admin) {
		this.admin = admin;

	}

	@Override
	public void setProperties(final Map<String, String> properties) {
		this.properties = properties;
		this.config = new CloudMachineProvisioningConfig(properties);

	}

	@Override
	public Map<String, String> getProperties() {
		return this.properties;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final String cloudContents = properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_CONFIGURATION);
		if(cloudContents == null) {
			throw new IllegalArgumentException("Cloud configuration was not set!");
		}
	
		// TODO - remove this printout - it includes the API key!
		logger.info("Cloud contents passed in elastic properties: " + cloudContents);
		try {
			this.cloud = ServiceReader.readCloud(cloudContents);
			this.cloudTemplate = properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME);

			if(this.cloudTemplate == null) {
				throw new IllegalArgumentException("Cloud template was not set!");
			}
			
			
			// This code runs on the ESM in the remote machine, 
			// so set the local directory to the value of the remote directory
			cloud.getProvider().setLocalDirectory(cloud.getProvider().getRemoteDirectory());
			
			// load the provisioning class and set it up
			try {
				this.cloudifyProvisioning = (CloudifyProvisioning) Class.forName(
						this.cloud.getConfiguration().getClassName()).newInstance();
				this.cloudifyProvisioning.setConfig(cloud, cloudTemplate, false);

			} catch (Exception e) {
				throw new IllegalArgumentException("Failed to load provisioning class from cloud: " + this.cloud);
			}

			this.lookupLocatorsString = createLocatorsString();

			logger.info("Locators string used for new instances will be: " + this.lookupLocatorsString);

		} catch (DSLException e) {
			logger.severe("Could not parse the provided cloud configuration: " + cloudContents + ": " + e.getMessage());
			throw new IllegalArgumentException("Could not parse the prvided cloud configuration: " + cloudContents
					+ ": " + e.getMessage());
		}

	}

	private String createLocatorsString() {
		LookupLocator[] locators = this.admin.getLocators();
		StringBuilder sb = new StringBuilder();
		for (LookupLocator lookupLocator : locators) {
			sb.append(lookupLocator.getHost()).append(":").append(lookupLocator.getPort()).append(",");
		}
		if (sb.toString().length() > 0){
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	@Override
	public void destroy() throws Exception {
		this.cloudifyProvisioning.close();
	}

}
