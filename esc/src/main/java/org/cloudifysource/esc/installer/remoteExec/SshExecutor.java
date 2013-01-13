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

/*********
 * Executor implementation for SSH remote calls. Uses Ant ssh task.
 *
 * @author barakme
 * @since 2.5.0
 */
public class SshExecutor implements RemoteExecutor {


	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(SshExecutor.class.getName());

	@Override
	public void execute(final String targetHost, final InstallationDetails details, final String scriptPath,
			final long endTimeMillis)
			throws InstallerException, TimeoutException, InterruptedException {

		final String fullCommand = "chmod +x " + scriptPath + ";" + scriptPath;

		// TODO - replace Ant based ssh command implementation with sshj
		try {
			Utils.executeSSHCommand(targetHost, fullCommand, details.getUsername(), details.getPassword(),
					details.getKeyFile(),
					endTimeMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		} catch (final BuildException e) {
			// There really should be a better way to check that this is a
			// timeout
			logger.log(Level.FINE, "The remote boostrap command failed with error: " + e.getMessage()
					+ ". The command that failed to execute is : " + fullCommand, e);

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

	@Override
	public void initialize(final AgentlessInstaller installer, final InstallationDetails details) {
	}

}
