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
package com.gigaspaces.cloudify.shell.commands;

import java.io.File;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.jansi.Ansi.Color;

import com.gigaspaces.cloudify.dsl.Application;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.dsl.internal.packaging.Packager;
import com.gigaspaces.cloudify.shell.Constants;
import com.gigaspaces.cloudify.shell.GigaShellMain;

@Command(scope = "cloudify", name = "install-application", description = "Installs an application. If you specify a folder path it will be packed and deployed. If you sepcify an application archive, the shell will deploy that file.")
public class InstallApplication extends AdminAwareCommand {

	@Argument(required = true, name = "application-file", description = "The application recipe file path, folder or archive")
	File applicationFile;

	@Option(required = false, name = "-name", description = "The name of the application")
	private String applicationName = null;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. Defaults to 10 minutes.")
	int timeoutInMinutes=10;

	@Option(required = false, name = "-progress", description = "The polling time interval in minutes, used for checking if the operation is done. Defaults to 1 minute. Use together with the -timeout option")
	int progressInMinutes=1;

	final String TIMEOUT_ERROR_MESSAGE = "Application installation timed out";

	@Override
	protected Object doExecute() throws Exception {
		if (!applicationFile.exists()) {
			throw new CLIStatusException("application_not_found", applicationFile.getAbsolutePath());
		}

		logger.info("Validating file " + applicationFile.getName());
		final Application application = ServiceReader.getApplicationFromFile(applicationFile).getApplication();

		normalizeApplicationName(application);
		
		if (adminFacade.getApplicationsList().contains(applicationName)){
			throw new CLIStatusException("application_already_deployed", application.getName());
		}

		File zipFile = null;
		if (!applicationFile.isFile()) {			
			zipFile = Packager.packApplication(application, applicationFile);						
		} else {
			if ((applicationFile.getName().endsWith(".zip"))
					|| (applicationFile.getName().endsWith(".jar"))) {
				zipFile = applicationFile;
			} else {
				throw new CLIStatusException(
						"application_file_format_mismatch",
						applicationFile.getPath());
			}
		}
		
		// toString of string list (i.e. [service1, service2])
		logger.info("Uploading application " + applicationName);
		String serviceOrder = adminFacade.installApplication(zipFile,
				applicationName);
		//If temp file was created, Delete it.
		if (!applicationFile.isFile()){
			zipFile.delete();
		}
		
		if (serviceOrder.charAt(0) != '[' && serviceOrder.charAt(serviceOrder.length()-1) != ']') {
			throw new IllegalStateException("Cannot parse service order response: " + serviceOrder);
		}
		if (serviceOrder.length() > 2) {
			serviceOrder = serviceOrder.substring(1,serviceOrder.length() -1);
			logger.fine("Services will be installed in the following order: " + serviceOrder);
			printApplicationInfo(application);
			for (String serviceName : serviceOrder.split(Pattern.quote(","))) {
				String trimmedServiceName = serviceName.trim();
				Service service = getServiceByName(application, trimmedServiceName);
				int plannedNumberOfInstances = service.getNumInstances();
				adminFacade.waitForServiceInstances(trimmedServiceName, applicationName, plannedNumberOfInstances, TIMEOUT_ERROR_MESSAGE, timeoutInMinutes, TimeUnit.MINUTES);
				logger.info(MessageFormat.format(
						   messages.getString("service_install_ended"), trimmedServiceName));
			}
		}
		
        session.put(Constants.ACTIVE_APP, applicationName);
        GigaShellMain.getInstance().setCurrentApplicationName(applicationName);
        
		return this.getFormattedMessage("application_installed_succesfully", Color.GREEN, applicationName);
	}

	private void printApplicationInfo(Application application) {
		logger.info("Application [" + applicationName + "] with " + application.getServices().size() + " services");
		for (Service  service : application.getServices()) {
			if (service.getDependsOn().isEmpty()){
				logger.info("Service [" + service.getName() + "] " 
						+ service.getNumInstances() 
						+ " planned instances");
			}else{//Service has dependencies
				logger.info("Service [" + service.getName() + "] depends on " 
						+ service.getDependsOn().toString() + " "
						+ service.getNumInstances() 
						+ " planned instances");				
			}
		}
	}

	private void normalizeApplicationName(Application application) {
		if ((applicationName == null) || (applicationName.length() == 0)) {
			applicationName = application.getName();
		}
		if ((applicationName == null) || (applicationName.length() == 0)) {
			applicationName = applicationFile.getName();
			final int endIndex = applicationName.lastIndexOf('.');
			if (endIndex > 0) {
				applicationName = applicationName.substring(0, endIndex);
			}
		}
	}

	private Service getServiceByName(Application application, String serviceName) {
		for (Service service : application.getServices() ) {
			if (serviceName.equals(service.getName())) {
				return service;
			}
		}
		throw new IllegalStateException("Cannot find service " + serviceName + " in application.");
	}
}
