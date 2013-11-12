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
package org.cloudifysource.domain.context.network;

/**
 * 
 * @author adaml
 * @since 2.7.0
 *
 */
public interface NetworkDriver {

	/********
	 * Assigns a floating IP address to the dedicated instance.
	 * @param ip
	 * 			The floating IP address.
	 * @param instanceID
	 * 			The dedicated instance ID.
	 */
	void assign(final String ip, final String instanceID);
	
	/********
	 * Unassigns the floating IP address.
	 * @param ip
	 * 			The floating IP address.
	 * @param instanceID
	 * 			The instance ID.
	 */
	void unassign(final String ip, final String instanceID);
	
	/********
	 * Reserves a floating IP address from the available blocks of floating IP addresses. 
	 * @return
	 * 		The reserved IP address.
	 */
	String create();
	
	/********
	 * Releases the floating IP address back to the floating IP address pool. 
	 * @param ip
	 * 			The floating IP address to release.
	 * 
	 */
	void release(final String ip);
}
