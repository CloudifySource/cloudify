package com.gigaspaces.cloudify.recipes;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.gigaspaces.cloudify.dsl.Application;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;

public class ApplicationSetUpTest {

	private final static String LEGAL_RESOURCES_PATH = "target/classes/applications/";
	private String travelAppName = "travel";
	private File appDslFile;
	private Application application;
	static final List<String> EXPECTED_SERVICE_NAMES = Arrays
			.asList(new String[] { "cassandra", "tomcat" });

	@Test
	public void travelApplication() throws Exception {
		appDslFile = new File(LEGAL_RESOURCES_PATH
				+ "/travel/travel-application.groovy");
		application = ServiceReader.getApplicationFromFile(appDslFile)
				.getApplication();
		assertNotNull(application);
		assertTrue("Application name isn't correct", application.getName()
				.compareTo(travelAppName) == 0);
		// List<String> serviceNames = application.getServices();
		// assertTrue("Service names are not as expected" ,
		// serviceNames.equals(EXPECTED_SERVICE_NAMES));
		List<Service> services = application.getServices();
		assertNotNull("The services are null", services);

		Iterator<String> nameIter = EXPECTED_SERVICE_NAMES.iterator();
		// assertEquals("services and serviceNames are of different length" ,
		// services.size(), serviceNames.size());
		for (Service service : services) {
			ServiceTestUtil.validateName(service, nameIter.next());
			ServiceTestUtil.validateIcon(service);
		}
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
