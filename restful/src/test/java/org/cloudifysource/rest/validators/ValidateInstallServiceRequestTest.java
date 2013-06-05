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
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.junit.Ignore;
import org.junit.Test;

public class ValidateInstallServiceRequestTest extends InstallServiceValidatorTest {

    private static final String UPLOAD_KEY = "key";

    private static final String WRONG_DEBUG_MODE = "no_such_mode";
    private static final String DEBUG_EVENTS = "init,install";
    private static final String DUPLICATE_DEBUG_EVENTS = DEBUG_EVENTS + ",init";
    private static final String WRONG_DEBUG_EVENTS = DEBUG_EVENTS + ", EVENT_NOT_EXIST";


    @Ignore
    @Test
    public void testMissingUploadKey() {
        final InstallServiceRequest request = new InstallServiceRequest();
        testValidator(request, CloudifyMessageKeys.UPLOAD_KEY_PARAMETER_MISSING.getName());
    }


    @Test
    public void testWrongDebugEventsAndAll() {
        final InstallServiceRequest request = new InstallServiceRequest();
        request.setServiceFolderUploadKey(UPLOAD_KEY);
        request.setDebugAll(true);
        request.setDebugEvents(DEBUG_EVENTS);
        testValidator(request, CloudifyErrorMessages.DEBUG_EVENTS_AND_ALL_SET.getName());
    }

    @Test
    public void testWrongDebugEvents() {
        final InstallServiceRequest request = new InstallServiceRequest();
        request.setServiceFolderUploadKey(UPLOAD_KEY);
        request.setDebugAll(false);
        request.setDebugEvents(WRONG_DEBUG_EVENTS);
        testValidator(request, CloudifyErrorMessages.DEBUG_EVENT_UNKNOWN.getName());
    }

    @Test
    public void testDuplicateDebugEvents() {
        final InstallServiceRequest request = new InstallServiceRequest();
        request.setServiceFolderUploadKey(UPLOAD_KEY);
        request.setDebugAll(false);
        request.setDebugEvents(DUPLICATE_DEBUG_EVENTS);
        testValidator(request, CloudifyErrorMessages.DEBUG_EVENT_REPEATS.getName());
    }

    @Test
    public void testWrongDebugMode() {
        final InstallServiceRequest request = new InstallServiceRequest();
        request.setServiceFolderUploadKey(UPLOAD_KEY);
        request.setDebugAll(true);
        request.setDebugMode(WRONG_DEBUG_MODE);
        testValidator(request, CloudifyErrorMessages.DEBUG_UNKNOWN_MODE.getName());
    }

    @Override
    public InstallServiceValidator getValidatorInstance() {
        return new ValidateInstallServiceRequest();
    }

}
