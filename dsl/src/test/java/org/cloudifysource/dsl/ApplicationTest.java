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

import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

import static org.junit.Assert.*;

public class ApplicationTest {

	private final static String SIMPLE_APP_PATH = "testResources/applications/simple/simple-application.groovy";
	
	@Test
	public void testSimpleApplication() throws Exception {
		Application app = ServiceReader.getApplicationFromFile(new File(SIMPLE_APP_PATH)).getApplication();
		assertNotNull(app);
		assertEquals(2, app.getServices().size());
		assertNotNull(app.getServices().get(0).getLifecycle());
		assertNotNull(app.getServices().get(1).getLifecycle());
		assertEquals("icon2.jpg", app.getServices().get(1).getIcon());
		
	}

	
}
