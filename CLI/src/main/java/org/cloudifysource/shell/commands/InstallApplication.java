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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.domain.Application;
import org.cloudifysource.domain.Service;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLErrorMessageException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.debug.DebugModes;
import org.cloudifysource.dsl.internal.debug.DebugUtils;
import org.cloudifysource.dsl.internal.packaging.Packager;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.dsl.rest.request.InstallApplicationRequest;
import org.cloudifysource.dsl.rest.response.InstallApplicationResponse;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.rest.RestLifecycleEventsLatch;
import org.cloudifysource.shell.rest.inspect.CLIApplicationInstaller;
import org.cloudifysource.shell.util.ApplicationResolver;
import org.cloudifysource.shell.util.NameAndPackedFileResolver;
import org.cloudifysource.shell.util.PreparedApplicationPackageResolver;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, barakm, adaml
 * @since 2.0.0
 *
 *        Installs an application, including it's contained services ordered according to their dependencies.
 *
 *        Required arguments: application-file - The application recipe file path, folder or archive (zip/jar)
 *
 *        Optional arguments: name - The name of the application timeout - The number of minutes to wait until the
 *        operation is completed (default: 10 minutes)
 *
 *        Command syntax: install-application [-name name] [-timeout timeout] application-file
 */
@Command(scope = "cloudify", name = "install-application", description = "Installs an application. If you specify"
		+ " a folder path it will be packed and deployed. If you sepcify an application archive, the shell will deploy"
		+ " that file.")
public class InstallApplication extends AdminAwareCommand implements NewRestClientCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 10;
	private static final String TIMEOUT_ERROR_MESSAGE = "Application installation timed out."
			+ " Configure the timeout using the -timeout flag.";
	private static final long TEN_K = 10 * FileUtils.ONE_KB;

	@Argument(required = true, name = "application-file", description = "The application recipe file path, folder "
			+ "or archive")
	private File applicationFile;

	@Option(required = false, name = "-authGroups", description = "The groups authorized to access this application "
			+ "(multiple values can be comma-separated)")
	private String authGroups;

	@Option(required = false, name = "-name", description = "The name of the application")
	private String applicationName;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation"
			+ " is done.")
	private int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	@Option(required = false, name = "-disableSelfHealing",
			description = "Disables service self healing")
	private boolean disableSelfHealing = false;

	@Option(required = false, name = "-cloudConfiguration",
			description = "File or directory containing configuration information to be used by the cloud driver "
					+ "for this application")
	private File cloudConfiguration;

	@Option(required = false, name = "-overrides",
			description = "File containing properties to be used to override the current "
					+ "properties of the application and its services")
	private File overrides;

	@Option(required = false, name = "-cloud-overrides",
			description = "File containing properties to be used to override the current cloud "
					+ "configuration for this application and its services.")
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
	@SuppressWarnings("boxing")
	@Override
	protected Object doExecute()
			throws Exception {

		try {
			DebugUtils.validateDebugSettings(debugAll, debugEvents, getDebugModeString());
		} catch (final DSLErrorMessageException e) {
			throw new CLIStatusException(e.getErrorMessage().getName(), (Object[]) e.getArgs());
		}
		if (cloudOverrides != null) {
			if (cloudOverrides.length() >= TEN_K) {
				throw new CLIStatusException(CloudifyErrorMessages.CLOUD_OVERRIDES_TO_LONG.getName());
			}
		}

		final RecipePathResolver pathResolver = new RecipePathResolver();
		if (pathResolver.resolveApplication(applicationFile)) {
			applicationFile = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("application_not_found",
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}

		logger.info("Validating file " + applicationFile.getName());
		final DSLReader dslReader = createDslReader();
		final Application application = dslReader.readDslEntity(Application.class);

		if (StringUtils.isBlank(applicationName)) {
			applicationName = application.getName();
		}

		if (!org.cloudifysource.restclient.StringUtils.isValidRecipeName(applicationName)) {
			throw new CLIStatusException(CloudifyErrorMessages.APPLICATION_NAME_INVALID_CHARS.getName(),
					applicationName);
		}

		if (adminFacade.getApplicationNamesList().contains(applicationName)) {
			throw new CLIStatusException("application_already_deployed", application.getName());
		}

		final File cloudConfigurationZipFile = createCloudConfigurationZipFile();
		File zipFile;
		if (applicationFile.isFile()) {
			if (applicationFile.getName().endsWith(".zip") || applicationFile.getName().endsWith(".jar")) {
				zipFile = applicationFile;
			} else {
				throw new CLIStatusException("application_file_format_mismatch", applicationFile.getPath());
			}
		} else { // pack an application folder
			final List<File> additionalServiceFiles = new LinkedList<File>();
			if (cloudConfigurationZipFile != null) {
				additionalServiceFiles.add(cloudConfigurationZipFile);
			}
			zipFile = Packager.packApplication(application, applicationFile, additionalServiceFiles);
		}

		// toString of string list (i.e. [service1, service2])
		logger.info("Uploading application " + applicationName);

		final Map<String, String> result =
				adminFacade.installApplication(zipFile, applicationName,
						authGroups, getTimeoutInMinutes(), !isDisableSelfHealing(),
						overrides, cloudOverrides, debugAll, debugEvents, getDebugModeString());

		final String serviceOrder = result.get(CloudifyConstants.SERVICE_ORDER);

		// If temp file was created, Delete it.
		if (!applicationFile.isFile()) {
			final boolean delete = zipFile.delete();
			if (!delete) {
				logger.info("Failed to delete application file: " + zipFile.getAbsolutePath());
			}
		}

		if (serviceOrder.charAt(0) != '[' && serviceOrder.charAt(serviceOrder.length() - 1) != ']') {
			throw new IllegalStateException("Cannot parse service order response: " + serviceOrder);
		}
		printApplicationInfo(application);

		session.put(Constants.ACTIVE_APP, applicationName);
		GigaShellMain.getInstance().setCurrentApplicationName(applicationName);

		final String pollingID = result.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);
		final RestLifecycleEventsLatch lifecycleEventsPollingLatch =
				this.adminFacade.getLifecycleEventsPollingLatch(pollingID, TIMEOUT_ERROR_MESSAGE);
		boolean isDone = false;
		boolean continuous = false;
		while (!isDone) {
			try {
				if (!continuous) {
					lifecycleEventsPollingLatch.waitForLifecycleEvents(getTimeoutInMinutes(), TimeUnit.MINUTES);
				} else {
					lifecycleEventsPollingLatch.continueWaitForLifecycleEvents(getTimeoutInMinutes(), TimeUnit.MINUTES);
				}
				isDone = true;
			} catch (final TimeoutException e) {
				if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
					throw e;
				}
				final boolean continueInstallation = promptWouldYouLikeToContinueQuestion();
				if (!continueInstallation) {
					throw new CLIStatusException(e, "application_installation_timed_out_on_client",
							applicationName);
				}
				continuous = true;
			}
		}

		return this.getFormattedMessage("application_installed_successfully", Color.GREEN, applicationName);
	}

	private DSLReader createDslReader() {
		final DSLReader dslReader = new DSLReader();
		final File dslFile = DSLReader.findDefaultDSLFile(DSLUtils.APPLICATION_DSL_FILE_NAME_SUFFIX, applicationFile);
		dslReader.setDslFile(dslFile);
		dslReader.setCreateServiceContext(false);
		dslReader.addProperty(DSLUtils.APPLICATION_DIR, dslFile.getParentFile().getAbsolutePath());
		dslReader.setOverridesFile(overrides);
		return dslReader;
	}

	private File createCloudConfigurationZipFile()
			throws CLIStatusException, IOException {
		if (this.cloudConfiguration == null) {
			return null;
		}

		if (!this.cloudConfiguration.exists()) {
			throw new CLIStatusException("cloud_configuration_file_not_found",
					this.cloudConfiguration.getAbsolutePath());
		}

		// create a temp file in a temp directory
		final File tempDir = File.createTempFile("__Cloudify_Cloud_configuration", ".tmp");
		FileUtils.forceDelete(tempDir);
		final boolean mkdirs = tempDir.mkdirs();
		if (!mkdirs) {
			logger.info("Field to create temporary directory " + tempDir.getAbsolutePath());
		}
		final File tempFile = new File(tempDir, CloudifyConstants.SERVICE_CLOUD_CONFIGURATION_FILE_NAME);
		logger.info("Created temporary file " + tempFile.getAbsolutePath()
				+ " in temporary directory" + tempDir.getAbsolutePath());

		// mark files for deletion on JVM exit
		tempFile.deleteOnExit();
		tempDir.deleteOnExit();

		if (this.cloudConfiguration.isDirectory()) {
			ZipUtils.zip(this.cloudConfiguration, tempFile);
		} else if (this.cloudConfiguration.isFile()) {
			ZipUtils.zipSingleFile(this.cloudConfiguration, tempFile);
		} else {
			throw new IOException(this.cloudConfiguration + " is neither a file nor a directory");
		}

		return tempFile;
	}

	private boolean promptWouldYouLikeToContinueQuestion()
			throws IOException {
		return ShellUtils.promptUser(session, "would_you_like_to_continue_application_installation",
				this.applicationName);
	}

	/**
	 * Prints Application data - the application name and it's services name, dependencies and number of instances.
	 *
	 * @param application
	 *            Application object to analyze
	 */
	private void printApplicationInfo(final Application application) {
		final List<Service> services = application.getServices();
		logger.info("Application [" + applicationName + "] with " + services.size() + " services");
		for (final Service service : services) {
			if (service.getDependsOn().isEmpty()) {
				logger.info("Service [" + service.getName() + "] " + service.getNumInstances() + " planned instances");
			} else { // Service has dependencies
				logger.info("Service [" + service.getName() + "] depends on " + service.getDependsOn().toString()
						+ " " + service.getNumInstances() + " planned instances");
			}
		}
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

	public String getDebugModeString() {
		return debugModeString;
	}

	public void setDebugModeString(final String debugModeString) {
		this.debugModeString = debugModeString;
	}

	public boolean isDisableSelfHealing() {
		return disableSelfHealing;
	}

	public void setDisableSelfHealing(final boolean disableSelfHealing) {
		this.disableSelfHealing = disableSelfHealing;
	}

	@Override
	public Object doExecuteNewRestClient() 
			throws Exception {
		//resolve the path for the given app input
		final RecipePathResolver pathResolver = new RecipePathResolver();
		if (pathResolver.resolveApplication(applicationFile)) {
			applicationFile = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("application_not_found",
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}
		//resolve packed file and application name
		final NameAndPackedFileResolver nameAndPackedFileResolver = getResolver();
		if (StringUtils.isBlank(applicationName)) {
			applicationName = nameAndPackedFileResolver.getName();
		}
		
		final File packedFile = nameAndPackedFileResolver.getPackedFile();
		//upload relevant application deployment files 
		RestClient newRestClient = ((RestAdminFacade) getRestAdminFacade()).getNewRestClient();
		final String packedFileKey = ShellUtils.uploadToRepo(newRestClient, packedFile, displayer);
		final String overridesFileKey = ShellUtils.uploadToRepo(newRestClient, overrides, displayer);
		final String cloudOverridesFileKey = ShellUtils.uploadToRepo(newRestClient, cloudOverrides, displayer);
		final String cloudConfigurationFileKey = ShellUtils.uploadToRepo(newRestClient,
		        createCloudConfigurationZipFile(), displayer);
		
		//create the install request
		InstallApplicationRequest request = new InstallApplicationRequest();
		request.setApplcationFileUploadKey(packedFileKey);
		request.setApplicationOverridesUploadKey(overridesFileKey);
		request.setCloudOverridesUploadKey(cloudOverridesFileKey);
		request.setCloudConfigurationUploadKey(cloudConfigurationFileKey);
		request.setApplicationName(applicationName);
		request.setAuthGroups(authGroups);
		request.setDebugAll(debugAll);
		request.setDebugEvents(debugEvents);
		request.setDebugMode(debugModeString);
		request.setSelfHealing(!disableSelfHealing);

        //install application
        final InstallApplicationResponse installApplicationResponse =
        		newRestClient.installApplication(applicationName, request);

        Application application = ((Application) nameAndPackedFileResolver.getDSLObject());
        //print application info.
        printApplicationInfo(application);

        Map<String, Integer> plannedNumberOfInstancesPerService = nameAndPackedFileResolver
                .getPlannedNumberOfInstancesPerService();

        CLIApplicationInstaller installer = new CLIApplicationInstaller();
        installer.setApplicationName(applicationName);
        installer.setAskOnTimeout(true);
        installer.setDeploymentId(installApplicationResponse.getDeploymentID());
        installer.setPlannedNumberOfInstancesPerService(plannedNumberOfInstancesPerService);
        installer.setInitialTimeout(timeoutInMinutes);
        installer.setRestClient(newRestClient);
        installer.setSession(session);
        try {
        installer.install();
        } finally {
            // drop one line
            displayer.printEvent("");
            if (!applicationFile.isFile()) {
                final boolean delete = FileUtils.deleteQuietly(packedFile);
                if (!delete) {
                    logger.info("Failed to delete application file: " + packedFile.getAbsolutePath());
                }
            }
        }


		//set the active application in the CLI.
		session.put(Constants.ACTIVE_APP, applicationName);
		GigaShellMain.getInstance().setCurrentApplicationName(applicationName);
		// drop one line before printing the last message
        displayer.printEvent("");
		return this.getFormattedMessage("application_installed_successfully", Color.GREEN, applicationName);
	}
    
	private NameAndPackedFileResolver getResolver() 
			throws CLIStatusException {
		// this is a prepared package we can just use.
		if (applicationFile.isFile()) {
			if (applicationFile.getName().endsWith("zip") || applicationFile.getName().endsWith("jar")) {
				return new PreparedApplicationPackageResolver(applicationFile, overrides);
			} 
			throw new CLIStatusException("application_file_format_mismatch", applicationFile.getPath()); 
		}
		// this is an actual application directory
		return new ApplicationResolver(applicationFile, overrides);
	}

}
