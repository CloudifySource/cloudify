/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.rest.request;

import org.cloudifysource.dsl.internal.debug.DebugModes;


/**
 * POJO representation of an installApplication command request via the REST gateway.
 * 
 * @author adaml
 *
 */
public class InstallApplicationRequest {
	
	private String applcationFileUploadKey;
	
	private String applicationOverridesUploadKey;
	
	private String cloudOverridesUploadKey;
	
	private String applicationName;

	private boolean isSelfHealing;
	
	private String authGroups;
	
	private boolean isDebugAll;
	
	private String debugMode = DebugModes.INSTEAD.getName();
	
	private String debugEvents;
	
	private String cloudConfigurationUploadKey;
	
	public String getApplcationFileUploadKey() {
		return applcationFileUploadKey;
	}

	public void setApplcationFileUploadKey(final String applcationFileUploadKey) {
		this.applcationFileUploadKey = applcationFileUploadKey;
	}

	public String getCloudOverridesUploadKey() {
		return cloudOverridesUploadKey;
	}

	public void setCloudOverridesUploadKey(final String cloudOverridesUploadKey) {
		this.cloudOverridesUploadKey = cloudOverridesUploadKey;
	}

	public String getApplicationOverridesUploadKey() {
		return applicationOverridesUploadKey;
	}

	public void setApplicationOverridesUploadKey(
			final String applicationOverridesUploadKey) {
		this.applicationOverridesUploadKey = applicationOverridesUploadKey;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public boolean isSelfHealing() {
		return isSelfHealing;
	}

	public void setSelfHealing(final boolean isSelfHealing) {
		this.isSelfHealing = isSelfHealing;
	}

	public String getAuthGroups() {
		return authGroups;
	}

	public void setAuthGroups(final String authGroups) {
		this.authGroups = authGroups;
	}

	public boolean isDebugAll() {
		return isDebugAll;
	}

	public void setDebugAll(final boolean isDebugAll) {
		this.isDebugAll = isDebugAll;
	}

	public String getDebugMode() {
		return debugMode;
	}

	public void setDebugMode(final String debugMode) {
		this.debugMode = debugMode;
	}

	public String getDebugEvents() {
		return debugEvents;
	}

	public void setDebugEvents(final String debugEvents) {
		this.debugEvents = debugEvents;
	}

	public String getCloudConfigurationUploadKey() {
		return cloudConfigurationUploadKey;
	}

	public void setCloudConfigurationUploadKey(
			final String cloudConfigurationUploadKey) {
		this.cloudConfigurationUploadKey = cloudConfigurationUploadKey;
	}
}
