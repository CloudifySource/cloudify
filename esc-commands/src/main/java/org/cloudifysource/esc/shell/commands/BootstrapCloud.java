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
package org.cloudifysource.esc.shell.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.FileAppender;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.shell.installer.CloudGridAgentBootstrapper;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.KeystoreFileVerifier;
import org.cloudifysource.shell.commands.AbstractGSCommand;
import org.cloudifysource.shell.commands.CLIStatusException;

import com.j_spaces.kernel.Environment;

/************
 * CLI Command to bootstrap a cloud.
 *
 * @author barakme
 * @since 2.0.0
 *
 */
@Command(
		scope = "cloudify",
		name = "bootstrap-cloud",
		description = "Starts Cloudify Agent without any zone, and the Cloudify management processes on the provided "
				+ "cloud.")
public class BootstrapCloud extends AbstractGSCommand {

	private static final int PROGRESS_INTERVAL_SECONDS = 10;
	private static final int DEFAULT_TIMEOUT_MINUTES = 60;
	private static final String PATH_SEPARATOR = System.getProperty("file.separator");

	@Argument(required = true, name = "provider", description = "The cloud provider to use")
	private String cloudProvider;

	@Option(required = false, description = "Server security mode (on/off)", name = "-secured")
	private boolean secured;

	@Option(required = false, description = "Path to a custom spring security configuration file",
			name = "-security-file")
	private String securityFilePath;

	@Option(required = false, description = "The username when connecting to a secure admin server", name = "-user")
	private String username;

	@Option(required = false, description = "The password when connecting to a secure admin server", name = "-password")
	private String password;

	@Option(required = false, description = "The path to the keystore used for SSL connections", name = "-keystore")
	private String keystore;

	@Option(required = false, description = "The password to the keystore", name = "-keystore-password")
	private String keystorePassword;

	@Option(required = false, description = "Path to a file containing override properties", name = "-cloud-overrides")
	private File cloudOverrides;

	@Option(required = false, name = "-timeout",
			description = "The number of minutes to wait until the operation is done.")
	private int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	@Option(required = false, name = "-no-web-services",
			description = "if set, no attempt to deploy the rest admin and web-ui will be made")
	private boolean noWebServices;

	@Option(required = false, name = "-use-existing",
			description = "if set, will attempt to find existing management servers. "
					+ "Management should already have been shut-down with stop-management.")
	private boolean useExistingManagers = false;

	@Option(required = false, name = "-use-existing-from-file",
			description = "if set, will attempt to find existing management servers based on server "
					+ "details supplied in file. "
					+ "Management should already have been shut-down with stop-management.")
	private File existingManagersFile = null;

	private String securityProfile = CloudifyConstants.SPRING_PROFILE_NON_SECURE;
	// flags to indicate if bootstrap operation created a backup file that
	// should be reverted

	private static final String CLOUDIFY_HOME = Environment.getHomeDirectory();
	private static final String DEFAULT_SECURITY_FILE_PATH = CLOUDIFY_HOME + "/config/security/spring-security.xml";
	private static final String[] NON_VERBOSE_LOGGERS = { DefaultProvisioningDriver.class.getName(),
			AgentlessInstaller.class.getName() };

	private final Map<String, Level> loggerStates = new HashMap<String, Level>();

	private static final long TEN_K = 10 * FileUtils.ONE_KB;

	private File defaultSecurityTargetFile;
	private File defaultKeystoreTargetFile;

	@Override
	protected Object doExecute() throws Exception {

		if (this.existingManagersFile != null) {
			if (!this.existingManagersFile.exists() || !this.existingManagersFile.isFile()) {
				throw new CLIStatusException(CloudifyErrorMessages.FILE_NOT_EXISTS.getName(),
						this.existingManagersFile.getAbsolutePath());
			}
		}

		if (cloudOverrides != null) {
			if (cloudOverrides.length() >= TEN_K) {
				throw new CLIStatusException(CloudifyErrorMessages.CLOUD_OVERRIDES_TO_LONG.getName());
			}
		}

		final RecipePathResolver pathResolver = new RecipePathResolver();

		File providerDirectory = null;
		if (pathResolver.resolveCloud(new File(getCloudProvider()))) {
			providerDirectory = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("cloud_driver_file_doesnt_exist",
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}

		final File tempFolder = createTempFolder();
		FileUtils.copyDirectoryToDirectory(providerDirectory, tempFolder);
		providerDirectory = new File(tempFolder, providerDirectory.getName());

		defaultSecurityTargetFile = new File(providerDirectory + PATH_SEPARATOR
				+ CloudifyConstants.SECURITY_FILE_NAME);

		defaultKeystoreTargetFile = new File(providerDirectory + PATH_SEPARATOR
				+ CloudifyConstants.KEYSTORE_FILE_NAME);

		setSecurityMode();
		copySecurityFiles(providerDirectory.getAbsolutePath());

		// load the cloud file
		final File cloudFile = findCloudFile(providerDirectory);

		// load properties file
		final File cloudPropertiesFile = new File(providerDirectory, cloudFile.getName().split("\\.")[0]
				+ DSLUtils.PROPERTIES_FILE_SUFFIX);

		// check for overrides file
		Cloud cloud = null;
		if (cloudOverrides == null) {
			cloud = ServiceReader.readCloud(cloudFile);
		} else {

			// read cloud with overrides properties so they reflect during bootstrap.
			cloud = ServiceReader.
					readCloudFromDirectory(providerDirectory.getAbsolutePath(),
							FileUtils.readFileToString(cloudOverrides));

			// append the overrides file to the existing properties file
			final FileAppender appender = new FileAppender(cloudPropertiesFile);
			appender.append("Overrides File Properties", cloudOverrides);
			appender.flush();
		}

		// start the installer
		final CloudGridAgentBootstrapper installer = new CloudGridAgentBootstrapper();
		installer.setProviderDirectory(providerDirectory);
		if (this.adminFacade != null) {
			installer.setAdminFacade(this.adminFacade);
		} else {
			installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		}
		installer.setProgressInSeconds(PROGRESS_INTERVAL_SECONDS);
		installer.setVerbose(verbose);
		installer.setCloud(cloud);
		installer.setCloudFile(cloudFile);
		installer.setNoWebServices(noWebServices);
		installer.setUseExisting(this.useExistingManagers);
		installer.setExistingManagersFile(this.existingManagersFile);
		// Bootstrap!

		// Note: The cloud driver may be very verbose. This is EXTEREMELY useful
		// when debugging ESM
		// issues, but can also clutter up the CLI display. It makes more sense to temporarily raise the log level here,
		// so that all of these
		// messages will not be displayed on the console.
		limitLoggingLevel();
		logger.info(getFormattedMessage("bootstrapping_cloud", getCloudProvider()));
		try {
			// TODO: Create the event listeners here and pass them to the installer.
			installer.bootstrapCloudAndWait(securityProfile, username, password,
					keystorePassword, getTimeoutInMinutes(), TimeUnit.MINUTES);
			return getFormattedMessage("cloud_started_successfully", getCloudProvider());
		} finally {
			// if an overrides file was passed, then the properties file is dirty. delete it.
			if (cloudOverrides != null) {
				cloudPropertiesFile.delete();
			}
			FileUtils.deleteDirectory(tempFolder);
			installer.close();
			restoreLoggingLevel();
		}

	}

	private File createTempFolder() throws IOException {
		final File tempFile = File.createTempFile("cloud-", "");
		tempFile.delete();
		tempFile.mkdir();
		return tempFile;
	}

	private void limitLoggingLevel() {

		if (!this.verbose) {
			loggerStates.clear();
			for (final String loggerName : NON_VERBOSE_LOGGERS) {
				final Logger provisioningLogger = Logger.getLogger(loggerName);
				final Level logLevelBefore = provisioningLogger.getLevel();
				provisioningLogger.setLevel(Level.WARNING);
				loggerStates.put(loggerName, logLevelBefore);
			}
		}
	}

	private void restoreLoggingLevel() {
		if (!verbose) {
			final Set<Entry<String, Level>> entries = loggerStates.entrySet();
			for (final Entry<String, Level> entry : entries) {
				final Logger provisioningLogger = Logger.getLogger(entry.getKey());
				provisioningLogger.setLevel(entry.getValue());
			}
		}

	}

	private File findCloudFile(final File providerDirectory) throws FileNotFoundException {
		if (!providerDirectory.exists() || !providerDirectory.isDirectory()) {
			throw new FileNotFoundException("Could not find cloud provider directory: " + providerDirectory);
		}

		final File[] cloudFiles = providerDirectory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith("-cloud.groovy");
			}

		});

		if (cloudFiles.length == 0) {
			throw new FileNotFoundException("Could not find a cloud definition file in: " + providerDirectory
					+ ". Definitions file must end with the suffix '-cloud.groovy'");
		} else if (cloudFiles.length > 1) {
			throw new IllegalArgumentException("Found multiple cloud definition files in: " + providerDirectory
					+ ". Only one file may end with the suffix '-cloud.groovy'");
		}

		return cloudFiles[0];
	}

	private void setSecurityMode() throws CLIStatusException {

		if (secured) {
			// enable security
			if (StringUtils.isNotBlank(keystore) && StringUtils.isNotBlank(keystorePassword)) {
				logger.info(getFormattedMessage(CloudifyErrorMessages.SETTING_SERVER_SECURITY_PROFILE.getName(),
						CloudifyConstants.SPRING_PROFILE_SECURE));
				securityProfile = CloudifyConstants.SPRING_PROFILE_SECURE;
			} else {
				logger.info(getFormattedMessage(CloudifyErrorMessages.SETTING_SERVER_SECURITY_PROFILE.getName(),
						CloudifyConstants.SPRING_PROFILE_SECURE_NO_SSL));
				securityProfile = CloudifyConstants.SPRING_PROFILE_SECURE_NO_SSL;
			}
		} else {
			// disable security
			logger.info(getFormattedMessage(CloudifyErrorMessages.SETTING_SERVER_SECURITY_PROFILE.getName(),
					CloudifyConstants.SPRING_PROFILE_NON_SECURE));
			securityProfile = CloudifyConstants.SPRING_PROFILE_NON_SECURE;
		}

		if (securityProfile.equalsIgnoreCase(CloudifyConstants.SPRING_PROFILE_NON_SECURE)) {
			if (StringUtils.isNotBlank(username)) {
				throw new IllegalArgumentException("'-user' is only valid when '-secured' is set");
			}

			if (StringUtils.isNotBlank(password)) {
				throw new IllegalArgumentException("'-password' is only valid when '-secured' is set");
			}

			if (StringUtils.isNotBlank(securityFilePath)) {
				throw new IllegalArgumentException("'-securityfile' is only valid when '-secured' is set");
			}

			if (StringUtils.isNotBlank(keystore)) {
				throw new IllegalArgumentException("'-keystore' is only valid when '-secured' is set");
			}

			if (StringUtils.isNotBlank(keystorePassword)) {
				throw new IllegalArgumentException("'-keystore-password' is only valid when '-secured' is set");
			}
		}

		if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
			throw new IllegalArgumentException("Password is missing or empty");
		}

		if (StringUtils.isBlank(username) && StringUtils.isNotBlank(password)) {
			throw new IllegalArgumentException("Username is missing or empty");
		}

		if (StringUtils.isNotBlank(keystore) && StringUtils.isBlank(keystorePassword)) {
			throw new IllegalArgumentException("Keystore password is missing or empty");
		}

		if (StringUtils.isBlank(keystore) && StringUtils.isNotBlank(keystorePassword)) {
			throw new IllegalArgumentException("Keystore is missing or empty");
		}

		if (StringUtils.isNotBlank(keystore)) {
			new KeystoreFileVerifier().verifyKeystoreFile(new File(keystore), keystorePassword);
		}

		if (StringUtils.isNotBlank(keystore)) {
			new KeystoreFileVerifier().verifyKeystoreFile(new File(keystore), keystorePassword);
		}
	}

	private void copySecurityFiles(final String providerDirectory) throws Exception {

		final File defaultSecuritySourceFile = new File(DEFAULT_SECURITY_FILE_PATH);

		if (securityProfile.equalsIgnoreCase(CloudifyConstants.SPRING_PROFILE_NON_SECURE)) {
			// copy the default security file (defines no security) to the upload folder
			FileUtils.copyFile(defaultSecuritySourceFile, defaultSecurityTargetFile);
		} else {
			// handle the configuration file
			if (StringUtils.isNotBlank(securityFilePath)) {
				final File securitySourceFile = new File(securityFilePath);
				if (!securitySourceFile.isFile()) {
					throw new Exception("Security configuration file not found: " + securityFilePath);
				}

				// copy to the cloud provider's folder, to be copied to all management servers remote directory
				if (!securitySourceFile.getCanonicalFile().equals(defaultSecurityTargetFile.getCanonicalFile())) {
					FileUtils.copyFile(securitySourceFile, defaultSecurityTargetFile);
				}
			} else {
				// TODO : should we use the default security location and assume it was edited by the user?
				// securityFilePath = CLOUDIFY_HOME + "/config/security/spring-security.xml";
				throw new IllegalArgumentException("-security-file is missing or empty");
			}

			// handle the keystore file
			if (StringUtils.isNotBlank(keystore)) {
				final File keystoreSourceFile = new File(keystore);
				if (!keystoreSourceFile.isFile()) {
					throw new Exception("Keystore file not found: " + keystore);
				}

				// copy to the override folder, to be copied to all management servers as well
				final File defaultKeystoreTargetFile = new File(providerDirectory + PATH_SEPARATOR
						+ CloudifyConstants.KEYSTORE_FILE_NAME);
				if (!keystoreSourceFile.getCanonicalFile().equals(defaultKeystoreTargetFile.getCanonicalFile())) {
					FileUtils.copyFile(keystoreSourceFile, defaultKeystoreTargetFile);
				}
			}
		}
	}

	public int getTimeoutInMinutes() {
		return timeoutInMinutes;
	}

	public void setTimeoutInMinutes(final int timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
	}

	public String getCloudProvider() {
		return cloudProvider;
	}

	public void setCloudProvider(final String cloudProvider) {
		this.cloudProvider = cloudProvider;
	}

	public boolean isUseExistingManagers() {
		return useExistingManagers;
	}

	public void setUseExistingManagers(boolean useExistingManagers) {
		this.useExistingManagers = useExistingManagers;
	}

	public File getExistingManagersFile() {
		return existingManagersFile;
	}

	public void setExistingManagersFile(File existingManagersFile) {
		this.existingManagersFile = existingManagersFile;
	}
}
