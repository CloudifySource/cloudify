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
package org.cloudifysource.rest;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.packaging.CloudConfigurationHolder;

/**
 * 
 * @author yael
 * 
 */
public class RestConfiguration {

	private Cloud cloud = null;
	private CloudConfigurationHolder cloudConfigurationHolder;
	private File cloudConfigurationDir;
	private String defaultTemplateName;
	private ComputeTemplate managementTemplate;
	private String managementTemplateName;
	private final AtomicInteger lastTemplateFileNum = new AtomicInteger(0);

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public CloudConfigurationHolder getCloudConfigurationHolder() {
		return cloudConfigurationHolder;
	}

	public void setCloudConfigurationHolder(final CloudConfigurationHolder cloudConfigurationHolder) {
		this.cloudConfigurationHolder = cloudConfigurationHolder;
	}

	public File getCloudConfigurationDir() {
		return cloudConfigurationDir;
	}

	public void setCloudConfigurationDir(final File cloudConfigurationDir) {
		this.cloudConfigurationDir = cloudConfigurationDir;
	}

	public String getDefaultTemplateName() {
		return defaultTemplateName;
	}

	public void setDefaultTemplateName(final String defaultTemplateName) {
		this.defaultTemplateName = defaultTemplateName;
	}

	public ComputeTemplate getManagementTemplate() {
		return managementTemplate;
	}

	public void setManagementTemplate(final ComputeTemplate managementTemplate) {
		this.managementTemplate = managementTemplate;
	}

	public String getManagementTemplateName() {
		return managementTemplateName;
	}
	
	public void setManagementTemplateName(String managementTemplateName) {
		this.managementTemplateName = managementTemplateName;
	}

	public AtomicInteger getLastTemplateFileNum() {
		return lastTemplateFileNum;
	}

}
