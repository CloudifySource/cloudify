package org.cloudifysource.dsl;

import java.io.File;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

public class IsolationSLAParsingTest {
	
	private static final String GLOBAL_PATH = "testResources/groovy-public-" 
			+ "provisioning/groovy-global-service.groovy";
	
	private static final String APP_SHARED_PATH = "testResources/groovy-public-" 
			+ "provisioning/groovy-app-shared-service.groovy";

	private static final String TENANT_SHARED_PATH = "testResources/groovy-public-" 
			+ "provisioning/groovy-tenant-shared-service.groovy";

	
	@Test
	public void testPublic()
			throws Exception {
		
		Service service = ServiceReader.getServiceFromFile(new File(GLOBAL_PATH));
		Assert.assertTrue(service.getIsolationSLA() != null);
		Assert.assertTrue(service.getIsolationSLA().getGlobal() != null);
	}
	
	@Test
	public void testAppShared()
			throws Exception {
		
		Service service = ServiceReader.getServiceFromFile(new File(APP_SHARED_PATH));
		Assert.assertTrue(service.getIsolationSLA() != null);
		Assert.assertTrue(service.getIsolationSLA().getAppShared() != null);
	}
	
	@Test
	public void testTenantShared()
			throws Exception {
		
		Service service = ServiceReader.getServiceFromFile(new File(TENANT_SHARED_PATH));
		Assert.assertTrue(service.getIsolationSLA() != null);
		Assert.assertTrue(service.getIsolationSLA().getTenantShared() != null);
	}
}
