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

	private static List<CloudifyMachineValidator> managementValidatorsList = null;
	private static List<CloudifyMachineValidator> agentValidatorsList = null;
	
	private CloudifyMachineValidatorsFactory() {
		//private ctor, not meant to be called.
	}
	
	/**
	 * Gets a list of all {@link CloudifyMachineValidator} classes required.
	 * @param isManagement Trus if the list is required for a management machine. False otherwise.
	 * @return a list of all {@link CloudifyMachineValidator} classes.
	 */
	public static List<CloudifyMachineValidator> getValidators(final boolean isManagement) { 
		return getValidators(isManagement, null);
	}
	
	/**
	 * Gets a list of all {@link CloudifyMachineValidator} classes required for agents.
	 * @param isManagement is the list of validators meant for a management machine or not.
	 * @param nicAddress The nic address to validate.
	 * @return a list of all {@link CloudifyMachineValidator} classes.
	 */
	public static List<CloudifyMachineValidator> getValidators(final boolean isManagement, 
			final String nicAddress) {
		
		List<CloudifyMachineValidator> validatorsList = null;
		
		if (isManagement) {
			if (managementValidatorsList == null) {
				initManagementValidatorsList(nicAddress);
			}
			validatorsList = managementValidatorsList;
		} else {
			// agent
			if (agentValidatorsList == null) {
				initAgentValidatorsList(nicAddress);
			}
			validatorsList = agentValidatorsList;
		}
		
		return validatorsList;
	}
	
	
	private static void initManagementValidatorsList(final String nicAddress) {
		managementValidatorsList = new ArrayList<CloudifyMachineValidator>();
		managementValidatorsList.add(new HostNameValidator());
		//managementValidatorsList.add(new NicAddressValidator(nicAddress));
		//managementValidatorsList.add(new LusConnectionValidator());
		managementValidatorsList.add(new PortAvailabilityManagementValidator());
	}
	
	
	private static void initAgentValidatorsList(final String nicAddress) {
		agentValidatorsList = new ArrayList<CloudifyMachineValidator>();
		agentValidatorsList.add(new HostNameValidator());
		NicAddressValidator nicAddressValidator = new NicAddressValidator();
		nicAddressValidator.setNicAddress(nicAddress);
		agentValidatorsList.add(nicAddressValidator);
		agentValidatorsList.add(new LusConnectionValidator());
		agentValidatorsList.add(new PortAvailabilityAgentValidator());
	}
	
}
