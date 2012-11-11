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
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.shell.installer.CloudGridAgentBootstrapper;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.commands.AbstractGSCommand;
import org.cloudifysource.shell.commands.CLIStatusException;
import org.cloudifysource.shell.rest.RestAdminFacade;


@Command(
		scope = "cloudify",
		name = "bootstrap-cloud",
		description = "Starts Cloudify Agent without any zone, and the Cloudify management processes on the provided cloud.")
public class BootstrapCloud extends AbstractGSCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 60;
	private static final String PATH_SEPARATOR = System.getProperty("file.separator");
	private static final String CLOUDIFY_HOME = System.getProperty("JSHOMEDIR");

	@Argument(required = true, name = "provider", description = "the cloud provider to use")
	String cloudProvider;
	
    @Option(required = false, description = "The username when connecting to a secure admin server", name = "-user")
    private String username;

    @Option(required = false, description = "The password when connecting to a secure admin server", name = "-pwd",
            aliases = {"-password" })
    private String password;
    
    @Option(required = false, description = "Path to a custom spring security configuration file", name = "-security")
    private String securityFilePath;

	@Option(required = false, name = "-timeout",
			description = "The number of minutes to wait until the operation is done.")
	int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	
	private static final String[] NON_VERBOSE_LOGGERS = { DefaultProvisioningDriver.class.getName(), AgentlessInstaller.class.getName() };
	private Map<String, Level> loggerStates = new HashMap<String, Level>();

	
	@Override
	protected Object doExecute() throws Exception {
		RecipePathResolver pathResolver = new RecipePathResolver();
		
		File providerDirectory = null;
		if (pathResolver.resolveCloud(new File(cloudProvider))) {
			providerDirectory = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("cloud_driver_file_doesnt_exist", 
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}
		
		//copy custom security config file to the overrides folder
		
		copySecurityFile(providerDirectory.getAbsolutePath());
		
		// load the cloud file
		File cloudFile = findCloudFile(providerDirectory);
		Cloud cloud = ServiceReader.readCloud(cloudFile);

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
			installer.boostrapCloudAndWait(username, password, timeoutInMinutes, TimeUnit.MINUTES);
			return getFormattedMessage("cloud_started_successfully", cloudProvider);
		} finally {
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

	private File findCloudFile(File providerDirectory) throws FileNotFoundException {
		if (!providerDirectory.exists() || !providerDirectory.isDirectory()) {
			throw new FileNotFoundException("Could not find cloud provider directory: " + providerDirectory);
		}

		File[] cloudFiles = providerDirectory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
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

	public static void main(String[] args) throws Exception {
		BootstrapCloud cmd = new BootstrapCloud();
		cmd.cloudProvider = "ec2";
		cmd.verbose = true;
		cmd.adminFacade = new RestAdminFacade();
		cmd.execute(null);
	}
	
	private void copySecurityFile(final String providerDirectory) throws Exception {
		if (securityFilePath == null) {
			securityFilePath = CLOUDIFY_HOME + "/config/security/spring-security.xml";
		}
		
		File securitySourceFile = new File(securityFilePath);
		if (!securitySourceFile.isFile()) {
			throw new Exception("Security configuration file not found: " + securityFilePath);
		}
		File securityTargetFile = new File(providerDirectory, "upload" + PATH_SEPARATOR
				+ "cloudify-overrides" + PATH_SEPARATOR + "config" + PATH_SEPARATOR + "security" + PATH_SEPARATOR
				+ "spring-security.xml");
		FileUtils.copyFile(securitySourceFile, securityTargetFile);	
	}
}
