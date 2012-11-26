package org.cloudifysource.dsl;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.CloudTemplateHolder;
import org.cloudifysource.dsl.internal.CloudTemplatesReader;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.junit.Test;

public class ReadTemplatesFileTest {
	
	private final CloudTemplatesReader reader = new  CloudTemplatesReader();
	
	private static final String TEMPLATES_FILE_PATH = 
			"src/test/resources/externalDSLFiles/templateFiles";
	private static final String NO_UPLOAD_TEMPLATES_FILE_PATH = 
			"src/test/resources/externalDSLFiles/templateFilesWithoutUpload";
	
	private static final String ILLEGAL_TEMPLATES_FILE_PATH = 
			"src/test/resources/externalDSLFiles/illegalMultipleTemplatesInOneFile";
	private static final String ILLEGAL_DUPLICATE_TEMPLATES_FILE_PATH = 
			"src/test/resources/externalDSLFiles/illegalDuplicateTemplates";
	
	
	private static Object[] convertArgsToJaon(final Object[] args) {
		String[] newArgs = new String[args.length];
		if (newArgs.length < 2) {
			return args;
		}
		Map<String, Map<String, String>> failedToAddTemplates = (Map<String, Map<String, String>>) args[0];
		StringBuilder failedToAddTemplatesStr = new StringBuilder("\n{\n");
		for (Entry<String, Map<String, String>> entry : failedToAddTemplates.entrySet()) {
			Map<String, String> failedToAddTemplatesErrDesc = entry.getValue();
			failedToAddTemplatesStr.append(CloudifyConstants.TAB_CHAR)
			.append(entry.getKey())
			.append(":\n")
			.append(CloudifyConstants.TAB_CHAR)
			.append("{\n");
			for (Entry<String, String> tempalteErrDesc : failedToAddTemplatesErrDesc.entrySet()) {
				failedToAddTemplatesStr.append(CloudifyConstants.TAB_CHAR)
				.append(CloudifyConstants.TAB_CHAR)
				.append(tempalteErrDesc.getKey())
				.append(" - ")
				.append(tempalteErrDesc.getValue())
				.append("\n");
			}
			failedToAddTemplatesStr.append(CloudifyConstants.TAB_CHAR)
			.append("}\n");
		}
		failedToAddTemplatesStr.append("}");
		newArgs[0] = failedToAddTemplatesStr.toString();

		Map<String, List<String>> successfullyAddedTemplates = (Map<String, List<String>>) args[1];
		StringBuilder successfullyAddedTemplatesStr = new StringBuilder("\n{\n");
		for (Entry<String, List<String>> entry : successfullyAddedTemplates.entrySet()) {
			successfullyAddedTemplatesStr.append(CloudifyConstants.TAB_CHAR)
			.append(entry.getKey())
			.append(": ")
			.append(entry.getValue())
			.append("\n");
		}
		successfullyAddedTemplatesStr.append("}");
		newArgs[1] = successfullyAddedTemplatesStr.toString();
		
		return newArgs;
	}
	
	@Test
	public void readTemplateFilesFromFolderTest() {	
		readTempaltesTest(TEMPLATES_FILE_PATH);
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
	
	private void readTempaltesTest(final String folderName) {
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
