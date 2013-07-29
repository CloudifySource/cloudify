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
public class AddTemplatesResponse {
	private Map<String, Map<String, String>> failedToAddTempaltes;
	private List<String> successfullyAddedTempaltes;

	public Map<String, Map<String, String>> getFailedToAddTempaltes() {
		return failedToAddTempaltes;
	}

	public void setFailedToAddTempaltes(final Map<String, Map<String, String>> failedToAddTempaltes) {
		this.failedToAddTempaltes = failedToAddTempaltes;
	}

	public List<String> getSuccessfullyAddedTempaltes() {
		return successfullyAddedTempaltes;
	}

	public void setSuccessfullyAddedTempaltes(final List<String> successfullyAddedTempaltes) {
		this.successfullyAddedTempaltes = successfullyAddedTempaltes;
	}

}
