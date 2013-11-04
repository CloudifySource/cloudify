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

import org.cloudifysource.rest.ResponseConstants;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.internal.admin.DefaultAdmin;
import org.openspaces.admin.pu.ProcessingUnits;

/**
 * 
 * @author yael
 *
 */
public class ValidateServiceExistsTest {
	private static final long TIMEOUT_SECONDS = 10;
	private static final String PU_NAME = "app.service";

	@Test
	public void test() {
		UninstallServiceValidationContext context = new UninstallServiceValidationContext();
		DefaultAdmin admin = Mockito.mock(DefaultAdmin.class);
		ProcessingUnits pus = Mockito.mock(ProcessingUnits.class);
		
		Mockito.when(pus.waitFor(PU_NAME, TIMEOUT_SECONDS, TimeUnit.SECONDS)).thenReturn(null);
		Mockito.when(admin.getProcessingUnits()).thenReturn(pus);
		context.setAdmin(admin);
		context.setPuName(PU_NAME);
		
		ValidateServiceExists validator = new ValidateServiceExists();
		try {
			validator.validate(context);
			Assert.fail("RestErrorException expected");
		} catch (RestErrorException e) {
			Assert.assertEquals(ResponseConstants.FAILED_TO_LOCATE_SERVICE, e.getMessage());
		}
	}
}
