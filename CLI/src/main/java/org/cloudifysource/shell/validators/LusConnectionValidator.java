package org.cloudifysource.shell.validators;

import java.io.IOException;
import java.net.UnknownHostException;

import net.jini.discovery.Constants;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.shell.exceptions.CLIValidationException;

public class LusConnectionValidator implements CloudifyMachineValidator {

	@Override
	public void validate() throws CLIValidationException {
		
		// get lrmi ports from the environment variable
		String lusIpAddress = System.getenv(CloudifyConstants.LUS_IP_ADDRESS_ENV);
		
		//parse the ip address and port
		try {
			//getNicAddressFromEnvVar
			IPUtils.validateConnectionInPortRange(Constants.getHostAddress(), lowestPort, highestPort);
		} catch (UnknownHostException uhe) {
			// thrown if the IP address of the host could not be determined.
			throw new CLIValidationException(uhe, CloudifyErrorMessages.PORT_VALIDATION_ABORTED_UNKNOWN_HOST.getName());
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
