package org.cloudifysource.utilitydomain;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.cloudifysource.domain.ComputeTemplateHolder;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.utilitydomain.data.reader.ComputeTemplatesReader;
import org.junit.Assert;
import org.junit.Test;

public class ReadTemplatesFileTest {
	
	private final ComputeTemplatesReader reader = new  ComputeTemplatesReader();
	
	private static final String TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/templateFiles";
	private static final String NO_UPLOAD_TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/templateFilesWithoutUpload";
	
	private static final String ILLEGAL_TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/illegalMultipleTemplatesInOneFile";
	private static final String ILLEGAL_DUPLICATE_TEMPLATES_FILE_PATH = 
			"src/test/resources/ExternalDSLFiles/illegalDuplicateTemplates";
	
	private static final String TEMPLATES_FOLDER_PATH = "src/test/resources/templates";

	
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
		} catch (DSLException e) {
			assertRightError(e.getMessage(), "Could not find upload directory", "linux-template.groovy");
		}
	}

	@Test
	public void readTemplateFilesFromEmptyFolder() {		
		try {
			File templatesFile = File.createTempFile("temp", null);
			templatesFile.delete();
			templatesFile.mkdir();
			reader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.fail("Templates folder missing an upload folder yield no exception.");
		} catch (DSLException e) {
			Assert.assertTrue(e.getMessage().startsWith("There is no template files"));
		} catch (Exception e) {
			Assert.fail("Got " + e.getClass().getName() + " instead of DSLValidationException " 
					+ "(The case is templates folder is empty)");
		}
	}
	
	@Test
	public void illegalMultipleTemplatesInFileTest() {
		try {
			File templatesFile = new File(ILLEGAL_TEMPLATES_FILE_PATH);
			reader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.fail("Multiple templates in one file yielded no exception.");
		} catch (DSLException e) {
			assertRightError(e.getMessage(), "Too many templates in one groovy file", "multiple-template.groovy");
		}
	}
	
	@Test
	public void illegalDuplicateTemplatesFilesTest() {
		try {
			File templatesFile = new File(ILLEGAL_DUPLICATE_TEMPLATES_FILE_PATH);
			reader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.fail("Duplicate templates yielded no exception.");
		} catch (DSLException e) {
			assertRightError(e.getMessage(), "template with the name [TOMCAT] already exist in folder", "tomcat-template.groovy");
		}
	}
	
	@Test
	public void illegalFileTransferDeclarationTest() {
		try {
			File templatesFolder = new File(TEMPLATES_FOLDER_PATH);
			ComputeTemplatesReader reader = new  ComputeTemplatesReader();
			reader.readCloudTemplatesFromDirectory(templatesFolder);
			Assert.fail("Folder with illegal template yielded no exception.");
		} catch (Exception e) {
			assertRightError(e.getMessage(), "Could not resolve DSL entry with name: org", "wrongFileTransferPackage-template.groovy");
		}
	}
	
	private void readTemplatesTest(final String folderName) {
		try {
			File templatesFile = new File(folderName);
			
			List<ComputeTemplateHolder> cloudTemplatesFromFile = reader.readCloudTemplatesFromDirectory(templatesFile);
			Assert.assertEquals(2, cloudTemplatesFromFile.size());
			List<String> names = new LinkedList<String>();
			for (ComputeTemplateHolder cloudTemplateHolder : cloudTemplatesFromFile) {
				names.add(cloudTemplateHolder.getName());
			}
			Assert.assertTrue(names.contains("SMALL_LINUX"));
			Assert.assertTrue(names.contains("TOMCAT"));
		} catch (Exception e) {
			Assert.fail("failed to read templates from file " + TEMPLATES_FILE_PATH 
					+ " error message is " + e.getMessage());
		}
	}
	
	private void assertRightError(final String message, final String errMsgContains, final String templateFileName) {
		Assert.assertTrue(message.startsWith("Failed to read template file [" + templateFileName + "] from folder"));
		Assert.assertTrue(message.contains(errMsgContains));
	}
}
