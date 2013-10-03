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
package org.cloudifysource.usm.commands;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.cloudifysource.domain.context.ServiceContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * 
 * @author adaml
 *
 */
public class BuiltInCommandsTest {
	
	private boolean invoked;
	
	@Before
	public void beforeTest() {
		this.invoked = false;
	}

	@Test
	public void testStartMaintenance() {
		
		StartMaintenanceMode command = new StartMaintenanceMode();
		// create the context mock.
		final ServiceContext contextMock = Mockito.mock(ServiceContext.class);
		// set the mock context in the command.
		command.setContext(contextMock);
		
		// subscribe to the 'startMaintenanceMode' method invocation event.
		Mockito.doAnswer(new Answer<Object>() {
		        public Object answer(final InvocationOnMock invocation) {
		            invoked = true;
		            return null;
		        }
		    }).when(contextMock).startMaintenanceMode(5, TimeUnit.MINUTES);
		
		// invoke the command to trigger the event.
		command.invoke("5");
		Assert.assertTrue("the 'startMaintenanceMode' method was not invoked as expected.", invoked);
		
		// test wrong input type on invocation.
		try {
			command.invoke("string");
		} catch (final IllegalArgumentException e) {
			Assert.assertTrue("expecting exception cause to be 'NumberFormatException'",
					e.getCause() instanceof NumberFormatException);
			Assert.assertTrue("Expecting the following error message: " + e.getMessage(),
					e.getMessage().equals("parameter type mismatch. can't convert class java.lang.String to 'long'"));
		}
		// test wrong number of params in input. 
		try {
			command.invoke(4, 4);
		} catch (IllegalArgumentException e) {
			Assert.assertTrue("Expecting the following error message: " + e.getMessage(), 
					e.getMessage().equals("command start-maintenance-mode requires one "
							+ "param of type 'long', got [4, 4]"));
		}
	}
	
	@Test
	public void testSStopMaintenance() {
		
		StopMaintenanceMode command = new StopMaintenanceMode();
		// create the context mock.
		final ServiceContext contextMock = Mockito.mock(ServiceContext.class);
		// set the mock context in the command.
		command.setContext(contextMock);
		
		// subscribe to the 'startMaintenanceMode' method invocation event.
		Mockito.doAnswer(new Answer<Object>() {
		        public Object answer(final InvocationOnMock invocation) {
		            invoked = true;
		            return null;
		        }
		    }).when(contextMock).stopMaintenanceMode();
		
		// invoke the command to trigger the event.
		command.invoke();
		Assert.assertTrue("the 'startMaintenanceMode' method was not invoked as expected.", invoked);
		
		// test wrong number of params in input. 
		try {
			command.invoke("string");
		} catch (final IllegalArgumentException e) {
			Assert.assertTrue("Expecting the following error message: " + e.getMessage(),
					e.getMessage().equals("command stop-maintenance-mode does not accept parameters."
							+ " received [string]"));
		}
	}

	
}
