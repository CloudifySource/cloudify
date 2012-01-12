package org.cloudifysource.shell.commands;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.installer.LocalhostGridAgentBootstrapper;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;

@Command(
		scope = "cloudify",
		name = "start-management",
		description = "Starts Cloudify Agent with management zone, and the Cloudify management processes on local machine. The management processes communicate with other agent and management machines.")
public class StartManagement extends AbstractGSCommand {

	@Option(
			required = false,
			name = "-lookup-groups",
			description = "A unique name that is used to group together different Cloudify machines. Default is based on the product version. Override in order to group together cloudify managements/agents on a network that supports multicast.")
	String lookupGroups = null;

	@Option(
			required = false,
			name = "-lookup-locators",
			description = "A list of ip addresses used to identify all management machines. Default is null. Override when using a network without multicast.")
	String lookupLocators = null;

	@Option(
			required = false,
			name = "-nic-address",
			description = "The ip address of the local host network card. Specify when local machine has more than one network adapter, and a specific network card should be used for network communication.")
	String nicAddress;

	@Option(required = false, name = "-no-web-services",
			description = "if set, no attempt to deploy the rest admin and web-ui will be made")
	boolean noWebServices;

	@Option(required = false, name = "-timeout",
			description = "The number of minutes to wait until the operation is done. By default waits 5 minutes.")
	int timeoutInMinutes = 5;

	@Option(
			required = false,
			name = "-auto-shutdown",
			description = "Determines if undeploying or scaling-in the last service instance on the machine also triggers agent shutdown. By default false.")
	boolean autoShutdown = false;

	@Option(required = false, name = "-no-management-space",
			description = "if set, no attempt to deploy the management space will be made")
	boolean noManagementSpace;

	@Option(required = false, name = "-cloud-file",
			description = "if set, designated the location of the cloud configuration file")
	String cloudFileName;

	
	
	private Cloud parseCloud(File cloudFile) {

		Cloud cloud = null;

		if (cloudFile != null) {
			if (cloudFile.isFile()) {
				try {
					
					cloud = ServiceReader.readCloud(cloudFile);
					
				} catch (IOException e) {
					
				} catch (DSLException e) {
					// fallback to default
				}
			} else {
				throw new IllegalArgumentException(cloudFile + " is not a file");
			}
		}
		return cloud;

	}

	@Override
	protected Object doExecute() throws Exception {

		if (timeoutInMinutes < 0) {
			throw new CLIException("-timeout cannot be negative");
		}

		boolean notHighlyAvailableManagementSpace = false;
		File cloudFile = null;
		
		String cloudConfigurationContents = null;
		if (cloudFileName != null && cloudFileName.trim().length() > 0) {
			cloudFile = new File(cloudFileName);
			cloudConfigurationContents  = FileUtils.readFileToString(cloudFile);
			Cloud cloud = parseCloud(cloudFile);
			if (cloud != null) {
				if (cloud.getProvider() != null) {
					int numberOfManagementMachines = cloud.getProvider().getNumberOfManagementMachines();
					notHighlyAvailableManagementSpace = numberOfManagementMachines < 2;
				}
			}

		}


		LocalhostGridAgentBootstrapper installer = new LocalhostGridAgentBootstrapper();
		installer.setVerbose(verbose);
		installer.setLookupGroups(lookupGroups);
		installer.setLookupLocators(lookupLocators);
		installer.setNicAddress(nicAddress);
		installer.setProgressInSeconds(10);
		installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		installer.setNoWebServices(noWebServices);
		installer.setNoManagementSpace(noManagementSpace);
		installer.setNotHighlyAvailableManagementSpace(notHighlyAvailableManagementSpace);
		installer.setAutoShutdown(autoShutdown);
		installer.setWaitForWebui(true);
		installer.setCloudContents(cloudConfigurationContents);
		installer.startManagementOnLocalhostAndWait(timeoutInMinutes, TimeUnit.MINUTES);
		return "Management started succesfully. Use the shutdown-management command to shutdown management processes running on local machine.";
	}
	
	public static void main(String[] args) {
		
	}
}