package org.cloudifysource.domain;
/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

import org.cloudifysource.domain.cloud.compute.ComputeTemplate;

/**
 * 
 * @author yael
 *
 */
public class ComputeTemplateHolder {
	private String name;
	private ComputeTemplate cloudTemplate;
	private String templateFileName;
	private String propertiesFileName;
	private String overridesFileName;
	
	public ComputeTemplateHolder() {
		
	}
	
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public ComputeTemplate getCloudTemplate() {
		return cloudTemplate;
	}
	public void setCloudTemplate(final ComputeTemplate cloudTemplate) {
		this.cloudTemplate = cloudTemplate;
	}

	public String getTemplateFileName() {
		return templateFileName;
	}

	public void setTemplateFileName(final String templateFileName) {
		this.templateFileName = templateFileName;
	}

	public String getPropertiesFileName() {
		return propertiesFileName;
	}

	public void setPropertiesFileName(final String propertiesFileName) {
		this.propertiesFileName = propertiesFileName;
	}

	public String getOverridesFileName() {
		return overridesFileName;
	}

	public void setOverridesFileName(final String overridesFileName) {
		this.overridesFileName = overridesFileName;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		sb.append("name = " + this.name);
		if (overridesFileName != null) {
			sb.append(", overridesFileName = " + this.overridesFileName);
		}
		if (propertiesFileName != null) {
			sb.append(", propertiesFileName = " + this.propertiesFileName);
		}
		if (templateFileName != null) {
			sb.append(", templateFileName = " + this.templateFileName);
		}
		if (cloudTemplate != null) {
			sb.append(", cloudTemplate = " + this.cloudTemplate);
		}
		sb.append("]");
		return sb.toString();
	}
	
}
