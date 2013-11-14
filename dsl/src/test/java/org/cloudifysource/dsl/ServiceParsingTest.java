/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl;

import groovy.lang.Closure;
import groovyx.net.http.RESTClient;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.ServiceLifecycle;
import org.cloudifysource.domain.entry.ExecutableDSLEntry;
import org.cloudifysource.domain.entry.ExecutableDSLEntryType;
import org.cloudifysource.domain.scalingrules.HighThresholdDetails;
import org.cloudifysource.domain.scalingrules.LowThresholdDetails;
import org.cloudifysource.domain.scalingrules.ScalingRuleDetails;
import org.cloudifysource.domain.statistics.PerInstanceStatisticsDetails;
import org.cloudifysource.domain.statistics.ServiceStatisticsDetails;
import org.cloudifysource.dsl.entry.ClosureExecutableEntry;
import org.cloudifysource.dsl.entry.StringExecutableEntry;
import org.cloudifysource.dsl.internal.BaseDslScript;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 */
public class ServiceParsingTest {

	private final class DSLLoggerHandler extends Handler {

		private final Set<String> messages = new HashSet<String>();

		@Override
		public void publish(final LogRecord record) {
			this.messages.add(record.getMessage());
		}

		@Override
		public void flush() {
			// TODO Auto-generated method stub

		}

		@Override
		public void close() throws SecurityException {
			// TODO Auto-generated method stub

		}

		public Set<String> getMessages() {
			return messages;
		}

	}

	private static final String TEST_PARSING_RESOURCE_PATH = "testResources/testparsing/";
	private static final String TEST_PARSING_RESOURCE_PATH2 = "src/test/resources/ExternalDSLFiles/";
	private static final String TEST_PARSING_RESOURCE_PATH3 = "src/test/resources/groovyFileValidation/";
	private static final String TEST_PARSING_RESOURCE_BASE = "src/test/resources/services/";
	private static final String TEST_PARSING_AMP_MERGE_RESOURCE_PATH3 = "src/test/resources/inheritance/";
	private static final String TEST_PARSING_DEBUG = "src/test/resources/debug/printContext";
	private static final String TEST_PARSING_NETWORK = "src/test/resources/services/network/network-service";
	private static final String TEST_SYNTAX_ERROR_IN_SERVICE = "src/test/resources/services/bad-services/syntax-error";
	private static final String TEST_SYNTAX_ERROR_IN_APPLICATION = "src/test/resources/applications/bad-applications/syntax-error-in-service";

	/**
	 * 
	 * @throws DSLException .
	 * @throws UnknownHostException .
	 */
	@Test
	public void testDebugParsing() throws DSLException, UnknownHostException {
		final File testDebugPath = new File(TEST_PARSING_DEBUG);

		final Service service = ServiceReader.getServiceFromDirectory(testDebugPath).getService();

		// Assert.assertEquals("test features", service.getName());
		// Assert.assertEquals("http://"
		// + InetAddress.getLocalHost().getHostName() + ":8080",
		// service.getUrl());
		// final ServiceLifecycle lifecycle = service.getLifecycle();
		//
		// Assert.assertNotNull(lifecycle.getStart());
		// Assert.assertNotNull(lifecycle.getPostStart());
		// Assert.assertNotNull(lifecycle.getPreStop());
		// System.out.println(service);
	}

	/**
	 * 
	 * @throws DSLException .
	 * @throws UnknownHostException .
	 */
	@Test
	public void testFeaturesParsing() throws DSLException, UnknownHostException {
		final File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH
				+ "test_features-service.groovy");
		final File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		final Service service = ServiceReader.getServiceFromFile(
				testParsingBaseDslFile, testParsingBaseWorkDir).getService();
		Assert.assertEquals("test features", service.getName());
		Assert.assertEquals("http://"
				+ InetAddress.getLocalHost().getHostName() + ":8080",
				service.getUrl());
		final ServiceLifecycle lifecycle = service.getLifecycle();

		Assert.assertNotNull(lifecycle.getStart());
		Assert.assertNotNull(lifecycle.getPostStart());
		Assert.assertNotNull(lifecycle.getPreStop());
	}

	/**
	 * 
	 * @throws DSLException .
	 */
	@Test
	public void testDuplicateLifecycleEventParsing() throws DSLException {
		final File testParsingBaseDslFile = new File(
				TEST_PARSING_RESOURCE_PATH
						+ "test_parsing_on_duplicate_property_in_inner_class-service.groovy");
		final File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		try {
			ServiceReader.getServiceFromFile(testParsingBaseDslFile,
					testParsingBaseWorkDir).getService();
		} catch (final IllegalArgumentException e) {
			System.out.println("Test passed. found duplication: "
					+ e.getMessage());
		}
	}

	/**
	 * 
	 * @throws DSLException .
	 */
	@Test
	public void testDuplicateServicePropertyParsing() throws DSLException {
		final File testParsingBaseDslFile = new File(
				TEST_PARSING_RESOURCE_PATH
						+ "test_parsing_on_duplicate_property_in_service_class-service.groovy");
		final File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		try {
			ServiceReader.getServiceFromFile(testParsingBaseDslFile,
					testParsingBaseWorkDir).getService();
		} catch (final IllegalArgumentException e) {
			System.out.println("Test passed. found duplication: "
					+ e.getMessage());
		}
	}

	/**
	 * 
	 * @throws DSLException .
	 */
	@Test
	public void testBasicParsing() throws DSLException {
		final File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH
				+ "test_parsing_base-service.groovy");
		final File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		final Service service = ServiceReader.getServiceFromFile(
				testParsingBaseDslFile, testParsingBaseWorkDir).getService();
		Assert.assertEquals("test parsing base", service.getName());
		final ServiceLifecycle lifecycle = service.getLifecycle();
		Assert.assertEquals(ExecutableDSLEntryType.STRING, lifecycle.getInit()
				.getEntryType());
		Assert.assertEquals("test_parsing_base_install.groovy",
				((StringExecutableEntry) lifecycle.getInit()).getCommand());
		Assert.assertNotNull(lifecycle.getStart());
		Assert.assertNotNull(lifecycle.getPostStart());
		Assert.assertNotNull(lifecycle.getPreStop());
	}

	/**
	 * 
	 * @throws DSLException .
	 */
	@Test
	public void testBasicExtendParsing() throws DSLException {

		final File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH
				+ "test_parsing_base-service.groovy");
		final File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		final Service baseService = ServiceReader.getServiceFromFile(
				testParsingBaseDslFile, testParsingBaseWorkDir).getService();
		Assert.assertFalse(baseService.getName().equals("test parsing extend"));
		final ServiceLifecycle baseLifecycle = baseService.getLifecycle();
		Assert.assertFalse(baseLifecycle.getInit().equals(
				"test_parsing_extend_install.groovy"));
		Assert.assertNull(baseLifecycle.getStop());

		final File testParsingExtendDslFile = new File(
				TEST_PARSING_RESOURCE_PATH
						+ "test_parsing_extend-service.groovy");
		final File testParsingExtendWorkDir = new File(
				TEST_PARSING_RESOURCE_PATH);
		final Service service = ServiceReader.getServiceFromFile(
				testParsingExtendDslFile, testParsingExtendWorkDir)
				.getService();
		Assert.assertEquals("test parsing extend", service.getName());
		final ServiceLifecycle lifecycle = service.getLifecycle();
		Assert.assertEquals(ExecutableDSLEntryType.STRING, lifecycle.getInit()
				.getEntryType());
		Assert.assertEquals("test_parsing_extend_install.groovy",
				((StringExecutableEntry) lifecycle.getInit()).getCommand());
		Assert.assertNotNull(lifecycle.getStart());
		Assert.assertNotNull(lifecycle.getPostStart());
		Assert.assertNotNull(lifecycle.getPreStop());
		Assert.assertNotNull(lifecycle.getStop());
		Assert.assertEquals(1, service.getExtendedServicesPaths().size());
		Assert.assertEquals(new File(TEST_PARSING_RESOURCE_PATH, "test_parsing_base-service.groovy").getAbsolutePath(),
				service
						.getExtendedServicesPaths().getFirst());
	}

	/**
	 *
	 */
	@Test
	public void testBasicExtendIllegalPropertyLocation() {
		final File testParsingExtendDslFile = new File(
				TEST_PARSING_RESOURCE_PATH
						+ "test_parsing_extend_illegal-service.groovy");
		final File testParsingExtendWorkDir = new File(
				TEST_PARSING_RESOURCE_PATH);
		try {
			ServiceReader.getServiceFromFile(testParsingExtendDslFile,
					testParsingExtendWorkDir).getService();
			Assert.fail("No exception thrown while extend resides in illegal place");
		} catch (final Exception e) {
			// ignore
		}
	}

	/**
	 *
	 */
	@Test
	public void testBasicExtendIllegalNestedPropertyLocation() {
		final File testParsingExtendDslFile = new File(
				TEST_PARSING_RESOURCE_PATH
						+ "test_parsing_extend_illegal_nested-service.groovy");
		final File testParsingExtendWorkDir = new File(
				TEST_PARSING_RESOURCE_PATH);
		try {
			ServiceReader.getServiceFromFile(testParsingExtendDslFile,
					testParsingExtendWorkDir).getService();
			Assert.fail("No exception thrown while extend resides in illegal place");
		} catch (final Exception e) {
			// ignore
		}
	}

	/**
	 * 
	 * @throws DSLException .
	 */
	@Test
	public void testTwoLevelExtension() throws DSLException {
		final File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH
				+ "test_parsing_base-service.groovy");
		final File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		final Service baseService = ServiceReader.getServiceFromFile(
				testParsingBaseDslFile, testParsingBaseWorkDir).getService();
		Assert.assertFalse(baseService.getName().equals("test parsing extend"));
		final ServiceLifecycle baseLifecycle = baseService.getLifecycle();
		Assert.assertFalse(baseLifecycle.getInit().equals(
				"test_parsing_extend_install.groovy"));
		Assert.assertNull(baseLifecycle.getStop());
		Assert.assertFalse(baseLifecycle.getStart().equals("start"));

		final File testParsingExtendDslFile = new File(
				TEST_PARSING_RESOURCE_PATH
						+ "test_parsing_extend_two_level-service.groovy");
		final File testParsingExtendWorkDir = new File(
				TEST_PARSING_RESOURCE_PATH);
		final Service service = ServiceReader.getServiceFromFile(
				testParsingExtendDslFile, testParsingExtendWorkDir)
				.getService();
		Assert.assertEquals("test parsing extend two level", service.getName());
		final ServiceLifecycle lifecycle = service.getLifecycle();
		Assert.assertEquals(ExecutableDSLEntryType.STRING, lifecycle.getInit()
				.getEntryType());
		Assert.assertEquals("test_parsing_extend_install.groovy",
				((StringExecutableEntry) lifecycle.getInit()).getCommand());

		Assert.assertNotNull(lifecycle.getStart());
		Assert.assertNotNull(lifecycle.getPostStart());
		Assert.assertNotNull(lifecycle.getPreStop());
		Assert.assertNotNull(lifecycle.getStop());

		Assert.assertEquals(ExecutableDSLEntryType.STRING, lifecycle
				.getInstall().getEntryType());
		Assert.assertEquals("install",
				((StringExecutableEntry) lifecycle.getInstall()).getCommand());

		Assert.assertEquals(ExecutableDSLEntryType.STRING, lifecycle.getStart()
				.getEntryType());
		Assert.assertEquals("start",
				((StringExecutableEntry) lifecycle.getStart()).getCommand());

		Assert.assertEquals(2, service.getExtendedServicesPaths().size());
		Assert.assertEquals(new File(TEST_PARSING_RESOURCE_PATH,
				"test_parsing_extend-service.groovy").getAbsolutePath(),
				service.getExtendedServicesPaths().getFirst());
		Assert.assertEquals(new File(TEST_PARSING_RESOURCE_PATH, "test_parsing_base-service.groovy").getAbsolutePath(),
				service.getExtendedServicesPaths().getLast());
	}

	/**
	 * 
	 * @throws DSLException .
	 * @throws UnknownHostException .
	 */
	@Test
	public void testAutoScalingParsing() throws DSLException,
			UnknownHostException {
		final File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH
				+ "test_parsing_autoscaling-service.groovy");
		final File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		final Service service = ServiceReader.getServiceFromFile(
				testParsingBaseDslFile, testParsingBaseWorkDir).getService();
		Assert.assertTrue(service.getMinAllowedInstances() > 1);
		Assert.assertTrue(service.getNumInstances() >= service
				.getMinAllowedInstances());
		Assert.assertTrue(service.getMaxAllowedInstances() >= service
				.getNumInstances());
		Assert.assertEquals(1, service.getScaleInCooldownInSeconds());
		Assert.assertEquals(1, service.getScaleOutCooldownInSeconds());
		Assert.assertEquals(1, service.getScaleCooldownInSeconds());
		Assert.assertNotNull(service.getSamplingPeriodInSeconds());

		Assert.assertEquals("scalingRules", service.getName());

		final List<ServiceStatisticsDetails> serviceStatistics = service
				.getServiceStatistics();
		Assert.assertEquals(1, serviceStatistics.size());
		Assert.assertNotNull(serviceStatistics.get(0));
		final String servicestatisticsName = serviceStatistics.get(0).getName();
		Assert.assertNotNull(serviceStatistics.get(0).getMetric());
		Assert.assertNotNull(serviceStatistics.get(0).getInstancesStatistics());
		Assert.assertNotNull(serviceStatistics.get(0).getTimeStatistics());
		Assert.assertNotNull(serviceStatistics.get(0)
				.getMovingTimeRangeInSeconds());

		final List<PerInstanceStatisticsDetails> perInstanceStatistics = service
				.getPerInstanceStatistics();
		Assert.assertEquals(1, perInstanceStatistics.size());
		Assert.assertNotNull(perInstanceStatistics.get(0));
		Assert.assertNotNull(perInstanceStatistics.get(0).getMetric());
		Assert.assertNotNull(perInstanceStatistics.get(0)
				.getInstancesStatistics());
		Assert.assertNotNull(perInstanceStatistics.get(0).getTimeStatistics());
		Assert.assertNotNull(perInstanceStatistics.get(0)
				.getMovingTimeRangeInSeconds());

		final List<ScalingRuleDetails> scalingRules = service.getScalingRules();
		Assert.assertNotNull(scalingRules);
		Assert.assertEquals(2, scalingRules.size());
		Assert.assertNotNull(scalingRules.get(0).getHighThreshold());
		Assert.assertNotNull(scalingRules.get(0).getLowThreshold());
		Assert.assertEquals(servicestatisticsName, scalingRules.get(0)
				.getServiceStatistics());

		final HighThresholdDetails highThreshold = scalingRules.get(0)
				.getHighThreshold();
		Assert.assertNotNull(highThreshold.getValue());
		Assert.assertNotNull(highThreshold.getInstancesIncrease());

		final LowThresholdDetails lowThreshold = scalingRules.get(0)
				.getLowThreshold();
		Assert.assertNotNull(lowThreshold.getValue());
		Assert.assertNotNull(lowThreshold.getInstancesDecrease());

		// Assert.assertEquals(((ServiceStatisticsDetails) scalingRules.get(1)
		// .getServiceStatistics()).getMetric(), serviceStatistics.get(0)
		// .getMetric());
		// Assert.assertEquals(((ServiceStatisticsDetails) scalingRules.get(1)
		// .getServiceStatistics()).getInstancesStatistics()
		// .createInstancesStatistics(), serviceStatistics.get(0)
		// .getInstancesStatistics().createInstancesStatistics());
		// Assert.assertEquals(((ServiceStatisticsDetails) scalingRules.get(1)
		// .getServiceStatistics()).getTimeStatistics()
		// .createTimeWindowStatistics(1, TimeUnit.MINUTES),
		// serviceStatistics.get(0).getTimeStatistics()
		// .createTimeWindowStatistics(1, TimeUnit.MINUTES));
		Assert.assertEquals(((ServiceStatisticsDetails) scalingRules.get(1)
				.getServiceStatistics()).getMovingTimeRangeInSeconds(),
				serviceStatistics.get(0).getMovingTimeRangeInSeconds());

		Assert.assertNotNull(scalingRules.get(1).getHighThreshold());
		Assert.assertNotNull(scalingRules.get(1).getLowThreshold());
	}

	/**
	 * 
	 * @throws DSLException .
	 */
	@Test
	public void testPropertyInCustomCommand() throws DSLException {
		final File testParsingExtendDslFile = new File(
				TEST_PARSING_RESOURCE_PATH
						+ "test_property_in_custom_command-service.groovy");
		final File testParsingExtendWorkDir = new File(
				TEST_PARSING_RESOURCE_PATH);
		final Service service = ServiceReader.getServiceFromFile(
				testParsingExtendDslFile, testParsingExtendWorkDir)
				.getService();
		final Closure<?> customCommand = ((ClosureExecutableEntry) service
				.getCustomCommands().get("custom_command")).getCommand();
		final String[] params = new String[2];
		params[0] = "name";
		params[1] = "port";
		customCommand.call((Object[]) params);
		customCommand.call((Object[]) params);
	}

	/**
	 * 
	 * @throws Exception .
	 */
	@Test
	public void testNoLocatorsParsing() throws Exception {
		final File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH
				+ "test_no_locators_statement-service.groovy");
		final File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		final Service service = ServiceReader.getServiceFromFile(
				testParsingBaseDslFile, testParsingBaseWorkDir).getService();

		Assert.assertNotNull("Lifecycle should not be null",
				service.getLifecycle());
		Assert.assertNotNull("Lifecycle should not be null", service
				.getLifecycle().getLocator());
		final Object result = ((ClosureExecutableEntry) service.getLifecycle()
				.getLocator()).getCommand().call();
		Assert.assertNotNull("locators result should not be null", result);
		final List<Long> list = (List<Long>) result;
		Assert.assertEquals("Expected empty list", 0, list.size());
	}

	/**
	 * 
	 * @throws Exception .
	 */
	@Test
	public void testLocationAwareParsing() throws Exception {
		final File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH
				+ "test_location-aware-service.groovy");
		final File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		final Service service = ServiceReader.getServiceFromFile(
				testParsingBaseDslFile, testParsingBaseWorkDir).getService();

		Assert.assertTrue(
				"service.isLocationAware() returned false even though locationAware property is set to true",
				service.isLocationAware());

	}

	/**
	 * 
	 * @throws Exception .
	 */
	@Test
	public void testExternalScalingRule() throws Exception {
		final File dir = new File(TEST_PARSING_RESOURCE_PATH2
				+ "loadScalingRule");

		final Service service = ServiceReader.getServiceFromDirectory(dir).getService();

		Assert.assertTrue(service.getScalingRules().size() > 0);

	}

	/**
	 * 
	 * @throws Exception .
	 */
	@Test
	public void testExternalScalingRules() throws Exception {
		final File dir = new File(TEST_PARSING_RESOURCE_PATH2
				+ "loadScalingRules");

		final Service service = ServiceReader.getServiceFromDirectory(dir).getService();

		Assert.assertTrue(service.getScalingRules().size() > 0);

	}

	@Test
	public void testServiceWithGrab() throws Exception {
		final File serviceFile = new File(TEST_PARSING_RESOURCE_PATH3
				+ "serviceWithGrab-service.groovy");
		final File serviceDir = serviceFile.getParentFile();

		final Service service = ServiceReader.getServiceFromFile(serviceFile);

		Assert.assertNotNull(service);
		Assert.assertNotNull(service.getLifecycle());
		// check that the @grab annotation worked.
		Assert.assertEquals("Tomcat", service.getName());
	}

	@Test
	public void testServiceWithMapMerge() throws Exception {
		final File serviceFile = new File(TEST_PARSING_AMP_MERGE_RESOURCE_PATH3
				+ "/mapMerge/b/b-service.groovy");
		final Service service = ServiceReader.getServiceFromFile(serviceFile);

		Assert.assertNotNull(service);
		Assert.assertNotNull(service.getLifecycle());
		Assert.assertNotNull(service.getCustomCommands());
		Assert.assertEquals(2, service.getCustomCommands().size());
	}

	@Test
	public void testServiceWithMapOverride() throws Exception {
		final File serviceFile = new File(TEST_PARSING_AMP_MERGE_RESOURCE_PATH3
				+ "/mapOverride/b/b-service.groovy");
		final Service service = ServiceReader.getServiceFromFile(serviceFile);

		Assert.assertNotNull(service);
		Assert.assertNotNull(service.getLifecycle());
		Assert.assertNotNull(service.getCustomCommands());
		Assert.assertEquals(1, service.getCustomCommands().size());
		Assert.assertTrue(service.getCustomCommands().containsKey("cmdA"));
		Assert.assertTrue(service.getCustomCommands().get("cmdA") == null);

	}

	@Test
	public void testRestClientExists() {
		// this is just a validation that the groovy rest client project is in the
		// code - there are no direct references to it in the Cloudify project
		// but we want it to be available to service recipes by default.
		new RESTClient();

	}

	@Test
	public void testStorageDetailsParsing() throws Exception {
		final File serviceFile = new File(TEST_PARSING_RESOURCE_PATH3
				+ "simpleStorage-service.groovy");
		final Service service = ServiceReader.getServiceFromFile(serviceFile);
		Assert.assertNotNull(service.getStorage());
		Assert.assertNotNull(service.getStorage().getTemplate());
		Assert.assertNotNull(service.getStorage().getTemplate().equals("SMALL_BLOCK"));

	}

	@Test
	public void testServiceWithDSLLogger() throws Exception {
		final Logger logger = Logger.getLogger(BaseDslScript.class.getName());
		final DSLLoggerHandler handler = new DSLLoggerHandler();
		logger.addHandler(handler);

		final File dir = new File(TEST_PARSING_RESOURCE_BASE + "logger");
		final Service service = ServiceReader.getServiceFromDirectory(dir).getService();
		Assert.assertNotNull(service);

		Assert.assertTrue("Println logger not found", handler.getMessages().contains("println logger call"));
		Assert.assertTrue("Print logger not found", handler.getMessages().contains("print logger call"));
		Assert.assertTrue("Class println not found", handler.getMessages().contains("Test Class println"));
		Assert.assertTrue("Class print not found", handler.getMessages().contains("Test Class print"));

		Assert.assertFalse("preInstall event message appeared before expected",
				handler.getMessages().contains("This is the preInstall event"));

		final ExecutableDSLEntry preInstall = service.getLifecycle().getPreInstall();
		final ClosureExecutableEntry preInstallClosureEntry = (ClosureExecutableEntry) preInstall;
		preInstallClosureEntry.getCommand().call();

		Assert.assertTrue("preInstall event message not found",
				handler.getMessages().contains("This is the preInstall event"));

	}

	@Test
	public void testStopDetectionWithScript() {

		final File serviceFile = new File(TEST_PARSING_RESOURCE_BASE + "misc/stopDetectionScript-service.groovy");

		try {
			ServiceReader.getServiceFromFile(serviceFile);
			Assert.fail("Expected parsing to fail");
		} catch (PackagingException e) {
			Assert.assertTrue("Unexpected error message: " + e.getMessage(),
					e.getMessage().contains("The stop detection field only supports execution of closures"));
		}
	}

	@Test
	public void testInvalidProperties() {

		final File serviceDir = new File(TEST_PARSING_RESOURCE_BASE + "invalidProperties");

		try {
			Service service = ServiceReader.getServiceFromDirectory(serviceDir).getService();
			Assert.fail("Expected parsing to fail");
		} catch (Exception e) {
			Assert.assertTrue("Invalid error message", e.getMessage().contains("Error converting from"));
		}
	}

	@Test
	public void testInvalidPropertiesFile() {

		final File serviceDir = new File(TEST_PARSING_RESOURCE_BASE + "invalidPropertiesFile");

		try {
			ServiceReader.getServiceFromDirectory(serviceDir).getService();
			Assert.fail("Expected parsing to fail");
		} catch (Exception e) {
			Assert.assertTrue("Invalid error message: " + e.getMessage(), e.getMessage().contains("unexpected char"));
		}
	}

	@Test
	public void testNetworkService() throws Exception {
		final File serviceDir = new File(TEST_PARSING_NETWORK);
		final Service service = ServiceReader.getServiceFromDirectory(serviceDir).getService();

		Assert.assertNotNull(service.getNetwork());
		Assert.assertNotNull(service.getNetwork().getAccessRules() != null);
		Assert.assertNotNull(service.getNetwork().getAccessRules().getIncoming() != null);
		Assert.assertEquals(service.getNetwork().getAccessRules().getIncoming().get(0).getPortRange(), "80");
		Assert.assertEquals("My_Network", service.getNetwork().getTemplate());

	}
	
	@Test
	public void testSyntaxErrorInService() throws Exception {
		final File serviceDir = new File(TEST_SYNTAX_ERROR_IN_SERVICE);
		try {
			ServiceReader.getServiceFromDirectory(serviceDir).getService();
		} catch(final Exception e) {
			Assert.assertTrue("Expected name of file in error message", e.getMessage().contains("nothing-service.groovy"));
			return;
		}
		
		Assert.fail("Expected parsing compilation error");

	}

	@Test
	public void testSyntaxErrorInApplication() throws Exception {
		final File applicationDir = new File(TEST_SYNTAX_ERROR_IN_APPLICATION);
		try {
			ServiceReader.getApplicationFromFile(applicationDir);
		} catch(final Exception e) {
			Assert.assertTrue("Expected name of file in error message", e.getMessage().contains("nothing-service.groovy"));
			return;
		}
		
		Assert.fail("Expected parsing compilation error");

	}

	
}
