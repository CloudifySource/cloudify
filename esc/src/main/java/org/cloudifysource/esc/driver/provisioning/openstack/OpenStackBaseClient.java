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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.TokenAccess;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.TokenServiceCatalog;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.TokenServiceCatalogEndpoint;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

/**
 * A base class for openstack clients.<br />
 * It handle tokens.
 * 
 * @author victor
 * @since 2.7.0
 * 
 */
public abstract class OpenStackBaseClient {

	private static final long TOKEN_TIMEOUT_MARGING = 60000L;

	private static final Logger logger = Logger.getLogger(OpenStackBaseClient.class.getName());

	private String endpoint;
	private String username;
	private String password;
	private String tenant;

	private TokenAccess token;
	private String region;

	private Client serviceClient;
	private WebResource serviceWebResource;

	public OpenStackBaseClient() {
	}

	public OpenStackBaseClient(final String endpoint, final String username, final String password,
			final String tenant, final String region) throws OpenstackJsonSerializationException {
		this.endpoint = endpoint;
		this.username = username;
		this.password = password;
		this.tenant = tenant;
		this.region = region;
		this.initToken();
	}

	/**
	 * Destroy the client. <br />
	 */
	public void close() {
		serviceClient.destroy();
	}

	private String getEndpoint(final String endpointType) {
		for (TokenServiceCatalog tsc : this.token.getServiceCatalog()) {
			if (endpointType.equals(tsc.getName())) {
				for (TokenServiceCatalogEndpoint endpoint : tsc.getEndpoints()) {
					if (this.region.equals(endpoint.getRegion())) {
						return endpoint.getPublicURL();
					}
				}
			}
		}
		return null;
	}

	private void renewTokenIfNeeded() throws OpenstackJsonSerializationException {
		if (this.isTokenExpiredSoon()) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Token expired. Request a new token");
			}
			this.initToken();
		}
	}

	private boolean isTokenExpiredSoon() {
		if (token == null) {
			return true;
		}
		long current = System.currentTimeMillis() + TOKEN_TIMEOUT_MARGING;
		long tokenExpires = token.getToken().getExpires().getTime();
		return current > tokenExpires;
	}

	private void initToken() throws OpenstackJsonSerializationException {
		final Client client = Client.create();
		try {
			logger.info("Request openstack " + this.getServiceName() + " new token.");
			final WebResource webResource = client.resource(this.endpoint);
			final String tokenJsonRequest = "{\"auth\":{\"passwordCredentials\":"
					+ "{\"username\": \"%s\", \"password\":\"%s\"}, \"tenantName\":\"%s\"}}";
			final String input = String.format(tokenJsonRequest, username, password, tenant);
			final String response = webResource.path("tokens").accept(MediaType.APPLICATION_JSON)
					.type(MediaType.APPLICATION_JSON_TYPE).post(String.class, input);
			this.token = JsonUtils.unwrapRootToObject(TokenAccess.class, response, true);
		} finally {
			if (client != null) {
				client.destroy();
			}
		}
	}

	/**
	 * Return the WebResource pre configured with the endpoint.
	 * 
	 * @return The pre configured WebResource.
	 * @throws CloudProvisioningException
	 *             If the WebResource fails to initialize.
	 * @throws OpenstackJsonSerializationException
	 */
	protected WebResource getWebResource() throws CloudProvisioningException, OpenstackJsonSerializationException {
		if (serviceWebResource == null) {
			this.renewTokenIfNeeded();
			final String endpoint = this.getEndpoint(this.getServiceName());
			if (endpoint == null) {
				throw new CloudProvisioningException("Cannot find endpoint for service '"
						+ this.getServiceName() + "' in the service catalog.");
			}
			this.serviceClient = Client.create();
			this.serviceWebResource = serviceClient.resource(endpoint);
			logger.info("Openstack " + this.getServiceName() + " endpoint: " + endpoint);
		}
		return serviceWebResource;
	}

	/**
	 * Get the token id. It will renew it if needed.
	 * 
	 * @return The token id.
	 * @throws OpenstackJsonSerializationException
	 */
	protected String getTokenId() throws OpenstackJsonSerializationException {
		this.renewTokenIfNeeded();
		return this.token.getToken().getId();
	}

	/**
	 * The name of Openstack service the client.
	 * 
	 * @return
	 */
	abstract String getServiceName();

	protected OpenstackServerException createOpenstackServerException(final UniformInterfaceException e) {
		final ClientResponse client = e.getResponse();
		final String responseMessage = client.getEntity(String.class);
		return new OpenstackServerException(client.getStatus(), responseMessage, e);
	}

	protected void verifyStatusCode(final int expectedStatus, final ClientResponse response)
			throws OpenstackServerException {
		if (expectedStatus != response.getStatus()) {
			final String entity = response.getEntity(String.class);
			throw new OpenstackServerException(expectedStatus, response.getStatus(), entity);
		}
	}
}
