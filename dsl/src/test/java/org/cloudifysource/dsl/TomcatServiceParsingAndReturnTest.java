package org.cloudifysource.dsl;

import static org.junit.Assert.*;

import java.io.File;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;


public class TomcatServiceParsingAndReturnTest{
	
	private final static String CORRUPTED_RESOURCES_PATH = "testResources/tomcat/";
	private String nameInGroovy = "tomcat";
	
	@Test
	public void sample() throws Exception
	{
		return;
	}
	
	@Test
	public void uiOmmitedConfigGroovy() throws Exception
	{
		File tomcatDslFile = new File(CORRUPTED_RESOURCES_PATH + "tomcat_UI_ommited-service.groovy");
		File tomcatWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		Service service = ServiceReader.getServiceFromFile(tomcatDslFile, tomcatWorkDir).getService();
		assertNotNull(service);
		assertNull(service.getUserInterface());
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
	}
	@Test
	public void networkPortValueIsStringGroovy()
	{
		File tomcatDslFile = new File(CORRUPTED_RESOURCES_PATH + "tomcat_network_port_value_is_string-service.groovy");
		File tomcatWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		try{
			Service service = ServiceReader.getServiceFromFile(tomcatDslFile, tomcatWorkDir).getService();
			
		}catch(Throwable t){
			// getServiceFromFile should throw something informative due to corruption
			assertTrue("Throwable isn't informative enought" , t.getMessage().toLowerCase().contains("port"));
		}		
	}
	@Test
	public void nameFieldCorruptedGroovy()
	{
		File tomcatDslFile = new File(CORRUPTED_RESOURCES_PATH + "tomcat_name_field_corrupted-service.groovy");
		File tomcatWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		try{
			Service service = ServiceReader.getServiceFromFile(tomcatDslFile, tomcatWorkDir).getService();
			
		}catch(Throwable t){
			// getServiceFromFile should throw something informative due to corruption
			assertTrue("Throwable isn't informative enought" , t.getMessage().toLowerCase().contains("nae"));
		}		
	}
	@Test
	public void pluginsConfigFieldCorruptedGroovy()
	{
		File tomcatDslFile = new File(CORRUPTED_RESOURCES_PATH + "tomcat_plugins_config_field_corrupted-service.groovy");
		File tomcatWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		try{
			Service service = ServiceReader.getServiceFromFile(tomcatDslFile, tomcatWorkDir).getService();
			assertTrue("No exception was thrown due to Config field corruption" , false);
		}catch(Throwable t){
			// getServiceFromFile should throw something informative due to corruption
			assertTrue("Throwable isn't informative enought" , t.getMessage().toLowerCase().contains("confg"));
		}		
	}
	@Test
	public void axisYUnitFieldCorruptedGroovy()
	{
		File tomcatDslFile = new File(CORRUPTED_RESOURCES_PATH + "tomcat_axisYUnit_field_corrupted-service.groovy");
		File tomcatWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		try{
			Service service = ServiceReader.getServiceFromFile(tomcatDslFile, tomcatWorkDir).getService();
			
		}catch(Throwable t){
			// getServiceFromFile should throw something informative due to corruption
			assertTrue("Throwable isn't informative enought" , t.getMessage().toLowerCase().contains("axiszunit"));
		}		
	}
	@Test
	public void userInterfaceFieldCorruptedGroovy()
	{
		File tomcatDslFile = new File(CORRUPTED_RESOURCES_PATH + "tomcat_UserInterface_field_corrupted-service.groovy");
		File tomcatWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		try{
			Service service = ServiceReader.getServiceFromFile(tomcatDslFile, tomcatWorkDir).getService();
			
		}catch(Throwable t){
			// getServiceFromFile should throw something informative due to corruption
			assertTrue("Throwable isn't informative enought" , t.getMessage().toLowerCase().contains("ui"));
		}		
	}
}
