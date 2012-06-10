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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;

import org.cloudifysource.azure.files.AzureDeploymentConfigurationFile;
import org.cloudifysource.azure.files.XMLXPathEditorException;

//CR: Add comment thread safe or not
/**
 * This class serves as a Java/.Net bridge implementation to the Azure service management api through until
 * an official Java SDK is released.
 * 
 * This class is not thread safe
 * @author Dank
 */
public class AzureDeployment extends AzureConfigExe{
    

    public AzureDeployment(File azureConfigExeFile, String subscriptionId, String certificateThumbprint, boolean verbose) {
        super(azureConfigExeFile, null, subscriptionId, certificateThumbprint, verbose);
    }
    
	/**
	 * Creates a new deployment based on existing cspkg file.
     * @see http://msdn.microsoft.com/en-us/library/ee460813.aspx
     */
    public void createDeployment(
            String hostedServiceName, 
            String deploymentName, 
            String deploymentLabel, 
            AzureSlot deploymentSlot, 
            File cscfgFile, 
            URI packageUrl) throws InterruptedException, AzureDeploymentException {
        executeAzureConfig(
            argument(CREATE_DEPLOYMENT_FLAG),
            argument(HOSTED_SERVICE_FLAG, hostedServiceName),
            argument(NAME_FLAG, deploymentName),
            argument(LABEL_FLAG, deploymentLabel),
            argument(SLOT_FLAG, deploymentSlot.getSlot()),
            argument(CONFIG_FLAG, cscfgFile.getAbsolutePath()),
            argument(PACKAGE_FLAG, packageUrl.toString())
        );
    }
    
    /**
     * Uploads the cspkg file to azure blob store and creates a new deployment
     * </br>
     * See: <a href="http://msdn.microsoft.com/en-us/library/ee460813.aspx">http://msdn.microsoft.com/en-us/library/ee460813.aspx</a>
     * @param storageAccount 
     * @param storageKey 
     * @param storageContainer 
     */
    public void createDeployment(
    		String hostedServiceName, 
            String deploymentName, 
            String deploymentLabel, 
            AzureSlot deploymentSlot, 
            File cscfgFile, 
            File packageFile, 
            String storageAccount, 
            String storageKey, 
            String storageContainer) throws InterruptedException, AzureDeploymentException {

    	executeAzureConfig(
                argument(CREATE_DEPLOYMENT_FLAG),
                argument(HOSTED_SERVICE_FLAG, hostedServiceName),
                argument(NAME_FLAG, deploymentName),
                argument(LABEL_FLAG, deploymentLabel),
                argument(SLOT_FLAG, deploymentSlot.getSlot()),
                argument(CONFIG_FLAG, cscfgFile.getAbsolutePath()),
                argument(PACKAGE_FLAG, packageFile.getAbsolutePath()),
                argument(STORAGE_ACCOUNT_FLAG, storageAccount),
                argument(STORAGE_KEY_FLAG, storageKey),
                argument(STORAGE_CONTAINER_FLAG, storageContainer.toLowerCase()) // container name must be dns name compliant
            );
		
	}
   
    /**
     * Updates the deployment state
     * Specify AzureDeploymentStatus.Suspended in order to stop (pause) the machines 
     * @see http://msdn.microsoft.com/en-us/library/ee460808.aspx
     */
    public void updateDeployment(
            String hostedServiceName,
            AzureSlot deploymentSlot,
            AzureDeploymentStatus status) throws InterruptedException, AzureDeploymentException {
        if (!(status == AzureDeploymentStatus.RUNNING || status == AzureDeploymentStatus.SUSPENDED)) {
            throw new IllegalArgumentException("status must be either Running or Suspended");
        }
        
        executeAzureConfig(
            argument(UPDATE_DEPLOYMENT_FLAG),
            argument(HOSTED_SERVICE_FLAG, hostedServiceName),
            argument(SLOT_FLAG, deploymentSlot.getSlot()),
            argument(STATUS_FLAG, status.getStatus())
        );
    }
    
    /**
     * @see http://msdn.microsoft.com/en-us/library/ee460815.aspx)
     */
    public void deleteDeployment(
            String hostedServiceName,
            AzureSlot deploymentSlot) throws InterruptedException, AzureDeploymentException {
        
    	executeAzureConfig(
            argument(DELETE_DEPLOYMENT_FLAG),
            argument(HOSTED_SERVICE_FLAG, hostedServiceName),
            argument(SLOT_FLAG, deploymentSlot.getSlot())
        );       
    }
    
    public AzureDeploymentStatus getDeploymentStatus(
            String hostedService,
            AzureSlot slot) throws InterruptedException, AzureDeploymentException {
    	String output = executeAzureConfig(
                argument(GET_DEPLOYMENT_STATUS_FLAG),
                argument(HOSTED_SERVICE_FLAG, hostedService),
                argument(SLOT_FLAG, slot.getSlot())
            );
    	output = output.trim();
        return AzureDeploymentStatus.fromString(output);
    	       
    }

    public String getDeploymentUrl(
            String hostedService,
            AzureSlot slot) throws InterruptedException, AzureDeploymentException {
        return executeAzureConfig(
                argument(GET_DEPLOYMENT_URL_FLAG),
                argument(HOSTED_SERVICE_FLAG, hostedService),
                argument(SLOT_FLAG, slot.getSlot())
        );
    }

    public AzureDeploymentConfigurationFile getDeploymentConfig(
            String hostedService,
            AzureSlot slot,
            File outputCscfgFile) throws InterruptedException, AzureDeploymentException {
        String cfg = executeAzureConfig(
        		argument(GET_DEPLOYMENT_CONFIG_FLAG),
                argument(HOSTED_SERVICE_FLAG, hostedService),
                argument(SLOT_FLAG, slot.getSlot())
        );  
        BufferedWriter out = null;
        try {
	        try {
				out = 
					new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputCscfgFile.getAbsolutePath())));
				out.write(cfg);
	        }
	        finally {
				if (out != null) {
					out.close();
				}
			}
        }
        catch (IOException e) {
        	throw new AzureDeploymentException(e);
        }

        try {
            return new AzureDeploymentConfigurationFile(outputCscfgFile);
        } catch (XMLXPathEditorException e) {
            throw new AzureDeploymentException(e);
        }
    }
    
    public void updateDeploymentConfig(
            String hostedService,
            AzureSlot slot,
            AzureDeploymentConfigurationFile cscfgFile) throws InterruptedException, AzureDeploymentException {

    	executeAzureConfig(
                argument(SET_DEPLOYMENT_CONFIG_FLAG),
                argument(HOSTED_SERVICE_FLAG, hostedService),
                argument(SLOT_FLAG, slot.getSlot()),
                argument(CONFIG_FLAG, cscfgFile.getFile().getAbsolutePath())
        );       
    }
   
}
