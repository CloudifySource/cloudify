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
import java.util.Map;

/**
 * A POJO representing the result of invoking a custom command on all (one or more) the service instances.
 * 
 * @author noak
 * @since 2.7.0
 */
public class InvokeServiceCommandResponse {

	private Map<String, Map<String, String>> invocationResultPerInstance = new HashMap<String, Map<String, String>>();
	
	/**
	 * Sets the result of the invoke command action for the specified service instance.
	 * @param serviceInstanceName the name of service instance
	 * @param result the result of the command invocation as Map<String, String>
	 */
	public void setInvocationResult(final String serviceInstanceName, final Map<String, String> result) {
		invocationResultPerInstance.put(serviceInstanceName, result);
	}
	
	public Map<String, Map<String, String>> getInvocationResultPerInstance() {
		return invocationResultPerInstance;
	}
}
