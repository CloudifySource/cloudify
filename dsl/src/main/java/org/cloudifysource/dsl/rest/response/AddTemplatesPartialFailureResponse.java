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

import java.util.List;
import java.util.Map;

/**
 * A POJO represent add templates response used for making a REST API response .
 * 
 * @author yael
 * @since 2.7.0
 */
public class AddTemplatesPartialFailureResponse {
	private Map<String, AddTemplatesToPUResponse> partialFailedToAddTempaltesHosts;
	private Map<String, Map<String, String>> failedToAddAllTempaltesHosts;
	private List<String> successfullyAddedAllTempaltesHosts;

	public Map<String, AddTemplatesToPUResponse> getPartialFailedToAddTempaltesHosts() {
		return partialFailedToAddTempaltesHosts;
	}

	public void setPartialFailedToAddTempaltesHosts(
			final Map<String, AddTemplatesToPUResponse> partialFailedToAddTempaltesHosts) {
		this.partialFailedToAddTempaltesHosts = partialFailedToAddTempaltesHosts;
	}

	public Map<String, Map<String, String>> getFailedToAddAllTempaltesHosts() {
		return failedToAddAllTempaltesHosts;
	}

	public void setFailedToAddAllTempaltesHosts(final Map<String, Map<String, String>> failedToAddAllTempaltesHosts) {
		this.failedToAddAllTempaltesHosts = failedToAddAllTempaltesHosts;
	}

	public List<String> getSuccessfullyAddedAllTempaltesHosts() {
		return successfullyAddedAllTempaltesHosts;
	}

	public void setSuccessfullyAddedAllTempaltesHosts(final List<String> successfullyAddedAllTempaltesHosts) {
		this.successfullyAddedAllTempaltesHosts = successfullyAddedAllTempaltesHosts;
	}

}
