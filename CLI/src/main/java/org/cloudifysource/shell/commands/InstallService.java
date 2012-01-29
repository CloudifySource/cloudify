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
package org.cloudifysource.shell.commands;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Installs a service by deploying the service files as one packed file (zip, war or jar). Service
 *        files can also be supplied as a folder containing multiple files.
 * 
 *        Required arguments:
 *         service-file - Path to the service's packed file or folder
 * 
 *        Optional arguments:
 *         zone - The machines zone in which to install the service
 *         name - The name of the service
 *         timeout - The number of minutes to wait until the operation is completed (default: 5 minutes)
 * 
 *        Command syntax: install-service [-zone zone] [-name name] [-timeout timeout] service-file
 */
@Command(scope = "cloudify", name = "install-service", description = "Installs a service. If you specify a folder"
		+ " path it will be packed and deployed. If you sepcify a service archive, the shell will deploy that file.")
public class InstallService extends AdminAwareCommand {

	@Argument(required = true, name = "service-file", description = "The service recipe folder or archive")
	private File serviceFile;

	@Option(required = false, name = "-zone", description = "The machines zone in which to install the service")
	private String zone;

	@Option(required = false, name = "-name", description = "The name of the service")
	private String serviceName = null;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is "
			+ "done. Defaults to 5 minutes.")
	private int timeoutInMinutes = 5;

	private static final String TIMEOUT_ERROR_MESSAGE = "Service installation timed out";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		if (!serviceFile.exists()) {
			throw new CLIStatusException("service_file_doesnt_exist", serviceFile.getPath());
		}

		File packedFile;

		// TODO: ask Barak. this logics should not be done twice. should be done directly in the rest server.
		// also figure out how to treat war/jar files that have no .groovy file. create default?
		int plannedNumberOfInstances = 1;
		Service service = null;
		try {
			if (serviceFile.getName().endsWith(".jar") || serviceFile.getName().endsWith(".war")) {
				// legacy XAP Processing Unit
				packedFile = serviceFile;
			} else if (serviceFile.isDirectory()) {
				// Assume that a folder will contain a DSL file?
				packedFile = Packager.pack(serviceFile);
				packedFile.deleteOnExit();
				service = ServiceReader.readService(serviceFile);
			} else {
				// serviceFile is a zip file
				packedFile = serviceFile;
				service = ServiceReader.readServiceFromZip(packedFile, CloudifyConstants.DEFAULT_APPLICATION_NAME);
			}
		} catch (final IOException e) {
			throw new CLIException(e);
		} catch (final PackagingException e) {
			throw new CLIException(e);
		}
		final String currentApplicationName = getCurrentApplicationName();

		// TODO: All packaging logic should be moved to the REST server
		Properties props = null;
		if (service != null) {
			props = createServiceContextProperties(service);
			plannedNumberOfInstances = service.getNumInstances();
			if (serviceName == null || serviceName.length() == 0) {
				serviceName = service.getName();
			}
		}

		if (serviceName == null || serviceName.length() == 0) {
			serviceName = serviceFile.getName();
			final int endIndex = serviceName.lastIndexOf('.');
			if (endIndex > 0) {
				serviceName = serviceName.substring(0, endIndex);
			}
		}
		if (zone == null || zone.length() == 0) {
			zone = serviceName;
		}

		String templateName = null;
		// service is null when a simple deploying war for example
		if (service != null && service.getCompute() != null) {
			templateName = service.getCompute().getTemplate();
			if (templateName == null) {
				templateName = "";
			}
		} else {
			templateName = "";
		}
		adminFacade.installElastic(packedFile, currentApplicationName, serviceName, zone, props, templateName);

		// if a zip file was created, delete it at the end of use.
		if (serviceFile.isDirectory()) {
			FileUtils.deleteQuietly(packedFile.getParentFile());
		}

		// TODO: Refactor waitXXX outside of adminFacade
		adminFacade.waitForServiceInstances(serviceName, currentApplicationName, plannedNumberOfInstances,
				TIMEOUT_ERROR_MESSAGE, timeoutInMinutes, TimeUnit.MINUTES);

		return getFormattedMessage("service_install_ended", Color.GREEN, serviceName);
	}

	// TODO: THIS CODE IS COPIED AS IS FROM THE REST PROJECT
	// It is used originally in ApplicationInstallerRunnable
	// This copy is a bad idea, and should be moved out of here as soon as possible.
	/**
	 * Create Properties object with settings from the service object, if found on the given service. The
	 * supported settings are: com.gs.application.dependsOn com.gs.service.type com.gs.service.icon
	 * com.gs.service.network.protocolDescription
	 * 
	 * @param service
	 *            The service object the read the settings from
	 * @return Properties object populated with the above properties, if found on the given service.
	 */
	private Properties createServiceContextProperties(final Service service) {
		final Properties contextProperties = new Properties();

		// contextProperties.setProperty("com.gs.application.services",
		// serviceNamesString);
		if (service.getDependsOn() != null) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, service.getDependsOn()
					.toString());
		}
		if (service.getType() != null) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_SERVICE_TYPE, service.getType());
		}
		if (service.getIcon() != null) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_SERVICE_ICON,
					CloudifyConstants.SERVICE_EXTERNAL_FOLDER + service.getIcon());
		}
		if (service.getNetwork() != null) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_NETWORK_PROTOCOL_DESCRIPTION, service
					.getNetwork().getProtocolDescription());
		}
		return contextProperties;
	}
}
