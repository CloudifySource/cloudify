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
 * This is an abstract class that implements generic validations of configuration parsing and port availability tests.
 * @author noak
 * @since 2.7.0
 */
public abstract class PortAvailabilityValidator {
	
	private String gsaPortOrRange;
	private String gscPortOrRange;
	private String lusPortOrRange;
	private String esmPortOrRange;
	private String gsmPortOrRange;
	

	/**
	 * Setter for GSC lrmi port or range.
	 * @param gscPortOrRange The GSC port or range
	 */
	public void setGscPortOrRange(final String gscPortOrRange) {
		this.gscPortOrRange = gscPortOrRange;
	}
	
	
	/**
	 * Setter for GSA lrmi port or range.
	 * @param gsaPortOrRange The GSA port or range
	 */
	public void setGsaPortOrRange(final String gsaPortOrRange) {
		this.gsaPortOrRange = gsaPortOrRange;
	}
	
	
	/**
	 * Setter for LUS lrmi port or range.
	 * @param lusPortOrRange The LUS port or range
	 */
	public void setLusPortOrRange(final String lusPortOrRange) {
		this.lusPortOrRange = lusPortOrRange;
	}
	
	
	/**
	 * Setter for ESM lrmi port or range.
	 * @param esmPortOrRange The ESM port or range
	 */
	public void setEsmPortOrRange(final String esmPortOrRange) {
		this.esmPortOrRange = esmPortOrRange;
	}
	
	
	/**
	 * Setter for GSM lrmi port or range.
	 * @param gsmPortOrRange The GSM port or range
	 */
	public void setGsmPortOrRange(final String gsmPortOrRange) {
		this.gsmPortOrRange = gsmPortOrRange;
	}
	

	/**
	 * Validates the GSC lrmi port or range is available.
	 * @throws CLIValidationException Indicated the validation failed.
	 */
	protected void validateGscPorts() throws CLIValidationException {
		
		// get GSC lrmi ports from the environment variable if not already set
		if (StringUtils.isBlank(gscPortOrRange)) {
			gscPortOrRange = System.getenv(CloudifyConstants.GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR);
			
			if (StringUtils.isBlank(gscPortOrRange)) {
				throw new IllegalArgumentException("GSC LRMI port range not configred. The environment variable \"" 
						+ CloudifyConstants.GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR + "\" is not set.");
			}
		}
		
		try {
			validateFreePorts(gscPortOrRange);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("GSC LRMI ports validation failed. " + e.getMessage(), e);
		}
	}
	

	/**
	 * Validates the GSA lrmi port or range is available.
	 * @throws CLIValidationException Indicated the validation failed.
	 */
	protected void validateGsaPorts() throws CLIValidationException {
		
		// get GSA lrmi ports from the environment variable if not already set
		if (StringUtils.isBlank(gsaPortOrRange)) {
			gsaPortOrRange = retrievePortOrRange(CloudifyConstants.GSA_JAVA_OPTIONS_ENVIRONMENT_VAR);
			
			if (StringUtils.isBlank(gsaPortOrRange)) {
				throw new IllegalArgumentException("GSA java options port or range not configred. The environment "
							+ "variable \"" + CloudifyConstants.GSA_JAVA_OPTIONS_ENVIRONMENT_VAR + "\" is not set.");
			}
		}
		
		try {
			validateFreePorts(gsaPortOrRange);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("GSA LRMI ports validation failed. " + e.getMessage(), e);
		}
	}
	

	/**
	 * Validates the LUS port or range is available.
	 * @throws CLIValidationException Indicated the validation failed.
	 */
	protected void validateLusPorts() throws CLIValidationException {
		
		// get LUS lrmi ports from the environment variable if not already set
		if (StringUtils.isBlank(lusPortOrRange)) {
			lusPortOrRange = retrievePortOrRange(CloudifyConstants.LUS_JAVA_OPTIONS_ENVIRONMENT_VAR);
			
			if (StringUtils.isBlank(lusPortOrRange)) {
				throw new IllegalArgumentException("LUS java options port or range not configred. The environment "
							+ "variable \"" + CloudifyConstants.LUS_JAVA_OPTIONS_ENVIRONMENT_VAR + "\" is not set.");
			}
		}
		
		try {
			validateFreePorts(lusPortOrRange);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("LUS LRMI ports validation failed. " + e.getMessage(), e);
		}
		
	}


	/**
	 * Validates the GSM lrmi port or range is available.
	 * @throws CLIValidationException Indicated the validation failed.
	 */
	protected void validateGsmPorts() throws CLIValidationException {
		
		// get GSM lrmi ports from the environment variable if not already set
		if (StringUtils.isBlank(gsmPortOrRange)) {
			gsmPortOrRange = retrievePortOrRange(CloudifyConstants.GSM_JAVA_OPTIONS_ENVIRONMENT_VAR);
			
			if (StringUtils.isBlank(gsmPortOrRange)) {
				throw new IllegalArgumentException("GSM java options port or range not configred. The environment "
							+ "variable \"" + CloudifyConstants.GSM_JAVA_OPTIONS_ENVIRONMENT_VAR + "\" is not set.");
			}
		}
		
		try {
			validateFreePorts(gsmPortOrRange);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("GSM LRMI ports validation failed. " + e.getMessage(), e);
		}
		
	}

	
	/**
	 * Validates the ESM lrmi port or range is available.
	 * @throws CLIValidationException Indicated the validation failed.
	 */
	protected void validateEsmPorts() throws CLIValidationException {
		
		// get ESM lrmi ports from the environment variable if not already set
		if (StringUtils.isBlank(esmPortOrRange)) {
			esmPortOrRange = retrievePortOrRange(CloudifyConstants.ESM_JAVA_OPTIONS_ENVIRONMENT_VAR);
			
			if (StringUtils.isBlank(esmPortOrRange)) {
				throw new IllegalArgumentException("ESM java options port or range not configred. The environment "
							+ "variable \"" + CloudifyConstants.ESM_JAVA_OPTIONS_ENVIRONMENT_VAR + "\" is not set.");
			}
		}
		
		try {
			validateFreePorts(esmPortOrRange);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("ESM LRMI ports validation failed. " + e.getMessage(), e);
		}
	}
	
	
	private void validateFreePorts(final String portOrRange) throws CLIValidationException {
		try {
			if (IPUtils.isValidPortRange(portOrRange)) {
				int lowestPort = IPUtils.getMinimumPort(portOrRange);
				int highestPort = IPUtils.getMaximumPort(portOrRange);
				IPUtils.validatePortIsFreeInRange(Constants.getHostAddress(), lowestPort, highestPort);
			} else {
				try {
					int port = Integer.parseInt(portOrRange);
					if (IPUtils.isValidPortNumber(port)) {
						IPUtils.validatePortIsFree(Constants.getHostAddress(), port);
					} else {
						throw new IllegalArgumentException("Invalid port or range: " + portOrRange);
					}
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid port or range: " + portOrRange);
				}
			}
		} catch (UnknownHostException uhe) {
			// thrown if the IP address of the host could not be determined.
			throw new CLIValidationException(uhe, 124,
					CloudifyErrorMessages.PORT_VALIDATION_ABORTED_UNKNOWN_HOST.getName(), uhe.getMessage());
		} catch (IOException ioe) {
			// thrown if an I/O error occurs when creating the socket or connecting.
			throw new CLIValidationException(ioe, 125,
					CloudifyErrorMessages.PORT_VALIDATION_ABORTED_IO_ERROR.getName(), ioe.getMessage());
		} catch (SecurityException se) {
			// thrown if a security manager exists and permission to resolve the host name is denied.
			throw new CLIValidationException(se, 126,
					CloudifyErrorMessages.PORT_VALIDATION_ABORTED_NO_PERMISSION.getName(), se.getMessage());
		}		
	}
	
	
	/**
	 * Retrieves the port or range specified by the CloudifyConstants.LRMI_PORT_OR_RANGE_SYS_PROP system property 
	 * in the given environment variable.
	 * @param envVar The environment variable containing the port configuration
	 * @return the port or range defined
	 */
	private static String retrievePortOrRange(final String envVar) {
		
		String portOrRange = "";
		
		String javaOptionsStr = System.getenv(envVar);
		if (StringUtils.isBlank(javaOptionsStr)) {
			throw new IllegalArgumentException("The environment variable \"" + envVar + "\" is not set.");
		}
		
		int sysPropIndex = javaOptionsStr.indexOf(CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "=");
		if (sysPropIndex == -1) {
			throw new IllegalArgumentException("javaOptionsStr is missing the system property \"" 
					+ CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "\"");
		}
		
		int startIndex = sysPropIndex + CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY.length() + 1;
		int endIndex = javaOptionsStr.indexOf(" ", startIndex);
		
		if (endIndex > -1) {
			portOrRange = javaOptionsStr.substring(startIndex, endIndex);
		} else {
			portOrRange = javaOptionsStr.substring(startIndex);
		}

		if (StringUtils.isNotBlank(portOrRange)) {
			portOrRange = portOrRange.trim();
		}
		
		return portOrRange;
	}

}
