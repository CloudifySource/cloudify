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
package org.cloudifysource.rest.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLApplicationCompilatioResult;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.controllers.ServiceController;

import com.j_spaces.kernel.Environment;

public class ApplicationInstallerRunnable implements Runnable {

	private static final int SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES = 60;

	private static final java.util.logging.Logger logger = java.util.logging.Logger
	.getLogger(ApplicationInstallerRunnable.class.getName());

	private ServiceController controller;
	private DSLApplicationCompilatioResult result;
	private String applicationName;

	private List<Service> services;

	private Cloud cloud;

	public ApplicationInstallerRunnable(ServiceController controller,
			DSLApplicationCompilatioResult result, String applicationName,
			List<Service> services, Cloud cloud) {
		super();
		this.controller = controller;
		this.result = result;
		this.applicationName = applicationName;
		this.services = services;
		this.cloud = cloud;
	}

	@Override
	public void run() {

		File appDir = result.getApplicationDir();

		// final List<Service> services = application.getServices();

		logger.fine("Installing application " + applicationName
				+ " with the following services: " + services);

		final boolean asyncInstallPossible = isAsyncInstallPossibleForApplication();
		logger.info("async install setting: " + asyncInstallPossible);
		installServices(appDir, applicationName, asyncInstallPossible, cloud);
		try {
			FileUtils.deleteDirectory(appDir);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void installServices(File appDir, String applicationName, final boolean async, final Cloud cloud) {
		// TODO: refactor the last part of this method
		logger.info("Installing service for application: " + applicationName + ". Async install: " + async +". Number of services: " + this.services.size());
		for (final Service service : services) {
			logger.info("Installing service: " + service.getName() + " for application: " + applicationName);
			service.getCustomProperties().put("usmJarPath",
					Environment.getHomeDirectory() + "/lib/platform/usm");

			final Properties contextProperties = createServiceContextProperties(
					service, applicationName, async, cloud);

			final String serviceName = service.getName();
			boolean found = false;
			try {
				String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
				//Pack the folder and name it absolutePuName
				File packedFile = Packager.pack(new File(appDir, serviceName), absolutePUName);
				result.getApplicationFile().delete();
				packedFile.deleteOnExit();
				//Deployment will be done using the service's absolute PU name.
				logger.info("Deploying PU: " + absolutePUName + ". File: " + packedFile + ". Properties: " + contextProperties);
				final String templateName = (service.getCompute() == null ? null : service.getCompute().getTemplate() );
				controller.deployElasticProcessingUnit(absolutePUName,
						applicationName, serviceName, packedFile,
						contextProperties, templateName, true, 0, TimeUnit.SECONDS);
				try { 
					FileUtils.deleteDirectory(packedFile.getParentFile());
				} catch(IOException ioe) {
					// sometimes this delete fails. Not sure why. Maybe deploy is async?
					logger.warning("Failed to delete temporary directory: " + packedFile.getParentFile());
				}
				
				if (!async) {
					logger.info("Waiting for instance of service: " +serviceName + " of application: " + applicationName);
					boolean instanceFound = controller.waitForServiceInstance(
							applicationName, serviceName,
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
			} catch (Exception e) {
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
	}

	public boolean isAsyncInstallPossibleForApplication() {

		// check if all services are USM
		for (Service service : this.services) {
			if (service.getLifecycle() == null) {
				return false;
			}
		}

		return true;
	}

	private Properties createServiceContextProperties(final Service service, String applicationName,
			final boolean async, final Cloud cloud) {
		final Properties contextProperties = new Properties();

		if (service.getDependsOn() != null) {
			String serviceNames = service.getDependsOn().toString();
			serviceNames = serviceNames.substring(1, serviceNames.length() - 1);
			if (serviceNames.equals("")){
				contextProperties.setProperty(
						CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, "[]");
			}else{
				String[] splitServiceNames = serviceNames.split(",");
				List<String> absoluteServiceNames = new ArrayList<String>();
				for (String name : splitServiceNames) {
					absoluteServiceNames.add(ServiceUtils.getAbsolutePUName(applicationName, name.trim()));
				}
				contextProperties.setProperty(
						CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, 
						Arrays.toString(absoluteServiceNames.toArray()));
			}
		}
		if (service.getType() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_TYPE,
					service.getType());
		}
		if (service.getIcon() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_ICON,
					CloudifyConstants.SERVICE_EXTERNAL_FOLDER + service.getIcon());
		}
		if (service.getNetwork() != null) {
			contextProperties
			.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_NETWORK_PROTOCOL_DESCRIPTION,
					service.getNetwork().getProtocolDescription());
		}

		contextProperties.setProperty(
				CloudifyConstants.CONTEXT_PROPERTY_ASYNC_INSTALL,
				Boolean.toString(async));
		
		if(cloud != null) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_CLOUD_NAME, cloud.getName());
		}
		
		contextProperties.setProperty(
				CloudifyConstants.CONTEXT_PROPERTY_ELASTIC,
				Boolean.toString(service.isElastic()));
		return contextProperties;
	}

}
