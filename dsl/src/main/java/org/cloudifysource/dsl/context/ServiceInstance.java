/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.dsl.context;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/*********************
 *
 * @author barakme
 *
 */
public interface ServiceInstance {

	/***************
	 * Returns the instance id. Note that instance IDs are 1-based. When not running in a GSC, defaults to 1.
	 * Typo in this method name, so it is deprecated and will be deleted. use getInstanceId() instead.
	 *
	 * @return the instance id.
	 */
	@Deprecated
	int getInstanceID();

	/***************
	 * Returns the instance id. Note that instance IDs are 1-based. When not running in a GSC, defaults to 1.
	 *
	 * @return the instance id.
	 */
	int getInstanceId();

	/*************
	 * Returns the host address. When not running in a GSC, default to the local host address.
	 *
	 * @return the host address.
	 */
	String getHostAddress();

	/*************
	 * Returns the host name. When not running in a GSC, default to the local host name.
	 *
	 * @return the host name.
	 */
	String getHostName();

	/**********
	 * Returns the details for the given key by iterating over all service details in this processing unit instances and
	 * returning the first match.
	 *
	 * @param serviceDetailsKey
	 *            the details key.
	 * @return the details result, which may be null if the key does not exist in the service details.
	 */
	Object getDetails(final String serviceDetailsKey);

	/**********
	 * Returns the monitor for the given key by iterating over all service monitor in this processing unit instances and
	 * returning the first match.
	 *
	 * @param serviceMonitorsKey
	 *            the details key.
	 * @return the monitor result, which may be null if the key does not exist in the service details.
	 */
	Object getMonitors(final String serviceMonitorsKey);

	/*******************
	 * Invokes a custom command on this service instance, returning when the invocation finishes executing.
	 *
	 * @param commandName
	 *            the command name.
	 * @param params
	 *            the command parameters, may be zero-length.
	 * @return The invocation result.
	 * @throws InterruptedException .
	 * @throws ExecutionException .
	 * @throws TimeoutException .
	 */
	Object invoke(final String commandName, final Object[] params)
			throws InterruptedException, ExecutionException, TimeoutException;

}