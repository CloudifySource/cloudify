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

import javax.ws.rs.core.MediaType;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.FloatingIp;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.FloatingIpResponse;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.FloatingIpsResponse;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Network;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NetworksResponse;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Port;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.PortResponse;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.PortsResponse;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroup;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroupResponse;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroupRulesRequest;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroupsRequest;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SecurityGroupsResponse;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.Subnet;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.SubnetsResponse;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.UpdatePortRequest;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * A client for Openstack Quantum.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class OpenStackQuantumClient extends OpenStackBaseClient {

	private static final Logger logger = Logger.getLogger(OpenStackQuantumClient.class.getName());

	private static final int OK_STATUS = 204;
	private static final int CONFLICT_STATUS = 409;
	private static final int FLOATING_IP_EXCEEDED_STATUS = 409;

	private static final byte[] MUTEX_CREATE_SECURITY_GROUPS = new byte[0];

	private String quantumVersion;

	public OpenStackQuantumClient(final String endpoint, final String username, final String password,
			final String tenant, final String region, final String quantumVersion) {
		super(endpoint, username, password, tenant, region);
		this.quantumVersion = quantumVersion;
		logger.info("Openstack quantum version: " + this.quantumVersion);
	}

	@Override
	protected WebResource getWebResource() throws CloudProvisioningException {
		WebResource webResource = super.getWebResource();
		if (this.quantumVersion != null) {
			webResource = webResource.path(this.quantumVersion);
		}
		return webResource;
	}

	@Override
	protected String getServiceName() {
		return "quantum";
	}

	/**
	 * Retrieve the floating ip address associated with a fixed ip address.
	 * 
	 * @param fixedIpAddress
	 *            The fixed ip address.
	 * @return The associated floating ip address or <code>null</code> if no floating ip attached.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public String getFloatingIpByFixedIpAddress(final String fixedIpAddress) throws CloudProvisioningException {
		final String response = this.getWebResource().path("floatingips")
				.queryParam("fixed_ip_address", fixedIpAddress)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.get(String.class);
		final FloatingIpsResponse floatingips = JsonUtils.mapJsonToObject(FloatingIpsResponse.class, response);
		final List<FloatingIp> list = floatingips.getFloatingips();
		if (list != null && !list.isEmpty()) {
			return list.get(0).getFloatingIpAddress();
		}
		return null;
	}

	/**
	 * Retrieve the floating ip address associated to a port.
	 * 
	 * @param portId
	 *            The port id.
	 * @return The floating ip address or <code>null</code> if no floating ip attached.
	 * @throws UniformInterfaceException
	 *             If an error occurs with Openstack server.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public String getFloatingIpByPortId(final String portId)
			throws UniformInterfaceException, CloudProvisioningException {
		final String response = this.getWebResource().path("floatingips")
				.queryParam("port_id", portId)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.get(String.class);
		final FloatingIpsResponse floatingips = JsonUtils.mapJsonToObject(FloatingIpsResponse.class, response);
		final List<FloatingIp> list = floatingips.getFloatingips();
		if (list != null && !list.isEmpty()) {
			return list.get(0).getFloatingIpAddress();
		}
		return null;
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
	 *             If the server or the network could not be found.
	 * @throws UniformInterfaceException
	 *             If an error occurs with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public String createAndAssociateFloatingIp(final String deviceId, final String networkId)
			throws OpenstackException, UniformInterfaceException, CloudProvisioningException {
		if (networkId == null) {
			throw new OpenstackException("Public network not found for deviceId=" + deviceId);
		}

		final String floatingNetworkId = this.getPublicNetworkId();
		final Port port = this.getPort(deviceId, networkId);
		if (port == null) {
			throw new OpenstackException("Port not found for deviceId=" + deviceId);
		}

		try {
			final String input = String.format("{\"floatingip\":{\"floating_network_id\":\"%s\",\"port_id\":\"%s\"}}",
					floatingNetworkId, port.getId());

			if (logger.isLoggable(Level.FINER)) {
				logger.finer("Requesting creation and association request=" + input);
			}

			final String response = this.getWebResource().path("floatingips")
					.type(MediaType.APPLICATION_JSON_TYPE)
					.accept(MediaType.APPLICATION_JSON)
					.header("X-Auth-Token", this.getTokenId())
					.post(String.class, input);

			if (logger.isLoggable(Level.FINER)) {
				logger.finer("Response creation and association response=" + response);
			}

			final FloatingIpResponse floatingIpResponse = JsonUtils.mapJsonToObject(FloatingIpResponse.class, response);
			return floatingIpResponse.getFloatingip().getFloatingIpAddress();
		} catch (final UniformInterfaceException e) {
			final ClientResponse response = e.getResponse();
			// Cannot create a floating Ip. The pool is full.
			if (FLOATING_IP_EXCEEDED_STATUS == response.getStatus()) {
				throw new OpenstackException("Could not create Floating Ip. Quota exceeded.");
			}
			throw new OpenstackException(
					"Couldn't create/associate floating Ip (status=" + response.getStatus() + ")", e);
		}

	}

	/**
	 * Retrieve a network id which is connected to the external world.
	 * 
	 * @return The public network id or <code>null</code> if not found.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public String getPublicNetworkId() throws UniformInterfaceException, CloudProvisioningException {
		final NetworksResponse networksResponse = this.getPublicNetwork();
		for (final Network network : networksResponse.getNetworks()) {
			return network.getId();
		}
		return null;
	}

	/**
	 * Retrieve a network which is connected to the external world.
	 * 
	 * @return The public network.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public NetworksResponse getPublicNetwork() throws UniformInterfaceException, CloudProvisioningException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Requesting networks list");
		}

		final String response = this.getWebResource().path("networks")
				.queryParam("router:external", "true")
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId()).get(String.class);

		final NetworksResponse networkResponse = JsonUtils.mapJsonToObject(NetworksResponse.class, response);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Response for networks list: " + networkResponse);
		}
		return networkResponse;
	}

	/**
	 * Retrieve a network matching the given name.
	 * 
	 * @param networkName
	 *            The name of the looking network.
	 * @return The network.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public Network getNetworkByName(final String networkName)
			throws UniformInterfaceException, CloudProvisioningException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Requesting networks list");
		}

		final String response = this.getWebResource().path("networks")
				.queryParam("name", networkName)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId()).get(String.class);

		final NetworksResponse networkResponse = JsonUtils.mapJsonToObject(NetworksResponse.class, response);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Response for networks list: " + networkResponse);
		}

		if (networkResponse.getNetworks().isEmpty()) {
			return null;
		} else {
			return networkResponse.getNetworks().get(0);
		}
	}

	/**
	 * Retrieve the port which connect a given server to a given network.
	 * 
	 * @param serverId
	 *            The server id.
	 * @param networkId
	 *            The network id.
	 * @return The port or <code>null</code> if none.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public Port getPort(final String serverId, final String networkId)
			throws UniformInterfaceException, CloudProvisioningException {
		final String response = this.getWebResource().path("ports")
				.queryParam("device_id", serverId)
				.queryParam("network_id", networkId)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId()).get(String.class);
		final PortsResponse portsResponse = JsonUtils.mapJsonToObject(PortsResponse.class, response);
		return portsResponse.getPorts().get(0);
	}

	/**
	 * Update a port (to add security groups for instance).
	 * 
	 * @param request
	 *            The request.
	 * @return The updated port.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public Port updatePort(final UpdatePortRequest request) throws UniformInterfaceException,
			CloudProvisioningException {
		final String response = this.getWebResource().path("ports/" + request.getId())
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.put(String.class, request.computeRequest());
		final PortResponse portResponse = JsonUtils.mapJsonToObject(PortResponse.class, response);
		return portResponse.getPort();
	}

	/**
	 * Disassociate and release a floating ip which was mapped to a fixed ip address.
	 * 
	 * @param fixedIp
	 *            The fixed ip address.
	 * @throws OpenstackException
	 *             If the floating ip could not be released.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public void deleteFloatingIPByFixedIp(final String fixedIp)
			throws OpenstackException, UniformInterfaceException, CloudProvisioningException {
		final String response = this.getWebResource().path("floatingips")
				.queryParam("fixed_ip_address", fixedIp)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.get(String.class);
		final FloatingIpsResponse floatingIPsResponse = JsonUtils.mapJsonToObject(FloatingIpsResponse.class, response);

		if (floatingIPsResponse != null && floatingIPsResponse.getFloatingips() != null
				&& floatingIPsResponse.getFloatingips().size() != 0) {
			final String floatingIPId = floatingIPsResponse.getFloatingips().get(0).getId();
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
	 *             If the floating ip could not be released.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public void deleteFloatingIP(final String floatingIPId)
			throws OpenstackException, UniformInterfaceException, CloudProvisioningException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Deleting floating id=" + floatingIPId);
		}

		final ClientResponse response = this.getWebResource().path("floatingips/" + floatingIPId)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.delete(ClientResponse.class);

		if (OK_STATUS != response.getStatus()) {
			throw new OpenstackException("" + response.getStatus());
		}

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Deleted floating id=" + floatingIPId);
		}
	}

	/**
	 * Retrieve existing security groups by prefix name.
	 * 
	 * @param prefix
	 *            The prefix.
	 * @return A list of security groups that match the prefix.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public List<SecurityGroup> getSecurityGroupsByPrefix(final String prefix)
			throws UniformInterfaceException, CloudProvisioningException {
		final String response = this.getWebResource().path("security-groups")
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.get(String.class);
		final SecurityGroupsResponse securityGroupsResponse =
				JsonUtils.mapJsonToObject(SecurityGroupsResponse.class, response);
		final List<SecurityGroup> list = new ArrayList<SecurityGroup>();
		for (final SecurityGroup securityGroup : securityGroupsResponse.getSecurityGroups()) {
			if (securityGroup.getName().startsWith(prefix)) {
				list.add(securityGroup);
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
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public SecurityGroup getSecurityGroupsByName(final String name) throws UniformInterfaceException,
			CloudProvisioningException {
		final String response = this.getWebResource().path("security-groups")
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.get(String.class);
		final SecurityGroupsResponse securityGroupsResponse =
				JsonUtils.mapJsonToObject(SecurityGroupsResponse.class, response);
		for (final SecurityGroup securityGroup : securityGroupsResponse.getSecurityGroups()) {
			if (securityGroup.getName().equals(name)) {
				return securityGroup;
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
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public SecurityGroup getSecurityGroupsById(final String securityGroupId) throws UniformInterfaceException,
			CloudProvisioningException {
		final String response = this.getWebResource().path("security-groups")
				.queryParam("id", securityGroupId)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.get(String.class);

		final SecurityGroupsResponse securityGroupsResponse =
				JsonUtils.mapJsonToObject(SecurityGroupsResponse.class, response);

		List<SecurityGroup> secgroups = securityGroupsResponse.getSecurityGroups();
		return secgroups.isEmpty() ? null : secgroups.get(0);
	}

	/**
	 * Create a security group if it does not already exists.
	 * 
	 * @param request
	 *            The request.
	 * @return The created security group.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public SecurityGroup createSecurityGroupsIfNotExist(final SecurityGroupsRequest request)
			throws UniformInterfaceException, CloudProvisioningException {
		// We can either use synchronize or handle exception to ensure that we create only one group.
		synchronized (MUTEX_CREATE_SECURITY_GROUPS) {
			try {
				final SecurityGroup securityGroup = this.getSecurityGroupsByName(request.getName());
				if (securityGroup != null) {
					logger.info("Security group '" + request.getName() + "' already exists.");
					return securityGroup;
				}
				logger.info("Create security group : " + request.getName());
				final String response = this.getWebResource().path("security-groups")
						.type(MediaType.APPLICATION_JSON_TYPE)
						.accept(MediaType.APPLICATION_JSON)
						.header("X-Auth-Token", this.getTokenId())
						.post(String.class, request.computeRequest());

				return JsonUtils.mapJsonToObject(SecurityGroupResponse.class, response).getSecurityGroup();
			} catch (UniformInterfaceException e) {
				if (CONFLICT_STATUS == e.getResponse().getStatus()) {
					throw new CloudProvisioningException("Quota for security group might be exceeded.", e);
				}
				throw e;
			}
		}
	}

	/**
	 * Delete a security group.
	 * 
	 * @param securityGroupId
	 *            The id of the security group to delete
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 * @throws OpenstackException
	 *             If the security group has not been deleted.
	 */
	public void deleteSecurityGroup(final String securityGroupId)
			throws UniformInterfaceException, CloudProvisioningException, OpenstackException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Deleting security group id=" + securityGroupId);
		}

		final ClientResponse response = this.getWebResource().path("security-groups/" + securityGroupId)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.delete(ClientResponse.class);

		if (OK_STATUS != response.getStatus()) {
			throw new OpenstackException("" + response.getStatus());
		}

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Deleted security group id=" + securityGroupId);
		}
	}

	/**
	 * Create a security group rule.
	 * 
	 * @param request
	 *            The request.
	 * @return The security group updated with the new rule.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public SecurityGroup createSecurityGroupRules(final SecurityGroupRulesRequest request)
			throws UniformInterfaceException, CloudProvisioningException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Create security group rule " + request);
		}
		try {
			final String response = this.getWebResource().path("security-group-rules")
					.type(MediaType.APPLICATION_JSON_TYPE)
					.accept(MediaType.APPLICATION_JSON)
					.header("X-Auth-Token", this.getTokenId())
					.post(String.class, request.computeRequest());
			final SecurityGroupResponse securityGroup =
					JsonUtils.mapJsonToObject(SecurityGroupResponse.class, response);
			return securityGroup.getSecurityGroup();

		} catch (final UniformInterfaceException e) {
			if (CONFLICT_STATUS == e.getResponse().getStatus()) {
				logger.warning("Rule already exists or is conflincting with another one: " + request);
				return this.getSecurityGroupsById(request.getSecurityGroupId());
			} else {
				throw e;
			}
		}
	}

	/**
	 * Delete a security group rule.
	 * 
	 * @param securityGroupRuleId
	 *            The id of the rule to delete.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 * @throws OpenstackException
	 *             If an error occurs and the rule has not been deleted.
	 */
	public void deleteSecurityGroupRule(final String securityGroupRuleId)
			throws UniformInterfaceException, CloudProvisioningException, OpenstackException {
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Deleting security group rule id=" + securityGroupRuleId);
		}

		final ClientResponse response = this.getWebResource().path("security-group-rules/" + securityGroupRuleId)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.delete(ClientResponse.class);

		if (OK_STATUS != response.getStatus()) {
			throw new OpenstackException("" + response.getStatus());
		}

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Deleted security group id=" + securityGroupRuleId);
		}

	}

	/**
	 * Get all subnet of a given network.
	 * 
	 * @param networkId
	 *            The network id.
	 * @return A list of subnets belonging to a network.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public List<Subnet> getSubnetsByNetworkId(final String networkId)
			throws UniformInterfaceException, CloudProvisioningException {
		final String response = this.getWebResource().path("subnets")
				.queryParam("network_id", networkId)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.get(String.class);

		final SubnetsResponse subnets = JsonUtils.mapJsonToObject(SubnetsResponse.class, response);
		return subnets.getSubnets();
	}

	/**
	 * Get all subnet of a given network.
	 * 
	 * @param networkName
	 *            The network name.
	 * @return A list of subnets belonging to a network.
	 * @throws UniformInterfaceException
	 *             If something goes wrong with the request.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 */
	public List<Subnet> getSubnetsByNetworkName(final String networkName) throws UniformInterfaceException,
			CloudProvisioningException {
		final Network network = this.getNetworkByName(networkName);
		if (network != null) {
			return this.getSubnetsByNetworkId(network.getId());
		}
		return null;
	}

}
