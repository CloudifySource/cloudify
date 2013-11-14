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

/**
 * A dedicated exception for network provisioning errors.
 * 
 * @author adaml
 * @since 2.7.0
 *
 */
public class NetworkProvisioningException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NetworkProvisioningException(final String message) {
		super(message);
	}
	
	public NetworkProvisioningException(final Exception e) {
		super(e);
	}
	
	public NetworkProvisioningException(final String message, final Exception e) {
		super(message, e);
	}
	
	public NetworkProvisioningException() {
		super();
	}
}

	
