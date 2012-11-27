/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.internal;

/**********
 * Enum for cloudify error messages, including keys to message bundle.
 * @author barakme
 * 
 * @since 2.3.0
 */
public enum CloudifyErrorMessages {

	/********
	 * Indicates an unexpected error occured in the server.
	 */
	GENERAL_SERVER_ERROR("general_server_error", 1),
	/******
	 * If service recipe refers to missing template.
	 */
	MISSING_TEMPLATE("missing_template", 1),	
	
	/**
	 * If the cloud overrides given is to long. the file size limit is 10K.
	 */
	CLOUD_OVERRIDES_TO_LONG("cloud_overrides_file_to_long", 0),
	
	/*******
	 * Attempted to install service with name of already installed service.
	 */
	SERVICE_ALREADY_INSTALLED("service_already_installed", 1),
	
	/**
	 * Access to the resource is denied, permission not granted.
	 */
	NO_PERMISSION_ACCESS_DENIED("no_permission_access_is_denied", 0),
	

	/**
	 * Access to the resource is denied, permission not granted.
	 */
	BAD_CREDENTIALS("bad_credentials", 0),
	
	/**
	 * 
	 */
	MISSING_NODES_LIST("prov_missing_nodesList_configuration", 1);
	
	
	private final int numberOfParameters;
	private final String name;
	
	CloudifyErrorMessages(final String name, final int numberOfParameters) {
		this.name = name;
		this.numberOfParameters = numberOfParameters;
	}

	public int getNumberOfParameters() {
		return numberOfParameters;
	}

	public String getName() {
		return name;
	}
	
	
}
