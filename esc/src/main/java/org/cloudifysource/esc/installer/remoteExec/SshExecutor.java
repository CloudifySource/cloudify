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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.taskdefs.optional.testing.BuildTimeoutException;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.util.Utils;

public class SshExecutor implements RemoteExecutor {

	private static final String SSH_COMMAND_SEPARATOR = ";";

	private final String separator = SSH_COMMAND_SEPARATOR;
	private final StringBuilder sb = new StringBuilder();

	private boolean runInBackground = false;

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(SshExecutor.class.getName());

	/*******
	 * Adds a command to the command line.
	 * 
	 * @param str the command to add.
	 * @return this.
	 */
	public RemoteExecutor call(final String str) {
		sb.append(str);

		return this;
	}

	/********
	 * Adds a separator.
	 * 
	 * @return this.
	 */
	public RemoteExecutor separate() {
		sb.append(this.separator);
		return this;
	}

	/*********
	 * Adds an environment variable to the command line.
	 * 
	 * @param name variable name.
	 * @param value variable value.
	 * @return this.
	 */
	public RemoteExecutor exportVar(final String name, final String value) {

		String actualValue = value;
		if (value == null) {
			actualValue = "";
		}

		sb.append("export ").append(name).append("=").append(actualValue);

		separate();
		return this;
	}

	public RemoteExecutor chmodExecutable(final String path) {
		sb.append("chmod +x " + path);
		separate();
		return this;
	}

	/*****
	 * Marks a command line to be executed in the background.
	 * 
	 * @return this.
	 */
	public RemoteExecutor runInBackground() {
		sb.append(" &");
		this.runInBackground = true;

		return this;
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	public boolean isRunInBackground() {
		return runInBackground;
	}

	public void execute(final InstallationDetails details, final String command,
			final long endTimeMillis)
			throws InstallerException, TimeoutException {

		String host = null;
		if (details.isConnectedToPrivateIp()) {
			host = details.getPrivateIp();
		} else {
			host = details.getPublicIp();
		}

		try {
			Utils.executeSSHCommand(host, command, details.getUsername(), details.getPassword(), details.getKeyFile(),
					endTimeMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		} catch (final BuildException e) {
			// There really should be a better way to check that this is a
			// timeout
			logger.log(Level.FINE, "The remote boostrap command failed with error: " + e.getMessage()
					+ ". The command that failed to execute is : " + command, e);

			if (e instanceof BuildTimeoutException) {
				final TimeoutException ex =
						new TimeoutException("Remote bootstrap command failed to execute: " + e.getMessage());
				ex.initCause(e);
				throw ex;
			} else if (e instanceof ExitStatusException) {
				final ExitStatusException ex = (ExitStatusException) e;
				final int ec = ex.getStatus();
				throw new InstallerException("Remote bootstrap command failed with exit code: " + ec, e);
			} else {
				throw new InstallerException("Remote bootstrap command failed to execute.", e);
			}
		}

	}

	
	public void initialize(final AgentlessInstaller installer, final InstallationDetails details) {
	}

}
