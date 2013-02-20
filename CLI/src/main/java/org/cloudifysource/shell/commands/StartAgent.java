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
 *        Starts Cloudify Agent with the specified zone.
 * 
 *        Required arguments: zone - The agent zone that specifies the name of the service that can run on the machine
 * 
 *        Optional arguments: lookup-groups - A unique name that is used to group together Cloudify components. Override
 *        in order to group together cloudify managements/agents on a network that supports multicast. nic-address - The
 *        ip address of the local host network card. Specify when local machine has more than one network adapter, and a
 *        specific network card should be used for network communication. timeout - The number of minutes to wait until
 *        the operation is completed (default: 5 minutes) lookup-locators - A list of IP addresses used to identify all
 *        management machines. Override when using a network without multicast support (Default: null). auto-shutdown -
 *        etermines if undeploying or scaling-in the last service instance on the machine also triggers agent shutdown
 *        (default: false).
 * 
 *        Command syntax: start-agent -zone zone [-lookup-groups lookup-groups] [-nicAddress nicAddress] [-timeout
 *        timeout] [-lookup-locators lookup-locators] [-auto-shutdown auto-shutdown]
 */
@Command(scope = "cloudify", name = "start-agent", description = "For internal use only! Starts Cloudify Agent with "
		+ "the specified zone. The agent communicates with other agent and management machines.")
public class StartAgent extends AbstractGSCommand {

	private static final int DEFAULT_POLLING_INTERVAL = 10;

	private static final int DEFAULT_TIMEOUT_MINUTES = 5;

	@Option(required = false, name = "-zone", description = "The grid service agent zone")
	private String zone;

	@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together"
			+ " different Cloudify machines. Default is based on the product version. Override in order to group"
			+ " together cloudify managements/agents on a network that supports multicast.")
	private String lookupGroups = null;

	@Option(required = false, name = "-lookup-locators", description = "A list of ip addresses used to identify all"
			+ " management machines. Default is null. Override when using a network without multicast.")
	private String lookupLocators = null;

	@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card."
			+ " Specify when local machine has more than one network adapter, and a specific network card should be"
			+ " used for network communication.")
	private String nicAddress;

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is"
			+ " done. By default waits 5 minutes.")
	private int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	@Option(required = false, name = "-auto-shutdown", description = "Determines if undeploying or scaling-in the last"
			+ " service instance on the machine also triggers agent shutdown. By default false.")
	private boolean autoShutdown = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws Exception {

		if (timeoutInMinutes < 0) {
			throw new CLIException("-timeout cannot be negative");
		}

		final LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
		installer.setVerbose(verbose);
		installer.setLookupGroups(lookupGroups);
		installer.setLookupLocators(lookupLocators);
		installer.setNicAddress(nicAddress);
		installer.setProgressInSeconds(DEFAULT_POLLING_INTERVAL);
		installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		installer.setGridServiceAgentZone(zone);
		installer.setAutoShutdown(autoShutdown);

		installer.startAgentOnLocalhostAndWait("" /*securityProfile*/, "" /*keystorePassword*/, timeoutInMinutes,
				TimeUnit.MINUTES);
		return "Agent started succesfully. Use the shutdown-agent command to shutdown agent running on local machine.";
	}
}