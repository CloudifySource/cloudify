package com.gigaspaces.cloudify.rest.util;

import java.io.File;
import java.io.IOException;
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

	private static final int SERVICE_INSTANCE_STARTUP_TIMEOUT_MINUTES = 10;

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

		for (final Service service : services) {
			service.getCustomProperties().put("usmJarPath",
					Environment.getHomeDirectory() + "/lib/platform/usm");

			final Properties contextProperties = createServiceContextProperties(service);

			final String serviceName = service.getName();
			boolean found = false;
			File packedFile = null;
			try {
				
				packedFile = Packager.pack(new File(appDir, serviceName));
				result.getApplicationFile().delete();
				packedFile.deleteOnExit();
				String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
				controller.deployElasticProcessingUnit(absolutePUName,
						applicationName, absolutePUName, packedFile,
						contextProperties);
				boolean instanceFound = controller.waitForServiceInstance(
						applicationName, absolutePUName,
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
			}finally{
				try{
					if (packedFile != null){
						FileUtils.deleteDirectory(packedFile.getParentFile());
					}
				}catch(IOException e){
					logger.fine("Unable to delete temp applicaiton file " + packedFile.getName());
				}
			}

			if (!found) {
				logger.severe("Failed to find an instance of service: "
						+ serviceName
						+ " while installing application "
						+ applicationName
						+ ". Application installation will stop. Some services may have been installed!");
				return;

				// return "Failed to find an instance of service: "
				// + serviceName
				// + " while installing application "
				// + applicationName
				// +
				// ". Application installation will stop. Some services may have been installed!";
			}

		}
		try {
			FileUtils.deleteDirectory(appDir);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private Properties createServiceContextProperties(final Service service) {
		final Properties contextProperties = new Properties();

		// contextProperties.setProperty("com.gs.application.services",
		// serviceNamesString);
		if (service.getDependsOn() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, service
							.getDependsOn().toString());
		}
		if (service.getType() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_TYPE,
					service.getType());
		}
		if (service.getIcon() != null) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_SERVICE_ICON,
					service.getIcon());
		}
		if (service.getNetwork() != null) {
			contextProperties
					.setProperty(
							CloudifyConstants.CONTEXT_PROPERTY_NETWORK_PROTOCOL_DESCRIPTION,
							service.getNetwork().getProtocolDescription());
		}
		return contextProperties;
	}

}
