/*******************************************************************************
 * Copyright (c) 2014 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.shell.rest;

import org.cloudifysource.dsl.internal.CloudifyConstants;

import com.j_spaces.kernel.PlatformVersion;

/************
 * Logic for resolving the API version to use with the rest client.
 * 
 * @author barakme
 * @since 2.7.1
 * 
 */
public class APIVersionResolver {

	/*********
	 * Resolves the API version to be used.
	 * @return the REST API version.
	 */
	public String resolveAPIVersion() {
		final String sysprop = System.getProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_REST_API_VERSION);
		if (sysprop != null) {
			return sysprop;
		}

		final String platformVersion = PlatformVersion.getVersion();
		return platformVersion;
	}
	
	/*********
	 * Resolves the API version to be used with the old (Pre-2.7) API.
	 * @return the REST API version.
	 */
	public String resolveOldAPIVersion() {
		final String sysprop = System.getProperty(CloudifyConstants.SYSTEM_PROPERTY_CLI_OLD_REST_API_VERSION);
		if (sysprop != null) {
			return sysprop;
		}

		final String platformVersion = PlatformVersion.getVersionNumber();
		return platformVersion;
	}

}
