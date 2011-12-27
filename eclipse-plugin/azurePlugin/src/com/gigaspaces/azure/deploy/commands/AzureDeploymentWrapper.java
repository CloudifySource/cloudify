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
import java.net.URI;

import com.gigaspaces.azure.deploy.azureconfig.AzureDeployment;
import com.gigaspaces.azure.deploy.azureconfig.AzureDeploymentException;
import com.gigaspaces.azure.deploy.azureconfig.AzureDeploymentStatus;
import com.gigaspaces.azure.deploy.azureconfig.AzureSlot;
import com.gigaspaces.azure.deploy.files.AzureDeploymentConfigurationFile;
import com.gigaspaces.azure.deploy.util.AzureUtils;

public class AzureDeploymentWrapper {

	private boolean verbose;
	private String azureHostedServiceName;
	private String azureDeploymentSlotName;
	private String certificateThumbprint;
	private String subscriptionId;
	private String storageAccount;
	private String storageAccessKey;
	private String storageBlobContainerName;
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	public void setAzureHostedServiceName(String azureHostedServiceName) {
		this.azureHostedServiceName = azureHostedServiceName;
	}


	public void setAzureDeploymentSlotName(String azureDeploymentSlotName) {
		this.azureDeploymentSlotName = azureDeploymentSlotName;
	}


	public void setCertificateThumbprint(String certificateThumbprint) {
		this.certificateThumbprint = certificateThumbprint;
	}


	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public void setStorageAccount(String storageAccount) {
		this.storageAccount = storageAccount;
	}

	public void setStorageAccessKey(String storageAccessKey) {
		this.storageAccessKey = storageAccessKey;
		
	}

	public void setStorageBlobContainerName(String storageBlobContainerName) {
		this.storageBlobContainerName = storageBlobContainerName;
		
	}
	public AzureDeployment getAzureDeployment() {
		return new AzureDeployment(AzureUtils.getAzureConfigEXE(), subscriptionId, certificateThumbprint, verbose);
	}
	
	public void createDeployment(String azureDeploymentName, File cscfgFile, File cspkgFile) throws InterruptedException, AzureDeploymentException {
			getAzureDeployment().createDeployment(
					azureHostedServiceName, 
					azureDeploymentName, 
					azureDeploymentName, 
					AzureSlot.fromString(azureDeploymentSlotName), 
					cscfgFile,
					cspkgFile,
					storageAccount,
					storageAccessKey,
					storageBlobContainerName);
	}
	
	public void createDeployment(String azureDeploymentName, File cscfgFile, URI cspkgUri) throws InterruptedException, AzureDeploymentException {
		getAzureDeployment().createDeployment(
				azureHostedServiceName, 
				azureDeploymentName, 
				azureDeploymentName, 
				AzureSlot.fromString(azureDeploymentSlotName), 
				cscfgFile,
				cspkgUri);
	}
	
	
	public void stopDeployment() throws InterruptedException, AzureDeploymentException {
		getAzureDeployment().updateDeployment(azureHostedServiceName, AzureSlot.fromString(azureDeploymentSlotName), AzureDeploymentStatus.Suspended);
	}
	
	public void deleteDeployment() throws InterruptedException, AzureDeploymentException  {
		getAzureDeployment().deleteDeployment(azureHostedServiceName, AzureSlot.fromString(azureDeploymentSlotName));
	}

    
    public AzureDeploymentStatus getStatus() throws InterruptedException, AzureDeploymentException {
    	return getAzureDeployment().getDeploymentStatus(
					azureHostedServiceName, 
					AzureSlot.fromString(azureDeploymentSlotName));
    }

	public void updateDeploymentConfig(
			AzureDeploymentConfigurationFile configFile) throws InterruptedException, AzureDeploymentException {
			getAzureDeployment().updateDeploymentConfig(
					azureHostedServiceName, 
					AzureSlot.fromString(azureDeploymentSlotName), 
					configFile);
	}

	public AzureDeploymentConfigurationFile getDeploymentConfig() throws InterruptedException, AzureDeploymentException {
		
		AzureSlot azureSlot = AzureSlot.fromString(azureDeploymentSlotName);
		
		File tempCscfgFile = AzureUtils.createTempCscfgFile(azureHostedServiceName,azureSlot);
	
		return getAzureDeployment().getDeploymentConfig(
		        azureHostedServiceName, 
		        azureSlot, 
		        tempCscfgFile);
		
	}

}