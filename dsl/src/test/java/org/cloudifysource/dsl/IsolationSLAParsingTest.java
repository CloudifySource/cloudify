package org.cloudifysource.dsl;

import java.io.File;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

public class IsolationSLAParsingTest {
	
	private static final String PATH = "testResources/groovy-public-" 
			+ "provisioning/groovy-service.groovy";
	
	@Test
	public void testIsolationSLA()
			throws Exception {
		
		Service service = ServiceReader.getServiceFromFile(new File(PATH));
		Assert.assertTrue(service.getIsolationSLA() != null);
		Assert.assertTrue(service.getIsolationSLA().getGlobal() != null);
	}

}
