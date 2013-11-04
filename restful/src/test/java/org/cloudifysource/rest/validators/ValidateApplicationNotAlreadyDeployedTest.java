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

import java.util.Iterator;

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.Applications;
import org.openspaces.admin.internal.admin.DefaultAdmin;
import org.openspaces.admin.internal.application.DefaultApplication;

public class ValidateApplicationNotAlreadyDeployedTest {

	private static final String APP_NAME = "application";
	
	@Test
	public void test() {
		DefaultAdmin adminMock = Mockito.mock(DefaultAdmin.class);
		Applications applications = Mockito.mock(Applications.class);
		Iterator<Application> iter = Mockito.mock(Iterator.class);
		Mockito.when(iter.hasNext()).thenReturn(true, false);
		Mockito.when(iter.next()).thenReturn(new DefaultApplication(adminMock, APP_NAME));
		Mockito.when(applications.iterator()).thenReturn(iter);
		Mockito.when(adminMock.getApplications()).thenReturn(applications);
		org.cloudifysource.domain.Application application = new org.cloudifysource.domain.Application();
		application.setName(APP_NAME);
		
		InstallApplicationValidationContext context = new InstallApplicationValidationContext();
		context.setAdmin(adminMock);
		context.setApplication(application);
		
		ValidatorsTestsUtils.validate(
				new ValidateApplicationNotAlreadyDeployed(), 
				context, 
				CloudifyMessageKeys.APPLICATION_NAME_IS_ALREADY_IN_USE.getName());
	}

}
