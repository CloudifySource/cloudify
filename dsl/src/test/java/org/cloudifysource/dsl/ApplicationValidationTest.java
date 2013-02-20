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

import java.io.File;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

/*******
 * JUnitTest.
 * 
 * @author yael
 * 
 */
public class ApplicationValidationTest {

	private static final String APPLICATION_WITHOUT_NAME_GROOVY 
	= "testResources/applications/ApplicationValidationTest/appWithoutNameTest";
	private static final String APPLICATION_WITH_EMPTY_NAME_GROOVY 
	= "testResources/applications/ApplicationValidationTest/appWithEmptyNameTest";
	private static final String APPLICATION_WITH_INVALID_NAME_GROOVY 
	= "testResources/applications/ApplicationValidationTest/appWithInvalidNameTest";
	private static final String INVALID_APP_NAME = "my[1]app";

	/*******
	 * Tests the validation of an illegal application's name (application
	 * without a name or with an empty name).
	 * <p>
	 * Should throw <code>DSLValidationException</code>.
	 */
	@Test
	public void testIllgalApplicationName() {
		final Application app = new Application();

		// application without a name
		try {
			app.validateName(new DSLValidationContext());
			Assert.fail("An application without a name was successfully validated.");
		} catch (final DSLValidationException e) {
			// OK - the invalid application name caused the exception
		}

		// application with an empty name
		app.setName(StringUtils.EMPTY);
		try {
			app.validateName(new DSLValidationContext());
			Assert.fail("An application with an empty name was successfully validated.");
		} catch (final DSLValidationException e) {
			// OK - the invalid application name caused the exception
		}
		
		// application with an invalid name
		app.setName(INVALID_APP_NAME);
		try {
			app.validateName(new DSLValidationContext());
			Assert.fail("An application with an invalid name was successfully validated: " + INVALID_APP_NAME);
		} catch (final DSLValidationException e) {
			// OK - the invalid application name caused the exception
		}
	}

	/*******
	 * Tests the validation of an application without a name using DSL parsing
	 * of a groovy file.
	 * <p>
	 * Should throw <code>DSLValidationException</code>.
	 */
	@Test
	public void testEmptyNameGroovyFileValidation() {
		final File applicationFile = new File(
				APPLICATION_WITH_EMPTY_NAME_GROOVY);
		try {
			ServiceReader.getApplicationFromFile(applicationFile)
					.getApplication();
			Assert.fail("Application name is empty, IllegalArgumentException expected.");
		} catch (final IllegalArgumentException e) {
			// OK - the invalid application name caused the exception
		} catch (final Exception e) {
			Assert.fail("Application name is empty, IllegalArgumentException expected, instead "
					+ e.getClass() + " was thrown.");
		}

	}

	/*******
	 * Tests the validation of an application with an empty name using DSL
	 * parsing of a groovy file.
	 * <p>
	 * Should throw <code>DSLValidationException</code>.
	 */
	@Test
	public void testNameNotExistGroovyFileValidation() {
		final File applicationFile = new File(APPLICATION_WITHOUT_NAME_GROOVY);
		try {
			ServiceReader.getApplicationFromFile(applicationFile)
					.getApplication();
			Assert.fail("Application name is missing, IllegalArgumentException expected.");
		} catch (final IllegalArgumentException e) {
			// OK - the invalid application name caused the exception
		} catch (final Exception e) {
			Assert.fail("Application name is missing, IllegalArgumentException expected, instead "
					+ e.getClass() + " was thrown.");
			e.printStackTrace();
		}
	}
	
	/*******
	 * Tests the validation of an application with an invalid name using DSL parsing of a groovy file.
	 * <p>
	 * Should throw <code>DSLValidationException</code>.
	 */
	@Test
	public void testInvalidNameGroovyFileValidation() {
		final File applicationFile = new File(APPLICATION_WITH_INVALID_NAME_GROOVY);
		try {
			ServiceReader.getApplicationFromFile(applicationFile).getApplication();
			Assert.fail("Application name is invalid: " + INVALID_APP_NAME + ". IllegalArgumentException expected.");
		} catch (final IllegalArgumentException e) {
			// OK - the invalid application name caused the exception
		} catch (final Exception e) {
			Assert.fail("Application name is invalid: " + INVALID_APP_NAME +". IllegalArgumentException expected, " +
					"but " + e.getClass() + " was thrown instead.");
			e.printStackTrace();
		}
	}
}
