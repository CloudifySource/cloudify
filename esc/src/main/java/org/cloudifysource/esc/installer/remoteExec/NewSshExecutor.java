/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.esc.installer.remoteExec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

import org.cloudifysource.dsl.cloud.RemoteExecutionModes;
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
public class NewSshExecutor implements RemoteExecutor {

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(NewSshExecutor.class.getName());

	private static final String NEWLINE = System.getProperty("line.separator");

	private AgentlessInstaller installer;

	@Override
	public void execute(final String targetHost, final InstallationDetails details, final String scriptPath,
			final long endTimeMillis)
			throws InstallerException, TimeoutException, InterruptedException {
		final int port = Utils.getRemoteExecutionPort(details.getInstallerConfiguration(), RemoteExecutionModes.SSH);
		final String fullCommand = "chmod +x " + scriptPath + ";" + scriptPath;

		SSHClient sshClient = Utils.createSSHClient(details, targetHost, port);
		Session session = null;

		try {
			session = sshClient.startSession();
			session.allocateDefaultPTY();
			Command cmd = session.exec(fullCommand);
			BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));

			StringBuilder sb = new StringBuilder();
			while (System.currentTimeMillis() < endTimeMillis) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				logger.fine(line);
				installer.publishEvent("ssh_output_line", line);
				sb.append(line).append(NEWLINE);

			}

			Integer exitCode = cmd.getExitStatus();
			if (exitCode == null) {
				throw new InstallerException("Remote command returned a null exit code!");
			}

			if (exitCode == 0) {
				return;
			} else {
				throw new InstallerException("Command exited with abnormal exit code: " + exitCode
						+ ". Command output was: " + sb.toString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (IOException e) {
					logger.log(Level.WARNING, "Failed to close ssh session: " + e.getMessage(), e);
				}
			}
			try {
				sshClient.close();
			} catch (IOException e) {
				logger.log(Level.WARNING, "Failed to close ssh client: " + e.getMessage(), e);
			}
		}

	}

	@Override
	public void initialize(final AgentlessInstaller installer, final InstallationDetails details) {

		this.installer = installer;
	}

}
