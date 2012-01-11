package org.cloudifysource.shell.commands;

import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;



@Command(scope ="cloudify", name = "start-agent", description = "Starts Cloudify Agent with the specified zone. The agent communicates with other agent and management machines.")
public class StartAgent extends AbstractGSCommand{
	
		@Option(required = true, name = "-zone", description = "The agent zone that specifies the name of the service that can run on the machine.")
		String zone;
	
		@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together different Cloudify machines. Default is based on the product version. Override in order to group together cloudify managements/agents on a network that supports multicast.")
		String lookupGroups = null;
		
		@Option(required = false, name = "-lookup-locators", description = "A list of ip addresses used to identify all management machines. Default is null. Override when using a network without multicast.")
		String lookupLocators = null;
	
		@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. Specify when local machine has more than one network adapter, and a specific network card should be used for network communication.")
		String nicAddress;
		
		@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. By default waits 5 minutes.")
		int timeoutInMinutes=5;
			
		@Option(required = false, name = "-auto-shutdown", description = "Determines if undeploying or scaling-in the last service instance on the machine also triggers agent shutdown. By default false.")
		boolean autoShutdown = false;
		
		@Override
		protected Object doExecute() throws Exception {
					    
		    if (timeoutInMinutes < 0) {
		        throw new CLIException("-timeout cannot be negative");
		    }
		    
			LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
			installer.setVerbose(verbose);
			installer.setLookupGroups(lookupGroups);
			installer.setLookupLocators(lookupLocators);
			installer.setNicAddress(nicAddress);
			installer.setProgressInSeconds(10);
			installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
			installer.setZone(zone);
			installer.setAutoShutdown(autoShutdown);
			
			installer.startAgentOnLocalhostAndWait(timeoutInMinutes, TimeUnit.MINUTES);
			return "Agent started succesfully. Use the shutdown-agent command to shutdown agent running on local machine.";
		}
}