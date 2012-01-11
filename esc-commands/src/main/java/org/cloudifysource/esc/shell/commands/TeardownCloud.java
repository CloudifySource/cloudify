package org.cloudifysource.esc.shell.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.cloud.Cloud2;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.esc.shell.installer.CloudGridAgentBootstrapper;

import org.cloudifysource.esc.driver.provisioning.jclouds.DefaultCloudProvisioning;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.AbstractGSCommand;
import org.cloudifysource.shell.rest.RestAdminFacade;

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

		// Note: The cloud driver may be very verbose. This is EXTEREMELY useful
		// when debugging ESM
		// issues, but can also clutter up the CLI display. It makes more sense
		// to temporarily raise the log level here,
		// so that all of these
		// messages will not be displayed on the console.
		limitLoggingLevel();
		try {
			installer.teardownCloudAndWait(timeoutInMinutes, TimeUnit.MINUTES);
			return getFormattedMessage("cloud_terminated_successfully", cloudProvider);
		} finally {
			restoreLoggingLevel();
		}

		



	}

	private static final String[] NON_VERBOSE_LOGGERS = { DefaultCloudProvisioning.class.getName(), AgentlessInstaller.class.getName() };
	private Map<String, Level> loggerStates = new HashMap<String, Level>();

	private void limitLoggingLevel() {

		if (!this.verbose) {
			loggerStates.clear();
			for (String loggerName : NON_VERBOSE_LOGGERS) {
				Logger provisioningLogger = Logger.getLogger(loggerName);
				Level logLevelBefore = provisioningLogger.getLevel();
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
		TeardownCloud cmd = new TeardownCloud();
		cmd.cloudProvider = "ec2";
		cmd.verbose = true;
		cmd.adminFacade = new RestAdminFacade();
		cmd.adminFacade.connect(null, null, "http://107.20.37.95:8100/");
		cmd.execute(null);
	}

}
