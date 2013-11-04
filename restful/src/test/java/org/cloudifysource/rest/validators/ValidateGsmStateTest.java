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
import java.util.concurrent.TimeUnit;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.CloudConfiguration;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.admin.DefaultAdmin;
import org.openspaces.admin.internal.gsm.DefaultGridServiceManagers;

public class ValidateGsmStateTest {

    protected static final String FOLDER = "src/test/resources/validators";

	private static final long TIMEOUT_SECONDS = 10;
    private static final String CLOUD_FILE_PATH = ValidatorsTestsUtils.FOLDER + "/byon/byon-cloud.groovy";
    private static final String ERR_MSG = CloudifyMessageKeys.NOT_ALL_GSM_INSTANCES_RUNNING.getName();
	
    private ValidateGsmState validateGsmState;
	private DefaultAdmin adminMock;
	private Cloud cloud;

	@Before
	public void getValidatorInstance() {
		validateGsmState = new ValidateGsmState();
		
		cloud = null;
		try {
			cloud = ServiceReader.readCloud(new File(CLOUD_FILE_PATH));
		} catch (Exception e) {
			Assert.fail(e.getLocalizedMessage());
		}
		CloudConfiguration configuration = cloud.getConfiguration();
		String persistentStoragePath = "";
		configuration.setPersistentStoragePath(persistentStoragePath);
		DefaultGridServiceManagers internalGSMs = Mockito.mock(DefaultGridServiceManagers.class);
		Mockito.when(internalGSMs.waitFor(
				cloud.getProvider().getNumberOfManagementMachines(), TIMEOUT_SECONDS, TimeUnit.SECONDS))
				.thenReturn(false);
		
		adminMock = Mockito.mock(DefaultAdmin.class);
		GridServiceManager[] gsmArray = {};
		Mockito.when(internalGSMs.getManagers()).thenReturn(gsmArray);
		Mockito.when(adminMock.getGridServiceManagers()).thenReturn(internalGSMs);
	}
	
	@Test
	public void testGsmStateInstallService() {
		InstallServiceValidationContext validationContext = new InstallServiceValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setAdmin(adminMock);
		ValidatorsTestsUtils.validate(validateGsmState, validationContext , ERR_MSG);
	}
	
	@Test
	public void testGsmStateUninstallService() {
		UninstallServiceValidationContext validationContext = new UninstallServiceValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setAdmin(adminMock);
		ValidatorsTestsUtils.validate(validateGsmState, validationContext , ERR_MSG);
	}
	
	@Test
	public void testGsmStateInstallApplication() {
		InstallApplicationValidationContext validationContext = new InstallApplicationValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setAdmin(adminMock);
		ValidatorsTestsUtils.validate(validateGsmState, validationContext , ERR_MSG);
	}
	
	@Test
	public void testGsmStateUninstallApplication() {
		UninstallApplicationValidationContext validationContext = new UninstallApplicationValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setAdmin(adminMock);
		ValidatorsTestsUtils.validate(validateGsmState, validationContext , ERR_MSG);
	}
	
	@Test
	public void testGsmStateSetServiceInstances() {
		SetServiceInstancesValidationContext validationContext = new SetServiceInstancesValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setAdmin(adminMock);
		ValidatorsTestsUtils.validate(validateGsmState, validationContext , ERR_MSG);
	}

}
