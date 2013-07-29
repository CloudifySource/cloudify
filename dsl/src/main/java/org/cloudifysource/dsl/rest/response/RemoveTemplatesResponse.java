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

import java.util.List;
import java.util.Map;

/**
 * 
 * @author yael
 * @since 2.7.0
 */
public class RemoveTemplatesResponse {
	private List<String> successfullyRemovedFromHosts;
	private Map<String, String> failedToRemoveFromHosts;

	public List<String> getSuccessfullyRemovedFromHosts() {
		return successfullyRemovedFromHosts;
	}

	public void setSuccessfullyRemovedFromHosts(final List<String> successfullyRemovedFromHosts) {
		this.successfullyRemovedFromHosts = successfullyRemovedFromHosts;
	}

	public Map<String, String> getFailedToRemoveFromHosts() {
		return failedToRemoveFromHosts;
	}

	public void setFailedToRemoveFromHosts(final Map<String, String> failedToRemoveFromHosts) {
		this.failedToRemoveFromHosts = failedToRemoveFromHosts;
	}

}
