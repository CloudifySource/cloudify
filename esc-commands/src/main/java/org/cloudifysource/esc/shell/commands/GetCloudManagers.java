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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.FileAppender;
import org.cloudifysource.dsl.utils.RecipePathResolver;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.shell.installer.CloudGridAgentBootstrapper;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.commands.AbstractGSCommand;
import org.cloudifysource.shell.commands.CLIStatusException;

/************
 * CLI Command to list cloud managers using the cloud driver.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
@Command(
		scope = "cloudify",
		name = "get-cloud-managers",
		description = "Gets the list of cloudify managers, using the cloud configuration file.")
public class GetCloudManagers extends AbstractGSCommand {

	private static final String UNDEFINED_DETAILS = "Undefined";

	@Argument(required = true, name = "provider", description = "The cloud provider to use")
	private File cloudProvider;

	@Option(required = false, description = "Path to a file containing override properties", name = "-cloud-overrides")
	private File cloudOverrides;

	private static final long TEN_K = 10 * FileUtils.ONE_KB;

	@Override
	protected Object doExecute() throws Exception {

		if (cloudOverrides != null) {
			if (cloudOverrides.length() >= TEN_K) {
				throw new CLIStatusException(CloudifyErrorMessages.CLOUD_OVERRIDES_TO_LONG.getName());
			}
		}

		final RecipePathResolver pathResolver = new RecipePathResolver();

		File providerDirectory = null;
		if (pathResolver.resolveCloud(this.getCloudProvider())) {
			providerDirectory = pathResolver.getResolved();
		} else {
			throw new CLIStatusException("cloud_driver_file_doesnt_exist",
					StringUtils.join(pathResolver.getPathsLooked().toArray(), ", "));
		}

		final File tempFolder = createTempFolder();
		FileUtils.copyDirectoryToDirectory(providerDirectory, tempFolder);
		providerDirectory = new File(tempFolder, providerDirectory.getName());

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
		installer.setVerbose(verbose);
		installer.setCloud(cloud);
		installer.setCloudFile(cloudFile);

		// logger.info(getFormattedMessage("bootstrapping_cloud", getCloudProvider()));

		try {
			MachineDetails[] managers = installer.getCloudManagers();
			if (managers.length == 0) {
				return getFormattedMessage(CloudifyErrorMessages.MANAGEMENT_SERVERS_NOT_LOCATED.getName());
			}
			final StringBuilder sb = new StringBuilder();
			final String newline = System.getProperty("line.separator");
			for (MachineDetails manager : managers) {
				sb.append(getFormattedMessage(CloudifyErrorMessages.MANAGEMENT_SERVERS_DETAILS.getName(),
						normalizeDetailsString(manager.getMachineId()),
						normalizeDetailsString(manager.getPrivateAddress()),
						normalizeDetailsString(manager.getPublicAddress())));
				sb.append(newline);
			}
			return sb.toString();
		} finally {
			// if an overrides file was passed, then the properties file is dirty. delete it.
			if (cloudOverrides != null) {
				cloudPropertiesFile.delete();
			}
			FileUtils.deleteDirectory(tempFolder);
			installer.close();
		}

	}

	private String normalizeDetailsString(final String details) {
		if (details == null) {
			return UNDEFINED_DETAILS;
		} else {
			return details;
		}
	}

	private File createTempFolder() throws IOException {
		final File tempFile = File.createTempFile("cloud-", "");
		tempFile.delete();
		tempFile.mkdir();
		return tempFile;
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

	public File getCloudProvider() {
		return cloudProvider;
	}

	public void setCloudProvider(final File cloudProvider) {
		this.cloudProvider = cloudProvider;
	}

}
