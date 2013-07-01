package org.cloudifysource.shell.validators;

import java.util.ArrayList;
import java.util.List;

/**
 * This factory classes creates a list of all {@link CloudifyMachineValidator} classes and returns it.
 * @author noak
 * @since 2.7.0
 */
public final class CloudifyMachineValidatorsFactory {

	private static List<CloudifyMachineValidator> validatorsList = null;
	
	private CloudifyMachineValidatorsFactory() {
		//private ctor, not meant to be called.
	}
	
	/**
	 * Gets a list of all {@link CloudifyMachineValidator} classes.
	 * @return a list of all {@link CloudifyMachineValidator} classes.
	 */
	public static List<CloudifyMachineValidator> getValidators() {
		if (validatorsList == null) {
			initValidatorsList();
		}

		return validatorsList;
	}
	
	private static void initValidatorsList() {
		validatorsList = new ArrayList<CloudifyMachineValidator>();
		validatorsList.add(new HostNameValidator());
		validatorsList.add(new NicAddressValidator());
		validatorsList.add(new LusConnectionValidator());
		validatorsList.add(new PortAvailabilityValidator());
	}
}
