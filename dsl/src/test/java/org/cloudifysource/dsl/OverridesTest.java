package org.cloudifysource.dsl;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.entry.ExecutableDSLEntry;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

/**
 * 
 * @author yael
 *
 */
public class OverridesTest {

	private static final String SERVICE_PATH = 
			"src/test/resources/overridesTest/services/cassandra";
	private static final String EXTERNAL_OVERRIDES_SERVICE_PATH = 
			"src/test/resources/overridesTest/services/cassandraWithoutOverridesFile/cassandra";
	private static final String SERVICE_EXTERNAL_OVERRIDES_FILE_PATH = 
			"src/test/resources/overridesTest/overridesFiles/cassandraWithoutOverridesFile.overrides";

	private static final String APPLICATION_PATH = 
			"src/test/resources/overridesTest/apps/overridesTestApplication";
	private static final String EXTERNAL_OVERRIDES_APPLICATION_PATH = 
			"src/test/resources/overridesTest/apps/overridesTestApplicationWithoutOverridesFile";
	private static final String APPLICATION_EXTERNAL_OVERRIDES_FILE_PATH = 
			"src/test/resources/overridesTest/overridesFiles/overridesTestApplicationWithoutOverridesFile.overrides";
	private static final String APPLICATION_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH = 
			"src/test/resources/overridesTest/overridesFiles/applicationIllegalOverridesFileFormat.overrides";
	private static final String APPLICATION_ILLEGAL_OVERRIDES_PROPERTIES_PATH = 
			"src/test/resources/overridesTest/overridesFiles/applicationIllegalOverridesProperties.overrides";

	private static final String APPLICATION_SERVICE_PATH = 
			"src/test/resources/overridesTest/services/service1";

	private static final String EC2_CLOUD_WITH_FILE_PATH = "testResources/cloud/ec2-overrides-with-file";
	
	private static final String EC2_CLOUD_WITH_FILE_OVERRIDES_PATH = "testResources/cloud/ec2-overrides-with-file/ec2-cloud.overrides";
	
	private static final String EC2_CLOUD_WITH_SCRIPT_PATH = "testResources/cloud/ec2-overrides-with-script";	

	private static final Map<String, Object> SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = 
			new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = 
			new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = 
			new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = 
			new HashMap<String, Object>();
	
	private static final Map<String, Object> EC2_CLOUD_OVERRIDES_PROPERTIES_MATCHING_FIELDS = 
			new HashMap<String, Object>();
 
	private static final Integer NUM_INSTANCES = new Integer(5);
	private static final Integer OVERRIDEN_NUM_INSTANCES = new Integer(3);

	static {
		
			// cloud with overrides file
			EC2_CLOUD_OVERRIDES_PROPERTIES_MATCHING_FIELDS.put("myUser", "\"OverridesTestUser\"");
			EC2_CLOUD_OVERRIDES_PROPERTIES_MATCHING_FIELDS.put("myApiKey", "\"OverridesTestApiKey\"");
			EC2_CLOUD_OVERRIDES_PROPERTIES_MATCHING_FIELDS.put("myKeyPair", "\"OverridesTestApiKey\"");
			
			// service with overrides file.
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name",
					"overridesTest");
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("numInstances",
					OVERRIDEN_NUM_INSTANCES);
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("lifecycle.init",
					"overridesTest_install.groovy");

			//service with overrides, application properties and application overrides files.
			APPLICATION_SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"icon", "applicationIcon.png");
			APPLICATION_SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"numInstances", OVERRIDEN_NUM_INSTANCES);

			// application with overrides file.
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"services[0].icon", "applicationIcon.png");
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"services[0].numInstances", OVERRIDEN_NUM_INSTANCES);
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"services[1].icon", "applicationIcon.png");
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"services[1].numInstances", OVERRIDEN_NUM_INSTANCES);
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name",
					"overridesTestApplicationOverriden");

			// application without an overrides file 
			// (test only the override of application properties of services properties).
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("services[0].icon", "applicationIcon.png");
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("services[0].numInstances", NUM_INSTANCES);
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("services[1].icon", "applicationIcon.png");
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("services[1].numInstances", NUM_INSTANCES);
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("name", "overridesTestApplication");
	}

	/**
	 * 
	 * @throws IllegalAccessException .
	 * @throws InvocationTargetException .
	 * @throws NoSuchMethodException .
	 */
	@Test
	public void testServiceExistingOverridesFile()
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		try {
			testDSLOverrides(SERVICE_PATH, null, Service.class,
					SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		} catch (final DSLException e) {
			fail("Failed to read service " + SERVICE_PATH + e);
		}
	}

	/**
	 * 
	 * @throws IllegalAccessException .
	 * @throws InvocationTargetException .
	 * @throws NoSuchMethodException .
	 */
	@Test
	public void testServiceExternalOverridesFile()
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH,
					SERVICE_EXTERNAL_OVERRIDES_FILE_PATH, Service.class,
					SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		} catch (final DSLException e) {
			fail("Failed to read application " + SERVICE_PATH + e);
		}
	}

	/**
	 * 
	 * @throws IllegalAccessException .
	 * @throws InvocationTargetException .
	 * @throws NoSuchMethodException .
	 */
	@Test
	public void testServiceWithApplicationPropertiesAndOverridesFiles() 
			throws IllegalAccessException, InvocationTargetException, 
			NoSuchMethodException {
		try {
			testDSLOverrides(APPLICATION_SERVICE_PATH, null, Service.class, 
					APPLICATION_SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		} catch (final DSLException e) {
			fail("Failed to read application " + APPLICATION_SERVICE_PATH + e);
		}
	}

	/**
	 * 
	 * @throws IllegalAccessException .
	 * @throws InvocationTargetException .
	 * @throws NoSuchMethodException .
	 */
	@Test
	public void testApplicationExistingOverridesFile()
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		try {
			testDSLOverrides(APPLICATION_PATH, null, Application.class,
					APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		} catch (final DSLException e) {
			fail("Failed to read application " + APPLICATION_PATH + e);
		}
	}

	/**
	 * 
	 * @throws IllegalAccessException .
	 * @throws InvocationTargetException .
	 * @throws NoSuchMethodException .
	 */
	@Test
	public void testApplicationExternalOverridesFile()
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH,
					APPLICATION_EXTERNAL_OVERRIDES_FILE_PATH,
					Application.class,
					APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		} catch (final DSLException e) {
			fail("Failed to read application "
					+ EXTERNAL_OVERRIDES_APPLICATION_PATH + e);
		}
	}

	/**
	 * 
	 * @throws IllegalAccessException .
	 * @throws InvocationTargetException .
	 * @throws NoSuchMethodException .
	 */
	@Test
	public void testApplicationWithoutOverridesFile()
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH, null,
					Application.class,
					APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		} catch (final DSLException e) {
			fail("Failed to read application "
					+ EXTERNAL_OVERRIDES_APPLICATION_PATH + e);
		}
	}
	
	@Test
	public void testCloudWithOverridesFile() throws IllegalAccessException, 
		InvocationTargetException, NoSuchMethodException, DSLException {
		
		Cloud cloud = ServiceReader.readCloudFromDirectory(EC2_CLOUD_WITH_FILE_PATH);		
		// overriden props
		Assert.assertEquals("OverridesTestUser", cloud.getUser().getUser());
		Assert.assertEquals("OverridesTestApiKey", cloud.getUser().getApiKey());
		Assert.assertEquals("OverridesTestKeyPair", (String) cloud.getTemplates().
				get("SMALL_LINUX").getOptions().get("keyPair"));
		Assert.assertEquals("OverridesTestImageId", cloud.getTemplates().get("SMALL_LINUX").getImageId());
		
		// not overrides, taken from .properties file
		Assert.assertEquals("TestKeyFile.pem", cloud.getTemplates().get("SMALL_LINUX").getKeyFile());
	}
	
	@Test
	public void testCloudWithOverridesScript() throws DSLException, IOException {
		
		Cloud cloud = ServiceReader.readCloudFromDirectory(EC2_CLOUD_WITH_SCRIPT_PATH, 
				FileUtils.readFileToString(new File(EC2_CLOUD_WITH_FILE_OVERRIDES_PATH)));		
		// overriden props
		Assert.assertEquals("OverridesTestUser", cloud.getUser().getUser());
		Assert.assertEquals("OverridesTestApiKey", cloud.getUser().getApiKey());
		Assert.assertEquals("OverridesTestKeyPair", (String) cloud.getTemplates().
				get("SMALL_LINUX").getOptions().get("keyPair"));
		Assert.assertEquals("OverridesTestImageId", cloud.getTemplates().get("SMALL_LINUX").getImageId());
		
		// not overrides, taken from .properties file
		Assert.assertEquals("TestKeyFile.pem", cloud.getTemplates().get("SMALL_LINUX").getKeyFile());
		
	}

	private static Object testDSLOverrides(final String servicePath,
			final String overridesFilePath, final Class<?> clazz,
			final Map<String, Object> expectedFields) throws
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, DSLException {
		final File workDir = new File(servicePath);

		final boolean isApplication = clazz.equals(Application.class);
		final boolean isService = clazz.equals(Service.class);
		final boolean isCloud = clazz.equals(Cloud.class);

		File overridesFile = null;
		if (overridesFilePath != null) {
			overridesFile = new File(overridesFilePath);
		}
		final DSLReader reader = new DSLReader();
		reader.setWorkDir(workDir);
		reader.setRunningInGSC(true);
		if (isApplication) {
			reader.setDslFileNameSuffix(DSLReader.APPLICATION_DSL_FILE_NAME_SUFFIX);
			reader.addProperty(DSLUtils.APPLICATION_DIR,
					workDir.getAbsolutePath());
			reader.setCreateServiceContext(false);
		} else if (isService) {
			reader.setDslFileNameSuffix(DSLReader.SERVICE_DSL_FILE_NAME_SUFFIX);
		} else if (isCloud) {
			reader.setDslFileNameSuffix(DSLReader.CLOUD_DSL_FILE_NAME_SUFFIX);
			reader.setCreateServiceContext(false);
		} else {
			throw new DSLException("Class " + clazz.getName() + " does not exist in the DSL Domain");
		}
		reader.setOverridesFile(overridesFile);
		final Object object = reader.readDslEntity(clazz);
		assertOverrides(object, reader, expectedFields, workDir);
		return object;

	}

	private static void assertOverrides(final Object object, final DSLReader reader,
			final Map<String, Object> expectedFields , final File workDirectory)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		final Set<Entry<String, Object>> entrySet = expectedFields.entrySet();
		for (final Entry<String, Object> entry : entrySet) {
			String fieldName = entry.getKey();
			Object fieldValue = PropertyUtils.getNestedProperty(object, fieldName);
			if (fieldValue instanceof ExecutableDSLEntry) {
				fieldValue = fieldValue.toString();
			}
			Assert.assertEquals(entry.getValue(), fieldValue);
		}
	}

}
