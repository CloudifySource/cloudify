package org.cloudifysource.shell.commands;

import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;



@Command(scope ="cloudify", name = "bootstrap-localcloud", description = "Starts Cloudify Agent without any zone, and the Cloudify management processes on local machine. These processes are isolated from Cloudify processes running on other machines.")
public class BootstrapLocalCloud extends AbstractGSCommand{
	
		@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together Cloudify components. The default localcloud lookup group is '" + LocalhostGridAgentBootstrapper.LOCALCLOUD_LOOKUPGROUP +"'. Override in order to start multiple local clouds on the local machine.")
		String lookupGroups;
	
		@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. Specify when local machine has more than one network adapter, and a specific network card should be used for network communication.")
		String nicAddress;
		
		@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. By default waits 5 minutes.")
		int timeoutInMinutes=5;
			
		@Override
		protected Object doExecute() throws Exception {
					    
		    if (timeoutInMinutes < 0) {
		        throw new CLIException("-timeout cannot be negative");
		    }
		    
			LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
			installer.setVerbose(verbose);
			installer.setLookupGroups(lookupGroups);
			installer.setNicAddress(nicAddress);
			installer.setProgressInSeconds(10);
			installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
			
			installer.startLocalCloudOnLocalhostAndWait(timeoutInMinutes, TimeUnit.MINUTES);
			return "Local-cloud started successfully. Use the teardown-localcloud command to shutdown all processes.";
		}
}
