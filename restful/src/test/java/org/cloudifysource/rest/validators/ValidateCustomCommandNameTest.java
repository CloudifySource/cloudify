/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.validators;

import java.util.LinkedList;
import java.util.List;

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.ExecutableEntriesMap;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.junit.Before;
import org.junit.Test;

public class ValidateCustomCommandNameTest {

	private static final String RESERVED_PREFIX = CloudifyConstants.BUILT_IN_COMMAND_PREFIX;
	private static final String ERR_MSG = CloudifyErrorMessages.ILLEGAL_CUSTOM_COMMAND_PREFIX.getName();
	private ValidateCustomCommandName validateCustomCommandName;
	private ValidateApplicationServices validateApplicationServices;

	@Before
	public void init() {
		validateCustomCommandName = new ValidateCustomCommandName();
		validateApplicationServices = new ValidateApplicationServices();
		InstallServiceValidator[] installServiceValidators = {validateCustomCommandName};
		validateApplicationServices.setInstallServiceValidators(installServiceValidators);
	}
	
	@Test
	public void invalidCustomCommandNameInInstallService1() {
		Service service = new Service();
		ExecutableEntriesMap customCommands = new ExecutableEntriesMap();
		customCommands.put("key1", null);
		customCommands.put(RESERVED_PREFIX + " key2", null);
		service.setCustomCommands(customCommands);
		
		InstallServiceValidationContext context = new InstallServiceValidationContext();
		context.setService(service);
		ValidatorsTestsUtils.validate(validateCustomCommandName, context, ERR_MSG);
	}
	
	@Test
	public void invalidCustomCommandNameInInstallService2() {
		Service service = new Service();
		ExecutableEntriesMap customCommands = new ExecutableEntriesMap();
		customCommands.put(RESERVED_PREFIX, null);
		service.setCustomCommands(customCommands);
		
		InstallServiceValidationContext context = new InstallServiceValidationContext();
		context.setService(service);
		ValidatorsTestsUtils.validate(validateCustomCommandName, context, ERR_MSG);
	}
	
	@Test
	public void invalidCustomCommandNameInInstallService3() {
		Service service = new Service();
		service.setCustomCommands(new ExecutableEntriesMap());
		
		InstallServiceValidationContext context = new InstallServiceValidationContext();
		context.setService(service);
		ValidatorsTestsUtils.validate(validateCustomCommandName, context, null);
	}
	
	@Test
	public void invalidCustomCommandNameInInstallApplication1() {
		Service service = new Service();
		ExecutableEntriesMap customCommands = new ExecutableEntriesMap();
		customCommands.put("key1", null);
		customCommands.put(RESERVED_PREFIX + " key2", null);
		service.setCustomCommands(customCommands);
		
		InstallApplicationValidationContext context = new InstallApplicationValidationContext();
		Application application = new Application();
		List<Service> services = new LinkedList<Service>();
		services.add(service);
		application.setServices(services);
		context.setApplication(application);
		ValidatorsTestsUtils.validate(validateApplicationServices, context, ERR_MSG);
	}
	
	@Test
	public void invalidCustomCommandNameInInstallApplication2() {
		Service service = new Service();
		ExecutableEntriesMap customCommands = new ExecutableEntriesMap();
		customCommands.put(RESERVED_PREFIX, null);
		service.setCustomCommands(customCommands);
		
		InstallApplicationValidationContext context = new InstallApplicationValidationContext();
		Application application = new Application();
		List<Service> services = new LinkedList<Service>();
		services.add(service);
		application.setServices(services);
		context.setApplication(application);
		ValidatorsTestsUtils.validate(validateApplicationServices, context, ERR_MSG);
	}
	
	@Test
	public void invalidCustomCommandNameInInstallApplication3() {
		Service service = new Service();
		service.setCustomCommands(new ExecutableEntriesMap());
		
		InstallApplicationValidationContext context = new InstallApplicationValidationContext();
		Application application = new Application();
		List<Service> services = new LinkedList<Service>();
		services.add(service);
		application.setServices(services);
		context.setApplication(application);
		ValidatorsTestsUtils.validate(validateApplicationServices, context, null);
	}
}
