package org.cloudifysource.shell.validators;

import java.io.IOException;
import java.net.UnknownHostException;

import net.jini.discovery.Constants;

import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.shell.exceptions.CLIValidationException;

public class NicAddressValidator implements CloudifyMachineValidator {

	@Override
	public void validate() throws CLIValidationException {
		try {
			//getNicAddressFromEnvVar
			IPUtils.validateConnection(Constants.getHostAddress(), 0);
		} catch (UnknownHostException uhe) {
			// thrown if the IP address of the host could not be determined.
			// TODO noak handle
		} catch (IOException ioe) {
			// thrown if an I/O error occurs when creating the socket.
			// TODO noak handle
		} catch (SecurityException se) {
			// thrown if a security manager exists and its checkConnect method doesn't allow the operation.
			// TODO noak handle
		}
		
	}

}
