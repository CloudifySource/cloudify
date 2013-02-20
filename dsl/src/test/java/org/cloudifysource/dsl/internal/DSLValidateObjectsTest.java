package org.cloudifysource.dsl.internal;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.junit.Test;

/**
 * 
 * @author yael
 *
 */
public class DSLValidateObjectsTest {

private final static String CLOUD_PATH = "../esc/src/main/resources/clouds/rsopenstack/rsopenstack-cloud.groovy";
private final static String SERVICE_PATH = "testResources/simple/simple-service.groovy";

	//@Test
	public void testCloud() throws IOException {
		File cloudFile = new File(CLOUD_PATH);
		
		DSLReader dslReader = new DSLReader();
		dslReader.setDslFile(cloudFile);
		dslReader.setWorkDir(cloudFile.getParentFile());
		dslReader.setCreateServiceContext(false);
		
		try {
			dslReader.readDslEntity(Cloud.class);
			Assert.fail("The validation of file " + CLOUD_PATH + " was supposed to fail");
		} catch (Exception e) {
			
		}
		try {
			dslReader.setValidateObjects(false);
			dslReader.readDslEntity(Cloud.class);
		} catch (Exception e) {
			Assert.fail("Expecting no validation for cloud file " + CLOUD_PATH + ", exception was " + e.getMessage());
		}
	}
	
	@Test
	public void testService() throws IOException {
		File serviceFile = new File(SERVICE_PATH);
		
		DSLReader dslReader = new DSLReader();
		dslReader.setDslFile(serviceFile);
		dslReader.setWorkDir(serviceFile.getParentFile());		
		
		try {
			dslReader.readDslEntity(Service.class);
			Assert.fail("The validation of service file " + SERVICE_PATH + " was supposed to fail");
		} catch (Exception e) {
			
		}
		try {
			dslReader.setValidateObjects(false);
			dslReader.readDslEntity(Service.class);
		} catch (Exception e) {
			Assert.fail("Expecting no Validation for service file " + SERVICE_PATH + " exception was " + e.getMessage());
		}
	}
}
