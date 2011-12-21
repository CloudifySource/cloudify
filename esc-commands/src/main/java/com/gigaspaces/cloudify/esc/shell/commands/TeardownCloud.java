package com.gigaspaces.cloudify.esc.shell.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import com.gigaspaces.cloudify.dsl.cloud.Cloud2;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.esc.shell.installer.CloudGridAgentBootstrapper;
import com.gigaspaces.cloudify.shell.AdminFacade;
import com.gigaspaces.cloudify.shell.Constants;
import com.gigaspaces.cloudify.shell.ShellUtils;
import com.gigaspaces.cloudify.shell.commands.AbstractGSCommand;
import com.gigaspaces.cloudify.shell.rest.RestAdminFacade;

@Command(scope = "cloudify", name = "teardown-cloud", description = "Terminates management machines.")
public class TeardownCloud extends AbstractGSCommand {

	@Argument(required = true, name = "provider", description = "the cloud prodiver to use")
	String cloudProvider;

	@Option(required = false, name = "-timeout",
			description = "The number of minutes to wait until the operation is done. By default waits 5 minutes.")
	int timeoutInMinutes = 60;

	@Option(required = false, name = "-force",
			description = "Should management machine be shutdown if other applications are installed")
	boolean force = false;

	@Override
	protected Object doExecute() throws Exception {

		CloudGridAgentBootstrapper installer = new CloudGridAgentBootstrapper();

		// TODO use DSL
		String pathSeparator = System.getProperty("file.separator");
		File providerDirectory = new File(ShellUtils.getCliDirectory(), "plugins" + pathSeparator + "esc"
				+ pathSeparator + cloudProvider);

		// load the cloud file
		File cloudFile = findCloudFile(providerDirectory);
		Cloud2 cloud = ServiceReader.readCloud(cloudFile);



		installer.setProviderDirectory(providerDirectory);
		if (this.adminFacade != null) {
			installer.setAdminFacade(this.adminFacade);
		} else {
			installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		}

		installer.setProgressInSeconds(10);
		installer.setVerbose(verbose);
		installer.setForce(force);
		installer.setCloud(cloud);
		installer.teardownCloudAndWait(timeoutInMinutes, TimeUnit.MINUTES);

		return getFormattedMessage("cloud_terminated_successfully", cloudProvider);

	}

	private File findCloudFile(File providerDirectory) throws FileNotFoundException {
		if (!providerDirectory.exists() || !providerDirectory.isDirectory()) {
			throw new FileNotFoundException("Could not find cloud provider directory: " + providerDirectory);
		}

		File[] cloudFiles = providerDirectory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("-cloud.groovy");
			}

		});

		if (cloudFiles.length == 0) {
			throw new FileNotFoundException("Could not find a cloud definition file in: " + providerDirectory
					+ ". Definitions file must end with the suffix '-cloud.groovy'");
		} else if (cloudFiles.length > 1) {
			throw new IllegalArgumentException("Found multiple cloud definition files in: " + providerDirectory
					+ ". Only one file may end with the suffix '-cloud.groovy'");
		}

		File cloudFile = cloudFiles[0];
		return cloudFile;
	}

	public static void main(String[] args) throws Exception {
		TeardownCloud cmd = new TeardownCloud();
		cmd.cloudProvider = "ec2";
		cmd.verbose = true;
		cmd.adminFacade = new RestAdminFacade();
		cmd.adminFacade.connect(null, null, "http://107.20.37.95:8100/");
		cmd.execute(null);
	}

}
