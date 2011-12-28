package com.gigaspaces.cloudify.rest.util;

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

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.internal.DSLApplicationCompilatioResult;
import com.gigaspaces.cloudify.dsl.internal.packaging.Packager;
import com.gigaspaces.cloudify.dsl.utils.ServiceUtils;
import com.gigaspaces.cloudify.rest.controllers.ServiceController;
import com.j_spaces.kernel.Environment;

public class ApplicationInstallerRunnable implements Runnable {

	private static final int SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES = 30;

	private static final java.util.logging.Logger logger = java.util.logging.Logger
	.getLogger(ApplicationInstallerRunnable.class.getName());

	private ServiceController controller;
	private DSLApplicationCompilatioResult result;
	private String applicationName;

	private List<Service> services;

	public ApplicationInstallerRunnable(ServiceController controller,
			DSLApplicationCompilatioResult result, String applicationName,
			List<Service> services) {
		super();
		this.controller = controller;
		this.result = result;
		this.applicationName = applicationName;
		this.services = services;
	}

	@Override
	public void run() {
		File appDir = result.getApplicationDir();

		// final List<Service> services = application.getServices();

		logger.fine("Installing application " + applicationName
				+ " with the following services: " + services);

		final boolean asyncInstallPossible = isAsyncInstallPossibleForApplication();
		logger.info("async install setting: " + asyncInstallPossible);
		installServices(appDir, applicationName, asyncInstallPossible);
		try {
			FileUtils.deleteDirectory(appDir);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void installServices(File appDir, String applicationName, final boolean async) {
		// TODO: refactor the last part of this method
		logger.info("Installing service for application: " + applicationName + ". Async install: " + async +". Number of services: " + this.services.size());
		for (final Service service : services) {
			logger.info("Installing service: " + service.getName() + " for application: " + applicationName);
			service.getCustomProperties().put("usmJarPath",
					Environment.getHomeDirectory() + "/lib/platform/usm");

			final Properties contextProperties = createServiceContextProperties(
					service, applicationName, async);

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
						contextProperties, templateName);
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

	private boolean isAsyncInstallPossibleForApplication() {

		// check if all services are USM
		for (Service service : this.services) {
			if (service.getLifecycle() == null) {
				return false;
			}
		}

		return true;
	}

	private Properties createServiceContextProperties(final Service service, String applicationName,
			final boolean async) {
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
					CloudifyConstants.SERVICE_EXTERNAL_FOLDER + CloudifyConstants.CONTEXT_PROPERTY_SERVICE_ICON,
					service.getIcon());
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
		return contextProperties;
	}

}
