/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gigaspaces.azure.deploy.commands;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gigaspaces.azure.deploy.azureconfig.AzureDeploymentException;
import com.gigaspaces.azure.deploy.azureconfig.AzureHostedService;
import com.gigaspaces.azure.deploy.azureconfig.AzureSlot;
import com.gigaspaces.azure.deploy.files.AzureDeploymentConfigurationFile;
import com.gigaspaces.azure.deploy.util.AzureUtils;

/**
 * @author dank
 * @author itaif
 * @since 8.0.4
 */
public class AzureDeployApplication {

	private static final Logger logger = Logger.getLogger(AzureDeployApplication.class.getName());
	
	private File cspkgFile;
	
	private File cscfgFile;
	
	
    private String azureHostedServiceName = null;

	private String azureDeploymentSlotName = "staging";
	
	private String azureDeploymentName = null;

	private String azureHostedServiceLocation = "Anywhere US";
	
	private String azureHostedServiceDescription = "";
	
	private String azureRemoteDesktopPfxFilePassword;
	
	private boolean verbose;
	
	public File getCspkgFile() {
        return cspkgFile;
    }

    public void setCspkgFile(File cspkgFile) {
        this.cspkgFile = cspkgFile;
    }

    public File getCscfgFile() {
        return cscfgFile;
    }

    public void setCscfgFile(File cscfgFile) {
        this.cscfgFile = cscfgFile;
    }

    public String getAzureHostedServiceName() {
        return azureHostedServiceName;
    }

    public void setAzureHostedServiceName(String azureHostedServiceName) {
        this.azureHostedServiceName = azureHostedServiceName;
    }

    public String getAzureDeploymentSlotName() {
        return azureDeploymentSlotName;
    }

    public void setAzureDeploymentSlotName(String azureDeploymentSlotName) {
        this.azureDeploymentSlotName = azureDeploymentSlotName;
    }

    public String getAzureDeploymentName() {
        return azureDeploymentName;
    }

    public void setAzureDeploymentName(String azureDeploymentName) {
        this.azureDeploymentName = azureDeploymentName;
    }

    public String getAzureHostedServiceLocation() {
        return azureHostedServiceLocation;
    }

    public void setAzureHostedServiceLocation(String azureHostedServiceLocation) {
        this.azureHostedServiceLocation = azureHostedServiceLocation;
    }

    public String getAzureHostedServiceDescription() {
        return azureHostedServiceDescription;
    }

    public void setAzureHostedServiceDescription(
            String azureHostedServiceDescription) {
        this.azureHostedServiceDescription = azureHostedServiceDescription;
    }

    public String getAzureRemoteDesktopPfxFilePassword() {
        return azureRemoteDesktopPfxFilePassword;
    }

    public void setAzureRemoteDesktopPfxFilePassword(
            String azureRemoteDesktopPfxFilePassword) {
        this.azureRemoteDesktopPfxFilePassword = azureRemoteDesktopPfxFilePassword;
    }

	
	public void deploy() throws Exception {
		
	    if (!azureDeploymentSlotName.equals(AzureSlot.Production.getSlot()) && 
	        !azureDeploymentSlotName.equals(AzureSlot.Staging.getSlot())) {
	        throw new AzureDeploymentException("azure-deployment-slot must be either " + AzureSlot.Production.getSlot() + 
	                " or " + AzureSlot.Staging.getSlot());
	    }
	    
		Properties properties = AzureUtils.getAzureProperties();
		
		String storageAccount = AzureUtils.getProperty(properties,"storageAccount");
		String storageAccessKey = AzureUtils.getProperty(properties,"storageAccessKey");
		String storageBlobContainerName = AzureUtils.getProperty(properties,"storageBlobContainerName");
		String subscriptionId = AzureUtils.getProperty(properties, "subscriptionId");
		String certificateThumbprint = AzureUtils.getProperty(properties, "certificateThumbprint");
		
		File rdpCertFile = AzureUtils.getFileProperty(properties,"rdpCertFile");
		File rdpPfxFile = AzureUtils.getFileProperty(properties,"rdpPfxFile");
		
		//create hosted service
		AzureHostedService azureHostedService = new AzureHostedService(AzureUtils.getAzureConfigEXE(), AzureUtils.getEncUtilEXE(), subscriptionId, certificateThumbprint, verbose);
		List<String> azureHostedServices = Arrays.asList(azureHostedService.listHostedServices());
		if (azureHostedServices.contains(azureHostedServiceName)) {
			logger.log(Level.INFO,"Found azure hosted service " + azureHostedServiceName);
		}
		else {
			logger.log(Level.INFO,"Creating azure hosted service " + azureHostedServiceName);
			azureHostedService.createHostedService(azureHostedServiceName, azureHostedServiceName, azureHostedServiceLocation, azureHostedServiceDescription);
		}
	
		// upload rdp certificate to hosted service
		Collection<String> certificateThumbprints = Arrays.asList(azureHostedService.listCertificateThumbprints(azureHostedServiceName));
		String rdpCertificateThumbprint = azureHostedService.getCertificateThumbprint(rdpCertFile);
		if (certificateThumbprints.contains(rdpCertificateThumbprint)) {
			logger.log(Level.INFO,"Found RDP certificate " + rdpCertificateThumbprint);
		}
		else {
			logger.log(Level.INFO,"Uploading Remote Desktop certificate");
			azureHostedService.addCertificate(azureHostedServiceName, rdpPfxFile, azureRemoteDesktopPfxFilePassword);
		}
		
		logger.log(Level.INFO,"updating " + cscfgFile.getName() + " with certificate thumbprint " + rdpCertificateThumbprint);
		AzureDeploymentConfigurationFile cscfg = new AzureDeploymentConfigurationFile(cscfgFile);
		cscfg.setRdpCertificateThumbprint(rdpCertificateThumbprint);
		cscfg.flush();
		
		// create deployment
        logger.log(Level.INFO,"Creating azure deployment " + azureDeploymentName);
        
		AzureDeploymentWrapper azureDeploymentWrapper = new AzureDeploymentWrapper();
		azureDeploymentWrapper.setVerbose(verbose);
		azureDeploymentWrapper.setAzureHostedServiceName(azureHostedServiceName);
		azureDeploymentWrapper.setAzureDeploymentSlotName(azureDeploymentSlotName);
		azureDeploymentWrapper.setCertificateThumbprint(certificateThumbprint);
		azureDeploymentWrapper.setSubscriptionId(subscriptionId);
		azureDeploymentWrapper.setStorageAccount(storageAccount);
		azureDeploymentWrapper.setStorageAccessKey(storageAccessKey);
		azureDeploymentWrapper.setStorageBlobContainerName(storageBlobContainerName);
		azureDeploymentWrapper.createDeployment(azureDeploymentName, cscfgFile, cspkgFile);
		
	}

}
