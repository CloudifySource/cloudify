/*
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
 * *****************************************************************************
 */
package org.cloudifysource.rest.deploy;

import java.io.File;
import java.util.List;

import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.rest.controllers.DeploymentsController;

/**
 * 
 * @author yael
 * 
 */
public class ApplicationDeployerRequest {

	private DeploymentsController controller;
	private InstallApplicationRequest request;
	private File appFile;
	private File appDir;
	private File appPropertiesFile;
	private File appOverridesFile;
	private String deploymentID;
	private List<Service> services;

	public final DeploymentsController getController() {
		return controller;
	}

	public final void setController(final DeploymentsController controller) {
		this.controller = controller;
	}

	public final InstallApplicationRequest getRequest() {
		return request;
	}

	public final void setRequest(final InstallApplicationRequest request) {
		this.request = request;
	}

	public final File getAppFile() {
		return appFile;
	}

	public final void setAppFile(final File appFile) {
		this.appFile = appFile;
	}

	public final File getAppDir() {
		return appDir;
	}

	public final void setAppDir(final File appDir) {
		this.appDir = appDir;
	}

	public final File getAppPropertiesFile() {
		return appPropertiesFile;
	}

	public final void setAppPropertiesFile(final File appPropertiesFile) {
		this.appPropertiesFile = appPropertiesFile;
	}
	
	public final File getAppOverridesFile() {
		return appOverridesFile;
	}

	public final void setAppOverridesFile(final File appOverridesFile) {
		this.appOverridesFile = appOverridesFile;
	}

	public final String getDeploymentID() {
		return deploymentID;
	}

	public final void setDeploymentID(final String deploymentID) {
		this.deploymentID = deploymentID;
	}

	public final List<Service> getServices() {
		return services;
	}

	public final void setServices(final List<Service> services) {
		this.services = services;
	}

}
