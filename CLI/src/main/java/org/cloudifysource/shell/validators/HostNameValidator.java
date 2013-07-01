package org.cloudifysource.shell.validators;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.cloudifysource.shell.exceptions.CLIValidationException;

public class HostNameValidator implements CloudifyMachineValidator {

	@Override
	public void validate() throws CLIValidationException {
		try {
			InetAddress.getLocalHost().getHostName();
			InetAddress.getLocalHost().getHostAddress();
			InetAddress.getByName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			//throw new CLIValidationException(e);
		}		
	}

}
