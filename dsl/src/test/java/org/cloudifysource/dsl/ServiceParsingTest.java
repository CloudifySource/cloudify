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

import java.io.File;

import junit.framework.Assert;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.ServiceLifecycle;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.junit.Test;


public class ServiceParsingTest {
	
	private static final String TEST_PARSING_RESOURCE_PATH = "testResources/testparsing/";

	@Test
	public void testBasicParsing() throws DSLException{
		File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH + "test_parsing_base-service.groovy");
		File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		Service service = ServiceReader.getServiceFromFile(testParsingBaseDslFile, testParsingBaseWorkDir).getService();
		Assert.assertEquals("test parsing base", service.getName());
		ServiceLifecycle lifecycle = service.getLifecycle();
		Assert.assertEquals("test_parsing_base_install.groovy", lifecycle.getInit());
		Assert.assertNotNull(lifecycle.getStart());
		Assert.assertNotNull(lifecycle.getPostStart());
		Assert.assertNotNull(lifecycle.getPreStop());
	}
	
	@Test
	public void testBasicExtendParsing() throws DSLException{
		
		File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH + "test_parsing_base-service.groovy");
		File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		Service baseService = ServiceReader.getServiceFromFile(testParsingBaseDslFile, testParsingBaseWorkDir).getService();
		Assert.assertFalse(baseService.getName().equals("test parsing extend"));
		ServiceLifecycle baseLifecycle = baseService.getLifecycle();
		Assert.assertFalse(baseLifecycle.getInit().equals("test_parsing_extend_install.groovy"));
		Assert.assertNull(baseLifecycle.getStop());
		
		File testParsingExtendDslFile = new File(TEST_PARSING_RESOURCE_PATH + "test_parsing_extend-service.groovy");
		File testParsingExtendWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		Service service = ServiceReader.getServiceFromFile(testParsingExtendDslFile, testParsingExtendWorkDir).getService();
		Assert.assertEquals("test parsing extend", service.getName());
		ServiceLifecycle lifecycle = service.getLifecycle();
		Assert.assertEquals("test_parsing_extend_install.groovy", lifecycle.getInit());
		Assert.assertNotNull(lifecycle.getStart());
		Assert.assertNotNull(lifecycle.getPostStart());
		Assert.assertNotNull(lifecycle.getPreStop());
		Assert.assertNotNull(lifecycle.getStop());
		Assert.assertEquals(1, service.getExtendedServicesPaths().size());
		Assert.assertEquals("test_parsing_base-service.groovy", service.getExtendedServicesPaths().getFirst());
	}
	
	@Test
	public void testBasicExtendIllegalPropertyLocation() {
		File testParsingExtendDslFile = new File(TEST_PARSING_RESOURCE_PATH + "test_parsing_extend_illegal-service.groovy");
		File testParsingExtendWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		try{		
			ServiceReader.getServiceFromFile(testParsingExtendDslFile, testParsingExtendWorkDir).getService();
			Assert.fail("No exception thrown while extend resides in illegal place");
		}catch(Exception e){
			
		}
	}
	
	@Test
	public void testBasicExtendIllegalNestedPropertyLocation() {
		File testParsingExtendDslFile = new File(TEST_PARSING_RESOURCE_PATH + "test_parsing_extend_illegal_nested-service.groovy");
		File testParsingExtendWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		try{		
			ServiceReader.getServiceFromFile(testParsingExtendDslFile, testParsingExtendWorkDir).getService();
			Assert.fail("No exception thrown while extend resides in illegal place");
		}catch(Exception e){
			
		}
	}
	
	@Test
	public void testTwoLevelExtension() throws DSLException {
		File testParsingBaseDslFile = new File(TEST_PARSING_RESOURCE_PATH + "test_parsing_base-service.groovy");
		File testParsingBaseWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		Service baseService = ServiceReader.getServiceFromFile(testParsingBaseDslFile, testParsingBaseWorkDir).getService();
		Assert.assertFalse(baseService.getName().equals("test parsing extend"));
		ServiceLifecycle baseLifecycle = baseService.getLifecycle();
		Assert.assertFalse(baseLifecycle.getInit().equals("test_parsing_extend_install.groovy"));
		Assert.assertNull(baseLifecycle.getStop());
		Assert.assertFalse(baseLifecycle.getStart().equals("start"));
		
		File testParsingExtendDslFile = new File(TEST_PARSING_RESOURCE_PATH + "test_parsing_extend_two_level-service.groovy");
		File testParsingExtendWorkDir = new File(TEST_PARSING_RESOURCE_PATH);
		Service service = ServiceReader.getServiceFromFile(testParsingExtendDslFile, testParsingExtendWorkDir).getService();
		Assert.assertEquals("test parsing extend two level", service.getName());
		ServiceLifecycle lifecycle = service.getLifecycle();
		Assert.assertEquals("test_parsing_extend_install.groovy", lifecycle.getInit());
		Assert.assertNotNull(lifecycle.getStart());
		Assert.assertNotNull(lifecycle.getPostStart());
		Assert.assertNotNull(lifecycle.getPreStop());
		Assert.assertNotNull(lifecycle.getStop());
		Assert.assertEquals("install", lifecycle.getInstall());
		Assert.assertEquals("start", lifecycle.getStart());
		Assert.assertEquals(2, service.getExtendedServicesPaths().size());
		Assert.assertEquals("test_parsing_extend-service.groovy", service.getExtendedServicesPaths().getFirst());
		Assert.assertEquals("test_parsing_base-service.groovy", service.getExtendedServicesPaths().getLast());
	}
}
