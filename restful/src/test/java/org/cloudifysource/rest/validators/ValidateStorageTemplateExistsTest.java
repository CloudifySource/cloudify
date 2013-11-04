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

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.StorageDetails;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.storage.CloudStorage;
import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.junit.Before;
import org.junit.Test;

public class ValidateStorageTemplateExistsTest {

	private static final String TEMPLATE_NAME = "storage_template";
	private static final String ERR_MSG = CloudifyErrorMessages.MISSING_TEMPLATE.getName();
	
	private ValidateStorageTemplateExists validateStorageTemplateExists;
	private ValidateApplicationServices validateApplicationServices;

	@Before
	public void init() {
		validateStorageTemplateExists = new ValidateStorageTemplateExists();
		validateApplicationServices = new ValidateApplicationServices();
		InstallServiceValidator[] installServiceValidators = {validateStorageTemplateExists};
		validateApplicationServices.setInstallServiceValidators(installServiceValidators);
	}
	
	private Cloud createCloudWithStorageTemplate(final String templateName) {
		Cloud cloud = new Cloud();
		Map<String, StorageTemplate> templates = new HashMap<String, StorageTemplate>();
		StorageTemplate template = new StorageTemplate();
		templates .put(templateName, template);
		CloudStorage cloudStorage = new CloudStorage();
		cloudStorage.setTemplates(templates);
		cloud.setCloudStorage(cloudStorage);
		return cloud;
	}
	
	private Service createServiceWithStorageTemplate(final String templateName) {
		Service service = new Service();
		StorageDetails storage = new StorageDetails();
		storage.setTemplate(templateName);
		service.setStorage(storage);
		return service;
	}
	
	@Test
	public void testServiceStorageNotExistAtCloudStorage() {
		Cloud cloud = createCloudWithStorageTemplate(TEMPLATE_NAME);
		Service service = createServiceWithStorageTemplate(TEMPLATE_NAME + "_service");
		
		InstallServiceValidationContext validationContext = new InstallServiceValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setService(service);

		ValidatorsTestsUtils.validate(validateStorageTemplateExists, validationContext , ERR_MSG);
	}
	
	@Test
	public void testServiceStorageNotExistAtCloudComputeAndStorage() {
		Cloud cloud = new Cloud();
		Service service = createServiceWithStorageTemplate(TEMPLATE_NAME);
		
		InstallServiceValidationContext validationContext = new InstallServiceValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setService(service);

		ValidatorsTestsUtils.validate(validateStorageTemplateExists, validationContext , ERR_MSG);
	}

	@Test
	public void testApplicationServiceStorageNotExistAtCloudStorage() {
		Cloud cloud = createCloudWithStorageTemplate(TEMPLATE_NAME);
		Service service = createServiceWithStorageTemplate(TEMPLATE_NAME + "_service");
		
		InstallApplicationValidationContext validationContext = new InstallApplicationValidationContext();
		validationContext.setCloud(cloud);
		Application application = new Application();
		application.setService(service);
		validationContext.setApplication(application);

		ValidatorsTestsUtils.validate(validateApplicationServices, validationContext , ERR_MSG);
	}
	
	@Test
	public void testApplicationServiceStorageNotExistAtCloudComputeAndStorage() {
		Cloud cloud = new Cloud();
		Service service = createServiceWithStorageTemplate(TEMPLATE_NAME);
		
		InstallApplicationValidationContext validationContext = new InstallApplicationValidationContext();
		validationContext.setCloud(cloud);
		Application application = new Application();
		application.setService(service);
		validationContext.setApplication(application);

		ValidatorsTestsUtils.validate(validateApplicationServices, validationContext , ERR_MSG);
	}
	
}
