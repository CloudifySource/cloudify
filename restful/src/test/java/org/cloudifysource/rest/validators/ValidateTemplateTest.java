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

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.junit.Test;

public class ValidateTemplateTest extends InstallServiceValidatorTest {

    private static final String FOLDER = "src/test/resources/validators";
    private static final String CLOUD_FILE_PATH = FOLDER + "/byon/byon-cloud.groovy";
    private static final String NO_COMPUTE_SERVICE = FOLDER + "/simple.groovy";
    private static final String NOT_EXIST_TEMPLATE_SERVICE_GROOVY = FOLDER + "/template5service.groovy";

    @Test
    public void testMissingTemplate() throws IOException, DSLException, PackagingException {
        Cloud cloud = ServiceReader.readCloud(new File(CLOUD_FILE_PATH));
        Service service = ServiceReader.readService(new File(NOT_EXIST_TEMPLATE_SERVICE_GROOVY));
        testValidator(cloud, service, service.getCompute().getTemplate(),
                CloudifyMessageKeys.MISSING_TEMPLATE.getName());
    }

    @Test
    public void testNullCompute() throws IOException, DSLException, PackagingException {
        Service service = ServiceReader.readService(new File(NO_COMPUTE_SERVICE));
        testValidator(null, service, null);
    }

    @Override
    public InstallServiceValidator getValidatorInstance() {
        return new ValidateTemplate();
    }

}
