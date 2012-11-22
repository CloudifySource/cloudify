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
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.CloudifyLicenseVerifier;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.installer.CLILocalhostBootstrapperListener;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;
import org.fusesource.jansi.Ansi.Color;

import com.j_spaces.kernel.Environment;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        Starts Cloudify Agent without any zone, and the Cloudify management processes on local machine. These
 *        processes are isolated from Cloudify processes running on other machines.
 * 
 *        Optional arguments:
 *        lookup-groups - A unique name that is used to group together Cloudify components (default: localcloud).
 *        nic-address - The IP address of the local host network card. Specify when local machine has more
 *        than one network adapter, and a specific network card should be used for network communication.
 *        user - The username for a secure connection to the rest server
 *        pwd - The password for a secure connection to the rest server
 *        timeout - The number of minutes to wait until the operation is completed (default: 5).
 * 
 *        Command syntax: bootstrap-localcloud [-lookup-groups lookup-groups] [-nic-address nic-address] 
 *        [-user username] [-password password] [-timeout timeout]
 */
@Command(scope = "cloudify", name = "bootstrap-localcloud", description = "Starts Cloudify Agent without any zone,"
		+ " and the Cloudify management processes on local machine. These processes are isolated from Cloudify "
		+ "processes running on other machines.")
public class BootstrapLocalCloud extends AbstractGSCommand {

	private static final int DEFAULT_PROGRESS_INTERVAL = 2;
	private static final int DEFAULT_TIMEOUT = 5;
	private static final String PATH_SEPARATOR = System.getProperty("file.separator");
	private static final String CLOUDIFY_HOME = Environment.getHomeDirectory();  //JSHOMEDIR is not set yet	

	@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together"
			+ " Cloudify components. The default localcloud lookup group is '"
			+ LocalhostGridAgentBootstrapper.LOCALCLOUD_LOOKUPGROUP
			+ "'. Override in order to start multiple local clouds on the local machine.")
	private String lookupGroups;
	
    @Option(required = false, description = "The username for a secure connection to the rest server", name = "-user")
    private String username;

    @Option(required = false, description = "The password for a secure connection to the rest server", name = "-pwd",
            aliases = {"-password" })
    private String password;
    
    @Option(required = false, description = "Path to a custom spring security configuration file", name = "-security")
    private String securityFilePath;

	@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. "
			+ "Specify when local machine has more than one network adapter, and a specific network card should be "
			+ "used for network communication.")
	private String nicAddress = "127.0.0.1";

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is "
			+ "done.")
	private int timeoutInMinutes = DEFAULT_TIMEOUT;
	
	private boolean isSecurityOn = false;

		
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
		if (isSecurityOn) {
			//copy the security configuration file to the overrides folder
			copySecurityFile();
		}
		
		final LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
		installer.setVerbose(verbose);
		installer.setLookupGroups(lookupGroups);
		installer.setNicAddress(nicAddress);
		installer.setProgressInSeconds(DEFAULT_PROGRESS_INTERVAL);
		installer.setWaitForWebui(true);
		installer.addListener(new CLILocalhostBootstrapperListener());
		installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		installer.startLocalCloudOnLocalhostAndWait(isSecurityOn, username, password, timeoutInMinutes, TimeUnit.MINUTES);

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

				return pathname.getName().startsWith("javac");
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

	private void setSecurityMode() {
		if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
			throw new IllegalArgumentException("Password is missing or empty");
		}
		
		if (StringUtils.isBlank(username) && StringUtils.isNotBlank(password)) {
			throw new IllegalArgumentException("Username is missing or empty");
		}
		
		if ((StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) ||
				StringUtils.isNotBlank(securityFilePath)) {
			
			//activate security
			isSecurityOn = true;
		}
	}
	
	private void copySecurityFile() throws Exception {
		String defaultSecurityFilePath = CLOUDIFY_HOME + PATH_SEPARATOR + "config" + PATH_SEPARATOR + "security" + PATH_SEPARATOR + "spring-security.xml";
		if (StringUtils.isBlank(securityFilePath)) {
			//TODO : should we use the default security location and assume it was edited by the user?
			securityFilePath = defaultSecurityFilePath;
		}
		
		File securitySourceFile = new File(securityFilePath);
		if (!securitySourceFile.isFile()) {
			throw new Exception("Security configuration file not found: " + securityFilePath);
		}
		
		if (!securityFilePath.equalsIgnoreCase(defaultSecurityFilePath)) {
			FileUtils.copyFile(securitySourceFile, new File(defaultSecurityFilePath));
		}
	}

}
