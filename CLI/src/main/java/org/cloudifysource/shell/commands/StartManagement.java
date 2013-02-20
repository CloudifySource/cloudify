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

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.CloudifyLicenseVerifier;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        Starts Cloudify Agent with management zone, and the Cloudify management processes on local machine.
 * 
 *        Optional arguments:
 *        lookup-groups - A unique name that is used to group together Cloudify components. Override
 *        in order to group together cloudify managements/agents on a network that supports multicast.
 *        nic-address - The IP address of the local host network card. Specify when local machine has more than one 
 *        network adapter, and a specific network card should be used for network communication.
 *        user - The username for a secure connection to the rest server
 *        pwd - The password for a secure connection to the rest server
 *        timeout - The number of minutes to wait until
 *        the operation is completed (default: 5 minutes)
 *        lookup-locators - A list of IP addresses used to identify all management machines. Override when using a
 *        network without multicast support (Default: null).
 *        auto-shutdown - determines if undeploying or scaling-in the last service instance on the machine also 
 *        triggers agent shutdown (default: false).
 *        no-web-services - if set, no attempt to deploy the rest admin and web-ui will be made.
 *        no-management-space - if set, no attempt to deploy the management space will be made.
 *        cloud-file - if set, designates the location of the cloud configuration file.
 * 
 *        Command syntax: start-management [-lookup-groups lookup-groups] [-nicAddress nicAddress] [-user username]
 *        [-password password] [-timeout timeout] [-lookup-locators lookup-locators] [-auto-shutdown auto-shutdown]
 *        [-no-web-services no-web-services] [-no-management-space no-management-space] [-cloud-file cloud-file]
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
	private static final String SPRING_SECURITY_CONFIG_FILE = 
			System.getenv(CloudifyConstants.SPRING_SECURITY_CONFIG_FILE_ENV_VAR);
	private static final String KEYSTORE_FILE = System.getenv(CloudifyConstants.KEYSTORE_FILE_ENV_VAR);
	private static final String KEYSTORE_PASSWORD = System.getenv(CloudifyConstants.KEYSTORE_PASSWORD_ENV_VAR);
	private static String securityProfile = System.getenv(CloudifyConstants.SPRING_ACTIVE_PROFILE_ENV_VAR);
	
	@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together "
			+ "different Cloudify machines. Default is based on the product version. Override in order to group "
			+ "together cloudify managements/agents on a network that supports multicast.")
	private final String lookupGroups = null;

	@Option(required = false, name = "-lookup-locators", description = "A list of ip addresses used to identify all "
			+ "management machines. Default is null. Override when using a network without multicast.")
	private final String lookupLocators = null;

	@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. "
			+ "Specify when local machine has more than one network adapter, and a specific network card should be"
			+ " used for network communication.")
	private String nicAddress;
	
    @Option(required = false, description = "The username for a secure connection to the rest server", name = "-user")
    private String username;

    @Option(required = false, description = "The password for a secure connection to the rest server", name = "-password")
    private String password;

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
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws Exception {
		new CloudifyLicenseVerifier().verifyLicense();

		if (getTimeoutInMinutes() < 0) {
			throw new CLIException("-timeout cannot be negative");
		}
		
		if (SPRING_SECURITY_CONFIG_FILE == null) {
			throw new IllegalStateException("Environment variable " 
					+ CloudifyConstants.SPRING_SECURITY_CONFIG_FILE_ENV_VAR + " cannot be null");
		}
		if (securityProfile == null) {
			throw new IllegalStateException("Environment variable " + CloudifyConstants.SPRING_ACTIVE_PROFILE_ENV_VAR
					+ " cannot be null");
		}
		if (CloudifyConstants.SPRING_PROFILE_SECURE.equals(securityProfile)) {
			if (KEYSTORE_FILE == null) {
				throw new IllegalStateException("Environment variable " + CloudifyConstants.KEYSTORE_FILE_ENV_VAR 
						+ " cannot be null");				
			}
			if (KEYSTORE_PASSWORD == null) {
				throw new IllegalStateException("Environment variable " + CloudifyConstants.KEYSTORE_PASSWORD_ENV_VAR 
						+ " cannot be null");
			}
		}

		setSecurityMode();

		final LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
		installer.setVerbose(verbose);
		installer.setLookupGroups(lookupGroups);
		installer.setLookupLocators(lookupLocators);
		installer.setNicAddress(nicAddress);
		installer.setProgressInSeconds(DEFAULT_PROGRESS_INTERVAL_SECONDS);
		installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		installer.setNoWebServices(noWebServices);
		installer.setNoManagementSpace(noManagementSpace);
		installer.setNotHighlyAvailableManagementSpace(isNotHAManagementSpace());
		installer.setAutoShutdown(autoShutdown);
		installer.setWaitForWebui(true);
		installer.setCloudFilePath(cloudFileName);

		installer.startManagementOnLocalhostAndWait(securityProfile, SPRING_SECURITY_CONFIG_FILE, username, password,
				KEYSTORE_FILE, KEYSTORE_PASSWORD, getTimeoutInMinutes(), TimeUnit.MINUTES);
		return "Management started successfully. Use the shutdown-management command to shutdown"
				+ " management processes running on local machine.";
	}

	private boolean isNotHAManagementSpace() throws IOException, DSLException {
		if (cloudFileName != null && !cloudFileName.trim().isEmpty()) {
			File cloudFile = new File(cloudFileName);
			
			final Cloud cloud = ServiceReader.readCloud(cloudFile);
			if (cloud != null) {
				if (cloud.getProvider() != null) {
					final int numberOfManagementMachines = cloud.getProvider().getNumberOfManagementMachines();
					return numberOfManagementMachines < 2;
				}
			}

		}
		return true;
	}
	public int getTimeoutInMinutes() {
		return timeoutInMinutes;
	}

	public void setTimeoutInMinutes(final int timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
	}

	public boolean isAutoShutdown() {
		return autoShutdown;
	}

	public void setAutoShutdown(final boolean autoShutdown) {
		this.autoShutdown = autoShutdown;
	}
	
	private void setSecurityMode() throws IOException {
		
		if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
			throw new IllegalArgumentException("Password is missing or empty");
		}
		
		if (StringUtils.isBlank(username) && StringUtils.isNotBlank(password)) {
			throw new IllegalArgumentException("Username is missing or empty");
		}
		
		//no need to copy security config file / keystore file since we're on the mgmt server.
		if (StringUtils.isBlank(securityProfile)) {
			// TODO [noak] : log this, warning?
			securityProfile = CloudifyConstants.SPRING_PROFILE_NON_SECURE;
		}

		// The security files are expected to be in <cloudify home>\config\security
		if (securityProfile.equalsIgnoreCase(CloudifyConstants.SPRING_PROFILE_SECURE_NO_SSL)
				|| securityProfile.equalsIgnoreCase(CloudifyConstants.SPRING_PROFILE_SECURE)) {
			//verify we have the security config file at place
			File securityConfigFile = new File(SPRING_SECURITY_CONFIG_FILE);
			if (!securityConfigFile.isFile()) {
				throw new IllegalArgumentException("Security configuration file not found on management server at the "
						+ "expected location: " + securityConfigFile.getCanonicalPath());
			}
		}
		
		if (securityProfile.equalsIgnoreCase(CloudifyConstants.SPRING_PROFILE_SECURE)) {
			//verify we have the keystore file at place
			File keystoreFile = new File(KEYSTORE_FILE);
			if (!keystoreFile.isFile()) {
				throw new IllegalArgumentException("Keystore file not found on management server at the expected "
						+ "location: " + keystoreFile.getCanonicalPath());
			}
		}
	}

}