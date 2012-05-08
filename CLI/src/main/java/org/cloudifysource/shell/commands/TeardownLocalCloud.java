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
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.util.Properties.PropertiesReader;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.installer.CLILocalhostBootstrapperListener;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        Tears down the Local Cloud installed on the local machine.
 *
 *        Optional arguments:
 *         lookup-groups - A unique name that is used to group together Cloudify components.
 *         Override in order to teardown a specific local cloud running on the local machine.
 *         nic-address - The IP address of the local host network card. Specify when local machine has more
 *         than one network adapter, and a specific network card should be used for network communication.
 *         timeout - The number of minutes to wait until the operation is completed (default: 5 minutes)
 *  
 *        Command syntax: teardown-localcloud [-lookup-groups lookup-groups] [-nicAddress nicAddress]
 *        					[-timeout timeout]
 */
@Command(scope = "cloudify", name = "teardown-localcloud", description = "Tears down the Local Cloud installed"
		+ " on the local machine.")
public class TeardownLocalCloud extends AbstractGSCommand {
	
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
	private int timeoutInMinutes = 5;
	
	@Option(required = false, name = "-force",
			description = "Should management machine be shutdown if other applications are installed")
	boolean force = false;

	/**
	 * Shuts down the local cloud, and waits until shutdown is complete or until the timeout is reached.
	 */
	@Override
	protected Object doExecute() throws Exception {

		if (!askUninstallConfirmationQuestion()){
			return getFormattedMessage("teardown_aborted");
		}
		
		final LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
		installer.setVerbose(verbose);
		installer.setLookupGroups(lookupGroups);
		installer.setNicAddress(nicAddress);
		installer.setProgressInSeconds(DEFAULT_PROGRESS_INTERVAL);
		installer.setForce(force);
		installer.addListener(new CLILocalhostBootstrapperListener());
		installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));

		installer.teardownLocalCloudOnLocalhostAndWait(timeoutInMinutes, TimeUnit.MINUTES);
		return getFormattedMessage("teardown_localcloud_terminated_successfully");
	}
	
	private boolean askUninstallConfirmationQuestion() throws IOException {

		// we skip question if the shell is running a script.
		if ((Boolean) session.get(Constants.INTERACTIVE_MODE)) {
			final String confirmationQuestion = getFormattedMessage("teardown_confirmation_question");
			System.out.print(confirmationQuestion);
			System.out.flush();
			final PropertiesReader pr = new PropertiesReader(new InputStreamReader(System.in));
			final String readLine = pr.readProperty();
			System.out.println();
			System.out.flush();
			return "y".equalsIgnoreCase(readLine);
		}
		// Shell is running in nonInteractive mode. we skip the question.
		return true;
	}
}
