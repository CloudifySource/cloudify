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
import java.net.ServerSocket;
import java.net.UnknownHostException;

import net.jini.discovery.Constants;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.shell.exceptions.CLIValidationException;

/**
 * This class validates the a server socket can be created on the given NIC address can be established.
 * If no NIC address was specified, the local host address is used.
 * @author noak
 * @since 2.7.0
 */
public class NicAddressValidator implements CloudifyMachineValidator {
	
	private String nicAddress;
	
	/**
	 * Empty Ctor.
	 */
	public NicAddressValidator() {
	}
	
	/**
	 * Ctor.
	 * @param nicAddress The NIC address to validate, or null to use the local host.
	 */
	public NicAddressValidator(final String nicAddress) {
		this.nicAddress = nicAddress;
	}

	
	@Override
	public void validate() throws CLIValidationException {
		
		ServerSocket serverSocket = null;
		try {
			if (StringUtils.isBlank(nicAddress)) {
				nicAddress = Constants.getHostAddress();
			}
			IPUtils.validatePortIsFree(nicAddress, 0);
		} catch (UnknownHostException uhe) {
			// thrown if the IP address of the host could not be determined.
			throw new CLIValidationException(uhe, CloudifyErrorMessages.NIC_VALIDATION_ABORTED_UNKNOWN_HOST.getName(), 
					uhe.getMessage());
		} catch (IOException ioe) {
			// thrown if an I/O error occurs when creating the socket or connecting to it.
			throw new CLIValidationException(ioe, CloudifyErrorMessages.NIC_VALIDATION_ABORTED_IO_ERROR.getName(),
					ioe.getMessage());
		} catch (SecurityException se) {
			// thrown if a security manager exists and doesn't allow the operation.
			throw new CLIValidationException(se, CloudifyErrorMessages.NIC_VALIDATION_ABORTED_NO_PERMISSION.getName(),
					se.getMessage());
		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (Exception e) {
					//ignore
				}
			}
		}
		
	}

}
