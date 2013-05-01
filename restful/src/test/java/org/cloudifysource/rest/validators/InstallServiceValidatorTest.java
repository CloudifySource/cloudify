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

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.security.CustomPermissionEvaluator;
import org.junit.Assert;

/**
 * 
 * @author yael
 * 
 */
public abstract class InstallServiceValidatorTest {
	
	public abstract InstallServiceValidator getValidatorInstance(Object optionalValidatorParam);
	
	public void testValidator(final InstallServiceRequest request, final Cloud cloud, final Service service,
			final boolean shouldFail, final CloudifyMessageKeys exceptionCause, 
			final CustomPermissionEvaluator permissionEvaluator, final Object optionalValidatorParam) {
		
		final InstallServiceValidator validator = getValidatorInstance(optionalValidatorParam);
		InstallServiceValidationContext validationContext = new InstallServiceValidationContext();
		validationContext.setRequest(request);
		validationContext.setCloud(cloud);
		validationContext.setService(service);
		try {
			validator.validate(validationContext);
			if (shouldFail) {
				Assert.fail(exceptionCause + " didn't yield the expected RestErrorException.");
			}
		} catch (final RestErrorException e) {
			if (!shouldFail) {
				e.printStackTrace();
				Assert.fail();
			}
			Assert.assertEquals(exceptionCause.getName(), e.getMessage());
		}
	}

	public void testValidator(final InstallServiceRequest request, final Cloud cloud, final Service service,
			final boolean shouldFail, final CloudifyMessageKeys exceptionCause) {
		testValidator(request, cloud, service, shouldFail, exceptionCause, null, null);
	}
	
	public void testValidator(final InstallServiceRequest request, final CloudifyMessageKeys exceptionCause) {
		testValidator(request, null, null, true, exceptionCause);
	}
	
	public void testValidator(final InstallServiceRequest request, final CloudifyMessageKeys exceptionCause, 
			final Object optionalValidatorParam) {
		testValidator(request, null, null, true, exceptionCause, null, optionalValidatorParam);
	}

	public void testValidator(final Cloud cloud, final Service service, final CloudifyMessageKeys exceptionCause) {
		testValidator(null, cloud, service, true, exceptionCause);
	}
	
	public void testValidator(final Cloud cloud, final Service service, 
			final boolean shouldFail, final CloudifyMessageKeys exceptionCause) {
		testValidator(null, cloud, service, shouldFail, exceptionCause);
	}
	
}
