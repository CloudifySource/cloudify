package org.cloudifysource.dsl;

import java.io.File;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

public class AddTemplatesTest {
	
	private static final String TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/templates.groovy";
	
	private static final String TEMPLATE_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/template.groovy";

	@Test
	public void createCloudTemplatesTest() {
		try {
			File templatesFile = new File(TEMPLATES_FILE_PATH);
			ServiceReader.getCloudTemplatesFromFile(templatesFile);
		} catch (DSLException e) {
			Assert.fail("failed to read templates from file " + TEMPLATES_FILE_PATH 
					+ " error message is " + e.getMessage());
		}
		try {
			File templateFile = new File(TEMPLATE_FILE_PATH);
			ServiceReader.getCloudTemplatesFromFile(templateFile);
		} catch (DSLException e) {
			Assert.fail("failed to read template from file " + TEMPLATE_FILE_PATH 
					+ " error message is " + e.getMessage());
		}
	}
}
