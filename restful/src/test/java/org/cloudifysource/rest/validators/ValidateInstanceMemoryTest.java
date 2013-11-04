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

import java.io.File;

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.ComputeDetails;
import org.cloudifysource.domain.GlobalIsolationSLADescriptor;
import org.cloudifysource.domain.IsolationSLA;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Assert;
import org.junit.Test;

public class ValidateInstanceMemoryTest {

	private static final String TEMPLATE_NAME = "TEMPLATE_1";
    private static final String CLOUD_FILE_PATH = ValidatorsTestsUtils.FOLDER + "/byon/byon-cloud.groovy";
    private static final String ERR_MSG = CloudifyMessageKeys.INSUFFICIENT_MEMORY.getName();
	
    private Cloud byonCloud;
	private Service service;
	private ValidateInstanceMemory validateInstanceMemory;
	private ValidateApplicationServices validateApplicationServices;
	
    public void initInsufficientMemory(final boolean includeReservedMemory) {	
    	validateInstanceMemory = new ValidateInstanceMemory();
    	validateApplicationServices = new ValidateApplicationServices();
		InstallServiceValidator[] installServiceValidators = {new ValidateInstanceMemory()};
		validateApplicationServices.setInstallServiceValidators(installServiceValidators);
		
		// cloud
    	try {
			byonCloud = ServiceReader.readCloud(new File(CLOUD_FILE_PATH));
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
    	int templateMemory = byonCloud.getCloudCompute().getTemplates().get(TEMPLATE_NAME).getMachineMemoryMB();
    	int reservedMemoryCapacityPerMachineInMB = byonCloud.getProvider().getReservedMemoryCapacityPerMachineInMB();
    	// service
    	service = new Service();
    	ComputeDetails compute = new ComputeDetails();
    	compute.setTemplate(TEMPLATE_NAME);
    	service.setCompute(compute);
    	IsolationSLA isolationSLA = new IsolationSLA();
    	GlobalIsolationSLADescriptor global = new GlobalIsolationSLADescriptor();
    	int instanceMemory = templateMemory;
    	if (includeReservedMemory) {
    		instanceMemory = templateMemory - reservedMemoryCapacityPerMachineInMB + 1;
    	} 
		global.setInstanceMemoryMB(instanceMemory);
    	isolationSLA.setGlobal(global);
    	service.setIsolationSLA(isolationSLA);
    }
    
	@Test
	public void testInsufficientMemoryInInstallService() {
		initInsufficientMemory(false);
		InstallServiceValidationContext context = new InstallServiceValidationContext();
		context.setCloud(byonCloud);
		context.setService(service);
		ValidatorsTestsUtils.validate(validateInstanceMemory, context, ERR_MSG);
	}
	
	@Test
	public void testInsufficientReservedMemoryInInstallService() {
		initInsufficientMemory(true);
		InstallServiceValidationContext context = new InstallServiceValidationContext();
		context.setCloud(byonCloud);
		context.setService(service);
		ValidatorsTestsUtils.validate(validateInstanceMemory, context, ERR_MSG);
	}
    
	@Test
	public void testInsufficientMemoryInInstallApplication() {
		initInsufficientMemory(false);
		InstallApplicationValidationContext context = new InstallApplicationValidationContext();
		context.setCloud(byonCloud);
		Application application = new Application();
		application.setService(service);
		context.setApplication(application);
		
		ValidatorsTestsUtils.validate(validateApplicationServices, context, ERR_MSG);
	}
	
	@Test
	public void testInsufficientReservedMemoryInInstallApplication() {
		initInsufficientMemory(true);
		InstallApplicationValidationContext context = new InstallApplicationValidationContext();
		context.setCloud(byonCloud);
		Application application = new Application();
		application.setService(service);
		context.setApplication(application);
		
		ValidatorsTestsUtils.validate(validateApplicationServices, context, ERR_MSG);
	}
	
}
