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
package org.cloudifysource.rest.validators;

import java.io.File;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.debug.DebugModes;
import org.openspaces.admin.Admin;

/**
 * A POJO for holding install-service validator's parameters.
 * 
 * @author yael
 * 
 */
public class InstallServiceValidationContext {
	private Cloud cloud;
	private Admin admin;
	private Service service;
	private File cloudOverridesFile;
	private File serviceOverridesFile;
	private File cloudConfigurationFile;
	private boolean debugAll;
	private String debugEvents;
	private String debugMode = DebugModes.INSTEAD.getName();

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

	public Service getService() {
		return service;
	}

	public void setService(final Service service) {
		this.service = service;
	}

	public File getCloudOverridesFile() {
		return cloudOverridesFile;
	}

	public void setCloudOverridesFile(final File cloudOverridesFile) {
		this.cloudOverridesFile = cloudOverridesFile;
	}

	public File getServiceOverridesFile() {
		return serviceOverridesFile;
	}

	public void setServiceOverridesFile(final File serviceOverridesFile) {
		this.serviceOverridesFile = serviceOverridesFile;
	}

	public File getCloudConfigurationFile() {
		return cloudConfigurationFile;
	}

	public void setCloudConfigurationFile(final File cloudConfigurationFile) {
		this.cloudConfigurationFile = cloudConfigurationFile;
	}

	public boolean isDebugAll() {
		return debugAll;
	}

	public void setDebugAll(final boolean debugAll) {
		this.debugAll = debugAll;
	}

	public String getDebugEvents() {
		return debugEvents;
	}

	public void setDebugEvents(final String debugEvents) {
		this.debugEvents = debugEvents;
	}

	public String getDebugMode() {
		return debugMode;
	}

	public void setDebugMode(final String debugMode) {
		this.debugMode = debugMode;
	}

}
