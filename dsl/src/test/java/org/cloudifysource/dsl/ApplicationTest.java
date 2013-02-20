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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;


/***********
 * JUnit test.
 * @author barakme
 *
 */
public class ApplicationTest {

	private static final String SIMPLE_APP_PATH = "testResources/applications/simple/simple-application.groovy";

	/***********
	 * Test DSL parsing of a basic application.
	 * 
	 * @throws Exception .
	 */
	@Test
	public void testSimpleApplication() throws Exception {
		final Application app = ServiceReader.getApplicationFromFile(new File(SIMPLE_APP_PATH)).getApplication();
		assertNotNull(app);
		assertEquals(2, app.getServices().size());
		assertNotNull(app.getServices().get(0).getLifecycle());
		assertNotNull(app.getServices().get(1).getLifecycle());
		assertEquals("icon2.jpg", app.getServices().get(1).getIcon());

	}

}
