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

import java.util.Properties;

import com.gigaspaces.azure.deploy.files.AzureDeploymentConfigurationFile;
import com.gigaspaces.azure.deploy.util.AzureUtils;

/**
 * @author barakme
 * @author itaif
 * @since 8.0.4
 */
public class AzureSetNumberOfInstances {

	private String roleName;
	
	private int instances;
	
	private String azureHostedServiceName = null;

	private String azureDeploymentSlotName = "staging";

	private boolean verbose;

	public void update() throws Exception {
	
        
        Properties properties = AzureUtils.getAzureProperties();
        String subscriptionId = AzureUtils.getProperty(properties, "subscriptionId");
        String certificateThumbprint = AzureUtils.getProperty(properties, "certificateThumbprint");
	    
        AzureDeploymentWrapper azureDeploymentWrapper = new AzureDeploymentWrapper();
		azureDeploymentWrapper.setVerbose(verbose);
		azureDeploymentWrapper.setAzureHostedServiceName(azureHostedServiceName);
		azureDeploymentWrapper.setAzureDeploymentSlotName(azureDeploymentSlotName);
		azureDeploymentWrapper.setCertificateThumbprint(certificateThumbprint);
		azureDeploymentWrapper.setSubscriptionId(subscriptionId);
		
		AzureDeploymentConfigurationFile configFile = 
			azureDeploymentWrapper.getDeploymentConfig();
		
		configFile.setNumberOfInstances(roleName,instances);
		configFile.flush();
		
		azureDeploymentWrapper.updateDeploymentConfig(configFile);
		
	}

	
}
