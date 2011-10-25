package org.openspaces.cloud.azure;

import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            return waitForStatus(AzureDeploymentStatus.Running, statusTimeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Assert.fail("Failed deploying: " + e.getLocalizedMessage());
        }
        return false;
    }
    
    public boolean stop() {
        try {
            azureConfig.updateDeployment(hostedService, slot, AzureDeploymentStatus.Suspended);
            return waitForStatus(AzureDeploymentStatus.Suspended, statusTimeoutInMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Assert.fail("Failed stopping: " + e.getLocalizedMessage());
        }
        return false;
    }
    
    public boolean delete() {
        try {
            azureConfig.deleteDeployment(hostedService, slot);
            return waitForStatus(AzureDeploymentStatus.NotFound, statusTimeoutInMillis, TimeUnit.MILLISECONDS);
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
            output = output.trim();
            return output;
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
