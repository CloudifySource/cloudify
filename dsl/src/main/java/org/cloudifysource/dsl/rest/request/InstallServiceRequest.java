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


/**
 * Represents a request to install-service command via the REST Gateway.
 * It holds all the needed parameters to install the service.
 * @author yael
 *
 */
public class InstallServiceRequest {
	
	
	private String uploadKey;
	private String cloudConfigurationUploadKey;
	private String authGroups;
	private Boolean selfHealing = true;
	private String cloudOverrides;
	private String serviceOverrides;
	
	public String getUploadKey() {
		return uploadKey;
	}
	public void setUploadKey(String uploadKey) {
		this.uploadKey = uploadKey;
	}
	public String getCloudConfigurationUploadKey() {
		return cloudConfigurationUploadKey;
	}
	public void setCloudConfigurationUploadKey(String cloudConfigurationUploadKey) {
		this.cloudConfigurationUploadKey = cloudConfigurationUploadKey;
	}
	public String getAuthGroups() {
		return authGroups;
	}
	public void setAuthGroups(String authGroups) {
		this.authGroups = authGroups;
	}
	public Boolean getSelfHealing() {
		return selfHealing;
	}
	public void setSelfHealing(Boolean selfHealing) {
		this.selfHealing = selfHealing;
	}
	public String getCloudOverrides() {
		return cloudOverrides;
	}
	public void setCloudOverrides(String cloudOverrides) {
		this.cloudOverrides = cloudOverrides;
	}
	public String getServiceOverrides() {
		return serviceOverrides;
	}
	public void setServiceOverrides(String serviceOverrides) {
		this.serviceOverrides = serviceOverrides;
	}	
}
