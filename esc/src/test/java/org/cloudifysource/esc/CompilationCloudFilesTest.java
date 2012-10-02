package org.cloudifysource.esc;

import groovy.lang.MissingPropertyException;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

/**
 * 
 * @author yael
 *
 */
public class CompilationCloudFilesTest {

	private static final String CLOUDS_PATH = "src/main/resources/clouds/";
	private static final String AZURE = CLOUDS_PATH + "azure/azure-cloud.groovy";
	private static final String BYON = CLOUDS_PATH + "byon/byon-cloud.groovy";
	private static final String EC2 = CLOUDS_PATH + "ec2/ec2-cloud.groovy";
	private static final String EC2_WIN = CLOUDS_PATH + "ec2-win/ec2-win-cloud.groovy";
	private static final String OPENSTACK = CLOUDS_PATH + "openstack/openstack-cloud.groovy";
	private static final String RSOPENSTACK = CLOUDS_PATH + "rsopenstack/rsopenstack-cloud.groovy";

	@Test 
	public void testAzure() throws IOException, DSLException {
		try{
			testCloudFileCompilation(AZURE);
		} catch(MissingPropertyException e) {
			Assert.assertTrue(e.getMessage().contains("No such property: username for class: dslEntity"));
		} catch (Exception e) {
			Assert.fail("AZURE validation failed: " + e.getMessage());
		}
	}
	
	@Test 
	public void testByon() {
		try{
			testCloudFileCompilation(BYON);
		} catch(Exception e) {
			Assert.fail("BYON validation failed: " + e.getMessage());
		}
	}
	
	@Test 
	public void testEc2() {
		try{
			testCloudFileCompilation(EC2);
		} catch(RuntimeException e) {
			Assert.assertTrue(e.getCause() instanceof DSLValidationException
					&& e.getMessage().contains("User field still has default configuration value of ENTER_USER"));
		} catch(Exception e) {
			Assert.fail("EC2 validation failed: " + e.getMessage());
		}
	}
	
	@Test 
	public void testEc2win() {
		try{
			testCloudFileCompilation(EC2_WIN);
		} catch(RuntimeException e) {
			Assert.assertTrue(e.getCause() instanceof DSLValidationException
					&& e.getMessage().contains("User field still has default configuration value of ENTER_USER"));
		} catch(Exception e) {
			Assert.fail("EC2_WIN validation failed: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	@Test 
	public void testOpenstack() {
		try{
			testCloudFileCompilation(OPENSTACK);
		} catch(RuntimeException e) {
			Assert.assertTrue(e.getCause() instanceof DSLValidationException
					&& e.getMessage().contains("User field still has default configuration value of ENTER_USER"));
		}
		catch (Exception e) {
			Assert.fail("OPENSTACK validation failed: " + e.getMessage());
		}
	}
	
	@Test 
	public void testRsopenstack() throws IOException, DSLException {
		try{
			testCloudFileCompilation(RSOPENSTACK);
		} catch(RuntimeException e) {
			Assert.assertTrue(e.getCause() instanceof DSLValidationException 
					&& e.getMessage().contains("The tenant id property must be set"));
		} catch (Exception e) {
			Assert.fail("RSOPENSTACK validation failed: " + e.getMessage());
		}
	}

	private void testCloudFileCompilation(String cloudFilePath) throws IOException, DSLException {
		File dslFile = new File(cloudFilePath);
		ServiceReader.readCloud(dslFile);
	}
}
