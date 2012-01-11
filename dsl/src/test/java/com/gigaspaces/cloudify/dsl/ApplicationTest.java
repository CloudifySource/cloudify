package com.gigaspaces.cloudify.dsl;

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
