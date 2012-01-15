/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;


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
