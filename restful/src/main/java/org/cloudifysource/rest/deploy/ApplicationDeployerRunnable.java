/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.deploy;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.controllers.DeploymentsController;
import org.cloudifysource.rest.controllers.helpers.PropertiesOverridesMerger;

import com.j_spaces.kernel.Environment;

/**
 * A Runnable implementation that executes the deployment logic of an application.
 * 
 * @author adaml
 *
 */
public class ApplicationDeployerRunnable implements Runnable {
	private static final int SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES = 60;

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ApplicationDeployerRunnable.class.getName());

	private final InstallApplicationRequest installApplicationRequest;
	private final File appFile;
	private final File appDir;
	private final String applicationName;
	private final List<Service> services;
	private final String deploymentID;
	private final File applicationPropertiesFile;
	private final File applicationOverridesFile;
	private DeploymentsController controller;

	/**
	 * Constructor.
	 * @param controller
	 * 		installation requests are delegated to this controller.
	 * @param request
	 * 		the install application request.
	 * @param result
	 * 		the application compilation result.
	 * @param services
	 * 		the list of services.
	 * @param overridesFile
	 * 		application overrides file.
	 */
	public ApplicationDeployerRunnable(final ApplicationDeployerRequest request) {
		this.installApplicationRequest = request.getRequest();
		this.appFile = request.getAppFile();
		this.appDir = request.getAppDir();
		this.applicationName = installApplicationRequest.getApplicationName();
		this.services = request.getServices();
		this.deploymentID = request.getDeploymentID();
		this.applicationPropertiesFile = request.getAppPropertiesFile();
		this.applicationOverridesFile = request.getAppOverridesFile();
		this.controller = request.getController();
	}

	@Override
	public void run() {
		logger.fine("Installing application " + applicationName + " with the following services: " + services);

		final boolean asyncInstallPossible = isAsyncInstallPossibleForApplication();
		logger.info("Async install setting is " + asyncInstallPossible);
		try {
			installServices(asyncInstallPossible);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void installServices(final boolean async)
			throws IOException {

		logger.info("Installing services for application: " + applicationName 
				+ ". Async install: " + async + ". Number of services: " + this.services.size());
		
		for (final Service service : services) {			
			final String serviceName = service.getName();
			final String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
			logger.info("Installing service: " + absolutePUName);

			service.getCustomProperties().put("usmJarPath", Environment.getHomeDirectory() + "/lib/platform/usm");
			boolean found = false;
			try {
				final File serviceDir = new File(appDir, serviceName);
				File servicePropertiesFile = DSLReader.findDefaultDSLFileIfExists(
						DSLUtils.SERVICE_PROPERTIES_FILE_NAME_SUFFIX, serviceDir);
				if (servicePropertiesFile == null 
						&& (applicationOverridesFile != null || applicationPropertiesFile != null)) {
					// creating the service's properties to be used as the destination file of the merging process.
					String propertiesFileName = 
							DSLUtils.getPropertiesFileName(serviceDir, DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);
					logger.finer("Application [" + applicationName 
							+ "] has overrides or properties file but the service [" + serviceName 
							+ "], does not have properties file, so, creating an empty properties file [" 
							+ propertiesFileName 
							+ "] to contain the merge of the application's overrides and properties files.");
					servicePropertiesFile = new File(serviceDir, propertiesFileName);
				}
				// merge service properties with application properties and overrides files 
				// merge into service's properties file
				PropertiesOverridesMerger merger = new PropertiesOverridesMerger(
						servicePropertiesFile, 
						applicationPropertiesFile, 
						servicePropertiesFile, 
						applicationOverridesFile);
				merger.merge();
				
				// Pack the folder and name it absolutePuName
				final File packedFile = Packager.pack(service, 
						serviceDir, 
						absolutePUName, 
						null /* additionalServiceFiles */);
				appFile.delete();
				packedFile.deleteOnExit();

				// Deployment will be done using the service's absolute PU name.
				final InstallServiceRequest installServiceReq = createInstallServiceRequest();
				
				final ServiceApplicationDependentProperties serviceProps = new ServiceApplicationDependentProperties();
				serviceProps.setDependsOn(service.getDependsOn());
						
				controller.installServiceInternal(
						applicationName, 
						serviceName,
						installServiceReq, 
						deploymentID,
						serviceProps,
						service,
						packedFile);
				try {
					FileUtils.deleteDirectory(packedFile.getParentFile());
				} catch (final IOException ioe) {
					// sometimes this delete fails. Not sure why. Maybe deploy
					// is async?
					logger.warning("Failed to delete temporary directory: " + packedFile.getParentFile());
				}

				if (!async) {
					logger.info("Waiting for instance of service: " + serviceName 
							+ " of application: "	+ applicationName);
					final boolean instanceFound = controller
							.waitForServiceInstance(applicationName,
									serviceName,
									SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES,
									TimeUnit.MINUTES);
					if (!instanceFound) {
						throw new TimeoutException(
								"Service "
										+ serviceName
										+ " of application "
										+ applicationName
										+ " was installed, but no instance of the service has started after "
										+ SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES
										+ " minutes.");
					}
					logger.info("Found instance of: " + serviceName);
				}

				found = true;
				logger.fine("service " + service + " deployed.");
			} catch (final Exception e) {
				logger.log(
						Level.SEVERE,
						"Failed to install service: "
								+ serviceName
								+ " of application: "
								+ applicationName
								+ ". Application installation will halt. "
								+ "Some services may already have started, and should be shutdown manually. Error was: "
								+ e.getMessage(), e);
				return;
			}

			if (!found) {
				logger.severe("Failed to find an instance of service: "
						+ serviceName
						+ " while installing application "
						+ applicationName
						+ ". Application installation will stop. Some services may have been installed!");
				return;
			}
		}
		FileUtils.deleteDirectory(appDir);
	}

	InstallServiceRequest createInstallServiceRequest() {
		final InstallServiceRequest installServiceReq = new InstallServiceRequest();
		installServiceReq.setCloudOverridesUploadKey(installApplicationRequest.getCloudOverridesUploadKey());
		installServiceReq.setCloudConfigurationUploadKey(installApplicationRequest.getCloudConfigurationUploadKey());
		installServiceReq.setAuthGroups(installApplicationRequest.getAuthGroups());
		installServiceReq.setDebugAll(installApplicationRequest.isDebugAll());
		installServiceReq.setDebugEvents(installApplicationRequest.getDebugEvents());
		installServiceReq.setDebugMode(installApplicationRequest.getDebugMode());
		installServiceReq.setSelfHealing(installApplicationRequest.isSelfHealing());
		installServiceReq.setServiceFileName(null);
		installServiceReq.setTimeoutInMillis(0);

		return installServiceReq;
	}

	/**
	 *
	 * @return true if all services have Lifecycle events.
	 */
	public boolean isAsyncInstallPossibleForApplication() {
		// check if all services are USM
		for (final Service service : this.services) {
			if (service.getLifecycle() == null) {
				return false;
			}
		}

		return true;
	}
}
