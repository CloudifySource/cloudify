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

package org.openspaces.cloud.azure.shell.commands;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.openspaces.cloud.azure.AzureDeploymentStatus;
import org.openspaces.cloud.azure.files.AzureDeploymentConfigurationFile;
import org.openspaces.cloud.azure.shell.AzureUtils;
import com.gigaspaces.cloudify.shell.ShellUtils;
import com.gigaspaces.cloudify.shell.commands.AdminAwareCommand;
import com.gigaspaces.cloudify.shell.commands.CLIException;

/**
 * @author barakme
 * @author itaif
 * @since 8.0.4
 */
@Command(scope = "azure", name = "set-instances", description = "Sets the number of service instances")
public class AzureSetNumberOfInstances extends AdminAwareCommand {

	private static final String MANAGEMENT_APPLICATION_NAME = "Management";
	private static final String TIMEOUT_ERROR_STRING = "Azure set number of instances timed-out";
	private static final Logger logger = Logger.getLogger(AzureSetNumberOfInstances.class.getName());
	
	@Argument(index = 0, required = true, name = "service-name", description = "The Cloudify service name (Azure Role)")
	String roleName;
	
	@Argument(index = 1, required = true, name = "instances", description = "The required number of instances for the specified role")
	int instances;
	
	@Option(required = true, name = "-azure-svc", description = "The Azure Hosted Service name")
	String azureHostedServiceName = null;

	@Option(required = false, name = "-azure-slot", description = "The Azure Deployment slot (staging or production)")
	String azureDeploymentSlotName = "staging";

	@Option(required = false, name = "-timeout", description = "The time to wait until the operation is done. Defaults to 30 minutes.")
	int timeoutInMinutes=30;
	
	@Option(required = false, name = "-progress", description = "The polling time interval in minutes, used for checking if the operation is done. Defaults to 1 minute. Use together with the -timeout option")
	int progressInMinutes=1;

	@Override
	protected Object doExecute() throws Exception {
	
       if (timeoutInMinutes < 0) {
            throw new CLIException("-timeout cannot be negative");
        }
        
        if (progressInMinutes < 1) {
            throw new CLIException("-progress must be positive");
        }
        
        if (timeoutInMinutes > 0 && timeoutInMinutes < progressInMinutes) {
            throw new CLIException("-timeout must be bigger than -progress");
        }
        
        long end = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeoutInMinutes);
        
        Properties properties = AzureUtils.getAzureProperties();
        String subscriptionId = AzureUtils.getProperty(properties, "subscriptionId");
        String certificateThumbprint = AzureUtils.getProperty(properties, "certificateThumbprint");
	    
        AzureDeploymentWrapper azureDeploymentWrapper = new AzureDeploymentWrapper();
		azureDeploymentWrapper.setVerbose(verbose);
		azureDeploymentWrapper.setProgressInMinutes(progressInMinutes);
		azureDeploymentWrapper.setAzureHostedServiceName(azureHostedServiceName);
		azureDeploymentWrapper.setAzureDeploymentSlotName(azureDeploymentSlotName);
		azureDeploymentWrapper.setCertificateThumbprint(certificateThumbprint);
		azureDeploymentWrapper.setSubscriptionId(subscriptionId);
		azureDeploymentWrapper.setTimeoutErrorMessage(TIMEOUT_ERROR_STRING);
		
		AzureDeploymentConfigurationFile configFile = 
			azureDeploymentWrapper.getDeploymentConfig();
		
		configFile.setNumberOfInstances(roleName,instances);
		configFile.flush();
		
		azureDeploymentWrapper.updateDeploymentConfig(configFile);
		if (timeoutInMinutes == 0) {
				return roleName + " update to " + instances + " instances is in progress";	
		}
		
		logger.info(ShellUtils.getExpectedExecutionTimeMessage());
		
		azureDeploymentWrapper.waitForAzureDeploymentStatus(AzureDeploymentStatus.Running, ShellUtils.millisUntil(TIMEOUT_ERROR_STRING, end), TimeUnit.MILLISECONDS);

		if (!adminFacade.isConnected()) {
			azureDeploymentWrapper.connect(adminFacade);
		}
		String applicationName = tryGetInstalledApplicationName();
		if (applicationName != null) {
			azureDeploymentWrapper.waitForNumberOfMachines(adminFacade, applicationName, roleName, instances, ShellUtils.millisUntil(TIMEOUT_ERROR_STRING, end), TimeUnit.MILLISECONDS);
		}
		
		return roleName + " updated to " + instances + " instances.";
	}

	String tryGetInstalledApplicationName() throws CLIException {
		
		final List<String> applications = super.adminFacade.getApplicationsList();
		if (applications.isEmpty()) {
			throw new CLIException("No applications found");
		}
		
		if (applications.size() > 2) {
			throw new CLIException("Expected only 1 application (in addition to the Management application). Applications discovered: " + applications.toString());
		}
		
		String applicationName = null;
		if (applications.size() == 2) {
			if (applications.get(0).equalsIgnoreCase(MANAGEMENT_APPLICATION_NAME)) {
				applicationName = applications.get(1);
			}
			else if (applications.get(1).equalsIgnoreCase(MANAGEMENT_APPLICATION_NAME)) {
				applicationName = applications.get(0);
			}
		}
		return applicationName;
	}
	
}
