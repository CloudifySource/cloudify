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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.openstack.rest.FloatingIp;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Port;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Router;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.RouterInterface;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroup;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroupRule;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Subnet;
import org.springframework.util.StringUtils;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * A client for Openstack Network API.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class OpenStackNetworkClient extends OpenStackBaseClient {

	private static final Logger logger = Logger.getLogger(OpenStackNetworkClient.class.getName());

	private static final byte[] MUTEX_CREATE_SECURITY_GROUPS = new byte[0];
	private static final byte[] MUTEX_CREATE_NETWORK = new byte[0];

	private final String serviceName;
	private final String networkApiVersion;

	public OpenStackNetworkClient(final String endpoint, final String username, final String password,
			final String tenant, final String region)
			throws OpenstackJsonSerializationException {
		this(endpoint, username, password, tenant, region, null, null);
	}

	public OpenStackNetworkClient(final String endpoint, final String username, final String password,
			final String tenant, final String region, final String serviceName)
			throws OpenstackJsonSerializationException {
		this(endpoint, username, password, tenant, region, serviceName, null);
	}

	public OpenStackNetworkClient(final String endpoint, final String username, final String password,
			final String tenant, final String region, final String serviceName, final String networkApiVersion)
			throws OpenstackJsonSerializationException {
		super(endpoint, username, password, tenant, region);
		this.serviceName = StringUtils.isEmpty(serviceName) ? "neutron" : serviceName;
		this.networkApiVersion = StringUtils.isEmpty(networkApiVersion) ? "v2.0" : networkApiVersion;
		logger.info("Openstack " + this.serviceName + " api version: " + this.networkApiVersion);
		// this.initToken();
	}

	@Override
	protected WebResource getWebResource() throws OpenstackException {
		WebResource webResource = super.getWebResource();
		if (this.networkApiVersion != null) {
			webResource = webResource.path(this.networkApiVersion);
		}
		return webResource;
	}

	@Override
	protected String getServiceName() {
		return this.serviceName;
	}

	/**
	 * Retrieve the floating ip address associated to a port.
	 * 
	 * @param portId
	 *            The port id.
	 * @return The floating ip address or <code>null</code> if no floating ip attached.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public FloatingIp getFloatingIpByPortId(final String portId) throws OpenstackException {
		final String response = this.doGet("floatingips", new String[] { "port_id", portId });

		final List<FloatingIp> floatingips = JsonUtils.unwrapRootToList(FloatingIp.class, response);
		if (floatingips != null && !floatingips.isEmpty()) {
			return floatingips.get(0);
		}
		return null;
	}

	/**
	 * Retrieve floating ip by its ip address.
	 * 
	 * @param floatingIp
	 *            The ip address of the floating ip.
	 * @return The floating ip object.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public FloatingIp getFloatingIpByIp(final String floatingIp) throws OpenstackException {
		final String response = this.doGet("floatingips", new String[] { "floating_ip_address", floatingIp });

		final List<FloatingIp> floatingips = JsonUtils.unwrapRootToList(FloatingIp.class, response);
		if (floatingips != null && !floatingips.isEmpty()) {
			return floatingips.get(0);
		}
		return null;
	}

	/**
	 * Allocation a floating ip for a network.
	 * 
	 * @param extNetworkName
	 *            The network name.
	 * @return The allocated floating ip.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public FloatingIp allocateFloatingIp(final String extNetworkName) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Allocate floatingIp from network=" + extNetworkName);
		}
		final Network extNetwork = this.getNetworkByName(extNetworkName);
		if (extNetwork == null) {
			throw new OpenstackException("External network not found: " + extNetworkName);
		}
		final String input = String.format("{\"floatingip\":{\"floating_network_id\":\"%s\"}}", extNetwork.getId());
		final String response = doPost("floatingips", input);
		final FloatingIp floatingIp = JsonUtils.unwrapRootToObject(FloatingIp.class, response);
		return floatingIp;
	}

	/**
	 * Release a floating ip to free a slot in the pool.
	 * 
	 * @param floatingIpId
	 *            The id of the floating ip to free.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public void releaseFloatingIp(final String floatingIpId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Release floatingIp=" + floatingIpId);
		}
		this.doDelete("floatingips/" + floatingIpId, CODE_OK_204);
	}

	/**
	 * Assign a floating ip to a port.
	 * 
	 * @param floatingId
	 *            The floating ip to assign.
	 * @param portId
	 *            The port.
	 * @return The updated floating ip object.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public FloatingIp assignFloatingIp(final String floatingId, final String portId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Assign floating ip for floatingId=" + floatingId + " to portId=" + portId);
		}
		final String input = String.format("{\"floatingip\":{\"port_id\":\"%s\"}}", portId);
		final String response = this.doPut("floatingips/" + floatingId, input);
		final FloatingIp floatingIp = JsonUtils.unwrapRootToObject(FloatingIp.class, response);
		return floatingIp;
	}

	/**
	 * Unassign a floating ip.
	 * 
	 * @param floatingId
	 *            The id of the floating ip to unsassign.
	 * @return The updated floating ip object.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public FloatingIp unassignFloatingIp(final String floatingId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Unassign floating ip for floatingId=" + floatingId);
		}
		final String input = String.format("{\"floatingip\":{\"port_id\":null}}");
		final String response = this.doPut("floatingips/" + floatingId, input);
		final FloatingIp floatingIp = JsonUtils.unwrapRootToObject(FloatingIp.class, response);
		return floatingIp;
	}

	/**
	 * Allocate a floating from the pool and associate it to the given instance and network.
	 * 
	 * @param deviceId
	 *            The server id.
	 * @param networkId
	 *            The network id.
	 * @return The floating ip address.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public String createAndAssociateFloatingIp(final String deviceId, final String networkId)
			throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Create and associate floating ip for devideId=" + deviceId + "/networkId=" + networkId);
		}
		if (networkId == null) {
			throw new OpenstackException("Public network not found for deviceId=" + deviceId);
		}

		final String floatingNetworkId = this.getPublicNetworkId();
		final Port port = this.getPort(deviceId, networkId);
		if (port == null) {
			throw new OpenstackException("Port not found for deviceId=" + deviceId);
		}

		try {
			final String input =
					String.format(
							"{\"floatingip\":{\"floating_network_id\":\"%s\","
									+ "\"port_id\":\"%s\", \"fixed_ip_address\":\"%s\"}}",
							floatingNetworkId, port.getId(), port.getFixedIps().get(0).getIpAddress());
			final String response = this.doPost("floatingips", input);
			final FloatingIp floatingIp = JsonUtils.unwrapRootToObject(FloatingIp.class, response);
			return floatingIp.getFloatingIpAddress();
		} catch (final UniformInterfaceException e) {
			throw this.createOpenstackServerException(e);
		}

	}

	/**
	 * Add interface to a router.
	 * 
	 * @param routerId
	 *            The router id.
	 * 
	 * @param subnetId
	 *            The subnet id.
	 * @return The response.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public RouterInterface addRouterInterface(final String routerId, final String subnetId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Add interface subnetId=" + subnetId + " from routerId=" + routerId);
		}
		final String path = "routers/" + routerId + "/add_router_interface";
		final String input = "{\"subnet_id\":\"" + subnetId + "\"}";
		final String response = this.doPut(path, input);
		final RouterInterface routerInterface = JsonUtils.mapJsonToObject(RouterInterface.class, response);
		return routerInterface;
	}

	/**
	 * Remove an interface from a router.
	 * 
	 * @param routerId
	 *            The router id.
	 * @param subnetId
	 *            The subnet id.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public void deleteRouterInterface(final String routerId, final String subnetId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Remove interface subnetId=" + subnetId + " from routerId=" + routerId);
		}
		final String path = "routers/" + routerId + "/remove_router_interface";
		final String input = "{\"subnet_id\":\"" + subnetId + "\"}";
		this.doPut(path, input);
	}

	/**
	 * Create a router.
	 * 
	 * @param request
	 *            The request.
	 * @return The created router.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public Router createRouter(final Router request) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Create router=" + request);
		}
		final String jsonRequest = JsonUtils.toJson(request);
		final String response = this.doPost("routers", jsonRequest);
		final Router router = JsonUtils.unwrapRootToObject(Router.class, response);
		return router;
	}

	/**
	 * Delete a router.
	 * 
	 * @param routerId
	 *            The if of the router to delete.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public void deleteRouter(final String routerId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Delete routerId=" + routerId);
		}
		this.doDelete("routers/" + routerId, CODE_OK_204);
	}

	/**
	 * Retrieve a list of routers.
	 * 
	 * @return The list of routers.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public List<Router> getRouters() throws OpenstackException {
		final String response = this.doGet("routers");
		final List<Router> routers = JsonUtils.unwrapRootToList(Router.class, response);
		return routers;
	}

	/**
	 * Retrieve a router by name.
	 * 
	 * @param name
	 *            The name of the router.
	 * @return The router matching the given name.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public Router getRouterByName(final String name) throws OpenstackException {
		final String response = this.doGet("routers", new String[] { "name", name });
		final List<Router> routers = JsonUtils.unwrapRootToList(Router.class, response);
		if (routers == null || routers.isEmpty()) {
			return null;
		}
		return routers.get(0);
	}

	/**
	 * Create a network if its not already exists.<br />
	 * To know if a network exists, it will check the network's name.
	 * 
	 * @param request
	 *            The request.
	 * @return The created network.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public Network createNetworkIfNotExists(final Network request) throws OpenstackException {
		if (request.getName() == null) {
			throw new IllegalArgumentException("Network templates should have names.");
		}

		synchronized (MUTEX_CREATE_NETWORK) {
			final Network existingNetwork = this.getNetworkByName(request.getName());
			if (existingNetwork != null) {
				logger.info("Network '" + request.getName() + "' already exists.");
				return existingNetwork;
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Create network=" + request);
			}
			final String json = JsonUtils.toJson(request);
			final String response = this.doPost("networks", json);
			final Network network = JsonUtils.unwrapRootToObject(Network.class, response);
			return network;
		}
	}

	/**
	 * Delete a network.
	 * 
	 * @param networkId
	 *            The id of the network to delete.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public void deleteNetwork(final String networkId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Delete networkId=" + networkId);
		}
		final List<Port> ports = getPortsByNetworkId(networkId);
		for (final Port port : ports) {
			this.deletePort(port.getId());
		}
		this.doDelete("networks/" + networkId, CODE_OK_204);
	}

	/**
	 * Retrieve a network by name.
	 * 
	 * @param networkName
	 *            The name of the network.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public void deleteNetworkByName(final String networkName) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Delete network=" + networkName);
		}
		final Network network = this.getNetworkByName(networkName);
		if (network != null) {
			this.deleteNetwork(network.getId());
		}
	}

	/**
	 * Retrieve a network id which is connected to the external world.
	 * 
	 * @return The public network id or <code>null</code> if not found.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public String getPublicNetworkId() throws OpenstackException {
		final List<Network> networks = this.getPublicNetwork();
		for (final Network network : networks) {
			return network.getId();
		}
		return null;
	}

	/**
	 * Retrieve a network which is connected to the external world.
	 * 
	 * @return The public network.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public List<Network> getPublicNetwork() throws OpenstackException {
		final String response = this.doGet("networks", new String[] { "router:external", "true" });
		final List<Network> networks = JsonUtils.unwrapRootToList(Network.class, response);
		return networks;
	}

	/**
	 * Retrieve a network by id.
	 * 
	 * @param networkId
	 *            The id of the network.
	 * @return The network.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public Network getNetwork(final String networkId) throws OpenstackException {
		final String response = this.doGet("networks/" + networkId);
		final Network network = JsonUtils.unwrapRootToObject(Network.class, response);
		return network;
	}

	/**
	 * Retrieve a network matching the given name.
	 * 
	 * @param networkName
	 *            The name of the looking network.
	 * @return The network.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public Network getNetworkByName(final String networkName) throws OpenstackException {
		final String response = this.doGet("networks", new String[] { "name", networkName });
		final List<Network> networks = JsonUtils.unwrapRootToList(Network.class, response);
		if (networks != null) {
			for (final Network network : networks) {
				if (networkName.equals(network.getName())) {
					return network;
				}

			}
		}
		return null;
	}

	/**
	 * Retrieve networks with a prefix name.
	 * 
	 * @param prefix
	 *            The prefix name.
	 * @return A list of networks matching the prefix.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public List<Network> getNetworkByPrefix(final String prefix) throws OpenstackException {
		final String response = this.doGet("networks");
		final List<Network> list = JsonUtils.unwrapRootToList(Network.class, response);
		final List<Network> networksToReturn = new ArrayList<Network>();
		for (Network network : list) {
			if (network.getName().startsWith(prefix)) {
				networksToReturn.add(network);
			}

		}
		return networksToReturn;
	}

	/**
	 * Retrieve the port which connect a given server to a given network.
	 * 
	 * @param serverId
	 *            The server id.
	 * @param networkId
	 *            The network id.
	 * @return The port or <code>null</code> if none.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public Port getPort(final String serverId, final String networkId) throws OpenstackException {
		final String[] params = new String[] { "device_id", serverId, "network_id", networkId };
		final String response = this.doGet("ports", params);
		final List<Port> ports = JsonUtils.unwrapRootToList(Port.class, response);
		if (ports == null || ports.isEmpty()) {
			return null;
		}
		return ports.get(0);
	}

	/**
	 * Retrieve all ports attached to a network.
	 * 
	 * @param networkId
	 *            The network id.
	 * @return All ports attached to a network.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	private List<Port> getPortsByNetworkId(final String networkId) throws OpenstackException {
		final String[] params = new String[] { "network_id", networkId };
		final String response = this.doGet("ports", params);
		final List<Port> ports = JsonUtils.unwrapRootToList(Port.class, response);
		return ports;
	}

	/**
	 * Retrieve all ports attached to a device.
	 * 
	 * @param deviceId
	 *            The server to request.
	 * @return The list ports attached to the server.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public List<Port> getPortsByDeviceId(final String deviceId) throws OpenstackException {
		final String[] params = new String[] { "device_id", deviceId };
		final String response = this.doGet("ports", params);
		final List<Port> ports = JsonUtils.unwrapRootToList(Port.class, response);
		return ports;
	}

	/**
	 * Create a port.
	 * 
	 * @param request
	 *            The request.
	 * @return The created port.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public Port createPort(final Port request) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Create port=" + request);
		}
		final String jsonRequest = JsonUtils.toJson(request);
		final String response = this.doPost("ports", jsonRequest);
		final Port port = JsonUtils.unwrapRootToObject(Port.class, response);
		return port;
	}

	/**
	 * Update a port (to add security groups for instance).
	 * 
	 * @param request
	 *            The request.
	 * @return The updated port.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public Port updatePort(final Port request) throws OpenstackException {
		final String portId = request.getId();
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Update portId=" + portId);
		}
		try {
			// TODO Should handle the request properly without changing the request object.
			request.setId(null);
			final String jsonRequest = JsonUtils.toJson(request);
			final String response = this.doPut("ports/" + portId, jsonRequest);
			final Port port = JsonUtils.unwrapRootToObject(Port.class, response);
			return port;
		} finally {
			request.setId(portId);
		}
	}

	/**
	 * Delete a port.
	 * 
	 * @param portId
	 *            The id of the port to delete.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public void deletePort(final String portId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Delete portId=" + portId);
		}
		this.doDelete("ports/" + portId, CODE_OK_204);
	}

	/**
	 * Disassociate and release a floating ip which was mapped to a fixed ip address.
	 * 
	 * @param fixedIp
	 *            The fixed ip address.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public void deleteFloatingIPByFixedIp(final String fixedIp) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Delete floatingIp associated to fixedIp=" + fixedIp);
		}
		final String response = this.doGet("floatingips",
				new String[] { "fixed_ip_address", fixedIp });
		final List<FloatingIp> floatingIPs = JsonUtils.unwrapRootToList(FloatingIp.class, response);
		if (floatingIPs != null && floatingIPs.size() != 0) {
			final String floatingIPId = floatingIPs.get(0).getId();
			this.deleteFloatingIP(floatingIPId);
		} else {
			logger.warning("No Floating IP found for fixedIp=" + fixedIp);
		}
	}

	/**
	 * Release a floating ip and return it to the pool.
	 * 
	 * @param floatingIPId
	 *            The floating id to delete.
	 * 
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public void deleteFloatingIP(final String floatingIPId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Delete floatingIp=" + floatingIPId);
		}
		this.doDelete("floatingips/" + floatingIPId, CODE_OK_204);
	}

	/**
	 * Retrieve existing security groups by prefix name.
	 * 
	 * @param prefix
	 *            The prefix.
	 * @return A list of security groups that match the prefix.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public List<SecurityGroup> getSecurityGroupsByPrefix(final String prefix) throws OpenstackException {
		final String response = this.doGet("security-groups");
		final List<SecurityGroup> securityGroupsResponse = JsonUtils.unwrapRootToList(SecurityGroup.class, response);
		final List<SecurityGroup> list = new ArrayList<SecurityGroup>();
		if (securityGroupsResponse != null) {
			for (final SecurityGroup securityGroup : securityGroupsResponse) {
				if (securityGroup.getName().startsWith(prefix)) {
					list.add(securityGroup);
				}
			}
		}
		return list;
	}

	/**
	 * Retrieve existing security groups by name.
	 * 
	 * @param name
	 *            The name.
	 * @return A list of security groups that match the exact given name.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public SecurityGroup getSecurityGroupsByName(final String name) throws OpenstackException {
		final String response = this.doGet("security-groups");
		final List<SecurityGroup> securityGroups = JsonUtils.unwrapRootToList(SecurityGroup.class, response);
		if (securityGroups != null) {
			for (final SecurityGroup securityGroup : securityGroups) {
				if (securityGroup.getName().equals(name)) {
					return securityGroup;
				}
			}
		}
		return null;
	}

	/**
	 * Retrieve security groups by id.
	 * 
	 * @param securityGroupId
	 *            The security group id.
	 * @return The security group.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public SecurityGroup getSecurityGroupsById(final String securityGroupId) throws OpenstackException {
		final String response = this.doGet("security-groups/" + securityGroupId);
		final SecurityGroup securityGroup = JsonUtils.unwrapRootToObject(SecurityGroup.class, response);
		return securityGroup;
	}

	/**
	 * Create a security group if it does not already exists.
	 * 
	 * @param request
	 *            The request.
	 * @return The created security group or <code>null</code> if the security group already exists.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public SecurityGroup createSecurityGroupsIfNotExist(final SecurityGroup request) throws OpenstackException {
		// We can either use synchronize or handle exception to ensure that we create only one group.
		synchronized (MUTEX_CREATE_SECURITY_GROUPS) {
			try {
				final String jsonRequest = JsonUtils.toJson(request);
				final SecurityGroup securityGroup = this.getSecurityGroupsByName(request.getName());
				if (securityGroup != null) {
					logger.info("Security group '" + request.getName() + "' already exists.");
					return null;
				}
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Create security group : " + request.getName());
				}
				final String response = this.doPost("security-groups", jsonRequest);
				final SecurityGroup created = JsonUtils.unwrapRootToObject(SecurityGroup.class, response);
				return created;
			} catch (UniformInterfaceException e) {
				throw this.createOpenstackServerException(e);
			}
		}
	}

	/**
	 * Delete a security group.
	 * 
	 * @param securityGroupId
	 *            The id of the security group to delete
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public void deleteSecurityGroup(final String securityGroupId) throws OpenstackException {
		this.doDelete("security-groups/" + securityGroupId, CODE_OK_204);
	}

	/**
	 * Create a security group rule.
	 * 
	 * @param request
	 *            The request.
	 * @return The security group updated with the new rule.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public SecurityGroupRule createSecurityGroupRule(final SecurityGroupRule request) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Create securityGroupRule=" + request);
		}
		final String jsonRequest = JsonUtils.toJson(request);
		String response = null;
		try {
			response = this.doPost("security-group-rules", jsonRequest);
		} catch (final OpenstackServerException e) {
			if (e.getMessage().contains("already exists")) {
				logger.warning("Rule already exists: " + request);
				return null;

			}
		}
		final SecurityGroupRule created = JsonUtils.unwrapRootToObject(SecurityGroupRule.class, response);
		return created;

	}

	/**
	 * Delete a security group rule.
	 * 
	 * @param securityGroupRuleId
	 *            The id of the rule to delete.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public void deleteSecurityGroupRule(final String securityGroupRuleId) throws OpenstackException {
		this.doDelete("security-group-rules/" + securityGroupRuleId, CODE_OK_204);
	}

	/**
	 * Create a subnet.
	 * 
	 * @param request
	 *            The request.
	 * @return The created subnet.
	 * @throws OpenstackException
	 *             Thrown if something went wrong with the request.
	 */
	public Subnet createSubnet(final Subnet request) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Create subnet=" + request);
		}
		String jsonRequest = JsonUtils.toJson(request);
		// When getwayIp="null" this means that Openstack should not automatically assign a gateway to the subnet.
		jsonRequest = jsonRequest.replaceAll("\"gateway_ip\"\\s*:\\s*\"null\"", "\"gateway_ip\" : null");
		Subnet subnet = null;
		try {
			final String response = this.doPost("subnets", jsonRequest);
			subnet = JsonUtils.unwrapRootToObject(Subnet.class, response);
		} catch (final UniformInterfaceException e) {
			throw this.createOpenstackServerException(e);
		}
		return subnet;
	}

	/**
	 * Get all subnet of a given network.
	 * 
	 * @param networkId
	 *            The network id.
	 * @return A list of subnets belonging to a network.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public List<Subnet> getSubnetsByNetworkId(final String networkId) throws OpenstackException {
		final String response = this.doGet("subnets", new String[] { "network_id", networkId });
		final List<Subnet> subnets = JsonUtils.unwrapRootToList(Subnet.class, response);
		return subnets;
	}

	/**
	 * Get all subnet of a given network.
	 * 
	 * @param networkName
	 *            The network name.
	 * @return A list of subnets belonging to a network.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public List<Subnet> getSubnetsByNetworkName(final String networkName) throws OpenstackException {
		final Network network = this.getNetworkByName(networkName);
		if (network != null) {
			return this.getSubnetsByNetworkId(network.getId());
		}
		return null;
	}
}
