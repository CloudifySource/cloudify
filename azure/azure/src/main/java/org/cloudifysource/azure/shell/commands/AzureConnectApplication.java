package org.cloudifysource.azure.shell.commands;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.azure.shell.AzureUtils;

import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.commands.AbstractGSCommand;
import org.cloudifysource.shell.commands.CLIException;

/**
 * @author itaif
 * @author dank
 * @since 8.0.4
 */
@Command(scope = "azure", name = "connect-app", description = "Connects to a bootstrapped application.")
public class AzureConnectApplication extends AbstractGSCommand {

	private static final String TIMEOUT_ERROR_STRING = "Azure connect application timed-out";

	private static final Logger logger = Logger.getLogger(AzureConnectApplication.class.getName());
		
	@Option(required = true, name = "-azure-svc", description = "The Azure Hosted Service name. Default: [application name]")
	String azureHostedServiceName = null;

	@Option(required = false, name = "-azure-slot", description = "The Azure Deployment slot (staging or production). Default: staging")
	String azureDeploymentSlotName = "staging";
	
	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. By default waits 1 minute.")
	int timeoutInMinutes=1;
	
	@Option(required = false, name = "-progress", description = "The polling time interval in minutes used for checking if the operation is done. Defaults to 1 minute. Use together with the -timeout option")
	int progressInMinutes=1;
	
	@Override
	protected Object doExecute() throws Exception {
		
	   AdminFacade adminFacade = (AdminFacade) session.get(Constants.ADMIN_FACADE);
	    
       if (adminFacade != null && adminFacade.isConnected()) {
            throw new CLIException("already_connected");
       }
	    
       if (timeoutInMinutes < 0) {
            throw new CLIException("-timeout cannot be negative");
        }
        
        if (progressInMinutes < 1) {
            throw new CLIException("-progress must be positive");
        }
        
        if (timeoutInMinutes > 0 && timeoutInMinutes < progressInMinutes) {
            throw new CLIException("-timeout must be bigger than -progress-min");
        }
        
        long end = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeoutInMinutes);
        if (end < 0) {
            throw new CLIException("-timeout caused an overflow. please use a smaller value");
        }
	    
		Properties properties = AzureUtils.getAzureProperties();
		String subscriptionId = AzureUtils.getProperty(properties, "subscriptionId");
		String certificateThumbprint = AzureUtils.getProperty(properties, "certificateThumbprint");
		
		logger.info("Connecting...");
	
		AzureDeploymentWrapper azureDeploymentWrapper = new AzureDeploymentWrapper();
		azureDeploymentWrapper.setVerbose(verbose);
		azureDeploymentWrapper.setProgressInMinutes(progressInMinutes);
		azureDeploymentWrapper.setAzureHostedServiceName(azureHostedServiceName);
		azureDeploymentWrapper.setAzureDeploymentSlotName(azureDeploymentSlotName);
		azureDeploymentWrapper.setCertificateThumbprint(certificateThumbprint);
		azureDeploymentWrapper.setSubscriptionId(subscriptionId);
		azureDeploymentWrapper.setTimeoutErrorMessage(TIMEOUT_ERROR_STRING);
		
		if (timeoutInMinutes == 0) {
			azureDeploymentWrapper.connect(adminFacade);
		}
		else {
			azureDeploymentWrapper.connectAndWait(adminFacade, timeoutInMinutes, TimeUnit.MINUTES);
		}
		
		return messages.getString("connected_successfully");
    }
}
