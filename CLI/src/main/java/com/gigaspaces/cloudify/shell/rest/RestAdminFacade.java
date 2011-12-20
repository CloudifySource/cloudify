/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.gigaspaces.cloudify.shell.rest;

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

import org.fusesource.jansi.Ansi.Color;

import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.utils.ServiceUtils;
import com.gigaspaces.cloudify.shell.AbstractAdminFacade;
import com.gigaspaces.cloudify.shell.ComponentType;
import com.gigaspaces.cloudify.shell.ShellUtils;
import com.gigaspaces.cloudify.shell.commands.CLIException;

/**
 * @author rafi
 * @since 8.0.3
 */
public class RestAdminFacade extends AbstractAdminFacade {

	private static final int PROCESSINGUNIT_LOOKUP_TIMEOUT = 30;
	private static final int POLLING_INTERVAL = 2000;
	private static final String GS_USM_COMMAND_NAME = "GS_USM_CommandName";
	private static final String SERVICE_CONTROLLER_URL = "/service/";
	private static final String CLOUD_CONTROLLER_URL = "/cloudcontroller/";

	private GSRestClient client;
	private EventLoggingTailer eventLoggingTailer;

	@Override
	public void doConnect(String user, String password, String url)
	throws CLIException {

		if (!url.endsWith("/")) {
			url = url + "/";
		}
		if (!url.startsWith("http://")) {
			url = "http://" + url;
		}

		URL urlObj;
		try {
			urlObj = new URL(url);
		} catch (MalformedURLException e) {
			throw new ErrorStatusException("could_not_parse_url", url);
		}

		client = new GSRestClient(user, password, urlObj);
		eventLoggingTailer = new EventLoggingTailer();
		// test connection
		client.get(SERVICE_CONTROLLER_URL + "testrest");
	}
	/**
	 * This method waits for the specified number of planned instances to be installed.
	 * In case of a datagrid or a stateful PU the specified value is ignored and the 
	 * return value indicates the correct planned number of instances.
	 */
	//TODO: Move this method or at least logger.info() code outside of RestAdminFacade
	@Override
	public boolean waitForServiceInstances(String serviceName, String applicationName, int plannedNumberOfInstances, String timeoutErrorMessage, long timeout, TimeUnit timeunit) throws CLIException, TimeoutException, InterruptedException {
		long end = System.currentTimeMillis() + timeunit.toMillis(timeout);

		int serviceShutDownEventsCount = 0;

		String pollingURL = "processingUnits/Names/" + ServiceUtils.getAbsolutePUName(applicationName, serviceName);

		//The polling will not start until the service processing unit is found.
		waitForServicePU(applicationName, serviceName, pollingURL, timeoutErrorMessage, PROCESSINGUNIT_LOOKUP_TIMEOUT, TimeUnit.SECONDS);

		logger.info(MessageFormat.format(messages.getString("deploying_service"),serviceName));

		int currentNumberOfNonUSMInstances = -1;
		int currentNumberOfRunningUSMInstances = -1;
		boolean statusChanged = false;
		
		while (System.currentTimeMillis() < end) {
			
			Map<String, Object> serviceStatusMap = client.getAdminData(pollingURL);
			if ("partitioned-sync2backup".equals(serviceStatusMap.get("ClusterSchema"))) {
				plannedNumberOfInstances = Integer.valueOf((String) serviceStatusMap.get("TotalNumberOfInstances"));
			}
			
			//Update all service instance numbers.
			//isUsmService can only be called when an instance of the service exists. 
			if (!serviceStatusMap.get("Instances-Size").equals(0) && isUSMService(applicationName, serviceName)){
				int actualNumberOfUSMServicesWithRunningState = getNumberOfUSMServicesWithRunningState(serviceName, 
															applicationName,
															(Integer)serviceStatusMap.get("Instances-Size"));
				if(currentNumberOfRunningUSMInstances != actualNumberOfUSMServicesWithRunningState && actualNumberOfUSMServicesWithRunningState != 0){
					currentNumberOfRunningUSMInstances = actualNumberOfUSMServicesWithRunningState;
					statusChanged = true;
				}else{
					statusChanged = false;
				}
			}else{//Not a USM Service
				
				int actualNumberOfInstances = (Integer)serviceStatusMap.get("Instances-Size");
				if (actualNumberOfInstances != currentNumberOfNonUSMInstances && actualNumberOfInstances != 0){
					currentNumberOfNonUSMInstances = actualNumberOfInstances;
					statusChanged = true;
				}else{
					statusChanged = false;
				}
			}

			//Print the event logs and return shutdown event count.
			serviceShutDownEventsCount = handleEventLogs(serviceName, applicationName, plannedNumberOfInstances, serviceShutDownEventsCount);
				
			if ((Integer)serviceStatusMap.get("Instances-Size") == 0){
				printStatusMessage(plannedNumberOfInstances, (Integer)serviceStatusMap.get("Instances-Size"), statusChanged);
			//Too many instances.
			}else if (plannedNumberOfInstances < currentNumberOfNonUSMInstances ||
					plannedNumberOfInstances < currentNumberOfRunningUSMInstances ){
				throw new CLIException(MessageFormat.format(
						messages.getString("number_of_instances_exceeded_planned"),
						plannedNumberOfInstances, Math.max(currentNumberOfNonUSMInstances, currentNumberOfRunningUSMInstances))); 
			}else if ((Integer)serviceStatusMap.get("Instances-Size") > 0){
				if (isUSMService(applicationName, serviceName)){
					currentNumberOfRunningUSMInstances = getNumberOfUSMServicesWithRunningState(serviceName, 
								applicationName,
								(Integer)serviceStatusMap.get("Instances-Size"));
					printStatusMessage(plannedNumberOfInstances, currentNumberOfRunningUSMInstances, statusChanged);
					
					//are all usm service instances in Running state?
					if (currentNumberOfRunningUSMInstances == plannedNumberOfInstances){
						return true;
					}
				}else{//non USM Service.
					printStatusMessage(plannedNumberOfInstances, currentNumberOfNonUSMInstances, statusChanged);
					//if all services up, return.
					if (plannedNumberOfInstances  == currentNumberOfNonUSMInstances){
						return true;
					}
				}
			}
			Thread.sleep(POLLING_INTERVAL);
		}
		throw new TimeoutException(timeoutErrorMessage);
		
	}
	
	private void printStatusMessage(int plannedNumberOfInstances,
			Integer currentNumberOfInstances, boolean statusChanged) {
		if (statusChanged){
			//Treat special cases. doesn't print newline in beginning of the installation 
			if ((!currentNumberOfInstances.equals(0))){
				System.out.println('.');
				System.out.flush();
			}
			logger.info(MessageFormat.format(
					messages.getString("deploying_service_updates"),
					plannedNumberOfInstances, currentNumberOfInstances));
			
			}else{//Status hasn't changed. print a '.'
				System.out.print('.');
				System.out.flush();
			}
	}

	private void printEventLogs(List<String> events){
		if (events.size() != 0){
			System.out.println('.');
			System.out.flush();
		}
		for (String eventString : events) {
			if (eventString.contains(CloudifyConstants.USM_EVENT_EXEC_SUCCESSFULLY)){
				System.out.println(eventString + " " 
						+ ShellUtils.getColorMessage(CloudifyConstants.USM_EVENT_EXEC_SUCCEED_MESSAGE, Color.GREEN));
			}else if (eventString.contains(CloudifyConstants.USM_EVENT_EXEC_FAILED)){
				System.out.println(eventString + " " 
						+ ShellUtils.getColorMessage(CloudifyConstants.USM_EVENT_EXEC_FAILED_MESSAGE, Color.RED));
			}else
			System.out.println(eventString);
		}
	}
	
	//Prints event logs and monitors shutdown events.
	private int handleEventLogs(String serviceName, String applicationName,
			int plannedNumberOfInstances, int serviceShutDownEventsCount)
			throws CLIException {
		List<String> eventLogs = getUnreadEventLogs(applicationName, serviceName);
		if (eventLogs != null){
			printEventLogs(eventLogs);
			serviceShutDownEventsCount += getShutdownEventCount(eventLogs, serviceName);
			//If all planned instances of the processing unit had Shutdown we throw an exception.
			if (serviceShutDownEventsCount == plannedNumberOfInstances){
				throw new CLIException("Service " + serviceName + " Failed to instantiate");
			}
		}
		return serviceShutDownEventsCount;
	}

	//returns the number of RUNNING processing unit instances.
	private int getNumberOfUSMServicesWithRunningState(String serviceName,
			String applicationName, int currentNumberOfInstances) throws CLIException {

		String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		String serviceMonitorsUrl = "ProcessingUnits/Names/"
			+ absolutePUName 
			+ "/ProcessingUnitInstances/%d/Statistics/Monitors/USM/Monitors/"
			+ CloudifyConstants.USM_MONITORS_STATE_ID;

		int runningServiceInstanceCounter = 0;
		
		for (int i = 0; i < currentNumberOfInstances; i++) {
			String instanceUrl = String.format(serviceMonitorsUrl, i);
			Map<String, Object> map = client.getAdminData(instanceUrl);
			int instanceState = Integer.valueOf((String)map.get(CloudifyConstants.USM_MONITORS_STATE_ID));
			if (CloudifyConstants.USMState.values()[instanceState].equals(CloudifyConstants.USMState.RUNNING)){
				runningServiceInstanceCounter++;
			}
		}
		return runningServiceInstanceCounter;
	}

	private boolean isUSMService(String applicationName, String serviceName) {
		String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);	
		String usmUrl = "ProcessingUnits/Names/"
			+ absolutePUName + 
			"/Instances/0/Statistics/" 
			+ "Monitors/"
			+ CloudifyConstants.USM_MONITORS_SERVICE_ID;
		try{
			Map<String, Object> adminData = client.getAdminData(usmUrl);
			if (adminData.containsKey("Id") 
					&& adminData.get("Id").equals(CloudifyConstants.USM_MONITORS_SERVICE_ID)){
				return true;
			}else if (adminData.containsKey(CloudifyConstants.USM_MONITORS_SERVICE_ID) 
					&& adminData.get(CloudifyConstants.USM_MONITORS_SERVICE_ID).equals("<null>")){
				return false;
			}
		}catch (CLIException e){
			
		}
		return false;
	}

	private void waitForServicePU(String applicationName, String serviceName, String url, String timeoutErrorMessage, long timeout, TimeUnit timeunit) throws TimeoutException, InterruptedException {

		long end = System.currentTimeMillis() + timeunit.toMillis(timeout);
		String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		while (System.currentTimeMillis() < end) {
			try{ 
				Map<String, Object> pu = client.getAdminData(url);
				if (absolutePuName.equals(pu.get("Name"))) {
					return;
				}
			}catch (CLIException e) {
				Thread.sleep(POLLING_INTERVAL);
			}
			Thread.sleep(POLLING_INTERVAL);
		}
		throw new TimeoutException(timeoutErrorMessage);
	}

	private List<String> getUnreadEventLogs(String applicationName, String serviceName) throws CLIException{
		List<Map<String, String>> allEventsExecutedList = (List<Map<String, String>>) client
		.get(SERVICE_CONTROLLER_URL + "/applications/"
				+ applicationName + "/services/"
				+ serviceName
				+ "/USMEventsLogs/");
		return eventLoggingTailer.getLinesToPrint(allEventsExecutedList);
	}

	private int getShutdownEventCount(List<String> events, String serviceName){
		int shutdownEventCount = 0;
		for (String eventString : events) {
			if (eventString.contains(serviceName) && eventString.contains("SHUTDOWN invoked")){
				shutdownEventCount++;
			}
		}
		return shutdownEventCount;
	}


	@Override
	public void doDisconnect() {
		client = null;
	}

	public void removeInstance(String applicationName, String serviceName,
			int instanceId) throws CLIException {
		String relativeUrl = SERVICE_CONTROLLER_URL + "applications/"
		+ applicationName + "/services/" + serviceName + "/instances/"
		+ instanceId + "/remove";
		client.delete(relativeUrl);
	}

	public void restart(ComponentType componentType, String componentName,
			Set<Integer> componentInstanceIDs) throws CLIException {
		// TODO: Implement
	}

	@SuppressWarnings("unchecked")
	public List<String> getApplicationsList() throws CLIException {
		return (List<String>) client.get("/service/applications");
	}

	@SuppressWarnings("unchecked")
	public List<String> getServicesList(String applicationName)
	throws CLIException {
		return (List<String>)client.get("/service/applications/"
				+ applicationName + "/services");
	}

	@Override
	protected String doDeploy(String applicationName, File packedFile)
	throws CLIException {
		return client.postFile(SERVICE_CONTROLLER_URL + CLOUD_CONTROLLER_URL
				+ "deploy?" + "applicationName=" + applicationName, packedFile);
	}

	public void startService(String applicationName, File serviceFile)
	throws CLIException {
		doDeploy(applicationName, serviceFile);
	}

	public void undeploy(String applicationName, String serviceName)
	throws CLIException {
		String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName
		+ "/services/" + serviceName + "/undeploy";
		client.delete(url);
	}

	public void addInstance(String applicationName, String serviceName,
			int timeout) throws CLIException {
		String url = SERVICE_CONTROLLER_URL + "applications/na/services/"
		+ serviceName + "/addinstance";
		Map<String, String> params = new HashMap<String, String>();
		params.put("timeout", Integer.toString(timeout));
		client.post(url, params);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getInstanceList(String applicationName,
			String serviceName) throws CLIException {
		String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName
		+ "/services/" + serviceName + "/instances";
		return (Map<String, Object>) client.get(url);
	}

	public void installElastic(File packedFile, String applicationName,
			String serviceName, String zone, Properties contextProperties)
	throws CLIException {

		String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName
		+ "/services/" + serviceName;
		client.postFile(url + "?zone=" + zone, packedFile, contextProperties);
	}

	public Map<String, InvocationResult> invokeServiceCommand(
			String applicationName, String serviceName, String beanName,
			String commandName, Map<String, String> params) throws CLIException {
		String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName
		+ "/services/" + serviceName + "/beans/" + beanName + "/invoke";

		Object result = client.post(url, buildCustomCommandParams(commandName, params));

		@SuppressWarnings("unchecked")
		Map<String, Object> restResult = (Map<String, Object>) result;

		Map<String, InvocationResult> invocationResultMap = new LinkedHashMap<String, InvocationResult>();
		for (Map.Entry<String, Object> entry : restResult.entrySet()) {
			Object value = entry.getValue();

			if (!(value instanceof Map<?, ?>)) {
				logger.severe("Received an unexpected return value to the invoke command. Key: "
						+ entry.getKey() + ", value: " + value);
			} else {
				@SuppressWarnings("unchecked")
				Map<String, String> curr = (Map<String, String>) value;
				InvocationResult invocationResult = InvocationResult
				.createInvocationResult(curr);
				invocationResultMap.put(entry.getKey(), invocationResult);
			}

		}
		return invocationResultMap;
	}

	public InvocationResult invokeInstanceCommand(String applicationName,
			String serviceName, String beanName, int instanceId,
			String commandName, Map<String, String> paramsMap) throws CLIException {
		String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName + "/services/"
		+ serviceName + "/instances/" + instanceId + "/beans/"
		+ beanName + "/invoke";
		@SuppressWarnings("unchecked")
		Map<String, String> resultMap = (Map<String, String>) client.post(url,
				buildCustomCommandParams(commandName, paramsMap));

		// resultMap.entrySet().iterator().next();
		// InvocationResult invocationResult = InvocationResult
		// .createInvocationResult(resultMap);
		// @SuppressWarnings("unchecked")
		// Map<String, String> curr = (Map<String, String>) resultMap;
		// InvocationResult invocationResult = InvocationResult
		// .createInvocationResult(curr);
		//
		InvocationResult invocationResult = InvocationResult
		.createInvocationResult(resultMap);

		return invocationResult;
		// return GSRestClient.mapToInvocationResult(resultMap);

	}

	private Map<String, String> buildCustomCommandParams(String commandName, Map<String, String> parametersMap) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(GS_USM_COMMAND_NAME, commandName);
		//add all of the predefined parameters into the params map.
		for (Map.Entry<String, String> entry : parametersMap.entrySet()) {
			params.put(entry.getKey(), entry.getValue());
		}
		return params;
	}

	@Override
	public List<String> getMachines() throws CLIException {
		Map<String, Object> map = client
		.getAdminData("machines/HostsByAddress");
		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) map.get("HostsByAddress-Elements");
		List<String> result = new ArrayList<String>(list.size());
		for (String host : list) {
			String[] parts = host.split("/");
			String ip = parts[parts.length - 1];
			result.add(ip);
		}
		return result;
	}

	@Override
	public void uninstallApplication(String applicationName)
	throws CLIException {
		String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName;
		client.delete(url);
	}

	public Set<String> getGridServiceContainerUidsForApplication(
			String applicationName) throws CLIException {
		Set<String> containerUids = new HashSet<String>();

		for (String serviceName : getServicesList(applicationName)) {
			containerUids.addAll(getGridServiceContainerUidsForService(
					applicationName, serviceName));
		}
		return containerUids;
	}

	public Set<String> getGridServiceContainerUidsForService(
			String applicationName, String serviceName) throws CLIException {

		Set<String> containerUids = new HashSet<String>();

		int numberOfInstances = this.getInstanceList(applicationName,
				serviceName).size();

		String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		for (int i = 0; i < numberOfInstances; i++) {

			String containerUrl = "applications/Names/" + applicationName
			+ "/ProcessingUnits/Names/" + absolutePUName + "/Instances/"
			+ i + "/GridServiceContainer/Uid";
			try{
				Map<String, Object> container = (Map<String, Object>) client
				.getAdmin(containerUrl);

				if (container == null) {
					throw new IllegalStateException("Could not find container "
							+ containerUrl);
				}
				if (!container.containsKey("Uid")) {
					throw new IllegalStateException(
							"Could not find AgentUid of container " + containerUrl);
				}

				containerUids.add((String) container.get("Uid"));
			}catch (CLIException e) {
				throw new ErrorStatusException("cant_find_service_for_app", serviceName, applicationName);
			}
		}
		return containerUids;
	}

	public Set<String> getGridServiceContainerUids() throws CLIException {
		Map<String, Object> container = (Map<String, Object>) client
		.getAdmin("GridServiceContainers");
		@SuppressWarnings("unchecked")
		List<String> containerUris = (List<String>) container
		.get("Uids-Elements");
		Set<String> containerUids = new HashSet<String>();
		for (String containerUri : containerUris) {
			String uid = containerUri.substring(containerUri.lastIndexOf("/")+1);
			containerUids.add(uid);
		}
		return containerUids;
	}

	@Override
	public String installApplication(File applicationFile,
			String applicationName) throws CLIException {
		String url = SERVICE_CONTROLLER_URL + "applications/" + applicationName;
		return client.postFile(url, applicationFile);

	}

}
