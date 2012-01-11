package org.cloudifysource.shell.commands;

import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;



@Command(scope ="cloudify", name = "shutdown-management", description = "Shuts down the agent running on the local machine.")
public class ShutdownManagement extends AbstractGSCommand{
	
		@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together Cloudify components. Default is 'local-cloud'. Override in order to start multiple local clouds on the local machine.")
		String lookupGroups;
	
		@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. Specify when local machine has more than one network adapter, and a specific network card should be used for network communication.")
		String nicAddress;
		
		@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. By default waits 5 minutes.")
		int timeoutInMinutes=5;
		
		@Option(required = false, name = "-lookup-locators", description = "A list of ip addresses used to identify all management machines. Default is null. Override when using a network without multicast.")
		String lookupLocators = null;
		
		@Override
		protected Object doExecute() throws Exception {
			
			LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
			installer.setVerbose(verbose);
			installer.setLookupGroups(lookupGroups);
			installer.setLookupLocators(lookupLocators);
			installer.setNicAddress(nicAddress);
			installer.setProgressInSeconds(10);
			installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
			
			installer.shutdownManagementOnLocalhostAndWait(timeoutInMinutes, TimeUnit.MINUTES);
			return "Completed management shutdown.";
		}
}
