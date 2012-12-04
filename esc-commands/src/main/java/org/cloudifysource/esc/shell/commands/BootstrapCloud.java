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
package org.cloudifysource.esc.shell.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
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
import org.cloudifysource.shell.commands.AbstractGSCommand;
import org.cloudifysource.shell.commands.CLIStatusException;

import com.j_spaces.kernel.Environment;


@Command(
		scope = "cloudify",
		name = "bootstrap-cloud",
		description = "Starts Cloudify Agent without any zone, and the Cloudify management processes on the provided " 
				+ "cloud.")
public class BootstrapCloud extends AbstractGSCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 60;
	private static final String PATH_SEPARATOR = System.getProperty("file.separator");
	private static final String CLOUDIFY_HOME = Environment.getHomeDirectory();  //JSHOMEDIR is not set yet
	private static final String OVERRIDES_FOLDER = "upload" + PATH_SEPARATOR + "cloudify-overrides" + PATH_SEPARATOR
			+ "config" + PATH_SEPARATOR + "security";

	@Argument(required = true, name = "provider", description = "the cloud provider to use")
	String cloudProvider;
	
    @Option(required = false, description = "Server security mode (on/off)", name = "-secured")
    private boolean secured;
    
    @Option(required = false, description = "Path to a custom spring security configuration file",
    		name = "-securityFile", aliases = {"-securityfile" })
    private String securityFilePath;
    
    @Option(required = false, description = "The username when connecting to a secure admin server", name = "-user",
    		aliases = {"-username" })
    private String username;
	
    @Option(required = false, description = "The password when connecting to a secure admin server", name = "-pwd",
            aliases = {"-password" })
    private String password;
    
	@Option(required = false, description = "The path to the keystore used for SSL connections", name = "-keystore")
    private String keystore;
	
	@Option(required = false, description = "The password to the keystore", name = "-keystorePassword")
    private String keystorePassword;

    @Option(required = false, description = "Path to a file containing override properties", name = "-cloud-overrides")
    private File cloudOverrides;    
    
	@Option(required = false, name = "-timeout",
			description = "The number of minutes to wait until the operation is done.")
	int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;
	
	@Option(required = false, name = "-no-web-services",
			description = "if set, no attempt to deploy the rest admin and" + " web-ui will be made")
	private boolean noWebServices;
	
	private String securityProfile = CloudifyConstants.SPRING_PROFILE_NON_SECURE;
	private static final String[] NON_VERBOSE_LOGGERS = { DefaultProvisioningDriver.class.getName(), 
		AgentlessInstaller.class.getName() };
	private Map<String, Level> loggerStates = new HashMap<String, Level>();

	private static final long TEN_K = 10 * FileUtils.ONE_KB;
	
	@Override
	protected Object doExecute() throws Exception {
		
		if (cloudOverrides != null) {
			if (cloudOverrides.length() >= TEN_K) {
				throw new CLIStatusException(CloudifyErrorMessages.CLOUD_OVERRIDES_TO_LONG.getName());
			}
		}
		
		RecipePathResolver pathResolver = new RecipePathResolver();
		
		File providerDirectory = null;
		if (pathResolver.resolveCloud(new File(cloudProvider))) {
			providerDirectory = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("cloud_driver_file_doesnt_exist", 
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}
		
		setSecurityMode();
		if (securityProfile.equalsIgnoreCase(CloudifyConstants.SPRING_PROFILE_SECURE_NO_SSL)
				|| securityProfile.equalsIgnoreCase(CloudifyConstants.SPRING_PROFILE_SECURE)) {
			copySecurityFiles(providerDirectory.getAbsolutePath());
		}
		
		// load the cloud file
		File cloudFile = findCloudFile(providerDirectory);
		
		// load properties file
		File cloudPropertiesFile = new File(providerDirectory, cloudFile.getName().split("\\.")[0] + DSLUtils.PROPERTIES_FILE_SUFFIX);

		File backupCloudPropertiesFile = new File(cloudPropertiesFile.getParentFile(), 
				cloudPropertiesFile.getName() + ".backup");
		
		// check for overrides file
		Cloud cloud = null;
		if (cloudOverrides == null) {
			cloud = ServiceReader.readCloud(cloudFile);
		} else {
			
			// read cloud with overrides properties so they reflect during bootstrap.
			cloud = ServiceReader.
					readCloudFromDirectory(providerDirectory.getAbsolutePath(), 
							FileUtils.readFileToString(cloudOverrides));
			
			// create a backup of the existing properties file
			if (cloudPropertiesFile.exists()) {
				FileUtils.copyFile(cloudPropertiesFile, backupCloudPropertiesFile);
			}
			
			// append the overrides file to the existing properties file
			FileAppender appender = new FileAppender(cloudPropertiesFile);
			appender.append("Overrides File Properties", cloudOverrides);
			appender.flush();
		}

		// start the installer
		CloudGridAgentBootstrapper installer = new CloudGridAgentBootstrapper();
		installer.setProviderDirectory(providerDirectory);
		if (this.adminFacade != null) {
			installer.setAdminFacade(this.adminFacade);
		} else {
			installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		}
		installer.setProgressInSeconds(10);
		installer.setVerbose(verbose);
		installer.setCloud(cloud);
		installer.setCloudFile(cloudFile);
		installer.setNoWebServices(noWebServices);

		// Bootstrap!

		// Note: The cloud driver may be very verbose. This is EXTEREMELY useful
		// when debugging ESM
		// issues, but can also clutter up the CLI display. It makes more sense to temporarily raise the log level here,
		// so that all of these
		// messages will not be displayed on the console.
		limitLoggingLevel();
		logger.info(getFormattedMessage("bootstrapping_cloud", cloudProvider));
		try {
			// TODO: Create the event listeners here and pass them to the installer.
			installer.boostrapCloudAndWait(securityProfile, username, password, keystorePassword, timeoutInMinutes, 
					TimeUnit.MINUTES);
			return getFormattedMessage("cloud_started_successfully", cloudProvider);
		} finally {
			// if an overrides file was passed, then the properties file is dirty. delete it.
			if (cloudOverrides != null) {
				cloudPropertiesFile.delete();
			}
			if (backupCloudPropertiesFile.exists()) {
				// restore original properties file if it existed in the first place (backup file exists).
				FileUtils.copyFile(backupCloudPropertiesFile, cloudPropertiesFile);
				// delete temp backup file
				backupCloudPropertiesFile.delete();
				
			}
			installer.close();
			restoreLoggingLevel();
		}

	}


	private void limitLoggingLevel() {

		if (!this.verbose) {
			loggerStates.clear();
			for (String loggerName : NON_VERBOSE_LOGGERS) {
				final Logger provisioningLogger = Logger.getLogger(loggerName);
				final Level logLevelBefore = provisioningLogger.getLevel();
				provisioningLogger.setLevel(Level.WARNING);
				loggerStates.put(loggerName, logLevelBefore);
			}
		}
	}

	private void restoreLoggingLevel() {
		if (!verbose) {
			Set<Entry<String, Level>> entries = loggerStates.entrySet();
			for (Entry<String, Level> entry : entries) {
				Logger provisioningLogger = Logger.getLogger(entry.getKey());
				provisioningLogger.setLevel(entry.getValue());
			}			
		}

	}

	private File findCloudFile(final File providerDirectory) throws FileNotFoundException {
		if (!providerDirectory.exists() || !providerDirectory.isDirectory()) {
			throw new FileNotFoundException("Could not find cloud provider directory: " + providerDirectory);
		}

		File[] cloudFiles = providerDirectory.listFiles(new FilenameFilter() {

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

	
	private void setSecurityMode() {
		
		if (secured) {
			//enable security
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
			//disable security
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
				throw new IllegalArgumentException("'-keystorePassword' is only valid when '-secured' is set");
			}
		}
			
		
		if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
			throw new IllegalArgumentException("Password is missing or empty");
		}
		
		if (StringUtils.isBlank(username) && StringUtils.isNotBlank(password)) {
			throw new IllegalArgumentException("Username is missing or empty");
		}
		
		if (StringUtils.isNotBlank(keystore) && StringUtils.isBlank(keystorePassword)) {
			throw new IllegalArgumentException("keystorePassword is missing or empty");
		}
		
		if (StringUtils.isBlank(keystore) && StringUtils.isNotBlank(keystorePassword)) {
			throw new IllegalArgumentException("keystore is missing or empty");
		}

	}
	
	
	private void copySecurityFiles(final String providerDirectory) throws Exception {
		
		//handle the configuration file
		if (StringUtils.isNotBlank(securityFilePath)) {
			File securitySourceFile = new File(securityFilePath);
			if (!securitySourceFile.isFile()) {
				throw new Exception("Security configuration file not found: " + securityFilePath);
			}
			
			//copy to the overrides folder, to be copied to all management servers as well
			File securityTargetFile = new File(providerDirectory,  OVERRIDES_FOLDER + PATH_SEPARATOR
					+ "spring-security.xml");
			FileUtils.copyFile(securitySourceFile, securityTargetFile);
		} else {
			//TODO : should we use the default security location and assume it was edited by the user?
			//securityFilePath = CLOUDIFY_HOME + "/config/security/spring-security.xml";
			throw new IllegalArgumentException("-securityfile is missing or empty");
		}
		
		//handle the keystore file
		if (StringUtils.isNotBlank(keystore)) {
			File keystoreSourceFile = new File(keystore);
			if (!keystoreSourceFile.isFile()) {
				throw new Exception("Keystore file not found: " + keystore);
			}
			
			//copy to the override folder, to be copied to all management servers as well
			File keystoreTargetFile = new File(providerDirectory,  OVERRIDES_FOLDER + PATH_SEPARATOR + "keystore");
			FileUtils.copyFile(keystoreSourceFile, keystoreTargetFile);
		}
		
	}
	
}