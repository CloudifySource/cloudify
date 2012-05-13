/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
 ******************************************************************************/
package org.cloudifysource.dsl;

import static org.junit.Assert.fail;

import org.cloudifysource.dsl.internal.DSLValidationException;
import org.junit.Test;

public class ServiceValidationTest {

	@Test
	public void testIllegalNumberOfInstances() {
		//illegal number of instances:
		try {
			Service service = new Service();
			service.setNumInstances(2);
			service.setMaxAllowedInstances(1);
			service.validateDefaultValues();
			fail("an invalid service was successfully validated");
		} catch (DSLValidationException e) {
			
		}
		//no num instances defined. using default values:
		try {
			Service service = new Service();
			service.validateDefaultValues();
		}catch (DSLValidationException e) {
			fail("Validation of service failed");
		}
		//test legal state:
		try {
			Service service = new Service();
			service.setNumInstances(1);
			service.setMaxAllowedInstances(1);
			service.validateDefaultValues();
		}catch (DSLValidationException e) {
			fail("Validation of service failed");
		}
	}
}

