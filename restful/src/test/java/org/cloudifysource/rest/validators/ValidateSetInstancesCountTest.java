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

import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.request.SetServiceInstancesRequest;
import org.junit.Before;
import org.junit.Test;

public class ValidateSetInstancesCountTest {

	private static final String ERR_MSG = CloudifyErrorMessages.INVALID_INSTANCES_COUNT.getName();

	private SetServiceInstancesValidator validator;
	
    @Before
    public void init() {
        validator = new ValidateSetInstancesCount();
    }
    
	@Test
	public void zeroCountInSetInstances() {
		SetServiceInstancesValidationContext context = new SetServiceInstancesValidationContext();
		SetServiceInstancesRequest request = new SetServiceInstancesRequest();
		request.setCount(0);
		context.setRequest(request);
		SetServiceInstancesValidator validator = new ValidateSetInstancesCount();
		ValidatorsTestsUtils.validate(validator , context, ERR_MSG);
	}
	
	@Test
	public void negativeCountInSetInstances() {
		SetServiceInstancesValidationContext context = new SetServiceInstancesValidationContext();
		SetServiceInstancesRequest request = new SetServiceInstancesRequest();
		request.setCount(-1);
		context.setRequest(request);
		ValidatorsTestsUtils.validate(validator, context, ERR_MSG);
	}
}
