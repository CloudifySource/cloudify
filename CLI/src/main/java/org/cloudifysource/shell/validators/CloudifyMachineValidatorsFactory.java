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
		return getValidators(null);
	}
	
	/**
	 * Gets a list of all {@link CloudifyMachineValidator} classes.
	 * @param nicAddress The nic address to validate.
	 * @return a list of all {@link CloudifyMachineValidator} classes.
	 */
	public static List<CloudifyMachineValidator> getValidators(final String nicAddress) {
		if (validatorsList == null) {
			initValidatorsList(nicAddress);
		}

		return validatorsList;
	}
	
	private static void initValidatorsList(final String nicAddress) {
		validatorsList = new ArrayList<CloudifyMachineValidator>();
		validatorsList.add(new HostNameValidator());
		validatorsList.add(new NicAddressValidator(nicAddress));
		validatorsList.add(new LusConnectionValidator());
		validatorsList.add(new PortAvailabilityValidator());
	}
}
