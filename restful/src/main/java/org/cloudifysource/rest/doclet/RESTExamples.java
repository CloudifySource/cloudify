/*******************************************************************************
 * 
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
 *******************************************************************************/
package org.cloudifysource.rest.doclet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.math.RandomUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
import org.cloudifysource.dsl.internal.debug.DebugModes;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.InstanceDescription;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.dsl.rest.response.ServiceInstanceMetricsData;

/**
 * 
 * @author yael
 * @since 2.6.0
 */
public final class RESTExamples {

	private static final int TIMEOUT_MINUTES_EXAMPLE = 5;
	private static final int MAX_INSTANCE_ID = 5;

	private RESTExamples() {
	}

	static String getAttribute() {
		return "attributeValue";
	}

	static Map<String, Object> getAttributes() {
		final Map<String, Object> attributesExample = new HashMap<String, Object>();
		attributesExample.put("attr1", "value1");
		attributesExample.put("attr2", "value2");
		attributesExample.put("attr3", "value3");
		return attributesExample;
	}

	static String getAppName() {
		return "petclinic";
	}

	static ServiceInstanceMetricsData getServiceInstanceMetricsData() {
		final Map<String, Object> metricsExample = new HashMap<String, Object>();
		final int id1 = getInstanceId();
		final int id2 = id1 + 1;

		metricsExample.put("metric" + id1, "value" + id1);
		metricsExample.put("metric" + id2, "value" + id2);

		return new ServiceInstanceMetricsData(id1, metricsExample);
	}

	static String getServiceName() {
		return "tomcat";
	}

	static String getAuthGroup() {
		return "myAuthGroup";
	}

	static boolean isDebugAll() {
		return false;
	}

	static String getDebugEvents() {
		return "init,install";
	}

	static String getDebugMode() {
		return DebugModes.ON_ERROR.getName();
	}

	static boolean isSelfHealing() {
		return true;
	}

	static String getServiceFileName() {
		return "myService.groovy";
	}

	static int getTimeoutMinutes() {
		return TIMEOUT_MINUTES_EXAMPLE;
	}

	static String getUploadKey() {
		return UUID.randomUUID().toString();
	}

	static String getDeploymentID() {
		return UUID.randomUUID().toString();
	}

	static List<ServiceInstanceMetricsData> getServiceInstanceMetricsDataList() {
		final List<ServiceInstanceMetricsData> list = new LinkedList<ServiceInstanceMetricsData>();
		list.add(getServiceInstanceMetricsData());
		list.add(getServiceInstanceMetricsData());
		return list;
	}

	static List<String> getInstanceNames() {
		final List<String> instanceNames = new ArrayList<String>();
		instanceNames.add(getInstanceName(null, null));
		instanceNames.add(getInstanceName(null, null));
		instanceNames.add(getInstanceName(null, null));
		return instanceNames;
	}

	static int getNumberOfInstances() {
		return getInstanceId() + 1;
	}

	static List<DeploymentEvent> getEvents() {
		final List<DeploymentEvent> events = new LinkedList<DeploymentEvent>();

		final DeploymentEvent event1 = new DeploymentEvent();
		event1.setDescription("[127.0.0.1/127.0.0.1] - tomcat-1 INIT invoked");
		event1.setIndex(0);
		final DeploymentEvent event2 = new DeploymentEvent();
		event2.setDescription("[127.0.0.1/127.0.0.1] - tomcat-1 INIT completed, duration: 4.2 seconds");
		event2.setIndex(1);

		events.add(event1);
		events.add(event2);

		return events;
	}

	static DeploymentState getApplicationState() {
		return DeploymentState.STARTED;
	}

	static List<ServiceDescription> getServicesDescription() {
		final List<ServiceDescription> list = new LinkedList<ServiceDescription>();
		final int id1 = getInstanceId();
		list.add(getServiceDescription("service" + id1, id1));
		final int id2 = id1 + 1;
		list.add(getServiceDescription("service" + id2, id2));
		return list;
	}

	static String getAuthGroups() {
		return getAuthGroup() + "1," + getAuthGroup() + "2," + getAuthGroup() + "3";
	}

	static String getInstanceName(final String serviceName, final String appName) {
		final String effServiceName = serviceName != null ? serviceName : getServiceName();
		final String effAppName = appName != null ? appName : getAppName();
		return effAppName + "." + effServiceName;
	}

	static int getInstanceId() {
		return getRandomInt(MAX_INSTANCE_ID);
	}

	static String getHardwareId() {
		return "localcloud";
	}

	static String getImageId() {
		return "localcloud";
	}

	static String getMachineId() {
		return "localcloud";
	}

	static String getPrivateIp() {
		return getLocalHost().getHostAddress();
	}

	static Map<String, Object> getProcessDetails(final int instanceId) {
		final Map<String, Object> map = new HashMap<String, Object>();
			map.put("icon", "icon.png");
			map.put("url", null);
			map.put("Cloud Public IP", getPublicIp());
			map.put("Cloud Image ID", getImageId());
			map.put("Cloud Private IP", getPrivateIp());
			map.put("GSC PID", "4276");
			map.put("Cloud Hardware ID", getHardwareId()); 
			map.put("Instance ID", instanceId); 
			map.put("Machine ID", getMachineId());
			map.put("Working Directory", 
					"D:\\gigaSpaces\\gigaspaces-cloudify\\work\\processing-units\\simpleApp_simple_" 
							+ instanceId + "_1051025036\\ext");

		
		return map;
	}

	static String getPublicIp() {
		return getLocalHost().getHostAddress();
	}

	static String getTemplateName() {
		return "SMALL_LINUX";
	}

	private static int getRandomInt(final int max) {
		return RandomUtils.nextInt(max);
	}

	private static ServiceDescription getServiceDescription(final String serviceName, final int id) {
		final ServiceDescription serviceDescription = new ServiceDescription();

		final int numberOfInstances = getNumberOfInstances();
		final List<InstanceDescription> instancesDescriptionList = new LinkedList<InstanceDescription>();
		instancesDescriptionList.add(getInstanceDescription(serviceName, id));
		instancesDescriptionList.add(getInstanceDescription(serviceName, id + 2));

		serviceDescription.setApplicationName(getAppName());
		serviceDescription.setInstanceCount(numberOfInstances);
		serviceDescription.setPlannedInstances(numberOfInstances);
		serviceDescription.setServiceName(serviceName);
		serviceDescription.setInstancesDescription(instancesDescriptionList);
		serviceDescription.setServiceState(DeploymentState.STARTED);

		return serviceDescription;
	}

	private static InstanceDescription getInstanceDescription(final String serviceName, final int id) {
		final InstanceDescription instanceDescription = new InstanceDescription();

		final InetAddress localHost = getLocalHost();
		String hostAddress;
		String hostName;
		if (localHost == null) {
			hostAddress = "localhost";
			hostName = "localhost";
		} else {
			hostAddress = localHost.getHostAddress();
			hostName = localHost.getHostName();
		}

		instanceDescription.setHostAddress(hostAddress);
		instanceDescription.setHostName(hostName);
		instanceDescription.setInstanceId(id);
		instanceDescription.setInstanceName(getInstanceName(serviceName, null));
		instanceDescription.setInstanceStatus("RUNNING");

		return instanceDescription;
	}

	private static InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		} catch (final UnknownHostException e) {
			return null;
		}
	}

}
