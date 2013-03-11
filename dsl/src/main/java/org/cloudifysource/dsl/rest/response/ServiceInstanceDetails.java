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
 *******************************************************************************/
package org.cloudifysource.dsl.rest.response;

import java.util.Map;

/**
 * A POJO represent service instance details.
 * 
 * 
 * @author ahmad
 * 
 */
public class ServiceInstanceDetails {

	private int instanceId;
	private String applicationName;
	private String serviceName;
	private String serviceInstanceName;
	private String privateIp;
	private String publicIp;
	private String templateName;
	private String imageId;
	private String hardwareId;
	private String machineId;
	private Map<String, Object> processDetails;

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public Map<String, Object> getProcessDetails() {
		return processDetails;
	}

	public void setProcessDetails(final Map<String, Object> processDetails) {
		this.processDetails = processDetails;
	}

	public int getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(final int instanceId) {
		this.instanceId = instanceId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceInstanceName() {
		return serviceInstanceName;
	}

	public void setServiceInstanceName(final String serviceInstanceName) {
		this.serviceInstanceName = serviceInstanceName;
	}

	public String getPrivateIp() {
		return privateIp;
	}

	public void setPrivateIp(final String privateIp) {
		this.privateIp = privateIp;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(final String publicIp) {
		this.publicIp = publicIp;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(final String templateName) {
		this.templateName = templateName;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(final String imageId) {
		this.imageId = imageId;
	}

	public String getHardwareId() {
		return hardwareId;
	}

	public void setHardwareId(final String hardwareId) {
		this.hardwareId = hardwareId;
	}

	public String getMachineId() {
		return machineId;
	}

	public void setMachineId(final String machineId) {
		this.machineId = machineId;
	}

}
