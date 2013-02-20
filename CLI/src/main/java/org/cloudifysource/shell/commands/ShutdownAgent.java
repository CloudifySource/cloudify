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

import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        Shuts down the agent running on the local machine.
 * 
 *        Optional arguments: lookup-groups - A unique name that is used to group together Cloudify components. Override
 *        in order to start multiple local clouds on the local machine. nic-address - The ip address of the local host
 *        network card. Specify when local machine has more than one network adapter, and a specific network card should
 *        be used for network communication. timeout - The number of minutes to wait until the operation is completed
 *        (default: 5 minutes) lookup-locators - A list of IP addresses used to identify all management machines.
 *        Override when using a network without multicast support (Default: null). force - When specified, the agent
 *        shuts down even when running service instances on local machine.
 * 
 *        Command syntax: shutdown-agent [-lookup-groups lookup-groups] [-nicAddress nicAddress] [-timeout timeout]
 *        [-lookup-locators lookup-locators] [-force force]
 */
@Command(scope = "cloudify", name = "shutdown-agent", description = "For internal use only! "
		+ "Shuts down the agent running on the local machine.")
public class ShutdownAgent extends AbstractGSCommand {

	@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together"
			+ " Cloudify components. Default is 'local-cloud'. Override in order to start multiple local clouds on"
			+ " the local machine.")
	private String lookupGroups;

	@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. "
			+ "Specify when local machine has more than one network adapter, and a specific network card should be"
			+ " used for network communication.")
	private String nicAddress;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is"
			+ " done. By default waits 5 minutes.")
	private int timeoutInMinutes = 5;

	@Option(required = false, name = "-lookup-locators", description = "A list of ip addresses used to identify all"
			+ " management machines. Default is null. Override when using a network without multicast.")
	private String lookupLocators = null;

	@Option(required = false, name = "-force", description = "When specified, the agent shuts down even when running"
			+ " service instances on local machine.")
	private boolean force;

	/**
	 * Shuts down the local agent, if exists, and waits until shutdown is complete or until the timeout is reached. If
	 * management processes (GSM, ESM, LUS) are still active, the agent is not shutdown and a CLIException is thrown.
	 * 
	 * @return .
	 * @throws Exception .
	 */
	@Override
	protected Object doExecute()
			throws Exception {

		final LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
		installer.setVerbose(verbose);
		installer.setLookupGroups(lookupGroups);
		installer.setLookupLocators(lookupLocators);
		installer.setNicAddress(nicAddress);
		installer.setProgressInSeconds(10);
		installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));

		installer.shutdownAgentOnLocalhostAndWait(force, timeoutInMinutes, TimeUnit.MINUTES);
		return getFormattedMessage("agent_terminated_successfully");
	}
}
