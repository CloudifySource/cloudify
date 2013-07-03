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

import net.jini.discovery.Constants;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.shell.exceptions.CLIValidationException;

/**
 * This class validates the ports required for Cloudify services (esm, lus, gsm, gsa, gsc) are free.
 * @author noak
 * @since 2.7.0
 */
public class PortAvailabilityValidator implements CloudifyMachineValidator {
	
	private static final String PORT_RANGE_SEPARATOR = "-";
	
	private String lrmiPortRange;
	
	// TODO  noa - if agent - test gsa, gsc
	// if mgmt - esm, lus, gsm, gsa, gsc
	// test Iserver socket
	
	/**
	 * Empty Ctor.
	 */
	public PortAvailabilityValidator() {
	}
	
	/**
	 * Ctor.
	 * @param lrmiPortRange The lrmi port range to validate, or null to use the env var setting.
	 */
	public PortAvailabilityValidator(final String lrmiPortRange) {
		this.lrmiPortRange = lrmiPortRange;
	}


	/**
	 * Validates a connection can be established to the local host on at least one port in the lrmi port range.
	 * @throws CLIValidationException Indicates a failure to establish a connection.
	 */
	@Override
	public void validate() throws CLIValidationException {
		
		// get lrmi ports from the environment variable if not already set
		if (StringUtils.isBlank(lrmiPortRange)) {
			lrmiPortRange = System.getenv(CloudifyConstants.GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR);
		}
		
		//parse the port range
		if (StringUtils.isBlank(lrmiPortRange)) {
			throw new IllegalArgumentException("LRMI port range not configred. The environment variable \"" 
					+ CloudifyConstants.GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR + "\" is not set.");
		}
		
		if (lrmiPortRange.indexOf(PORT_RANGE_SEPARATOR) == -1) {
			throw new IllegalArgumentException("Invalid LRMI port range: " + lrmiPortRange + ". The expected "
					+ "format is <lowest port>-<highest port>, e.g. 7010-7110");
		}
		
		String lowestPortStr = StringUtils.substringBefore(lrmiPortRange, PORT_RANGE_SEPARATOR);
		String highestPortStr = StringUtils.substringAfter(lrmiPortRange, PORT_RANGE_SEPARATOR);
		int lowestPort = Integer.parseInt(lowestPortStr);
		int highestPort = Integer.parseInt(highestPortStr);
		
		IPUtils.validatePortIsFree(Constants.getHostAddress(), 0);
		
		try {
			// TODO handle not range, specific port
			IPUtils.validateConnectionInPortRange(Constants.getHostAddress(), lowestPort, highestPort);
		} catch (UnknownHostException uhe) {
			// thrown if the IP address of the host could not be determined.
			throw new CLIValidationException(uhe, CloudifyErrorMessages.PORT_VALIDATION_ABORTED_UNKNOWN_HOST.getName(),
					uhe.getMessage());
		} catch (IOException ioe) {
			// thrown if an I/O error occurs when creating the socket or connecting.
			throw new CLIValidationException(ioe, CloudifyErrorMessages.PORT_VALIDATION_ABORTED_IO_ERROR.getName(),
					ioe.getMessage());
		} catch (SecurityException se) {
			// thrown if a security manager exists and permission to resolve the host name is denied.
			throw new CLIValidationException(se, CloudifyErrorMessages.PORT_VALIDATION_ABORTED_NO_PERMISSION.getName(),
					se.getMessage());
		}
		
		
	}

}
