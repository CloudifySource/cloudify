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

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.openspaces.admin.Admin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * validates that if running with high availability, both GSMs are up.
 *
 * @author adaml
 *
 */
@Component
public class ValidateGsmState implements
        InstallServiceValidator,
        UninstallServiceValidator,
        InstallApplicationValidator,
        UninstallApplicationValidator,
        SetServiceInstancesValidator {

    private static final Logger logger = Logger.getLogger(ValidateGsmState.class.getName());


    @Autowired(required = true)
    private Admin admin;

    @Override
    public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
        validateGsmState(validationContext.getCloud());
    }

    @Override
    public void validate(final UninstallServiceValidationContext validationContext) throws RestErrorException {
        validateGsmState(validationContext.getCloud());
    }

    @Override
    public void validate(final InstallApplicationValidationContext validationContext) throws RestErrorException {
        validateGsmState(validationContext.getCloud());
    }

    @Override
    public void validate(final UninstallApplicationValidationContext validationContext)
            throws RestErrorException {
        validateGsmState(validationContext.getCloud());
    }

    @Override
    public void validate(final SetServiceInstancesValidationContext validationContext) throws RestErrorException {
        validateGsmState(validationContext.getCloud());

    }

    void validateGsmState(final Cloud cloud) throws RestErrorException {
        //When a manager is down and persistence is used along with HA, install/uninstall/scaling should be disabled.
        logger.info("Validating Gsm state.");
        if (cloud != null) {
            String persistentStoragePath = cloud.getConfiguration().getPersistentStoragePath();
            if (persistentStoragePath != null) {
                int numManagementMachines = cloud.getProvider().getNumberOfManagementMachines();
                final boolean isGsmStateValid = admin.getGridServiceManagers()
                        .waitFor(numManagementMachines, 10, TimeUnit.SECONDS);
                if (!isGsmStateValid) {
                    int gsmCount = admin.getGridServiceManagers().getManagers().length;
                    logger.warning("Not all gsm instances are intact. Found " + gsmCount);
                    throw new RestErrorException(CloudifyMessageKeys.NOT_ALL_GSM_INSTANCES_RUNNING.getName(),
                            numManagementMachines, gsmCount);
                }
            }
        }
    }



}
