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
package org.cloudifysource.recipes;

import java.io.File;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;


public class CassandraServiceParsingAndReturnFromCommitedRecipesTest {
	
	private final static String LEGAL_RESOURCES_PATH = "target/classes/recipes/services/cassandra/";
	private String nameInGroovy = "cassandra";
	private File cassandraDslFile;
	private File cassandraWorkDir;
	private Service service;
	
	@Test
	public void fullyConfigedGroovy() throws Exception
    {
		cassandraDslFile = new File(LEGAL_RESOURCES_PATH + "cassandra-service.groovy");
		cassandraWorkDir = new File(LEGAL_RESOURCES_PATH);
		service = ServiceReader.getServiceFromFile(cassandraDslFile, cassandraWorkDir).getService();
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
    }
	@Test
	public void getServiceFromDirInvocation() throws Exception
    {
		cassandraWorkDir = new File(LEGAL_RESOURCES_PATH);
		service = ServiceReader.getServiceFromDirectory(cassandraWorkDir, CloudifyConstants.DEFAULT_APPLICATION_NAME).getService();
		ServiceTestUtil.validateName(service , nameInGroovy);
		ServiceTestUtil.validateIcon(service);
    }
}