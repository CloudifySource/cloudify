package org.cloudifysource.dsl;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.beanutils.PropertyUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.junit.Ignore;
import org.junit.Test;

public class OverridesTest {

	private final String SERVICE_PATH = "src/test/resources/overridesTest/cassandra";
	private final String EXTERNAL_OVERRIDES_SERVICE_PATH = "src/test/resources/cassandraWithoutOverridesFile/overridesTest/cassandra";

	private final String SERVICE_EXTERNAL_OVERRIDES_FILE_PATH = "src/test/resources/overridesTest/overridesFiles/cassandraWithoutOverridesFile.overrides";

	final private String SERVICE_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH = "src/test/resources/overridesTest/overridesFiles/cassandraIllegalOverridesFileFormat.overrides";
	final private String SERVICE_ILLEGAL_OVERRIDES_PROPERTIES_FILE_PATH = "src/test/resources/overridesTest/overridesFiles/cassandraIllegalOverridesProperties.overrides";

	private static final String APPLICATION_PATH = "src/test/resources/overridesTest/apps/overridesTestApplication";
	private static final String EXTERNAL_OVERRIDES_APPLICATION_PATH = "src/test/resources/overridesTest/apps/overridesTestApplicationWithoutOverridesFile";

	private static final String APPLICATION_EXTERNAL_OVERRIDES_FILE_PATH = "src/test/resources/overridesTest/overridesFiles/overridesTestApplicationWithoutOverridesFile.overrides";

	private static final String APPLICATION_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH = "src/test/resources/overridesTest/overridesFiles/applicationIllegalOverridesFileFormat.overrides";
	private static final String APPLICATION_ILLEGAL_OVERRIDES_PROPERTIES_PATH = "src/test/resources/overridesTest/overridesFiles/applicationIllegalOverridesProperties.overrides";

	private static final Map<String, Object> SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = new HashMap<String, Object>();

	static{
		{
			// service with overrides file.
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name","overridesTest");
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("numInstances",new Integer(3));
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("lifecycle.init","overridesTest_install.groovy");

			// application with overrides file.
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service1\").name", "overridenService1");
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service1\").icon", "applicationIcon.png");
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service1\").numInstances",new Integer(3));
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service2\").name", "overridesTestApplicationOverridenService2");
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service2\").icon", "applicationIcon.png");
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service2\").numInstances",new Integer(3));
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name", "overridesTestApplicationOverriden");

			// application without an overrides file (test that application properties overrides its services properties).
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service1\").name", "overridenService1");
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service1\").icon", "applicationIcon.png");
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service1\").numInstances",new Integer(5));
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service2\").name", "overridesTestApplicationOverridenService2");
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service2\").icon", "applicationIcon.png");
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("services(\"service2\").numInstances",new Integer(5));
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name", "overridesTestApplication");
		}
	}

	@Test
	@Ignore
	public void testServiceExistingOverridesFile() {
		try {
			testDSLOverrides(SERVICE_PATH, null, Service.class, SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		}
		catch (DSLException e) {
			fail("Failed to read service " + SERVICE_PATH + e);
		}
	}

	@Test
	@Ignore
	public void testServiceExternalOverridesFile() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH, SERVICE_EXTERNAL_OVERRIDES_FILE_PATH, Service.class, SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		}
		catch (DSLException e) {
			fail("Failed to read application " + SERVICE_PATH + e);
		}
	}

	@Test
	@Ignore
	public void testServiceIllegalOverridesFileFormat() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH, SERVICE_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH, Service.class, null);
			fail("Expected DSLException for service " + EXTERNAL_OVERRIDES_SERVICE_PATH);
		}
		catch (DSLException e) {
		}
	}

	@Test
	@Ignore
	public void testServiceIllegalOverridesProperties() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH, SERVICE_ILLEGAL_OVERRIDES_PROPERTIES_FILE_PATH, Service.class, null);
			fail("Expected DSLException for service " + EXTERNAL_OVERRIDES_SERVICE_PATH);
		}
		catch (DSLException e) {

		}
	}

	@Test
	public void testApplicationExistingOverridesFile() {
		try {
			testDSLOverrides(APPLICATION_PATH, null, Application.class, APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		}
		catch (DSLException e) {
			fail("Failed to read application " + APPLICATION_PATH + e);
		}
	}

	@Test
	@Ignore
	public void testApplicationExternalOverridesFile() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH, APPLICATION_EXTERNAL_OVERRIDES_FILE_PATH, Application.class, APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		}
		catch (DSLException e) {
			fail("Failed to read application " + EXTERNAL_OVERRIDES_APPLICATION_PATH + e);
		}
	}
	
	@Test
	@Ignore
	public void testApplicationWithoutOverridesFile() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH, null, Application.class, APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS);
		}
		catch (DSLException e) {
			fail("Failed to read application " + EXTERNAL_OVERRIDES_APPLICATION_PATH + e);
		}
	}

	@Test
	@Ignore
	public void testApplicationIllegalOverridesFileFormat() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH, APPLICATION_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH, Application.class, null);
			fail("Expected DSLException for application " + EXTERNAL_OVERRIDES_APPLICATION_PATH);
		}
		catch (DSLException e) {
		}
	}

	@Test
	@Ignore
	public void testApplicationIllegalOverridesProperties() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH, APPLICATION_ILLEGAL_OVERRIDES_PROPERTIES_PATH, Application.class, null);
			fail("Expected DSLException for application " + EXTERNAL_OVERRIDES_APPLICATION_PATH);
		}
		catch (DSLException e) {

		}
	}

	private Object testDSLOverrides(String servicePath, String overridesFilePath, Class<?> clazz, Map<String, Object> expectedFields) throws DSLException {
		File workDir = new File(servicePath);

		boolean isApplication = !clazz.equals(Service.class);

		File overridesFile = null;
		if (overridesFilePath != null) {
			overridesFile = new File(overridesFilePath);
		}
		DSLReader reader = new DSLReader();
		reader.setWorkDir(workDir);
		reader.setRunningInGSC(true);
		if (isApplication) {
			reader.setDslFileNameSuffix(DSLReader.APPLICATION_DSL_FILE_NAME_SUFFIX);
			reader.addProperty(DSLUtils.APPLICATION_DIR, workDir.getAbsolutePath());
			reader.setCreateServiceContext(false);
		}
		else { //service
			reader.setDslFileNameSuffix(DSLReader.SERVICE_DSL_FILE_NAME_SUFFIX); 
		}
		reader.setOverridesFile(overridesFile);
		Object object = reader.readDslEntity(clazz);
		assertOverrides(object, reader, expectedFields);
		return object;

	}

	private void assertOverrides(Object object, DSLReader reader, Map<String, Object> expectedFields) {
		Set<Entry<String,Object>> entrySet = expectedFields.entrySet();
		for (Entry<String, Object> entry : entrySet) {
			try {
				Object fieldValue = PropertyUtils.getNestedProperty(object, entry.getKey());
				Assert.assertEquals(entry.getValue(), fieldValue);
			} catch (Exception e) {	
			}
		}
	}





}
