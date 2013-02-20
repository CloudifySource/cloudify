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
 * Application description POJO. This class contains deployment information
 * regarding the application and all its service and more specifically
 * information regarding each of it's service instances.
 * 
 * @author adaml
 * @since 2.3.0
 * 
 */
public class ApplicationDescription {

	private List<ServiceDescription> servicesDescription = new ArrayList<ServiceDescription>();
	private String applicationName;
	private String authGroups;
	private DeploymentState applicationState;

	public List<ServiceDescription> getServicesDescription() {
		return servicesDescription;
	}

	public void setServicesDescription(final List<ServiceDescription> servicesDescription) {
		this.servicesDescription = servicesDescription;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public String getAuthGroups() {
		return authGroups;
	}

	public void setAuthGroups(final String authGroups) {
		this.authGroups = authGroups;
	}

	public void setApplicationState(final DeploymentState applicationState) {
		this.applicationState = applicationState;

	}

	public DeploymentState getApplicationState() {
		return this.applicationState;
	}

}
