/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.shell.rest;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.ApplicationDescription;
import org.cloudifysource.dsl.rest.ServiceDescription;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.restclient.ErrorStatusException;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.InvocationResult;
import org.cloudifysource.restclient.RestException;
import org.cloudifysource.restclient.StringUtils;
import org.cloudifysource.shell.AbstractAdminFacade;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;
import org.codehaus.jackson.map.ObjectMapper;

import com.j_spaces.kernel.PlatformVersion;

/**
 * This class implements the {@link AdminFacade}, relying on the abstract implementation of {@link AbstractAdminFacade}.
 * It discovers and manages applications, services, containers and other components over REST, using the
 * {@link GSRestClient}.
 *
 * @author rafi, barakm, adaml, noak
 * @since 2.0.0
 */
public class RestAdminFacade extends AbstractAdminFacade {

	private static final String GS_USM_COMMAND_NAME = "GS_USM_CommandName";
	private static final String SERVICE_CONTROLLER_URL = "/service/";
	private static final String CLOUD_CONTROLLER_URL = "/cloudcontroller/";

	private GSRestClient client;
	private URL urlObj;
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doConnect(final String user, final String password, final String url, final boolean sslUsed)
			throws CLIException {



		try {
			this.urlObj = new URL(ShellUtils.getFormattedRestUrl(url, sslUsed));
			client = new GSRestClient(user, password, getUrl(), PlatformVersion.getVersionNumber());
			// test connection
			client.get(SERVICE_CONTROLLER_URL + "testrest");
			if (user != null || password != null) {
				reconnect(user, password);
			}
		} catch (final MalformedURLException e) {
			throw new CLIStatusException("could_not_parse_url", url, e);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e);
		} catch (final RestException e) {
			throw new CLIException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reconnect(final String username, final String password) throws CLIException {
		try {
			client.setCredentials(username, password);
			// test connection
			client.get(SERVICE_CONTROLLER_URL + "testlogin");
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void verifyCloudAdmin() throws CLIException {
		try {
			client.get(SERVICE_CONTROLLER_URL + "verifyCloudAdmin");
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doDisconnect() {
		client = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeInstance(final String applicationName,
			final String serviceName, final int instanceId) throws CLIException {

		final String relativeUrl = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/instances/"
				+ instanceId + "/remove";
		try {
			client.delete(relativeUrl);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<ApplicationDescription> getApplicationDescriptionsList() throws CLIException {

		final List<ApplicationDescription> applicationDescriptionList = new ArrayList<ApplicationDescription>();

		try {
			final List<Object> objectsList = (List<Object>) client.get("/service/applications/description");
			final ObjectMapper map = new ObjectMapper();
			for (final Object object : objectsList) {
				final ApplicationDescription applicationDescription =
						map.convertValue(object, ApplicationDescription.class);
				applicationDescriptionList.add(applicationDescription);
			}
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}

		return applicationDescriptionList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<String> getApplicationNamesList() throws CLIException {
		try {
			final Map<String, String> resultsMap = (Map<String, String>) client.get("/service/applications");
			return new ArrayList<String>(resultsMap.keySet());
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ApplicationDescription getServicesDescriptionList(final String applicationName)
			throws CLIException {
		try {
			@SuppressWarnings("unchecked")
			final List<Object> applicationDescriptionList = (List<Object>) client
					.get("/service/applications/" + applicationName
							+ "/services/description");
			if (applicationDescriptionList == null || applicationDescriptionList.isEmpty()) {
				return null;
			}
			final ObjectMapper map = new ObjectMapper();
			final Object descriptionObject = applicationDescriptionList.get(0);
			final ApplicationDescription applicationDescription =
					map.convertValue(descriptionObject, ApplicationDescription.class);
			return applicationDescription;
			// http://stackoverflow.com/questions/5219073/json-deserialization-problem
		} catch (final ErrorStatusException ese) {
			throw new CLIStatusException(ese, ese.getReasonCode(),
					ese.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getServicesList(final String applicationName)
			throws CLIException {
		try {
			@SuppressWarnings("unchecked")
			final List<String> services = (List<String>) client
					.get("/service/applications/" + applicationName
							+ "/services");
			return services;
		} catch (final ErrorStatusException ese) {
			throw new CLIStatusException(ese, ese.getReasonCode(),
					ese.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String doDeploy(final String applicationName,
			final File packedFile) throws CLIException {
		final String url = SERVICE_CONTROLLER_URL + CLOUD_CONTROLLER_URL
				+ "deploy?applicationName=" + applicationName;
		try {
			return (String) client.postFile(url, packedFile);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String startService(final String applicationName,
			final File serviceFile) throws CLIException {
		return doDeploy(applicationName, serviceFile);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> undeploy(final String applicationName,
			final String serviceName, final int timeoutInMinutes)
			throws CLIException {

		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/timeout/"
				+ timeoutInMinutes + "/undeploy";
		try {
			@SuppressWarnings("unchecked")
			final Map<String, String> response = (Map<String, String>) client
					.delete(url);
			return response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addInstance(final String applicationName, final String serviceName, final String authGroups,
			final int timeout) throws CLIException {

		final String url = SERVICE_CONTROLLER_URL + "applications/na/services/" + serviceName + "/addinstance";
		final Map<String, String> params = new HashMap<String, String>();
		try {
			params.put("timeout", Integer.toString(timeout));
			params.put("authGroups", authGroups);
			client.post(url, params);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> getInstanceList(final String applicationName,
			final String serviceName) throws CLIException {

		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/instances";
		try {
			@SuppressWarnings("unchecked")
			final Map<String, Object> instances = (Map<String, Object>) client
					.get(url);
			return instances;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String installElastic(final File packedFile,
			final String applicationName, final String serviceName,
			final String zone, final Properties contextProperties,
			final String templateName, final String authGroups,
			final int timeout, final boolean selfHealing,
			final File cloudOverrides, final File serviceOverrides) throws CLIException {

		String response;
		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/timeout/"
				+ timeout + "?zone=" + zone + "&template=" + templateName
				+ "&selfHealing=" + Boolean.toString(selfHealing);
		try {
			final Map<String, File> additionalFiles = new HashMap<String, File>();
			additionalFiles.put("file", packedFile);
			additionalFiles.put("cloudOverridesFile", cloudOverrides);
			additionalFiles.put(CloudifyConstants.SERVICE_OVERRIDES_FILE_PARAM, serviceOverrides);
			if (org.apache.commons.lang.StringUtils.isBlank(authGroups)) {
				response = (String) client.postFiles(url, contextProperties, null/* params */, additionalFiles);
			} else {
				final Map<String, String> paramsMap = new HashMap<String, String>();
				paramsMap.put("authGroups", authGroups);
				response = (String) client.postFiles(url, contextProperties, paramsMap, additionalFiles);
			}
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

		return response;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void waitForLifecycleEvents(final String pollingID,
			final int timeout, final String timeoutMessage)
			throws CLIException, InterruptedException, TimeoutException {
		final RestLifecycleEventsLatch restLifecycleEventsLatch = new RestLifecycleEventsLatch();
		restLifecycleEventsLatch.setPollingId(pollingID);
		restLifecycleEventsLatch.setRestClient(client);
		restLifecycleEventsLatch.setTimeoutMessage(timeoutMessage);
		restLifecycleEventsLatch.waitForLifecycleEvents(timeout,
				TimeUnit.MINUTES);
	}

	@Override
	public RestLifecycleEventsLatch getLifecycleEventsPollingLatch(
			final String pollingID, final String timeoutMessage) {
		final RestLifecycleEventsLatch restLifecycleEventsLatch = new RestLifecycleEventsLatch();
		restLifecycleEventsLatch.setPollingId(pollingID);
		restLifecycleEventsLatch.setRestClient(client);
		restLifecycleEventsLatch.setTimeoutMessage(timeoutMessage);
		return restLifecycleEventsLatch;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, InvocationResult> invokeServiceCommand(
			final String applicationName, final String serviceName,
			final String beanName, final String commandName,
			final Map<String, String> params) throws CLIException {
		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/beans/"
				+ beanName + "/invoke";

		Object result;
		try {
			result = client.post(url,
					buildCustomCommandParams(commandName, params));
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

		@SuppressWarnings("unchecked")
		final Map<String, Object> restResult = (Map<String, Object>) result;

		final Map<String, InvocationResult> invocationResultMap = new LinkedHashMap<String, InvocationResult>();
		for (final Map.Entry<String, Object> entry : restResult.entrySet()) {
			final Object value = entry.getValue();

			if (!(value instanceof Map<?, ?>)) {
				logger.severe("Received an unexpected return value to the invoke command. Key: "
						+ entry.getKey() + ", value: " + value);
			} else {
				@SuppressWarnings("unchecked")
				final Map<String, String> curr = (Map<String, String>) value;
				final InvocationResult invocationResult = InvocationResult
						.createInvocationResult(curr);
				invocationResultMap.put(entry.getKey(), invocationResult);
			}

		}
		return invocationResultMap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InvocationResult invokeInstanceCommand(final String applicationName,
			final String serviceName, final String beanName,
			final int instanceId, final String commandName,
			final Map<String, String> paramsMap) throws CLIException {
		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/instances/"
				+ instanceId + "/beans/" + beanName + "/invoke";

		Map<String, String> resultMap;
		try {
			@SuppressWarnings("unchecked")
			final Map<String, String> response = (Map<String, String>) client
					.post(url, buildCustomCommandParams(commandName, paramsMap));
			resultMap = response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

		// resultMap.entrySet().iterator().next();
		// InvocationResult invocationResult = InvocationResult
		// .createInvocationResult(resultMap);
		// @SuppressWarnings("unchecked")
		// Map<String, String> curr = (Map<String, String>) resultMap;
		// InvocationResult invocationResult = InvocationResult
		// .createInvocationResult(curr);
		//
		return InvocationResult.createInvocationResult(resultMap);
		// return GSRestClient.mapToInvocationResult(resultMap);

	}

	private Map<String, String> buildCustomCommandParams(
			final String commandName, final Map<String, String> parametersMap) {
		final Map<String, String> params = new HashMap<String, String>();
		params.put(GS_USM_COMMAND_NAME, commandName);
		// add all of the predefined parameters into the params map.
		for (final Map.Entry<String, String> entry : parametersMap.entrySet()) {
			params.put(entry.getKey(), entry.getValue());
		}
		return params;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<String> getMachines() throws CLIException {

		Map<String, Object> map;
		try {
			map = client.getAdminData("machines/HostsByAddress");
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}
		@SuppressWarnings("unchecked")
		final List<String> list = (List<String>) map
				.get("HostsByAddress-Elements");
		final List<String> result = new ArrayList<String>(list.size());
		for (final String host : list) {
			final String[] parts = host.split("/");
			final String ip = parts[parts.length - 1];
			result.add(ip);
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> uninstallApplication(
			final String applicationName, final int timeoutInMinutes)
			throws CLIException {

		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/timeout/" + timeoutInMinutes;
		try {
			@SuppressWarnings("unchecked")
			final Map<String, String> response = (Map<String, String>) client
					.delete(url);
			return response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * Gets all the UIDs of the services of the given application.
	 *
	 * @param applicationName
	 *            The name of the application to query for its services
	 * @return A set of UIDs of all the services of the given application.
	 * @throws CLIException
	 *             Reporting a failure to the application's services or a service's UID
	 */
	public Set<String> getGridServiceContainerUidsForApplication(
			final String applicationName) throws CLIException {
		final Set<String> containerUids = new HashSet<String>();

		final ApplicationDescription servicesList = getServicesDescriptionList(applicationName);
		for (final ServiceDescription serviceDescription : servicesList.getServicesDescription()) {
			final String serviceName = serviceDescription.getServiceName();
			containerUids.addAll(getGridServiceContainerUidsForService(
					applicationName, serviceName));
		}
		return containerUids;
	}

	/**
	 * Returns all the UIDs of the GSCs that run the given service, in the context of the given application.
	 *
	 * @param applicationName
	 *            The name of the application deployed in the requested GSCs
	 * @param serviceName
	 *            The name of the service deployed in the requested GSCs
	 * @return a set of UIDs of the GSCs that run the given service in the context of the given application
	 * @throws CLIException
	 *             Reporting a failure to locate the requested GSCs or get the UIDs.
	 */
	public Set<String> getGridServiceContainerUidsForService(
			final String applicationName, final String serviceName)
			throws CLIException {

		final Set<String> containerUids = new HashSet<String>();

		final int numberOfInstances = this.getInstanceList(applicationName,
				serviceName).size();

		final String absolutePUName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		for (int i = 0; i < numberOfInstances; i++) {

			final String containerUrl = "applications/Names/" + applicationName
					+ "/ProcessingUnits/Names/" + absolutePUName
					+ "/Instances/" + i + "/GridServiceContainer/Uid";
			try {
				final Map<String, Object> container = client
						.getAdmin(containerUrl);

				if (container == null) {
					throw new IllegalStateException("Could not find container "
							+ containerUrl);
				}
				if (!container.containsKey("Uid")) {
					throw new IllegalStateException(
							"Could not find AgentUid of container "
									+ containerUrl);
				}

				containerUids.add((String) container.get("Uid"));
			} catch (final ErrorStatusException e) {
				throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
			} catch (final RestException e) {
				throw new CLIStatusException(e, "cant_find_service_for_app",
						serviceName, applicationName);
			}
		}
		return containerUids;
	}

	/**
	 * Gets the UIDs of all GSCs.
	 *
	 * @return a set of UIDs
	 * @throws CLIException
	 *             Reporting a failure to locate the requested GSCs or get the UIDs.
	 */
	public Set<String> getGridServiceContainerUids() throws CLIException {
		Map<String, Object> container;
		try {
			container = client.getAdmin("GridServiceContainers");
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}
		@SuppressWarnings("unchecked")
		final List<String> containerUris = (List<String>) container
				.get("Uids-Elements");
		final Set<String> containerUids = new HashSet<String>();
		for (final String containerUri : containerUris) {
			final String uid = containerUri.substring(containerUri
					.lastIndexOf('/') + 1);
			containerUids.add(uid);
		}
		return containerUids;
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, String> dumpAgent(final String ip) throws CLIException {

		final String url = SERVICE_CONTROLLER_URL + "dump/" + ip;
		try {
			@SuppressWarnings("unchecked")
			final Map<String, String> response = (Map<String, String>) client
					.get(url);
			return response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Map<String, String> installApplication(final File applicationFile,
			final String applicationName, final String authGroups, final int timeout,
			final boolean selfHealing, final File applicationOverrides,
			final File cloudOverrides) throws CLIException {

		final String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/timeout/" + timeout
				+ "?selfHealing=" + Boolean.toString(selfHealing);

		final Map<String, File> filesToPost = new HashMap<String, File>();
		filesToPost.put("file", applicationFile);
		filesToPost.put(CloudifyConstants.CLOUD_OVERRIDES_FILE_PARAM, cloudOverrides);
		filesToPost.put(CloudifyConstants.APPLICATION_OVERRIDES_FILE_PARAM, applicationOverrides);

		try {
			if (org.apache.commons.lang.StringUtils.isBlank(authGroups)) {
				return (Map<String, String>) client.postFiles(url, null/* props */
						, null/* params */, filesToPost);
			}
			final Map<String, String> paramsMap = new HashMap<String, String>();
			paramsMap.put("authGroups", authGroups);
			return (Map<String, String>) client.postFiles(url, null/* props */
					, paramsMap, filesToPost);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}
	}

	@Override
	public Map<String, String> setInstances(final String applicationName,
			final String serviceName, final int count,
			final boolean locationAware, final int timeout) throws CLIException {

		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/timeout/"
				+ timeout + "/set-instances?count=" + count
				+ "&location-aware=" + locationAware;
		try {
			@SuppressWarnings("unchecked")
			final Map<String, String> response = (Map<String, String>) client
					.post(url);
			return response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

	}

	@Override
	public void updateAttributes(final String scope,
			final String appplicationName, final Map<String, String> attributes)
			throws CLIException {
		final String url = getRelativeUrlForAttributes(scope, appplicationName);
		try {
			client.post(url, attributes);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

	}

	@Override
	public Map<String, String> listAttributes(final String scope,
			final String applicationName) throws CLIException {
		final String url = getRelativeUrlForAttributes(scope, applicationName);
		try {
			@SuppressWarnings("unchecked")
			final Map<String, String> response = (Map<String, String>) client
					.get(url, null);
			return response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	@Override
	public void deleteAttributes(final String scope,
			final String applicationName, final String... attributeNames)
			throws CLIException {
		final String url = getRelativeUrlForAttributes(scope, applicationName);
		for (final String attributeName : attributeNames) {
			try {
				client.delete(url + "/" + attributeName);
			} catch (final ErrorStatusException e) {
				throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
			}
		}

	}

	private String getRelativeUrlForAttributes(final String scope,
			final String applicationName) throws CLIException {
		if ("global".equals(scope)) {
			return "/attributes/globals";
		}
		if ("application".equals(scope)) {
			return "/attributes/applications/" + applicationName;
		}
		if (!scope.toLowerCase().startsWith("service")) {
			throw new CLIStatusException("illegal_scope", scope);
		}
		final String[] serviceScope = scope.split(":");
		if (serviceScope.length < 2) {
			throw new CLIStatusException(
					"service_scope_must_contain_service_name");
		}
		final String serviceName = serviceScope[1];
		if (serviceScope.length == 2) {
			return "/attributes/services/" + applicationName + "/"
					+ serviceName;
		}
		final String instanceIdentifier = serviceScope[2];
		if ("all-instances".equalsIgnoreCase(instanceIdentifier)) {
			return "/attributes/instances/" + applicationName + "/" + serviceName;
		}
		final Integer instanceId = StringUtils.safeParseInt(instanceIdentifier);
		if (instanceIdentifier == null) {
			throw new CLIStatusException("illegal_instance_identifier",
					instanceIdentifier);
		}
		validateInstanceId(instanceId, applicationName, serviceName);
		return "/attributes/instances/" + applicationName + "/" + serviceName
				+ "/" + instanceIdentifier;
	}

	private void validateInstanceId(final Integer instanceId,
			final String applicationName, final String serviceName)
			throws CLIException {
		if (instanceId != null) {
			final Map<String, Object> instanceList = getInstanceList(
					applicationName, serviceName);
			if (instanceList == null || instanceList.isEmpty()) {
				throw new CLIStatusException("service_has_no_instances",
						serviceName);
			}
		}
	}

	@Override
	public String getTailByInstanceId(final String serviceName,
			final String applicationName, final int instanceId,
			final int numLines) throws CLIException {
		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/instances/"
				+ instanceId + "/tail/" + "?numLines=" + numLines;
		try {
			final String response = (String) client.get(url);
			return response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	@Override
	public String getTailByHostAddress(final String serviceName,
			final String applicationName, final String hostAddress,
			final int numLines) throws CLIException {
		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/address/"
				+ hostAddress + "/tail/" + "?numLines=" + numLines;
		try {
			final String response = (String) client.get(url);
			return response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	@Override
	public String getTailByServiceName(final String serviceName,
			final String applicationName, final int numLines)
			throws CLIException {
		final String url = SERVICE_CONTROLLER_URL + "applications/"
				+ applicationName + "/services/" + serviceName + "/tail/"
				+ "?numLines=" + numLines;
		try {
			final String response = (String) client.get(url);
			return response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<String> addTemplates(final File templatesFile)
			throws CLIException {
		final String url = SERVICE_CONTROLLER_URL + "templates";
		List<String> response;
		try {
			final Map<String, File> fileToPost = new HashMap<String, File>();
			fileToPost.put(CloudifyConstants.TEMPLATES_DIR_PARAM_NAME, templatesFile);
			response = (List<String>) client.postFiles(url, fileToPost);
		} catch (final ErrorStatusException es) {
			throw new CLIStatusException(es, es.getReasonCode(), es.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}
		return response;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, ComputeTemplate> listTemplates() 
			throws CLIStatusException {
		final String url = SERVICE_CONTROLLER_URL + "templates";
		Map<String, ComputeTemplate> response = new HashMap<String, ComputeTemplate>();
		try {
			Map<String, Object> responseMap = (Map<String, Object>) client.get(url);
			for (Entry<String, Object> entry : responseMap.entrySet()) {
				ObjectMapper mapper = new ObjectMapper();
				ComputeTemplate convertValue = mapper.convertValue(entry.getValue(), ComputeTemplate.class);
				response.put(entry.getKey(), convertValue);
			}
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
		return response;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ComputeTemplate getTemplate(final String templateName) 
			throws CLIStatusException {
		final String url = SERVICE_CONTROLLER_URL + "templates/" + templateName;
		ComputeTemplate response;
		try {
			final Object result = client.get(url);

			final ObjectMapper mapper = new ObjectMapper();
			response = mapper.convertValue(result, ComputeTemplate.class);

		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
		return response;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeTemplate(final String templateName)
			throws CLIStatusException {
		final String url = SERVICE_CONTROLLER_URL + "templates/" + templateName;
		try {
			client.delete(url);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	@Override
	public void hasInstallPermissions(final String applicationName) throws CLIStatusException {
		final String url = SERVICE_CONTROLLER_URL + "application/" + applicationName + "/install/permissions";
		try {
			client.get(url);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	@Override
	public List<ControllerDetails> shutdownManagers() throws CLIException {

		try {
			List<ControllerDetails> controllers = this.client.shutdownManagers();
			return controllers;
		} catch (ErrorStatusException e) {
			throw new CLIStatusException(e);
		}

	}

	public URL getUrl() {
		return urlObj;
	}

	@Override
	public List<ControllerDetails> getManagers() throws CLIException {
		try {
			return client.getManagers();
		} catch (ErrorStatusException e) {
			throw new CLIStatusException(e);
		}
	}

}
