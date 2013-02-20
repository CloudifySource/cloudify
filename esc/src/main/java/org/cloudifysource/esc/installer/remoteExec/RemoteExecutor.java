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

import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;

/*******
 * An interface for remote command execution used to launch a cloudify agent on
 * a remote machine.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
public interface RemoteExecutor {

	/**********
	 * Executes a command on the remote host.
	 *
	 * @param targetHost
	 *            the target host where the command will be executed.
	 * @param details
	 *            the details of the installation request, including details of
	 *            the remote host.
	 * @param command
	 *            the command to execute.
	 * @param endTimeMillis
	 *            end time by which the command must finish.
	 * @throws InstallerException
	 *             if the command failed or could not be executed.
	 * @throws TimeoutException
	 *             if the timeout expired.
	 * @throws InterruptedException
	 *             in the request was interrupted while waiting on the network.
	 */
	void execute(String targetHost, final InstallationDetails details, final String command,
			final long endTimeMillis)
			throws InstallerException, TimeoutException, InterruptedException;


	/**********
	 * Initializes the remote executor. This method is called once, before the execute request is issued.
	 * @param installer The agentless installer that invoked this remote executor.
	 * @param details the details of the installation request.
	 */
	void initialize(final AgentlessInstaller installer, final InstallationDetails details);

}
