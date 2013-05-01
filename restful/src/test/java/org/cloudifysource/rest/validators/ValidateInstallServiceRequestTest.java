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

import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.junit.Test;

public class ValidateInstallServiceRequestTest extends InstallServiceValidatorTest {
	
	private static final String UPLOAD_KEY = "key";
	private static final String CLOUD_CONFIG_UPLOAD_KEY = "cloud_config_key";
	private static final String AUTH_GROUP = "";
	private static final boolean SELF_HEALING = true;
	private static final String CLOUD_OVERRIDES = "";
	private static final String SERVICE_OVERRIDES = "";
	private static final Long OVERRIDES_MAX_LENGTH = new Long(3);
	

	@Test
	public void testMissingUploadKey() {
		final InstallServiceRequest request = buildRequest(
				null /* uploadKey */, CLOUD_CONFIG_UPLOAD_KEY /* cloudConfigUploadKey */, 
				AUTH_GROUP /* authGroups */, SELF_HEALING /* selfHealing */, 
				CLOUD_OVERRIDES /* cloudOverrides */, SERVICE_OVERRIDES /* serviceOverrides */);
		testValidator(request, CloudifyMessageKeys.UPLOAD_KEY_PARAMETER_MISSING);
	}
	
	@Test
	public void testCloudOverridesMaxLengthExceeded() {
		final InstallServiceRequest request = buildRequest(
				UPLOAD_KEY /* uploadKey */, CLOUD_CONFIG_UPLOAD_KEY /* cloudConfigUploadKey */, 
				AUTH_GROUP /* authGroups */, SELF_HEALING /* selfHealing */, 
				"a = b" /* cloudOverrides */, SERVICE_OVERRIDES /* serviceOverrides */);
		testValidator(request, CloudifyMessageKeys.OVERRIDES_LENGTH_LIMIT_EXCEEDED, OVERRIDES_MAX_LENGTH);
	}
	
	@Test
	public void testServiceOverridesMaxLengthExceeded() {
		final InstallServiceRequest request = buildRequest(
				UPLOAD_KEY /* uploadKey */, CLOUD_CONFIG_UPLOAD_KEY /* cloudConfigUploadKey */, 
				AUTH_GROUP /* authGroups */, SELF_HEALING /* selfHealing */, 
				CLOUD_OVERRIDES /* cloudOverrides */, "a = b" /* serviceOverrides */);
		testValidator(request, CloudifyMessageKeys.OVERRIDES_LENGTH_LIMIT_EXCEEDED, OVERRIDES_MAX_LENGTH);
	}

	private InstallServiceRequest buildRequest(
			final String uploadKey, final String cloudConfigUploadKey, final String authGroups,
			final boolean selfHealing, final String cloudOverrides,
			final String serviceOverrides) {
		final InstallServiceRequest request = new InstallServiceRequest();
		request.setAuthGroups(authGroups);
		request.setCloudOverrides(cloudOverrides);
		request.setSelfHealing(selfHealing);
		request.setServiceOverrides(serviceOverrides);
		request.setUploadKey(uploadKey);
		request.setCloudConfigurationUploadKey(cloudConfigUploadKey);
		return request;
	}

	@Override
	public InstallServiceValidator getValidatorInstance(final Object optionalValidatorParam) {
		ValidateInstallServiceRequest validator = new ValidateInstallServiceRequest();
		if (optionalValidatorParam != null) {
			validator.setOverrdiesMaxLength((Long) optionalValidatorParam);
		}
		return validator;
	}

}
