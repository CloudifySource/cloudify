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
package org.cloudifysource.dsl.rest;

import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.dsl.internal.CloudifyConstants.DeploymentState;
/**
 * Service description POJO.
 * This class contains deployment information regarding the service and more specifically  
 * information regarding each of it's service instances.
 * 
 * @author adaml
 * @since 2.3.0
 *
 */
public class ServiceDescription {
	
	private String serviceName;
	private int instanceCount;
	private int plannedInstances;
	private List<InstanceDescription> instancesDescription = new ArrayList<InstanceDescription>();
	private String applicationName;
	private DeploymentState serviceState;
	
	
	public ServiceDescription() {
		
	}


	public int getInstanceCount() {
		return instanceCount;
	}


	public void setInstanceCount(int instanceCount) {
		this.instanceCount = instanceCount;
	}


	public String getServiceName() {
		return serviceName;
	}


	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}


	public int getPlannedInstances() {
		return plannedInstances;
	}


	public void setPlannedInstances(int plannedInstances) {
		this.plannedInstances = plannedInstances;
	}
	
	
	public void setInstancesDescription(final List<InstanceDescription> instancesStatus) {
		this.instancesDescription = instancesStatus;
	}
	
	
	public List<InstanceDescription> getInstancesDescription() {
		return this.instancesDescription;
	}


	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
		
	}
	
	
	public String getApplicationName() {
		return this.applicationName;
	}


	public DeploymentState getServiceState() {
		return serviceState;
	}


	public void setServiceState(DeploymentState serviceState) {
		this.serviceState = serviceState;
	}
	
	

}
