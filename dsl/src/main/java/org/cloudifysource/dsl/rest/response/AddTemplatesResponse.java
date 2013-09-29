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
package org.cloudifysource.dsl.rest.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A POJO represent add templates response used for making a REST API response .
 * 
 * @author yael
 * @since 2.7.0
 */
public class AddTemplatesResponse {

	/*
	 * A map that holds responses for each template. 
	 * Each response describes which instances failed to add the template
	 * and which succeeded.
	 */
	private Map<String, AddTemplateResponse> templates;
	private List<String> instances;
	private AddTemplatesStatus status; 

	public AddTemplatesResponse() {
		templates = new HashMap<String, AddTemplateResponse>();
	}
	
	public Map<String, AddTemplateResponse> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, AddTemplateResponse> templates) {
		this.templates = templates;
	}

	public List<String> getInstances() {
		return instances;
	}

	public void setInstances(final List<String> instances) {
		this.instances = instances;
	}

	public AddTemplatesStatus getStatus() {
		return status;
	}

	public void setStatus(final AddTemplatesStatus status) {
		this.status = status;
	}

}
