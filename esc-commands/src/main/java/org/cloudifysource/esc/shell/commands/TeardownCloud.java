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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.cloudifysource.shell.GigaShellMain;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.AbstractGSCommand;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;
/**
 * Tears down the remote Cloud.
 *
 * Optional arguments:
 * 		timeout - The number of minutes to wait until the operation is completed (default: 60 minutes)
 * 		force - states whether the management machine be shutdown if other applications are installed
 *
 * @author barakme, adaml
 *
 */
@Command(scope = "cloudify", name = "teardown-cloud", description = "Terminates management machines.")
public class TeardownCloud extends AbstractGSCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 60;

	private static final int POLLING_INTERVAT_SEC = 10;

	@Argument(required = true, name = "provider", description = "the cloud provider to use")
	private String cloudProvider;

	@Option(required = false, name = "-timeout",
			description = "The number of minutes to wait until the operation is done. ")
	private final int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	@Option(required = false, name = "-force",
			description = "Should management machine be shutdown if other applications are installed")
	private final boolean force = false;

	@Override
	protected Object doExecute() throws Exception {

		if (!confirmTeardown()) {
            return getFormattedMessage("teardown_aborted");
        }

		if (this.adminFacade == null) {
			adminFacade = (AdminFacade) session.get(Constants.ADMIN_FACADE);
		}

		if (adminFacade.isConnected()) {
			adminFacade.verifyCloudAdmin();
		} else {
			if (!force) {
				throw new CLIException("Please connect to the cloud before tearing down");
			}
		}

		CloudGridAgentBootstrapper installer = new CloudGridAgentBootstrapper();

		RecipePathResolver pathResolver = new RecipePathResolver();

		File providerDirectory = null;
		if (pathResolver.resolveCloud(new File(cloudProvider))) {
			providerDirectory = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("cloud_driver_file_doesnt_exist",
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}

		// load the cloud file
		File cloudFile = findCloudFile(providerDirectory);
		Cloud cloud = ServiceReader.readCloud(cloudFile);

		installer.setProviderDirectory(providerDirectory);
		if (this.adminFacade != null) {
			installer.setAdminFacade(this.adminFacade);
		} else {
			installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		}

		installer.setProgressInSeconds(POLLING_INTERVAT_SEC);
		installer.setVerbose(verbose);
		installer.setForce(force);
		installer.setCloud(cloud);

		// Note: The cloud driver may be very verbose. This is EXTEREMELY useful
		// when debugging ESM
		// issues, but can also clutter up the CLI display. It makes more sense
		// to temporarily raise the log level here,
		// so that all of these
		// messages will not be displayed on the console.
		limitLoggingLevel();
		try {
			installer.teardownCloudAndWait(timeoutInMinutes, TimeUnit.MINUTES);

			session.put(Constants.ACTIVE_APP, "default");
			GigaShellMain.getInstance().setCurrentApplicationName("default");
			return getFormattedMessage("cloud_terminated_successfully", cloudProvider);
		} finally {
			installer.close();
			restoreLoggingLevel();
		}
	}

    private boolean confirmTeardown() throws IOException {
        return ShellUtils.promptUser(session, "teardown_cloud_confirmation_question");
    }

	private static final String[] NON_VERBOSE_LOGGERS = { DefaultProvisioningDriver.
		class.getName(), AgentlessInstaller.class.getName() };
	private final Map<String, Level> loggerStates = new HashMap<String, Level>();

	private void limitLoggingLevel() {

		if (!this.verbose) {
			loggerStates.clear();
			for (String loggerName : NON_VERBOSE_LOGGERS) {
				Logger provisioningLogger = Logger.getLogger(loggerName);
				Level logLevelBefore = provisioningLogger.getLevel();
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
}
