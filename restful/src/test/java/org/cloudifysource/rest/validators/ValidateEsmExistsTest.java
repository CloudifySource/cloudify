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

import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.internal.admin.DefaultAdmin;
import org.openspaces.admin.internal.esm.InternalElasticServiceManagers;

public class ValidateEsmExistsTest {

	private static final int TIMEOUT = 5000;
	private static final String ERR_MSG = CloudifyMessageKeys.ESM_MISSING.getName();
	private ValidateEsmExists validateEsmExists;
	private DefaultAdmin adminMock;
	
	@Before
	public void init() {
		validateEsmExists = new ValidateEsmExists();
		adminMock = Mockito.mock(DefaultAdmin.class);
		InternalElasticServiceManagers internalESMsMock = Mockito.mock(InternalElasticServiceManagers.class);
		Mockito.when(internalESMsMock.waitForAtLeastOne(TIMEOUT, TimeUnit.MILLISECONDS)).thenReturn(null);
		Mockito.when(adminMock.getElasticServiceManagers()).thenReturn(internalESMsMock);
	}
	
	@Test
	public void testServiceEsmExists() {
		InstallServiceValidationContext validationContext = new InstallServiceValidationContext();
		validationContext.setAdmin(adminMock);
		ValidatorsTestsUtils.validate(validateEsmExists, validationContext, ERR_MSG);
	}
	
	
	@Test
	public void testApplicationEsmExists() {
		InstallApplicationValidationContext validationContext = new InstallApplicationValidationContext();
		validationContext.setAdmin(adminMock);
		ValidatorsTestsUtils.validate(validateEsmExists, validationContext, ERR_MSG);
	}

}
