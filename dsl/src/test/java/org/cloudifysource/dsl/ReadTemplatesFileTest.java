package org.cloudifysource.dsl;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.CloudTemplateHolder;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;

public class ReadTemplatesFileTest {
	
	private static final String TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/tamplteFiles";
	private static final String NO_UPLOAD_TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/tamplteFilesWithoutUpload";
	
	private static final String ILLEGAL_TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/illegalMultipleTemplateFile";
	private static final String ILLEGAL_DUPLICATE_TEMPLATES_FILES_PATH = 
			"src/test/resources/ExternalDSLFiles/illegalDuplicateTemplateFiles";
	private static final String ILLEGAL_TEMPLATE_FILE_NAME_PATH = 
			"src/test/resources/ExternalDSLFiles/illegalTemplateFileName";
	
	
	@Test
	public void readTemplateFilesFromFolderTest() {
		readTempaltesTest(TEMPLATES_FILE_PATH);
	}
	
	@Test
	public void readTemplateFilesFromFolderWithoutUploadTest() {
		readTempaltesTest(NO_UPLOAD_TEMPLATES_FILE_PATH);
	}
	
	@Test
	public void illegalMultipleTemplatesInFileTest() {
		try {
			File templatesFile = new File(ILLEGAL_TEMPLATES_FILE_PATH);
			ServiceReader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.fail("Multiple templates in one file should throw an exception.");
		} catch (Exception e) {
			// Exception was thrown as expected.
		}
	}
	
	@Test
	public void illegalDuplicateTemplatesFilesTest() {
		try {
			File templatesFile = new File(ILLEGAL_DUPLICATE_TEMPLATES_FILES_PATH);
			ServiceReader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.fail("Multiple templates in one file should throw an exception.");
		} catch (Exception e) {
			// Exception was thrown as expected.
		}
	}
	
	@Test
	public void illegalTemplatesFileNameTest() {
		try {
			File templatesFile = new File(ILLEGAL_TEMPLATE_FILE_NAME_PATH);
			ServiceReader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.fail("Multiple templates in one file should throw an exception.");
		} catch (Exception e) {
			// Exception was thrown as expected.
		}
	}
	private static void readTempaltesTest(final String folderName) {
		try {
			File templatesFile = new File(folderName);
			
			List<CloudTemplateHolder> cloudTemplatesFromFile = 
					ServiceReader.readCloudTemplatesFromDirectory(templatesFile);
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
