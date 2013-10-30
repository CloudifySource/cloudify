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
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ValidateCloudOverridesFileSizeTest {

	private File cloudOverridesFile;
    private ValidateCloudOverridesFileSize validator;
    private static final long TEST_FILE_SIZE_LIMIT = 3;
    private static final String ERR_MSG = CloudifyMessageKeys.CLOUD_OVERRIDES_SIZE_LIMIT_EXCEEDED.getName();

    
    @Before
    public void init() {
        validator = new ValidateCloudOverridesFileSize();
        validator.setCloudOverridesFileSizeLimit(TEST_FILE_SIZE_LIMIT);
    	try {
    		cloudOverridesFile = File.createTempFile("cloudOverrides", "");
			FileUtils.writeStringToFile(cloudOverridesFile, "I'm longer than 3 bytes !");
		} catch (IOException e) {
			Assert.fail(e.getLocalizedMessage());
		}
    }

    @Test
    public void testSizeLimitExeededInInstallService() {
    	InstallServiceValidationContext context = new InstallServiceValidationContext();
    	context.setCloudOverridesFile(cloudOverridesFile);
    	ValidatorsTestsUtils.validate(validator, context, ERR_MSG);
    }
    
    @Test
    public void testSizeLimitExeededInInstallApplication() {
    	InstallApplicationValidationContext context = new InstallApplicationValidationContext();
    	context.setCloudOverridesFile(cloudOverridesFile);
    	ValidatorsTestsUtils.validate(validator, context, ERR_MSG);
    }
    
    
    @After
    public void deleteFile() {
    	cloudOverridesFile.delete();    	
    }


}
