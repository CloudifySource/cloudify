/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl;

import java.io.File;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

/******
 * Tests for parsing of DSL files using the 'load' method to read DSL elements from external files.
 * 
 * @author barakme
 * 
 */
public class ServiceWithExternalFileLoadingTest {

	private static final String RECIPE_PATH = "testResources/testparsing/externalLoad/test_loading.groovy";
	private static final String RECIPE_PATH_MISSING_FILE =
			"testResources/testparsing/externalLoad/test_loading_error.groovy";

	@Test
	public void testServiceParser()
			throws Exception {
		Service service = ServiceReader.readService(new File(RECIPE_PATH));
		Assert.assertNotNull("lifecycle loaded from external file should not be null", service.getLifecycle());
		Assert.assertNotNull("unexpected field value in lifecycle", service.getLifecycle().getInit());
		Assert.assertNotNull("unexpected field value in lifecycle", service.getLifecycle().getStart());
		Assert.assertNotNull("userinterface loaded from external file should not be null", service.getUserInterface());
		Assert.assertEquals("unexpected value in UI field", 1, service.getUserInterface().getMetricGroups().size());
		Assert.assertEquals("unexpected value in UI field", 2, service.getUserInterface().getWidgetGroups().size());

	}

	@Test
	public void testServiceParserWithMissingFile()
			throws Exception {
		try {
			ServiceReader.readService(new File(RECIPE_PATH_MISSING_FILE));
			Assert.fail("Service parsing succeeded but it should have failed");
		} catch (Exception e) {
			Assert.assertTrue("Incorrent error message in parse error of service with missing external file", e
					.getMessage().contains("ui_MISSING.groovy"));
		}

	}

}
