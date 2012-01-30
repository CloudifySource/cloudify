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

package org.cloudifysource.azure.shell.commands;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.cloudifysource.azure.AzureDeployment;
import org.cloudifysource.azure.AzureDeploymentException;
import org.cloudifysource.azure.AzureDeploymentStatus;
import org.cloudifysource.azure.AzureSlot;
import org.cloudifysource.azure.files.AzureDeploymentConfigurationFile;
import org.cloudifysource.azure.shell.AzureUtils;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIException;



public class AzureDeploymentWrapper {

	private static final Logger logger = Logger.getLogger(AzureDeploymentWrapper.class.getName());
	
	private boolean verbose;
	private long progressInMinutes;
	private String azureHostedServiceName;
	private String azureDeploymentSlotName;
	private String certificateThumbprint;
	private String subscriptionId;
	private String storageAccount;
	private String storageAccessKey;
	private String storageBlobContainerName;
	private String timeoutErrorMessage;
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	public void setTimeoutErrorMessage(String timeoutErrorMessage) {
		this.timeoutErrorMessage = timeoutErrorMessage;
	}

	public void setProgressInMinutes(long progressInMinutes) {
		this.progressInMinutes = progressInMinutes;
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
	
	public void createDeployment(String azureDeploymentName, File cscfgFile, File cspkgFile) throws InterruptedException, CLIException {
		 try {
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
	        } catch (AzureDeploymentException e) {
				throw new CLIException(e);
			}
	}
	
	public void createDeployment(String azureDeploymentName, File cscfgFile, URI cspkgUri) throws InterruptedException, CLIException {
		
        try {
			getAzureDeployment().createDeployment(
					azureHostedServiceName, 
					azureDeploymentName, 
					azureDeploymentName, 
					AzureSlot.fromString(azureDeploymentSlotName), 
					cscfgFile,
					cspkgUri);
        } catch (AzureDeploymentException e) {
			throw new CLIException(e);
		}
	}
	
	
	public void stopDeployment() throws InterruptedException, CLIException {
	    try {
			getAzureDeployment().updateDeployment(azureHostedServiceName, AzureSlot.fromString(azureDeploymentSlotName), AzureDeploymentStatus.Suspended);
	    } catch (AzureDeploymentException e) {
			throw new CLIException(e);
		}
	}
	
	public void deleteDeployment() throws InterruptedException, CLIException  {
	    try {
			getAzureDeployment().deleteDeployment(azureHostedServiceName, AzureSlot.fromString(azureDeploymentSlotName));
		} catch (AzureDeploymentException e) {
			throw new CLIException(e);
		}
	}

	public URI connect(AdminFacade adminFacade) throws InterruptedException, CLIException {
	
		String restAdminUrl;
		restAdminUrl = getRestAdminUrl(getAzureDeployment());
		adminFacade.connect(null, null, restAdminUrl);
		try {
			return new URI(restAdminUrl);
		} catch (URISyntaxException e) {
			throw new CLIException(e);
		}
	}

	public URI connectAndWait(AdminFacade adminFacade, long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException, CLIException {
	
		if (timeout == 0) {
			throw new IllegalArgumentException("use connect instead of connectAndWait()");
		}
		
		long end = System.currentTimeMillis() + timeUnit.toMillis(timeout);
	
		logger.log(Level.INFO,"Waiting for azure deployment to run.");
		waitForAzureDeploymentStatus(AzureDeploymentStatus.Running, ShellUtils.millisUntil(timeoutErrorMessage,end), TimeUnit.MILLISECONDS);

		try {
			String restAdminUrl = getRestAdminUrl(getAzureDeployment());
		
			waitForConnection(adminFacade, restAdminUrl, ShellUtils.millisUntil(timeoutErrorMessage,end), TimeUnit.MILLISECONDS);
				return new URI(restAdminUrl);
		} catch (URISyntaxException e) {
			throw new CLIException(e);
		}		
	}
		
	public void waitForAzureDeploymentStatus(
			final AzureDeploymentStatus status, 
			long timeout, TimeUnit timeUnit) 
		throws InterruptedException, TimeoutException, CLIException {
	    waitForAzureDeploymentStatus(status, timeUnit.convert(progressInMinutes, TimeUnit.MINUTES), timeout, timeUnit);
	}
	
	public void waitForAzureDeploymentStatus(final AzureDeploymentStatus status, long pollingInterval, long timeout, TimeUnit timeunit) 
		throws InterruptedException, TimeoutException, CLIException {
	    
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
		     
            @Override
            public boolean isDone() throws InterruptedException, CLIException {
                AzureDeploymentStatus currentStatus = getStatus();
                if (currentStatus == status) {
                    return true;
                }
                if (verbose) {
                    logger.log(Level.INFO, "Current status: " + currentStatus.getStatus() + " waiting for " + status);
                }
                return false;
                
            } 
        });
	}
	
	private void waitForConnection(final AdminFacade adminFacade, final String restAdminUrl, long timeout, TimeUnit timeunit) 
		throws InterruptedException, TimeoutException, CLIException { 
		
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
    		
			@Override
			public boolean isDone() throws CLIException, InterruptedException {
			
	            flushDns();
	            try {
	                adminFacade.connect(null, null, restAdminUrl);
	                return true;
	            } catch (CLIException e) {
					String message = "Waiting for role instances and REST Admin server: " + restAdminUrl;
	                if (verbose) {
	                	logger.log(Level.INFO, message, e);
	                }
	                else {
	                	logger.log(Level.INFO, message);
	                }
	                return false;
	            } 
			}
    	});

	}

    
    public AzureDeploymentStatus getStatus() throws InterruptedException, CLIException {
    	try {
	    	return getAzureDeployment().getDeploymentStatus(
						azureHostedServiceName, 
						AzureSlot.fromString(azureDeploymentSlotName));
    	}
    	catch (AzureDeploymentException e) {
    		throw new CLIException(e);
    	}
    }
    

    /**
    /**
     * Runs ipconfig /flushdns. In case the azure load balancer DNS has not been
     * resolved yet, or is mapped to an old ip address. This is a temporary
     * measure until we get the resolved ip address directly from the azure REST
     * API.
     * 
     * @see http://altamodatech.com/blogs/?p=93
     */
    private void flushDns() throws CLIException, InterruptedException {
        runProcess(new String[] { "ipconfig", "/flushdns" });
        runProcess(new String[] { "cmd", "/c", "\"sc query dnscache | findstr RUNNING && net stop dnscache && net start dnscache\"" });
    }
    
    private void runProcess(String[] cmd) throws CLIException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (verbose) {
            logger.info("Executing command: " + Arrays.toString(cmd));
        }
        Process p;
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new CLIException("error occured while running "
                    + Arrays.toString(cmd), e);
        }

        p.waitFor();        
    }
	
    private String getRestAdminUrl(AzureDeployment azureDeployment) throws InterruptedException, CLIException  {
    	
		String url;
		try {
			url = azureDeployment.getDeploymentUrl(
					azureHostedServiceName, 
					AzureSlot.fromString(azureDeploymentSlotName));
		} catch (AzureDeploymentException e) {
			throw new CLIException(e);
		} 

		url = url.trim();
		
		while (url.startsWith(Pattern.quote("\n"))) {
			url = url.substring(1);
		}
		
		while (url.endsWith("\n")) {
		    url = url.substring(0, url.length()-1);
		}
		
    	if (url.endsWith("/")) {
            url = url.substring(0, url.length()-1);
        }
        return url + "/rest/";
    }
	
	public void updateDeploymentConfig(
			AzureDeploymentConfigurationFile configFile) throws InterruptedException, CLIException {
		try {
			getAzureDeployment().updateDeploymentConfig(
					azureHostedServiceName, 
					AzureSlot.fromString(azureDeploymentSlotName), 
					configFile);
		} catch (AzureDeploymentException e) {
			throw new CLIException(e);
		}
		
	}

	public AzureDeploymentConfigurationFile getDeploymentConfig() throws InterruptedException, CLIException {
		
		AzureSlot azureSlot = AzureSlot.fromString(azureDeploymentSlotName);
		
		try {
			File tempCscfgFile = AzureUtils.createTempCscfgFile(azureHostedServiceName,azureSlot);
		
			return getAzureDeployment().getDeploymentConfig(
			        azureHostedServiceName, 
			        azureSlot, 
			        tempCscfgFile);
		} catch (AzureDeploymentException e) {
			throw new CLIException(e);
		}
		
	}

	public void waitForNumberOfMachines(final AdminFacade adminFacade, final int numberOfMachines, long timeout, TimeUnit timeunit) 
		throws TimeoutException, InterruptedException, CLIException{
		
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
    		
			@Override
			public boolean isDone() throws InterruptedException, CLIException {
		    	
		        int currentNumberOfMachines = adminFacade.getMachines().size();
		        if (currentNumberOfMachines == numberOfMachines) {
		        	return true;
		        }
		    
		        logger.log(Level.INFO, 
		            "Currently running: " + currentNumberOfMachines + " instances" +
		            "\nWaiting for: " + numberOfMachines + " instances");
		        
		        return false;
			}
		});
	}

	public void waitForNumberOfMachines(
			final AdminFacade adminFacade,
			final String applicationName, 
			final String serviceName, 
			final int numberOfMachines, 
			long timeout,TimeUnit timeunit)
	
		throws InterruptedException, TimeoutException, CLIException{
		
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {
			
    			@Override
				public boolean isDone() throws CLIException {
			    	
			        int currentNumberOfMachines = adminFacade.getInstanceList(applicationName, serviceName).size();
			        if (currentNumberOfMachines == numberOfMachines) {
			        	return true;
			        }
			    
			        logger.log(Level.INFO, 
			            "Currently running: " + currentNumberOfMachines + " instances of service " + serviceName +
			            "\nWaiting for: " + numberOfMachines + " instances");
			        
			        return false;
    			}
    		});
	}


	private ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
		return 
			new ConditionLatch()
			.timeout(timeout,timeunit)
			.pollingInterval(progressInMinutes, TimeUnit.MINUTES)
			.timeoutErrorMessage(timeoutErrorMessage)
			.verbose(verbose);
	}
}