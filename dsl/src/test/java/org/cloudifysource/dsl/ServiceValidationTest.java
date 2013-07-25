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
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.internal.validators.ServiceValidator;
import org.junit.Test;
import org.openspaces.ui.Unit;
import org.openspaces.ui.UserInterface;

/**
 * Test Service DSL Validations.
 * @author noak, adaml
 *
 */
public class ServiceValidationTest {

	private static final String SERVICE_WITHOUT_ICON_PATH = "testResources/simple/simple-service.groovy";
	
	private static final String SERVICE_WITH_ICON_PATH = 
			"testResources/applications/simple/service1/service1-service.groovy";
	
	private static final String APPLICATION_MISSING_SERVICE_ICON_PATH = 
			"testResources/applications/ApplicationValidationTest/appMissingServiceIconTest";

    private static final String MULTI_TENANT_WITH_STATIC_STORAGE =
            "testResources/applications/ServiceValidationTest/multitenantWithStaticStorage";

    private static final String ICON_FILE = "icon.png";

	private static final String SERVICE_WITHOUT_NAME_GROOVY 
	= "testResources/applications/ServiceValidationTest/serviceWithoutNameTest";
	
	private static final String SERVICE_WITH_EMPTY_NAME_GROOVY 
	= "testResources/applications/ServiceValidationTest/serviceWithEmptyNameTest";
	
	private static final String SERVICE_WITH_INVALID_NAME_GROOVY 
	= "testResources/applications/ServiceValidationTest/serviceWithInvalidNameTest";

	private static final String SERVICE_WITH_VALID_USER_INTERFACE = 
			"src/test/resources/ExternalDSLFiles/userInterfaceConversionTestFiles/" 
			+ "service_with_metrics_and_widgets.groovy";

	private static final String SERVICE_WITH_INVALID_USER_INTERFACE = 
			"src/test/resources/groovyFileValidation/badUserInterface.groovy";
	
	private static final String INVALID_SERVICE_NAME = "my[1]service";

    @Test
    public void testStaticStorageMTWithNumInstances() {

        try {
            ServiceReader.readService(new File(MULTI_TENANT_WITH_STATIC_STORAGE));
            Assert.fail("Multi-tenant with numInstances > 1 and static storage is illegal, DSLValidationException expected.");
        } catch (DSLValidationException e) {
            return;
            //OK - the invalid service name caused the exception
        } catch (Exception e) {
            Assert.fail("Multi-tenant with numInstances > 1 and static storage is defined but no DSLValidationException was thrown : " + e.getMessage());
        }
        Assert.fail("Multi-tenant with numInstances > 1 and static storage is defined but no DSLValidationException was thrown.");
    }

	/**
	 * Triple-test for the instances number (invalid configuration, default configuration and a valid configuration).
	 */
	@Test
	public void testIllegalNumberOfInstances() {
		//illegal number of instances:
		try {
			Service service = new Service();
			ServiceValidator serviceValidator = new ServiceValidator();
			serviceValidator.setDSLEntity(service);
			service.setNumInstances(2);
			service.setMaxAllowedInstances(1);
			serviceValidator.validateDefaultValues(new DSLValidationContext());
			fail("an invalid service was successfully validated");
		} catch (DSLValidationException e) {
			//OK - the invalid number of instances caused the exception
		}

		//no num instances defined. using default values:
		try {
			Service service = new Service();
			ServiceValidator serviceValidator = new ServiceValidator();
			serviceValidator.setDSLEntity(service);
			service.setType("WEB_SERVER");
			serviceValidator.validateDefaultValues(new DSLValidationContext());
		} catch (DSLValidationException e) {
			fail("Validation of service failed");
		}

		//test legal state:
		try {
			Service service = new Service();
			ServiceValidator serviceValidator = new ServiceValidator();
			serviceValidator.setDSLEntity(service);
			service.setNumInstances(1);
			service.setMaxAllowedInstances(1);
			service.setType("WEB_SERVER");
			serviceValidator.validateDefaultValues(new DSLValidationContext());
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
			ServiceValidator serviceValidator = new ServiceValidator();
			serviceValidator.setDSLEntity(service);
			service.setType("NonExistionType");
			serviceValidator.validateDefaultValues(new DSLValidationContext());
			fail("an invalid service was successfully validated");
		} catch (DSLValidationException e) {
			//OK - the invalid service type caused the exception
		}

		//valid service type
		try {
			Service service = new Service();
			ServiceValidator serviceValidator = new ServiceValidator();
			serviceValidator.setDSLEntity(service);
			service.setType("WEB_SERVER");
			serviceValidator.validateDefaultValues(new DSLValidationContext());
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
		ServiceValidator serviceValidator = new ServiceValidator();
		serviceValidator.setDSLEntity(service);
		try {
			serviceValidator.validateName(new DSLValidationContext());
			fail("A service without a name was successfully validated");
		} catch (DSLValidationException e) {
			//OK - the invalid service name caused the exception
		}

		try {
			service.setName(StringUtils.EMPTY);
			serviceValidator.validateName(new DSLValidationContext());
			fail("A service with an empty name was successfully validated");
		} catch (DSLValidationException e) {
			//OK - the invalid service name caused the exception
		}
		
		try {
			service.setName(INVALID_SERVICE_NAME);
			serviceValidator.validateName(new DSLValidationContext());
			fail("A service with an invalid name was successfully validated");
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
	
	/*******
	 * Tests the validation of a service with an invalid name using DSL parsing of a groovy file.
	 * <p>Should throw <code>DSLValidationException</code>
	 */
	@Test
	public void testServiceWithInvalidNameGroovy() {
		try {
			ServiceReader.readService(new File(SERVICE_WITH_INVALID_NAME_GROOVY));
			Assert.fail("Service name is invalid: " + INVALID_SERVICE_NAME + ". DSLValidationException expected.");
		} catch (DSLValidationException e) {
			//OK - the invalid service name caused the exception
		} catch (Exception e) {
			Assert.fail("Service name is invalid: " + INVALID_SERVICE_NAME + ". DSLValidationException expected, "
					+ "but " + e.getClass() + " was thrown instead.");
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
		ServiceValidator serviceValidator = new ServiceValidator();
		serviceValidator.setDSLEntity(service);
		service.setIcon(ICON_FILE);
		DSLValidationContext validationContext = new DSLValidationContext();

		//missing icon file:
		try {
			validationContext.setFilePath(SERVICE_WITHOUT_ICON_PATH);
			serviceValidator.validateIcon(validationContext);
			fail("an invalid icon path was successfully validated: " + SERVICE_WITHOUT_ICON_PATH);
		} catch (DSLValidationException e) {
			//OK - the invalid icon path caused the exception
		}

		//valid icon file:
		try {
			validationContext.setFilePath(SERVICE_WITH_ICON_PATH);
			serviceValidator.validateIcon(validationContext);
		} catch (DSLValidationException e) {
			fail("Validation of service failed on a valid icon path: " 
					+ SERVICE_WITH_ICON_PATH + ": " + e.getMessage());
		}
	}

	@Test
	public void testBadUserInterfaceDef() 
			throws PackagingException, DSLException {

		Service service = ServiceReader.readService(new File(SERVICE_WITH_VALID_USER_INTERFACE));
		ServiceValidator serviceValidator = new ServiceValidator();
		serviceValidator.setDSLEntity(service);
		DSLValidationContext validationContext = new DSLValidationContext();
		validationContext.setFilePath(SERVICE_WITH_VALID_USER_INTERFACE);
		try {
			serviceValidator.validateUserInterfaceObjectIsWellDefined(validationContext);
		} catch (DSLValidationException e) {
			fail("Validation of a valid User Interface object failed");
		}
		validationContext.setFilePath(SERVICE_WITH_INVALID_USER_INTERFACE);
		UserInterface userInterface = service.getUserInterface();
		
		//we change the UserInterface object a few times and run a validation test on it.

		//invalid because expecting string not object
		List<Object> firstInvalidMetricList = new ArrayList<Object>();
		firstInvalidMetricList.add(new Object());
		userInterface.getMetricGroups().get(0).setMetrics(firstInvalidMetricList);
		try {
			serviceValidator.validateUserInterfaceObjectIsWellDefined(validationContext);
			fail("Validation of User Interface object is expected to fail");
		} catch (DSLValidationException e) {
			//expected
		}

		//invalid because expecting Unit not String 
		List<Object> secondInvalidMetricList = new ArrayList<Object>();
		List<Object> invalidMetricListForm = new ArrayList<Object>();
		invalidMetricListForm.add("metricName");
		invalidMetricListForm.add("nonUnit instance type object");
		secondInvalidMetricList.add(invalidMetricListForm);
		userInterface.getMetricGroups().get(0).setMetrics(secondInvalidMetricList);
		try {
			serviceValidator.validateUserInterfaceObjectIsWellDefined(validationContext);
			fail("Validation of User Interface object is expected to fail");
		} catch (DSLValidationException e) {
			//expected
		}

		//invalid because expecting string not object
		List<Object> thirdInvalidMetricList = new ArrayList<Object>();
		invalidMetricListForm = new ArrayList<Object>();
		invalidMetricListForm.add(new Object());
		invalidMetricListForm.add(Unit.PERCENTAGE);
		thirdInvalidMetricList.add(invalidMetricListForm);
		userInterface.getMetricGroups().get(0).setMetrics(thirdInvalidMetricList);
		try {
			serviceValidator.validateUserInterfaceObjectIsWellDefined(validationContext);
			fail("Validation of User Interface object is expected to fail");
		} catch (DSLValidationException e) {
			//expected
		}

		//invalid because expecting metric to be either string or a list 
		//of size 2
		List<Object> fourthInvalidMetricList = new ArrayList<Object>();
		invalidMetricListForm = new ArrayList<Object>();
		invalidMetricListForm.add("metricName");
		invalidMetricListForm.add(Unit.PERCENTAGE);
		invalidMetricListForm.add("some unrelatedString");
		fourthInvalidMetricList.add(invalidMetricListForm);
		userInterface.getMetricGroups().get(0).setMetrics(fourthInvalidMetricList);
		try {
			serviceValidator.validateUserInterfaceObjectIsWellDefined(validationContext);
			fail("Validation of User Interface object is expected to fail");
		} catch (DSLValidationException e) {
			//expected
		}
	}

}

