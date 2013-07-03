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
package org.cloudifysource.shell.validators;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.shell.exceptions.CLIValidationException;

/**
 * This class validates a connection to the lookup service can be established on the specified port.
 * @author noak
 * @since 2.7.0
 */
public class HostNameValidator implements CloudifyMachineValidator {

	@Override
	public void validate() throws CLIValidationException {
		try {
			final String hostName = InetAddress.getLocalHost().getHostName();
			InetAddress.getLocalHost().getHostAddress();
			InetAddress.getByName(hostName);
		} catch (UnknownHostException uhe) {
			throw new CLIValidationException(uhe, CloudifyErrorMessages.HOST_VALIDATION_ABORTED_UNKNOWN_HOST.getName(),
					uhe.getMessage());
		} catch (SecurityException se) {
			// thrown if a security manager exists and permission to resolve the host name is denied.
			throw new CLIValidationException(se, CloudifyErrorMessages.HOST_VALIDATION_ABORTED_NO_PERMISSION.getName(), 
					se.getMessage());
		}
	}

}
