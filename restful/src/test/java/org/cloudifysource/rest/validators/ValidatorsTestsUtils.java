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

import org.cloudifysource.rest.controllers.RestErrorException;
import org.junit.Assert;

public final class ValidatorsTestsUtils {

    public static final String FOLDER = "src/test/resources/validators";

	private ValidatorsTestsUtils() {

	}
	
	public static void validate(
			final InstallServiceValidator validator, 
			final InstallServiceValidationContext validationContext, 
			final String exceptionCause) {
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
	
	public static void validate(
			final InstallApplicationValidator validator, 
			final InstallApplicationValidationContext validationContext, 
			final String exceptionCause) {
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
	
	public static void validate(
			final UninstallServiceValidator validator, 
			final UninstallServiceValidationContext validationContext, 
			final String exceptionCause) {
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
	
	public static void validate(
			final UninstallApplicationValidator validator, 
			final UninstallApplicationValidationContext validationContext, 
			final String exceptionCause) {
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
	
	public static void validate(
			final SetServiceInstancesValidator validator, 
			final SetServiceInstancesValidationContext validationContext, 
			final String exceptionCause) {
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
