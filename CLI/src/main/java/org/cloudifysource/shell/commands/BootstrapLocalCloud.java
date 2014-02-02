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
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.CloudifyLicenseVerifier;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.KeystoreFileVerifier;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLILocalhostBootstrapperListener;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;
import org.fusesource.jansi.Ansi.Color;

import com.j_spaces.kernel.Environment;

/**
 * @author rafi, barakm
 * @since 2.0.0
 *
 *        Starts Cloudify Agent without any zone, and the Cloudify management
 *        processes on local machine. These processes are isolated from Cloudify
 *        processes running on other machines.
 *
 *        Optional arguments: lookup-groups - A unique name that is used to
 *        group together Cloudify components (default: localcloud). nic-address
 *        - The IP address of the local host network card. Specify when local
 *        machine has more than one network adapter, and a specific network card
 *        should be used for network communication. user - The username for a
 *        secure connection to the rest server pwd - The password for a secure
 *        connection to the rest server timeout - The number of minutes to wait
 *        until the operation is completed (default: 5).
 *
 *        Command syntax: bootstrap-localcloud [-lookup-groups lookup-groups]
 *        [-nic-address nic-address] [-user username] [-password password]
 *        [-timeout timeout]
 */
@Command(scope = "cloudify", name = "bootstrap-localcloud", description = "Starts Cloudify Agent without any zone,"
		+ " and the Cloudify management processes on local machine. These processes are isolated from Cloudify "
		+ "processes running on other machines.")
public class BootstrapLocalCloud extends AbstractGSCommand {

	private static final int DEFAULT_PROGRESS_INTERVAL = 2;
	private static final int DEFAULT_TIMEOUT = 5;
	private static final String PATH_SEPARATOR = System.getProperty("file.separator");
	// JSHOMEDIR is not set yet
	private static final String CLOUDIFY_HOME = Environment.getHomeDirectory();
	private static final String DEFAULT_SECURITY_FOLDER =
			CLOUDIFY_HOME + PATH_SEPARATOR + "config" + PATH_SEPARATOR + "security";
	private static final String DEFAULT_SECURITY_FILE_PATH =
			DEFAULT_SECURITY_FOLDER + PATH_SEPARATOR + "spring-security.xml";

	@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together"
			+ " Cloudify components. The default localcloud lookup group is '"
			+ LocalhostGridAgentBootstrapper.LOCALCLOUD_LOOKUPGROUP
			+ "'. Override in order to start multiple local clouds on the local machine.")
	private String lookupGroups;
	
	@Option(required = false, 
			description = "if set, bootstrap will not wait for webui service to start.", name = "-no-wait-for-webui")
	private boolean noWaitForWebui = false;

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
	private String keystoreFilePath;

	@Option(required = false, description = "The password to the keystore", name = "-keystore-password")
	private String keystorePassword;

	@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. "
			+ "Specify when local machine has more than one network adapter, and a specific network card should be "
			+ "used for network communication.")
	private String nicAddress = "127.0.0.1";

	@Option(required = false, description = "The number of minutes to wait until the operation is done.",
			name = "-timeout")
	private int timeoutInMinutes = DEFAULT_TIMEOUT;


	private String securityProfile = CloudifyConstants.SPRING_PROFILE_NON_SECURE;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws Exception {

		new CloudifyLicenseVerifier().verifyLicense();

		// first check java home is correctly configured
		final String javaHome = System.getenv("JAVA_HOME");
		if (javaHome == null || javaHome.trim().length() == 0) {
			return messages.getString("missing_java_home");
		}

		final boolean javaHomeValid = isJavaHomeValid(javaHome);
		if (!javaHomeValid) {
			return getFormattedMessage("incorrect_java_home", Color.RED, javaHome);
		}

		setSecurityMode();

		final LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
		installer.setVerbose(verbose);
		installer.setLookupGroups(lookupGroups);
		installer.setNicAddress(nicAddress);
		installer.setProgressInSeconds(DEFAULT_PROGRESS_INTERVAL);
		installer.setWaitForWebui(!noWaitForWebui);
		installer.addListener(new CLILocalhostBootstrapperListener());
		installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		installer.startLocalCloudOnLocalhostAndWait(securityProfile, securityFilePath, username, password,
				keystoreFilePath, keystorePassword, timeoutInMinutes, TimeUnit.MINUTES);

		return messages.getString("local_cloud_started");
	}

	private boolean isJavaHomeValid(final String javaHome) {
		final File javaHomeDir = new File(javaHome);
		if (!javaHomeDir.exists() || !javaHomeDir.isDirectory()) {
			return false;
		}

		final File binDir = new File(javaHomeDir, "bin");
		if (!binDir.exists() || !binDir.isDirectory()) {
			return false;
		}

		final File[] javacCandidates = binDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File pathname) {
				if (!pathname.isFile()) {
					return false;
				}
				if (ServiceUtils.isWindows()) {
					return pathname.getName().equals("javac.exe");
				} else {
					return pathname.getName().equals("javac");
				}
			}
		});

		return javacCandidates.length > 0;

	}

	public String getNicAddress() {
		return nicAddress;
	}

	public void setNicAddress(final String nicAddress) {
		this.nicAddress = nicAddress;
	}

	public int getTimeoutInMinutes() {
		return timeoutInMinutes;
	}

	public void setTimeoutInMinutes(final int timeoutInMinutes) {
		this.timeoutInMinutes = timeoutInMinutes;
	}

	private void setSecurityMode() throws CLIStatusException, IOException {

		if (secured) {
			// enable security
			if (keystoreFilePath == null || keystorePassword == null || securityFilePath == null) {
				throw new IllegalArgumentException(ShellUtils.getFormattedMessage(
						CloudifyErrorMessages.SECURED_BOOTSTRAP_IS_PARTIAL.getName()));
			}
			
			securityProfile = CloudifyConstants.SPRING_PROFILE_SECURE;
			logger.info(getFormattedMessage(CloudifyErrorMessages.SETTING_SERVER_SECURITY_PROFILE.getName(),
					CloudifyConstants.SPRING_PROFILE_SECURE));
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

			if (StringUtils.isNotBlank(keystoreFilePath)) {
				throw new IllegalArgumentException("'-keystore' is only valid when '-secured' is set");
			}

			if (StringUtils.isNotBlank(keystorePassword)) {
				throw new IllegalArgumentException("'-keystore-password' is only valid when '-secured' is set");
			}

		} else {
			if (StringUtils.isBlank(securityFilePath)) {
				throw new IllegalArgumentException("Security file is missing or empty");
			}
			
			if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
				throw new IllegalArgumentException("Password is missing or empty");
			}

			if (StringUtils.isBlank(username) && StringUtils.isNotBlank(password)) {
				throw new IllegalArgumentException("Username is missing or empty");
			}

			if (StringUtils.isBlank(keystoreFilePath)) {
				throw new IllegalArgumentException("Keystore is missing or empty");
			}
			
			if (StringUtils.isBlank(keystorePassword)) {
				throw new IllegalArgumentException("Keystore password is missing or empty");
			}

			if (StringUtils.isNotBlank(keystoreFilePath)) {
				validateKeystoreFile(keystorePassword, new File(keystoreFilePath));
			}
		}

		validateSecurityVars();

	}


	private void validateSecurityVars() throws IOException {

		// handle the configuration file
		if (!ShellUtils.isSecureConnection(securityProfile)) {
			securityFilePath = new File(DEFAULT_SECURITY_FILE_PATH).getCanonicalPath();
		} else {
			if (StringUtils.isNotBlank(securityFilePath)) {
				final File securityFile = new File(securityFilePath);
				if (!securityFile.isFile()) {
					throw new IOException("Security configuration file not found: " + securityFilePath);
				}

				securityFilePath = securityFile.getCanonicalPath();
			} else {
				throw new IllegalArgumentException("-security-file is missing or empty");
			}

			// handle the keystore file
			if (StringUtils.isNotBlank(keystoreFilePath)) {
				final File keystoreFile = new File(keystoreFilePath);
				if (!keystoreFile.isFile()) {
					throw new IOException("Keystore file not found: " + keystoreFilePath);
				}

				keystoreFilePath = keystoreFile.getCanonicalPath();
			} else {
				throw new IllegalArgumentException("-keystore is missing or empty: ");
			}
		}
	}


	private void validateKeystoreFile(final String password, final File keystoreFile) throws CLIStatusException {
		new KeystoreFileVerifier().verifyKeystoreFile(keystoreFile, password);
	}

}
