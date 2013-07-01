package org.cloudifysource.shell.validators;

import java.io.IOException;
import java.net.UnknownHostException;

import net.jini.discovery.Constants;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.shell.exceptions.CLIValidationException;

public class PortAvailabilityValidator implements CloudifyMachineValidator {
	
	private static final String PORT_RANGE_SEPARATOR = "-";

	/**
	 * Validates a connection can be on at least one port in the lrmi port range.
	 */
	@Override
	public void validate() throws CLIValidationException {
		// get lrmi ports
		String lrmiPortRange = System.getenv(CloudifyConstants.GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR);
		
		//parse the port range
		if (lrmiPortRange == null) {
			throw new IllegalArgumentException("LRMI port range not configred. The environment variable " 
					+ CloudifyConstants.GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR + " is not set");
		}
		
		if (lrmiPortRange.indexOf(PORT_RANGE_SEPARATOR) == -1) {
			throw new IllegalArgumentException("Invalid LRMI port range: " + lrmiPortRange + ". The expected "
					+ "format is <lowest port>-<highest port>, e.g. 7010-7110");
		}
		
		String lowestPortStr = StringUtils.substringBefore(lrmiPortRange, PORT_RANGE_SEPARATOR);
		String highestPortStr = StringUtils.substringAfter(lrmiPortRange, PORT_RANGE_SEPARATOR);
		
		int lowestPort = Integer.parseInt(lowestPortStr);
		int highestPort = Integer.parseInt(highestPortStr);
		
		try {
			//getNicAddressFromEnvVar
			IPUtils.validateConnection(Constants.getHostAddress(), lowestPort, highestPort);
		} catch (UnknownHostException uhe) {
			// thrown if the IP address of the host could not be determined.
			// TODO noak handle
			//throw new CLIValidationException();
		} catch (IOException ioe) {
			// thrown if an I/O error occurs when creating the socket.
			// TODO noak handle
			//throw new CLIValidationException();
		} catch (SecurityException se) {
			// thrown if a security manager exists and its checkConnect method doesn't allow the operation.
			// TODO noak handle
			//throw new CLIValidationException();
		}
		
		
	}

}
