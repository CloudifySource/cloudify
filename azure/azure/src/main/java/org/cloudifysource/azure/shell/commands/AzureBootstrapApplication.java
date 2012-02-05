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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.azure.AzureDeploymentStatus;
import org.cloudifysource.azure.AzureHostedService;
import org.cloudifysource.azure.AzureSlot;
import org.cloudifysource.azure.files.AzureDeploymentConfigurationFile;
import org.cloudifysource.azure.files.AzureDeploymentDefinitionFile;
import org.cloudifysource.azure.files.AzureServiceDefinition;
import org.cloudifysource.azure.files.XMLXPathEditorException;
import org.cloudifysource.azure.shell.AzureUtils;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.Sla;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.AbstractGSCommand;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;


/**
 * @author dank
 * @author itaif
 * @since 8.0.4
 */
@Command(scope = "azure", name = "bootstrap-app", description = "Starts Azure Role Instances based on the specified application description.")
public class AzureBootstrapApplication extends AbstractGSCommand {

	private static final String TIMEOUT_ERROR_STRING = "Azure application bootsrap timed-out";

	private static final Logger logger = Logger.getLogger(AzureBootstrapApplication.class.getName());
	
	@Argument(required = true, name = "application-file", description = "The application directory or archive")
	File applicationFile;
	
	@Option(required = false, name = "-azure-svc", description = "The Azure Hosted Service name. Default: [application name]")
	String azureHostedServiceName = null;

	@Option(required = false, name = "-azure-slot", description = "The Azure Deployment slot (staging or production). Default: staging")
	String azureDeploymentSlotName = "staging";
	
	@Option(required = false, name = "-azure-name", description = "The Azure Deployment name in production is the [application name], and in staging is the [application name]-staging")
	String azureDeploymentName = null;

	@Option(required = false, name = "-azure-location", description = "The Azure Hosted service location (data center region).")
	String azureHostedServiceLocation = "Anywhere US";
	
	@Option(required = false, name = "-azure-description", description = "The Azure Hosted service description (empty by default).")
	String azureHostedServiceDescription = "";
	
	@Option(required = false, name = "-azure-pwd", description = "The password that protects the PFX file containing the Remote Desktop certificate. This option must be specified the first time an application is bootstrapped since it is required for creating an azure hosted-service")
	String azureRemoteDesktopPfxFilePassword;
	
	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. By default waits 30 minutes.")
	int timeoutInMinutes=30;
	
	@Option(required = false, name = "-progress", description = "The polling interval in minutes used for checking if the operation is done. Defaults to 1 minute. Use together with the -timeout option")
	int progressInMinutes=1;
	
	@Override
	protected Object doExecute() throws Exception {
		
	    AdminFacade adminFacade = (AdminFacade) session.get(Constants.ADMIN_FACADE);
	    
	    if (adminFacade != null && adminFacade.isConnected()) {
	        throw new CLIStatusException("already_connected");
	    }
	    
	    if (timeoutInMinutes < 0) {
	        throw new CLIException("-timeout cannot be negative");
	    }
	    
	    if (progressInMinutes < 1) {
	        throw new CLIException("-progress-min must be positive");
	    }
	    
	    if (timeoutInMinutes > 0 && timeoutInMinutes < progressInMinutes) {
	        throw new CLIException("-timeout must be bigger than -progress-min");
	    }
	    
	    long end = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeoutInMinutes);
	    if (end < 0) {
	        throw new CLIException("-timeout caused an overflow. please use a smaller value");
	    }
	    
	    if (!applicationFile.exists()) {
	        throw new CLIException("Could not find application at: " + applicationFile.getPath());
	    }
	    
	    if (!azureDeploymentSlotName.equals(AzureSlot.Production.getSlot()) && 
	        !azureDeploymentSlotName.equals(AzureSlot.Staging.getSlot())) {
	        throw new CLIException("azure-deployment-slot must be either " + AzureSlot.Production.getSlot() + 
	                " or " + AzureSlot.Staging.getSlot());
	    }
	    
		Properties properties = AzureUtils.getAzureProperties();
		
		String storageAccount = AzureUtils.getProperty(properties,"storageAccount");
		String storageAccessKey = AzureUtils.getProperty(properties,"storageAccessKey");
		String storageBlobContainerName = AzureUtils.getProperty(properties,"storageBlobContainerName");
		String rdpLoginUsername = AzureUtils.getProperty(properties, "rdpLoginUsername");
		String rdpLoginEncrypedPassword = AzureUtils.getProperty(properties, "rdpLoginEncrypedPassword");
		String subscriptionId = AzureUtils.getProperty(properties, "subscriptionId");
		String certificateThumbprint = AzureUtils.getProperty(properties, "certificateThumbprint");
		
		File templateFolder = AzureUtils.getFileProperty(properties,"workerRoleFolder");
		File cspackFolder = AzureUtils.getFileProperty(properties,"cspackFolder");
		File rdpCertFile = AzureUtils.getFileProperty(properties,"rdpCertFile");
		File rdpPfxFile = AzureUtils.getFileProperty(properties,"rdpPfxFile");
		
		// read application dsl (applicationFile)
		Application application = ServiceReader.getApplicationFromFile(this.applicationFile).getApplication();

		if (azureHostedServiceName == null) {
			azureHostedServiceName = application.getName();
		}
		if (azureDeploymentName == null) {
			azureDeploymentName = azureHostedServiceName;
			if (AzureSlot.Staging == AzureSlot.fromString(azureDeploymentSlotName)) {
				azureDeploymentName += "-staging";
			}
		}

		List<AzureServiceDefinition> serviceDefinitions = new ArrayList<AzureServiceDefinition>();
		
		// TODO support statful/stateless/datagrid
		List<Service> services = application.getServices();
		for (Service service : services) {
			if(service.getNetwork() != null) {
				int port = service.getNetwork().getPort();
		        serviceDefinitions.add(new AzureServiceDefinition(service.getName(), calculateNumberOfInstances(service), port, port));
			} else {
				serviceDefinitions.add(new AzureServiceDefinition(service.getName(), calculateNumberOfInstances(service)));
			}
		}

		File projectDirectory = createTempDirectory(new File(templateFolder,".."));
		
		updateConfigurationFiles(serviceDefinitions, templateFolder, projectDirectory,
		        storageAccount, storageAccessKey, rdpLoginUsername, rdpLoginEncrypedPassword);
		
		List<String> serviceNames = new ArrayList<String>();
		for (Service service : application.getServices()) {
		    serviceNames.add(service.getName());
		}
		
		File cspkgFile = cspackCspkg(cspackFolder ,projectDirectory, serviceNames);
		File cscfgFile = new File(cspkgFile.getParent(), "ServiceConfiguration.Cloud.cscfg");
		
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
		azureDeploymentWrapper.setProgressInMinutes(progressInMinutes);
		azureDeploymentWrapper.setAzureHostedServiceName(azureHostedServiceName);
		azureDeploymentWrapper.setAzureDeploymentSlotName(azureDeploymentSlotName);
		azureDeploymentWrapper.setCertificateThumbprint(certificateThumbprint);
		azureDeploymentWrapper.setSubscriptionId(subscriptionId);
		azureDeploymentWrapper.setStorageAccount(storageAccount);
		azureDeploymentWrapper.setStorageAccessKey(storageAccessKey);
		azureDeploymentWrapper.setStorageBlobContainerName(storageBlobContainerName);
		azureDeploymentWrapper.setTimeoutErrorMessage(TIMEOUT_ERROR_STRING);
		azureDeploymentWrapper.createDeployment(azureDeploymentName, cscfgFile, cspkgFile);
		
		if (timeoutInMinutes > 0) {
		    
		    logger.info(ShellUtils.getExpectedExecutionTimeMessage());
		    waitForStatus(azureDeploymentWrapper,AzureDeploymentStatus.Running,ShellUtils.millisUntil(TIMEOUT_ERROR_STRING,end),TimeUnit.MILLISECONDS);
    		try {
    			URI url = azureDeploymentWrapper.connectAndWait(adminFacade, ShellUtils.millisUntil(TIMEOUT_ERROR_STRING,end),TimeUnit.MILLISECONDS);
    			logger.log(Level.INFO, "Cloudify REST gateway URL is " + url);
        		
        		int numberOfMachines = 0;
        		numberOfMachines += 1; // Ui instance
        		numberOfMachines += 2; // Management instances
        		for (String service : serviceNames) {
        		    numberOfMachines += cscfg.getNumberOfInstances(service);
        		}
        		logger.log(Level.INFO,"Waiting for " + numberOfMachines + " instances to start");
        		azureDeploymentWrapper.waitForNumberOfMachines(adminFacade, numberOfMachines, ShellUtils.millisUntil(TIMEOUT_ERROR_STRING,end), TimeUnit.MILLISECONDS);
        		logger.log(Level.INFO,"Azure travel application bootstrapping complete. Cloudify REST gateway URL is "+ url);
    		} catch (TimeoutException e) {
    		    logger.log(Level.INFO, MessageFormat.format(messages.getString("operation_timeout"), "azure:bootstrap-application"));
    		}
		}
		
	    // this line is intentionally not in a finally block since we don't want the folder 
        // to be deleted if there is a problem that requires manual inspection.
        deleteDirectory(projectDirectory);
		
		return messages.getString("bootstrap_succesfully"); 
	}

	private File cspackCspkg(File cspackDirectory, File projectDirectory, List<String> serviceNames) throws CLIException,
			InterruptedException {
		logger.log(Level.INFO, "Creating " + azureHostedServiceName+".cspkg");
		File csPackExe = new File(cspackDirectory,"cspack.exe");
				
		List<String> commands = new ArrayList<String>(Arrays.asList(
	        csPackExe.toString(),
            "ServiceDefinition.csdef",
            "/out:" + azureHostedServiceName + ".cspkg",
	        "/role:ui;..\\ui;UIRole.dll",
	        "/role:management;..\\management;ManagementRole.dll"
		));
	
		for (String serviceName : serviceNames) {
		    commands.add("/role:" + serviceName + ";..\\internal;InternalWorkerRole.dll");
		}
		
		executeCommandLine(projectDirectory, commands.toArray(new String[commands.size()]));
		
		return new File(projectDirectory,"" + azureHostedServiceName + ".cspkg");
	}

	private File updateConfigurationFiles(List<AzureServiceDefinition> serviceList, File inDirectory, File outDirectory,
	        String storageAccount, String storageAccessKey,
	        String rdpLoginUsername, String rdpLoginEncryptedPassword) throws XMLXPathEditorException {
	    AzureServiceDefinition[] services = serviceList.toArray(new AzureServiceDefinition[serviceList.size()]);
		File cspkg = new File(azureHostedServiceName+".cspkg"); 
		AzureDeploymentConfigurationFile cscfgFile = new AzureDeploymentConfigurationFile(new File(inDirectory, "ServiceConfiguration.Cloud.cscfg"));
		AzureDeploymentDefinitionFile csdefFile = new AzureDeploymentDefinitionFile(new File(inDirectory, "ServiceDefinition.csdef"));

		String cscfgFileName = "ServiceConfiguration.Cloud.cscfg";
		String csdefFileName = "ServiceDefinition.csdef";
		
		cscfgFile.setDeploymentName(azureHostedServiceName);
		cscfgFile.setServices(services);
		cscfgFile.setBlobStoreAccountCredentials(storageAccount, storageAccessKey);
		cscfgFile.setRdpLoginCredentials(rdpLoginUsername, rdpLoginEncryptedPassword);
		
		csdefFile.setDeploymentName(azureHostedServiceName);
		csdefFile.setServices(services);
		
		logger.log(Level.INFO,"Creating updated ServiceConfiguration.Cloud.cscfg");
		cscfgFile.writeTo(new File(outDirectory, cscfgFileName));
		csdefFile.writeTo(new File(outDirectory, csdefFileName));
		return cspkg;
	}
	
	static File createTempDirectory(File parentDirectory) throws IOException {
		final File temp;

		temp = File.createTempFile("temp", Long.toString(System.nanoTime()),parentDirectory);

		if (!(temp.delete())) {
			throw new IOException("Could not delete temp file: "
					+ temp.getAbsolutePath());
		}

		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: "
					+ temp.getAbsolutePath());
		}

		return (temp);
	}

	/**
	 * Force deletion of directory
	 * @param path
	 * @return
	 */
	static public boolean deleteDirectory(File path) {
	    if (path.exists()) {
	        File[] files = path.listFiles();
	        for (int i = 0; i < files.length; i++) {
	            if (files[i].isDirectory()) {
	                deleteDirectory(files[i]);
	            } else {
	                files[i].delete();
	            }
	        }
	    }
	    return (path.delete());
	}

	String executeCommandLine(File workingDirectory, final String[] cmd) throws CLIException, InterruptedException {
		
		try {

			final ProcessBuilder pb = new ProcessBuilder(cmd);
			
			pb.redirectErrorStream(true);

			final StringBuilder sb = new StringBuilder();
			pb.directory(workingDirectory);
			logger.fine("Executing command: " + Arrays.toString(cmd));
			final Process p = pb.start();

			final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = reader.readLine();
			while (line != null) {
				sb.append(line).append("\n");
				line = reader.readLine();
			}

			final String readResult = sb.toString();
			// the process is dead
			final int exitValue = p.exitValue();
			if (exitValue != 0) {
				throw new CLIException("Failed to execute command on azure. Error was: " + readResult);
			}

			return readResult;
		} catch (final IOException ioe) {
			throw new CLIException("Failed to execute get config command", ioe);
		}

	}
	    
    // TODO calculate number of instances based on machine size (as done in ServiceController)
    private static int calculateNumberOfInstances(Service service) {
        int numberOfInstances;
    	if (service.getDataGrid() != null) {
            Sla sla = service.getDataGrid().getSla();
            numberOfInstances = sla.getHighlyAvailable() ? 2 : 1;
            logger.fine("Service " + service.getName() + " is a datagrid. Starting " + numberOfInstances + " instances");
        } else if (service.getStatefulProcessingUnit() != null) {
            Sla sla = service.getStatefulProcessingUnit().getSla();
            numberOfInstances = sla.getHighlyAvailable() ? 2 : 1;
            logger.fine("Service " + service.getName() + " is a stateful processing unit. Starting " + numberOfInstances + " instances");
        } else if (service.getStatelessProcessingUnit() != null) {
        	numberOfInstances = service.getNumInstances();
        	logger.fine("Service " + service.getName() + " is a stateless processing unit. Starting " + numberOfInstances + " instances");
        } else {
        	numberOfInstances = service.getNumInstances();
        	logger.fine("Starting " + numberOfInstances + " instances for service " + service.getName());
        }
    	return numberOfInstances;
    }
    
    public void waitForStatus(final AzureDeploymentWrapper azureDeploymentWrapper, final AzureDeploymentStatus status, long timeout, TimeUnit timeunit) throws InterruptedException, TimeoutException, CLIException {
        
    	createConditionLatch(timeout, timeunit)
		.waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone() throws CLIException,
					InterruptedException {
				AzureDeploymentStatus currentStatus = azureDeploymentWrapper.getStatus();
				logger.log(Level.INFO, "Current status: " + currentStatus.getStatus() + ". Waiting for status " + status);
				return currentStatus == status;
			}
			
		});
    }

    private ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
		return 
			new ConditionLatch()
			.timeout(timeout,timeunit)
			.pollingInterval(progressInMinutes, TimeUnit.MINUTES)
			.timeoutErrorMessage(TIMEOUT_ERROR_STRING)
			.verbose(verbose);
	}


}
