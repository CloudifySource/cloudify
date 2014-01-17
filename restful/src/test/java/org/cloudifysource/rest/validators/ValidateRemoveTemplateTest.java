/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.validators;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.CloudCompute;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.internal.admin.DefaultAdmin;
import org.openspaces.admin.internal.pu.DefaultProcessingUnits;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.core.properties.BeanLevelProperties;

/**
 * 
 * @author yael
 *
 */
public class ValidateRemoveTemplateTest {
	private static final String DEFAULT_TEMPLATE = "SMALL_LINUX";
	private static final String TEMPLATE = "TEMPLATE";
	private Cloud cloud;
	private List<String> cloudDeclaredTemplates;
	
	@Before
	public void init() {
		cloud = new Cloud();
		CloudCompute cloudCompute = new CloudCompute();
		Map<String, ComputeTemplate> templates = new HashMap<String, ComputeTemplate>();
		templates.put(DEFAULT_TEMPLATE, new ComputeTemplate());
		templates.put(TEMPLATE, new ComputeTemplate());
		cloudCompute.setTemplates(templates);
		cloud.setCloudCompute(cloudCompute);
		cloudDeclaredTemplates = new LinkedList<String>();
		cloudDeclaredTemplates.add(DEFAULT_TEMPLATE);
	}
	
	@Test
	public void testValidateRemoveTemplateIsNotDefault() {
		RemoveTemplatesValidationContext validationContext = new RemoveTemplatesValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setOperationName("remove-templates");
		validationContext.setTemplateName(DEFAULT_TEMPLATE);
		validationContext.setCloudDeclaredTemplates(cloudDeclaredTemplates);

		ValidateRemoveTemplate validator = new ValidateRemoveTemplate();
		try {
			validator.validate(validationContext);
			Assert.fail("RestErrorException expected");
		} catch (RestErrorException e) {
			Assert.assertEquals(CloudifyErrorMessages.ILLEGAL_REMOVE_DEFAULT_TEMPLATE.getName(), e.getMessage());
		}
	}
	
	@Test
	public void testValidateRemoveTemplateExists() {
		RemoveTemplatesValidationContext validationContext = new RemoveTemplatesValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setOperationName("remove-templates");
		validationContext.setTemplateName("NOT_EXIST");

		ValidateRemoveTemplate validator = new ValidateRemoveTemplate();
		try {
			validator.validate(validationContext);
			Assert.fail("RestErrorException expected");
		} catch (RestErrorException e) {
			Assert.assertEquals(CloudifyErrorMessages.TEMPLATE_NOT_EXIST.getName(), e.getMessage());
		}
	}
	
	@Test
	public void testValidateTemplateNotInUse() {
		RemoveTemplatesValidationContext validationContext = new RemoveTemplatesValidationContext();
		validationContext.setCloud(cloud);
		validationContext.setOperationName("remove-templates");
		validationContext.setTemplateName(TEMPLATE);

		
		DefaultAdmin admin = Mockito.mock(DefaultAdmin.class);
		DefaultProcessingUnits mockedPus = Mockito.mock(DefaultProcessingUnits.class);
		ProcessingUnit mockedPu = Mockito.mock(ProcessingUnit.class);
		Mockito.when(mockedPu.getName()).thenReturn("SERVICE");
		BeanLevelProperties mockedBlp = Mockito.mock(BeanLevelProperties.class);
		Properties props = new Properties();
		props.setProperty(CloudifyConstants.CONTEXT_PROPERTY_TEMPLATE, TEMPLATE);
		Mockito.when(mockedBlp.getContextProperties()).thenReturn(props);
		Mockito.when(mockedPu.getBeanLevelProperties()).thenReturn(mockedBlp);
		
		Mockito.when(admin.getProcessingUnits()).thenReturn(mockedPus);
		ProcessingUnit[] pus = {mockedPu};
		Iterator<ProcessingUnit> pusIterator = new ArrayIterator(pus);
		Mockito.when(mockedPus.iterator()).thenReturn(pusIterator);
		
		
		validationContext.setAdmin(admin);
		validationContext.setCloudDeclaredTemplates(cloudDeclaredTemplates);

		ValidateRemoveTemplate validator = new ValidateRemoveTemplate();
		try {
			validator.validate(validationContext);
			Assert.fail("RestErrorException expected");
		} catch (RestErrorException e) {
			Assert.assertEquals(CloudifyErrorMessages.TEMPLATE_IN_USE.getName(), e.getMessage());
		}
	}
}
