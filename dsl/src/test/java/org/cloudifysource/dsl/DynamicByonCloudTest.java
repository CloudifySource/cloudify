package org.cloudifysource.dsl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import groovy.lang.Closure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudConfiguration;
import org.cloudifysource.dsl.cloud.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

public class DynamicByonCloudTest {

	private static final String DYNAMIC_BYON_CLOUD_PATH = 
			"testResources/cloud/dynamicByon/dynamic-byon-cloud.groovy";
	private static final String DYNAMIC_BYON_CLOUD_PROPERTIES_PATH = 
			"testResources/cloud/dynamicByon/dynamic-byon-cloud.properties";
	private static final String START_MACHINE_IP_PROPERTY_NAME = "startMachineIP";
	private static final String MISSING_START_MNG_GROOVY_PATH = 
			"testResources/cloud/dynamicByon/missingStartMng/dynamic-byon-cloud.groovy";
	private static final String MISSING_STOP_MNG_GROOVY_PATH = 
			"testResources/cloud/dynamicByon/missingStopMng/dynamic-byon-cloud.groovy";
	private static final String MISSING_START_MACHINE_GROOVY_PATH = 
			"testResources/cloud/dynamicByon/missingStartMachine/dynamic-byon-cloud.groovy";
	private static final String MISSING_STOP_MACHINE_GROOVY_PATH = 
			"testResources/cloud/dynamicByon/missingStopMachine/dynamic-byon-cloud.groovy";
	
	@SuppressWarnings("resource")
	@Test
	public void testDynamicByonCloudParser() throws IOException, DSLException {
		
		Cloud cloud = ServiceReader.readCloud(new File(DYNAMIC_BYON_CLOUD_PATH));
		
		assertNotNull(cloud);
		assertNotNull(cloud.getProvider());
		final Map<String, ComputeTemplate> templates = cloud.getCloudCompute().getTemplates();
		assertNotNull(templates);
		assertNotNull(cloud.getUser());
		final CloudConfiguration configuration = cloud.getConfiguration();
		assertNotNull(configuration);
		String managementMachineTemplate = configuration.getManagementMachineTemplate();
		assertNotNull(managementMachineTemplate);
		final ComputeTemplate managementTemplate = templates.get(managementMachineTemplate);
		assertNotNull(managementTemplate);
		assertEquals("org.cloudifysource.esc.driver.provisioning.byon.DynamicByonProvisioningDriver", 
				configuration.getClassName());
		
		Map<String, Object> custom = managementTemplate.getCustom();
		assertNotNull(custom);
		// startMachine
		@SuppressWarnings("unchecked")
		Closure<String> startClosure = (Closure<String>) custom.get(CloudifyConstants.DYNAMIC_BYON_START_MACHINE_KEY);
		String ip = startClosure.call();
		assertNotNull(ip);
		Properties props = new Properties();
		props.load(new FileInputStream(new File(DYNAMIC_BYON_CLOUD_PROPERTIES_PATH)));
		String expectedIP = props.getProperty(START_MACHINE_IP_PROPERTY_NAME).replace("\"", "");
		assertEquals(expectedIP, ip);
		
		// stopMachine
		String ipProperty = System.getProperty(CloudifyConstants.DYNAMIC_BYON_STOP_MACHINE_KEY);
		assertNull(ipProperty);
		Closure<?> stopClosure = (Closure<?>) custom.get(CloudifyConstants.DYNAMIC_BYON_STOP_MACHINE_KEY);
		stopClosure.call(ip);
		ipProperty = System.getProperty(CloudifyConstants.DYNAMIC_BYON_STOP_MACHINE_KEY);
		assertNotNull(ipProperty);
		assertEquals(expectedIP, ipProperty);
		
		// startManagementMachines
		@SuppressWarnings("unchecked")
		Closure<List<String>> startMngMachinesClosure = (Closure<List<String>>) 
				custom.get(CloudifyConstants.DYNAMIC_BYON_START_MNG_MACHINES_KEY);
		List<String> managemetnMachines = startMngMachinesClosure.call();
		
		// stopManagementMachines
		ipProperty = System.getProperty(CloudifyConstants.DYNAMIC_BYON_STOP_MNG_MACHINES_KEY);
		assertNull(ipProperty);
		Closure<?> stopMngMachinesClosure = (Closure<?>) 
				custom.get(CloudifyConstants.DYNAMIC_BYON_STOP_MNG_MACHINES_KEY);
		stopMngMachinesClosure.call();
		ipProperty = System.getProperty(CloudifyConstants.DYNAMIC_BYON_STOP_MNG_MACHINES_KEY);
		String[] split = ipProperty.split(",");
		List<String> asList = Arrays.asList(split);
		assertEquals(managemetnMachines, asList);
	}
	
	@Test
	public void testIllegalDynamicByonMissingClosures() {
		try {
			Cloud cloud = ServiceReader.readCloud(new File(MISSING_START_MNG_GROOVY_PATH));
			Assert.fail("Dynamic byon cloud groovy missing a declaration for the startManagementMachines closure "
					+ "in its management template's custom closure, " 
					+ "a DSL validation exception was expected.");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("The " + CloudifyConstants.DYNAMIC_BYON_START_MNG_MACHINES_KEY 
					+ " closure is missing"));
		}
		
		try {
			Cloud cloud = ServiceReader.readCloud(new File(MISSING_STOP_MNG_GROOVY_PATH));
			Assert.fail("Dynamic byon cloud groovy missing a declaration for the stopManagementMachines closure "
					+ "in its management template's custom closure, " 
					+ "a DSL validation exception was expected.");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("The " + CloudifyConstants.DYNAMIC_BYON_STOP_MNG_MACHINES_KEY 
					+ " closure is missing"));
		}
		
		try {
			Cloud cloud = ServiceReader.readCloud(new File(MISSING_START_MACHINE_GROOVY_PATH));
			Assert.fail("Dynamic byon cloud groovy missing a declaration for a startMachine closure "
					+ "in at least one of its template's custom closure, " 
					+ "a DSL validation exception was expected.");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("The " + CloudifyConstants.DYNAMIC_BYON_START_MACHINE_KEY 
					+ " closure is missing"));
		}
		
		try {
			Cloud cloud = ServiceReader.readCloud(new File(MISSING_STOP_MACHINE_GROOVY_PATH));
			Assert.fail("Dynamic byon cloud groovy missing a declaration for a stopMachine closure "
					+ "in at least one of its template's custom closure, " 
					+ "a DSL validation exception was expected.");
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("The " + CloudifyConstants.DYNAMIC_BYON_STOP_MACHINE_KEY 
					+ " closure is missing"));
		}
	}

}
