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

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.junit.Assert;

/**
 *
 * @author yael
 *
 */
public abstract class InstallServiceValidatorTest {

    public abstract InstallServiceValidator getValidatorInstance();

    public void testValidator(final InstallServiceRequest request, final Cloud cloud, final Service service,
                              final String templateName, final File cloudOverridesFile, final File serviceOverridesFile,
                              final File cloudConfigurationFile, final String exceptionCause) {

        final InstallServiceValidator validator = getValidatorInstance();
        InstallServiceValidationContext validationContext = new InstallServiceValidationContext();
        validationContext.setRequest(request);
        validationContext.setCloud(cloud);
        validationContext.setService(service);
        validationContext.setTemplateName(templateName);
        validationContext.setCloudOverridesFile(cloudOverridesFile);
        validationContext.setServiceOverridesFile(serviceOverridesFile);
        validationContext.setCloudConfigurationFile(cloudConfigurationFile);
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

    public void testValidator(final InstallServiceRequest request, final Cloud cloud, final Service service,
                              final String exceptionCause) {
        testValidator(request, cloud, service, null, null, null, null, exceptionCause);
    }

    public void testValidator(final InstallServiceRequest request, final String exceptionCause) {
        testValidator(request, null, null, null, null, null, null, exceptionCause);
    }

    public void testValidator(final Cloud cloud, final Service service, final String exceptionCause) {
        testValidator(null, cloud, service, null, null, null, null, exceptionCause);
    }

    public void testValidator(final Cloud cloud, final Service service, final String templateName,
                              final String exceptionCause) {
        testValidator(null, cloud, service, templateName, null, null, null, exceptionCause);
    }

}
