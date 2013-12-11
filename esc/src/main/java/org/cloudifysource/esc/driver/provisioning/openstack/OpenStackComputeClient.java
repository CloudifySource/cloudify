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

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServer;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerAddress;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerResquest;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerSecurityGroup;

/**
 * A client for Openstack Nova.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class OpenStackComputeClient extends OpenStackBaseClient {

	private static final int RESOURCE_NOT_FOUND_STATUS = 404;
	private static final Logger logger = Logger.getLogger(OpenStackComputeClient.class.getName());

	private String serviceName;

	public OpenStackComputeClient() {
		super();
	}

	public OpenStackComputeClient(final String endpoint, final String username, final String password,
			final String tenant, final String region) throws OpenstackJsonSerializationException {
		this(endpoint, username, password, tenant, region, null);
	}

	public OpenStackComputeClient(final String endpoint, final String username, final String password,
			final String tenant, final String region, final String serviceName)
			throws OpenstackJsonSerializationException {
		super(endpoint, username, password, tenant, region);
		this.serviceName = serviceName;
		// this.initToken();
	}

	@Override
	protected String getServiceName() {
		return this.serviceName;
	}

	@Override
	protected String getServiceType() {
		return "compute";
	}

	/**
	 * Create a new VM instance.
	 * 
	 * @param request
	 *            The request.
	 * @return The server details.
	 * @throws OpenstackException
	 *             Thrown when a problem occurs with the request.
	 */
	public NovaServer createServer(final NovaServerResquest request) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Launch instance=" + request);
		}
		final String computeRequest = JsonUtils.toJson(request, false);
		final String response = this.doPost("servers", computeRequest);
		final NovaServer nsr = JsonUtils.unwrapRootToObject(NovaServer.class, response);
		return nsr;
	}

	/**
	 * List existing servers.
	 * 
	 * @return A list of existing servers.
	 * @throws OpenstackException
	 *             Thrown when a problem occurs with the request.
	 */
	public List<NovaServer> getServers() throws OpenstackException {
		final String response = this.doGet("servers");
		final List<NovaServer> list = JsonUtils.unwrapRootToList(NovaServer.class, response);
		return list;
	}

	/**
	 * Get a list of instances that match the prefix name.
	 * 
	 * @param prefix
	 *            The prefix to be match.
	 * @return A list of instances prefixed by the given name.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public List<NovaServer> getServersByPrefix(final String prefix) throws OpenstackException {
		final String response;
		try {
			response = this.doGet("servers", new String[] { "name", prefix });
		} catch (OpenstackServerException e) {
			if (RESOURCE_NOT_FOUND_STATUS == e.getStatusCode()) {
				return null;
			}
			throw e;
		}

		final List<NovaServer> servers = JsonUtils.unwrapRootToList(NovaServer.class, response);
		final List<NovaServer> detailServers = new ArrayList<NovaServer>(servers.size());
		for (final NovaServer sv : servers) {
			final NovaServer serverDetails = this.getServerDetails(sv.getId());
			detailServers.add(serverDetails);
		}
		return detailServers;
	}

	/**
	 * Get an instance by ip.
	 * 
	 * @param serverIp
	 *            The ip of the server to get.
	 * @return The server instance that match the ip.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public NovaServer getServerByIp(final String serverIp) throws OpenstackException {
		final List<NovaServer> servers = this.getServers();

		for (final NovaServer server : servers) {
			final NovaServer serverDetails = this.getServerDetails(server.getId());
			if (serverDetails != null) {
				final List<NovaServerAddress> addresses = serverDetails.getAddresses();
				if (addresses != null) {
					for (final NovaServerAddress novaServerAddress : addresses) {
						final String addr = novaServerAddress.getAddr();
						if (StringUtils.equals(addr, serverIp)) {
							return serverDetails;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get an instance by ip and security group name.
	 * 
	 * @param serverIp
	 *            The ip of the server.
	 * @param secgroupName
	 *            The security group name which must be present in the server.
	 * @return The server instance which match the request.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public NovaServer getServerByIpAndSecurityGroup(final String serverIp, final String secgroupName)
			throws OpenstackException {
		final List<NovaServer> servers = this.getServers();

		for (final NovaServer server : servers) {
			final NovaServer serverDetails = this.getServerDetails(server.getId());
			if (serverDetails != null) {
				final List<NovaServerAddress> addresses = serverDetails.getAddresses();
				if (addresses != null) {
					for (final NovaServerAddress novaServerAddress : addresses) {
						final String addr = novaServerAddress.getAddr();
						if (StringUtils.equals(addr, serverIp)) {
							for (final NovaServerSecurityGroup secgroup : serverDetails.getSecurityGroups()) {
								if (secgroup.getName().equals(secgroupName)) {
									return serverDetails;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Retrieve server's details.
	 * 
	 * @param serverId
	 *            The id of the server.
	 * @return An instance of the server with all its details.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public NovaServer getServerDetails(final String serverId) throws OpenstackException {
		final String response;
		try {
			response = doGet("servers/" + serverId);
		} catch (final OpenstackServerException e) {
			if (RESOURCE_NOT_FOUND_STATUS == e.getStatusCode()) {
				return null;
			}
			throw e;
		}

		final NovaServer nsr = JsonUtils.unwrapRootToObject(NovaServer.class, response);
		return nsr;
	}

	/**
	 * Terminate a server instance in Openstack.
	 * 
	 * @param serverId
	 *            The server id tto terminate.
	 * @throws OpenstackException
	 *             Thrown when something went wrong with the request.
	 */
	public void deleteServer(final String serverId) throws OpenstackException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Terminate serverId=" + serverId);
		}
		this.doDelete("servers/" + serverId, CODE_OK_204);
	}
}
