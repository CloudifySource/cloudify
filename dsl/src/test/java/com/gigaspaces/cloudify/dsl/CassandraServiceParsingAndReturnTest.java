package com.gigaspaces.cloudify.dsl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.gigaspaces.cloudify.dsl.internal.ServiceReader;

public class CassandraServiceParsingAndReturnTest {
	
	private final static String CORRUPTED_RESOURCES_PATH = "testResources/cassandra/";
	private String nameInGroovy = "cassandra";

	@Test
	public void minConfigedGroovy()throws Exception
	{
		File cassandraDslFile = new File(CORRUPTED_RESOURCES_PATH + "cassandra_bare_essentials-service.groovy");
		File cassandraWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		Service service = ServiceReader.getServiceFromFile(cassandraDslFile, cassandraWorkDir).getService();
		assertNotNull(service);
		ServiceTestUtil.validateName(service , nameInGroovy);
	}
	@Test
	public void uiOmmitedConfigGroovy() throws Exception
	{
		File cassandraDslFile = new File(CORRUPTED_RESOURCES_PATH + "cassandra_UI_ommited-service.groovy");
		File cassandraWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		Service service = ServiceReader.getServiceFromFile(cassandraDslFile, cassandraWorkDir).getService();
		assertNotNull(service);
		assertNull(service.getUserInterface());
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
	}
	@Test
	public void pluginsConfigFieldCorruptedGroovy()
	{
		File cassandraDslFile = new File(CORRUPTED_RESOURCES_PATH + "cassandra_plugins_config_field_corrupted-service.groovy");
		File cassandraWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		try{
			Service service = ServiceReader.getServiceFromFile(cassandraDslFile, cassandraWorkDir).getService();
			assertTrue("No exception was thrown due to Config field corruption" , false);
		}catch(Throwable t){
			// getServiceFromFile should throw something informative due to corruption
			assertTrue("Throwable isn't informative enought" , t.getMessage().toLowerCase().contains("confg"));
		}		
	}
	@Test	
	public void nameFieldCorruptedGroovy()
	{
		File cassandraDslFile = new File(CORRUPTED_RESOURCES_PATH + "cassandra_name_field_corrupted-service.groovy");
		File cassandraWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		try{
			Service service = ServiceReader.getServiceFromFile(cassandraDslFile, cassandraWorkDir).getService();
			assertTrue("No exception was thrown due to Name field corruption" , false);
		}catch(Throwable t){
			// getServiceFromFile should throw something informative due to corruption
			assertTrue("Throwable isn't informative enought" , t.getMessage().toLowerCase().contains("nae"));
		}		
	}
	@Test
	public void axisYUnitFieldCorruptedGroovy()
	{
		File cassandraDslFile = new File(CORRUPTED_RESOURCES_PATH + "cassandra_axisYUnit_field_corrupted-service.groovy");
		File cassandraWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		try{
			Service service = ServiceReader.getServiceFromFile(cassandraDslFile, cassandraWorkDir).getService();
			assertTrue("No exception was thrown due to AxisYUnit field corruption" , false);
		}catch(Throwable t){
			// getServiceFromFile should throw something informative due to corruption
			assertTrue("Throwable isn't informative enought" , t.getMessage().toLowerCase().contains("axiszunit"));
		}		
	}
	@Test
	public void userInterfaceFieldCorruptedGroovy()
	{
		File cassandraDslFile = new File(CORRUPTED_RESOURCES_PATH + "cassandra_UserInterface_field_corrupted-service.groovy");
		File cassandraWorkDir = new File(CORRUPTED_RESOURCES_PATH);
		try{
			Service service = ServiceReader.getServiceFromFile(cassandraDslFile, cassandraWorkDir).getService();
			assertTrue("No exception was thrown due to UserInterface field corruption" , false);
		}catch(Throwable t){
			// getServiceFromFile should throw something informative due to corruption
			assertTrue("Throwable isn't informative enought" , t.getMessage().toLowerCase().contains("ui"));
		}		
	}
}
