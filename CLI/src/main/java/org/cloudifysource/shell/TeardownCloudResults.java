/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
 ******************************************************************************/
package org.cloudifysource.shell;

/**
 * @author noak
 * @since 2.0.0
 * 
 *        This enumeration represents the cloud's tear-down result.
 * 
 */
public enum TeardownCloudResults {
	/**
	 * The tear-down completed successfully. An agent was shut down within the specified time frame.
	 */
	COMPLETED_SUCCESSFULLY("Completed successfully"),
	/**
	 * Tear-down was not performed, an agent was not found on the local machine.
	 */
	AGENT_NOT_FOUND_ON_LOCAL_MACHINE("Operation aborted, agent not running on local machine");

	private String description;

	TeardownCloudResults(final String description) {
		this.description = description;
	}

	/**
	 * Gets the description of this tear-down result.
	 * @return Result description
	 */
	public String getDescription() {
		return description;
	}
}
