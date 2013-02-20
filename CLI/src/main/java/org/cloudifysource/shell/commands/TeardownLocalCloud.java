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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.installer.CLILocalhostBootstrapperListener;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        Tears down the Local Cloud installed on the local machine.
 * 
 *        Optional arguments: lookup-groups - A unique name that is used to
 *        group together Cloudify components. Override in order to teardown a
 *        specific local cloud running on the local machine. nic-address - The
 *        IP address of the local host network card. Specify when local machine
 *        has more than one network adapter, and a specific network card should
 *        be used for network communication. timeout - The number of minutes to
 *        wait until the operation is completed (default: 5 minutes)
 * 
 *        Command syntax: teardown-localcloud [-lookup-groups lookup-groups]
 *        [-nicAddress nicAddress] [-timeout timeout]
 */
@Command(scope = "cloudify", name = "teardown-localcloud", description = "Tears down the Local Cloud installed"
		+ " on the local machine.")
public class TeardownLocalCloud extends AbstractGSCommand {

	private static final int DEDAULT_TIMEOUT_MINUTES = 5;

	private static final int DEFAULT_PROGRESS_INTERVAL = 2;

	@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together"
			+ " Cloudify components. The default localcloud lookup group is '"
			+ LocalhostGridAgentBootstrapper.LOCALCLOUD_LOOKUPGROUP
			+ "'. Override in order to teardown a specific local cloud running on the local machine.")
	private String lookupGroups;

	@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. "
			+ "Specify when local machine has more than one network adapter, and a specific network card should be"
			+ " used for network communication.")
	private String nicAddress = "127.0.0.1";

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is"
			+ " done.")
	private int timeoutInMinutes = DEDAULT_TIMEOUT_MINUTES;

	@Option(required = false, name = "-force",
			description = "Should management machine be shutdown if other applications are installed")
	private boolean force = false;

	/**
	 * Shuts down the local cloud, and waits until shutdown is complete or until
	 * the timeout is reached.
	 * 
	 * @return command return message.
	 * @throws Exception
	 *             if command failed.
	 */
	@Override
	protected Object doExecute() throws Exception {

		if (!confirmTeardown()) {
			return getFormattedMessage("teardown_aborted");
		}
		
		if (this.adminFacade == null) {
			adminFacade = (AdminFacade) session.get(Constants.ADMIN_FACADE);
		}
		
		if (adminFacade.isConnected()) {
			adminFacade.verifyCloudAdmin();
		} else {
			if (!force) {
				throw new CLIException("Please connect to the cloud before tearing down");
			}
		}
		
		final LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
		installer.setVerbose(verbose);
		installer.setLookupGroups(lookupGroups);
		installer.setNicAddress(nicAddress);
		installer.setProgressInSeconds(DEFAULT_PROGRESS_INTERVAL);
		installer.setForce(isForce());
		installer.addListener(new CLILocalhostBootstrapperListener());
		installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));

		installer.teardownLocalCloudOnLocalhostAndWait(timeoutInMinutes, TimeUnit.MINUTES);
		session.put(Constants.ACTIVE_APP, "default");
		GigaShellMain.getInstance().setCurrentApplicationName("default");
		return getFormattedMessage("teardown_localcloud_terminated_successfully");
	}

	private boolean confirmTeardown() throws IOException {
		return ShellUtils.promptUser(session, "teardown_localcloud_confirmation_question");
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(final boolean force) {
		this.force = force;
	}
}
