package org.cloudifysource.dsl;

import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.beanutils.PropertyUtils;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.junit.Test;

public class OverridesTest {

	private final String SERVICE_PATH = "src/test/resources/overridesTest/cassandra";
	private final String EXTERNAL_OVERRIDES_SERVICE_PATH = "src/test/resources/cassandraWithoutOverridesFile/overridesTest/cassandra";

	private final String SERVICE_EXTERNAL_OVERRIDES_FILE_PATH = "src/test/resources/overridesTest/overridesFiles/cassandraWithoutOverridesFile.overrides";

	final private String SERVICE_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH = "src/test/resources/overridesTest/overridesFiles/cassandraIllegalOverridesFileFormat.overrides";
	final private String SERVICE_ILLEGAL_OVERRIDES_PROPERTIES_FILE_PATH = "src/test/resources/overridesTest/overridesFiles/cassandraIllegalOverridesProperties.overrides";
	final private String SERVICE_ILLEGAL_OVERRIDES_FIELDS_FILE_PATH = "src/test/resources/overridesTest/overridesFiles/cassandraIllegalOverridesFields.overrides";
	
	private static final String APPLICATION_PATH = "src/test/resources/overridesTest/apps/overridesTestApplication";
	private static final String EXTERNAL_OVERRIDES_APPLICATION_PATH = "src/test/resources/overridesTest/apps/overridesTestApplicationWithoutOverridesFile";
	
	private static final String APPLICATION_EXTERNAL_OVERRIDES_FILE_PATH = "src/test/resources/overridesTest/overridesFiles/overridesTestApplicationWithoutOverridesFile.overrides";

	private static final String APPLICATION_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH = "src/test/resources/overridesTest/overridesFiles/applicationIllegalOverridesFileFormat.overrides";
	private static final String APPLICATION_ILLEGAL_OVERRIDES_PROPERTIES_PATH = "src/test/resources/overridesTest/overridesFiles/applicationIllegalOverridesProperties.overrides";
	private static final String APPLICATION_ILLEGAL_OVERRIDES_FIELDS_PATH = "src/test/resources/overridesTest/overridesFiles/applicationIllegalOverridesFields.overrides";

	
	private static final Map<String, Object> SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_SERVICE1_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_SERVICE2_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = new HashMap<String, Object>();
	private static final Map<String, Object> APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS = new HashMap<String, Object>();
	static{
		{
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name","overridesTest");
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("numInstances",3);
			SERVICE_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("lifecycle.init","overridesTest_install.groovy");
			
			APPLICATION_SERVICE1_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name", "overridesTestApplicationOverridenService1");
			APPLICATION_SERVICE1_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("lifecycle.init", "overridenService1_install.groovy");
			APPLICATION_SERVICE1_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("numInstances",3);
			APPLICATION_SERVICE1_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("plugins[0].className","overridenService1Plugin1ClassNameOverriden");

			APPLICATION_SERVICE2_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name", "overridesTestApplicationOverridenService2");
			APPLICATION_SERVICE2_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("lifecycle.init", "overridenService2_install.groovy");

			APPLICATION_OVERRIDEN_PROEPRTIES_MATCHING_FIELDS.put("name", "overridesTestApplicationOverriden");
		}
	}
	
	@Test
	public void testServiceExistingOverridesFile() {
		try {
			testDSLOverrides(SERVICE_PATH, null, Service.class);
		}
		catch (DSLException e) {
			fail("Failed to read service " + SERVICE_PATH + e);
		}
	}
	
	@Test
	public void testServiceExternalOverridesFile() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH, SERVICE_EXTERNAL_OVERRIDES_FILE_PATH, Service.class);
		}
		catch (DSLException e) {
			fail("Failed to read application " + SERVICE_PATH + e);
		}
	}

	@Test
	public void testServiceIllegalOverridesFileFormat() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH, SERVICE_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH, Service.class);
			fail("Expected DSLException for service " + EXTERNAL_OVERRIDES_SERVICE_PATH);
		}
		catch (DSLException e) {
		}
	}

	@Test
	public void testServiceIllegalOverridesProperties() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH, SERVICE_ILLEGAL_OVERRIDES_PROPERTIES_FILE_PATH, Service.class);
			fail("Expected DSLException for service " + EXTERNAL_OVERRIDES_SERVICE_PATH);
		}
		catch (DSLException e) {

		}
	}

	@Test
	public void testServiceIllegalOverridesFields() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_SERVICE_PATH, SERVICE_ILLEGAL_OVERRIDES_FIELDS_FILE_PATH, Service.class);
			fail("Expected DSLException for service " + EXTERNAL_OVERRIDES_SERVICE_PATH);
		}
		catch (DSLException e) {

		}
	}

	@Test
	public void testApplicationExistingOverridesFile() {
		try {
			testDSLOverrides(APPLICATION_PATH, null, Application.class);
		}
		catch (DSLException e) {
			fail("Failed to read application " + APPLICATION_PATH + e);
		}
	}

	@Test
	public void testApplicationExternalOverridesFile() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH, APPLICATION_EXTERNAL_OVERRIDES_FILE_PATH, Application.class);
		}
		catch (DSLException e) {
			fail("Failed to read application " + EXTERNAL_OVERRIDES_APPLICATION_PATH + e);
		}
	}
	
	@Test
	public void testApplicationIllegalOverridesFileFormat() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH, APPLICATION_ILLEGAL_OVERRIDES_FILE_FORMAT_PATH, Application.class);
			fail("Expected DSLException for application " + EXTERNAL_OVERRIDES_APPLICATION_PATH);
		}
		catch (DSLException e) {
		}
	}

	@Test
	public void testApplicationIllegalOverridesProperties() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH, APPLICATION_ILLEGAL_OVERRIDES_PROPERTIES_PATH, Application.class);
			fail("Expected DSLException for application " + EXTERNAL_OVERRIDES_APPLICATION_PATH);
		}
		catch (DSLException e) {

		}
	}
	
	@Test
	public void testApplicationIllegalOverridesFields() {
		try {
			testDSLOverrides(EXTERNAL_OVERRIDES_APPLICATION_PATH, APPLICATION_ILLEGAL_OVERRIDES_FIELDS_PATH, Application.class);
			fail("Expected DSLException for application " + EXTERNAL_OVERRIDES_APPLICATION_PATH);
		}
		catch (DSLException e) {

		}
	}

	private Object testDSLOverrides(String servicePath, String overridesFilePath, Class<?> clazz) throws DSLException {
		File workDir = new File(servicePath);

		File overridesFile = null;
		if(overridesFilePath != null)
			overridesFile = new File(overridesFilePath);
		DSLReader reader = new DSLReader();
		reader.setWorkDir(workDir);
		reader.setRunningInGSC(true);
		if(clazz.equals(Service.class))
			reader.setDslFileNameSuffix(DSLReader.SERVICE_DSL_FILE_NAME_SUFFIX); 
		else 
			reader.setDslFileNameSuffix(DSLReader.APPLICATION_DSL_FILE_NAME_SUFFIX);
		reader.setOverridesFile(overridesFile);
		try{
			Object object = reader.readDslEntity(clazz);
			assertOverrides(object, reader);
			return object;
		} catch (DSLException e) {
			Map<String, Object> overrides = reader.getOverrides();
			Map<String, Object> overrideFields = reader.getOverrideFields();
			Field[] declaredFields = clazz.getDeclaredFields();
			List<String> fields = new LinkedList<String>();
			for (Field field : declaredFields) {
				fields.add(field.getName());
			}
			throw new DSLException("failed to read " + clazz.getName() + ".\n\toverrides map = " + overrides 
					+ "\n\toverride fields = " + overrideFields + "\n\tDeclared fields = " + fields + "\n" + e);
		}
	}


	private void assertOverrides(Object object, DSLReader reader) {
		Set<Entry<String,Object>> entrySet = reader.getOverrideFields().entrySet();
		for (Entry<String, Object> entry : entrySet) {
			try {
				Object fieldValue = PropertyUtils.getNestedProperty(object, entry.getKey());
				Assert.assertEquals(entry.getValue(), fieldValue);
			} catch (Exception e) {	
			}
		}
		try {
			//Object fieldValue = PropertyUtils.getNestedProperty(object, OVERRIDEN_PROEPRTY_NAME);
			//Assert.assertEquals(OVERRIDEN_PROEPRTY_VALUE, fieldValue);
		} catch (Exception e) {	
		}
	}





}
