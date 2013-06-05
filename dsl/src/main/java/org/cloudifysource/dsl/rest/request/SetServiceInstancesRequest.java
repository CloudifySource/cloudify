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
package org.cloudifysource.dsl.rest.request;


/*********
 * Request POJO for the set-instances REST API. See
 * org.cloudifysource.rest.controllers.DeploymentsController.setServiceInstances(String, String,
 * SetServiceInstancesRequest)
 *
 * @author barakme
 * @since 2.6.0
 *
 */
public class SetServiceInstancesRequest {

	private static final int DEFAULT_TIMEOUT_MINUTES = 15;
	private int timeout = DEFAULT_TIMEOUT_MINUTES;
	private int count = -1;
	private boolean locationAware = false;

	/****
	 * Scaling timeout, in minutes.
	 * @return the timeout.
	 */
	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(final int timeout) {
		this.timeout = timeout;
	}

	/****
	 * The number of instances to scale to (can be more or less then the current number).
	 * @return the number of instances.
	 */
	public int getCount() {
		return count;
	}

	public void setCount(final int count) {
		this.count = count;
	}

	/********
	 * Whether or not this scaling operation is location aware.
	 * @return Whether or not this scaling operation is location aware.
	 */
	public boolean isLocationAware() {
		return locationAware;
	}

	public void setLocationAware(final boolean locationAware) {
		this.locationAware = locationAware;
	}

}
