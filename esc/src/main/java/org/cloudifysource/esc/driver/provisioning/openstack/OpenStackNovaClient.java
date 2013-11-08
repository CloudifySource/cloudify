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

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServer;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerAddress;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.NovaServerResquest;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * A client for Openstack Nova.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public class OpenStackNovaClient extends OpenStackBaseClient {

	private static final int RESOURCE_NOT_FOUND_STATUS = 404;
	private static final Logger logger = Logger.getLogger(OpenStackNovaClient.class.getName());

	public OpenStackNovaClient() {
		super();
	}

	public OpenStackNovaClient(final String endpoint, final String username, final String password,
			final String tenant, final String region) throws OpenstackJsonSerializationException {
		super(endpoint, username, password, tenant, region);
	}

	@Override
	protected String getServiceName() {
		return "nova";
	}

	/**
	 * Create a new VM instance.
	 * 
	 * @param request
	 *            The request.
	 * @return The server details.
	 * @throws CloudProvisioningException
	 *             If the instance fails to be created.
	 * @throws OpenstackJsonSerializationException
	 *             If a serialization issue occurs with Openstack request/response.
	 */
	public NovaServer createServer(final NovaServerResquest request) throws CloudProvisioningException,
			OpenstackJsonSerializationException {
		final WebResource webResource = this.getWebResource();
		final String computeRequest = JsonUtils.toJson(request, true);

		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "Request=createServer: " + computeRequest);
		}

		try {
			final String response = webResource.path("servers")
					.type(MediaType.APPLICATION_JSON_TYPE)
					.accept(MediaType.APPLICATION_JSON)
					.header("X-Auth-Token", this.getTokenId())
					.post(String.class, computeRequest);

			final NovaServer nsr = JsonUtils.unwrapRootToObject(NovaServer.class, response);
			return nsr;
		} catch (Exception e) {
			throw new CloudProvisioningException("Could not create a new server", e);
		}
	}

	/**
	 * List existing servers.
	 * 
	 * @return A list of existing servers.
	 * @throws CloudProvisioningException
	 *             If an error occurs with Openstack server.
	 * @throws OpenstackJsonSerializationException
	 *             If a serialization issue occurs with Openstack request/response.
	 */
	public List<NovaServer> getServers() throws CloudProvisioningException, OpenstackJsonSerializationException {
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "Request=getServerDetails");
		}

		final WebResource webResource = this.getWebResource();
		final String response = webResource.path("servers")
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId())
				.get(String.class);

		final List<NovaServer> list = JsonUtils.unwrapRootToList(NovaServer.class, response);
		return list;
	}

	/**
	 * Get a list of instances that match the prefix name.
	 * 
	 * @param prefix
	 *            The prefix to be match.
	 * @return A list of instances prefixed by the given name.
	 * @throws CloudProvisioningException
	 *             If an error occurs with Openstack server.
	 * @throws OpenstackJsonSerializationException
	 *             If a serialization issue occurs with Openstack request/response.
	 * @throws OpenstackServerException
	 *             If an error occurs when requesting Openstack server.
	 */
	public List<NovaServer> getServersByPrefix(final String prefix) throws CloudProvisioningException,
			OpenstackJsonSerializationException, OpenstackServerException {
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "Request=getServerWithName");
		}

		final WebResource webResource = this.getWebResource();
		try {
			final String response = webResource.path("servers")
					.queryParam("name", prefix)
					.type(MediaType.APPLICATION_JSON_TYPE)
					.accept(MediaType.APPLICATION_JSON)
					.header("X-Auth-Token", this.getTokenId())
					.get(String.class);

			final List<NovaServer> servers = JsonUtils.unwrapRootToList(NovaServer.class, response);
			final List<NovaServer> detailServers = new ArrayList<NovaServer>(servers.size());
			for (final NovaServer sv : servers) {
				final NovaServer serverDetails = this.getServerDetails(sv.getId());
				detailServers.add(serverDetails);
			}
			return detailServers;
		} catch (final UniformInterfaceException e) {
			if (RESOURCE_NOT_FOUND_STATUS == e.getResponse().getStatus()) {
				return null;
			}
			throw this.createOpenstackServerException(e);
		}
	}

	/**
	 * Get an instance by ip.
	 * 
	 * @param serverIp
	 *            The ip of the server to get.
	 * @return The server instance that match the ip.
	 * @throws CloudProvisioningException
	 *             If an error occurs with Openstack server.
	 * @throws OpenstackServerException
	 *             If an error occurs when requesting Openstack server.
	 */
	public NovaServer getServerByIp(final String serverIp) throws CloudProvisioningException,
			OpenstackJsonSerializationException {
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "Request=getServerWithIp");
		}

		final List<NovaServer> servers = this.getServers();

		for (final NovaServer server : servers) {
			final NovaServer serverDetails = this.getServerDetails(server.getId());
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
		return null;
	}

	/**
	 * Retrieve server's details.
	 * 
	 * @param serverId
	 *            The id of the server.
	 * @return An instance of the server with all its details.
	 * @throws CloudProvisioningException
	 *             If an error occurs with Openstack server.
	 * @throws OpenstackJsonSerializationException
	 *             If a serialization issue occurs with Openstack request/response.
	 */
	public NovaServer getServerDetails(final String serverId)
			throws CloudProvisioningException, OpenstackJsonSerializationException {

		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "Request=getServerDetails: " + serverId);
		}

		final WebResource webResource = this.getWebResource();
		final String response;
		try {
			response = webResource.path("servers/" + serverId)
					.type(MediaType.APPLICATION_JSON_TYPE)
					.accept(MediaType.APPLICATION_JSON)
					.header("X-Auth-Token", this.getTokenId())
					.get(String.class);
		} catch (final UniformInterfaceException e) {
			if (RESOURCE_NOT_FOUND_STATUS == e.getResponse().getStatus()) {
				return null;
			}
			throw new CloudProvisioningException(e);
		}

		final NovaServer nsr = JsonUtils.unwrapRootToObject(NovaServer.class, response);
		return nsr;
	}

	/**
	 * Terminate a server instance in Openstack.
	 * 
	 * @param serverId
	 *            The server id tto terminate.
	 * @throws CloudProvisioningException
	 *             If the service's endpoint has not been found in Openstack service's catalog.
	 * @throws OpenstackJsonSerializationException
	 */
	public void deleteServer(final String serverId) throws CloudProvisioningException,
			OpenstackJsonSerializationException {
		final WebResource webResource = this.getWebResource();
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "Request=deleteServer: " + serverId);
		}
		webResource.path("servers/" + serverId)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.accept(MediaType.APPLICATION_JSON)
				.header("X-Auth-Token", this.getTokenId()).delete();
	}

}
