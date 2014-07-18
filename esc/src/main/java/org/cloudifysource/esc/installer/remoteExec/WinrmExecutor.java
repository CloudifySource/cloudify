/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.esc.installer.remoteExec;

import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.cloudifysource.domain.cloud.CloudTemplateInstallerConfiguration;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.installer.remoteExec.PowershellClient.PowerShellOutputListener;




/********
 * Remote Executor implementation for Windows Remote Management, using the
 * powershell command.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
public class WinrmExecutor implements RemoteExecutor {

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(WinrmExecutor.class.getName());

	private static final String CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX = "/[a-zA-Z][$]/.*";


	private static Pattern pattern = Pattern.compile(CIFS_ABSOLUTE_PATH_WITH_DRIVE_REGEX);


	@Override
	public void initialize(final AgentlessInstaller installer, final InstallationDetails details) {
	}

	/****************
	 * Given a path of the type /C$/PATH - indicating an absolute cifs path,
	 * returns /PATH. If the string does not match, returns the original
	 * unmodified string.
	 *
	 * @param str
	 *            the input path.
	 * @return the input path, adjusted to remove the cifs drive letter, if it
	 *         exists, or the original path if the drive letter is not present.
	 */
	public static String normalizeCifsPath(final String str) {

		if (pattern.matcher(str).matches()) {
			final char drive = str.charAt(1);
			return drive + ":\\" + str.substring("/c$/".length()).replace('/', '\\');
		}
		return str;
	}


	@Override
	public void execute(final String targetHost, final InstallationDetails details, final String scriptPath,
			final long endTimeMillis)
					throws InstallerException, TimeoutException, InterruptedException {

		final String fullCommand = normalizeCifsPath(scriptPath);

		final PowershellClient client = new PowershellClient();
		client.addOutputListener(new PowerShellOutputListener() {

			@Override
			public void onPowerShellOutput(final String line) {
				logger.info(line);

			}
		});
		
		CloudTemplateInstallerConfiguration installerConfiguration = details.getInstallerConfiguration();
		
		// if "testRemoteExecutionConnection" set to True check the WinRM connection before performing the remote call
		if (installerConfiguration.isTestRemoteExecutionConnection()) {
			int connectionTestIntervalMillis = installerConfiguration.getConnectionTestIntervalMillis();
			int remoteExecConnectionTimeoutMillis = installerConfiguration.getRemoteExecutionConnectionTimeoutMillis();
			long end = System.currentTimeMillis() + remoteExecConnectionTimeoutMillis;
			
			try {
				client.checkWinrmConnection(targetHost, details.getUsername(), details.getPassword(), 
						details.getLocalDir(), connectionTestIntervalMillis, end);
			} catch (final PowershellClientException e) {
				throw new InstallerException("WinRM connection failed", e);
			}
		}
		
		logger.fine("Invoking remote powershell command " + fullCommand + " on target host: " + targetHost);
		try {
			client.invokeRemotePowershellCommand(targetHost, fullCommand, details.getUsername(), 
						details.getPassword(), details.getLocalDir());
		} catch (final PowershellClientException e) {
			throw new InstallerException("Failed to execute powershell remote command", e);
		}
		
	}

}
