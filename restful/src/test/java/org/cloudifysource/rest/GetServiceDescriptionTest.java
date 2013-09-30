/*
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * *****************************************************************************
 */
package org.cloudifysource.rest;

import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.InstanceDescription;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.rest.util.ApplicationDescriptionFactory;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.internal.machine.DefaultMachine;
import org.openspaces.admin.internal.pu.DefaultProcessingUnit;
import org.openspaces.admin.internal.vm.DefaultVirtualMachine;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitType;
import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.core.properties.BeanLevelProperties;

/**
 * 
 * @author yael
 * 
 */
public class GetServiceDescriptionTest {

	@Test
	public void testUSMStateNotAvailable() {

		final ApplicationDescriptionFactory factory = new ApplicationDescriptionFactory(null);

		final ProcessingUnit mockProcessingUnit = Mockito.mock(DefaultProcessingUnit.class);
	
		// mockProcessingUnitInstance
		final ProcessingUnitInstance mockProcessingUnitInstance = Mockito.mock(ProcessingUnitInstance.class);
		Mockito.when(mockProcessingUnitInstance.getProcessingUnit()).thenReturn(mockProcessingUnit);
		Mockito.when(mockProcessingUnitInstance.getProcessingUnit()).thenReturn(mockProcessingUnit);
		VirtualMachine mockVirtualMachine = Mockito.mock(DefaultVirtualMachine.class);
		Machine mockMachine = Mockito.mock(DefaultMachine.class);
		Mockito.when(mockMachine.getHostName()).thenReturn("localhost");
		Mockito.when(mockMachine.getHostAddress()).thenReturn("localhost");
		Mockito.when(mockVirtualMachine.getMachine()).thenReturn(mockMachine);
		Mockito.when(mockProcessingUnitInstance.getVirtualMachine()).thenReturn(mockVirtualMachine);

		// mockProcessingUnit
		final ProcessingUnitInstance[] puis = { mockProcessingUnitInstance };
		Mockito.when(mockProcessingUnit.getInstances()).thenReturn(puis);
		Mockito.when(mockProcessingUnit.iterator()).thenCallRealMethod();
		Mockito.when(mockProcessingUnit.getType()).thenReturn(ProcessingUnitType.UNIVERSAL);
		Mockito.when(mockProcessingUnit.getName()).thenReturn("default.tomcat");
		final BeanLevelProperties blp = Mockito.mock(BeanLevelProperties.class);
		final Properties contextProperties = new Properties();
		contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEPLOYMENT_ID, "12345");
		Mockito.when(blp.getContextProperties()).thenReturn(contextProperties);
		Mockito.when(mockProcessingUnit.getBeanLevelProperties()).thenReturn(blp);
		Mockito.when(mockProcessingUnit.getNumberOfInstances()).thenReturn(1);

		// asserts
		ServiceDescription result = factory.getServiceDescription(mockProcessingUnit);
		Assert.assertEquals("default", result.getApplicationName());
		Assert.assertEquals("12345", result.getDeploymentId());
		Assert.assertEquals(0, result.getInstanceCount());
		Assert.assertEquals(1, result.getPlannedInstances());
		Assert.assertEquals("tomcat", result.getServiceName());
		List<InstanceDescription> instancesDescription = result.getInstancesDescription();
		Assert.assertEquals(1, instancesDescription.size());
		InstanceDescription instanceDescription = instancesDescription.get(0);
		Assert.assertNotNull(instanceDescription);
		Assert.assertEquals("localhost", instanceDescription.getHostAddress());
		Assert.assertEquals("localhost", instanceDescription.getHostName());
		Assert.assertEquals("NA", instanceDescription.getInstanceStatus());
	}

}
