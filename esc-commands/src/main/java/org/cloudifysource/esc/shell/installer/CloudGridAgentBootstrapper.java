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
package org.cloudifysource.esc.shell.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.dsl.cloud.Cloud;

import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.installer.ManagementWebServiceInstaller;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.util.Utils;
import com.gigaspaces.internal.utils.StringUtils;
import com.j_spaces.kernel.Environment;

public class CloudGridAgentBootstrapper {

	private static final String MANAGEMENT_APPLICATION = ManagementWebServiceInstaller.MANAGEMENT_APPLICATION_NAME;

	private static final int WEBUI_PORT = 8099;

	private static final int REST_GATEWAY_PORT = 8100;

	private static final String OPERATION_TIMED_OUT = "Operation timed out";

	private static final Logger logger = Logger.getLogger(CloudGridAgentBootstrapper.class.getName());

	private File providerDirecotry;

	private AdminFacade adminFacade;

	private boolean verbose;

	private boolean force;

	private int progressInSeconds;

	private ProvisioningDriver provisioning;

	private Cloud cloud;

	private File cloudFile;

	public void setProviderDirectory(File providerDirecotry) {
		this.providerDirecotry = providerDirecotry;
	}

	public void setAdminFacade(AdminFacade adminFacade) {
		this.adminFacade = adminFacade;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setProgressInSeconds(int progressInSeconds) {
		this.progressInSeconds = progressInSeconds;
	}

	public void setForce(boolean force) {
		this.force = force;
	}
	
	private static String nodePrefix(MachineDetails node) {
		return "[" + node.getMachineId() + "] ";
	}

	private static void logServerDetails(MachineDetails server) {
		if (logger.isLoggable(Level.INFO)) {
			logger.info(nodePrefix(server) + "Cloud Server was created.");

			logger.info(nodePrefix(server) + "Public IP: " + (server.getPublicAddress() == null ? "" : server.getPublicAddress()));
			logger.info(nodePrefix(server) + "Private IP: " + (server.getPrivateAddress() == null?"":server.getPrivateAddress()));
			
			
		}
	}


	public void boostrapCloudAndWait(long timeout, TimeUnit timeoutUnit) throws InstallerException, TimeoutException,
			CLIException, InterruptedException {

		// load the provisioning class and set it up
		try {
			this.provisioning = (ProvisioningDriver) Class.forName(this.cloud.getConfiguration().getClassName()).newInstance();
			provisioning.setConfig(cloud, cloud.getConfiguration()
					.getManagementMachineTemplate(), true);
		} catch (Exception e) {
			throw new CLIException("Failed to load provisioning class from cloud: " + this.cloud);
		}

		
		long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);

		try {
			// Start the cloud machines!!!
			MachineDetails[] servers;
			try {
				servers = provisioning.startManagementMachines(timeout, timeoutUnit);
			} catch (CloudProvisioningException e) {
				throw new InstallerException("Failed to start managememnt servers", e);
			}

			if (servers.length == 0) {
				throw new IllegalArgumentException("Received zero management servers from provisioning implementation");
			}
			
			if(logger.isLoggable(Level.INFO)) {
				for (MachineDetails server: servers) {
					logServerDetails(server);
				}
			}
			
			// Start the management agents and other processes
			if (servers[0].isAgentRunning()) {
				// must be using existing machines.
				
				// TODO - check if management machines are running properly. If so - use them, like connect.
				throw new IllegalStateException("Cloud bootstrapper found existing management machines with the same name. Please shut them down before continuing");
				

			} else {
				startManagememntProcesses(servers, end);
			}

			
			// Wait for rest to become available
			// When the rest gateway is up and running, the cloud is ready to go
			for (MachineDetails server : servers) {
				String ipAddress =null;
				if(cloud.getConfiguration().isBootstrapManagementOnPublicIp()) {
					ipAddress = server.getPublicAddress();
				} else {
					ipAddress = server.getPrivateAddress();
				}

				URL restAdminUrl = new URI("http", null, ipAddress, REST_GATEWAY_PORT, null, null, null).toURL();
				URL webUIUrl = new URI("http", null, ipAddress, WEBUI_PORT, null, null, null).toURL();

				// We are relying on start-management command to be run on the
				// new machine, so
				// everything should be up if the rest admin is up
				waitForConnection(restAdminUrl, Utils.millisUntil(end), TimeUnit.MILLISECONDS);

				logger.info("Rest service is available at: " + restAdminUrl);
				logger.info("Webui service is available at: " + webUIUrl);
			}

		} catch (IOException e) {
			throw new CLIException("bootstrap-cloud failed", e);
		} catch (URISyntaxException e) {
			throw new CLIException("bootstrap-cloud failed", e);
		}

	}

	public void teardownCloudAndWait(long timeout, TimeUnit timeoutUnit) throws InstallerException, TimeoutException,
			CLIException, InterruptedException {

		// load the provisioning class and set it up
		try {
			this.provisioning = (ProvisioningDriver) Class.forName(cloud.getConfiguration().getClassName()).newInstance();
		} catch (Exception e) {
			throw new CLIException("Failed to load provisioning class from cloud: " + this.cloud);
		}
		provisioning.setConfig(cloud, cloud.getConfiguration().getManagementMachineTemplate(), true);
		
		long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);

		ShellUtils.checkNotNull("providerDirectory", providerDirecotry);

		destroyManagementServers(Utils.millisUntil(end), TimeUnit.MILLISECONDS);

	}

	private void destroyManagementServers(long timeout, TimeUnit timeoutUnit) throws CLIException,
			InterruptedException, TimeoutException {

		long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);



		if (!force) {
			
			if (!adminFacade.isConnected()) {
				throw new CLIException("Please connect to the cloud before tearing down");
			}

			for (String application : adminFacade.getApplicationsList()) {
				if (!application.equals(MANAGEMENT_APPLICATION)) {
					adminFacade.uninstallApplication(application);
				}
			}

			waitForUninstallApplications(Utils.millisUntil(end), TimeUnit.MILLISECONDS);

		}

		logger.info("Terminating cloud machines");

		try {
			provisioning.stopManagementMachines();
		} catch (CloudProvisioningException e) {
			throw new CLIException("Failed to shut down management machine during tear down of cloud: "
					+ e.getMessage(), e);
		}

	}

	protected static InstallationDetails createInstallationDetails(final Cloud cloud) throws FileNotFoundException {
		final InstallationDetails details = new InstallationDetails();
		details.setLocalDir(cloud.getProvider().getLocalDirectory());
		details.setZones(StringUtils.join(cloud.getProvider().getZones().toArray(new String[0]), ",", 0, cloud
				.getProvider().getZones().size()));
		details.setRemoteDir(cloud.getProvider().getRemoteDirectory());
		details.setLocator(null);
		details.setPrivateIp(null);
		details.setLus(true);
		details.setCloudifyUrl(cloud.getProvider().getCloudifyUrl());
		details.setConnectedToPrivateIp(cloud.getConfiguration().isConnectToPrivateIp());
		details.setUsername(cloud.getUser().getUser());
		
		//if ((cloud.getUser().getKeyPair() != null) && (cloud.getUser().getKeyPair().length() > 0)) {
		if(cloud.getUser().getKeyFile() != null && cloud.getUser().getKeyFile().length() > 0) {
			File keyFile = new File(cloud.getUser().getKeyFile());
			if (!keyFile.isAbsolute()) {
				keyFile = new File(details.getLocalDir(), cloud.getUser().getKeyFile());
			}
			if (!keyFile.isFile()) {
				throw new FileNotFoundException("keyfile : " + keyFile.getAbsolutePath() + " not found");
			}
			details.setKeyFile(keyFile.getAbsolutePath());
		}
		//}
		return details;
	}

	private MachineDetails[] startManagememntProcesses(MachineDetails[] machines, final long endTime)
			throws InterruptedException, TimeoutException, InstallerException, IOException {

		final AgentlessInstaller installer = new AgentlessInstaller();

		// Update the logging level of jsch used by the AgentlessInstaller
		Logger.getLogger(AgentlessInstaller.SSH_LOGGER_NAME).setLevel(
				Level.parse(cloud.getProvider().getSshLoggingLevel()));

		fixConfigRelativePaths(cloud);

		final int numOfManagementMachines = machines.length;

		InstallationDetails[] installations = createInstallationDetails(numOfManagementMachines, machines);
		// only one machine should try and deploy the WebUI and Rest Admin
		for (int i = 1; i < installations.length; i++) {
			installations[i].setNoWebServices(true);
		}

		String lookup = createLocatorsString(installations);
		for (InstallationDetails detail : installations) {
			detail.setLocator(lookup);
		}

		// copy cloud file to local upload directory so that cloud settings
		// will be available when rest gateway is deployed
		// TODO - this is a bit of a hack. Makes more sense to add the option for the installer
		// to copy additional files to the target. 
		File uploadDir = new File(cloud.getProvider().getLocalDirectory());
		try {
			FileUtils.copyFileToDirectory(this.cloudFile, uploadDir);
			// executes the agentless installer on all of the machines,
			// asynchronously
			installOnMachines(endTime, installer, numOfManagementMachines, installations);
		} finally {
			
			FileUtils.forceDelete(new File(uploadDir, this.cloudFile.getName()));
		}
		return machines;

	}

	private void installOnMachines(final long endTime, final AgentlessInstaller installer,
			final int numOfManagementMachines, InstallationDetails[] installations) throws InterruptedException,
			TimeoutException, InstallerException {
		ExecutorService executors = Executors.newFixedThreadPool(numOfManagementMachines);

		BootstrapLogsFilters bootstrapLogs = new BootstrapLogsFilters(verbose);
		try {

			bootstrapLogs.applyLogFilters();

			List<Future<Exception>> futures = new ArrayList<Future<Exception>>();

			for (final InstallationDetails detail : installations) {
				Future<Exception> future = executors.submit(new Callable<Exception>() {

					@Override
					public Exception call() {
						try {
							installer.installOnMachineWithIP(detail, Utils.millisUntil(endTime), TimeUnit.MILLISECONDS);
						} catch (TimeoutException e) {
							return e;
						} catch (InterruptedException e) {
							return e;
						} catch (InstallerException e) {
							return e;
						}
						return null;
					}
				});
				futures.add(future);

			}

			for (Future<Exception> future : futures) {
				try {
					Exception e = future.get();
					if (e != null) {
						if (e instanceof TimeoutException) {
							throw (TimeoutException) e;
						}
						if (e instanceof InterruptedException) {
							throw (InterruptedException) e;
						}
						if (e instanceof InstallerException) {
							throw (InstallerException) e;
						}
						throw new InstallerException("Failed creating machines", e);
					}
				} catch (ExecutionException e) {
					throw new InstallerException("Failed creating machines", e);
				}
			}

		} finally {
			executors.shutdown();
			bootstrapLogs.restoreLogFilters();
		}
	}

	private String createLocatorsString(InstallationDetails[] installations) {
		StringBuilder lookupSb = new StringBuilder();
		for (InstallationDetails detail : installations) {
			final String ip = (cloud.getConfiguration().isConnectToPrivateIp() ? detail.getPrivateIp() : detail
					.getPublicIp());
			lookupSb.append(ip).append(",");
		}

		lookupSb.setLength(lookupSb.length() - 1);

		return lookupSb.toString();
	}

	private InstallationDetails[] createInstallationDetails(final int numOfManagementMachines,
			MachineDetails[] machineDetails) throws FileNotFoundException {
		InstallationDetails template = createInstallationDetails(cloud);

		InstallationDetails[] details = new InstallationDetails[numOfManagementMachines];
		for (int i = 0; i < details.length; i++) {
			MachineDetails machine = machineDetails[i];
			InstallationDetails installationDetails = template.clone();
			installationDetails.setUsername(machine.getRemoteUsername());
			installationDetails.setPassword(machine.getRemotePassword());
			installationDetails.setPrivateIp(machine.getPrivateAddress());
			installationDetails.setPublicIp(machine.getPublicAddress());
			// Bootstrapping is usually done from a different network
			installationDetails.setConnectedToPrivateIp(!cloud.getConfiguration().isBootstrapManagementOnPublicIp());
			installationDetails.setCloudFile(this.cloudFile);
			details[i] = installationDetails;
		}
		return details;
	}

	
	private void fixConfigRelativePaths(Cloud config) {
		if (config.getProvider().getLocalDirectory() != null
				&& !new File(config.getProvider().getLocalDirectory()).isAbsolute()) {
			logger.info("Assuming " + config.getProvider().getLocalDirectory() + " is in "
					+ Environment.getHomeDirectory());
			config.getProvider().setLocalDirectory(
					new File(Environment.getHomeDirectory(), config.getProvider().getLocalDirectory())
							.getAbsolutePath());
		}
	}

	
	private void waitForUninstallApplications(final long timeout, final TimeUnit timeunit) throws InterruptedException,
			TimeoutException, CLIException {
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				List<String> applications = adminFacade.getApplicationsList();

				boolean done = true;

				for (String application : applications) {
					if (!MANAGEMENT_APPLICATION.equals(application)) {
						done = false;
						break;
					}
				}

				if (!done) {
					logger.info("Waiting for all applications to uninstall");
				}

				return done;
			}
		});
	}

	private void waitForConnection(final URL restAdminUrl, final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {

		adminFacade.disconnect();

		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone() throws CLIException, InterruptedException {

				try {
					adminFacade.connect(null, null, restAdminUrl.toString());
					return true;
				} catch (CLIException e) {
					if (verbose) {
						logger.log(Level.INFO, "Error connecting to rest service.", e);
					}
				}
				logger.log(Level.INFO, "Connecting to rest service.");
				return false;
			}
		});
	}

	private ConditionLatch createConditionLatch(long timeout, TimeUnit timeunit) {
		return new ConditionLatch().timeout(timeout, timeunit).pollingInterval(progressInSeconds, TimeUnit.SECONDS)
				.timeoutErrorMessage(OPERATION_TIMED_OUT).verbose(verbose);
	}

	

	public void setCloud(Cloud cloud) {
		this.cloud = cloud;

	}

	public void setCloudFile(File cloudFile) {
		this.cloudFile = cloudFile;
	}

}
