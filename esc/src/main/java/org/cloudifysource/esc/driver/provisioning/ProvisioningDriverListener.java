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
package org.cloudifysource.esc.driver.provisioning;

/**
 * A Listener for all published events in the DefaultProvisioningDriver class.
 * 
 * Registering a listener to a cloud driver allows an application that uses the cloud driver (like the Cloudify CLI) to
 * receive notifications about provisioning events. This is usually used for things like progress bars and on-screen
 * notifications, though you are of-course free to use as you wish.
 * 
 * A listener MUST NOT throw an exception - if it throws a runtime exception, the current cloud driver provisioning
 * action will stop. A listener MUST NOT perform long running operations - the listener implementation is called on the
 * same thread as the provisioning action and a long running operation may delay time sensitive cloud driver actions.
 * 
 * @author adaml
 * @since 2.0
 * 
 */
public interface ProvisioningDriverListener {

	/****************
	 * Callback method for a provisioning event.
	 * 
	 * @param eventName Event name.
	 * @param args Event arguments.
	 */
	void onProvisioningEvent(String eventName, Object... args);

}
