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
package org.cloudifysource.dsl.rest.response;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The status of one template.
 * @author yael
 *
 */
public class AddTemplateResponse {
	private Map<String, String> failedToAddHosts;
	private List<String> successfullyAddedHosts;

	public AddTemplateResponse() {
		failedToAddHosts = new HashMap<String, String>();
		successfullyAddedHosts = new LinkedList<String>();
	}
	
	public Map<String, String> getFailedToAddHosts() {
		return failedToAddHosts;
	}

	public void setFailedToAddHosts(final Map<String, String> failedToAddHosts) {
		this.failedToAddHosts = failedToAddHosts;
	}

	public List<String> getSuccessfullyAddedHosts() {
		return successfullyAddedHosts;
	}

	public void setSuccessfullyAddedHosts(final List<String> successfullyAddedHosts) {
		this.successfullyAddedHosts = successfullyAddedHosts;
	}
}
