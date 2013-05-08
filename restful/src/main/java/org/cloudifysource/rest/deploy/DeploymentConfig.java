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
package org.cloudifysource.rest.deploy;

import java.io.File;
import java.util.Properties;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.ServiceProcessingUnit;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;

/**
 * required deployment configuration.
 * 
 * @author adaml
 *
 */
public class DeploymentConfig {
	
	private InstallServiceRequest installRequest; 
	
	private String absolutePUName;
	
	private String applicationName;
	
	private String templateName;
	
	private Cloud cloud;
	
	private String authGroups;
	
	private Properties contextProperties;
	
	private Service service;
	
	private String locators;
	
	private File packedFile;
	
	private byte[] cloudConfig;

	private String cloudOverrides;
	
	private String deploymentId;
	
	public InstallServiceRequest getInstallRequest() {
		return installRequest;
	}

	public void setInstallRequest(final InstallServiceRequest installRequest) {
		this.installRequest = installRequest;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(final String templateName) {
		this.templateName = templateName;
	}

	public String getAbsolutePUName() {
		return absolutePUName;
	}

	public void setAbsolutePUName(final String absolutePUName) {
		this.absolutePUName = absolutePUName;
	}

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public String getAuthGroups() {
		return authGroups;
	}

	public void setAuthGroups(final String authGroups) {
		this.authGroups = authGroups;
	}

	public Properties getContextProperties() {
		return contextProperties;
	}

	public void setContextProperties(final Properties contextProperties) {
		this.contextProperties = contextProperties;
	}

	public Service getService() {
		return service;
	}

	public void setService(final Service service) {
		this.service = service;
	}

	public String getLocators() {
		return locators;
	}

	public void setLocators(final String locators) {
		this.locators = locators;
	}

	public File getPackedFile() {
		return packedFile;
	}

	public void setPackedFile(final File packedFile) {
		this.packedFile = packedFile;
	}

	public byte[] getCloudConfig() {
		return cloudConfig;
	}

	public void setCloudConfig(final byte[] cloudConfig) {
		this.cloudConfig = cloudConfig;
	}

	public String getCloudOverrides() {
		return this.cloudOverrides;
	}

	public void setCloudOverrides(final String cloudOverrides) {
		this.cloudOverrides = cloudOverrides;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(final String deploymentId) {
		this.deploymentId = deploymentId;
	}
}
