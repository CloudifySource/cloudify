package org.cloudifysource.dsl;

import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.beanutils.PropertyUtils;
import org.cloudifysource.dsl.entry.ExecutableDSLEntry;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.junit.Test;

/**
 * 
 * @author yael
 *
 */
public class OverridesTest {

	private final String SERVICE_PATH 
	= "src/test/resources/overridesTest/services/cassandra";
	private final String EXTERNAL_OVERRIDES_SERVICE_PATH 
	= "src/test/resources/overridesTest/services/cassandraWithoutOverridesFile/cassandra";

	private final String SERVICE_EXTERNAL_OVERRIDES_FILE_PATH 
	= "src/test/resources/overridesTest/overridesFiles/cassandraWithoutOverridesFile.overrides";

	final private String SERVICE_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH 
	= "src/test/resources/overridesTest/overridesFiles/cassandraIllegalOverridesFileFormat.overrides";
	final private String SERVICE_ILLEGAL_OVERRIDES_PROPERTIES_FILE_PATH 
	= "src/test/resources/overridesTest/overridesFiles/cassandraIllegalOverridesProperties.overrides";

	private static final String APPLICATION_PATH 
	= "src/test/resources/overridesTest/apps/overridesTestApplication";
	private static final String EXTERNAL_OVERRIDES_APPLICATION_PATH 
	= "src/test/resources/overridesTest/apps/overridesTestApplicationWithoutOverridesFile";

	private static final String APPLICATION_EXTERNAL_OVERRIDES_FILE_PATH 
	= "src/test/resources/overridesTest/overridesFiles/overridesTestApplicationWithoutOverridesFile.overrides";

	private static final String APPLICATION_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH 
	= "src/test/resources/overridesTest/overridesFiles/applicationIllegalOverridesFileFormat.overrides";
	private static final String APPLICATION_ILLEGAL_OVERRIDES_PROPERTIES_PATH 
	= "src/test/resources/overridesTest/overridesFiles/applicationIllegalOverridesProperties.overrides";

	private static final Map<String, Object> SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS 
	= new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS 
	= new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS 
	= new HashMap<String, Object>();

	static {
			// service with overrides file.
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name",
					"overridesTest");
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("numInstances",
					new Integer(3));
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("lifecycle.init",
					"overridesTest_install.groovy");

			// application with overrides file.
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"services[0].icon", "applicationIcon.png");
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"services[0].numInstances", new Integer(3));
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"services[1].icon", "applicationIcon.png");
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put(
					"services[1].numInstances", new Integer(3));
			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name",
					"overridesTestApplicationOverriden");

			// application without an overrides file (test that application
			// properties overrides its services properties).
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("services[0].icon", "applicationIcon.png");
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("services[0].numInstances", new Integer(5));
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("services[1].icon", "applicationIcon.png");
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("services[1].numInstances", new Integer(5));
			APPLICATION_WITHOUT_OVERRIDES_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS
					.put("name", "overridesTestApplication");
	}

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

	@Test
	public void testServiceIllegalOverridesFileFormat()
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, DSLException {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH,
					SERVICE_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH, Service.class,
					null);
			fail("Expected DSLException for service "
					+ EXTERNAL_OVERRIDES_SERVICE_PATH);
		} catch (final IllegalArgumentException e) { 
			
		}
	}

	@Test
	public void testServiceIllegalOverridesProperties()
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, DSLException {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH,
					SERVICE_ILLEGAL_OVERRIDES_PROPERTIES_FILE_PATH,
					Service.class, null);
			fail("Expected DSLException for service "
					+ EXTERNAL_OVERRIDES_SERVICE_PATH);
		} catch (final IllegalArgumentException e) {

		}
	}

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
	public void testApplicationIllegalOverridesFileFormat()
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, DSLException {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH,
					APPLICATION_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH,
					Application.class, null);
			fail("Expected DSLException for application "
					+ EXTERNAL_OVERRIDES_APPLICATION_PATH);
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testApplicationIllegalOverridesProperties()
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, DSLException {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH,
					APPLICATION_ILLEGAL_OVERRIDES_PROPERTIES_PATH,
					Application.class, null);
			fail("Expected DSLException for application "
					+ EXTERNAL_OVERRIDES_APPLICATION_PATH);
		} catch (final IllegalArgumentException e) {

		}
	}

	private Object testDSLOverrides(final String servicePath,
			final String overridesFilePath, final Class<?> clazz,
			final Map<String, Object> expectedFields) throws
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, DSLException {
		final File workDir = new File(servicePath);

		final boolean isApplication = !clazz.equals(Service.class);

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
		} else { // service
			reader.setDslFileNameSuffix(DSLReader.SERVICE_DSL_FILE_NAME_SUFFIX);
		}
		reader.setOverridesFile(overridesFile);
		final Object object = reader.readDslEntity(clazz);
		assertOverrides(object, reader, expectedFields, workDir);
		return object;

	}

	private void assertOverrides(final Object object, final DSLReader reader,
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
