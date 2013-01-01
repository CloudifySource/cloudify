package org.cloudifysource.dsl;

import java.io.File;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.junit.Test;

public class UseManagementIsolationSLATest {
	
	private static final String GLOBAL_PATH = "testResources/groovy-public-" 
			+ "provisioning-with-management/groovy-global-service.groovy";
	
	private static final String APP_SHARED_PATH = "testResources/groovy-public-" 
			+ "provisioning-with-management/groovy-app-shared-service.groovy";

	private static final String TENANT_SHARED_PATH = "testResources/groovy-public-" 
			+ "provisioning-with-management/groovy-tenant-shared-service.groovy";
	
	@Test
	public void testPublic()
			throws Exception {
		
		Service service = ServiceReader.getServiceFromFile(new File(GLOBAL_PATH));
		Assert.assertTrue(service.getIsolationSLA() != null);
		Assert.assertTrue(service.getIsolationSLA().getGlobal() != null);
		Assert.assertTrue(service.getIsolationSLA().getGlobal().isUseManagement());
	}
	
	@Test(expected = PackagingException.class)
	public void testAppShared()
			throws Exception {
		ServiceReader.readService(new File(APP_SHARED_PATH));
	}
	
	@Test(expected = PackagingException.class)
	public void testTenantShared()
			throws Exception {		
		ServiceReader.readService(new File(TENANT_SHARED_PATH));
	}
}
