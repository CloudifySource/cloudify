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

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.shell.exceptions.CLIValidationException;

/**
 * This class validates a connection to the lookup service can be established on the specified port.
 * @author noak
 * @since 2.7.0
 */
public class LusConnectionValidator implements CloudifyAgentValidator {
	
	private String lusIpAddress;
	
	// TODO noa run only on agent
	// use lookup locators
	
	
	/**
	 * Setter for lusIpAddress.
	 * @param lusIpAddress The LUS IP address to validate, or null to use the env var setting.
	 */
	public void setLusIpAddress(final String lusIpAddress) {
		this.lusIpAddress = lusIpAddress;
	}

	/**
	 * Validates a connection can be established to the lookup service on the specified port.
	 * @throws CLIValidationException Indicates a failure to establish a connection.
	 */
	@Override
	public void validate() throws CLIValidationException {

		// get lus IP address from the environment variable if not already set
		if (StringUtils.isBlank(lusIpAddress)) {
			lusIpAddress = System.getenv(CloudifyConstants.LUS_IP_ADDRESS_ENV);
		}

		// TODO noak : throw CLIValidationException with custom exit code (130) instead?
		if (StringUtils.isBlank(lusIpAddress)) {
			throw new IllegalArgumentException("LUS IP address not configred. The environment variable \"" 
					+ CloudifyConstants.LUS_IP_ADDRESS_ENV + "\" is not set.");
		}

		//parse the ip address and port
		String hostAddress = IPUtils.getHostFromFullAddress(lusIpAddress);
		int port = IPUtils.getPortFromFullAddress(lusIpAddress);		

		try {
			IPUtils.validateConnection(hostAddress, port);
		} catch (UnknownHostException uhe) {
			// thrown if the IP address of the host could not be determined.
			throw new CLIValidationException(uhe, 127,
					CloudifyErrorMessages.LUS_CONNECTION_VALIDATION_ABORTED_UNKNOWN_HOST.getName(), hostAddress);
		} catch (IOException ioe) {
			// thrown if an I/O error occurs when creating the socket or connecting.
			throw new CLIValidationException(ioe, 128,
					CloudifyErrorMessages.LUS_CONNECTION_VALIDATION_ABORTED_IO_ERROR.getName(), hostAddress, port,
					ioe.getMessage());
		} catch (SecurityException se) {
			// thrown if a security manager exists and permission to resolve the host name is denied.
			throw new CLIValidationException(se,  129,
					CloudifyErrorMessages.LUS_CONNECTION_VALIDATION_ABORTED_NO_PERMISSION.getName(), hostAddress, port,
					se.getMessage());
		}
		
	}

}
