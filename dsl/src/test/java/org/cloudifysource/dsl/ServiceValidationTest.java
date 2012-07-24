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

import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test Service DSL Validations.
 * @author noak
 *
 */
public class ServiceValidationTest {

	private static final String VALID_DSL_PATH = "testResources/applications/simple/mydsl.groovy";
	private static final String INVALID_DSL_PATH = "testResources/applications/mydsl.groovy";
	private static final String ICON_FILE = "icon.png";
	
	/**
	 * Triple-test for the instances number (invalid configuration, default configuration and a valid configuration).
	 */
	@Test
	public void testIllegalNumberOfInstances() {
		//illegal number of instances:
		try {
			Service service = new Service();
			service.setNumInstances(2);
			service.setMaxAllowedInstances(1);
			service.validateDefaultValues(new DSLValidationContext());
			fail("an invalid service was successfully validated");
		} catch (DSLValidationException e) {
			//OK - the invalid number of instances caused the exception
		}
		
		//no num instances defined. using default values:
		try {
			Service service = new Service();
			service.setType("WEB_SERVER");
			service.validateDefaultValues(new DSLValidationContext());
		} catch (DSLValidationException e) {
			fail("Validation of service failed");
		}
		
		//test legal state:
		try {
			Service service = new Service();
			service.setNumInstances(1);
			service.setMaxAllowedInstances(1);
			service.setType("WEB_SERVER");
			service.validateDefaultValues(new DSLValidationContext());
		} catch (DSLValidationException e) {
			fail("Validation of service failed");
		}
	}
	
	/**
	 * Double-test for the service type.
	 */
	@Test
	public void testIllegalServiceType() {
		//invalid service type
		try {
			Service service = new Service();
			service.setType("NonExistionType");
			service.validateDefaultValues(new DSLValidationContext());
			fail("an invalid service was successfully validated");
		} catch (DSLValidationException e) {
			//OK - the invalid service type caused the exception
		}
		
		//valid service type
		try {
			Service service = new Service();
			service.setType("WEB_SERVER");
			service.validateDefaultValues(new DSLValidationContext());
		} catch (DSLValidationException e) {
			fail("Validation of service failed");
		}
	}
	
	/**
	 * Double-test for the service icon.
	 */
	@Ignore
	@Test
	public void testMissingServiceIcon() {
		
		Service service = new Service();
		service.setIcon(ICON_FILE);
		DSLValidationContext validationContext = new DSLValidationContext();
		
		
		//missing icon file:
		try {
			validationContext.setFilePath(INVALID_DSL_PATH);
			service.validateIcon(validationContext);
			fail("an invalid icon path was successfully validated: " + INVALID_DSL_PATH);
		} catch (DSLValidationException e) {
			//OK - the invalid icon path caused the exception
		}
		
		//valid icon file:
		try {
			validationContext.setFilePath(VALID_DSL_PATH);
			service.validateIcon(validationContext);
		} catch (DSLValidationException e) {
			fail("Validation of service failed on a valid icon path: " + VALID_DSL_PATH);
		}
	}
	
}

