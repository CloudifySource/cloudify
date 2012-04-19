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
package org.cloudifysource.recipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;


public class ApplicationSetUpTest {

	private final static String LEGAL_RESOURCES_PATH = "target/classes/recpies/apps/";
	private String travelAppName = "travel";
	private File appDslFile;
	private Application application;
	static final List<String> EXPECTED_SERVICE_NAMES = Arrays
			.asList(new String[] { "cassandra", "tomcat" });

	//The target application groovy file path does not match the path cassendra service used in
	//Travel under the GS_HOME dir. ()becase path is relative to the target folder therefore the cassandra service file is not found.
	//TODO: fix this test.
	@Test
	public void travelApplication() throws Exception {
//		appDslFile = new File(LEGAL_RESOURCES_PATH
//				+ "/travel/travel-application.groovy");
//		application = ServiceReader.getApplicationFromFile(appDslFile)
//				.getApplication();
//		assertNotNull(application);
//		assertTrue("Application name isn't correct", application.getName()
//				.compareTo(travelAppName) == 0);
//		// List<String> serviceNames = application.getServices();
//		// assertTrue("Service names are not as expected" ,
//		// serviceNames.equals(EXPECTED_SERVICE_NAMES));
//		List<Service> services = application.getServices();
//		assertNotNull("The services are null", services);
//
//		Iterator<String> nameIter = EXPECTED_SERVICE_NAMES.iterator();
//		// assertEquals("services and serviceNames are of different length" ,
//		// services.size(), serviceNames.size());
//		for (Service service : services) {
//			ServiceTestUtil.validateName(service, nameIter.next());
//			ServiceTestUtil.validateIcon(service);
//		}
	}


	@Test
	public void simpleApplication() throws Exception {
		appDslFile = new File(LEGAL_RESOURCES_PATH
				+ "simple/simple-application.groovy");
		
		application = ServiceReader.getApplicationFromFile(appDslFile).getApplication();
		assertNotNull(application);
		assertTrue("Application name isn't correct", application.getName()
				.compareTo("simple") == 0);

		//String simpleServiceName = application.getServiceNames().get(0);
		
		assertEquals(1, application.getServices().size());
		
		Service simpleService = application.getServices().get(0);		
		ServiceTestUtil.validateName(simpleService, "simple");		
		ServiceTestUtil.validateIcon(simpleService);
	}
}
