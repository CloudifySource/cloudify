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
package org.cloudifysource.rest.validators;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;

/**
 * A POJO for holding install-service validator's parameters.
 * 
 * @author yael
 * 
 */
public class InstallServiceValidationContext {
	private String absolutePuName;
	private String templateName;
	private Cloud cloud;
	private Service service;
	private InstallServiceRequest request;

	public String getAbsolutePuName() {
		return absolutePuName;
	}
	
	public void setAbsolutePuName(final String absolutePuName) {
		this.absolutePuName = absolutePuName;
	}
	public String getTemplateName() {
		return templateName;
	}
	
	public void setTemplateName(final String templateName) {
		this.templateName = templateName;
	}
	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public Service getService() {
		return service;
	}

	public void setService(final Service service) {
		this.service = service;
	}

	public InstallServiceRequest getRequest() {
		return request;
	}

	public void setRequest(final InstallServiceRequest request) {
		this.request = request;
	}

}
