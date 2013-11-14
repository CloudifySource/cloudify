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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.ServiceNetwork;
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
import org.cloudifysource.domain.cloud.network.CloudNetwork;
import org.cloudifysource.domain.cloud.network.ManagementNetwork;
import org.cloudifysource.domain.cloud.network.NetworkConfiguration;
import org.cloudifysource.domain.network.AccessRule;
import org.cloudifysource.domain.network.PortRange;
import org.cloudifysource.domain.network.PortRangeEntry;
import org.cloudifysource.domain.network.PortRangeFactory;
import org.cloudifysource.esc.driver.provisioning.BaseProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ComputeDriverConfiguration;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ManagementProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServer;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerResquest;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Port;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.RouteFixedIp;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Router;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.RouterExternalGatewayInfo;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroup;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroupRule;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Subnet;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.Applications;

/**
 * Openstack Driver which creates security groups and networks.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class OpenStackCloudifyDriver extends BaseProvisioningDriver {

	private static final String MANAGEMENT_PUBLIC_ROUTER_NAME = "management-public-router";
	private static final String DEFAULT_PROTOCOL = "tcp";

	private static final int MANAGEMENT_SHUTDOWN_TIMEOUT = 5; // 5 seconds
	private static final int CLOUD_NODE_STATE_POLLING_INTERVAL = 2000;

	protected static final String OPT_KEY_PAIR = "keyPairName";
	protected static final String OPT_QUANTUM_VERSION = "quantumVersion";
	protected static final String JCLOUDS_ENDPOINT = "jclouds.endpoint";

	private OpenStackNovaClient novaApi;
	private OpenStackQuantumClient quantumApi;

	private SecurityGroupNames securityGroupNames;

	private String managementNetworkName;
	private String applicationNetworkName;
	private NetworkConfiguration networkConfiguration;

	private String applicationName;

	@Override
	public void setConfig(final ComputeDriverConfiguration configuration) throws CloudProvisioningException {
		super.setConfig(configuration);

		final String serviceName;
		if (!this.management) {
			StringTokenizer st = new StringTokenizer(configuration.getServiceName(), ".");
			if (st.countTokens() == 2) {
				applicationName = st.nextToken();
				serviceName = st.nextToken();
			} else {
				applicationName = "default";
				serviceName = st.nextToken();
			}
		} else {
			applicationName = null;
			serviceName = null;
		}

		String managementGroup = cloud.getProvider().getManagementGroup();
		managementGroup = managementGroup == null ? MANAGMENT_MACHINE_PREFIX : managementGroup;

		this.securityGroupNames = new SecurityGroupNames(managementGroup, applicationName, serviceName);

		// Init networks names
		final ManagementNetwork managementNetwork = this.cloud.getCloudNetwork().getManagement();
		final NetworkConfiguration managementNetworkConfig = managementNetwork.getNetworkConfiguration();
		this.managementNetworkName = managementGroup + managementNetworkConfig.getName();
		if (!management) {
			final CloudNetwork cloudNetwork = this.cloud.getCloudNetwork();
			final Map<String, NetworkConfiguration> templates = cloudNetwork.getTemplates();
			if (templates == null || templates.isEmpty()) {
				throw new IllegalStateException("No network template found.");
			}

			// If no template defined in service network, use one by default.
			final ServiceNetwork serviceNetwork = this.configuration.getNetwork();
			if (serviceNetwork == null || StringUtils.isEmpty(serviceNetwork.getTemplate())) {
				// Get the first network found if none specified.
				final String networkTemplateName = templates.keySet().iterator().next();
				this.networkConfiguration = templates.get(networkTemplateName);
			} else {
				this.networkConfiguration = templates.get(serviceNetwork.getTemplate());
			}

			this.applicationNetworkName = this.securityGroupNames.getPrefix() + networkConfiguration.getName();
		}
	}

	private void initManagementSecurityGroups() throws CloudProvisioningException {
		try {
			// ** Clean security groups
			this.cleanAllSecurityGroups();

			// ** Create Cluster security group
			this.createSecurityGroup(this.securityGroupNames.getClusterName());

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

			// ** Create Management security group
			final String managementSecgroupName = this.securityGroupNames.getManagementName();
			final SecurityGroup managementSecurityGroup = this.createSecurityGroup(managementSecgroupName);

			// ** Create Agent security groups
			final String agentSecgroupName = this.securityGroupNames.getAgentName();
			final SecurityGroup agentSecurityGroup = this.createSecurityGroup(agentSecgroupName);

			// Retrieve subnet
			final NetworkConfiguration networkConfiguration =
					this.cloud.getCloudNetwork().getManagement().getNetworkConfiguration();
			List<org.cloudifysource.domain.cloud.network.Subnet> subnets = networkConfiguration.getSubnets();
			if (subnets == null || subnets.size() != 1) {
				throw new CloudProvisioningException("Management network must have one subnet");
			}
			final org.cloudifysource.domain.cloud.network.Subnet mngSubnet = subnets.get(0);

			// ** Create Management rules
			@SuppressWarnings("unchecked")
			final Set<Object> managementPorts = new HashSet<Object>(Arrays.asList(
					agent.getPort(),
					deployer.getPort(),
					deployer.getWebsterPort(),
					discovery.getPort(),
					orchestrator.getPort(),
					usm.getPortRange(),
					"4150-4200" // LUS
			));
			final String managementPortRange = StringUtils.join(managementPorts, ",");
			this.createManagementRule(managementSecurityGroup.getId(), managementPortRange, mngSubnet.getRange());

			// ** Create Agent rules
			@SuppressWarnings("unchecked")
			final Set<Object> agentPorts = new HashSet<Object>(Arrays.asList(
					agent.getPort(),
					deployer.getPort(),
					discovery.getPort(),
					usm.getPortRange()
					));
			for (final FileTransferModes mode : FileTransferModes.values()) {
				agentPorts.add(mode.getDefaultPort());
			}
			final String agentPortRange = StringUtils.join(agentPorts, ",");
			this.createManagementRule(agentSecurityGroup.getId(), agentPortRange, mngSubnet.getRange());

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
			throw new CloudProvisioningException(e);
		}
	}

	private void cleanAllSecurityGroups() throws OpenstackException {
		final String prefix = this.securityGroupNames.getPrefix();
		final List<SecurityGroup> securityGroupsByName = this.quantumApi.getSecurityGroupsByPrefix(prefix);
		for (final SecurityGroup securityGroup : securityGroupsByName) {
			this.quantumApi.deleteSecurityGroup(securityGroup.getId());
		}
	}

	private void createManagementRule(final String targetSecurityGroupId, final String portRangeString,
			final String cidr) throws OpenstackException {

		final PortRange portRange = PortRangeFactory.createPortRange(portRangeString);
		SecurityGroupRule request;
		for (final PortRangeEntry entry : portRange.getRanges()) {
			request = new SecurityGroupRule();
			request.setSecurityGroupId(targetSecurityGroupId);
			request.setDirection("ingress");
			request.setProtocol(DEFAULT_PROTOCOL);
			request.setPortRangeMax(entry.getTo() == null ? entry.getFrom().toString() : entry.getTo().toString());
			request.setPortRangeMin(entry.getFrom().toString());
			if (cidr == null) {
				request.setRemoteIpPrefix("0.0.0.0/0");
			} else {
				request.setRemoteIpPrefix(cidr);
			}
			quantumApi.createSecurityGroupRule(request);
		}
	}

	private SecurityGroup createSecurityGroup(final String secgroupName) throws OpenstackException {
		final SecurityGroup request = new SecurityGroup();
		request.setName(secgroupName);
		request.setDescription("Security groups " + secgroupName);
		return quantumApi.createSecurityGroupsIfNotExist(request);
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
			endpoint = (String) overrides.get(JCLOUDS_ENDPOINT);
		}

		final String quantumVersion = (String) cloudTemplate.getOptions().get(OPT_QUANTUM_VERSION);

		final String cloudImageId = cloudTemplate.getImageId();
		final String region = cloudImageId.split("/")[0];

		final String cloudUser = cloud.getUser().getUser();
		final String password = cloud.getUser().getApiKey();

		final StringTokenizer st = new StringTokenizer(cloudUser, ":");
		final String tenant = st.hasMoreElements() ? (String) st.nextToken() : null;
		final String username = st.hasMoreElements() ? (String) st.nextToken() : null;

		try {
			this.novaApi = new OpenStackNovaClient(endpoint, username, password, tenant, region);
			this.quantumApi = new OpenStackQuantumClient(endpoint, username, password, tenant, region, quantumVersion);
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
			this.createSecurityGroup(this.securityGroupNames.getApplicationName());
			this.createSecurityGroup(this.securityGroupNames.getServiceName());
			this.createSecurityGroup(this.securityGroupNames.getServicePublicName());
			this.createSecurityGroupsRules();

			// Create application/service network
			this.createNetwork();

			final String groupName = this.createNewServerName();
			logger.fine("Starting a new cloud server with group: " + groupName);
			final ComputeTemplate computeTemplate =
					this.cloud.getCloudCompute().getTemplates().get(this.cloudTemplateName);
			final MachineDetails md = this.createServer(groupName, end, computeTemplate);
			return md;
		} catch (final OpenstackException e) {
			throw new CloudProvisioningException("Failed to start cloud machine", e);
		}
	}

	private void createNetwork() throws OpenstackException {

		final String networkName = this.networkConfiguration.getName();

		// Network
		final Network networkRequest = new Network();
		networkRequest.setName(this.securityGroupNames.getPrefix() + networkName);
		networkRequest.setAdminStateUp(true);
		final Network network = quantumApi.createNetworkIfNotExists(networkRequest);

		if (network != null) {
			// Subnet
			final List<org.cloudifysource.domain.cloud.network.Subnet> subnets = this.networkConfiguration.getSubnets();
			for (final org.cloudifysource.domain.cloud.network.Subnet subnetConfig : subnets) {
				final Subnet subnetRequest = this.createSubnetRequest(subnetConfig, network.getId());
				quantumApi.createSubnet(subnetRequest);
			}
		}

	}

	/*********
	 * Looks for a free server name by appending a counter to the pre-calculated server name prefix. If the max counter
	 * value is reached, code will loop back to 0, so that previously used server names will be reused.
	 * 
	 * @return the server name.
	 * @throws CloudProvisioningException
	 *             if no free server name could be found.
	 */
	protected String createNewServerName() throws CloudProvisioningException {

		String serverName = null;
		int attempts = 0;
		boolean foundFreeName = false;

		final List<NovaServer> servers;
		try {
			servers = novaApi.getServers();
		} catch (final OpenstackException e) {
			throw new CloudProvisioningException(e);
		}

		final Set<String> existingNames = new HashSet<String>(servers.size());
		for (final NovaServer sv : servers) {
			existingNames.add(sv.getName());
		}

		while (attempts < MAX_SERVERS_LIMIT) {
			++attempts;
			serverName = serverNamePrefix + counter.incrementAndGet();
			if (!existingNames.contains(serverName)) {
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
		this.createManagementNetwork();

		// launch the management machines
		publishEvent(EVENT_ATTEMPT_START_MGMT_VMS);
		final int numberOfManagementMachines = this.cloud.getProvider().getNumberOfManagementMachines();
		final MachineDetails[] createdMachines = this.doStartManagementMachines(endTime, numberOfManagementMachines);
		publishEvent(EVENT_MGMT_VMS_STARTED);
		return createdMachines;
	}

	private void createManagementNetwork() throws CloudProvisioningException {
		try {
			// Clear existing network
			this.cleanAllNetworks();

			final ManagementNetwork managementNetwork = this.cloud.getCloudNetwork().getManagement();
			final NetworkConfiguration networkConfiguration = managementNetwork.getNetworkConfiguration();

			if (networkConfiguration.getSubnets().size() != 1) {
				throw new IllegalStateException("Management network should have exactly 1 subnet.");
			}

			// Network
			final Network networkRequest = new Network();
			networkRequest.setName(this.managementNetworkName);
			networkRequest.setAdminStateUp(true);
			final Network network = quantumApi.createNetworkIfNotExists(networkRequest);

			// Subnet
			final org.cloudifysource.domain.cloud.network.Subnet subnetConfig =
					networkConfiguration.getSubnets().get(0);
			final Subnet subnetRequest = this.createSubnetRequest(subnetConfig, network.getId());
			final Subnet subnet = quantumApi.createSubnet(subnetRequest);

			// Router
			final String publicNetworkId = quantumApi.getPublicNetworkId();
			final Router request = new Router();
			request.setName(this.securityGroupNames.getPrefix() + MANAGEMENT_PUBLIC_ROUTER_NAME);
			request.setAdminStateUp(true);
			request.setExternalGatewayInfo(new RouterExternalGatewayInfo(publicNetworkId));
			final Router router = quantumApi.createRouter(request);

			// Add interface
			quantumApi.addRouterInterface(router.getId(), subnet.getId());
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}
	}

	private Subnet createSubnetRequest(final org.cloudifysource.domain.cloud.network.Subnet subnetConfig,
			final String networkId) {
		final Subnet subnetRequest = new Subnet();
		subnetRequest.setNetworkId(networkId);
		subnetRequest.setCidr(subnetConfig.getRange());
		subnetRequest.setName(subnetConfig.getName());
		subnetRequest.setEnableDhcp(true);
		subnetRequest.addDnsNameservers("8.8.8.8");
		if (subnetConfig.getOptions().containsKey("gateway")) {
			final String gatewayStr = subnetConfig.getOptions().get("gateway");
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

	private void cleanAllNetworks() throws CloudProvisioningException {
		try {
			// Delete management networks
			final Router router =
					quantumApi.getRouterByName(this.securityGroupNames.getPrefix() + MANAGEMENT_PUBLIC_ROUTER_NAME);
			final Network network = quantumApi.getNetworkByName(this.managementNetworkName);
			if (router != null) {
				quantumApi.deleteRouterInterface(router.getId(), network.getSubnets()[0]);
				quantumApi.deleteRouter(router.getId());
			}
			if (network != null) {
				quantumApi.deleteNetwork(network.getId());
			}

			// Delete remaining application networks
			List<Network> appliNetworks = quantumApi.getNetworkByPrefix(this.securityGroupNames.getPrefix());
			for (Network n : appliNetworks) {
				quantumApi.deleteNetwork(n.getId());

			}
		} catch (final Exception e) {
			throw new CloudProvisioningException(e);
		}
	}

	@Override
	public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
		try {
			final String mngTemplateName = this.cloud.getConfiguration().getManagementMachineTemplate();
			final ComputeTemplate template = this.cloud.getCloudCompute().getTemplates().get(mngTemplateName);
			final List<NovaServer> servers = novaApi.getServersByPrefix(this.serverNamePrefix);

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
		try {
			final Network managementNetwork = quantumApi.getNetworkByName(this.managementNetworkName);

			final NovaServerResquest request = new NovaServerResquest();
			request.setName(serverName);
			request.setKeyName(keyName);
			request.setImageRef(imageId);
			request.setFlavorRef(hardwareId);
			request.addNetworks(managementNetwork.getId());
			if (!management) {
				// Attach application network to the server
				final Network appliNetwork = quantumApi.getNetworkByName(this.applicationNetworkName);
				request.addNetworks(appliNetwork.getId());
			}

			NovaServer newServer = novaApi.createServer(request);
			serverId = newServer.getId();
			newServer = this.waitForServerToBecomeReady(serverId, endTime);

			quantumApi.createAndAssociateFloatingIp(serverId, managementNetwork.getId());

			if (this.management) {
				// Add management secgroup to cloudify management network
				this.addSecurityGroupsToNetwork(serverId, managementNetwork,
						new String[] { this.securityGroupNames.getManagementName() });
			} else {
				// Add agent secgroup to cloudify management network
				this.addSecurityGroupsToNetwork(serverId, managementNetwork, new String[] {
						this.securityGroupNames.getAgentName(),
						this.securityGroupNames.getServicePublicName() });

				// Add cluster, application and service secgroups to the application private network
				final Network appliNetwork = quantumApi.getNetworkByName(this.applicationNetworkName);
				this.addSecurityGroupsToNetwork(serverId, appliNetwork, new String[] {
						this.securityGroupNames.getClusterName(),
						this.securityGroupNames.getApplicationName(),
						this.securityGroupNames.getServiceName() });
			}

			final MachineDetails md = this.createMachineDetails(template, newServer);

			return md;
		} catch (final Exception e) {
			// catch any exception - to prevent a cloud machine leaking.
			logger.log(Level.SEVERE,
					"Cloud machine was started but an error occured during initialization. Shutting down machine", e);
			if (serverId != null) {
				try {
					novaApi.deleteServer(serverId);
				} catch (final OpenstackException e1) {
					throw new CloudProvisioningException(e1);
				}
			}
			throw new CloudProvisioningException(e);
		}
	}

	private void addSecurityGroupsToNetwork(final String serverId, final Network network,
			final String[] securityGroupNames) throws OpenstackException {
		final Port port = quantumApi.getPort(serverId, network.getId());

		final Port updateRequest = new Port();
		updateRequest.setId(port.getId());
		for (final String sgn : securityGroupNames) {
			final SecurityGroup sg = quantumApi.getSecurityGroupsByName(sgn);
			updateRequest.addSecurityGroup(sg.getId());
		}

		quantumApi.updatePort(updateRequest);
	}

	private void createSecurityGroupsRules() throws OpenstackException {
		// Create rules
		final ServiceNetwork network = this.configuration.getNetwork();
		if (network != null) {
			for (final AccessRule accessRule : network.getAccessRules().getIncoming()) {
				this.createAccessRule("ingress", accessRule);
			}
			for (final AccessRule accessRule : network.getAccessRules().getOutgoing()) {
				// If there is egress rules defined. we should delete the openstack default egress rules.
				this.deleteEgressRulesFromSecurityGroup(this.securityGroupNames.getServiceName());
				this.deleteEgressRulesFromSecurityGroup(this.securityGroupNames.getServicePublicName());

				this.createAccessRule("egress", accessRule);
			}
		}
	}

	private void deleteEgressRulesFromSecurityGroup(final String securityGroupName) throws OpenstackException {
		final SecurityGroup securityGroup = quantumApi.getSecurityGroupsByName(securityGroupName);
		final SecurityGroupRule[] securityGroupRules = securityGroup.getSecurityGroupRules();
		for (final SecurityGroupRule rule : securityGroupRules) {
			if ("egress".equals(rule.getDirection())) {
				quantumApi.deleteSecurityGroupRule(rule.getId());
			}
		}
	}

	private void createAccessRule(final String direction, final AccessRule accessRule)
			throws OpenstackException {

		final String securityGroupName = this.securityGroupNames.getServiceName();
		final SecurityGroup securityGroup = quantumApi.getSecurityGroupsByName(securityGroupName);

		// Parse ports
		final PortRange portRange = PortRangeFactory.createPortRange(accessRule.getPortRange());

		String targetSecurityGroupId = securityGroup.getId();
		String ip = "0.0.0.0/0";
		String group = null;

		switch (accessRule.getType()) {
		case PUBLIC:
			// Rules to apply to public network
			final SecurityGroup servicePublicSecgroup =
					quantumApi.getSecurityGroupsByName(this.securityGroupNames.getServicePublicName());
			targetSecurityGroupId = servicePublicSecgroup.getId();
			break;
		case SERVICE:
			// Rules with group filtering
			group = this.securityGroupNames.getServiceName();
			break;
		case APPLICATION:
			// Rules with group filtering
			group = this.securityGroupNames.getApplicationName();
			break;
		case CLUSTER:
			// Rules with group filtering
			group = this.securityGroupNames.getClusterName();
			break;
		case GROUP:
			// Rules with group filtering
			group = accessRule.getTarget();
			break;
		case RANGE:
			// Rules with ip filtering
			if (accessRule.getTarget() == null) {
				throw new IllegalStateException("No IP defined for the 'Range' access rule type :" + accessRule);
			}
			ip = accessRule.getTarget();
			break;
		case PRIVATE:
		default:
			throw new IllegalStateException("Unsupported type of rule '" + accessRule.getType() + "'");
		}

		SecurityGroup existingSecgroup = null;
		if (group != null) {
			existingSecgroup = this.quantumApi.getSecurityGroupsByName(group);
			if (existingSecgroup == null) {
				throw new IllegalStateException("Security group '" + group + "' does not exist.");
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
			quantumApi.createSecurityGroupRule(request);
		}
	}

	private MachineDetails createMachineDetails(final ComputeTemplate template, final NovaServer server)
			throws CloudProvisioningException {
		try {
			final MachineDetails md = this.createMachineDetailsForTemplate(template);

			md.setMachineId(server.getId());
			md.setCloudifyInstalled(false);
			md.setInstallationDirectory(null);
			// md.setInstallationDirectory(template.getRemoteDirectory());
			// md.setRemoteDirectory(remoteDirectory);
			md.setOpenFilesLimit(template.getOpenFilesLimit());
			//
			// md.setInstallerConfigutation(installerConfigutation);
			// md.setKeyFile(keyFile);
			// md.setLocationId(locationId);
			final Network managementNetwork = quantumApi.getNetworkByName(this.managementNetworkName);
			final Port managementPort = quantumApi.getPort(server.getId(), managementNetwork.getId());
			final RouteFixedIp fixedIp = managementPort.getFixedIps().get(0);
			md.setPrivateAddress(fixedIp.getIpAddress());

			md.setPublicAddress(quantumApi.getFloatingIpByPortId(managementPort.getId()));

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
				server = novaApi.getServerDetails(serverId);
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
						this.novaApi.deleteServer(machineDetails.getMachineId());
					} catch (final OpenstackException e) {
						throw new CloudProvisioningException(e);
					}
				}
			}
		}
		throw new CloudProvisioningException(
				"One or more managememnt machines failed. The first encountered error was: "
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
					try {
						quantumApi.deleteFloatingIPByFixedIp(md.getPrivateAddress());
					} catch (final Exception e) {
						logger.warning("Couldn't delete floating IP : " + md.getPublicAddress());
					}
					this.novaApi.deleteServer(md.getMachineId());
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
				logger.warning("Couldn't clean security groups " + this.securityGroupNames.getPrefix() + "*");
			}
		} finally {
			if (this.novaApi != null) {
				this.novaApi.close();
			}
			if (this.quantumApi != null) {
				this.quantumApi.close();
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
			server = novaApi.getServerByIp(serverIp);
		} catch (OpenstackException e) {
			throw new CloudProvisioningException(e);
		}

		if (server != null) {
			logger.info("Found server: " + server.getId() + ". Shutting it down and waiting for shutdown to complete");

			try {
				// Release and delete floating Ip if exists
				final String floatingIp = quantumApi.getFloatingIpByFixedIpAddress(serverIp);
				if (floatingIp != null) {
					try {
						logger.info("Deleting Floating Ip : " + floatingIp);
						quantumApi.deleteFloatingIPByFixedIp(serverIp);
					} catch (Exception e) {
						logger.warning("Couldn't delete floating IP : " + floatingIp);
					}
				}
			} catch (OpenstackException e) {
				logger.log(Level.WARNING, "Could not release floating Ip associated to server " + server.getName()
						+ " (" + server.getId() + ")", e);
			}

			// Delete server
			try {
				novaApi.deleteServer(server.getId());
			} catch (final OpenstackException e) {
				throw new CloudProvisioningException(e);
			}
			this.waitForServerToBeShutdown(server.getId(), duration, unit);
			logger.info("Server: " + server.getId() + " shutdown has finished.");
			stopResult = true;
		} else {
			logger.log(Level.SEVERE,
					"Recieved scale in request for machine with ip "
							+ serverIp
							+ " but this IP could not be found in the Cloud server list");
			stopResult = false;
		}

		return stopResult;
	}

	private void waitForServerToBeShutdown(final String serverId, final long duration, final TimeUnit unit)
			throws CloudProvisioningException, InterruptedException, TimeoutException {
		final long endTime = System.currentTimeMillis() + unit.toMillis(duration);

		while (System.currentTimeMillis() < endTime) {
			final NovaServer server;
			try {
				server = novaApi.getServerDetails(serverId);
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

		throw new TimeoutException("Node failed to reach RUNNING mode in time");
	}

	@Override
	public void onServiceUninstalled(final long duration, final TimeUnit unit) throws InterruptedException,
			TimeoutException, CloudProvisioningException {

		final String ssgName = this.securityGroupNames.getServiceName();
		logger.info("Service '" + ssgName + "'is being uninstall.");
		try {
			final Applications applications = this.admin.getApplications();
			final Application application = applications.getApplication(applicationName);
			if (application == null) {
				logger.info("No remaining services in the application.");
				logger.info("Delete the application security group.");
				final String applicationName = this.securityGroupNames.getApplicationName();
				final SecurityGroup secgroup = this.quantumApi.getSecurityGroupsByName(applicationName);
				if (secgroup != null) {
					quantumApi.deleteSecurityGroup(secgroup.getId());
				}

				logger.info("Delete the network.");
				try {
					quantumApi.deleteNetworkByName(this.applicationNetworkName);
				} catch (final Exception e) {
					logger.warning("Network '" + this.applicationNetworkName + "' was not deleted: " + e.getMessage());
				}
			}

			logger.info("Clean service's security group :" + ssgName + "*");
			final List<SecurityGroup> securityGroups = this.quantumApi.getSecurityGroupsByPrefix(ssgName);
			for (final SecurityGroup securityGroup : securityGroups) {
				this.quantumApi.deleteSecurityGroup(securityGroup.getId());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Fail to clean security group resources of service " + ssgName, e);
		} finally {
			if (this.novaApi != null) {
				this.novaApi.close();
			}
			if (this.quantumApi != null) {
				this.quantumApi.close();
			}
		}

	}
}