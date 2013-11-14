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

import java.util.Map;

import org.cloudifysource.domain.cloud.compute.ComputeTemplate;

/**
 * A POJO representing a response to list-templates command via the REST Gateway.
 * It holds a map of the templates. 
 * 
 * @author yael
 * @since 2.7.0
 */
public class ListTemplatesResponse {
	private Map<String, ComputeTemplate> templates;

	public Map<String, ComputeTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, ComputeTemplate> templates) {
		this.templates = templates;
	}
}
