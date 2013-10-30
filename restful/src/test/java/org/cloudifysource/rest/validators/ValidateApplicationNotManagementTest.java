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

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.rest.ResponseConstants;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.Applications;
import org.openspaces.admin.internal.admin.DefaultAdmin;
import org.openspaces.admin.internal.application.DefaultApplication;
import org.openspaces.admin.internal.application.DefaultApplications;

public class ValidateApplicationNotManagementTest {
	private static final long TIMEOUT_SECONDS = 10;
	private static final String APP_NAME = CloudifyConstants.MANAGEMENT_APPLICATION_NAME;

	@Test
	public void test() {
		UninstallApplicationValidationContext context = new UninstallApplicationValidationContext();
		DefaultAdmin admin = Mockito.mock(DefaultAdmin.class);
		Applications applications = Mockito.mock(DefaultApplications.class);
		Mockito.when(applications.getAdmin()).thenReturn(admin);
		Application application = new DefaultApplication(admin, APP_NAME);
		Mockito.when(applications.waitFor(APP_NAME, TIMEOUT_SECONDS, TimeUnit.SECONDS)).thenReturn(application);
		Mockito.when(admin.getApplications()).thenReturn(applications);
		context.setAdmin(admin);
		context.setApplicationName(APP_NAME);
		
		ValidateApplicationNotManagement validator = new ValidateApplicationNotManagement();
		try {
			validator.validate(context);
			Assert.fail("RestErrorException expected");
		} catch (RestErrorException e) {
			Assert.assertEquals(ResponseConstants.CANNOT_UNINSTALL_MANAGEMENT_APP, e.getMessage());
		}
	}
}
