package com.gigaspaces.cloudify.recipes;

import java.io.File;

import org.junit.Test;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;

public class TomcatServiceParsingAndReturnFromCommitedRecipesTest {
	
	private final static String LEGAL_RESOURCES_PATH = "target/classes/tomcat/";
	private String nameInGroovy = "tomcat";
	private File tomcatDslFile;
	private File tomcatWorkDir;
	private Service service;
	
	@Test
	public void fullyConfigedGroovy() throws Exception
    {
		tomcatDslFile = new File(LEGAL_RESOURCES_PATH + "tomcat-service.groovy");
		tomcatWorkDir = new File(LEGAL_RESOURCES_PATH);
		service = ServiceReader.getServiceFromFile(tomcatDslFile, tomcatWorkDir, CloudifyConstants.DEFAULT_APPLICATION_NAME).getService();
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
    }
	
	@Test
	public void getServiceFromDirInvocation() throws Exception
    {
		tomcatWorkDir = new File(LEGAL_RESOURCES_PATH);
		service = ServiceReader.getServiceFromDirectory(tomcatWorkDir, CloudifyConstants.DEFAULT_APPLICATION_NAME).getService();
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
    }
	
}