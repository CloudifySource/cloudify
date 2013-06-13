/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.jclouds;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.FileTransferModes;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.CustomServiceDataAware;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ManagementLocator;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.cloudifysource.esc.util.JCloudsUtils;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ec2.EC2AsyncClient;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.KeyPair;
import org.jclouds.ec2.services.KeyPairClient;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.jclouds.rest.RestContext;

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
public class DefaultProvisioningDriver extends BaseProvisioningDriver implements
		ProvisioningDriver, ProvisioningDriverClassContextAware,
		CustomServiceDataAware, ManagementLocator {

	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	private static final String PUBLIC_IP_REGEX = "org.cloudifysource.default-cloud-driver.public-ip-regex";
	private static final String PUBLIC_IP_CIDR = "org.cloudifysource.default-cloud-driver.public-ip-cidr";
	private static final String PRIVATE_IP_REGEX = "org.cloudifysource.default-cloud-driver.private-ip-regex";
	private static final String PRIVATE_IP_CIDR = "org.cloudifysource.default-cloud-driver.private-ip-cidr";
	private static final int CLOUD_NODE_STATE_POLLING_INTERVAL = 2000;
	private static final String DEFAULT_EC2_WINDOWS_USERNAME = "Administrator";
	private static final String EC2_API = "aws-ec2";
	private static final String VCLOUD = "vcloud";
	private static final String OPENSTACK_API = "openstack-nova";
	private static final String CLOUDSTACK = "cloudstack";
	private static final String ENDPOINT_OVERRIDE = "jclouds.endpoint";
	private static final String CLOUDS_FOLDER_PATH = Environment.getHomeDirectory() + "clouds";


	// TODO: should it be volatile?
	private static ResourceBundle defaultProvisioningDriverMessageBundle;

	private String cloudFolder;
	private String groovyFile;
	private String propertiesFile;	
	
	private JCloudsDeployer deployer;
	private SubnetInfo privateSubnetInfo;
	private Pattern privateIpPattern;
	private SubnetInfo publicSubnetInfo;
	private Pattern publicIpPattern;

	@Override
	protected void initDeployer(final Cloud cloud) {
		if (this.deployer != null) {
			return;
		}

		try {
			// TODO - jcloudsUniqueId should be unique per cloud configuration.
			// TODO - The deployer object should be reusable across templates.
			// The current API is not appropriate.
			// TODO - key should be based on entire cloud configuraion!
			// TODO - this shared context only works if we have reference
			// counting, to check when this item is
			// no longer there. Otherwise, either this context will leak, or it
			// will be shutdown by the first
			// service to by undeployed.
			this.deployer = createDeployer(cloud);
			// (JCloudsDeployer)
			// context.getOrCreate("UNIQUE_JCLOUDS_DEPLOYER_ID_" +
			// this.cloudTemplateName,
			// new Callable<Object>() {
			//
			// @Override
			// public Object call()
			// throws Exception {
			// return createDeplyer(cloud);
			// }
			// });

			// Initialize IP CIDR blocks and Regex
			initIPFilters(cloud);

		} catch (final Exception e) {
			publishEvent("connection_to_cloud_api_failed", cloud.getProvider()
					.getProvider());
			throw new IllegalStateException("Failed to create cloud Deployer",
					e);
		}
	}

	private void initIPFilters(final Cloud cloud) {
		final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(
				cloudTemplateName);

		final String privateCidr =
				(String) template.getCustom().get(PRIVATE_IP_CIDR);
		if (!StringUtils.isBlank(privateCidr)) {
			this.privateSubnetInfo = new SubnetUtils(privateCidr).getInfo();
		}

		final String privateRegex =
				(String) template.getCustom().get(PRIVATE_IP_REGEX);
		if (!StringUtils.isBlank(privateRegex)) {
			this.privateIpPattern = Pattern.compile(privateRegex);
		}

		final String publicCidr =
				(String) template.getCustom().get(PUBLIC_IP_CIDR);
		if (!StringUtils.isBlank(publicCidr)) {
			this.publicSubnetInfo = new SubnetUtils(publicCidr).getInfo();
		}

		final String publicRegex =
				(String) template.getCustom().get(PUBLIC_IP_REGEX);
		if (!StringUtils.isBlank(publicRegex)) {
			this.publicIpPattern = Pattern.compile(publicRegex);
		}

	}

	@Override
	public MachineDetails startMachine(final String locationId, final long timeout,
			final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {

		logger.fine(this.getClass().getName()
				+ ": startMachine, management mode: " + management);
		final long end = System.currentTimeMillis() + unit.toMillis(timeout);

		if (System.currentTimeMillis() > end) {
			throw new TimeoutException("Starting a new machine timed out");
		}

		try {
			final String groupName = createNewServerName();
			logger.fine("Starting a new cloud server with group: " + groupName);
			final MachineDetails md = createServer(end, groupName, locationId);
			return md;
		} catch (final Exception e) {
			throw new CloudProvisioningException(
					"Failed to start cloud machine", e);
		}
	}

	@Override
	protected MachineDetails createServer(final String serverName, final long endTime,
			final ComputeTemplate template) throws CloudProvisioningException,
			TimeoutException {
		return createServer(endTime, serverName, null);
	}

	private MachineDetails createServer(final long end, final String groupName,
			final String locationIdOverride) throws CloudProvisioningException {

		final ComputeTemplate cloudTemplate = this.cloud.getCloudCompute().getTemplates().get(
				this.cloudTemplateName);
		String locationId;
		if (locationIdOverride == null) {
			locationId = cloudTemplate.getLocationId();
		} else {
			locationId = locationIdOverride;
		}

		NodeMetadata node;
		final MachineDetails machineDetails;

		try {
			logger.fine("Cloudify Deployer is creating a new server with tag: "
					+ groupName + ". This may take a few minutes");
			node = deployer.createServer(groupName, locationId);
		} catch (final InstallerException e) {
			throw new CloudProvisioningException(
					"Failed to create cloud server", e);
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

			final FileTransferModes fileTransfer = cloudTemplate
					.getFileTransfer();

			if (this.cloud.getProvider().getProvider().equals("aws-ec2")
					&& fileTransfer == FileTransferModes.CIFS) {
				// Special password handling for windows on EC2
				if (machineDetails.getRemotePassword() == null) {
					// The template did not specify a password, so we must be
					// using the aws windows password mechanism.
					handleEC2WindowsCredentials(end, node, machineDetails,
							cloudTemplate);
				}

			} else {
				// Credentials required special handling.
				handleServerCredentials(machineDetails, cloudTemplate);
			}

		} catch (final Exception e) {
			// catch any exception - to prevent a cloud machine leaking.
			logger.log(
					Level.SEVERE,
					"Cloud machine was started but an error occured during initialization. Shutting down machine",
					e);
			deployer.shutdownMachine(nodeId);
			throw new CloudProvisioningException(e);
		}

		return machineDetails;
	}

	private void handleEC2WindowsCredentials(final long end,
			final NodeMetadata node, final MachineDetails machineDetails,
			final ComputeTemplate cloudTemplate) throws FileNotFoundException,
			InterruptedException, TimeoutException, CloudProvisioningException {
		File pemFile = null;

		if (this.management) {
			final File localDirectory = new File(cloudTemplate.getAbsoluteUploadDir());

			pemFile = new File(localDirectory, cloudTemplate.getKeyFile());
		} else {
			final String localDirectoryName = cloudTemplate.getLocalDirectory();
			logger.fine("local dir name is: " + localDirectoryName);
			final File localDirectory = new File(localDirectoryName);

			pemFile = new File(localDirectory, cloudTemplate.getKeyFile());
		}

		if (!pemFile.exists()) {
			logger.severe("Could not find pem file: " + pemFile);
			throw new FileNotFoundException("Could not find key file: "
					+ pemFile);
		}

		String password;
		if (cloudTemplate.getPassword() == null) {
			// get the password using Amazon API
			this.publishEvent("waiting_for_ec2_windows_password", node.getId());

			final LoginCredentials credentials = new EC2WindowsPasswordHandler()
					.getPassword(node, this.deployer.getContext(), end, pemFile);
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
		machineDetails.setRemoteExecutionMode(cloudTemplate
				.getRemoteExecution());
	}

	private NodeMetadata waitForNodeToBecomeReady(final String id,
			final long end) throws CloudProvisioningException,
			InterruptedException, TimeoutException {
		NodeMetadata node = null;
		while (System.currentTimeMillis() < end) {
			node = deployer.getServerByID(id);

			if (node == null) {
				logger.fine("Server Status (" + id
						+ ") Not Found, please wait...");
				Thread.sleep(CLOUD_NODE_STATE_POLLING_INTERVAL);
				break;
			} else {
				switch (node.getStatus()) {
				case RUNNING:
					return node;
				case PENDING:
					logger.fine("Server Status (" + id
							+ ") still PENDING, please wait...");
					Thread.sleep(CLOUD_NODE_STATE_POLLING_INTERVAL);
					break;
				case TERMINATED:
				case ERROR:
				case UNRECOGNIZED:
				case SUSPENDED:
				default:
					throw new CloudProvisioningException(
							"Failed to allocate server - Cloud reported node in "
									+ node.getStatus().toString()
									+ " state. Node details: " + node);
				}
			}

		}
		throw new TimeoutException("Node failed to reach RUNNING mode in time");
	}

	/*********
	 * Looks for a free server name by appending a counter to the pre-calculated server name prefix. If the max counter
	 * value is reached, code will loop back to 0, so that previously used server names will be reused.
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
			// counter = (counter + 1) % MAX_SERVERS_LIMIT;
			++attempts;
			serverName = serverNamePrefix + counter.incrementAndGet();
			// verifying this server name is not already used
			final NodeMetadata existingNode = deployer
					.getServerByID(serverName);
			if (existingNode == null) {
				foundFreeName = true;
				break;
			}
		}

		if (!foundFreeName) {
			throw new CloudProvisioningException(
					"Number of servers has exceeded allowed server limit ("
							+ MAX_SERVERS_LIMIT + ")");
		}

		return serverName;
	}

	@Override
	public MachineDetails[] startManagementMachines(final long duration,
			final TimeUnit unit) throws TimeoutException,
			CloudProvisioningException {

		if (duration < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}
		final long endTime = System.currentTimeMillis()
				+ unit.toMillis(duration);

		logger.fine("DefaultCloudProvisioning: startMachine - management == "
				+ management);

		final String managementMachinePrefix = this.cloud.getProvider().getManagementGroup();
		if (StringUtils.isBlank(managementMachinePrefix)) {
			throw new CloudProvisioningException(
					"The management group name is missing - can't locate existing servers!");
		}

		// first check if management already exists
		final MachineDetails[] existingManagementServers = getExistingManagementServers();
		if (existingManagementServers.length > 0) {
			final String serverDescriptions =
					createExistingServersDescription(managementMachinePrefix, existingManagementServers);
			throw new CloudProvisioningException("Found existing servers matching group "
					+ managementMachinePrefix + ": " + serverDescriptions);

		}

		// launch the management machines
		publishEvent(EVENT_ATTEMPT_START_MGMT_VMS);
		final int numberOfManagementMachines = this.cloud.getProvider()
				.getNumberOfManagementMachines();
		final MachineDetails[] createdMachines = doStartManagementMachines(
				endTime, numberOfManagementMachines);
		publishEvent(EVENT_MGMT_VMS_STARTED);
		return createdMachines;
	}

	private String createExistingServersDescription(final String managementMachinePrefix,
			final MachineDetails[] existingManagementServers) {
		logger.info("Found existing servers matching the name: "
				+ managementMachinePrefix);
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (final MachineDetails machineDetails : existingManagementServers) {
			final String existingManagementServerDescription = createManagementServerDescription(machineDetails);
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append("[").append(existingManagementServerDescription).append("]");
		}
		final String serverDescriptions = sb.toString();
		return serverDescriptions;
	}

	private String createManagementServerDescription(final MachineDetails machineDetails) {
		final StringBuilder sb = new StringBuilder();
		sb.append("Machine ID: ").append(machineDetails.getMachineId());
		if (machineDetails.getPublicAddress() != null) {
			sb.append(", Public IP: ").append(machineDetails.getPublicAddress());
		}

		if (machineDetails.getPrivateAddress() != null) {
			sb.append(", Private IP: ").append(machineDetails.getPrivateAddress());
		}

		return sb.toString();
	}

	@Override
	public boolean stopMachine(final String serverIp, final long duration,
			final TimeUnit unit) throws CloudProvisioningException,
			TimeoutException, InterruptedException {

		boolean stopResult = false;

		logger.info("Stop Machine - machineIp: " + serverIp);

		logger.info("Looking up cloud server with IP: " + serverIp);
		final NodeMetadata server = deployer.getServerWithIP(serverIp);
		if (server != null) {
			logger.info("Found server: "
					+ server.getId()
					+ ". Shutting it down and waiting for shutdown to complete");
			deployer.shutdownMachineAndWait(server.getId(), unit, duration);
			logger.info("Server: " + server.getId()
					+ " shutdown has finished.");
			stopResult = true;
		} else {
			logger.log(
					Level.SEVERE,
					"Recieved scale in request for machine with ip "
							+ serverIp
							+ " but this IP could not be found in the Cloud server list");
			stopResult = false;
		}

		return stopResult;
	}

	@Override
	public void stopManagementMachines() throws TimeoutException,
			CloudProvisioningException {

		initDeployer(this.cloud);
		final MachineDetails[] managementServers = getExistingManagementServers();

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

	/*
	 * (non-Javadoc)
	 *
	 * @see org.cloudifysource.esc.driver.provisioning.jclouds.ManagementLocator#getExistingManagementServers()
	 */
	@Override
	public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
		final String managementMachinePrefix = this.serverNamePrefix;
		Set<? extends NodeMetadata> existingManagementServers = null;
		try {
			existingManagementServers = this.deployer
					.getServers(new Predicate<ComputeMetadata>() {

						@Override
						public boolean apply(final ComputeMetadata input) {
							final NodeMetadata node = (NodeMetadata) input;
							if (node.getGroup() == null) {
								return false;
							}
							// only running or pending nodes are interesting
							if (node.getStatus() == NodeMetadata.Status.RUNNING
									|| node.getStatus() == NodeMetadata.Status.PENDING) {
								return node
										.getGroup()
										.toLowerCase()
										.startsWith(
												managementMachinePrefix
														.toLowerCase());
							}
							return false;
						}
					});

		} catch (final Exception e) {
			throw new CloudProvisioningException("Failed to read existing management servers: " + e.getMessage(), e);
		}

		final MachineDetails[] result = new MachineDetails[existingManagementServers
				.size()];
		int i = 0;
		for (final NodeMetadata node : existingManagementServers) {
			result[i] = createMachineDetailsFromNode(node);
			// result[i].setAgentRunning(true);
			// result[i].setCloudifyInstalled(true);
			i++;

		}
		return result;
	}

	@Override
	protected void handleProvisioningFailure(
			final int numberOfManagementMachines, final int numberOfErrors,
			final Exception firstCreationException,
			final MachineDetails[] createdManagementMachines)
			throws CloudProvisioningException {
		logger.severe("Of the required " + numberOfManagementMachines
				+ " management machines, " + numberOfErrors
				+ " failed to start.");
		if (numberOfManagementMachines > numberOfErrors) {
			logger.severe("Shutting down the other managememnt machines");

			for (final MachineDetails machineDetails : createdManagementMachines) {
				if (machineDetails != null) {
					logger.severe("Shutting down machine: " + machineDetails);
					this.deployer
							.shutdownMachine(machineDetails.getMachineId());
				}
			}
		}

		throw new CloudProvisioningException(
				"One or more managememnt machines failed. The first encountered error was: "
						+ firstCreationException.getMessage(),
				firstCreationException);
	}

	private void populateIPs(final NodeMetadata node, final MachineDetails md, final ComputeTemplate template) {

		CloudAddressResolver resolver = new CloudAddressResolver();
		final String privateAddress =
				resolver.getAddress(node.getPrivateAddresses(), node.getPublicAddresses(), privateSubnetInfo,
						this.privateIpPattern);
		final String publicAddress =
				resolver.getAddress(node.getPublicAddresses(), node.getPrivateAddresses(), publicSubnetInfo,
						this.publicIpPattern);

		md.setPrivateAddress(privateAddress);
		md.setPublicAddress(publicAddress);

	}

	private MachineDetails createMachineDetailsFromNode(final NodeMetadata node) {
		final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(
				this.cloudTemplateName);

		final MachineDetails md = createMachineDetailsForTemplate(template);

		md.setCloudifyInstalled(false);
		md.setInstallationDirectory(null);
		md.setMachineId(node.getId());

		populateIPs(node, md, template);

		final String username = createMachineUsername(node, template);
		final String password = createMachinePassword(node, template);

		md.setRemoteUsername(username);
		md.setRemotePassword(password);

		// this will ensure that the availability zone is added to GSA that
		// starts on this machine.
		final String locationId = node.getLocation().getId();
		md.setLocationId(locationId);

		return md;
	}

	private String createMachineUsername(final NodeMetadata node,
			final ComputeTemplate template) {

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

	private String createMachinePassword(final NodeMetadata node,
			final ComputeTemplate template) {

		// Template configuration takes precedence.
		if (template.getPassword() != null) {
			return template.getPassword();
		}

		// Check if node returned a username - some clouds support this
		// (Rackspace, for instance)
		if (node.getCredentials() != null
				&& node.getCredentials().getOptionalPassword() != null) {
			if (node.getCredentials().getOptionalPassword().isPresent()) {
				return node.getCredentials().getPassword();
			}
		}

		return null;
	}

	@Override
	public void close() {
		if (deployer != null) {
			deployer.close();
		}
	}

	private JCloudsDeployer createDeployer(final Cloud cloud)
			throws IOException {
		logger.fine("Creating JClouds context deployer with user: "
				+ cloud.getUser().getUser());
		final ComputeTemplate cloudTemplate = cloud.getCloudCompute().getTemplates().get(
				cloudTemplateName);

		logger.fine("Cloud Template: " + cloudTemplateName + ". Details: "
				+ cloudTemplate);
		final Properties props = new Properties();
		props.putAll(cloudTemplate.getOverrides());

		deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(),
				cloud.getUser().getApiKey(), props);

		deployer.setImageId(cloudTemplate.getImageId());
		deployer.setMinRamMegabytes(cloudTemplate.getMachineMemoryMB());
		deployer.setHardwareId(cloudTemplate.getHardwareId());
		deployer.setExtraOptions(cloudTemplate.getOptions());
		return deployer;
	}

	@Override
	public void setCustomDataFile(final File customDataFile) {
		logger.info("Received custom data file: " + customDataFile);
	}

	@Override
	public MachineDetails[] getExistingManagementServers(final ControllerDetails[] controllers)
			throws CloudProvisioningException, UnsupportedOperationException {
		throw new UnsupportedOperationException(
				"Locating management servers from file information is not supported in this cloud driver");
	}

	@Override
	public Object getComputeContext() {
		ComputeServiceContext computeContext = null;
		if (deployer != null) {
			computeContext = deployer.getContext();
		}

		return computeContext;
	}

	@Override
	public void validateCloudConfiguration(final ValidationContext validationContext)
			throws CloudProvisioningException {
		// 1. Provider/API name
		// 2. Authentication to the cloud
		// 3. Image IDs
		// 4. Hardware IDs
		// 5. Location IDs
		// 6. Security groups
		// 7. Key-pair names (TODO: finger-print check)
		// TODO : move the security groups to the Template section (instead of custom map),
		// it is now supported by jclouds.

		String providerName = cloud.getProvider().getProvider();
		cloudFolder = CLOUDS_FOLDER_PATH + FILE_SEPARATOR + cloud.getName();
		groovyFile = cloudFolder + FILE_SEPARATOR + cloud.getName() + "-cloud.groovy";
		propertiesFile = cloudFolder + FILE_SEPARATOR + cloud.getName() + "-cloud.properties";
		
		String apiId;
		boolean endpointRequired = false;

		try {
			validationContext.validationOngoingEvent(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE,
					getFormattedMessage("validating_provider_or_api_name", providerName));
			ProviderMetadata providerMetadata = Providers.withId(providerName);
			ApiMetadata apiMetadata = providerMetadata.getApiMetadata();
			apiId = apiMetadata.getId();
			validationContext.validationEventEnd(ValidationResultType.OK);
		} catch (NoSuchElementException e) {
			// there is no jclouds Provider by that name, this could be the name of an API used in a private cloud
			try {
				ApiMetadata apiMetadata = Apis.withId(providerName);
				apiId = apiMetadata.getId();
				endpointRequired = true;
				validationContext.validationEventEnd(ValidationResultType.OK);
			} catch (NoSuchElementException ex) {
				validationContext.validationEventEnd(ValidationResultType.ERROR);
				throw new CloudProvisioningException(getFormattedMessage("error_provider_or_api_name_validation",
						providerName, cloudFolder), ex);
			}
		}
		validateComputeTemplates(endpointRequired, apiId, validationContext);
	}
	

	private void validateComputeTemplates(final boolean endpointRequired, final String apiId,
			final ValidationContext validationContext) throws CloudProvisioningException {

		JCloudsDeployer deployer = null;
		String templateName = "";
		String imageId = "";
		String hardwareId = "";
		String locationId = "";

		try {
			validationContext.validationEvent(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE,
					getFormattedMessage("validating_all_templates"));
			for (Entry<String, ComputeTemplate> entry : cloud.getCloudCompute().getTemplates().entrySet()) {
				templateName = entry.getKey();
				validationContext.validationEvent(ValidationMessageType.GROUP_VALIDATION_MESSAGE,
						getFormattedMessage("validating_template", templateName));
				ComputeTemplate template = entry.getValue();
				String endpoint = getEndpoint(template);
				if (endpointRequired && StringUtils.isBlank(endpoint)) {
					throw new CloudProvisioningException("Endpoint not defined. Please add a \"jclouds.endpoint\""
							+ " entry in the template's overrides section");
				}

				try {
					validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
							getFormattedMessage("validating_cloud_credentials"));
					final Properties templateProps = new Properties();
					Map<String, Object> templateOverrides = template.getOverrides();
					templateProps.putAll(templateOverrides);
					logger.fine("Creating a new cloud deployer");
					deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(),
							cloud.getUser().getApiKey(), templateProps);
					validationContext.validationEventEnd(ValidationResultType.OK);
				} catch (IOException e) {
					closeDeployer(deployer);
					validationContext.validationEventEnd(ValidationResultType.ERROR);
					throw new CloudProvisioningException(getFormattedMessage("error_cloud_credentials_validation",
							groovyFile, propertiesFile));
				}

				imageId = template.getImageId();
				hardwareId = template.getHardwareId();
				locationId = template.getLocationId();

				deployer.setImageId(imageId);
				deployer.setHardwareId(hardwareId);
				deployer.setExtraOptions(template.getOptions());
				// TODO: check this memory validation
				// deployer.setMinRamMegabytes(template.getMachineMemoryMB());
				try {

					validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
							getFormattedMessage("validating_image_hardware_location_combination",
									imageId == null ? "" : imageId, hardwareId == null ? "" : hardwareId,
									locationId == null ? "" : locationId));
					// calling JCloudsDeployer.getTemplate effectively tests the above configuration through jclouds
					deployer.getTemplate(locationId);
					validationContext.validationEventEnd(ValidationResultType.OK);
				} catch (Exception ex) {
					validationContext.validationEventEnd(ValidationResultType.ERROR);
					throw new CloudProvisioningException(
							getFormattedMessage("error_image_hardware_location_combination_validation",
							imageId == null ? "" : imageId,
									hardwareId == null ? "" : hardwareId, locationId == null ? "" : locationId,
											groovyFile, propertiesFile), ex);
				}

				if (isKnownAPI(apiId)) {
					validateSecurityGroupsForTemplate(template, apiId, deployer.getContext(), validationContext);
					validateKeyPairForTemplate(template, apiId, deployer.getContext(), validationContext);
				}
				validationContext.validationOngoingEvent(ValidationMessageType.GROUP_VALIDATION_MESSAGE,
						getFormattedMessage("template_validated", templateName));
				validationContext.validationEventEnd(ValidationResultType.OK);
				closeDeployer(deployer);
			}
		} finally {
			closeDeployer(deployer);
		}
	}
	

	private void validateSecurityGroupsForTemplate(final ComputeTemplate template, final String apiId,
			final ComputeServiceContext computeServiceContext, final ValidationContext validationContext)
			throws CloudProvisioningException {

		String locationId = template.getLocationId();
		if (StringUtils.isBlank(locationId) && apiId.equalsIgnoreCase(OPENSTACK_API)) {
			locationId = getOpenstackLocationByHardwareId(template.getHardwareId());
		}

		if (locationId == null) {
			throw new CloudProvisioningException("locationId is missing");
		}

		Object securityGroupsObj = template.getOptions().get("securityGroupNames");
		if (securityGroupsObj == null) {
			securityGroupsObj = template.getOptions().get("securityGroups");
		}

		if (securityGroupsObj != null) {
			if (securityGroupsObj instanceof String[]) {
				String[] securityGroupsArr = (String[]) securityGroupsObj;

				if (securityGroupsArr.length > 0) {
					try {

						if (securityGroupsArr.length == 1) {
							validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
									getFormattedMessage("validating_security_group", securityGroupsArr[0]));
						} else {
							validationContext.validationOngoingEvent(
									ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
									getFormattedMessage("validating_security_groups",
											org.cloudifysource.esc.util.StringUtils.arrayToString(securityGroupsArr,
													", ")));
						}

						if (apiId.equalsIgnoreCase(EC2_API)) {
							RestContext<EC2Client, EC2AsyncClient> unwrapped = computeServiceContext.unwrap();
							validateEc2SecurityGroups(unwrapped.getApi(), locationId, securityGroupsArr);
						} else if (apiId.equalsIgnoreCase(OPENSTACK_API)) {
							RestContext<NovaApi, NovaAsyncApi> unwrapped = computeServiceContext.unwrap();
							validateOpenstackSecurityGroups(unwrapped.getApi(), locationId,
									securityGroupsArr);
						} else if (apiId.equalsIgnoreCase(CLOUDSTACK)) {
							/*
							 * RestContext<CloudStackClient, CloudStackAsyncClient> unwrapped =
							 * computeServiceContext.unwrap();
							 * validateCloudstackSecurityGroups(unwrapped.getApi().getSecurityGroupClient(),
							 * aggregateAllValues(securityGroupsByRegions));
							 */

						} else if (apiId.equalsIgnoreCase(VCLOUD)) {
							// security groups not supported
						} else {
							// api validations not supported yet
						}

						validationContext.validationEventEnd(ValidationResultType.OK);
					} catch (CloudProvisioningException ex) {
						validationContext.validationEventEnd(ValidationResultType.ERROR);
						throw ex;
					}
				}
			} else {
				// TODO : Validation not supported
			}
		}
	}
	

	private void validateKeyPairForTemplate(final ComputeTemplate template, final String apiId,
			final ComputeServiceContext computeServiceContext, final ValidationContext validationContext)
			throws CloudProvisioningException {

		String locationId = template.getLocationId();
		if (StringUtils.isBlank(locationId) && apiId.equalsIgnoreCase(OPENSTACK_API)) {
			locationId = getOpenstackLocationByHardwareId(template.getHardwareId());
		}

		if (StringUtils.isBlank(locationId)) {
			throw new CloudProvisioningException("locationId is missing");
		}

		Object keyPairObj = template.getOptions().get("keyPairName");
		if (keyPairObj == null) {
			keyPairObj = template.getOptions().get("keyPair");
		}

		if (keyPairObj != null) {
			if (!(keyPairObj instanceof String)) {
				throw new CloudProvisioningException("Invalid configuration: keyPair must be of type String");
			}

			String keyPairString = (String) keyPairObj;
			if (StringUtils.isNotBlank(keyPairString)) {
				try {
					validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
							getFormattedMessage("validating_key_pair", keyPairString));

					if (apiId.equalsIgnoreCase(EC2_API)) {
						validateEC2KeyPair(computeServiceContext, locationId, keyPairString);
					} else if (apiId.equalsIgnoreCase(OPENSTACK_API)) {
						validateOpenstackKeyPair(computeServiceContext, locationId, keyPairString);
					} else if (apiId.equalsIgnoreCase(CLOUDSTACK)) {
						/*
						 * RestContext<CloudStackClient, CloudStackAsyncClient> unwrapped =
						 * computeServiceContext.unwrap();
						 * validateCloudstackKeyPairs(unwrapped.getApi().getSSHKeyPairClient(),
						 * aggregateAllValues(keyPairsByRegions));
						 */
					} else if (apiId.equalsIgnoreCase(VCLOUD)) {
						// security groups not supported
					} else {
						// api validations not supported yet
					}

					validationContext.validationEventEnd(ValidationResultType.OK);
					
				} catch (CloudProvisioningException ex) {
					validationContext.validationEventEnd(ValidationResultType.ERROR);
					throw ex;
				}
			}
		}
	}
	

	private void validateEC2KeyPair(final ComputeServiceContext computeServiceContext, final String locationId,
			final String keyPairName) throws CloudProvisioningException {
		RestContext<EC2Client, EC2AsyncClient> unwrapped = computeServiceContext.unwrap();
		EC2Client ec2Client = unwrapped.getApi();
		KeyPairClient ec2KeyPairClient = ec2Client.getKeyPairServices();
		String region = JCloudsUtils.getEC2region(ec2Client, locationId);
		Set<KeyPair> foundKeyPairs = ec2KeyPairClient.describeKeyPairsInRegion(region, keyPairName);
		if (foundKeyPairs == null || foundKeyPairs.size() == 0 || foundKeyPairs.iterator().next() == null) {
			throw new CloudProvisioningException(getFormattedMessage("error_key_pair_validation", keyPairName, 
					groovyFile, propertiesFile));
		}
	}
	

	private void validateOpenstackKeyPair(final ComputeServiceContext computeServiceContext, final String locationId,
			final String keyPairName) throws CloudProvisioningException {
		RestContext<NovaApi, NovaAsyncApi> unwrapped = computeServiceContext.unwrap();
		KeyPairApi keyPairApi = unwrapped.getApi().getKeyPairExtensionForZone(locationId).get();
		Predicate<org.jclouds.openstack.nova.v2_0.domain.KeyPair> keyPairNamePredicate =
				org.jclouds.openstack.nova.v2_0.predicates.KeyPairPredicates.nameEquals(keyPairName);
		if (!keyPairApi.list().anyMatch(keyPairNamePredicate)) {
			throw new CloudProvisioningException(getFormattedMessage("error_key_pair_validation", keyPairName, 
					groovyFile, propertiesFile));
		}
	}
	

	private boolean isKnownAPI(final String apiName) {
		boolean supported = false;

		if (apiName.equalsIgnoreCase(EC2_API)
				|| apiName.equalsIgnoreCase(OPENSTACK_API)) {
			// || apiName.equalsIgnoreCase(VCLOUD)
			// || apiName.equalsIgnoreCase(CLOUDSTACK)) {
			supported = true;
		}

		return supported;
	}
	

	private void validateEc2SecurityGroups(final EC2Client ec2Client, final String locationId,
			final String[] securityGroupsInRegion) throws CloudProvisioningException {

		String region = JCloudsUtils.getEC2region(ec2Client, locationId);
		org.jclouds.ec2.services.SecurityGroupClient ec2SecurityGroupsClient = ec2Client.getSecurityGroupServices();
		Set<String> missingSecurityGroups = new HashSet<String>();

		for (String securityGroupName : securityGroupsInRegion) {
			Set<org.jclouds.ec2.domain.SecurityGroup> foundGroups =
					ec2SecurityGroupsClient.describeSecurityGroupsInRegion(region, securityGroupName);
			if (foundGroups == null || foundGroups.size() == 0 || foundGroups.iterator().next() == null) {
				missingSecurityGroups.add(securityGroupName);
			}
		}

		if (missingSecurityGroups.size() == 1) {
			throw new CloudProvisioningException(getFormattedMessage("error_security_group_validation",
					missingSecurityGroups.iterator().next(), groovyFile, propertiesFile));
		} else if (missingSecurityGroups.size() > 1) {
			throw new CloudProvisioningException(getFormattedMessage("error_security_groups_validation",
					Arrays.toString(missingSecurityGroups.toArray()), groovyFile, propertiesFile));
		}
	}
	

	private void validateOpenstackSecurityGroups(final NovaApi novaApi, final String region,
			final String[] securityGroupsInRegion) throws CloudProvisioningException {

		Set<String> missingSecurityGroups = new HashSet<String>();
		SecurityGroupApi securityGroupApi = novaApi.getSecurityGroupExtensionForZone(region).get();

		for (String securityGroupName : securityGroupsInRegion) {
			Predicate<org.jclouds.openstack.nova.v2_0.domain.SecurityGroup> securityGroupNamePredicate =
					org.jclouds.openstack.nova.v2_0.predicates.SecurityGroupPredicates.nameEquals(securityGroupName);
			if (!securityGroupApi.list().anyMatch(securityGroupNamePredicate)) {
				missingSecurityGroups.add(securityGroupName);
			}
		}

		if (missingSecurityGroups.size() == 1) {
			throw new CloudProvisioningException(getFormattedMessage("error_security_group_validation",
					missingSecurityGroups.iterator().next(), groovyFile, propertiesFile));
		} else if (missingSecurityGroups.size() > 1) {
			throw new CloudProvisioningException(getFormattedMessage("error_security_groups_validation",
					Arrays.toString(missingSecurityGroups.toArray()), groovyFile, propertiesFile));
		}
	}
	

	private String getEndpoint(final ComputeTemplate template) {
		String endpoint = null;

		Map<String, Object> templateOverrides = template.getOverrides();
		if (templateOverrides != null && templateOverrides.size() > 0) {
			endpoint = (String) templateOverrides.get(ENDPOINT_OVERRIDE);
		}

		return endpoint;
	}
	

	private void closeDeployer(final JCloudsDeployer jcloudsDeployer) {
		if (jcloudsDeployer != null) {
			logger.fine("Attempting to close cloud deployer");
			jcloudsDeployer.close();
			logger.fine("Cloud deployer closed");
		}
	}
	

	private String getOpenstackLocationByHardwareId(final String hardwareId) {
		String region = "";
		if (hardwareId.indexOf("/") == -1) {
			logger.info("HardwareId is: " + hardwareId + ". It must be formatted "
					+ "as region / profile id");
			throw new IllegalArgumentException("HardwareId is: " + hardwareId + ". It must be formatted "
					+ "as region / profile id");
		}

		region = StringUtils.substringBefore(hardwareId, "/");
		if (StringUtils.isBlank(region)) {
			logger.info("HardwareId " + hardwareId + " is missing the region name. It must be formatted "
					+ "as region / profile id");
			throw new IllegalArgumentException("HardwareId is: " + hardwareId + ". It must be formatted "
					+ "as region / profile id");
		}

		logger.fine("region: " + region);
		return region;
	}
	

	/**
	 * returns the message as it appears in the DefaultProvisioningDriver message bundle.
	 *
	 * @param msgName
	 *            the message key as it is defined in the message bundle.
	 * @param arguments
	 *            the message arguments
	 * @return the formatted message according to the message key.
	 */
	protected String getFormattedMessage(final String msgName, final Object... arguments) {
		return getFormattedMessage(getDefaultProvisioningDriverMessageBundle(), msgName, arguments);
	}
	

	/**
	 * Returns the message bundle of this cloud driver.
	 *
	 * @return the message bundle of this cloud driver.
	 */
	protected static ResourceBundle getDefaultProvisioningDriverMessageBundle() {
		if (defaultProvisioningDriverMessageBundle == null) {
			defaultProvisioningDriverMessageBundle = ResourceBundle.getBundle("DefaultProvisioningDriverMessages",
					Locale.getDefault());
		}
		return defaultProvisioningDriverMessageBundle;
	}

}
