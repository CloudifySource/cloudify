package com.gigaspaces.cloudify.esc.shell.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import com.gigaspaces.cloudify.dsl.cloud.Cloud2;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.esc.driver.provisioning.jclouds.DefaultCloudProvisioning;
import com.gigaspaces.cloudify.esc.installer.AgentlessInstaller;
import com.gigaspaces.cloudify.esc.shell.installer.CloudGridAgentBootstrapper;
import com.gigaspaces.cloudify.shell.AdminFacade;
import com.gigaspaces.cloudify.shell.Constants;
import com.gigaspaces.cloudify.shell.ShellUtils;
import com.gigaspaces.cloudify.shell.commands.AbstractGSCommand;
import com.gigaspaces.cloudify.shell.rest.RestAdminFacade;

@Command(
		scope = "cloudify",
		name = "bootstrap-cloud",
		description = "Starts Cloudify Agent without any zone, and the Cloudify management processes on the provided cloud.")
public class BootstrapCloud extends AbstractGSCommand {

	@Argument(required = true, name = "provider", description = "the cloud prodiver to use")
	String cloudProvider;

	@Option(required = false, name = "-timeout",
			description = "The number of minutes to wait until the operation is done. By default waits 5 minutes.")
	int timeoutInMinutes = 60;

	
	private static final String[] NON_VERBOSE_LOGGERS = { DefaultCloudProvisioning.class.getName(), AgentlessInstaller.class.getName() };
	private Map<String, Level> loggerStates = new HashMap<String, Level>();

	
	@Override
	protected Object doExecute() throws Exception {
		String pathSeparator = System.getProperty("file.separator");
		File providerDirectory = new File(ShellUtils.getCliDirectory(), "plugins" + pathSeparator + "esc"
				+ pathSeparator + cloudProvider);

		// load the cloud file
		File cloudFile = findCloudFile(providerDirectory);
		Cloud2 cloud = ServiceReader.readCloud(cloudFile);

		// start the installer
		CloudGridAgentBootstrapper installer = new CloudGridAgentBootstrapper();
		installer.setProviderDirectory(providerDirectory);
		if (this.adminFacade != null) {
			installer.setAdminFacade(this.adminFacade);
		} else {
			installer.setAdminFacade((AdminFacade) session.get(Constants.ADMIN_FACADE));
		}
		installer.setProgressInSeconds(10);
		installer.setVerbose(verbose);
		installer.setCloud(cloud);
		installer.setCloudFile(cloudFile);

		// Bootstrap!

		// Note: The cloud driver may be very verbose. This is EXTEREMELY useful
		// when debugging ESM
		// issues, but can also clutter up the CLI display. It makes more sense to temporarily raise the log level here,
		// so that all of these
		// messages will not be displayed on the console.
		limitLoggingLevel();
		logger.info(getFormattedMessage("bootstrapping_cloud"));
		try {
			
			installer.boostrapCloudAndWait(timeoutInMinutes, TimeUnit.MINUTES);
			return getFormattedMessage("cloud_started_successfully", cloudProvider);
		} finally {
			restoreLoggingLevel();
		}

	}


	private void limitLoggingLevel() {

		if (!this.verbose) {
			loggerStates.clear();
			for (String loggerName : NON_VERBOSE_LOGGERS) {
				final Logger provisioningLogger = Logger.getLogger(loggerName);
				final Level logLevelBefore = provisioningLogger.getLevel();
				provisioningLogger.setLevel(Level.WARNING);
				loggerStates.put(loggerName, logLevelBefore);
			}
		}
	}

	private void restoreLoggingLevel() {
		if (!verbose) {
			Set<Entry<String, Level>> entries = loggerStates.entrySet();
			for (Entry<String, Level> entry : entries) {
				Logger provisioningLogger = Logger.getLogger(entry.getKey());
				provisioningLogger.setLevel(entry.getValue());
			}			
		}

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
		BootstrapCloud cmd = new BootstrapCloud();
		cmd.cloudProvider = "ec2";
		cmd.verbose = true;
		cmd.adminFacade = new RestAdminFacade();
		cmd.execute(null);
	}
}
