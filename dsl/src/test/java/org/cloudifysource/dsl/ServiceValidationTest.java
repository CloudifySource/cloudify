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

import java.io.File;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.junit.Test;

/**
 * Test Service DSL Validations.
 * @author noak
 *
 */
public class ServiceValidationTest {

	private static final String SERVICE_WITHOUT_ICON_PATH = "testResources/simple/simple-service.groovy";
	private static final String SERVICE_WITH_ICON_PATH = "testResources/applications/simple/service1/service1-service.groovy";
	private static final String APPLICATION_MISSING_SERVICE_ICON_PATH = "testResources/applications/ApplicationValidationTest/appMissingServiceIconTest";

	private static final String ICON_FILE = "icon.png";
	
	private static final String SERVICE_WITHOUT_NAME_GROOVY 
	= "testResources/applications/ServiceValidationTest/serviceWithoutNameTest";
	private static final String SERVICE_WITH_EMPTY_NAME_GROOVY 
	= "testResources/applications/ServiceValidationTest/serviceWithEmptyNameTest";
	
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
	
	/*******
	 * Tests the validation of an illegal service's name 
	 * (service without a name or with an empty name).
	 * <p>Should throw <code>DSLValidationException</code>.
	 */
	@Test
	public void testIllegalServiceName() {
		Service service = new Service();
		try {
			service.validateNameExists(new DSLValidationContext());
			fail("A service without a name was successfully validated");
		} catch (DSLValidationException e) {
			//OK - the invalid service name caused the exception
		}
		
		try {
			service.setName(StringUtils.EMPTY);
			service.validateNameExists(new DSLValidationContext());
			fail("A service with an empty name was successfully validated");
		} catch (DSLValidationException e) {
			//OK - the invalid service name caused the exception
		}
	}
	
	/*******
	 * Tests the validation of a service without a name using DSL parsing of a groovy file.
	 * <p>Should throw <code>DSLValidationException</code>.
	 */
	@Test
	public void testServiceWithoutNameGroovy() {
		try {
			ServiceReader.readService(new File(SERVICE_WITHOUT_NAME_GROOVY));
			Assert.fail("Service name is missing, DSLValidationException expected."); 
		} catch (DSLValidationException e) {
			//OK - the invalid service name caused the exception
		} catch (Exception e) {
			Assert.fail("Service name is missing, DSLValidationException expected, instead " 
					+ e.getClass() + " was thrown.");
		}
	}

	/*******
	 * Tests the validation of a service without a name using DSL parsing of a groovy file.
	 * <p>Should throw <code>DSLValidationException</code>
	 */
	@Test
	public void testServiceWithEmptyNameGroovy() {
		try {
			ServiceReader.readService(new File(SERVICE_WITH_EMPTY_NAME_GROOVY));
			Assert.fail("Service name is empty, DSLValidationException expected.");
		} catch (DSLValidationException e) {
			//OK - the invalid service name caused the exception
		} catch (Exception e) {
			Assert.fail("Service name is empty, DSLValidationException expected, instead " 
					+ e.getClass() + " was thrown.");
		}
	}
	
	@Test
	public void testApplicationMissingServiceIcon() {
		final File applicationFile = new File(APPLICATION_MISSING_SERVICE_ICON_PATH);
		try {
			ServiceReader.getApplicationFromFile(applicationFile).getApplication();
			Assert.fail("Application has a service without an icon, IllegalArgumentException expected.");
		} catch (final IllegalArgumentException e) {
			// OK - the invalid application name caused the exception
		} catch (final Exception e) {
			Assert.fail("Application has a service without an icon, IllegalArgumentException expected, instead "
					+ e.getClass() + " was thrown.");
		}
	}
	
	@Test
	public void testServiceWithoutIconGroovy() {
		try {
			ServiceReader.readService(new File(SERVICE_WITHOUT_ICON_PATH));
			Assert.fail("Service name is empty, DSLValidationException expected.");
		} catch (PackagingException e) {
			//OK - the invalid service name caused the exception
		} catch (Exception e) {
			Assert.fail("Service name is empty, DSLValidationException expected, instead " 
					+ e.getClass() + " was thrown.");
		}
	}
	
	/**
	 * Double-test for the service icon.
	 */
	
	@Test
	public void testMissingServiceIcon() {
		
		Service service = new Service();
		service.setIcon(ICON_FILE);
		DSLValidationContext validationContext = new DSLValidationContext();
		
		//missing icon file:
		try {
			validationContext.setFilePath(SERVICE_WITHOUT_ICON_PATH);
			service.validateIcon(validationContext);
			fail("an invalid icon path was successfully validated: " + SERVICE_WITHOUT_ICON_PATH);
		} catch (DSLValidationException e) {
			//OK - the invalid icon path caused the exception
		}
		
		//valid icon file:
		try {
			validationContext.setFilePath(SERVICE_WITH_ICON_PATH);
			service.validateIcon(validationContext);
		} catch (DSLValidationException e) {
			fail("Validation of service failed on a valid icon path: " + SERVICE_WITH_ICON_PATH);
		}
	}
	
}

