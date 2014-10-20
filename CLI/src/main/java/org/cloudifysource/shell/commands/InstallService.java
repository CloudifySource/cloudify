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
package org.cloudifysource.shell.commands;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLErrorMessageException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.debug.DebugModes;
import org.cloudifysource.dsl.internal.debug.DebugUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.dsl.rest.request.InstallServiceRequest;
import org.cloudifysource.dsl.rest.response.InstallServiceResponse;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.rest.RestLifecycleEventsLatch;
import org.cloudifysource.shell.rest.inspect.CLIServiceInstaller;
import org.cloudifysource.shell.util.NameAndPackedFileResolver;
import org.cloudifysource.shell.util.PreparedPackageResolver;
import org.cloudifysource.shell.util.ServiceResolver;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Installs a service by deploying the service files as one packed file (zip, war or jar). Service files can also
 *        be supplied as a folder containing multiple files.
 * 
 *        Required arguments: service-file - Path to the service's packed file or folder
 * 
 *        Optional arguments: zone - The machines zone in which to install the service name - The name of the service
 *        timeout - The number of minutes to wait until the operation is completed (default: 5 minutes)
 * 
 *        Command syntax: install-service [-zone zone] [-name name] [-timeout timeout] service-file
 */
@Command(scope = "cloudify", name = "install-service", description = "Installs a service. If you specify a folder"
		+ " path it will be packed and deployed. If you specify a service archive, the shell will deploy that file.")
public class InstallService extends AdminAwareCommand implements NewRestClientCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 5;
	private static final String TIMEOUT_ERROR_MESSAGE = "Service installation timed out."
			+ " Configure the timeout using the -timeout flag.";
	private static final long TEN_K = 10 * FileUtils.ONE_KB;

	@Argument(required = true, name = "recipe", description = "The service recipe folder or archive")
	private File recipe;

	@Option(required = false, name = "-authGroups", description = "The groups authorized to access this application "
			+ "(multiple values can be comma-separated)")
	private String authGroups;

	@Option(required = false, name = "-zone", description = "The machines zone in which to install the service")
	private String zone;

	@Option(required = false, name = "-name", description = "The name of the service")
	private String serviceName = null;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is "
			+ "done. Defaults to 5 minutes.")
	private int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	@Deprecated
	@Option(required = false, name = "-service-file-name", description = "Name of the service file in the "
			+ "recipe folder. If not specified, uses the default file name")
	private String serviceFileName = null;

	@Option(required = false, name = "-cloudConfiguration", description =
			"File of directory containing configuration information to be used by the cloud driver "
					+ "for this application")
	private File cloudConfiguration;

	@Option(required = false, name = "-disableSelfHealing",
			description = "Disables service self healing")
	private boolean disableSelfHealing = false;

	@Option(required = false, name = "-overrides", description =
			"File containing properties to be used to overrides the current service's properties.")
	private File overrides;

	@Option(required = false, name = "-cloud-overrides",
			description = "File containing properties to be used to override the current cloud "
					+ "configuration for this service.")
	private File cloudOverrides;

	@Option(required = false, name = "-debug-all",
			description = "Debug all supported lifecycle events")
	private boolean debugAll;

	@Option(required = false, name = "-debug-events",
			description = "Debug the specified events")
	private String debugEvents;

	@Option(required = false, name = "-debug-mode",
			description = "Debug mode. One of: instead, after or onError")
	private String debugModeString = DebugModes.INSTEAD.getName();

	private CLIEventsDisplayer displayer = new CLIEventsDisplayer();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws Exception {
		logger.fine("install-service using the old rest client");
		try {
			DebugUtils.validateDebugSettings(debugAll, debugEvents, debugModeString);
		} catch (final DSLErrorMessageException e) {
			throw new CLIStatusException(e, e.getErrorMessage().getName(), (Object[]) e.getArgs());
		}

		if (cloudOverrides != null) {
			if (cloudOverrides.length() >= TEN_K) {
				throw new CLIStatusException(CloudifyErrorMessages.CLOUD_OVERRIDES_TO_LONG.getName());
			}
		}

		final RecipePathResolver pathResolver = new RecipePathResolver();
		if (pathResolver.resolveService(recipe)) {
			recipe = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("service_file_doesnt_exist",
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}

		File packedFile;

		final File cloudConfigurationZipFile = createCloudConfigurationZipFile();

		// TODO: this logics should not be done twice. should be done directly
		// in the rest server.
		// also figure out how to treat war/jar files that have no .groovy file.
		// create default?
		Service service = null;
		try {
			if (recipe.getName().endsWith(".jar")
					|| recipe.getName().endsWith(".war")) {
				// legacy XAP Processing Unit
				packedFile = recipe;
			} else if (recipe.isDirectory()) {
				// Assume that a folder will contain a DSL file?

				final List<File> additionFiles = new LinkedList<File>();
				if (cloudConfigurationZipFile != null) {
					additionFiles.add(cloudConfigurationZipFile);
				}
				File recipeFile = recipe;
				if (getServiceFileName() != null) {
					final File fullPathToRecipe = new File(
							recipe.getAbsolutePath() + "/" + getServiceFileName());
					if (!fullPathToRecipe.exists()) {
						throw new CLIStatusException(
								"service_file_doesnt_exist",
								fullPathToRecipe.getPath());
					}
					// locate recipe file
					recipeFile = fullPathToRecipe.isDirectory()
							? DSLReader.findDefaultDSLFile(DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX, fullPathToRecipe)
							: fullPathToRecipe;
				} else {
					recipeFile = DSLReader.findDefaultDSLFile(DSLUtils.SERVICE_DSL_FILE_NAME_SUFFIX, recipe);
				}
				service = ServiceReader.readService(recipeFile, recipe, null, false, overrides);
				packedFile = Packager.pack(recipeFile, false, service, additionFiles);
				packedFile.deleteOnExit();
			} else {
				// serviceFile is a zip file
				packedFile = recipe;
				service = ServiceReader.readServiceFromZip(packedFile);
			}
		} catch (final IOException e) {
			throw new CLIException(e);
		} catch (final PackagingException e) {
			throw new CLIException(e);
		}
		final String currentApplicationName = getCurrentApplicationName();

		Properties props = null;
		if (service != null) {
			props = createServiceContextProperties(service);
			if (getServiceFileName() != null) {
				props.setProperty(CloudifyConstants.CONTEXT_PROPERTY_SERVICE_FILE_NAME, getServiceFileName());
			}
			if (serviceName == null || serviceName.isEmpty()) {
				serviceName = service.getName();
			}

			if (!org.cloudifysource.restclient.StringUtils.isValidRecipeName(serviceName)) {
				throw new CLIStatusException(CloudifyErrorMessages.SERVICE_NAME_INVALID_CHARS.getName(), serviceName);
			}
		} else {
			if (serviceName == null || serviceName.isEmpty()) {
				serviceName = recipe.getName();
				final int endIndex = serviceName.lastIndexOf('.');
				if (endIndex > 0) {
					serviceName = serviceName.substring(0, endIndex);
				}
			}
		}
		if (zone == null || zone.isEmpty()) {
			zone = serviceName;
		}

		String templateName;
		// service is null when a simple deploying war for example
		if (service == null || service.getCompute() == null) {
			templateName = "";
		} else {
			templateName = service.getCompute().getTemplate();
			if (templateName == null) {
				templateName = "";
			}
		}

		try {
			final String lifecycleEventContainerPollingID = adminFacade
					.installElastic(packedFile, currentApplicationName,
							serviceName, zone, props, templateName, authGroups,
							getTimeoutInMinutes(), !disableSelfHealing, cloudOverrides, overrides);

			pollForLifecycleEvents(lifecycleEventContainerPollingID);

		} finally {
			// if a zip file was created, delete it at the end of use.
			if (recipe.isDirectory()) {
				FileUtils.deleteQuietly(packedFile.getParentFile());
			}
		}

		// TODO - server may have failed! We should check the service state and
		// decide accordingly
		// which message to display.
		return getFormattedMessage("service_install_ended", Color.GREEN, serviceName);
	}

	private void pollForLifecycleEvents(final String lifecycleEventContainerPollingID) throws InterruptedException,
			CLIException, TimeoutException, IOException {
		final RestLifecycleEventsLatch lifecycleEventsPollingLatch = this.adminFacade
				.getLifecycleEventsPollingLatch(
						lifecycleEventContainerPollingID, TIMEOUT_ERROR_MESSAGE);
		boolean isDone = false;
		boolean continuous = false;
		while (!isDone) {
			try {
				if (!continuous) {
					lifecycleEventsPollingLatch.waitForLifecycleEvents(
							getTimeoutInMinutes(), TimeUnit.MINUTES);
				} else {
					lifecycleEventsPollingLatch.continueWaitForLifecycleEvents(
							getTimeoutInMinutes(), TimeUnit.MINUTES);
				}
				isDone = true;
			} catch (final TimeoutException e) {
				if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
					throw e;
				}
				final boolean continueInstallation = promptWouldYouLikeToContinueQuestion();
				if (!continueInstallation) {
					throw new CLIStatusException(e,
							"service_installation_timed_out_on_client",
							serviceName);
				} else {
					continuous = true;
				}
			}
		}
	}

	private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
		return ShellUtils.promptUser(session,
				"would_you_like_to_continue_service_installation", serviceName);
	}

	// TODO: THIS CODE IS COPIED AS IS FROM THE REST PROJECT
	// It is used originally in ApplicationInstallerRunnable
	// This copy is a bad idea, and should be moved out of here as soon as
	// possible.
	/**
	 * Create Properties object with settings from the service object, if found on the given service. The supported
	 * settings are: com.gs.application.dependsOn com.gs.service.type com.gs.service.icon
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
					CloudifyConstants.SERVICE_EXTERNAL_FOLDER
							+ service.getIcon());
		}
		if (service.getNetwork() != null) {
			if (service.getNetwork().getProtocolDescription() != null) {
				contextProperties
						.setProperty(
								CloudifyConstants.CONTEXT_PROPERTY_NETWORK_PROTOCOL_DESCRIPTION,
								service.getNetwork().getProtocolDescription());
			}
		}

		contextProperties.setProperty(
				CloudifyConstants.CONTEXT_PROPERTY_ELASTIC,
				Boolean.toString(service.isElastic()));

		if (this.debugAll) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_ALL, Boolean.TRUE.toString());
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_MODE, this.getDebugModeString());
		} else if (this.debugEvents != null) {
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_EVENTS, this.debugEvents);
			contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_DEBUG_MODE, this.getDebugModeString());
		}

		return contextProperties;
	}

	private File createCloudConfigurationZipFile() throws CLIStatusException,
			IOException {
		if (this.cloudConfiguration == null) {
			return null;
		}

		if (!this.cloudConfiguration.exists()) {
			throw new CLIStatusException("cloud_configuration_file_not_found",
					this.cloudConfiguration.getAbsolutePath());
		}

		// create a temp file in a temp directory
		final File tempDir = File.createTempFile(
				"__Cloudify_Cloud_configuration", ".tmp");
		FileUtils.forceDelete(tempDir);
		tempDir.mkdirs();

		final File tempFile = new File(tempDir,
				CloudifyConstants.SERVICE_CLOUD_CONFIGURATION_FILE_NAME);

		// mark files for deletion on JVM exit
		tempFile.deleteOnExit();
		tempDir.deleteOnExit();

		if (this.cloudConfiguration.isDirectory()) {
			ZipUtils.zip(this.cloudConfiguration, tempFile);
		} else if (this.cloudConfiguration.isFile()) {
			ZipUtils.zipSingleFile(this.cloudConfiguration, tempFile);
		} else {
			throw new IOException(this.cloudConfiguration
					+ " is neither a file nor a directory");
		}

		return tempFile;
	}

	public File getCloudConfiguration() {
		return cloudConfiguration;
	}

	public void setCloudConfiguration(final File cloudConfiguration) {
		this.cloudConfiguration = cloudConfiguration;
	}

	public int getTimeoutInMinutes() {
		return timeoutInMinutes;
	}

	public void setTimeoutInMinutes(final int timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
	}

	public boolean isDisableSelfHealing() {
		return disableSelfHealing;
	}

	public void setDisableSelfHealing(final boolean disableSelfHealing) {
		this.disableSelfHealing = disableSelfHealing;
	}

	public boolean isDebugAll() {
		return debugAll;
	}

	public void setDebugAll(final boolean debugAll) {
		this.debugAll = debugAll;
	}

	public String getDebugEvents() {
		return debugEvents;
	}

	public void setDebugEvents(final String debugEvents) {
		this.debugEvents = debugEvents;
	}

	public String getDebugModeString() {
		return debugModeString;
	}

	public void setDebugModeString(final String debugModeString) {
		this.debugModeString = debugModeString;
	}

	public String getServiceFileName() {
		return serviceFileName;
	}

	public void setServiceFileName(final String serviceFileName) {
		this.serviceFileName = serviceFileName;
	}

	@Override
	public Object doExecuteNewRestClient() throws Exception {
		logger.fine("Installing service " + recipe + " using the new rest client");
		RestClient newRestClient = ((RestAdminFacade) getRestAdminFacade()).getNewRestClient();
        NameAndPackedFileResolver nameAndPackedFileResolver = getResolver(recipe);
        serviceName = nameAndPackedFileResolver.getName();
        File packedFile = nameAndPackedFileResolver.getPackedFile();

        // upload the files if necessary
        final String cloudConfigurationFileKey = ShellUtils.uploadToRepo(newRestClient, cloudConfiguration, displayer);
        final String cloudOverridesFileKey = ShellUtils.uploadToRepo(newRestClient, cloudOverrides, displayer);
        final String overridesFileKey = ShellUtils.uploadToRepo(newRestClient, overrides, displayer);

        final String recipeFileKey = ShellUtils.uploadToRepo(newRestClient, packedFile, displayer);

        InstallServiceRequest request = new InstallServiceRequest();
        request.setAuthGroups(authGroups);
        request.setCloudConfigurationUploadKey(cloudConfigurationFileKey);
        request.setDebugAll(debugAll);
        request.setCloudOverridesUploadKey(cloudOverridesFileKey);
        request.setDebugEvents(debugEvents);
        request.setServiceOverridesUploadKey(overridesFileKey);
        request.setServiceFolderUploadKey(recipeFileKey);
        request.setSelfHealing(!disableSelfHealing);

        // execute the request
        InstallServiceResponse installServiceResponse = 
        		newRestClient.installService(getCurrentApplicationName(), serviceName, request);

        CLIServiceInstaller installer = new CLIServiceInstaller();
        installer.setApplicationName(getCurrentApplicationName());
        installer.setAskOnTimeout(true);
        installer.setDeploymentId(installServiceResponse.getDeploymentID());
        installer.setInitialTimeout(timeoutInMinutes);
        installer.setRestClient(newRestClient);
        installer.setServiceName(serviceName);
        installer.setSession(session);
        installer.setPlannedNumberOfInstances(
        		nameAndPackedFileResolver.getPlannedNumberOfInstancesPerService().get(serviceName));

        try {
            installer.install();
        } finally {
            // drop one line
            displayer.printEvent("");
        }

        return getFormattedMessage("service_install_ended", Color.GREEN, serviceName);
	}
	
    private NameAndPackedFileResolver getResolver(final File recipe) throws CLIStatusException {
        if (recipe.isFile()) {
            // this is a prepared package we can just use.
            return new PreparedPackageResolver(recipe);
        }
		// this is an actual service directory
		return new ServiceResolver(resolve(recipe), overrides, serviceName);
    }
    
    private File resolve(final File recipe) throws CLIStatusException {
        final RecipePathResolver pathResolver = new RecipePathResolver();
        if (pathResolver.resolveService(recipe)) {
            return pathResolver.getResolved();
        }
		throw new CLIStatusException("service_file_doesnt_exist",
		        StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
    }

}
