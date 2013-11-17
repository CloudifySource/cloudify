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
package org.cloudifysource.dsl.rest.request;

import java.util.List;

/**
 * 
 * A POJO representing a command and its parameters, used for invoking a custom 
 * command through the new REST API.
 * 
 * @author noak
 * @since 2.7.0
 */
public class InvokeCustomCommandRequest {

	private String commandName;
	private List<String> parameters;

	public String getCommandName() {
		return commandName;
	}
	public void setCommandName(final String commandName) {
		this.commandName = commandName;
	}
	public List<String> getParameters() {
		return parameters;
	}
	public void setParameters(final List<String> parameters) {
		this.parameters = parameters;
	}
	
	
}
