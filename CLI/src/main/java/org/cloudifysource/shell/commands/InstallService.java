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
import java.io.IOException;
import java.util.Properties;

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

	@Argument(required = true, name = "recipe", description = "The service recipe folder or archive")
	private File recipe;

	@Option(required = false, name = "-zone", description = "The machines zone in which to install the service")
	private String zone;

	@Option(required = false, name = "-name", description = "The name of the service")
	private String serviceName = null;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is "
			+ "done. Defaults to 5 minutes.")
	private int timeoutInMinutes = 5;
	
	@Option(required = false, name = "-service-file-name", description = "Name of the service file in the "
		+ "recipe folder. If not specified, uses the default file name")
	private String serviceFileName = null;
	
	private static final String TIMEOUT_ERROR_MESSAGE = "Service installation timed out." 
				+ " Configure the timeout using the -timeout flag.";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		if (!recipe.exists()) {
			throw new CLIStatusException("service_file_doesnt_exist", recipe.getPath());
		}

		File packedFile;

		// TODO: this logics should not be done twice. should be done directly in the rest server.
		// also figure out how to treat war/jar files that have no .groovy file. create default?
		int plannedNumberOfInstances = 1;
		Service service = null;
		try {
			if (recipe.getName().endsWith(".jar") || recipe.getName().endsWith(".war")) {
				// legacy XAP Processing Unit
				packedFile = recipe;
			} else if (recipe.isDirectory()) {
				// Assume that a folder will contain a DSL file?
				
				if (serviceFileName != null) {
					File fullPathToRecipe = new File(recipe.getAbsolutePath() + "/" + serviceFileName);
					if (!fullPathToRecipe.exists()) {
						throw new CLIStatusException("service_file_doesnt_exist", fullPathToRecipe.getPath());
					}
					packedFile = Packager.pack(fullPathToRecipe);
					service = ServiceReader.readService(fullPathToRecipe);
				}
				else {
					packedFile = Packager.pack(recipe);
					service = ServiceReader.readService(recipe);
				}
				packedFile.deleteOnExit();
			} else {
				// serviceFile is a zip file
				packedFile = recipe;
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
			if (serviceFileName != null) {
				props.setProperty(CloudifyConstants.CONTEXT_PROPERTY_SERVICE_FILE_NAME, serviceFileName);
			}
			plannedNumberOfInstances = service.getNumInstances();
			if (serviceName == null || serviceName.isEmpty()) {
				serviceName = service.getName();
			}
		}

		if (serviceName == null || serviceName.isEmpty()) {
			serviceName = recipe.getName();
			final int endIndex = serviceName.lastIndexOf('.');
			if (endIndex > 0) {
				serviceName = serviceName.substring(0, endIndex);
			}
		}
		if (zone == null || zone.isEmpty()) {
			zone = serviceName;
		}

		String templateName;
		// service is null when a simple deploying war for example
		if (service == null || service.getCompute() == null) {
			templateName = "";
		}else{
			templateName = service.getCompute().getTemplate();
			if (templateName == null) {
				templateName = "";
			}
		}
		
		String lifecycleEventContainerPollingID = adminFacade.installElastic(packedFile, currentApplicationName, serviceName, zone, props, templateName, timeoutInMinutes);

		if (lifecycleEventContainerPollingID != null){
			this.adminFacade.waitForLifecycleEvents(lifecycleEventContainerPollingID, timeoutInMinutes, TIMEOUT_ERROR_MESSAGE);
		} else {
			throw new CLIException("Failed to retrieve lifecycle logs from rest. " +
			"Check logs for more details.");
		}

		// if a zip file was created, delete it at the end of use.
		if (recipe.isDirectory()) {
			FileUtils.deleteQuietly(packedFile.getParentFile());
		}

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
			if (service.getNetwork().getProtocolDescription() != null){
				contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_NETWORK_PROTOCOL_DESCRIPTION, service
						.getNetwork().getProtocolDescription());
			}
		}
		
		contextProperties.setProperty(
				CloudifyConstants.CONTEXT_PROPERTY_ELASTIC,
				Boolean.toString(service.isElastic()));
		
		return contextProperties;
	}
}
