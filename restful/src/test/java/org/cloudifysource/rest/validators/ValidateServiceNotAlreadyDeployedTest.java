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

import java.util.Iterator;

import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.internal.admin.DefaultAdmin;
import org.openspaces.admin.internal.pu.DefaultProcessingUnits;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnits;

public class ValidateServiceNotAlreadyDeployedTest {

	private static final String PU_NAME = "service.application";
	
	@Test
	public void test() {
		DefaultAdmin adminMock = Mockito.mock(DefaultAdmin.class);
		Iterator<ProcessingUnit> iter = Mockito.mock(Iterator.class);
		ProcessingUnit pu = Mockito.mock(ProcessingUnit.class);
		Mockito.when(pu.getName()).thenReturn(PU_NAME);
		Mockito.when(iter.next()).thenReturn(pu);
		Mockito.when(iter.hasNext()).thenReturn(true, false);
		ProcessingUnits pus = Mockito.mock(DefaultProcessingUnits.class);
		Mockito.when(pus.iterator()).thenReturn(iter);
		Mockito.when(adminMock.getProcessingUnits()).thenReturn(pus);
		
		InstallServiceValidationContext validationContext = new InstallServiceValidationContext();
		validationContext.setAdmin(adminMock);
		validationContext.setPuName(PU_NAME);
		
		ValidatorsTestsUtils.validate(
				new ValidateServiceNotAlreadyDeployed(), 
				validationContext , 
				CloudifyErrorMessages.SERVICE_ALREADY_INSTALLED.getName());
	}

}
