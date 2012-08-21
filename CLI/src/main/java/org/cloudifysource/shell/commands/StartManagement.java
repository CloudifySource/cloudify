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
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        Starts Cloudify Agent with management zone, and the Cloudify management processes on local machine.
 * 
 *        Optional arguments: lookup-groups - A unique name that is used to group together Cloudify components. Override
 *        in order to group together cloudify managements/agents on a network that supports multicast. nic-address - The
 *        IP address of the local host network card. Specify when local machine has more than one network adapter, and a
 *        specific network card should be used for network communication. timeout - The number of minutes to wait until
 *        the operation is completed (default: 5 minutes) lookup-locators - A list of IP addresses used to identify all
 *        management machines. Override when using a network without multicast support (Default: null). auto-shutdown -
 *        determines if undeploying or scaling-in the last service instance on the machine also triggers agent shutdown
 *        (default: false). no-web-services - if set, no attempt to deploy the rest admin and web-ui will be made.
 *        no-management-space - if set, no attempt to deploy the management space will be made. cloud-file - if set,
 *        designates the location of the cloud configuration file.
 * 
 *        Command syntax: start-management [-lookup-groups lookup-groups] [-nicAddress nicAddress] [-timeout timeout]
 *        [-lookup-locators lookup-locators] [-auto-shutdown auto-shutdown] [-no-web-services no-web-services]
 *        [-no-management-space no-management-space] [-cloud-file cloud-file]
 */
@Command(
		scope = "cloudify",
		name = "start-management",
		description = "For internal use only! Starts Cloudify Agent with management zone, "
				+ "and the Cloudify management processes on local machine. " 
				+ "The management processes communicate with other"
				+ " agent and management machines.")
public class StartManagement extends AbstractGSCommand {

	private static final int DEFAULT_PROGRESS_INTERVAL_SECONDS = 10;

	private static final int DEFAULT_TIMEOUNT_MINUTES = 5;

	@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together "
			+ "different Cloudify machines. Default is based on the product version. Override in order to group "
			+ "together cloudify managements/agents on a network that supports multicast.")
	private String lookupGroups = null;

	@Option(required = false, name = "-lookup-locators", description = "A list of ip addresses used to identify all "
			+ "management machines. Default is null. Override when using a network without multicast.")
	private String lookupLocators = null;

	@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. "
			+ "Specify when local machine has more than one network adapter, and a specific network card should be"
			+ " used for network communication.")
	private String nicAddress;

	@Option(required = false, name = "-no-web-services",
			description = "if set, no attempt to deploy the rest admin and" + " web-ui will be made")
	private boolean noWebServices;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is"
			+ " done. By default waits 5 minutes.")
	private int timeoutInMinutes = DEFAULT_TIMEOUNT_MINUTES;

	@Option(required = false, name = "-auto-shutdown", description = "Determines if undeploying or scaling-in the last"
			+ " service instance on the machine also triggers agent shutdown. By default false.")
	private boolean autoShutdown = false;

	@Option(required = false, name = "-no-management-space", description = "if set, no attempt to deploy the"
			+ " management space will be made")
	private boolean noManagementSpace;

	@Option(required = false, name = "-cloud-file", description = "if set, designated the location of the cloud"
			+ " configuration file")
	private String cloudFileName;

	/**
	 * Parses the cloud configuration file.
	 * 
	 * @param cloudFile The cloud configuration file
	 * @return Cloud object, configured according to the given file
	 * @throws IOException
	 */
	private Cloud parseCloud(final File cloudFile)
			throws IOException {

		Cloud cloud = null;

		if (cloudFile != null) {
			if (cloudFile.isFile()) {
				try {
					cloud = ServiceReader.readCloud(cloudFile);
				} catch (final DSLException e) {
					throw new IllegalArgumentException("Cloud configuration file: " + cloudFile
							+ " could not be parsed: " + e.getMessage(), e);
				}
			} else {
				throw new IllegalArgumentException(cloudFile + " is not a file");
			}
		}
		return cloud;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws Exception {

		if (timeoutInMinutes < 0) {
			throw new CLIException("-timeout cannot be negative");
		}

		boolean notHighlyAvailableManagementSpace = false;
		File cloudFile = null;

		String cloudConfigurationContents = null;
		if (cloudFileName != null && !cloudFileName.trim().isEmpty()) {
			cloudFile = new File(cloudFileName);
			cloudConfigurationContents = FileUtils.readFileToString(cloudFile);
			final Cloud cloud = parseCloud(cloudFile);
			if (cloud != null) {
				if (cloud.getProvider() != null) {
					final int numberOfManagementMachines = cloud.getProvider().getNumberOfManagementMachines();
					notHighlyAvailableManagementSpace = numberOfManagementMachines < 2;
				}
			}

		}

		final LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
		installer.setVerbose(verbose);
		installer.setLookupGroups(lookupGroups);
		installer.setLookupLocators(lookupLocators);
		installer.setNicAddress(nicAddress);
		installer.setProgressInSeconds(DEFAULT_PROGRESS_INTERVAL_SECONDS);
		installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		installer.setNoWebServices(noWebServices);
		installer.setNoManagementSpace(noManagementSpace);
		installer.setNotHighlyAvailableManagementSpace(notHighlyAvailableManagementSpace);
		installer.setAutoShutdown(autoShutdown);
		installer.setWaitForWebui(true);
		installer.setCloudContents(cloudConfigurationContents);
		installer.startManagementOnLocalhostAndWait(timeoutInMinutes,
				TimeUnit.MINUTES);
		return "Management started successfully. Use the shutdown-management command to shutdown"
				+ " management processes running on local machine.";
	}

}