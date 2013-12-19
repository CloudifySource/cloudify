/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.openstack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.AgentComponent;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.DeployerComponent;
import org.cloudifysource.domain.cloud.DiscoveryComponent;
import org.cloudifysource.domain.cloud.FileTransferModes;
import org.cloudifysource.domain.cloud.GridComponents;
import org.cloudifysource.domain.cloud.OrchestratorComponent;
import org.cloudifysource.domain.cloud.RestComponent;
import org.cloudifysource.domain.cloud.UsmComponent;
import org.cloudifysource.domain.cloud.WebuiComponent;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.cloud.compute.ComputeTemplateNetwork;
import org.cloudifysource.domain.cloud.network.CloudNetwork;
import org.cloudifysource.domain.cloud.network.ManagementNetwork;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.domain.network.AccessRule;
import org.cloudifysource.domain.network.AccessRules;
import org.cloudifysource.domain.network.PortRange;
import org.cloudifysource.domain.network.PortRangeEntry;
import org.cloudifysource.domain.network.PortRangeFactory;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.utils.ServiceUtils.FullServiceName;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ManagementProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.FloatingIp;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServer;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerNetwork;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerResquest;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Port;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.RouteFixedIp;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Router;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.RouterExternalGatewayInfo;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroup;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroupRule;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Subnet;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationMessageType;
import org.cloudifysource.esc.driver.provisioning.validation.ValidationResultType;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.Applications;

import com.j_spaces.kernel.Environment;

/**
 * Openstack Driver which creates security groups and networks.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class OpenStackCloudifyDriver extends BaseProvisioningDriver {

	private static final String CLOUDS_FOLDER_PATH = Environment.getHomeDirectory() + "clouds";
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	private static final String MANAGEMENT_PUBLIC_ROUTER_NAME = "management-public-router";
	private static final String DEFAULT_PROTOCOL = "tcp";

	private static final int MANAGEMENT_SHUTDOWN_TIMEOUT = 60; // 60 seconds
	private static final int CLOUD_NODE_STATE_POLLING_INTERVAL = 2000;

	/**
	 * Key to set keyPairName. <br />
	 * For instance: <code>keyPairName="cloudify</code>"
	 */
	public static final String OPT_KEY_PAIR = "keyPairName";
	/**
	 * Key to set endpoint. <br />
	 * For instance: <code>openstack.endpoint="https://<IP>:5000/v2.0/"</code>
	 * */
	public static final String OPENSTACK_ENDPOINT = "openstack.endpoint";
	/**
	 * Set the name to search to find openstack compute endpoint (default="nova"). <br />
	 * For instance: <code>computeServiceName="nova"</code>
	 */
	public static final String OPT_COMPUTE_SERVICE_NAME = "computeServiceName";
	/**
	 * Set the name to search to find openstack networking endpoint (default="neutron"). <br />
	 * For instance: <code>networkServiceName="quantum"</code>
	 */
	public static final String OPT_NETWORK_SERVICE_NAME = "networkServiceName";
	/**
	 * Set the network api version (default="v2.0"). <br />
	 * The Openstack network api need version in the URL (i.e.: https://192.168.2.100:9696/<b>v2.0</b>/networks). So we
	 * might need to provide the version number to the cloud driver. <br />
	 * For instance: <code>networkApiVersion="v2.0"</code>
	 * */
	public static final String OPT_NETWORK_API_VERSION = "networkApiVersion";
	/**
	 * Specify if you want the driver to handle the external networking (default="false").<br />
	 * By default, the driver will create a router and link it to an external network. If this property is set to
	 * <code>false</code> the driver will ignore this step.
	 */
	public static final String OPT_SKIP_EXTERNAL_NETWORKING = "skipExternalNetworking";
	/**
	 * Use an existing external router.
	 * */
	public static final String OPT_EXTERNAL_ROUTER_NAME = "externalRouterName";
	/**
	 * Specify an external network to use. If no name is configured, the driver will pick the first external network it
	 * will find.<br />
	 * If you specify <code>externalRouterName</code>, this property is ignored.
	 * */
	public static final String OPT_EXTERNAL_NETWORK_NAME = "externalNetworkName";

	private OpenStackComputeClient computeApi;
	private OpenStackNetworkClient networkApi;
	private OpenStackNetworkConfigurationHelper networkHelper;

	private OpenStackResourcePrefixes openstackPrefixes;

	private String applicationName;

	public static String getDefaultMangementPrefix() {
		return MANAGMENT_MACHINE_PREFIX;
	}

	private static ResourceBundle defaultProvisioningDriverMessageBundle = ResourceBundle.getBundle(
			"DefaultProvisioningDriverMessages", Locale.getDefault());

	void setComputeApi(final OpenStackComputeClient computeApi) {
		this.computeApi = computeApi;
	}

	void setNetworkApi(final OpenStackNetworkClient networkApi) {
		this.networkApi = networkApi;
	}

	@Override
	public void setConfig(final ComputeDriverConfiguration configuration) throws CloudProvisioningException {
		this.networkHelper = new OpenStackNetworkConfigurationHelper(configuration);

		super.setConfig(configuration);

		String serviceName = null;
		if (!this.management) {
			final FullServiceName fsn = ServiceUtils.getFullServiceName(configuration.getServiceName());
			applicationName = fsn.getApplicationName();
			serviceName = fsn.getServiceName();
		}
		String managementGroup = cloud.getProvider().getManagementGroup();
		managementGroup = managementGroup == null ? MANAGMENT_MACHINE_PREFIX : managementGroup;
		this.openstackPrefixes = new OpenStackResourcePrefixes(managementGroup, applicationName, serviceName);
	}

	private void initManagementSecurityGroups() throws CloudProvisioningException {
		try {
			final GridComponents components = this.cloud.getConfiguration().getComponents();
			// default 7002
			final AgentComponent agent = components.getAgent();
			// default 7000 and 6666
			final DeployerComponent deployer = components.getDeployer();
			// default: 7001
			final DiscoveryComponent discovery = components.getDiscovery();
			// default: 7003
			final OrchestratorComponent orchestrator = components.getOrchestrator();
			// default: 7010-7110
			final UsmComponent usm = components.getUsm();

			// ** Clean security groups
			this.cleanAllSecurityGroups();

			// ** Create Cluster security group
			final SecurityGroup clusterSecgroup = this.createSecurityGroup(this.openstackPrefixes.getClusterName());

			// ** Create Management security group
			final String managementSecgroupName = this.openstackPrefixes.getManagementName();
			final SecurityGroup managementSecurityGroup = this.createSecurityGroup(managementSecgroupName);

			// ** Create Agent security groups
			final String agentSecgroupName = this.openstackPrefixes.getAgentName();
			final SecurityGroup agentSecurityGroup = this.createSecurityGroup(agentSecgroupName);

			// ** Create Management rules
			@SuppressWarnings("unchecked")
			final Set<Object> managementPorts = new HashSet<Object>(Arrays.asList(
					agent.getPort(),
					deployer.getPort(),
					deployer.getWebsterPort(),
					discovery.getPort(),
					discovery.getDiscoveryPort(),
					orchestrator.getPort(),
					usm.getPortRange()
					));
			final String managementPortRange = StringUtils.join(managementPorts, ",");
			this.createManagementRule(managementSecurityGroup.getId(), managementPortRange, clusterSecgroup.getId());

			// ** Create Agent rules
			@SuppressWarnings("unchecked")
			final Set<Object> agentPorts = new HashSet<Object>(Arrays.asList(
					agent.getPort(),
					deployer.getPort(),
					discovery.getPort(),
					usm.getPortRange()
					));
			final String agentPortRange = StringUtils.join(agentPorts, ",");
			this.createManagementRule(agentSecurityGroup.getId(), agentPortRange, clusterSecgroup.getId());

			// ** Add Management public rules
			final WebuiComponent webui = components.getWebui();
			final RestComponent rest = components.getRest();

			// Retrieve file transfert port
			final String managementMachineTemplate = this.cloud.getConfiguration().getManagementMachineTemplate();
			final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(managementMachineTemplate);
			final FileTransferModes fileTransfer = template.getFileTransfer();

			final List<?> publicPorts = Arrays.asList(fileTransfer.getDefaultPort(), webui.getPort(), rest.getPort());
			final String publicPortRange = StringUtils.join(publicPorts, ",");
			this.createManagementRule(managementSecurityGroup.getId(), publicPortRange, null);
		} catch (final Exception e) {
			try {
				this.cleanAllSecurityGroups();
			} catch (OpenstackException e1) {
				logger.warning("Couldn't clean all security groups: " + e1.getMessage());
			}
			throw new CloudProvisioningException(e);
		}
	}

	private void cleanAllSecurityGroups() throws OpenstackException {
		final String prefix = this.openstackPrefixes.getPrefix();
		final List<SecurityGroup> securityGroupsByName = this.networkApi.getSecurityGroupsByPrefix(prefix);
		for (final SecurityGroup securityGroup : securityGroupsByName) {
			this.networkApi.deleteSecurityGroup(securityGroup.getId());
		}
	}

	private void createManagementRule(final String targetSecgroupId, final String portRangeString,
			final String remoteGroupId) throws OpenstackException {

		final PortRange portRange = PortRangeFactory.createPortRange(portRangeString);
		SecurityGroupRule request;
		for (final PortRangeEntry entry : portRange.getRanges()) {
			request = new SecurityGroupRule();
			request.setSecurityGroupId(targetSecgroupId);
			request.setDirection("ingress");
			request.setProtocol(DEFAULT_PROTOCOL);
			request.setPortRangeMax(entry.getTo() == null ? entry.getFrom().toString() : entry.getTo().toString());
			request.setPortRangeMin(entry.getFrom().toString());
			if (remoteGroupId == null) {
				request.setRemoteIpPrefix("0.0.0.0/0");
			} else {
				request.setRemoteGroupId(remoteGroupId);
			}
			networkApi.createSecurityGroupRule(request);
		}
	}

	private SecurityGroup createSecurityGroup(final String secgroupName) throws OpenstackException {
		final SecurityGroup request = new SecurityGroup();
		request.setName(secgroupName);
		request.setDescription("Security groups " + secgroupName);
		return networkApi.createSecurityGroupsIfNotExist(request);
	}

	@Override
	protected void initDeployer(final Cloud cloud) {
		final ComputeTemplate cloudTemplate;
		if (this.management) {
			final String managementMachineTemplate = cloud.getConfiguration().getManagementMachineTemplate();
			cloudTemplate = cloud.getCloudCompute().getTemplates().get(managementMachineTemplate);
		} else {
			cloudTemplate = cloud.getCloudCompute().getTemplates().get(cloudTemplateName);
		}

		String endpoint = null;
		final Map<String, Object> overrides = cloudTemplate.getOverrides();
		if (overrides != null && !overrides.isEmpty()) {
			endpoint = (String) overrides.get(OPENSTACK_ENDPOINT);
		}

		final String networkApiVersion = (String) cloudTemplate.getOptions().get(OPT_NETWORK_API_VERSION);
		final String networkServiceName = (String) cloudTemplate.getOptions().get(OPT_NETWORK_SERVICE_NAME);
		final String computeServiceName = (String) cloudTemplate.getOptions().get(OPT_COMPUTE_SERVICE_NAME);

		final String cloudImageId = cloudTemplate.getImageId();
		final String region = cloudImageId.split("/")[0];

		final String cloudUser = cloud.getUser().getUser();
		final String password = cloud.getUser().getApiKey();

		if (cloudUser == null || password == null) {
			throw new IllegalStateException("Cloud user or password not found.");
		}

		final StringTokenizer st = new StringTokenizer(cloudUser, ":");
		final String tenant = st.hasMoreElements() ? (String) st.nextToken() : null;
		final String username = st.hasMoreElements() ? (String) st.nextToken() : null;

		try {
			this.computeApi = new OpenStackComputeClient(endpoint, username, password, tenant, region,
					computeServiceName);
			this.networkApi = new OpenStackNetworkClient(endpoint, username, password, tenant, region,
					networkServiceName, networkApiVersion);

		} catch (OpenstackJsonSerializationException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public MachineDetails startMachine(final ProvisioningContext context, final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		logger.fine(this.getClass().getName() + ": startMachine, management mode: " + management);
		final long end = System.currentTimeMillis() + unit.toMillis(duration);

		if (System.currentTimeMillis() > end) {
			throw new TimeoutException("Starting a new machine timed out");
		}

		try {
			// Create application secgroups
			this.createSecurityGroup(this.openstackPrefixes.getApplicationName());
			this.createSecurityGroup(this.openstackPrefixes.getServiceName());
			this.createSecurityGroupsRules();

			if (networkHelper.useApplicationNetworkTemplate()) {
				// Network
				final Network network = this.getOrCreateNetwork(this.networkHelper.getApplicationNetworkPrefixedName());
				if (network != null) {
					// Subnets
					final NetworkConfiguration networkTemplate = this.networkHelper.getApplicationNetworkTemplate();
					final List<org.cloudifysource.domain.cloud.network.Subnet> subnets =
							networkTemplate.getSubnets();
					for (final org.cloudifysource.domain.cloud.network.Subnet subnetConfig : subnets) {
						this.getOrCreateSubnet(subnetConfig, network);
					}
				}
			}

			final String groupName =
					serverNamePrefix + this.configuration.getServiceName() + "-" + counter.incrementAndGet();
			logger.fine("Starting a new cloud server with group: " + groupName);
			final ComputeTemplate computeTemplate =
					this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);
			final MachineDetails md = this.createServer(groupName, end, computeTemplate);
			return md;
		} catch (final OpenstackException e) {
			throw new CloudProvisioningException("Failed to start cloud machine", e);
		}
	}

	@Override
	public MachineDetails[] startManagementMachines(final ManagementProvisioningContext context, final long duration,
			final TimeUnit unit) throws TimeoutException, CloudProvisioningException {

		if (duration < 0) {
			throw new TimeoutException("Starting a new machine timed out");
		}

		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		logger.fine("DefaultCloudProvisioning: startMachine - management == " + management);

		// first check if management already exists
		final MachineDetails[] existingManagementServers = this.getExistingManagementServers();
		if (existingManagementServers.length > 0) {
			final String serverDescriptions =
					this.createExistingServersDescription(this.serverNamePrefix, existingManagementServers);
			throw new CloudProvisioningException("Found existing servers matching group "
					+ this.serverNamePrefix + ": " + serverDescriptions);
		}

		// Create management secgroups and rules
		this.initManagementSecurityGroups();

		// Create management networks
		if (networkHelper.useManagementNetwork()) {
			this.createManagementNetworkAndSubnets();
		}

		// launch the management machines
		publishEvent(EVENT_ATTEMPT_START_MGMT_VMS);
		final int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		final MachineDetails[] createdMachines = this.doStartManagementMachines(endTime, numberOfManagementMachines);
		publishEvent(EVENT_MGMT_VMS_STARTED);
		return createdMachines;
	}

	private void createManagementNetworkAndSubnets() throws CloudProvisioningException {
		try {
			// Clear existing network
			this.cleanAllNetworks();

			// Network
			final String managementNetworkPrefixedName = this.networkHelper.getManagementNetworkPrefixedName();
			final Network network = this.getOrCreateNetwork(managementNetworkPrefixedName);
			if (network == null) {
				throw new CloudProvisioningException("Fail to create '" + managementNetworkPrefixedName + "' network");
			}

			// Subnets
			final NetworkConfiguration networkTemplate = this.networkHelper.getManagementNetworkTemplate();
			final List<Subnet> subnets = new ArrayList<Subnet>();
			if (networkTemplate.getSubnets() != null) {
				for (final org.cloudifysource.domain.cloud.network.Subnet subnetConfig : networkTemplate.getSubnets()) {
					final Subnet subnet = this.getOrCreateSubnet(subnetConfig, network);
					subnets.add(subnet);
				}
			}

			if (!this.networkHelper.skipExternalNetworking()) {
				this.createExternalNetworking(network, subnets.get(0));
			}
		} catch (final Exception e) {
			try {
				this.cleanAllNetworks();
			} catch (OpenstackException e1) {
				logger.warning("Couldn't clean all networks: " + e1.getMessage());
			}
			throw new CloudProvisioningException(e);
		}
	}

	private void createExternalNetworking(final Network network, final Subnet subnet)
			throws OpenstackException, CloudProvisioningException {

		final Router router;
		if (this.networkHelper.isCreateExternalRouter()) {
			final String publicNetworkId;
			if (this.networkHelper.isExternalNetworkNameSpecified()) {
				publicNetworkId = networkApi.getPublicNetworkId();
			} else {
				final Network extNetwork = networkApi.getNetworkByName(this.networkHelper.getExternalNetworkName());
				if (extNetwork == null) {
					throw new CloudProvisioningException("Couldn't find external network '"
							+ this.networkHelper.getExternalNetworkName() + "'");
				}
				if (!BooleanUtils.toBoolean(extNetwork.getRouterExternal())) {
					throw new CloudProvisioningException("The network '"
							+ this.networkHelper.getExternalNetworkName() + "' is not an external network");
				}

				publicNetworkId = extNetwork.getId();
			}
			final Router request = new Router();
			request.setName(this.openstackPrefixes.getPrefix() + MANAGEMENT_PUBLIC_ROUTER_NAME);
			request.setAdminStateUp(true);
			request.setExternalGatewayInfo(new RouterExternalGatewayInfo(publicNetworkId));
			router = networkApi.createRouter(request);
		} else {
			router = networkApi.getRouterByName(this.networkHelper.getExternalRouterName());
			if (router == null) {
				throw new CloudProvisioningException("Couldn't find external router '"
						+ this.networkHelper.getExternalRouterName() + "'");
			}
		}

		if (subnet == null) {
			throw new CloudProvisioningException("Cannot add router interface because the network '"
					+ network.getName() + "' don't have any subnets");
		}

		// Add interface
		networkApi.addRouterInterface(router.getId(), subnet.getId());
	}

	private Subnet getOrCreateSubnet(final org.cloudifysource.domain.cloud.network.Subnet subnetConfig,
			final Network network) throws CloudProvisioningException, OpenstackException {
		Subnet subnet = null;
		if (subnetConfig == null) {
			throw new CloudProvisioningException("The network '" + network.getName()
					+ "' is missing subnet configuration.");
		} else {
			// Search for a subnet with the specified name
			final List<Subnet> subnets = networkApi.getSubnetsByNetworkId(network.getId());
			for (Subnet sn : subnets) {
				if (sn.getName().equals(subnetConfig.getName())) {
					subnet = sn;
					break;
				}
			}
			if (subnet == null) {
				// If the subnet with the configuration name don't exists, create it.
				final Subnet subnetRequest = this.createSubnetRequest(subnetConfig, network.getId());
				subnet = networkApi.createSubnet(subnetRequest);
			}
		}

		if (subnet == null) {
			throw new CloudProvisioningException("Missing subnets for network '" + network.getName() + "'.");
		}
		return subnet;
	}

	private Network getOrCreateNetwork(final String networkName) throws OpenstackException, CloudProvisioningException {
		Network network = networkApi.getNetworkByName(networkName);
		if (network == null) {
			final Network networkRequest = new Network();
			networkRequest.setName(networkName);
			networkRequest.setAdminStateUp(true);
			network = networkApi.createNetworkIfNotExists(networkRequest);
		}
		return network;
	}

	private Subnet createSubnetRequest(final org.cloudifysource.domain.cloud.network.Subnet subnetConfig,
			final String networkId) {
		final Subnet subnetRequest = new Subnet();
		subnetRequest.setNetworkId(networkId);
		subnetRequest.setCidr(subnetConfig.getRange());
		subnetRequest.setName(subnetConfig.getName());
		subnetRequest.setEnableDhcp(true);

		final Map<String, String> options = subnetConfig.getOptions();

		if (options.containsKey("dnsNameServers")) {
			final String dnsNameServers = options.get("dnsNameServers");
			subnetRequest.addDnsNameservers(dnsNameServers);
		}

		if (options.containsKey("gateway")) {
			final String gatewayStr = options.get("gateway");
			if (StringUtils.isNotEmpty(gatewayStr) && !"null".equals(gatewayStr)) {
				subnetRequest.setGatewayIp(gatewayStr);
				subnetRequest.addHostRoute(gatewayStr, "0.0.0.0/0"); // FIXME is it necessary ?
			} else {
				subnetRequest.setGatewayIp("null");
			}
		}

		subnetRequest.setIpVersion("4");
		return subnetRequest;
	}

	private void cleanAllNetworks() throws OpenstackException {

		// Clean external router
		if (!this.networkHelper.skipExternalNetworking()) {
			final Router router;
			if (this.networkHelper.isCreateExternalRouter()) {
				// The driver has created an external router
				router = networkApi.getRouterByName(this.openstackPrefixes.getPrefix()
						+ MANAGEMENT_PUBLIC_ROUTER_NAME);
			} else {
				// User has specified an external router to use
				router = networkApi.getRouterByName(this.networkHelper.getExternalRouterName());
			}

			if (router != null) {
				try {
					final String privateIpNetworkName = this.networkHelper.getPrivateIpNetworkName();
					final Network privateNetwork = this.networkApi.getNetworkByName(privateIpNetworkName);
					if (privateNetwork != null) {
						final String[] privateNetSubnetIds = privateNetwork.getSubnets();
						if (privateNetSubnetIds != null && privateNetSubnetIds.length > 0) {
							final List<Port> ports = networkApi.getPortsByDeviceId(router.getId());
							if (ports != null) {
								for (final Port port : ports) {
									for (final RouteFixedIp fixedIp : port.getFixedIps()) {
										for (final String id : privateNetSubnetIds) {
											if (id.equals(fixedIp.getSubnetId())) {
												networkApi.deleteRouterInterface(router.getId(), fixedIp.getSubnetId());
											}
										}
									}
								}
							}
						}
					}
				} catch (final Exception e) {
					// If the private network doesn't exist there is no consequences:
					// we can't detached a network which doesn't exist anymore.
					logger.log(Level.WARNING, "Could not remove an interface from external router", e);
				}

				if (this.networkHelper.isCreateExternalRouter()) {
					networkApi.deleteRouter(router.getId());
				}
			}
		}

		// Delete all remaining application networks
		final List<Network> appliNetworks = networkApi.getNetworkByPrefix(this.openstackPrefixes.getPrefix());
		if (appliNetworks != null) {
			for (final Network n : appliNetworks) {
				networkApi.deleteNetwork(n.getId());

			}
		}
	}

	@Override
	public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
		try {
			final String mngTemplateName = this.cloud.getConfiguration().getManagementMachineTemplate();
			final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(mngTemplateName);
			final List<NovaServer> servers = computeApi.getServersByPrefix(this.serverNamePrefix);

			final MachineDetails[] mds = new MachineDetails[servers.size()];
			for (int i = 0; i < servers.size(); i++) {
				mds[i] = this.createMachineDetails(template, servers.get(i));
			}

			return mds;
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}
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
	protected MachineDetails createServer(final String serverName, final long endTime, final ComputeTemplate template)
			throws CloudProvisioningException, TimeoutException {

		final String imageId = template.getImageId().split("/")[1];
		final String hardwareId = template.getHardwareId().split("/")[1];
		final String keyName = (String) template.getOptions().get(OPT_KEY_PAIR);

		String serverId = null;
		final List<String> reservedPortIds = new ArrayList<String>();

		try {
			final NovaServerResquest request = new NovaServerResquest();
			request.setName(serverName);
			request.setKeyName(keyName);
			request.setImageRef(imageId);
			request.setFlavorRef(hardwareId);

			// Add management network if exists
			if (this.networkHelper.useManagementNetwork()) {
				final String managementNetworkName = this.networkHelper.getManagementNetworkPrefixedName();
				final Network managementNetwork = this.networkApi.getNetworkByName(managementNetworkName);
				if (managementNetwork == null) {
					throw new CloudProvisioningException("Unexpected missing management network '"
							+ managementNetworkName + "'");
				}
				if (managementNetwork.getSubnets() == null || managementNetwork.getSubnets().length <= 0) {
					throw new CloudProvisioningException("Unexpected missing subnet in management network '"
							+ managementNetworkName + "'");
				}
				if (managementNetwork.getSubnets().length == 1) {
					request.addNetworks(managementNetwork.getId());
				} else {
					final Port port = this.addPortToRequest(request,
							managementNetwork.getId(), managementNetwork.getSubnets());
					reservedPortIds.add(port.getId());
				}
			}

			// Add compute networks
			for (final String networkName : this.networkHelper.getComputeNetworks()) {
				final Network network = this.networkApi.getNetworkByName(networkName);
				if (network == null) {
					throw new CloudProvisioningException("Couldn't find network '" + networkName + "'");
				}
				if (network.getSubnets() == null || network.getSubnets().length <= 0) {
					throw new CloudProvisioningException("Unexpected missing subnet in network '" + networkName + "'");
				}
				if (network.getSubnets().length == 1) {
					request.addNetworks(network.getId());
				} else {
					final Port port = this.addPortToRequest(request, network.getId(), network.getSubnets());
					reservedPortIds.add(port.getId());
				}
			}

			// Add template networks
			if (!management && this.networkHelper.useApplicationNetworkTemplate()) {
				final String prefixedAppliNetworkName = this.networkHelper.getApplicationNetworkPrefixedName();
				final Network templateNetwork = this.networkApi.getNetworkByName(prefixedAppliNetworkName);
				if (templateNetwork == null) {
					throw new CloudProvisioningException("Unexpected missing management network '"
							+ prefixedAppliNetworkName + "'");
				}
				if (templateNetwork.getSubnets() == null || templateNetwork.getSubnets().length <= 0) {
					throw new CloudProvisioningException("Unexpected missing subnet in management network '"
							+ prefixedAppliNetworkName + "'");
				}
				if (templateNetwork.getSubnets().length == 1) {
					request.addNetworks(templateNetwork.getId());
				} else {
					final Port port = this.addPortToRequest(request,
							templateNetwork.getId(), templateNetwork.getSubnets());
					reservedPortIds.add(port.getId());
				}
			}

			NovaServer newServer = computeApi.createServer(request);
			serverId = newServer.getId();
			newServer = this.waitForServerToBecomeReady(serverId, endTime);

			// Add security groups to all ports
			Object securityGroupsObj = template.getOptions().get("securityGroupNames");
			if (securityGroupsObj == null) {
				securityGroupsObj = template.getOptions().get("securityGroups");
			}
			final List<String> securityGroups = new ArrayList<String>();
			if (securityGroupsObj != null) {
				if (securityGroupsObj instanceof String[]) {
					securityGroups.addAll(Arrays.asList(((String[]) securityGroupsObj)));
				}
			}
			if (management) {
				securityGroups.add(this.openstackPrefixes.getManagementName());
				securityGroups.add(this.openstackPrefixes.getClusterName());
				this.setSecurityGroupsToServer(serverId, securityGroups.toArray(new String[securityGroups.size()]));
			} else {
				securityGroups.add(this.openstackPrefixes.getAgentName());
				securityGroups.add(this.openstackPrefixes.getClusterName());
				securityGroups.add(this.openstackPrefixes.getApplicationName());
				securityGroups.add(this.openstackPrefixes.getServiceName());
				this.setSecurityGroupsToServer(serverId, securityGroups.toArray(new String[securityGroups.size()]));
			}

			// Associate floating ips if configured
			if (this.networkHelper.associateFloatingIp()) {
				final String privateIPNetworkName = this.networkHelper.getPrivateIpNetworkName();
				final Network privateIpNetwork = this.networkApi.getNetworkByName(privateIPNetworkName);
				if (privateIpNetwork == null) {
					throw new CloudProvisioningException("Couldn't find network '" + privateIPNetworkName
							+ "' to assign floating IP.");
				}
				networkApi.createAndAssociateFloatingIp(serverId, privateIpNetwork.getId());
			}

			final MachineDetails md = this.createMachineDetails(template, newServer);

			return md;
		} catch (final Exception e) {
			logger.log(Level.SEVERE, "An error occured during initialization."
					+ " Shutting down machine and cleaning openstack resources", e);
			if (serverId != null) {
				try {
					computeApi.deleteServer(serverId);
				} catch (final OpenstackException e1) {
					logger.log(Level.WARNING, "Cleaning after error. Could not delete server.", e1);
				}
			} else {
				for (final String portId : reservedPortIds) {
					try {
						// Application port are created before the VM.
						// So it can happen that port is created but an error occurs on VM instantiation.
						// In this case, we have to clear the port.
						// * Note: Port is deleted with server deletion, so no need to handle port deletion once the
						// server has been associated to the port.
						networkApi.deletePort(portId);
					} catch (final OpenstackException e1) {
						logger.log(Level.WARNING, "Cleaning after error. Could not delete server.", e1);
					}
				}
			}
			throw new CloudProvisioningException(e);
		}
	}

	/**
	 * Create a port to attached to the VM and add it to the request. <br />
	 */
	private Port addPortToRequest(final NovaServerResquest request, final String networkId, final String[] subnetIds)
			throws OpenstackException {
		final Port port = new Port();
		for (final String subnetId : subnetIds) {
			final RouteFixedIp fixedIp = new RouteFixedIp();
			fixedIp.setSubnetId(subnetId);
			port.addFixedIp(fixedIp);
		}
		port.setNetworkId(networkId);
		final Port createdPort = this.networkApi.createPort(port);

		final NovaServerNetwork nsn = new NovaServerNetwork();
		nsn.setPort(createdPort.getId());
		request.addNetworks(nsn);

		return createdPort;
	}

	private void setSecurityGroupsToServer(final String serverId, final String... securityGroupNames)
			throws OpenstackException, CloudProvisioningException {
		final List<Port> ports = networkApi.getPortsByDeviceId(serverId);

		for (final Port port : ports) {
			final Port updateRequest = new Port();
			updateRequest.setId(port.getId());
			for (final String sgn : securityGroupNames) {
				final SecurityGroup sg = networkApi.getSecurityGroupsByName(sgn);
				if (sg == null) {
					throw new CloudProvisioningException("Couldn't find security group '" + sgn + "'");
				}
				updateRequest.addSecurityGroup(sg.getId());
			}
			networkApi.updatePort(updateRequest);
		}
	}

	private void createSecurityGroupsRules() throws OpenstackException, CloudProvisioningException {

		final String serviceSecgroupName = this.openstackPrefixes.getServiceName();
		final SecurityGroup serviceSecGroup = networkApi.getSecurityGroupsByName(serviceSecgroupName);
		if (serviceSecGroup == null) {
			throw new CloudProvisioningException("Couldn't find security group '" + serviceSecgroupName + "'");
		}

		final String managementSecgroupName = this.openstackPrefixes.getManagementName();
		final SecurityGroup managementSecGroup = networkApi.getSecurityGroupsByName(managementSecgroupName);
		if (managementSecGroup == null) {
			throw new CloudProvisioningException("Couldn't find security group '" + managementSecgroupName + "'");
		}

		// Open the transfert mode port to the managers
		final ComputeTemplate cloudTemplate = cloud.getCloudCompute().getTemplates().get(cloudTemplateName);
		final String port = Integer.toString(cloudTemplate.getFileTransfer().getDefaultPort());
		final SecurityGroupRule request = new SecurityGroupRule();
		request.setSecurityGroupId(serviceSecGroup.getId());
		request.setDirection("ingress");
		request.setProtocol(DEFAULT_PROTOCOL);
		request.setPortRangeMax(port);
		request.setPortRangeMin(port);
		request.setRemoteGroupId(managementSecGroup.getId());
		networkApi.createSecurityGroupRule(request);

		// Create service rules
		final AccessRules accessRules = this.networkHelper.getServiceAccessRules();
		if (accessRules != null) {
			for (final AccessRule accessRule : accessRules.getIncoming()) {
				this.createAccessRule(serviceSecGroup.getId(), "ingress", accessRule);
			}
			for (final AccessRule accessRule : accessRules.getOutgoing()) {
				// If there is egress rules defined. we should delete the openstack default egress rules.
				this.deleteEgressRulesFromSecurityGroup(this.openstackPrefixes.getServiceName());
				this.createAccessRule(serviceSecGroup.getId(), "egress", accessRule);
			}
		}
	}

	private void deleteEgressRulesFromSecurityGroup(final String securityGroupName) throws OpenstackException {
		final SecurityGroup securityGroup = networkApi.getSecurityGroupsByName(securityGroupName);
		if (securityGroup != null) {
			final SecurityGroupRule[] securityGroupRules = securityGroup.getSecurityGroupRules();
			if (securityGroupRules != null) {
				for (final SecurityGroupRule rule : securityGroupRules) {
					if ("egress".equals(rule.getDirection())) {
						networkApi.deleteSecurityGroupRule(rule.getId());
					}
				}
			}
		}
	}

	private void createAccessRule(final String serviceSecgroupId, final String direction, final AccessRule accessRule)
			throws OpenstackException, CloudProvisioningException {

		// Parse ports
		final PortRange portRange = PortRangeFactory.createPortRange(accessRule.getPortRange());
		if (portRange != null && !portRange.getRanges().isEmpty()) {
			String targetSecurityGroupId = serviceSecgroupId;
			String ip = "0.0.0.0/0";
			String group = null;

			switch (accessRule.getType()) {
			case PUBLIC:
				// Rules to apply to public network
				break;
			case SERVICE:
				// Rules with group filtering
				group = this.openstackPrefixes.getServiceName();
				break;
			case APPLICATION:
				// Rules with group filtering
				group = this.openstackPrefixes.getApplicationName();
				break;
			case CLUSTER:
				// Rules with group filtering
				group = this.openstackPrefixes.getClusterName();
				break;
			case GROUP:
				// Rules with group filtering
				group = accessRule.getTarget();
				break;
			case RANGE:
				// Rules with ip filtering
				if (accessRule.getTarget() == null) {
					throw new CloudProvisioningException("No IP defined for the 'Range' access rule type :"
							+ accessRule);
				}
				ip = accessRule.getTarget();
				break;
			case PRIVATE:
			default:
				throw new CloudProvisioningException("Unsupported type of rule '" + accessRule.getType() + "'");
			}

			SecurityGroup existingSecgroup = null;
			if (group != null) {
				existingSecgroup = this.networkApi.getSecurityGroupsByName(group);
				if (existingSecgroup == null) {
					throw new CloudProvisioningException("Security group '" + group + "' does not exist.");
				}
			}

			// Create rules
			for (final PortRangeEntry pre : portRange.getRanges()) {
				final SecurityGroupRule request = new SecurityGroupRule();
				request.setDirection(direction);
				request.setProtocol(DEFAULT_PROTOCOL);
				request.setSecurityGroupId(targetSecurityGroupId);
				request.setPortRangeMax(pre.getTo() == null ? pre.getFrom().toString() : pre.getTo().toString());
				request.setPortRangeMin(pre.getFrom().toString());
				if (existingSecgroup != null) {
					request.setRemoteGroupId(existingSecgroup.getId());
				} else {
					request.setRemoteIpPrefix(ip);
				}
				networkApi.createSecurityGroupRule(request);
			}
		}
	}

	private MachineDetails createMachineDetails(final ComputeTemplate template, final NovaServer server)
			throws CloudProvisioningException {
		try {
			final MachineDetails md = this.createMachineDetailsForTemplate(template);

			md.setMachineId(server.getId());
			md.setCloudifyInstalled(false);
			md.setInstallationDirectory(null);
			md.setOpenFilesLimit(template.getOpenFilesLimit());
			// md.setInstallationDirectory(template.getRemoteDirectory());
			// md.setRemoteDirectory(remoteDirectory);
			// md.setInstallerConfigutation(installerConfigutation);
			// md.setKeyFile(keyFile);
			// md.setLocationId(locationId);

			final String privateIpNetworkName = this.networkHelper.getPrivateIpNetworkName();
			final Network privateIpNetwork = this.networkApi.getNetworkByName(privateIpNetworkName);
			if (privateIpNetwork == null) {
				throw new CloudProvisioningException("Couldn't find network '" + privateIpNetworkName
						+ "' to set private IP.");
			}
			final Port privateIpPort = networkApi.getPort(server.getId(), privateIpNetwork.getId());
			if (privateIpPort == null) {
				throw new CloudProvisioningException("Server '" + server.getName()
						+ "' has no port on network '" + privateIpNetwork.getName() + "'.");

			}
			if (privateIpPort.getFixedIps() == null || privateIpPort.getFixedIps().isEmpty()) {
				throw new CloudProvisioningException("No fixed IP found on the port which link server '"
						+ server.getName() + "' tonetwork '" + privateIpNetwork.getName() + "'.");
			}

			final RouteFixedIp fixedIp = privateIpPort.getFixedIps().get(0);
			md.setPrivateAddress(fixedIp.getIpAddress());

			if (this.networkHelper.associateFloatingIp()) {
				final FloatingIp floatingIp = networkApi.getFloatingIpByPortId(privateIpPort.getId());
				if (floatingIp != null) {
					md.setPublicAddress(floatingIp.getFloatingIpAddress());
				}
			}

			final String applicationNetworkName = this.networkHelper.getApplicationNetworkPrefixedName();
			if (applicationNetworkName != null) {
				// Since it is possible that the service itself will prefer to be available only on the application
				// network and not on all networks, the cloud driver should add an environment variable specifying the
				// IP of the NIC that is connected to the application network.
				final Network appliNetwork = this.networkApi.getNetworkByName(applicationNetworkName);
				final Port appliPort = networkApi.getPort(server.getId(), appliNetwork.getId());
				final RouteFixedIp appliFixedIp = appliPort.getFixedIps().get(0);
				Map<String, String> env = new HashMap<String, String>();
				env.put("CLOUDIFY_APPLICATION_NETWORK_IP", appliFixedIp.getIpAddress());
				md.setEnvironment(env);
			}

			this.handleServerCredentials(md, template);
			return md;
		} catch (Exception e) {
			throw new CloudProvisioningException(e);
		}
	}

	private NovaServer waitForServerToBecomeReady(final String serverId, final long endTime)
			throws CloudProvisioningException, InterruptedException, TimeoutException {

		while (System.currentTimeMillis() < endTime) {
			final NovaServer server;
			try {
				server = computeApi.getServerDetails(serverId);
			} catch (final OpenstackException e) {
				throw new CloudProvisioningException(e);
			}

			if (server == null) {
				logger.fine("Server Status (" + serverId + ") Not Found, please wait...");
				Thread.sleep(CLOUD_NODE_STATE_POLLING_INTERVAL);
				break;
			} else {
				switch (server.getStatus()) {
				case ACTIVE:
					return server;
				case BUILD:
					logger.fine("Server Status (" + serverId + ") still PENDING, please wait...");
					Thread.sleep(CLOUD_NODE_STATE_POLLING_INTERVAL);
					break;
				default:
					throw new CloudProvisioningException(
							"Failed to allocate server - Cloud reported node in "
									+ server.getStatus().toString()
									+ " state. Node details: " + server);
				}
			}

		}

		throw new TimeoutException("Node failed to reach RUNNING mode in time");
	}

	@Override
	protected void handleProvisioningFailure(final int numberOfManagementMachines, final int numberOfErrors,
			final Exception firstCreationException, final MachineDetails[] createdManagementMachines)
			throws CloudProvisioningException {
		logger.severe("Of the required " + numberOfManagementMachines
				+ " management machines, " + numberOfErrors
				+ " failed to start.");
		if (numberOfManagementMachines > numberOfErrors) {
			logger.severe("Shutting down the other managememnt machines");

			for (final MachineDetails machineDetails : createdManagementMachines) {
				if (machineDetails != null) {
					logger.severe("Shutting down machine: " + machineDetails);
					try {
						this.computeApi.deleteServer(machineDetails.getMachineId());
					} catch (final OpenstackException e) {
						throw new CloudProvisioningException(e);
					}
				}
			}
		}
		throw new CloudProvisioningException(
				"One or more management machines failed. The first encountered error was: "
						+ firstCreationException.getMessage(), firstCreationException);
	}

	@Override
	public void stopManagementMachines() throws TimeoutException, CloudProvisioningException {
		try {
			final MachineDetails[] managementServers = this.getExistingManagementServers();
			if (managementServers.length == 0) {
				throw new CloudProvisioningException(
						"Could not find any management machines for this cloud (management machine prefix is: "
								+ this.serverNamePrefix + ")");
			}

			for (final MachineDetails md : managementServers) {
				try {
					this.releaseFloatingIpsForServerId(md.getMachineId());
					this.computeApi.deleteServer(md.getMachineId());
				} catch (final Exception e) {
					throw new CloudProvisioningException(e);
				}
			}
			for (final MachineDetails md : managementServers) {
				try {
					this.waitForServerToBeShutdown(md.getMachineId(), MANAGEMENT_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			// ** Clean security groups & networks
			try {
				this.cleanAllSecurityGroups();
				this.cleanAllNetworks();
			} catch (final Exception e) {
				logger.warning("Couldn't clean security groups " + this.openstackPrefixes.getPrefix() + "*");
			}
		} finally {
			if (this.computeApi != null) {
				this.computeApi.close();
			}
			if (this.networkApi != null) {
				this.networkApi.close();
			}
		}

	}

	@Override
	public boolean stopMachine(final String serverIp, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, TimeoutException, InterruptedException {

		boolean stopResult = false;
		logger.info("Stop Machine - machineIp: " + serverIp);
		logger.info("Looking up cloud server with IP: " + serverIp);

		final NovaServer server;
		try {
			// We must provide the security group name.
			// Indeed with network support, 2 VMs of different services can now have the same ip address.
			// We must be sure to delete the right server.
			server = computeApi.getServerByIpAndSecurityGroup(serverIp, this.openstackPrefixes.getServiceName());
		} catch (final OpenstackException e) {
			throw new CloudProvisioningException(e);
		}

		if (server != null) {
			logger.info("Found server: " + server.getId() + ". Shutting it down and waiting for shutdown to complete");

			// Release and delete floating Ip if exists
			this.releaseFloatingIpsForServerId(server.getId());

			// Delete server
			try {
				computeApi.deleteServer(server.getId());
			} catch (final OpenstackException e) {
				throw new CloudProvisioningException(e);
			}
			if (duration != 0) {
				this.waitForServerToBeShutdown(server.getId(), duration, unit);
			}
			logger.info("Server: " + server.getId() + " is shutdown.");
			stopResult = true;
		} else {
			logger.log(Level.SEVERE,
					"Received scale in request for machine with ip "
							+ serverIp
							+ " but this IP could not be found in the Cloud server list");
			stopResult = false;
		}

		return stopResult;
	}

	private void releaseFloatingIpsForServerId(final String serverId) {
		try {
			final List<Port> ports = networkApi.getPortsByDeviceId(serverId);
			if (ports != null) {
				for (final Port port : ports) {
					final FloatingIp floatingIp = networkApi.getFloatingIpByPortId(port.getId());
					if (floatingIp != null) {
						try {
							logger.info("Deleting Floating ip: " + floatingIp);
							networkApi.deleteFloatingIP(floatingIp.getId());
						} catch (final Exception e) {
							logger.warning("Couldn't delete floating ip: " + floatingIp + " cause: " + e.getMessage());
						}
					}
				}
			}
		} catch (final OpenstackException e) {
			logger.log(Level.WARNING, "Could not release floating ip associated to server id='" + serverId + "'", e);
		}
	}

	private void waitForServerToBeShutdown(final String serverId, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, InterruptedException, TimeoutException {

		logger.finer("Wait server '" + serverId + "' to shutdown (" + duration + " " + unit + ")");

		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		while (System.currentTimeMillis() < endTime) {
			final NovaServer server;
			try {
				server = computeApi.getServerDetails(serverId);
			} catch (final OpenstackException e) {
				throw new CloudProvisioningException(e);
			}

			if (server == null) {
				logger.fine("Server Status (" + serverId + ") Not Found. Considered deleted.");
				return;
			} else {
				switch (server.getStatus()) {
				case STOPPED:
				case DELETED:
					return;
				case ERROR:
				case UNKNOWN:
				case UNRECOGNIZED:
					throw new CloudProvisioningException(
							"Failed to allocate server - Cloud reported node in "
									+ server.getStatus().toString()
									+ " state. Node details: " + server);
				default:
					logger.fine("Server Status (" + serverId + ") is " + server.getStatus()
							+ ", please wait until shutdown...");
					Thread.sleep(CLOUD_NODE_STATE_POLLING_INTERVAL);
					break;
				}
			}

		}

		throw new TimeoutException("Node failed to reach SHUTDOWN mode in time");
	}

	@Override
	public void onServiceUninstalled(final long duration, final TimeUnit unit) throws InterruptedException,
			TimeoutException, CloudProvisioningException {

		final String ssgName = this.openstackPrefixes.getServiceName();
		logger.info("Service '" + ssgName + "'is being uninstall.");
		try {
			final Applications applications = this.admin.getApplications();
			final Application application = applications.getApplication(this.applicationName);
			if (application == null) {
				logger.info("No remaining services in the application.");

				logger.info("Delete the application security group.");
				final String applicationName = this.openstackPrefixes.getApplicationName();
				final SecurityGroup secgroup = this.networkApi.getSecurityGroupsByName(applicationName);
				if (secgroup != null) {
					networkApi.deleteSecurityGroup(secgroup.getId());
				}

				if (this.networkHelper.useApplicationNetworkTemplate()) {
					logger.info("Delete the network.");
					final String prefixedNetworkName = this.networkHelper.getApplicationNetworkPrefixedName();
					try {
						final Network appliNetwork = networkApi.getNetworkByName(prefixedNetworkName);
						networkApi.deleteNetwork(appliNetwork.getId());
					} catch (final Exception e) {
						logger.warning("Network '" + prefixedNetworkName + "' was not deleted: " + e.getMessage());
					}
				}
			}

			logger.info("Clean service's security group :" + ssgName + "*");
			final List<SecurityGroup> securityGroups = this.networkApi.getSecurityGroupsByPrefix(ssgName);
			for (final SecurityGroup securityGroup : securityGroups) {
				this.networkApi.deleteSecurityGroup(securityGroup.getId());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Fail to clean security group resources of service " + ssgName, e);
		} finally {
			if (this.computeApi != null) {
				this.computeApi.close();
			}
			if (this.networkApi != null) {
				this.networkApi.close();
			}
		}

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

	@Override
	public void validateCloudConfiguration(final ValidationContext validationContext)
			throws CloudProvisioningException {

		String cloudFolder = CLOUDS_FOLDER_PATH + FILE_SEPARATOR + cloud.getName();
		String groovyFile = cloudFolder + FILE_SEPARATOR + cloud.getName() + "-cloud.groovy";
		String propertiesFile = cloudFolder + FILE_SEPARATOR + cloud.getName() + "-cloud.properties";

		validationContext.validationEvent(ValidationMessageType.TOP_LEVEL_VALIDATION_MESSAGE,
				getFormattedMessage("validating_all_templates"));

		final Map<String, ComputeTemplate> templates = cloud.getCloudCompute().getTemplates();

		final String mangementTemplateName = cloud.getConfiguration().getManagementMachineTemplate();
		final ComputeTemplate managementComputeTemplate =
				cloud.getCloudCompute().getTemplates().get(mangementTemplateName);

		// validating openstack endpoint
		this.validateOpenstackEndpoint(validationContext, managementComputeTemplate);

		// validating management network/subnets configuration
		final CloudNetwork cloudNetwork = configuration.getCloud().getCloudNetwork();
		this.validateManagementNetwork(validationContext, managementComputeTemplate, cloudNetwork);

		// validating templates networks configuration
		if (cloudNetwork != null) {
			this.validateTemplateNetworks(validationContext, cloudNetwork);
		}

		// validating templates
		this.validateComputeTemplates(validationContext, groovyFile, propertiesFile, templates);

	}

	private void validateTemplateNetworks(final ValidationContext validationContext, final CloudNetwork cloudNetwork)
			throws CloudProvisioningException {
		Map<String, NetworkConfiguration> templateNetworkConfigurations = cloudNetwork.getTemplates();
		if (templateNetworkConfigurations != null) {

			validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
					"Validating templates network configuration");

			for (Entry<String, NetworkConfiguration> networkConfigurationEntry : templateNetworkConfigurations
					.entrySet()) {

				if (!networkHelper.isValidNetworkName(networkConfigurationEntry.getValue())) {
					validationContext.validationEventEnd(ValidationResultType.ERROR);
					throw new CloudProvisioningException(String.format(
							"The name of template network configuration is missing. "
									+ "Please check template network in '%s'",
							networkConfigurationEntry.getKey()));
				}

				List<org.cloudifysource.domain.cloud.network.Subnet> templateNetworkSubnets =
						networkConfigurationEntry.getValue().getSubnets();

				if (templateNetworkSubnets == null || templateNetworkSubnets.isEmpty()) {
					validationContext.validationEventEnd(ValidationResultType.ERROR);
					throw new CloudProvisioningException(
							String.format(
									"Subnets list is empty. At least one subnet is required. "
											+ "Please check template network configuration in '%s'.",
									networkConfigurationEntry.getValue().getName()));
				}

				for (org.cloudifysource.domain.cloud.network.Subnet mSub : templateNetworkSubnets) {

					if (!networkHelper.isValidSubnetName(mSub)) {
						validationContext.validationEventEnd(ValidationResultType.ERROR);
						throw new CloudProvisioningException(
								String.format(
										"The name of the subnet is missing."
												+ " Please check subnet name in template network "
												+ "configuration '%s' ",
										networkConfigurationEntry.getValue().getName()));
					}

					if (mSub.getRange() == null || StringUtils.trim(mSub.getRange()).isEmpty()) {
						validationContext.validationEventEnd(ValidationResultType.ERROR);
						throw new CloudProvisioningException(
								String.format(
										"The range is missing in subnet '%s'. "
												+ "Please check subnet range in template network "
												+ "configuration '%s' ",
										mSub.getName(), networkConfigurationEntry.getKey()));
					}
				}
			}
			// template networks subnets are OK
			validationContext.validationEventEnd(ValidationResultType.OK);
		}
	}

	private void validateManagementNetwork(final ValidationContext validationContext,
			final ComputeTemplate managementComputeTemplate, final CloudNetwork cloudNetwork)
			throws CloudProvisioningException {

		validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
				"Validating management network configuration");

		boolean isNetworkExistsForManager = false;
		if (cloudNetwork != null) {
			final ManagementNetwork managementNetwork = cloudNetwork.getManagement();
			if (managementNetwork != null) {
				final NetworkConfiguration managementNetworkConfiguration = managementNetwork.getNetworkConfiguration();
				// network available for management
				if (managementNetworkConfiguration != null
						&& (managementNetworkConfiguration.getName() != null
								|| (managementNetworkConfiguration.getSubnets() != null
								&& !managementNetworkConfiguration.getSubnets().isEmpty())
								|| !managementNetworkConfiguration.getCustom().isEmpty())) {

					isNetworkExistsForManager = true;

					final String mngNetName = managementNetworkConfiguration.getName();
					if (mngNetName == null
							|| StringUtils.isEmpty(mngNetName.trim())) {
						validationContext.validationEventEnd(ValidationResultType.ERROR);
						throw new CloudProvisioningException(
								"The name of Management network is missing. "
										+ "Please check management network configuration in CloudNetwork block.");
					}

					// validating subnets for management network
					List<org.cloudifysource.domain.cloud.network.Subnet> managementNetwokSubnets =
							managementNetworkConfiguration.getSubnets();

					if (managementNetwokSubnets != null) {

						// no subnets defined in management network
						if (managementNetwokSubnets.isEmpty()) {
							validationContext.validationEventEnd(ValidationResultType.ERROR);
							throw new CloudProvisioningException(
									String.format(
											"Subnets list is empty. At least one subnet is required."
													+ " Please check management network configuration in '%s'.",
											mngNetName));
						} else {
							// subnets are defined
							for (org.cloudifysource.domain.cloud.network.Subnet mSub : managementNetwokSubnets) {
								if (!networkHelper.isValidSubnetName(mSub)) {
									validationContext.validationEventEnd(ValidationResultType.ERROR);
									throw new CloudProvisioningException(String.format("The Name of subnet is missing."
											+ " Please check subnet in management network "
											+ "configuration '%s.", managementNetwork.getNetworkConfiguration()
											.getName()));

								}
								if (!networkHelper.isValidSubnetRange(mSub)) {
									validationContext.validationEventEnd(ValidationResultType.ERROR);
									throw new CloudProvisioningException(
											String.format(
													"The range is missing in subnet '%s'. "
															+ "Please check subnet range in management network "
															+ "configuration.",
													mSub.getName()));
								}
							}
						}
					}
				}
			}
		}

		if (!isNetworkExistsForManager) {
			if (managementComputeTemplate != null) {
				final ComputeTemplateNetwork computeNetwork = managementComputeTemplate.getComputeNetwork();
				List<String> computeNetworks = computeNetwork.getNetworks();

				if (computeNetworks != null && !computeNetworks.isEmpty()) {
					isNetworkExistsForManager = true;
				}
			}

			if (!isNetworkExistsForManager) {
				validationContext.validationEventEnd(ValidationResultType.ERROR);
				throw new CloudProvisioningException(
						"A network must be provided to the management machines "
								+ "(use either cloudNetwork templates or computeNetwork configuration).");
			}
		}

		// management network/ subnets are OK
		validationContext.validationEventEnd(ValidationResultType.OK);
	}

	private void validateOpenstackEndpoint(final ValidationContext validationContext,
			final ComputeTemplate managementComputeTemplate) throws CloudProvisioningException {

		validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
				"Validating openstack endpoint property");

		if (managementComputeTemplate.getOverrides() != null
				&& !managementComputeTemplate.getOverrides().isEmpty()) {

			String openstackProperty =
					(String) managementComputeTemplate.getOverrides().get(OPENSTACK_ENDPOINT);

			if (openstackProperty == null || openstackProperty.trim().isEmpty()) {
				validationContext.validationEventEnd(ValidationResultType.ERROR);
				throw new CloudProvisioningException((String.format(
						"The openstack endpoint option '%s' is missing. "
								+ "Please check overrides block in management template '%s'. ",
						OPENSTACK_ENDPOINT, cloud.getConfiguration().getManagementMachineTemplate())));
			}
			validationContext.validationEventEnd(ValidationResultType.OK);
		} else {
			validationContext.validationEventEnd(ValidationResultType.ERROR);
			throw new CloudProvisioningException(String.format(
					"The openstack endpoint option '%s' is missing. "
							+ "Please check overrides block in management template ",
					OPENSTACK_ENDPOINT));
		}
	}

	private void validateComputeTemplates(final ValidationContext validationContext,
			final String groovyFile,
			final String propertiesFile,
			final Map<String, ComputeTemplate> templates)
			throws CloudProvisioningException {
		String templateName;
		for (Entry<String, ComputeTemplate> entry : templates.entrySet()) {
			final ComputeTemplate computeTemplate = entry.getValue();
			templateName = entry.getKey();

			validationContext.validationEvent(ValidationMessageType.GROUP_VALIDATION_MESSAGE,
					getFormattedMessage("validating_template", templateName));

			this.validateImageHardwareLocation(validationContext, groovyFile, propertiesFile, computeTemplate);

			// validating static securityGroupNames
			this.validateStaticSecgroups(validationContext, groovyFile, propertiesFile, computeTemplate);

			// validating static network
			this.validateStaticNetworks(validationContext, groovyFile, propertiesFile, computeTemplate);
		}
	}

	private void validateStaticNetworks(final ValidationContext validationContext, final String groovyFile,
			final String propertiesFile, final ComputeTemplate computeTemplate) throws CloudProvisioningException {
		final List<String> networks = computeTemplate.getComputeNetwork().getNetworks();
		if (networks != null && !networks.isEmpty()) {
			validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
					"Validating network(s): " + networks.toString());
			try {
				final Set<String> missingList = new HashSet<String>();
				final List<Network> existingList = networkApi.getNetworks();
				for (final String networkName : networks) {
					boolean found = false;
					if (existingList != null) {
						for (final Network network : existingList) {
							if (networkName.equals(network.getName())) {
								found = true;
								break;
							}
						}
					}
					if (!found || existingList == null || existingList.isEmpty()) {
						missingList.add(networkName);
					}
				}

				if (!missingList.isEmpty()) {
					validationContext.validationEventEnd(ValidationResultType.ERROR);
					if (missingList.size() == 1) {
						throw new CloudProvisioningException(String.format(
								"Network \"%s\" does not exist. Please create it or rename in %s or in %s",
								missingList.iterator().next(), groovyFile, propertiesFile));
					} else if (missingList.size() > 1) {
						throw new CloudProvisioningException(String.format(
								"Networks %s do not exist. Please create them or rename in %s or in %s",
								Arrays.toString(missingList.toArray()), groovyFile, propertiesFile));
					}
				}

			} catch (final OpenstackException ex) {
				validationContext.validationEventEnd(ValidationResultType.ERROR);
				throw new CloudProvisioningException("Error requesting networks.", ex);
			}
			validationContext.validationEventEnd(ValidationResultType.OK);
		}
	}

	private void validateStaticSecgroups(final ValidationContext validationContext, final String groovyFile,
			final String propertiesFile, final ComputeTemplate computeTemplate) throws CloudProvisioningException {
		final Map<String, Object> computeOptions = computeTemplate.getOptions();
		if (computeOptions != null) {
			Object securityGroups = computeOptions.get("securityGroupNames");
			if (securityGroups == null) {
				securityGroups = computeOptions.get("securityGroups");
			}
			if (securityGroups != null) {
				if (securityGroups instanceof String[] && ((String[]) securityGroups).length > 0) {
					final String[] scgArray = (String[]) securityGroups;
					if (scgArray.length == 1) {
						validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
								getFormattedMessage("validating_security_group", scgArray[0]));
					} else {
						validationContext.validationOngoingEvent(
								ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
								getFormattedMessage("validating_security_groups",
										org.cloudifysource.esc.util.StringUtils.arrayToString(scgArray, ", ")));
					}

					try {
						final Set<String> missingList = new HashSet<String>();
						final List<SecurityGroup> existingList = networkApi.getSecurityGroups();
						for (int i = 0; i < scgArray.length; i++) {
							boolean found = false;
							if (existingList != null) {
								for (final SecurityGroup existing : existingList) {
									if (scgArray[i].equals(existing.getName())) {
										found = true;
										break;
									}
								}
							}
							if (!found || existingList == null || existingList.isEmpty()) {
								missingList.add(scgArray[i]);
							}
						}
						if (!missingList.isEmpty()) {
							validationContext.validationEventEnd(ValidationResultType.ERROR);
							if (missingList.size() == 1) {
								throw new CloudProvisioningException(getFormattedMessage(
										"error_security_group_validation",
										missingList.iterator().next(), groovyFile, propertiesFile));
							} else if (missingList.size() > 1) {
								throw new CloudProvisioningException(getFormattedMessage(
										"error_security_groups_validation",
										Arrays.toString(missingList.toArray()), groovyFile, propertiesFile));
							}
						}

					} catch (final OpenstackException e) {
						validationContext.validationEventEnd(ValidationResultType.ERROR);
						throw new CloudProvisioningException("Error requesting security groups.", e);
					}
				}
				validationContext.validationEventEnd(ValidationResultType.OK);
			}
		}
	}

	private void validateImageHardwareLocation(final ValidationContext validationContext, final String groovyFile,
			final String propertiesFile, final ComputeTemplate computeTemplate) throws CloudProvisioningException {

		final String imageLocation = computeTemplate.getImageId();
		if (!imageLocation.contains("/")) {
			throw new CloudProvisioningException("'imageId' should be formatted as region/imageId");
		}
		final String hardwareLocation = computeTemplate.getHardwareId();
		if (!hardwareLocation.contains("/")) {
			throw new CloudProvisioningException("'hardwareId' should be formatted as region/flavorId");
		}

		final String imageId = imageLocation.split("/")[1];
		final String hardwareId = hardwareLocation.split("/")[1];
		final String locationId = imageLocation.split("/")[0];

		validationContext.validationOngoingEvent(ValidationMessageType.ENTRY_VALIDATION_MESSAGE,
				getFormattedMessage("validating_image_hardware_location_combination",
						imageId == null ? "" : imageId, hardwareId == null ? "" : hardwareId,
						locationId == null ? "" : locationId));
		// validating imageIds
		try {
			if (imageId != null) {
				try {
					computeApi.getImage(imageId);
				} catch (final OpenstackException e) {
					validationContext.validationEventEnd(ValidationResultType.ERROR);
					final String availableResources = this.formatResourceList(computeApi.getImages());
					throw new CloudProvisioningException(
							getFormattedMessage("error_image_id_validation",
									imageId == null ? "" : imageId, availableResources));
				}
			}

			// validating hardwareId
			if (hardwareId != null) {
				try {
					computeApi.getFlavor(hardwareId);
				} catch (final OpenstackException e) {
					validationContext.validationEventEnd(ValidationResultType.ERROR);
					final String availableResources = this.formatResourceList(computeApi.getFlavors());
					throw new CloudProvisioningException(
							getFormattedMessage("error_hardware_id_validation",
									hardwareId == null ? "" : hardwareId, availableResources));
				}
			}
		} catch (final OpenstackException ex) {
			validationContext.validationEventEnd(ValidationResultType.ERROR);
			throw new CloudProvisioningException(
					getFormattedMessage("error_image_hardware_location_combination_validation",
							imageId == null ? "" : imageId,
							hardwareId == null ? "" : hardwareId, locationId == null ? "" : locationId,
							groovyFile, propertiesFile), ex);
		}

		validationContext.validationEventEnd(ValidationResultType.OK);
	}

	private String formatResourceList(final List<?> resources) {
		final StringBuilder sb = new StringBuilder();
		for (final Object resource : resources) {
			sb.append(System.getProperty("line.separator"));
			sb.append(resource);
		}
		return sb.toString();
	}

}