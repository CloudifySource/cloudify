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
package org.cloudifysource.rest.validators;

import java.io.File;

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.junit.Assert;
import org.openspaces.admin.Admin;

/**
 * @author yael
 * 
 */
public abstract class InstallApplicationValidatorTest {

	private Cloud cloud;
	private Admin admin; 
	private Application application;
	private File applicationOverridesFile; 
	private File cloudConfigurationFile;
	private File cloudOverridesFile;
	private boolean debugAll;
	private String debugEvents; 
	private String debugMode;
	private String exceptionCause;
	
	public abstract InstallApplicationValidator getValidatorInstance();
	public void init() {
	}

	public void testValidator() {

		final InstallApplicationValidator validator = getValidatorInstance();
		final InstallApplicationValidationContext validationContext = new InstallApplicationValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setAdmin(admin);
		validationContext.setApplication(application);
		validationContext.setApplicationOverridesFile(applicationOverridesFile);
		validationContext.setCloudConfigurationFile(cloudConfigurationFile);
		validationContext.setCloudOverridesFile(cloudOverridesFile);
		validationContext.setDebugAll(debugAll);
		validationContext.setDebugEvents(debugEvents);
		validationContext.setDebugMode(debugMode);

		try {
			validator.validate(validationContext);
			if (exceptionCause != null) {
				Assert.fail(exceptionCause + " didn't yield the expected RestErrorException.");
			}
		} catch (final RestErrorException e) {
			if (exceptionCause == null) {
				e.printStackTrace();
				Assert.fail();
			}
			Assert.assertEquals(exceptionCause, e.getMessage());
		}
	}
}
