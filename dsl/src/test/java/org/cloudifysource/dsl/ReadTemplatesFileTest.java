package org.cloudifysource.dsl;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.CloudTemplateHolder;
import org.cloudifysource.dsl.internal.CloudTemplatesReader;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.junit.Test;

public class ReadTemplatesFileTest {
	
	private final CloudTemplatesReader reader = new  CloudTemplatesReader();
	
	private static final String TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/templateFiles";
	private static final String NO_UPLOAD_TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/templateFilesWithoutUpload";
	
	private static final String ILLEGAL_TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/illegalMultipleTemplatesInOneFile";
	private static final String ILLEGAL_DUPLICATE_TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/illegalDuplicateTemplates";
	
	@Test
	public void readTemplateFilesFromFolderTest() {	
		readTemplatesTest(TEMPLATES_FILE_PATH);
	}
	
	@Test
	public void readTemplateFilesFromFolderWithoutUploadTest() {		
		try {
			File templatesFile = new File(NO_UPLOAD_TEMPLATES_FILE_PATH);
			reader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.fail("Templates folder missing an upload folder yield no exception.");
		} catch (DSLValidationException e) {
			Assert.assertTrue(e.getMessage().startsWith("Could not find upload directory"));
		} catch (DSLException e) {
			// TODO Auto-generated catch block
			Assert.fail("Got DSLException instead of DSLValidationException " 
					+ "(The case is templates folder missing an upload folder)");
		}
	}
	
	@Test
	public void illegalMultipleTemplatesInFileTest() {
		try {
			File templatesFile = new File(ILLEGAL_TEMPLATES_FILE_PATH);
			reader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.fail("Multiple templates in one file yielded no exception.");
		} catch (DSLException e) {
			Assert.assertTrue(e.getMessage().
					startsWith("Too many templates in one groovy file"));
		}
	}
	
	@Test
	public void illegalDuplicateTemplatesFilesTest() {
		try {
			File templatesFile = new File(ILLEGAL_DUPLICATE_TEMPLATES_FILE_PATH);
			reader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.fail("Duplicate templates yielded no exception.");
		} catch (DSLException e) {
			Assert.assertTrue(e.getMessage().startsWith("Template with name [TOMCAT] already exist in folder"));
		}
	}	
	
	private void readTemplatesTest(final String folderName) {
		try {
			File templatesFile = new File(folderName);
			
			List<CloudTemplateHolder> cloudTemplatesFromFile = reader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.assertEquals(2, cloudTemplatesFromFile.size());
			List<String> names = new LinkedList<String>();
			for (CloudTemplateHolder cloudTemplateHolder : cloudTemplatesFromFile) {
				names.add(cloudTemplateHolder.getName());
			}
			Assert.assertTrue(names.contains("SMALL_LINUX"));
			Assert.assertTrue(names.contains("TOMCAT"));
		} catch (Exception e) {
			Assert.fail("failed to read templates from file " + TEMPLATES_FILE_PATH 
					+ " error message is " + e.getMessage());
		}
	}
}
