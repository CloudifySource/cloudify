package com.gigaspaces.cloudify.shell.commands;

import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import com.gigaspaces.cloudify.shell.AdminFacade;
import com.gigaspaces.cloudify.shell.Constants;
import com.gigaspaces.cloudify.shell.installer.LocalhostGridAgentBootstrapper;


@Command(scope ="cloudify", name = "teardown-localcloud", description = "Tears down the Local Cloud installed on the local machine.")
public class TeardownLocalCloud extends AbstractGSCommand{
	
		@Option(required = false, name = "-lookup-groups", description = "A unique name that is used to group together Cloudify components. The default localcloud lookup group is '" + LocalhostGridAgentBootstrapper.LOCALCLOUD_LOOKUPGROUP +"'. Override in order to teardown a specific local cloud running on the local machine.")
		String lookupGroups;
	
		@Option(required = false, name = "-nic-address", description = "The ip address of the local host network card. Specify when local machine has more than one network adapter, and a specific network card should be used for network communication.")
		String nicAddress;
		
		@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is done. By default waits 5 minutes.")
		int timeoutInMinutes=5;
			
		@Override
		protected Object doExecute() throws Exception {
			
			LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
			installer.setVerbose(verbose);
			installer.setLookupGroups(lookupGroups);
			installer.setNicAddress(nicAddress);
			installer.setProgressInSeconds(10);
			installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
			
			installer.teardownLocalCloudOnLocalhostAndWait(timeoutInMinutes, TimeUnit.MINUTES);
			return "Completed local-cloud teardown";
		}
}
