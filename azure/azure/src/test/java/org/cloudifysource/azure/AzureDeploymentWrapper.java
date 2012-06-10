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
package org.cloudifysource.azure;

import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.azure.AzureDeployment;
import org.cloudifysource.azure.AzureDeploymentStatus;
import org.cloudifysource.azure.AzureSlot;

import junit.framework.Assert;

public class AzureDeploymentWrapper {

    private static final Logger logger = Logger.getLogger(AzureDeploymentWrapper.class.getName());
    
    private static final long DEFAULT_STATUS_POLLING_INTERVAL_IN_MILLIS = 1000;
    private static final long DEFAULT_STATUS_TIMEOUT_IN_MILLIS = 1800000;
    
    private long statusTimeoutInMillis = DEFAULT_STATUS_TIMEOUT_IN_MILLIS;
    private long statusPollingIntervalInMillis = DEFAULT_STATUS_POLLING_INTERVAL_IN_MILLIS;
    private String hostedService;
    private AzureSlot slot;
    private String name;
    private String label;
    private File cscfgFile;
    private URI packageUri;
    private AzureDeployment azureConfig;
    
    public AzureDeploymentWrapper(
        File azureConfigExe,	
        String subscriptionId,
        String certificateThumbprint,
        String hostedService,
        AzureSlot slot,
        String name,
        String label,
        File cscfgFile,
        URI packageUri        
    ) {
        azureConfig = new AzureDeployment(azureConfigExe, subscriptionId, certificateThumbprint, true);
        this.hostedService = hostedService;
        this.slot = slot;
        this.name = name;
        this.label = label;
        this.cscfgFile = cscfgFile;
        this.packageUri = packageUri;
    }
    
    public boolean deploy() {
        try {
            azureConfig.createDeployment(hostedService, name, label, slot, cscfgFile, packageUri);
            return waitForStatus(AzureDeploymentStatus.RUNNING, statusTimeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Assert.fail("Failed deploying: " + e.getLocalizedMessage());
        }
        return false;
    }
    
    public boolean stop() {
        try {
            azureConfig.updateDeployment(hostedService, slot, AzureDeploymentStatus.SUSPENDED);
            return waitForStatus(AzureDeploymentStatus.SUSPENDED, statusTimeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Assert.fail("Failed stopping: " + e.getLocalizedMessage());
        }
        return false;
    }
    
    public boolean delete() {
        try {
            azureConfig.deleteDeployment(hostedService, slot);
            return waitForStatus(AzureDeploymentStatus.NOT_FOUND, statusTimeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Assert.fail("Failed deleting: " + e.getLocalizedMessage());
        }
        return false;
    }
    
    public AzureDeploymentStatus getStatus() {
        try {
            return azureConfig.getDeploymentStatus(hostedService, slot);
        } catch (Exception e) {
            Assert.fail("Failed getting status: " + e.getLocalizedMessage());
        }
        return null;
    }

    public String getUrl() {
        try {
            String output = azureConfig.getDeploymentUrl(hostedService, slot);
            return output.trim();
        } catch (Exception e) {
            Assert.fail("Failed getting url: " + e.getLocalizedMessage());
        }
        return null;
    }
    
    public boolean waitForStatus(AzureDeploymentStatus status, long timeout, TimeUnit timeUnit) {
        long timeoutInMillis = timeUnit.toMillis(timeout);
        long end = System.currentTimeMillis() + timeoutInMillis;
        
        AzureDeploymentStatus currentStatus = getStatus();
        
        while(currentStatus != status && System.currentTimeMillis() < end) {
            logger.log(Level.INFO, "Current status: " + currentStatus.getStatus());
            try {
                Thread.sleep(statusPollingIntervalInMillis);
            } catch (InterruptedException e) {

            }
            currentStatus = getStatus();
        }
        
        return currentStatus == status;
    }

    public void setStatusTimeoutInMillis(long operationTimeoutInMillis) {
        this.statusTimeoutInMillis = operationTimeoutInMillis;
    }

    public long getStatusTimeoutInMillis() {
        return statusTimeoutInMillis;
    }

    public void setStatusPollingIntervalInMillis(
            long statusPollingIntervalInMillis) {
        this.statusPollingIntervalInMillis = statusPollingIntervalInMillis;
    }

    public long getStatusPollingIntervalInMillis() {
        return statusPollingIntervalInMillis;
    }
    
}
