package org.cloudifysource.rest.validators;

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

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.rest.request.SetServiceInstancesRequest;
import org.openspaces.admin.Admin;
import org.openspaces.admin.pu.ProcessingUnit;

/**
 * A set-instances validation context containing all necessary validation parameters required for validation.
 * 
 * @author barakm
 * @since 2.6.0
 * 
 */
public class SetServiceInstancesValidationContext {

	private Cloud cloud;
	private Admin admin;
	private String applicationName;
	private String serviceName;
	private SetServiceInstancesRequest request;
	private ProcessingUnit processingUnit;

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	public SetServiceInstancesRequest getRequest() {
		return request;
	}

	public void setRequest(final SetServiceInstancesRequest request) {
		this.request = request;
	}

	public ProcessingUnit getProcessingUnit() {
		return processingUnit;
	}

	public void setProcessingUnit(final ProcessingUnit processingUnit) {
		this.processingUnit = processingUnit;
	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

}
