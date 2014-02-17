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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import org.cloudifysource.esc.driver.provisioning.openstack.rest.TokenAccess;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.TokenServiceCatalog;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.TokenServiceCatalogEndpoint;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;

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

	protected static final int CODE_OK_200 = 200;
	protected static final int CODE_OK_204 = 204;

	private static final Logger WIRE_LOGGER = Logger.getLogger("openstack.wire");
	private static final Logger logger = Logger.getLogger(OpenStackBaseClient.class.getName());

	private final byte[] webResourceMutex = new byte[0];

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
	}

	/**
	 * Destroy the client. <br />
	 */
	public void close() {
		synchronized (this.webResourceMutex) {
			if (this.serviceClient != null) {
				this.serviceClient.destroy();
			}
		}
	}

	private synchronized void renewTokenIfNeeded() throws OpenstackJsonSerializationException {
		if (this.isTokenExpiredSoon()) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("Token not found or expired. Request a new token");
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

	/**
	 * Initialize Openstack token.
	 * 
	 * @throws OpenstackJsonSerializationException
	 *             A problem occurs when requesting Openstack server.
	 */
	private void initToken() throws OpenstackJsonSerializationException {
		final Client client = Client.create();
		if (WIRE_LOGGER.isLoggable(Level.FINE)) {
			client.addFilter(new LoggingFilter(WIRE_LOGGER));
		}
		try {
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Request openstack " + this.getServiceType() + " new token.");
			}
			if (this.endpoint == null) {
				throw new IllegalStateException("No endpoint defined");
			}
			final WebResource webResource = client.resource(this.endpoint);
			final String tokenJsonRequest = "{\"auth\":{\"passwordCredentials\":"
					+ "{\"username\": \"%s\", \"password\":\"%s\"}, \"tenantName\":\"%s\"}}";
			final String input = String.format(tokenJsonRequest, username, password, tenant);
			final String response = webResource.path("tokens").accept(MediaType.APPLICATION_JSON)
					.type(MediaType.APPLICATION_JSON_TYPE).post(String.class, input);
			this.token = JsonUtils.unwrapRootToObject(TokenAccess.class, response, false);
		} finally {
			if (client != null) {
				client.destroy();
			}
		}
	}

	/**
	 * Get the token id. It will renew it if needed.
	 * 
	 * @return The token id.
	 * @throws OpenstackJsonSerializationException
	 *             If a serialization issue occurs with Openstack request/response.
	 */
	protected String getTokenId() throws OpenstackJsonSerializationException {
		this.renewTokenIfNeeded();
		return this.token.getToken().getId();
	}
	
	
	/**
	 * Get the tenant id, if token not null.
	 * 
	 * @return The tenant id, or null if token is null
	 * @throws OpenstackJsonSerializationException
	 *             If a serialization issue occurs with Openstack request/response.
	 */
	protected String getTenantId() throws OpenstackJsonSerializationException {
		String tenantId = null;
		if (token != null) {
			tenantId = token.getToken().getTenant().getId();
		}
		
		return tenantId;
	}
	

	/**
	 * Return the WebResource pre configured with the endpoint.
	 * 
	 * @return The pre configured WebResource.
	 * @throws OpenstackException
	 *             A problem occurs when requesting Openstack server.
	 */
	protected WebResource getWebResource() throws OpenstackException {
		synchronized (this.webResourceMutex) {
			if (this.serviceWebResource == null) {
				this.renewTokenIfNeeded();
				this.endpoint = this.getEndpoint();
				this.serviceClient = Client.create();
				if (WIRE_LOGGER.isLoggable(Level.FINE)) {
					this.serviceClient.addFilter(new LoggingFilter(WIRE_LOGGER));
				}
				this.serviceWebResource = this.serviceClient.resource(endpoint);
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Openstack endpoint: " + endpoint);
				}
			}
			return serviceWebResource;
		}
	}

	private String getEndpoint() throws OpenstackException {
		String endpoint = null;

		endpoint = this.getEndpointByName();

		if (endpoint == null) {
			endpoint = this.getEndpointByType();
		}

		if (endpoint == null) {
			throw new OpenstackException("Cannot find endpoint for service '"
					+ this.getServiceType() + "' in Openstack service catalog.");
		}
		return endpoint;
	}

	/**
	 * The name of the service the Openstack's catalog.
	 * 
	 * @return The name of the service in Openstack's catalog.
	 */
	protected abstract String getServiceName();

	/**
	 * The type of the service the Openstack's catalog.
	 * 
	 * @return The type of the service in Openstack's catalog.
	 */
	protected abstract String getServiceType();

	/**
	 * Retrieves the service endpoint by type.
	 * 
	 * @return The endpoint
	 */
	protected String getEndpointByType() {
		final String endpointType = this.getServiceType();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Trying to get service endpoint by type '" + endpointType + "'");
		}
		for (final TokenServiceCatalog tsc : this.token.getServiceCatalog()) {
			if (endpointType.equals(tsc.getType())) {
				for (final TokenServiceCatalogEndpoint endpoint : tsc.getEndpoints()) {
					if (this.region.equals(endpoint.getRegion())) {
						return endpoint.getPublicURL();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Retrieves the service endpoint by name.
	 * 
	 * @return The endpoint
	 */
	private String getEndpointByName() {
		final String endpointName = this.getServiceName();
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Trying to get service endpoint by name '" + endpointName + "'");
		}
		if (endpointName != null) {
			for (final TokenServiceCatalog tsc : this.token.getServiceCatalog()) {
				if (endpointName.equals(tsc.getName())) {
					for (final TokenServiceCatalogEndpoint endpoint : tsc.getEndpoints()) {
						if (this.region.equals(endpoint.getRegion())) {
							return endpoint.getPublicURL();
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Translate {@link UniformInterfaceException} to {@link OpenstackServerException} to get an accurate error message.
	 * 
	 * @param e
	 *            The {@link UniformInterfaceException} to translate
	 * @return The translated {@link OpenstackServerException}.
	 */
	protected OpenstackServerException createOpenstackServerException(final UniformInterfaceException e) {
		final ClientResponse client = e.getResponse();
		final String responseMessage = client.getEntity(String.class);
		return new OpenstackServerException(client.getStatus(), responseMessage, e);
	}

	/**
	 * Perform a get query with parameters.
	 * 
	 * @param path
	 *            The URI path of the request.
	 * @param params
	 *            The parameters for the request. The array must be formatted as [key, value, key, value,...].
	 * @return The response of the request.
	 * @throws OpenstackException
	 *             If an error occurs during the request.
	 */
	protected String doGet(final String path, final String[] params) throws OpenstackException {
		try {
			if (params != null && params.length % 2 != 0) {
				throw new IllegalArgumentException("Paramters array missing an element:" + Arrays.asList(params));
			}
			WebResource webResource = this.getWebResource();
			webResource = webResource.path(path);

			if (params != null) {
				for (int i = 0; i < params.length - 1; i += 2) {
					webResource = webResource.queryParam(params[i], params[i + 1]);
				}
			}

			if (logger.isLoggable(Level.FINER)) {
				logger.finer("GET '" + webResource + "'");
			}

			final String response = webResource.type(MediaType.APPLICATION_JSON_TYPE)
					.accept(MediaType.APPLICATION_JSON)
					.header("X-Auth-Token", this.getTokenId())
					.get(String.class);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("GET '" + webResource + "' got response: " + response);
			}
			return response;
		} catch (final UniformInterfaceException e) {
			throw this.createOpenstackServerException(e);
		}
	}

	/**
	 * Perform a get query.
	 * 
	 * @param path
	 *            The URI path of the request.
	 * @return The response of the request.
	 * @throws OpenstackException
	 *             If an error occurs during the request.
	 */
	protected String doGet(final String path) throws OpenstackException {
		return this.doGet(path, null);
	}

	/**
	 * Perform a delete query.
	 * 
	 * @param path
	 *            The URI path of the request.
	 * @param expectedStatus
	 *            The expected returned status code.
	 * @throws OpenstackException
	 *             If an error occurs during the request.
	 */
	protected void doDelete(final String path, final int expectedStatus) throws OpenstackException {
		try {
			final WebResource webResource = this.getWebResource();

			if (logger.isLoggable(Level.FINER)) {
				logger.finer("DELETE request: '" + webResource + "'");
			}

			final ClientResponse response = webResource.path(path)
					.type(MediaType.APPLICATION_JSON_TYPE)
					.accept(MediaType.APPLICATION_JSON)
					.header("X-Auth-Token", this.getTokenId())
					.delete(ClientResponse.class);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("DELETE '" + webResource + "' got response status: " + response.getStatus());
			}

			if (expectedStatus != response.getStatus()) {
				final String entity = response.getEntity(String.class);
				throw new OpenstackServerException(expectedStatus, response.getStatus(), entity);
			}
		} catch (final UniformInterfaceException e) {
			throw this.createOpenstackServerException(e);
		}
	}

	/**
	 * Perform a post request.
	 * 
	 * @param path
	 *            The URI path of the request.
	 * @param input
	 *            The body of the request.
	 * @return The response of the request.
	 * @throws OpenstackException
	 *             If an error occurs during the request.
	 */
	protected String doPost(final String path, final String input) throws OpenstackException {
		try {
			final WebResource webResource = this.getWebResource().path(path);

			if (logger.isLoggable(Level.FINER)) {
				logger.finer("POST '" + webResource + "' with body: '" + input + "'");
			}

			final String response = webResource.type(MediaType.APPLICATION_JSON_TYPE)
					.accept(MediaType.APPLICATION_JSON)
					.header("X-Auth-Token", this.getTokenId())
					.post(String.class, input);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("POST '" + webResource + "' with body: '" + input + "' got response: "
						+ response);
			}

			return response;
		} catch (final UniformInterfaceException e) {
			throw this.createOpenstackServerException(e);
		}
	}

	/**
	 * Perform a put request.
	 * 
	 * @param path
	 *            The URI path of the request.
	 * @param input
	 *            The body of the request.
	 * @return The response of the request.
	 * @throws OpenstackException
	 *             If an error occurs during the request.
	 */
	protected String doPut(final String path, final String input) throws OpenstackException {
		try {
			final WebResource webResource = this.getWebResource().path(path);

			if (logger.isLoggable(Level.FINER)) {
				logger.finer("PUT '" + webResource + "' with body: '" + input + "'");
			}

			final String response = webResource
					.type(MediaType.APPLICATION_JSON_TYPE)
					.accept(MediaType.APPLICATION_JSON)
					.header("X-Auth-Token", this.getTokenId())
					.put(String.class, input);

			if (logger.isLoggable(Level.FINEST)) {
				logger.finest("PUT '" + webResource + "' with body: '" + input + "' got response: "
						+ response);
			}

			return response;
		} catch (final UniformInterfaceException e) {
			throw this.createOpenstackServerException(e);
		}

	}
}
