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
package org.cloudifysource.restclient;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.AddTemplatesException;
import org.cloudifysource.dsl.rest.request.AddTemplatesRequest;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.request.SetServiceInstancesRequest;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;
import org.cloudifysource.dsl.rest.response.ApplicationDescription;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.dsl.rest.response.GetTemplateResponse;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.rest.response.ListTemplatesResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.dsl.rest.response.UninstallApplicationResponse;
import org.cloudifysource.dsl.rest.response.UninstallServiceResponse;
import org.cloudifysource.dsl.rest.response.UploadResponse;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.messages.MessagesUtils;
import org.cloudifysource.restclient.messages.RestClientMessageKeys;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * This class performs all the calls to the REST API, 
 * using the {@link RestClientExecutor}.
 * 
 * @author yael
 * 
 */
public class RestClient {

	private static final Logger logger = Logger.getLogger(RestClient.class.getName());

	private static final String FAILED_CREATING_CLIENT = "failed_creating_client";
	private static final String HTTPS = "https";

	private static final String UPLOAD_CONTROLLER_URL = "/upload/";
	private static final String DEPLOYMENT_CONTROLLER_URL = "/deployments/";
	private static final String TEMPLATES_CONTROLLER_URL = "/templates/";

	private static final String INSTALL_SERVICE_URL_FORMAT = "%s/services/%s";
	private static final String INSTALL_APPLICATION_URL_FORMAT = "%s";
	private static final String UPLOAD_URL_FORMAT = "%s";
	private static final String GET_DEPLOYMENT_EVENTS_URL_FORMAT = "%s/events/?from=%s&to=%s";
	private static final String GET_SERVICE_DESCRIPTION_URL_FORMAT = "%s/service/%s/description";
	private static final String GET_SERVICES_DESCRIPTION_URL_FORMAT = "%s/description";
	private static final String GET_APPLICATION_DESCRIPTION_URL_FORMAT = "applications/%s/description";
	private static final String ADD_TEMPALTES_URL_FORMAT = "";
	private static final String GET_TEMPALTE_URL_FORMAT = "%s";
	private static final String LIST_TEMPALTES_URL_FORMAT = "";
	private static final String REMOVE_TEMPALTE_URL_FORMAT = "%s";

	private static final String SET_INSTANCES_URL_FORMAT = "%s/services/%s/count";
	private static final String GET_LAST_EVENT_URL_FORMAT = "%s/events/last/";

	protected final RestClientExecutor executor;
	private String versionedDeploymentControllerUrl;
	private String versionedUploadControllerUrl;
	protected String versionedTemplatesControllerUrl;

	public RestClient(final URL url,
			final String username,
			final String password,
			final String apiVersion) throws RestClientException {

		versionedDeploymentControllerUrl = apiVersion + DEPLOYMENT_CONTROLLER_URL;
		versionedUploadControllerUrl = apiVersion + UPLOAD_CONTROLLER_URL;
		versionedTemplatesControllerUrl = apiVersion + TEMPLATES_CONTROLLER_URL;
		this.executor = createExecutor(url, apiVersion);

		setCredentials(username, password);
	}

	/**
	 * 
	 * @throws RestClientException .
	 */
	public void connect() throws RestClientException {
		executor.get(versionedDeploymentControllerUrl + "testrest", new TypeReference<Response<Void>>() {
		});
	}

	/**
	 * Sets the credentials.
	 * 
	 * @param username
	 *            .
	 * @param password
	 *            .
	 */
	public void setCredentials(final String username, final String password) {
		executor.setCredentials(username, password);
	}

	/**
	 * Executes a rest api call to install a specific service.
	 * 
	 * @param applicationName
	 *            The name of the application.
	 * @param serviceName
	 *            The name of the service to install.
	 * @param request
	 *            The install service request.
	 * @return The install service response.
	 * @throws RestClientException .
	 */
	public InstallServiceResponse installService(final String applicationName, final String serviceName,
			final InstallServiceRequest request) throws RestClientException {
		String effAppName = applicationName;
		if (applicationName == null) {
			effAppName = CloudifyConstants.DEFAULT_APPLICATION_NAME;
		}
		if (serviceName == null) {
			throw new RestClientException("service_name_missing", "install service is missing service name", null);
		}
		if (request == null) {
			throw new RestClientException("request_missing", "install service is missing install request", null);
		}
		final String installServiceUrl = getFormattedUrl(
				versionedDeploymentControllerUrl, 
				INSTALL_SERVICE_URL_FORMAT, 
				effAppName,
				serviceName);
		return executor.postObject(installServiceUrl, request, new TypeReference<Response<InstallServiceResponse>>() {
		});
	}

	/**
	 * Executes a rest api call to install an application.
	 * 
	 * @param applicationName
	 *            The name of the application.
	 * @param request
	 *            The install service request.
	 * @return The install service response.
	 * @throws RestClientException .
	 */
	public InstallApplicationResponse installApplication(final String applicationName,
			final InstallApplicationRequest request) throws RestClientException {
		final String installApplicationUrl = getFormattedUrl(
				versionedDeploymentControllerUrl, 
				INSTALL_APPLICATION_URL_FORMAT, 
				applicationName);
		return executor.postObject(installApplicationUrl, request,
				new TypeReference<Response<InstallApplicationResponse>>() {
		});
	}

	/**
	 * Uninstalls the specified service.
	 * 
	 * @param applicationName
	 *            The application containing the service.
	 * @param serviceName
	 *            The service name.
	 * @param timeoutInMinutes
	 *            Timeout in minutes.
	 * @return an uninstall service response object.
	 * @throws RestClientException
	 *             Indicates the uninstall operation failed.
	 */
	public UninstallServiceResponse uninstallService(final String applicationName, final String serviceName,
			final int timeoutInMinutes) throws RestClientException {

		final String url = getFormattedUrl(
				versionedDeploymentControllerUrl, 
				INSTALL_SERVICE_URL_FORMAT, 
				applicationName, 
				serviceName);
		final Map<String, String> requestParams = new HashMap<String, String>();
		requestParams.put(CloudifyConstants.REQ_PARAM_TIMEOUT_IN_MINUTES, String.valueOf(timeoutInMinutes));

		return executor.delete(url, requestParams, new TypeReference<Response<UninstallServiceResponse>>() {
		});
	}

	/**
	 * Uninstalls the specified application.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param timeoutInMinutes
	 *            Timeout in minutes.
	 * @return an uninstall application response object.
	 * 
	 * @throws RestClientException
	 *             Indicates the uninstall operation failed.
	 */
	public UninstallApplicationResponse uninstallApplication(final String applicationName, final int timeoutInMinutes)
			throws RestClientException {
		final String url = versionedDeploymentControllerUrl + applicationName;
		final Map<String, String> requestParams = new HashMap<String, String>();
		requestParams.put(CloudifyConstants.REQ_PARAM_TIMEOUT_IN_MINUTES, String.valueOf(timeoutInMinutes));

		return executor.delete(url, requestParams, new TypeReference<Response<UninstallApplicationResponse>>() {
		});
	}

	/**
	 * Uploads a file to the repository.
	 * 
	 * @param fileName
	 *            The name of the file to upload.
	 * @param file
	 *            The file to upload.
	 * @return upload response.
	 * @throws RestClientException .
	 */
	public UploadResponse upload(final String fileName, final File file) throws RestClientException {
		validateFile(file);
		final String finalFileName = fileName == null ? file.getName() : fileName;
        if (logger.isLoggable(Level.FINE)) {
        	logger.fine("uploading file " + file.getAbsolutePath() + " with name " + finalFileName);
        }
		final String uploadUrl = getFormattedUrl(
				versionedUploadControllerUrl, 
				UPLOAD_URL_FORMAT, 
				finalFileName);	
		
		final UploadResponse response = 
				executor.postFile(
						uploadUrl, 
						file, 
						CloudifyConstants.UPLOAD_FILE_PARAM_NAME, 
						new TypeReference<Response<UploadResponse>>() { });	
		return response;
	}
	
	
	/**
	 * Provides access to life cycle events of a service.
	 * 
	 * @param deploymentId
	 *            The deployment id given at installation time.
	 * @param from
	 *            The starting event index.
	 * @param to
	 *            The last event index. passing -1 means all events (limit to 100 at a time)
	 * @return The events.
	 * @throws RestClientException .
	 */
	public DeploymentEvents getDeploymentEvents(final String deploymentId, final int from, final int to)
			throws RestClientException {
		String url = getFormattedUrl(
				versionedDeploymentControllerUrl, 
				GET_DEPLOYMENT_EVENTS_URL_FORMAT, 
				deploymentId, 
				String.valueOf(from), 
				String.valueOf(to));
		return executor.get(url, new TypeReference<Response<DeploymentEvents>>() {
		});
	}

	/**
	 * 
	 * @param appName
	 *            .
	 * @param serviceName
	 *            .
	 * @return ServiceDescription.
	 * @throws RestClientException .
	 */
	public ServiceDescription getServiceDescription(final String appName, final String serviceName)
			throws RestClientException {
		String url = getFormattedUrl(
				versionedDeploymentControllerUrl, 
				GET_SERVICE_DESCRIPTION_URL_FORMAT, 
				appName,
				serviceName);		
		return executor.get(url, new TypeReference<Response<ServiceDescription>>() {
		});
	}

	/**
	 * Retrieves a list of services description by deployment id.
	 * 
	 * @param deploymentId
	 *            The deployment id.
	 * @return list of {@link ServiceDescription}
	 * @throws RestClientException 
	 */
	public List<ServiceDescription> getServiceDescriptions(final String deploymentId)
			throws RestClientException {
		String url = getFormattedUrl(
				versionedDeploymentControllerUrl, 
				GET_SERVICES_DESCRIPTION_URL_FORMAT, 
				deploymentId);
		return executor.get(url, new TypeReference<Response<List<ServiceDescription>>>() {
		});
	}

	/**
	 * 
	 * @param appName
	 *            .
	 * @return ApplicationDescription.
	 * @throws RestClientException .
	 */
	public ApplicationDescription getApplicationDescription(final String appName) throws RestClientException {
		String url = getFormattedUrl(
				versionedDeploymentControllerUrl, 
				GET_APPLICATION_DESCRIPTION_URL_FORMAT, 
				appName);
		return executor.get(url, new TypeReference<Response<ApplicationDescription>>() {
		});
	}

	/********
	 * Manually Scales a specific service in/out.
	 * 
	 * @param applicationName
	 *            the service's application name.
	 * @param serviceName
	 *            the service name.
	 * @param request
	 *            the scale request details.
	 * @throws RestClientException
	 *             in case of an error.
	 */
	public void setServiceInstances(final String applicationName, final String serviceName,
			final SetServiceInstancesRequest request) throws RestClientException {
		if (request == null) {
			throw new IllegalArgumentException("request may not be null");
		}

		final String setInstancesUrl = getFormattedUrl(
				versionedDeploymentControllerUrl, 
				SET_INSTANCES_URL_FORMAT, 
				applicationName, 
				serviceName);
		executor.postObject(
				setInstancesUrl,
				request,
				new TypeReference<Response<Void>>() {
				}
				);

	}

	/********
	 * Retrieves last event indes for this deployment id.
	 * 
	 * @param deploymentId
	 *            The deploymentId.
	 * @return {@link DeploymentEvents}
	 * @throws RestClientException
	 *             in case of an error on the rest server.
	 */
	public DeploymentEvents getLastEvent(final String deploymentId) throws RestClientException {

		final String url = getFormattedUrl(
				versionedDeploymentControllerUrl, 
				GET_LAST_EVENT_URL_FORMAT, 
				deploymentId);
		return executor.get(url, new TypeReference<Response<DeploymentEvents>>() {
		});

	}

	private void validateFile(final File file) throws RestClientException {
		if (file == null) {
			throw MessagesUtils.createRestClientException(RestClientMessageKeys.UPLOAD_FILE_MISSING.getName());
		}
		final String absolutePath = file.getAbsolutePath();
		if (!file.exists()) {
			throw MessagesUtils.createRestClientException(RestClientMessageKeys.UPLOAD_FILE_DOESNT_EXIST.getName(),
					absolutePath);
		}
		if (!file.isFile()) {
			throw MessagesUtils.createRestClientException(RestClientMessageKeys.UPLOAD_FILE_NOT_FILE.getName(),
					absolutePath);
		}
		final long length = file.length();
		if (length > CloudifyConstants.DEFAULT_UPLOAD_SIZE_LIMIT_BYTES) {
			throw MessagesUtils.createRestClientException(
					RestClientMessageKeys.UPLOAD_FILE_SIZE_LIMIT_EXCEEDED.getName(), absolutePath, length,
					CloudifyConstants.DEFAULT_UPLOAD_SIZE_LIMIT_BYTES);
		}
	}

	private RestClientExecutor createExecutor(final URL url, final String apiVersion) throws RestClientException {
		DefaultHttpClient httpClient;
		if (HTTPS.equals(url.getProtocol())) {
			httpClient = getSSLHttpClient(url);
		} else {
			httpClient = new SystemDefaultHttpClient();
		}
		final HttpParams httpParams = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_READ_TIMEOUT);
		return new RestClientExecutor(httpClient, url);
	}

	/**
	 * Returns a HTTP client configured to use SSL.
	 * 
	 * @param url
	 * 
	 * @return HTTP client configured to use SSL
	 * @throws org.cloudifysource.restclient.exceptions.RestClientException
	 *             Reporting different failures while creating the HTTP client
	 */
	private DefaultHttpClient getSSLHttpClient(final URL url) throws RestClientException {
		try {
			final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			// TODO : support self-signed certs if configured by user upon "connect"
			trustStore.load(null, null);

			final SSLSocketFactory sf = new RestSSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			final HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			final SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme(HTTPS, sf, url.getPort()));

			final ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (final Exception e) {
			throw new RestClientException(FAILED_CREATING_CLIENT, "Failed creating http client",
					ExceptionUtils.getFullStackTrace(e));
		}
	}

	/**
	 * 
	 * @param controllerUrl .
	 * @param format .
	 * @param args .
	 * @return the formatted URL.
	 */
	protected String getFormattedUrl(final String controllerUrl, final String format, final String... args) {
		return controllerUrl + String.format(format, (Object[]) args);
	}

	/**
	 * Executes a rest API call to get template.
	 * 
	 * @param templateName the name of the template to get.
	 * @return GetTemplateResponse.
	 * @throws RestClientException .
	 */
	public GetTemplateResponse getTemplate(final String templateName) 
			throws RestClientException {
		final String listTempaltesInternalUrl = getFormattedUrl(
				versionedTemplatesControllerUrl, 
				GET_TEMPALTE_URL_FORMAT,
				templateName);
		return executor.get(listTempaltesInternalUrl,
				new TypeReference<Response<GetTemplateResponse>>() { });
	}
	
	/**
	 * Executes a rest API call to list templates.
	 *  
	 * @return ListTemplatesResponse.
	 * @throws RestClientException .
	 */
	public ListTemplatesResponse listTemplates() throws RestClientException {
		final String listTempaltesInternalUrl = getFormattedUrl(
				versionedTemplatesControllerUrl, 
				LIST_TEMPALTES_URL_FORMAT);
		return executor.get(listTempaltesInternalUrl,
				new TypeReference<Response<ListTemplatesResponse>>() { });
	}
	
	/**
	 * Executes a rest API call to add templates to all REST instances.
	 *  
	 * @param request contains the templates folder.
	 * @return AddTemplatesResponse.
	 * @throws AddTemplatesException 
	 * 			If failed to add all templates (includes partial failure).
	 * @throws RestClientException .
	 */
	public AddTemplatesResponse addTemplates(final AddTemplatesRequest request) 
			throws RestClientException, AddTemplatesException {
		final String addTempaltesUrl = getFormattedUrl(
				versionedTemplatesControllerUrl, 
				ADD_TEMPALTES_URL_FORMAT);
		AddTemplatesResponse response = null;
		try {
			response = executor.postObject(
					addTempaltesUrl, 
					request, 
					new TypeReference<Response<AddTemplatesResponse>>() { });
		} catch (RestClientException e) {
			String verbose = e.getVerbose();
				try {
					response = new ObjectMapper().readValue(verbose, AddTemplatesResponse.class);
					throw new AddTemplatesException(response);
				} catch (JsonProcessingException e1) {
					logger.warning("Failed to process exception, got JsonProcessingException: " + e1.getMessage());
					throw e;
				} catch (IOException e1) {
					logger.warning("Failed to process exception, got IOException: " + e1.getMessage());
					throw e;
				}
		}
		return response;
	}
	
	/**
	 * Executes a rest API call to remove template from all REST instances.
	 * 
	 * @param templateName the template's name to remove.
	 * @throws RestClientException .
	 */
	public void removeTemplate(final String templateName) 
			throws RestClientException {
		final String removeTempalteUrl = getFormattedUrl(
				versionedTemplatesControllerUrl, 
				REMOVE_TEMPALTE_URL_FORMAT,
				templateName);
		executor.delete(
				removeTempalteUrl, 
				new TypeReference<Response<Void>>() { });
	}

}