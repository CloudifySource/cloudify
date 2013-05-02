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
import java.io.FileWriter;
import java.io.IOException;

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.junit.Before;
import org.junit.Test;


public class ValidateCloudOverridesSizeTest extends InstallServiceValidatorTest {

	private ValidateCloudOverridesSize validator;
	private static final long CLOUD_OVERRIDES_TEST_SIZE_LIMIT_KB = 1;
	private static final String CLOUD_OVERRIDES_PATH = "src/test/resources/validators/cloudOverrides";

	@Before
	public void initValidator() {
		validator = new ValidateCloudOverridesSize();
	}
	
	@Test
	public void test() throws IOException {
		validator.setCloudOverridesSizeLimit(CLOUD_OVERRIDES_TEST_SIZE_LIMIT_KB);
		File cloudOverridesFile = new File(CLOUD_OVERRIDES_PATH);
		cloudOverridesFile.createNewFile();
		FileWriter writer = new FileWriter(cloudOverridesFile);
		for (int i = 0; i < CLOUD_OVERRIDES_TEST_SIZE_LIMIT_KB * 200; i++) {
			writer.write("key" + i + "=" + i);
		}
		writer.close();
		cloudOverridesFile.deleteOnExit();
		testValidator(cloudOverridesFile, CloudifyMessageKeys.CLOUD_OVERRIDES_SIZE_LIMIT_EXCEEDED);
	}
	
	@Override
	public InstallServiceValidator getValidatorInstance() {
		return validator;
	}

}
