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
package org.cloudifysource.esc.driver.provisioning.network;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 * @author adaml
 * @since 2.7.0
 *
 */
public abstract class BaseNetworkDriver {

	protected NetworkDriverConfiguration networkConfig;

	/**
	 * Called once at initialization. Sets configuration members for later use. use this method to extract all necessary
	 * information needed for future provisioning calls.
	 * 
	 * @param config 
	 * 			The network driver configuration.
	 */
	public void setConfig(final NetworkDriverConfiguration config) {
		this.networkConfig = config;
	}
	
	/********
	 * Assigns a floating IP address to the dedicated instance.
	 * @param instanceIPAddress
	 * 			The dedicated instance IP address.
	 * @param floatingIP
	 * 			The floating IP address.
	 * @param context
	 * 			context properties.
	 * @param duration 
	 * 			duration until times out.
	 * @param timeUnit
	 *            the duration timeout units.
	 * @throws NetworkProvisioningException
	 * 			If assign action fails.
	 * @throws TimeoutException
	 * 			If execution exceeds duration.
	 */
	public void assignFloatingIP(final String instanceIPAddress, final String floatingIP,
			final Map<String, Object> context, final long duration, final TimeUnit timeUnit) 
					throws NetworkProvisioningException, TimeoutException {
		unsupported();
	}
	
	/********
	 * Unassigns the floating IP address.
	 * @param instanceIPAddress
	 * 			The instance IP address.
	 * @param floatingIP
	 * 			The floating IP address.
	 * @param context
	 * 			context properties.
	 * @param duration 
	 * 			duration until times out.
	 * @param timeUnit
	 *            the duration timeout units.
	 * @throws NetworkProvisioningException
	 * 			If unassign action fails.
	 * @throws TimeoutException 
	 * 			If execution exceeds duration.
	 */
	public void unassignFloatingIP(final String instanceIPAddress, final String floatingIP,
			final Map<String, Object> context, final long duration, final TimeUnit timeUnit) 
					throws NetworkProvisioningException, TimeoutException {
		unsupported();
	}
	
	/********
	 * Reserves a floating IP address from the available blocks of floating IP addresses.
	 * @param poolName
	 * 			The pool name from which to allocate the floating IP address. 
	 * @param context
	 * 			context properties.
	 * @param duration 
	 * 			duration until times out.
	 * @param timeUnit
	 *            the duration timeout units.
	 * @return
	 * 		The reserved IP address.
	 * @throws NetworkProvisioningException
	 * 			If allocation fails.
	 * @throws TimeoutException
	 * 			If execution exceeds duration.
	 */
	public String allocateFloatingIP(final String poolName, final Map<String, Object> context,
			final long duration, final TimeUnit timeUnit)
			throws NetworkProvisioningException, TimeoutException {
		unsupported();
		return null;
	}
	
	/********
	 * Releases the floating IP address back to the floating IP address pool. 
	 * @param ip
	 * 			The floating IP address to release.
	 * @param context
	 * 			context properties.
	 * @param duration 
	 * 			duration until times out.
	 * @param timeUnit
	 *            the duration timeout units.
	 * @throws NetworkProvisioningException 
	 * 			If release action fails.
	 * @throws TimeoutException
	 * 			If execution exceeds duration.
	 * 
	 */
	public void releaseFloatingIP(final String ip, final Map<String, Object> context,
			final long duration, final TimeUnit timeUnit)
			throws NetworkProvisioningException, TimeoutException {
		unsupported();
	}
	
	private void unsupported() {
		throw new UnsupportedOperationException("Method not implemented");
	}
}
