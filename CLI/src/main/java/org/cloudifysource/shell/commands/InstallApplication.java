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
package org.cloudifysource.shell.commands;

import java.io.File;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, barakm, adaml
 * @since 2.0.0
 * 
 *        Installs an application, including it's contained services ordered according to their dependencies.
 * 
 *        Required arguments:
 *         application-file - The application recipe file path, folder or archive (zip/jar)
 * 
 *        Optional arguments:
 *         name - The name of the application
 *         timeout - The number of minutes to wait until the operation is completed (default: 10 minutes)
 * 
 *        Command syntax: install-application [-name name] [-timeout timeout] application-file
 */
@Command(scope = "cloudify", name = "install-application", description = "Installs an application. If you specify"
		+ " a folder path it will be packed and deployed. If you sepcify an application archive, the shell will deploy"
		+ " that file.")
public class InstallApplication extends AdminAwareCommand {

	@Argument(required = true, name = "application-file", description = "The application recipe file path, folder "
			+ "or archive")
	private File applicationFile;

	@Option(required = false, name = "-name", description = "The name of the application")
	private String applicationName = null;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation"
			+ " is done. Defaults to 10 minutes.")
	private int timeoutInMinutes = 10;

	private static final String TIMEOUT_ERROR_MESSAGE = "Application installation timed out";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		if (!applicationFile.exists()) {
			throw new CLIStatusException("application_not_found", applicationFile.getAbsolutePath());
		}

		logger.info("Validating file " + applicationFile.getName());
		final Application application = ServiceReader.getApplicationFromFile(applicationFile).getApplication();

		normalizeApplicationName(application);

		if (adminFacade.getApplicationsList().contains(applicationName)) {
			throw new CLIStatusException("application_already_deployed", application.getName());
		}

		File zipFile = null;
		if (!applicationFile.isFile()) {
			zipFile = Packager.packApplication(application, applicationFile);
		} else {
			if (applicationFile.getName().endsWith(".zip") || applicationFile.getName().endsWith(".jar")) {
				zipFile = applicationFile;
			} else {
				throw new CLIStatusException("application_file_format_mismatch", applicationFile.getPath());
			}
		}

		// toString of string list (i.e. [service1, service2])
		logger.info("Uploading application " + applicationName);
		Map<String, String> result = adminFacade.installApplication(zipFile, applicationName, timeoutInMinutes);
		String serviceOrder = result.get(CloudifyConstants.SERVICE_ORDER);

		// If temp file was created, Delete it.
		if (!applicationFile.isFile()) {
			zipFile.delete();
		}

		if (serviceOrder.charAt(0) != '[' && serviceOrder.charAt(serviceOrder.length() - 1) != ']') {
			throw new IllegalStateException("Cannot parse service order response: " + serviceOrder);
		}
		
		printApplicationInfo(application);
		if (result.containsKey(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID)){
			String pollingID = result.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);
			this.adminFacade.waitForLifecycleEvents(pollingID, timeoutInMinutes, TIMEOUT_ERROR_MESSAGE);
		} else {
			throw new CLIException("Failed to retrieve lifecycle logs from rest. " +
			"Check logs for more details.");
		}

		session.put(Constants.ACTIVE_APP, applicationName);
		GigaShellMain.getInstance().setCurrentApplicationName(applicationName);

		return this.getFormattedMessage("application_installed_succesfully", Color.GREEN, applicationName);
	}

	/**
	 * Prints Application data - the application name and it's services name, dependencies and number of
	 * instances.
	 * 
	 * @param application
	 *            Application object to analyze
	 */
	private void printApplicationInfo(final Application application) {
		logger.info("Application [" + applicationName + "] with " + application.getServices().size() + " services");
		for (final Service service : application.getServices()) {
			if (service.getDependsOn().isEmpty()) {
				logger.info("Service [" + service.getName() + "] " + service.getNumInstances() + " planned instances");
			} else { // Service has dependencies
				logger.info("Service [" + service.getName() + "] depends on " + service.getDependsOn().toString()
						+ " " + service.getNumInstances() + " planned instances");
			}
		}
	}

	/**
	 * Set the application name, according to this logic: 1. If an application name argument is passed - use
	 * it. 2. If not - use the name configured in the Application object 3. If the configured name is empty -
	 * use the application's file name (preceding the "." sign)
	 * 
	 * @param application
	 *            The Application object
	 */
	private void normalizeApplicationName(final Application application) {
		if (applicationName == null || applicationName.length() == 0) {
			applicationName = application.getName();
		}
		if (applicationName == null || applicationName.length() == 0) {
			applicationName = applicationFile.getName();
			final int endIndex = applicationName.lastIndexOf('.');
			if (endIndex > 0) {
				applicationName = applicationName.substring(0, endIndex);
			}
		}
	}

	/**
	 * Gets a service object from the given application, by service name. In case a service by that name is
	 * not found - an IllegalStateException is thrown.
	 * 
	 * @param application
	 *            The Application object containing the service
	 * @param serviceName
	 *            The name of the required service
	 * @return Service object
	 */
	private Service getServiceByName(final Application application, final String serviceName) {
		for (final Service service : application.getServices()) {
			if (serviceName.equals(service.getName())) {
				return service;
			}
		}
		throw new IllegalStateException("Cannot find service " + serviceName + " in application.");
	}
}
