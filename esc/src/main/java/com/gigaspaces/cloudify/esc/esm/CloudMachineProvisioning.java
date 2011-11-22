package com.gigaspaces.cloudify.esc.esm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jclouds.compute.domain.NodeMetadata;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.events.GridServiceAgentAddedEventListener;
import org.openspaces.admin.machine.Machine;
import org.openspaces.core.bean.Bean;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.CpuCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioning;
import org.openspaces.grid.gsm.machines.plugins.ElasticMachineProvisioningException;

import com.gigaspaces.cloudify.esc.installer.AgentlessInstaller;
import com.gigaspaces.cloudify.esc.installer.InstallationDetails;
import com.gigaspaces.cloudify.esc.installer.InstallerException;
import com.gigaspaces.cloudify.esc.jclouds.JCloudsDeployer;
import com.gigaspaces.internal.utils.StringUtils;
import com.j_spaces.kernel.Environment;

/*************
 * ESM Provisioning implementation for cloud deployment, based on jclouds.
 * 
 * @author barakme
 * 
 */
public class CloudMachineProvisioning implements ElasticMachineProvisioning, Bean {

	protected Logger logger = Logger.getLogger(this.getClass().getName());
	
	private static final int MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT = 120000;
	private JCloudsDeployer deployer;
	private String machineNamePrefix;
	private static final int ESM_NAME_RANDOM_LIMIT = 1000;
	private CloudMachineProvisioningConfig config;

	public CloudMachineProvisioning() {
		logger.info("CloudMachineProvisioning instance has been constructed");
	}

	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

	public void setProperties(final Map<String, String> properties)  {
		logger.info("Setting Properties of Cloud Machine Provisioning: " + properties.toString());
		this.config = new CloudMachineProvisioningConfig(properties);
		
		// when in the cloud, remote dir == local dir
		logger.fine("Setting local dir to be the same as remote dir in cloud configuration");
		config.setLocalDirectory(config.getRemoteDirectory());
		
		try {
			this.deployer = new JCloudsDeployer(config.getProvider(), config.getUser(), config.getApiKey());
			this.deployer.setImageId(config.getImageId());
			this.deployer.setMinRamMegabytes((int) config.getMachineMemoryMB());
	        this.deployer.setHardwareId(config.getHardwareId());
	        this.deployer.setLocationId(config.getLocationId());
	        this.deployer.setSecurityGroup(config.getSecurityGroup());
			this.deployer.setKeyPair(config.getKeyPair());

		} catch (final IOException e) {
			throw new IllegalStateException("Failed to create cloud Deployer", e);
		}

		final String tmpGridName = this.config.getGridName();// properties.get("gridName");
		if (tmpGridName == null) {
			logger.warning("Property gridName is null! Proper clean up of handler will not be possible!");
		} else {
			this.gridName = tmpGridName;
		}

		String prefix = config.getMachineNamePrefix();// properties.get("machineNamePrefix");

		if ((prefix == null) || (prefix.length() == 0)) {
			prefix = "gs_esm_gsa_";
		}

		// attach a random number to the prefix to prevent collisions
		this.machineNamePrefix = prefix + new Random().nextInt(ESM_NAME_RANDOM_LIMIT) + "_";

		logger.info("Creating cloud jclouds context deployer with user: " + config.getUser());
	}

	public String getAbsolutePathValidateExists(String relativeFileName, File localDir) throws FileNotFoundException {

		if (relativeFileName == null) {
			return null;
		}
		
		File absoluteFile = new File(relativeFileName);
		
		if (!localDir.isAbsolute()) {
			logger.fine("Assuming " + localDir + " is in " + Environment.getHomeDirectory());
			localDir = new File(Environment.getHomeDirectory(),localDir.getPath());
		}

		if (!absoluteFile.isAbsolute()) {
			logger.fine("Assuming " + relativeFileName + " is in " + localDir.getPath());
			absoluteFile = new File(localDir, relativeFileName);
		}
				
		if (!absoluteFile.exists()) {
			logger.severe("Cound not find key file: " + absoluteFile.getAbsolutePath());
			throw new FileNotFoundException("Could not find key file: " + absoluteFile.getAbsolutePath());
		}
		return absoluteFile.getAbsolutePath();
	}

	public Map<String, String> getProperties() {
		return this.config.getProperties();
	}

	public void afterPropertiesSet() throws Exception {
		
		config.setKeyFile(getAbsolutePathValidateExists(config.getKeyFile(),new File(config.getLocalDirectory())));
		
		this.myIp = getMyIP(admin);
		this.installer = new AgentlessInstaller();

		logger.info("Initialized CloudMachineProvisioning with config");
	}

	public void destroy() throws Exception {
		// Clean up admin
		logger.info("Cloud Machine Provisioning is shutting down");
		if (this.deployer != null) {
			logger.info("Shutting down JClouds Context");
			this.deployer.close();
			this.deployer = null;
		}
		// admin.close();
	}

	public GridServiceAgent startMachine(final long timeout, final TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {
		
		if (timeout < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}
		long end = System.currentTimeMillis() + unit.toMillis(timeout);
		
		logger.info("CloudMachineProvisioning: startMachine");
		InstallationDetails details = createInstallationDetails();
		CloudScaleOutTask cloudScaleOutTask = null;
        try {
        	cloudScaleOutTask = new CloudScaleOutTask(installer, details, machineNamePrefix, deployer);
        } catch (FileNotFoundException e) {
            throw new ElasticMachineProvisioningException("", e);
        }
		GridServiceAgent agent =null;
		try 
		{
			
			logger.info("Starting server");
			
			InstallationDetails[] instDetails = cloudScaleOutTask.startServers(1, timeout, unit);
			details = instDetails[0];
			final AgentlessInstaller installer = new AgentlessInstaller();
			
			logger.info("installing on new machine");
			
			installer.installOnMachineWithIP(details, remainingTimeTill(end), TimeUnit.MILLISECONDS);
			
			logger.info("waiting for agent");
			
			agent = CloudMachineProvisioning.waitForGridServiceAgent(admin, details.getPrivateIp(), remainingTimeTill(end), TimeUnit.MILLISECONDS);
			
		}
		catch (InstallerException e) {
			if (details.getPrivateIp() != null)
				try {
					stopMachine(details.getPrivateIp(),"",timeout,unit);
				}
			catch (Exception ex) {
				//TODO: Have a standalone process that collects zombies and terminates them based on descrepency 
				// between agent list and ec2 machines list (that are running for at least 5 minutes)
				logger.log(Level.WARNING,"Error stopping machine " + details.getPrivateIp() + " could be zombie. Stop manually",ex);
			}
			throw new ElasticMachineProvisioningException("error starting new machine",e);
		}
				
		return agent;
	}

	private final Map<String, Long> stoppingMachines = new ConcurrentHashMap<String, Long>();

	public boolean stopMachine(final GridServiceAgent agent, final long duration, final TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {

		final String ip = agent.getMachine().getHostAddress();
		return stopMachine(ip, agent.getMachine().getUid() , duration, unit);
	}
	
	private boolean stopMachine(final String ip, final String agentUid, final long duration, final TimeUnit unit)
		throws ElasticMachineProvisioningException, InterruptedException, TimeoutException {
		
		logger.fine("Check that we are not shutting down LUS or ESM - lusIP  in locators: " + config.getLocator());
		if (config.getLocator() != null && config.getLocator().contains(ip)) {
			logger.info("Recieved scale in request for LUS/ESM server. Ignoring.");
			return false;
		}

		// ignore duplicate shutdown requests for same machine
		final Long previousRequest = stoppingMachines.get(ip);
		if ((previousRequest != null) &&
				(System.currentTimeMillis() - previousRequest < MULTIPLE_SHUTDOWN_REQUEST_IGNORE_TIMEOUT)) {
			return true;
		}
		stoppingMachines.put(ip, System.currentTimeMillis());
		logger.info("Scale IN -- " + ip + " --");

		logger.fine("Looking Up Cloud server with this IP");
		final NodeMetadata server = getServerWithIP(ip);
		if (server != null) {
			logger.info("Found server: " + server.getId() + ". Shutting it down");
			deployer.shutdownMachine(server.getId());

			logger.info("Server: " + server.getId() + " shutdown has started.");
			return true;

		} else {
			logger.log(Level.SEVERE, "Recieved scale in request for machine with ip " + ip + " " + "and id "
					+ agentUid+ " but this IP " + "could not be found in the Cloud server list");
		}
		return false;
	}

	private NodeMetadata getServerWithIP(final String ip) {
		return deployer.getServerWithIP(ip);
	}

	private static GridServiceAgent waitForGridServiceAgent(final Admin admin, final String hostAddress,
			final long timeout, final TimeUnit unit)
			throws InterruptedException, TimeoutException {

		if (timeout < 0) {
			throw new TimeoutException("Timeout waiting for grid service agent.");
		}
		
		final AtomicReference<GridServiceAgent> gsaRef = new AtomicReference<GridServiceAgent>(null);
		final CountDownLatch gridServiceAgentAddedLatch = new CountDownLatch(1);

		admin.getGridServiceAgents().getGridServiceAgentAdded().add(new GridServiceAgentAddedEventListener() {

			public void gridServiceAgentAdded(final GridServiceAgent gridServiceAgent) {
				if (gridServiceAgent.getMachine().getHostAddress().equals(hostAddress)) {
					gsaRef.set(gridServiceAgent);
					gridServiceAgentAddedLatch.countDown();
				}
			}
		});

		gridServiceAgentAddedLatch.await(unit.toMillis(timeout), TimeUnit.MILLISECONDS);
		final GridServiceAgent gsa = gsaRef.get();

		if (gsa == null) {
			throw new TimeoutException("Timeout waiting for grid service agent.");
		}

		return gsa;
	}

	public boolean isStartMachineSupported() {
		return true;
	}

	public GridServiceAgent[] getDiscoveredMachines(final long duration, final TimeUnit unit)
			throws ElasticMachineProvisioningException, InterruptedException,
			TimeoutException {

		// TODO: Replace mock with JClouds implementation
		return admin.getGridServiceAgents().getAgents();

		// Map<String, GridServiceAgent> addresses =
		// admin.getGridServiceAgents().getHostAddress();
		// Set<? extends ComputeMetadata> listNodes =
		// deployer.context.getComputeService().listNodes();
		// for (NodeMetaData metadata : listNodes) {
		// for (ip : metadata.getPrivateAddresses()) {
		// if(addresses.get(ip) != null) {
		// agents.add(addresses.get(ip));
		// break;
		// }
		// }
		// }
		// return agents.toArray();
	}

	public CloudMachineProvisioningConfig getConfig() {
		return config;
	}

	public CapacityRequirements getCapacityOfSingleMachine() {
		return new CapacityRequirements(
				new MemoryCapacityRequirement(config.getMachineMemoryMB()),
				new CpuCapacityRequirement(config.getMinimumNumberOfCpuCoresPerMachine()));
	}
	
	// Gigaspaces properties
	protected Admin admin;

	protected Object gridName;
	// Agentless installer configuration
	protected AgentlessInstaller installer;
	
	protected String myIp;

	public boolean accept(final Machine arg0) {
		logger.fine("Accept: " + arg0.getHostAddress());
		return true;
	}

	protected InstallationDetails createInstallationDetails() {
		final InstallationDetails details = new InstallationDetails();

		details.setLocalDir(config.getLocalDirectory());
		details.setRemoteDir(config.getRemoteDirectory());
		details.setManagementOnlyFiles(config.getManagementOnlyFiles());
		details.setZones(StringUtils.join(config.getGridServiceAgentZones(), 
				",", 0, config.getGridServiceAgentZones().length));
		
		details.setKeyFile(config.getKeyFile());

		details.setPrivateIp(null);

		logger.info("Setting LOCATOR for new installation details to: " + config.getLocator());
		details.setLocator(config.getLocator()); // TODO get actual locators (which could be more than 1 management machine)
		details.setLus(false);
		details.setCloudifyUrl(config.getCloudifyUrl());
		details.setConnectedToPrivateIp(true);
		details.setAdmin(this.admin);
		
		logger.info("Created new Installation Details: " + details);
		return details;
	}

	protected String getMyIP(final Admin admin) {

		logger.info("LOOKING UP NIC ADDRESS!");
		final String nic = System.getenv("NIC_ADDR");
		if (nic != null) {
			logger.info("ESM IP found in environment: " + nic);
			return nic;
		}
		// Create a new admin as the provided one is single threaded
		// and blocking calls are not allowed on it.
		final Admin tempAdmin = new AdminFactory().addGroup(admin.getGroups()[0])
				.addLocator(admin.getLocators()[0].toString()).createAdmin();

		try {
			final ElasticServiceManager esm = tempAdmin
					.getElasticServiceManagers().waitForAtLeastOne(30,
							TimeUnit.SECONDS);

			if (esm == null) {
				throw new IllegalStateException(
						"Could not find ESM in admin API. This should not be possible. "
								+ "Check Admin API settings!" + " Locator = "
								+ Arrays.toString(tempAdmin.getLocators())
								+ " Groups = "
								+ Arrays.toString(tempAdmin.getGroups()));
			}

			final String ip = esm.getMachine().getHostAddress();
			logger.info("ESM Scale Handler is running on IP: " + ip);
			return ip;
		} finally {
			if (tempAdmin != null) {
				tempAdmin.close();
			}
		}
	}

	private long remainingTimeTill(long end) throws TimeoutException {
		 long remaining = end - System.currentTimeMillis();
		 if (remaining <= 0) {
			 throw new TimeoutException("Passed target end time " + new Date(end));
		 }
		 return remaining;
	}
}
