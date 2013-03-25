/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLApplicationCompilatioResult;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.packaging.FileAppender;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.controllers.ServiceController;

import com.j_spaces.kernel.Environment;

/**********
 * A Runnable implementation that executes the deployment logic of an application.
 *
 * @author adaml, barakme
 * @since 2.0
 */
public class ApplicationInstallerRunnable implements Runnable {

	private static final int SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES = 60;

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ApplicationInstallerRunnable.class.getName());

	private final ServiceController controller;
	private final DSLApplicationCompilatioResult result;
	private final String applicationName;
	private final File overridesFile;
	private final String authGroups;
	private final List<Service> services;
	private final Cloud cloud;
	private final boolean selfHealing;
	private final File cloudOverrides;
	private UUID pollingTaskId;

	private final boolean debugAll;

	private final String debugModeString;

	private final String debugEvents;

	/**************
	 * Constructor.
	 *
	 * @param controller
	 *            installation requests are delegated to this controller.
	 * @param result
	 *            the application compilation result.
	 * @param applicationName
	 *            the application name.
	 * @param overridesFile
	 *            Application overrides file.
	 * @param authGroups
	 *            Security authorization groups for this application.
	 * @param services
	 *            the list of services.
	 * @param cloud
	 *            the cloud configuration object.
	 * @param selfHealing
	 *            true if self healing is enabled for all services in this application, false if it is disabled for
	 *            them.
	 * @param cloudOverrides
	 *            cloud configuration overrides for all services in this application.
	 * @param debugAll
	 * @param debugModeString
	 * @param debugEvents
	 */
	public ApplicationInstallerRunnable(
			final ServiceController controller,
			final DSLApplicationCompilatioResult result,
			final String applicationName,
			final File overridesFile,
			final String authGroups,
			final List<Service> services,
			final Cloud cloud,
			final boolean selfHealing,
			final File cloudOverrides,
			final boolean debugAll, final String debugEvents, final String debugModeString) {
		super();
		this.controller = controller;
		this.result = result;
		this.applicationName = applicationName;
		this.overridesFile = overridesFile;
		this.services = services;
		this.cloud = cloud;
		this.selfHealing = selfHealing;
		this.authGroups = authGroups;
		this.cloudOverrides = cloudOverrides;
		this.debugAll = debugAll;
		this.debugModeString = debugModeString;
		this.debugEvents = debugEvents;
	}

	@Override
	public void run() {

		final File appDir = result.getApplicationDir();

		logger.fine("Installing application " + applicationName + " with the following services: " + services);

		final boolean asyncInstallPossible = isAsyncInstallPossibleForApplication();
		logger.info("Async install setting is " + asyncInstallPossible);
		try {
			installServices(appDir, applicationName, authGroups, asyncInstallPossible, cloud, cloudOverrides);
			FileUtils.deleteDirectory(appDir);
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	private void installServices(
			final File appDir,
			final String applicationName,
			final String authGroups,
			final boolean async,
			final Cloud cloud,
			final File cloudOverrides)
			throws IOException {
		logger.info("Installing services for application: " + applicationName + ". Async install: " + async + ". Number of services: " + this.services.size());
		for (final Service service : services) {
			logger.info("Installing service: " + service.getName() + " for application: " + applicationName);
			service.getCustomProperties().put("usmJarPath",
					Environment.getHomeDirectory() + "/lib/platform/usm");

			final Properties contextProperties = createServiceContextProperties(
					service, applicationName, async, cloud);

			final String serviceName = service.getName();
			final String absolutePUName = ServiceUtils.getAbsolutePUName(
					applicationName, serviceName);
			final File serviceDirectory = new File(appDir, serviceName);

			// scan for service cloud configuration file

			final File serviceCloudConfiguration = new File(serviceDirectory,
					CloudifyConstants.SERVICE_CLOUD_CONFIGURATION_FILE_NAME);
			byte[] serviceCloudConfigurationContents = null;
			if (serviceCloudConfiguration.exists()) {
				serviceCloudConfigurationContents = FileUtils
						.readFileToByteArray(serviceCloudConfiguration);
				FileUtils.forceDelete(serviceCloudConfiguration);
			}

			boolean found = false;

			try {
				// this will actually create an empty props file.
				final FileAppender appender = new FileAppender("finalPropsFile.properties");
				final LinkedHashMap<File, String> filesToAppend = new LinkedHashMap<File, String>();

				// first add the application properties file. least important overrides.
				// lookup application properties file
				final File applicationPropertiesFile =
						DSLReader.findDefaultDSLFileIfExists(DSLUtils.APPLICATION_PROPERTIES_FILE_NAME, appDir);
				filesToAppend.put(applicationPropertiesFile, "Application Properties File");
				// add the service properties file, second level overrides.
				// lookup service properties file
				final String propertiesFileName = DSLUtils.getPropertiesFileName(serviceDirectory,
						DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX);
				final File servicePropertiesFile = new File(serviceDirectory, propertiesFileName);
				filesToAppend.put(servicePropertiesFile, "Service Properties File");
				// lookup overrides file
				File actualOverridesFile = overridesFile;
				if (actualOverridesFile == null) {
					// when using the CLI, the application overrides file is inside the directory
					actualOverridesFile =
							DSLReader.findDefaultDSLFileIfExists(DSLUtils.APPLICATION_OVERRIDES_FILE_NAME, appDir);
				}
				// add the overrides file given in the command or via REST, most important overrides.
				filesToAppend.put(actualOverridesFile, "Overrides Properties File");
				/*
				 * name the merged properties file as the original properties file. this will allow all properties to be
				 * available by anyone who parses the default properties file. (like Lifecycle scripts)
				 */
				appender.appendAll(servicePropertiesFile, filesToAppend);

				// Pack the folder and name it absolutePuName
				final File packedFile = Packager.pack(service, serviceDirectory, absolutePUName, null);
				result.getApplicationFile().delete();
				packedFile.deleteOnExit();
				// Deployment will be done using the service's absolute PU name.
				logger.info("Deploying PU: " + absolutePUName + ". File: "
						+ packedFile + ". Properties: " + contextProperties);
				final String templateName = service.getCompute() == null ? null
						: service.getCompute().getTemplate();
				controller.deployElasticProcessingUnit(
						absolutePUName,
						applicationName,
						authGroups,
						serviceName,
						packedFile,
						contextProperties,
						templateName,
						true,
						0,
						TimeUnit.SECONDS,
						serviceCloudConfigurationContents,
						selfHealing,
						null /* service overrides file */,
						cloudOverrides);
				try {
					FileUtils.deleteDirectory(packedFile.getParentFile());
				} catch (final IOException ioe) {
					// sometimes this delete fails. Not sure why. Maybe deploy
					// is async?
					logger.warning("Failed to delete temporary directory: "
							+ packedFile.getParentFile());
				}

				if (!async) {
					logger.info("Waiting for instance of service: " + serviceName + " of application: "	+ applicationName);
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
				this.controller.handleDeploymentException(e, this.pollingTaskId);
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

	/**
	 * Sets the polling id for this deployment task.
	 *
	 * @param taskPollingId
	 *            polling task id
	 */
	public void setTaskPollingId(final UUID taskPollingId) {
		this.pollingTaskId = taskPollingId;
	}

	private Properties createServiceContextProperties(final Service service,
			final String applicationName, final boolean async, final Cloud cloud) {
		final Properties contextProperties = new Properties();

		if (service.getDependsOn() != null) {
			String serviceNames = service.getDependsOn().toString();
			serviceNames = serviceNames.substring(1, serviceNames.length() - 1);
			if (serviceNames.equals("")) {
				contextProperties.setProperty(
						CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, "[]");
			} else {
				final String[] splitServiceNames = serviceNames.split(",");
				final List<String> absoluteServiceNames = new ArrayList<String>();
				for (final String name : splitServiceNames) {
					absoluteServiceNames.add(ServiceUtils.getAbsolutePUName(
							applicationName, name.trim()));
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
					CloudifyConstants.SERVICE_EXTERNAL_FOLDER
							+ service.getIcon());
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

		if (cloud != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_CLOUD_NAME,
					cloud.getName());
		}

		contextProperties.setProperty(
				CloudifyConstants.CONTEXT_PROPERTY_ELASTIC,
				Boolean.toString(service.isElastic()));

		if (debugAll) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_ALL, Boolean.TRUE.toString());
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_MODE, debugModeString);
		} else if (StringUtils.isNotBlank(debugEvents)) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_EVENTS, debugEvents);
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_MODE, debugModeString);
		}

		return contextProperties;
	}

}
