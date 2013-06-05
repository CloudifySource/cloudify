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

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.request.SetServiceInstancesRequest;
import org.cloudifysource.dsl.rest.response.*;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.messages.MessagesUtils;
import org.cloudifysource.restclient.messages.RestClientMessageKeys;
import org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author yael
 * 
 */
public class RestClient {

	private static final Logger logger = Logger.getLogger(RestClient.class.getName());

	private static final String FAILED_CREATING_CLIENT = "failed_creating_client";

	private final RestClientExecutor executor;

	private static final String UPLOAD_CONTROLLER_URL = "/upload/";
	private static final String DEPLOYMENT_CONTROLLER_URL = "/deployments/";
	private String versionedDeploymentControllerUrl;
	private String versionedUploadControllerUrl;

	private static final String HTTPS = "https";

	private static final String SET_INSTANCES_URL_FORMAT = "%s/services/%s/count";
    private static final String GET_LAST_EVENT_URL_FORMAT = "%s/events/last/";

	public RestClient(final URL url,
			final String username,
			final String password,
			final String apiVersion) throws RestClientException {

		this.executor = createExecutor(url, apiVersion);
		setCredentials(username, password);
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
		final String installServiceUrl = versionedDeploymentControllerUrl + applicationName + "/services/"
				+ serviceName;
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
		final String installApplicationUrl = versionedDeploymentControllerUrl + applicationName;
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

		final String url = versionedDeploymentControllerUrl + applicationName + "/services/" + serviceName;
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
		logger.fine("uploading file " + file.getAbsolutePath() + " with name " + finalFileName);
		final String uploadUrl = versionedUploadControllerUrl + finalFileName;
		final UploadResponse response = executor.postFile(uploadUrl, file, CloudifyConstants.UPLOAD_FILE_PARAM_NAME,
				new TypeReference<Response<UploadResponse>>() {
				});
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
		return executor.get(versionedDeploymentControllerUrl + "/" + deploymentId + "/events/" + "?from=" + from
				+ "&to=" + to, new TypeReference<Response<DeploymentEvents>>() {
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
		return executor.get(versionedDeploymentControllerUrl + "/" + appName + "/service/" + serviceName
				+ "/description", new TypeReference<Response<ServiceDescription>>() {
		});
	}

    /**
     * Retrieves a list of services description by deployment id.
     * @param deploymentId The deployment id.
     * @return
     * @throws RestClientException
     */
    public List<ServiceDescription> getServicesDescription(final String deploymentId)
            throws RestClientException {
        return executor.get(versionedDeploymentControllerUrl + "/" + deploymentId + "/description", new TypeReference<Response<List<ServiceDescription>>>() {
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
		return executor.get(versionedDeploymentControllerUrl + "/applications/" + appName + "/description",
				new TypeReference<Response<ApplicationDescription>>() {
				});
	}

	/**
	 *
	 * @throws RestClientException .
	 */
	public void connect() throws RestClientException {
		executor.get(versionedDeploymentControllerUrl + "testrest", new TypeReference<Response<Void>>() {
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
			httpClient = new DefaultHttpClient();
		}
		final HttpParams httpParams = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, CloudifyConstants.DEFAULT_HTTP_READ_TIMEOUT);
		versionedDeploymentControllerUrl = apiVersion + DEPLOYMENT_CONTROLLER_URL;
		versionedUploadControllerUrl = apiVersion + UPLOAD_CONTROLLER_URL;
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
			// TODO : support self-signed certs if configured by user upon
			// "connect"
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

	private String getFormattedUrl(final String format, final String... args) {
    	return versionedDeploymentControllerUrl  + String.format(format, (Object[])args);
    }
	  /********
     * Manually Scales a specific service in/out.
     * @param applicationName the service's application name.
     * @param serviceName the service name.
     * @param request the scale request details.
     * @throws RestClientException in case of an error.
     */
	public void setServiceInstances(final String applicationName, final String serviceName,
			final SetServiceInstancesRequest request) throws RestClientException {
		if (request == null) {
			throw new IllegalArgumentException("request may not be null");
		}

		final String setInstancesUrl =
				getFormattedUrl(SET_INSTANCES_URL_FORMAT, applicationName, serviceName);
		executor.postObject(
				setInstancesUrl,
				request,
				new TypeReference<Response<Void>>() {
				}
				);

	}

	/********
     * Retrieves last event indes for this deployment id.
     * @param deploymentId The deploymentId.
     * @throws RestClientException in case of an error on the rest server.
     */
	public DeploymentEvents getLastEvent(final String deploymentId) throws RestClientException {

		final String setInstancesUrl =
				getFormattedUrl(GET_LAST_EVENT_URL_FORMAT, deploymentId);
        return executor.get(setInstancesUrl, new TypeReference<Response<DeploymentEvents>>(){});

	}




}