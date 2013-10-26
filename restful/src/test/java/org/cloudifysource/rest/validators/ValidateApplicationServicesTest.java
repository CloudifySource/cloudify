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

import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

public class ValidateApplicationServicesTest extends InstallApplicationValidatorTest {
    private static final String FOLDER = 
    		"src" + File.separator + "test" + File.separator + "resources" + File.separator + "validators";
    private static final String CLOUD_FILE_PATH = 
    		FOLDER  + File.separator + "byon" + File.separator + "byon-cloud.groovy";
    private static final String NO_COMPUTE_APP = FOLDER + File.separator + "simpleApp";
    private static final String NOT_EXIST_TEMPLATE_APP = FOLDER + File.separator + "simpleAppTemplateNotExist";

    @Test
    public void testMissingTemplate() throws IOException, DSLException {
        Cloud cloud = ServiceReader.readCloud(new File(CLOUD_FILE_PATH));
        setCloud(cloud);
        Application application = 
        		ServiceReader.getApplicationFromFile(new File(NOT_EXIST_TEMPLATE_APP)).getApplication();
        setApplication(application);
        setExceptionCause(CloudifyMessageKeys.MISSING_TEMPLATE.getName());
        testValidator();
    }

    @Test
    public void testNullCompute() throws DSLException, IOException {
        Application application = ServiceReader.getApplicationFromFile(new File(NO_COMPUTE_APP)).getApplication();
        setApplication(application);
        testValidator();
    }
	
	@Override
	public InstallApplicationValidator getValidatorInstance() {
		return new ValidateApplicationServices();
	}


}
