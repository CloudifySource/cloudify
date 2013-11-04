/*******************************************************************************
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
 *******************************************************************************/
package org.cloudifysource.rest.validators;

import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.junit.Before;
import org.junit.Test;

public class ValidateDebugSettingsTest {

    private static final String WRONG_DEBUG_MODE = "no_such_mode";
    private static final String DEBUG_EVENTS = "init,install";
    private static final String DUPLICATE_DEBUG_EVENTS = DEBUG_EVENTS + ",init";
    private static final String WRONG_DEBUG_EVENTS = DEBUG_EVENTS + ", EVENT_NOT_EXIST";
	private ValidateDebugSettings validateDebugSettings;

    @Before
    public void init() {
    	validateDebugSettings = new ValidateDebugSettings();
    }

    // SERVICE
    
    @Test
	public void testWrongDebugEventsAndAllInInstallService() {
    	InstallServiceValidationContext context = new InstallServiceValidationContext();
    	context.setDebugAll(true);
    	context.setDebugEvents(DEBUG_EVENTS);
    	ValidatorsTestsUtils.validate(
    			validateDebugSettings, 
    			context, 
    			CloudifyErrorMessages.DEBUG_EVENTS_AND_ALL_SET.getName());
	}

    @Test
    public void testWrongDebugEventsInInstallService() {
    	InstallServiceValidationContext context = new InstallServiceValidationContext();
    	context.setDebugAll(false);
    	context.setDebugEvents(WRONG_DEBUG_EVENTS);
    	ValidatorsTestsUtils.validate(
    			validateDebugSettings, 
    			context, 
    			CloudifyErrorMessages.DEBUG_EVENT_UNKNOWN.getName());
    }

    @Test
    public void testDuplicateDebugEventsInInstallService() {
    	InstallServiceValidationContext context = new InstallServiceValidationContext();
    	context.setDebugAll(false);
    	context.setDebugEvents(DUPLICATE_DEBUG_EVENTS);
    	ValidatorsTestsUtils.validate(
    			validateDebugSettings, 
    			context, 
    			CloudifyErrorMessages.DEBUG_EVENT_REPEATS.getName());
    }

    @Test
    public void testWrongDebugModeInInstallService() {
    	InstallServiceValidationContext context = new InstallServiceValidationContext();
    	context.setDebugAll(true);
    	context.setDebugMode(WRONG_DEBUG_MODE);
    	ValidatorsTestsUtils.validate(
    			validateDebugSettings, 
    			context, 
    			CloudifyErrorMessages.DEBUG_UNKNOWN_MODE.getName());
    }
    
    // APPLICATION
    
    @Test
	public void testWrongDebugEventsAndAllInInstallApplication() {
    	InstallApplicationValidationContext context = new InstallApplicationValidationContext();
    	context.setDebugAll(true);
    	context.setDebugEvents(DEBUG_EVENTS);
    	ValidatorsTestsUtils.validate(
    			validateDebugSettings, 
    			context, 
    			CloudifyErrorMessages.DEBUG_EVENTS_AND_ALL_SET.getName());
	}

    @Test
    public void testWrongDebugEventsInInstallApplication() {
    	InstallApplicationValidationContext context = new InstallApplicationValidationContext();
    	context.setDebugAll(false);
    	context.setDebugEvents(WRONG_DEBUG_EVENTS);
    	ValidatorsTestsUtils.validate(
    			validateDebugSettings, 
    			context, 
    			CloudifyErrorMessages.DEBUG_EVENT_UNKNOWN.getName());
    }

    @Test
    public void testDuplicateDebugEventsInInstallApplication() {
    	InstallApplicationValidationContext context = new InstallApplicationValidationContext();
    	context.setDebugAll(false);
    	context.setDebugEvents(DUPLICATE_DEBUG_EVENTS);
    	ValidatorsTestsUtils.validate(
    			validateDebugSettings, 
    			context, 
    			CloudifyErrorMessages.DEBUG_EVENT_REPEATS.getName());
    }

    @Test
    public void testWrongDebugModeInInstallApplication() {
    	InstallApplicationValidationContext context = new InstallApplicationValidationContext();
    	context.setDebugAll(true);
    	context.setDebugMode(WRONG_DEBUG_MODE);
    	ValidatorsTestsUtils.validate(
    			validateDebugSettings, 
    			context, 
    			CloudifyErrorMessages.DEBUG_UNKNOWN_MODE.getName());
    }
    
}
