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
 *******************************************************************************/

package org.cloudifysource.dsl.context;

import java.util.concurrent.TimeUnit;

/***************
 * Represents a Cloudify service, accessible from the Service Context.
 * @author barakme
 * @since 1.0.0
 *
 */
public interface Service {

	/**
	 * Returns the service name.
	 * 
	 * @return service name
	 */
	String getName();

	/**
	 * Returns the number of planned instances for this service.
	 * 
	 * @return number of planned instances
	 */
	int getNumberOfPlannedInstances();

	/**
	 * Returns the number of actually running instances for this service.
	 * 
	 * @return number of actually running instances
	 */
	int getNumberOfActualInstances();

	/*************
	 * Waits for the specified number of instances to become available.
	 * 
	 * @param howmany
	 *            number of instances to wait for.
	 * @param timeout
	 *            time to wait.
	 * @param timeUnit
	 *            time unit to wait.
	 * @return the available instances, or null if the requested number of instances was not found. If found, the
	 *         returned number of instances may be larger then the requested amount.
	 */
	ServiceInstance[] waitForInstances(final int howmany, final long timeout, final TimeUnit timeUnit);

	/**
	 * Returns the instances of this service.
	 * 
	 * @return array of service instances
	 */
	ServiceInstance[] getInstances();

	/******************
	 * Invokes a custom command on this service.
	 * 
	 * @param commandName
	 *            the command name.
	 * @param params
	 *            the command parameters.
	 * @return The invocation results.
	 * @throws Exception
	 *             if any of the invocations failed. The thrown exception is the exception thrown by the failed
	 *             invocation.
	 */
	Object[] invoke(final String commandName, final Object[] params)
			throws Exception;

}