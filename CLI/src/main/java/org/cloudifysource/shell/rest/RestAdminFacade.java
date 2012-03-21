/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.shell.rest;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.restclient.ErrorStatusException;
import org.cloudifysource.restclient.EventLoggingTailer;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.InvocationResult;
import org.cloudifysource.restclient.RestException;
import org.cloudifysource.shell.AbstractAdminFacade;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ComponentType;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, barakm, adaml, noak
 * @since 2.0.0 This class implements the {@link AdminFacade}, relying on the abstract implementation of
 *        {@link AbstractAdminFacade}. It discovers and manages applications, services, containers and other components
 *        over REST, using the {@link GSRestClient}.
 */
public class RestAdminFacade extends AbstractAdminFacade {

	private static final int PROCESSINGUNIT_LOOKUP_TIMEOUT = 30;
	private static final int POLLING_INTERVAL = 2000;
	private static final String GS_USM_COMMAND_NAME = "GS_USM_CommandName";
	private static final String SERVICE_CONTROLLER_URL = "/service/";
	private static final String CLOUD_CONTROLLER_URL = "/cloudcontroller/";

	private GSRestClient client;
	private EventLoggingTailer eventLoggingTailer;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doConnect(final String user, final String password, final String url) throws CLIException {
		String formattedURL = url;
		if (!formattedURL.endsWith("/")) {
			formattedURL = formattedURL + "/";
		}
		if (!formattedURL.startsWith("http://")) {
			formattedURL = "http://" + formattedURL;
		}

		URL urlObj;
		try {
			urlObj = new URL(formattedURL);
			if (urlObj.getPort() == -1) {
				urlObj = getUrlWithDefaultPort(urlObj);
			}
		} catch (final MalformedURLException e) {
			throw new CLIStatusException("could_not_parse_url", url, e);
		}

		try {
			client = new GSRestClient(user, password, urlObj);
			eventLoggingTailer = new EventLoggingTailer();
			// test connection
			client.get(SERVICE_CONTROLLER_URL + "testrest");
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}
	}

	private URL getUrlWithDefaultPort(final URL urlObj) throws MalformedURLException {
		StringBuffer url = new StringBuffer(urlObj.toString());
		final int portIndex = url.indexOf("/", "http://".length());
		url = url.insert(portIndex, ":" + CloudifyConstants.DEFAULT_REST_PORT);
		return new URL(url.toString());
	}

	// TODO: Move this method or at least logger.info() code outside of RestAdminFacade
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean waitForServiceInstances(final String serviceName, final String applicationName,
			final int plannedNumberOfInstances, final String timeoutErrorMessage, final long timeout,
			final TimeUnit timeunit) throws CLIException, TimeoutException, InterruptedException {

		final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);

		int serviceShutDownEventsCount = 0;
		int calcedNumberOfInstances = plannedNumberOfInstances;

		final String pollingURL = "processingUnits/Names/"
				+ ServiceUtils.getAbsolutePUName(applicationName, serviceName);

		// The polling will not start until the service processing unit is found.
		waitForServicePU(applicationName, serviceName, pollingURL, timeoutErrorMessage, PROCESSINGUNIT_LOOKUP_TIMEOUT,
				timeunit);

		logger.info(MessageFormat.format(messages.getString("deploying_service"), serviceName));

		int currentNumberOfNonUSMInstances = -1;
		int currentNumberOfRunningUSMInstances = -1;
		boolean statusChanged = false;

		while (System.currentTimeMillis() < end) {

			Map<String, Object> serviceStatusMap = null;
			try {
				serviceStatusMap = client.getAdminData(pollingURL);
			} catch (final ErrorStatusException e) {
				throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
			} catch (final RestException e) {
				throw new CLIException(e);
			}

			if (serviceStatusMap.containsKey("ClusterSchema")
					&& "partitioned-sync2backup".equals(serviceStatusMap.get("ClusterSchema"))) {
				calcedNumberOfInstances = Integer.parseInt((String) serviceStatusMap.get("TotalNumberOfInstances"));
			}

			boolean serviceInstanceExists = false;
			int serviceInstancesSize = 0;
			if (serviceStatusMap.containsKey("Instances-Size")) {
				serviceInstancesSize = (Integer) serviceStatusMap.get("Instances-Size");
				if (!serviceStatusMap.get("Instances-Size").equals(0)) {
					serviceInstanceExists = true;
				}
			}
			// isUsmService can only be called when an instance of the service exists.
			if (serviceInstanceExists && isUSMService(applicationName, serviceName)) {
				final int actualNumberOfUSMServicesWithRunningState = getNumberOfUSMServicesWithRunningState(
						serviceName, applicationName, serviceInstancesSize);
				if (currentNumberOfRunningUSMInstances != actualNumberOfUSMServicesWithRunningState
						&& actualNumberOfUSMServicesWithRunningState != 0) {
					currentNumberOfRunningUSMInstances = actualNumberOfUSMServicesWithRunningState;
					statusChanged = true;
				} else {
					statusChanged = false;
				}
			} else { // Not a USM Service

				final int actualNumberOfInstances = serviceInstancesSize;
				if (actualNumberOfInstances != currentNumberOfNonUSMInstances && actualNumberOfInstances != 0) {
					currentNumberOfNonUSMInstances = actualNumberOfInstances;
					statusChanged = true;
				} else {
					statusChanged = false;
				}
			}

			// Print the event logs and return shutdown event count.
			serviceShutDownEventsCount = handleEventLogs(serviceName, applicationName, calcedNumberOfInstances,
					serviceShutDownEventsCount);

			if (serviceInstancesSize == 0) {
				printStatusMessage(calcedNumberOfInstances, serviceInstancesSize, statusChanged);
				// Too many instances.
			} else if (calcedNumberOfInstances < currentNumberOfNonUSMInstances
					|| calcedNumberOfInstances < currentNumberOfRunningUSMInstances) {
				throw new CLIException(MessageFormat.format(messages.getString("number_of_instances_exceeded_planned"),
						calcedNumberOfInstances,
						Math.max(currentNumberOfNonUSMInstances, currentNumberOfRunningUSMInstances)));
			} else if (serviceInstancesSize > 0) {
				if (isUSMService(applicationName, serviceName)) {
					currentNumberOfRunningUSMInstances = getNumberOfUSMServicesWithRunningState(serviceName,
							applicationName, serviceInstancesSize);
					printStatusMessage(calcedNumberOfInstances, currentNumberOfRunningUSMInstances, statusChanged);

					// are all usm service instances in Running state?
					if (currentNumberOfRunningUSMInstances == calcedNumberOfInstances) {
						return true;
					}
				} else { // non USM Service.
					printStatusMessage(calcedNumberOfInstances, currentNumberOfNonUSMInstances, statusChanged);
					// if all services up, return.
					if (calcedNumberOfInstances == currentNumberOfNonUSMInstances) {
						return true;
					}
				}
			}
			Thread.sleep(POLLING_INTERVAL);
		}
		throw new TimeoutException(timeoutErrorMessage);

	}

	private void printStatusMessage(final int plannedNumberOfInstances, final Integer currentNumberOfInstances,
			final boolean statusChanged) {
		if (statusChanged) {
			// Treat special cases. doesn't print newline in beginning of the installation
			if (!currentNumberOfInstances.equals(0)) {
				System.out.println('.');
				System.out.flush();
			}
			logger.info(MessageFormat.format(messages.getString("deploying_service_updates"), plannedNumberOfInstances,
					currentNumberOfInstances));

		} else { // Status hasn't changed. print a '.'
			System.out.print('.');
			System.out.flush();
		}
	}

	private void printEventLogs(final List<String> events) {
		if (events.size() != 0) {
			System.out.println('.');
			System.out.flush();
		}
		for (final String eventString : events) {
			if (eventString.contains(CloudifyConstants.USM_EVENT_EXEC_SUCCESSFULLY)) {
				System.out.println(eventString + " "
						+ ShellUtils.getColorMessage(CloudifyConstants.USM_EVENT_EXEC_SUCCEED_MESSAGE, Color.GREEN));
			} else if (eventString.contains(CloudifyConstants.USM_EVENT_EXEC_FAILED)) {
				System.out.println(eventString + " "
						+ ShellUtils.getColorMessage(CloudifyConstants.USM_EVENT_EXEC_FAILED_MESSAGE, Color.RED));
			} else {
				System.out.println(eventString);
			}
		}
	}

	private int handleEventLogs(final String serviceName, final String applicationName,
			final int plannedNumberOfInstances, final int serviceShutDownEventsCount) throws CLIException {
		return handleEventLogs(serviceName, applicationName, plannedNumberOfInstances, serviceShutDownEventsCount,
				false);
	}

	// Prints event logs and monitors shutdown events.
	private int handleEventLogs(final String serviceName, final String applicationName,
			final int plannedNumberOfInstances, final int currentServiceShutDownEventsCount, final boolean firstTime)
			throws CLIException {
		int serviceShutDownEventsCount = currentServiceShutDownEventsCount;
		final List<String> eventLogs = getUnreadEventLogs(applicationName, serviceName);
		if (eventLogs != null) {
			if (!firstTime) {
				printEventLogs(eventLogs);
			}

			serviceShutDownEventsCount += getShutdownEventCount(eventLogs, serviceName);

			// If all planned instances of the processing unit had Shutdown we
			// throw an exception.
			if (serviceShutDownEventsCount == plannedNumberOfInstances) {
				throw new CLIException("Service " + serviceName + " Failed to instantiate");
			}
		}
		return serviceShutDownEventsCount;
	}

	private int handleEventLogsInSetInstances(final String serviceName, final String applicationName,
			final int plannedNumberOfInstances, final int currentServiceShutDownEventsCount, final boolean firstTime)
			throws CLIException {
		int serviceShutDownEventsCount = currentServiceShutDownEventsCount;
		final List<String> eventLogs = getUnreadEventLogs(applicationName, serviceName);
		if (eventLogs != null) {
			if (!firstTime) {
				printEventLogs(eventLogs);
			}

			serviceShutDownEventsCount += getShutdownEventCount(eventLogs, serviceName);
			
		}
		return serviceShutDownEventsCount;
	}

	
	// returns the number of RUNNING processing unit instances.
	private int getNumberOfUSMServicesWithRunningState(final String serviceName, final String applicationName,
			final int currentNumberOfInstances) throws CLIException {

		final String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		final String serviceMonitorsUrl = "ProcessingUnits/Names/" + absolutePUName
				+ "/ProcessingUnitInstances/%d/Statistics/Monitors/USM/Monitors/"
				+ CloudifyConstants.USM_MONITORS_STATE_ID;

		int runningServiceInstanceCounter = 0;

		try {
			for (int i = 0; i < currentNumberOfInstances; i++) {
				final String instanceUrl = String.format(serviceMonitorsUrl, i);
				final Map<String, Object> map = client.getAdminData(instanceUrl);
				final int instanceState = Integer.parseInt((String) map.get(CloudifyConstants.USM_MONITORS_STATE_ID));
				if (CloudifyConstants.USMState.values()[instanceState] == CloudifyConstants.USMState.RUNNING) {
					runningServiceInstanceCounter++;
				}
			}
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

		return runningServiceInstanceCounter;
	}

	private boolean isUSMService(final String applicationName, final String serviceName) {
		final String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		final String usmUrl = "ProcessingUnits/Names/" + absolutePUName + "/Instances/0/Statistics/" + "Monitors/"
				+ CloudifyConstants.USM_MONITORS_SERVICE_ID;
		try {
			final Map<String, Object> adminData = client.getAdminData(usmUrl);
			if (adminData.containsKey("Id") && adminData.get("Id").equals(CloudifyConstants.USM_MONITORS_SERVICE_ID)) {
				return true;
			} else if (adminData.containsKey(CloudifyConstants.USM_MONITORS_SERVICE_ID)
					&& adminData.get(CloudifyConstants.USM_MONITORS_SERVICE_ID).equals("<null>")) {
				return false;
			}
		} catch (final RestException e) {
			// The path doesn't exist. either not a usm service or no instance was created yet.
		}
		return false;
	}

	private void waitForServicePU(final String applicationName, final String serviceName, final String url,
			final String timeoutErrorMessage, final long timeout, final TimeUnit timeunit) throws TimeoutException,
			InterruptedException {

		final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);
		final String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		while (System.currentTimeMillis() < end) {
			try {
				final Map<String, Object> pu = client.getAdminData(url);
				if (absolutePuName.equals(pu.get("Name"))) {
					return;
				}
			} catch (final RestException e) {
				Thread.sleep(POLLING_INTERVAL);
			}
			Thread.sleep(POLLING_INTERVAL);
		}
		throw new TimeoutException(timeoutErrorMessage);
	}

	private List<String> getUnreadEventLogs(final String applicationName, final String serviceName) throws CLIException {

		List<Map<String, String>> allEventsExecutedList = null;
		try {
			allEventsExecutedList = (List<Map<String, String>>) client.get(SERVICE_CONTROLLER_URL + "/applications/"
					+ applicationName + "/services/" + serviceName + "/USMEventsLogs/");
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
		return eventLoggingTailer.getLinesToPrint(allEventsExecutedList);
	}

	private int getShutdownEventCount(final List<String> events, final String serviceName) {
		int shutdownEventCount = 0;
		for (final String eventString : events) {
			if (eventString.contains(serviceName) && eventString.contains("SHUTDOWN invoked")) {
				shutdownEventCount++;
			}
		}
		return shutdownEventCount;
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
	public void removeInstance(final String applicationName, final String serviceName, final int instanceId)
			throws CLIException {
		try {
			final String relativeUrl = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/services/"
					+ serviceName + "/instances/" + instanceId + "/remove";
			client.delete(relativeUrl);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void restart(final ComponentType componentType, final String componentName,
			final Set<Integer> componentInstanceIDs) throws CLIException {
		// TODO: Implement
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<String> getApplicationsList() throws CLIException {

		List<String> applicationsList = null;

		try {
			applicationsList = (List<String>) client.get("/service/applications");
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}

		return applicationsList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<String> getServicesList(final String applicationName) throws CLIException {

		List<String> servicesList = null;
		try {
			servicesList = (List<String>) client.get("/service/applications/" + applicationName + "/services");
		} catch (final ErrorStatusException ese) {
			throw new CLIStatusException(ese, ese.getReasonCode(), ese.getArgs());
		}

		return servicesList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String doDeploy(final String applicationName, final File packedFile) throws CLIException {

		String result = null;
		try {
			result = (String)client.postFile(SERVICE_CONTROLLER_URL + CLOUD_CONTROLLER_URL + "deploy?" + "applicationName="
					+ applicationName, packedFile);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String startService(final String applicationName, final File serviceFile) throws CLIException {
		return doDeploy(applicationName, serviceFile);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> undeploy(final String applicationName, final String serviceName, int timeoutInMinutes) throws CLIException {
		try {
			final String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/services/" + serviceName + "/timeout/" + timeoutInMinutes
					+ "/undeploy"  ;
			Map<String, String> response = (Map<String, String>) client.delete(url);
			return response;
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addInstance(final String applicationName, final String serviceName, final int timeout)
			throws CLIException {
		try {
			final String url = SERVICE_CONTROLLER_URL + "applications/na/services/" + serviceName + "/addinstance";
			final Map<String, String> params = new HashMap<String, String>();
			params.put("timeout", Integer.toString(timeout));
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
	@SuppressWarnings("unchecked")
	public Map<String, Object> getInstanceList(final String applicationName, final String serviceName)
			throws CLIException {

		Map<String, Object> instanceList = null;
		try {
			final String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/services/" + serviceName
					+ "/instances";
			instanceList = (Map<String, Object>) client.get(url);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		}

		return instanceList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String installElastic(final File packedFile, final String applicationName, final String serviceName,
			final String zone, final Properties contextProperties, final String templateName, int timeout) throws CLIException {

		String result = null;
		try {
			final String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/services/" + serviceName + "/timeout/" + timeout;
			result = (String)client.postFile(url + "?zone=" + zone + "&template=" + templateName, packedFile,
					contextProperties);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

		return result;
	}
	
	public void waitForLifecycleEvents(final String pollingID, int timeout, String timeoutMessage) throws CLIException, InterruptedException, TimeoutException {
		
		RestLifecycleEventsLatch restLifecycleEventsLatch = new RestLifecycleEventsLatch();
		restLifecycleEventsLatch.setTimeoutMessage(timeoutMessage);
		restLifecycleEventsLatch.waitForLifecycleEvents(pollingID, client, timeout);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, InvocationResult> invokeServiceCommand(final String applicationName, final String serviceName,
			final String beanName, final String commandName, final Map<String, String> params) throws CLIException {
		final String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/services/" + serviceName
				+ "/beans/" + beanName + "/invoke";

		Object result = null;
		try {
			result = client.post(url, buildCustomCommandParams(commandName, params));
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
				logger.severe("Received an unexpected return value to the invoke command. Key: " + entry.getKey()
						+ ", value: " + value);
			} else {
				@SuppressWarnings("unchecked")
				final Map<String, String> curr = (Map<String, String>) value;
				final InvocationResult invocationResult = InvocationResult.createInvocationResult(curr);
				invocationResultMap.put(entry.getKey(), invocationResult);
			}

		}
		return invocationResultMap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InvocationResult invokeInstanceCommand(final String applicationName, final String serviceName,
			final String beanName, final int instanceId, final String commandName, final Map<String, String> paramsMap)
			throws CLIException {
		final String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/services/" + serviceName
				+ "/instances/" + instanceId + "/beans/" + beanName + "/invoke";
		@SuppressWarnings("unchecked")
		Map<String, String> resultMap;
		try {
			resultMap = (Map<String, String>) client.post(url, buildCustomCommandParams(commandName, paramsMap));
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
		final InvocationResult invocationResult = InvocationResult.createInvocationResult(resultMap);

		return invocationResult;
		// return GSRestClient.mapToInvocationResult(resultMap);

	}

	private Map<String, String> buildCustomCommandParams(final String commandName,
			final Map<String, String> parametersMap) {
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

		Map<String, Object> map = null;
		try {
			map = client.getAdminData("machines/HostsByAddress");
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}
		@SuppressWarnings("unchecked")
		final List<String> list = (List<String>) map.get("HostsByAddress-Elements");
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
	public Map<String, String> uninstallApplication(final String applicationName, int timeoutInMinutes) throws CLIException {
		try {
			final String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/timeout/" + timeoutInMinutes; 
			Map<String, String> response = (Map<String, String>) client.delete(url);
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
	public Set<String> getGridServiceContainerUidsForApplication(final String applicationName) throws CLIException {
		final Set<String> containerUids = new HashSet<String>();

		for (final String serviceName : getServicesList(applicationName)) {
			containerUids.addAll(getGridServiceContainerUidsForService(applicationName, serviceName));
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
	public Set<String> getGridServiceContainerUidsForService(final String applicationName, final String serviceName)
			throws CLIException {

		final Set<String> containerUids = new HashSet<String>();

		final int numberOfInstances = this.getInstanceList(applicationName, serviceName).size();

		final String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		for (int i = 0; i < numberOfInstances; i++) {

			final String containerUrl = "applications/Names/" + applicationName + "/ProcessingUnits/Names/"
					+ absolutePUName + "/Instances/" + i + "/GridServiceContainer/Uid";
			try {
				final Map<String, Object> container = client.getAdmin(containerUrl);

				if (container == null) {
					throw new IllegalStateException("Could not find container " + containerUrl);
				}
				if (!container.containsKey("Uid")) {
					throw new IllegalStateException("Could not find AgentUid of container " + containerUrl);
				}

				containerUids.add((String) container.get("Uid"));
			} catch (final ErrorStatusException e) {
				throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
			} catch (final RestException e) {
				throw new CLIStatusException("cant_find_service_for_app", serviceName, applicationName);
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
		Map<String, Object> container = null;
		try {
			container = client.getAdmin("GridServiceContainers");
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}
		@SuppressWarnings("unchecked")
		final List<String> containerUris = (List<String>) container.get("Uids-Elements");
		final Set<String> containerUids = new HashSet<String>();
		for (final String containerUri : containerUris) {
			final String uid = containerUri.substring(containerUri.lastIndexOf('/') + 1);
			containerUids.add(uid);
		}
		return containerUids;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> installApplication(final File applicationFile, final String applicationName, int timeout) throws CLIException {

		Map<String, String> result = null;
		try {
			final String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/timeout/" + timeout;
			result = (Map<String, String>)client.postFile(url, applicationFile);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

		return result;
	}

	@Override
	public void setInstances(final String applicationName, final String serviceName, final int count)
			throws CLIException {
		try {
			final String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/services/" + serviceName
					+ "/set-instances?count=" + count;
			client.post(url);
		} catch (final ErrorStatusException e) {
			throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
		} catch (final RestException e) {
			throw new CLIException(e);
		}

	}

	@Override
	public boolean waitForInstancesNumberOnRunningService(final String serviceName, final String applicationName,
			final int plannedNumberOfInstances, final int initialNumberOfInstances, final String timeoutErrorMessage,
			final long timeout, final TimeUnit timeunit) throws CLIException, TimeoutException, InterruptedException {
		final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);

		final boolean ascending = plannedNumberOfInstances > initialNumberOfInstances;
		int serviceShutDownEventsCount = 0;

		final String pollingURL = "processingUnits/Names/"
				+ ServiceUtils.getAbsolutePUName(applicationName, serviceName);

		// The polling will not start until the service processing unit is
		// found.
		waitForServicePU(applicationName, serviceName, pollingURL, timeoutErrorMessage, PROCESSINGUNIT_LOOKUP_TIMEOUT,
				timeunit);

		if (ascending) {
			logger.info(MessageFormat.format(messages.getString("deploying_service"), serviceName));
		} else {
			logger.info(MessageFormat.format(messages.getString("removing_service_instances"), serviceName));
		}

		// TODO - this only works for USM. Fix for PUs.
		int currentNumberOfRunningUSMInstances = ascending ? -1 : Integer.MAX_VALUE;
		boolean statusChanged = false;

		boolean firstTime = true;
		while (System.currentTimeMillis() < end) {

			Map<String, Object> serviceStatusMap = null;
			try {
				serviceStatusMap = client.getAdminData(pollingURL);
			} catch (final ErrorStatusException e) {
				throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
			} catch (final RestException e) {
				throw new CLIException(e);
			}

			int targetNumberOfInstances = plannedNumberOfInstances;
			if ("partitioned-sync2backup".equals(serviceStatusMap.get("ClusterSchema"))) {
				targetNumberOfInstances = Integer.valueOf((String) serviceStatusMap.get("TotalNumberOfInstances"));
			}

			// Update all service instance numbers.
			// isUsmService can only be called when an instance of the service
			// exists.
			if (ascending) {
				if (!serviceStatusMap.get("Instances-Size").equals(0) && isUSMService(applicationName, serviceName)) {
					final int actualNumberOfUSMServicesWithRunningState = getNumberOfUSMServicesWithRunningState(
							serviceName, applicationName, (Integer) serviceStatusMap.get("Instances-Size"));
					if (currentNumberOfRunningUSMInstances != actualNumberOfUSMServicesWithRunningState
							&& actualNumberOfUSMServicesWithRunningState != 0) {
						currentNumberOfRunningUSMInstances = actualNumberOfUSMServicesWithRunningState;
						statusChanged = true;
					} else {
						statusChanged = false;
					}
				}
			} else {
				final int actualNumberOfUSMServicesWithRunningState = (Integer) serviceStatusMap.get("Instances-Size");
				if (currentNumberOfRunningUSMInstances != actualNumberOfUSMServicesWithRunningState
						&& actualNumberOfUSMServicesWithRunningState != 0) {
					currentNumberOfRunningUSMInstances = actualNumberOfUSMServicesWithRunningState;
					statusChanged = true;
				} else {
					statusChanged = false;
				}

			}

			// Print the event logs and return shutdown event count.

			serviceShutDownEventsCount = handleEventLogsInSetInstances(serviceName, applicationName, targetNumberOfInstances,
					serviceShutDownEventsCount, firstTime);
			firstTime = false;

			if ((Integer) serviceStatusMap.get("Instances-Size") == 0) {
				printStatusMessage(targetNumberOfInstances, (Integer) serviceStatusMap.get("Instances-Size"),
						statusChanged);
				// Too many instances.
			} else if (ascending && targetNumberOfInstances < currentNumberOfRunningUSMInstances) {
				throw new CLIException(MessageFormat.format(messages.getString("number_of_instances_exceeded_planned"),
						targetNumberOfInstances, currentNumberOfRunningUSMInstances));
			} else if (!ascending && targetNumberOfInstances > currentNumberOfRunningUSMInstances) {
				throw new CLIException(MessageFormat.format(
						messages.getString("number_of_instances_removed_exceeded_planned"), targetNumberOfInstances,
						currentNumberOfRunningUSMInstances));
			} else if ((Integer) serviceStatusMap.get("Instances-Size") > 0) {
				if (isUSMService(applicationName, serviceName)) {
					// currentNumberOfRunningUSMInstances = getNumberOfUSMServicesWithRunningState(
					// serviceName, applicationName,
					// (Integer) serviceStatusMap.get("Instances-Size"));
					printStatusMessage(targetNumberOfInstances, currentNumberOfRunningUSMInstances, statusChanged);

					// are all usm service instances in Running state?
					if (currentNumberOfRunningUSMInstances == targetNumberOfInstances) {
						// call this again, just to make sure we do not miss the last event
						handleEventLogsInSetInstances(serviceName, applicationName, targetNumberOfInstances,
								serviceShutDownEventsCount, firstTime);
						return true;
					}
				}
			}
			Thread.sleep(POLLING_INTERVAL);
		}
		throw new TimeoutException(timeoutErrorMessage);

	}
}
