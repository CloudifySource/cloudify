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
package org.cloudifysource.rest;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import junit.framework.Assert;

import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.rest.controllers.DeploymentsController;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.repo.UploadRepo;
import org.cloudifysource.rest.validators.InstallApplicationValidator;
import org.cloudifysource.rest.validators.InstallServiceValidator;
import org.cloudifysource.rest.validators.ValidateOverridesFileSize;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author yael
 * 
 */
public class TestOverridesFileSizeLimitValidations {
	private static final String BASE_PATH =
			"src" + File.separator + "test" + File.separator + "resources" + File.separator + "validators";

	private static final String SERVICE_NAME = "simple";
	private static final String SERVICE_FOLDER_PATH = BASE_PATH + File.separator + "simple";
	private static final String SERVICE_DSL_FILE_NAME = SERVICE_NAME + DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX;
	private static final String SERVICE_OVERRIDES_FILE_PATH =
			BASE_PATH + File.separator + SERVICE_NAME + DSLUtils.SERVICE_OVERRIDES_FILE_NAME_SUFFIX;
	private static final String SERVICE_WITH_OVERRIDES_FOLDER_PATH =
			BASE_PATH + File.separator + "simpleWithOverrides";

	private static final String APPLICATION_NAME = "simpleApp";
	private static final String APPLICATION_FOLDER_PATH = BASE_PATH + File.separator + APPLICATION_NAME;
	private static final String APPLICATION_OVERRIDES_FILE_PATH = 
			BASE_PATH + File.separator + APPLICATION_NAME + "-" + DSLUtils.APPLICATION_OVERRIDES_FILE_NAME;
	private static final String APPLICATION_WITH_OVERRIDES_FOLDER_PATH =
			BASE_PATH + File.separator + "simpleAppWithOverrides";

	private static final long TEST_OVERRIDES_FILE_SIZE_LIMIT = 3;

	@Test
	public void testServiceExternalOverridesFileSizeLimitValidations()
			throws IOException, PackagingException, DSLException {
		testServiceOverridesFileSizeLimitValidations(
				new File(SERVICE_FOLDER_PATH),
				new File(SERVICE_OVERRIDES_FILE_PATH));
	}

	@Test
	public void testServiceOverridesFileSizeLimitValidations()
			throws IOException, PackagingException, DSLException {
		testServiceOverridesFileSizeLimitValidations(
				new File(SERVICE_WITH_OVERRIDES_FOLDER_PATH),
				null);
	}
	
	@Test
	public void testApplicationExternalOverridesFileSizeLimitValidations() {
		testApplicationOverridesFileSizeLimitValidations(
				new File(APPLICATION_FOLDER_PATH),
				new File(APPLICATION_OVERRIDES_FILE_PATH));
	}

	@Test
	public void testApplicationOverridesFileSizeLimitValidations() {
		testApplicationOverridesFileSizeLimitValidations(
				new File(APPLICATION_WITH_OVERRIDES_FOLDER_PATH),
				null);
	}

	public void testServiceOverridesFileSizeLimitValidations(final File serviceFolder, final File overridesFile)
			throws IOException, PackagingException, DSLException {
		final Service service = ServiceReader.readService(
				new File(serviceFolder, SERVICE_DSL_FILE_NAME),
				serviceFolder,
				null,
				true,
				overridesFile);
		final File servicePackedFolder = Packager.pack(serviceFolder, service, null);

		// upload repo mock
		final UploadRepo mockRepo = Mockito.mock(UploadRepo.class);
		final String serviceFolderUploadKey = UUID.randomUUID().toString();
		String overridesFileUploadKey = null;
		if (overridesFile != null) {
			overridesFileUploadKey = UUID.randomUUID().toString();
		}
		Mockito.when(mockRepo.get(serviceFolderUploadKey)).thenReturn(servicePackedFolder);
		Mockito.when(mockRepo.get(overridesFileUploadKey)).thenReturn(overridesFile);

		// rest config mock
		final RestConfiguration mockRestConfig = Mockito.mock(RestConfiguration.class);
		Mockito.when(mockRestConfig.getCloud()).thenReturn(null);

		// create controller
		final DeploymentsController controller = new DeploymentsController();
		controller.setRepo(mockRepo);
		controller.setRestConfig(mockRestConfig);
		final ValidateOverridesFileSize validator = new ValidateOverridesFileSize();
		final InstallServiceValidator[] installServiceValidators = { validator };
		validator.setServiceOverridesFileSizeLimit(TEST_OVERRIDES_FILE_SIZE_LIMIT);
		controller.setInstallServiceValidators(installServiceValidators);

		// create request
		final InstallServiceRequest request = new InstallServiceRequest();
		request.setServiceFolderUploadKey(serviceFolderUploadKey);
		request.setServiceOverridesUploadKey(overridesFileUploadKey);

		// asserts
		try {
			controller.installService(
					CloudifyConstants.DEFAULT_APPLICATION_NAME,
					SERVICE_NAME,
					request);
			Assert.fail("Expected RestErrorException");
		} catch (final RestErrorException e) {
			final String message = e.getMessage();
			Assert.assertEquals(CloudifyMessageKeys.SERVICE_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName(), message);
		}
	}

	public void testApplicationOverridesFileSizeLimitValidations(final File appFolder, final File overridesFile) {

		// upload repo mock
		final UploadRepo mockRepo = Mockito.mock(UploadRepo.class);
		final String appFolderUploadKey = UUID.randomUUID().toString();
		String overridesFileUploadKey = null;
		if (overridesFile != null) {
			overridesFileUploadKey = UUID.randomUUID().toString();
		}
		Mockito.when(mockRepo.get(appFolderUploadKey)).thenReturn(appFolder);
		Mockito.when(mockRepo.get(overridesFileUploadKey)).thenReturn(overridesFile);

		// rest config mock
		final RestConfiguration mockRestConfig = Mockito.mock(RestConfiguration.class);
		Mockito.when(mockRestConfig.getCloud()).thenReturn(null);

		// create controller
		final DeploymentsController controller = new DeploymentsController();
		controller.setRepo(mockRepo);
		controller.setRestConfig(mockRestConfig);
		final ValidateOverridesFileSize validator = new ValidateOverridesFileSize();
		InstallApplicationValidator[] installApplicationValidators = { validator };
		validator.setApplicationOverridesFileSizeLimit(TEST_OVERRIDES_FILE_SIZE_LIMIT);
		controller.setInstallApplicationValidators(installApplicationValidators);

		// create request
		final InstallApplicationRequest request = new InstallApplicationRequest();
		request.setApplcationFileUploadKey(appFolderUploadKey);
		request.setApplicationOverridesUploadKey(overridesFileUploadKey);

		// asserts
		try {
			controller.installApplication(APPLICATION_NAME, request);
			Assert.fail("Expected RestErrorException");
		} catch (final RestErrorException e) {
			final String message = e.getMessage();
			Assert.assertEquals(CloudifyMessageKeys.APPLICATION_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName(), message);
		}
	}


}
