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
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.context.DefaultProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.driver.provisioning.jclouds.ManagementWebServiceInstaller;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.shell.installer.BootstrapLogsFilters;
import org.cloudifysource.esc.shell.listener.CliAgentlessInstallerListener;
import org.cloudifysource.esc.shell.listener.CliProvisioningDriverListener;
import org.cloudifysource.esc.util.Utils;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIException;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.openspaces.admin.zone.config.ExactZonesConfigurer;

import com.j_spaces.kernel.Environment;

/**
 * This class handles the bootstrapping of machines, activation of management processes and cloud tear-down.
 * 
 * @author barakm, adaml
 * @since 2.0.0
 * 
 */
public class CloudGridAgentBootstrapper {

	private static final String MANAGEMENT_APPLICATION = ManagementWebServiceInstaller.MANAGEMENT_APPLICATION_NAME;
	private static final String MANAGEMENT_GSA_ZONE = "management";
	
	private static final int WEBUI_PORT = 8099;

	private static final int REST_GATEWAY_PORT = 8100;

	private static final String OPERATION_TIMED_OUT = "The operation timed out. " 
				+ "Try to increase the timeout using the -timeout flag";

	private static final Logger logger = Logger.getLogger(CloudGridAgentBootstrapper.class.getName());

	private File providerDirecotry;

	private AdminFacade adminFacade;

	private boolean verbose;

	private boolean force;

	private int progressInSeconds;

	private ProvisioningDriver provisioning;

	private Cloud cloud;

	private File cloudFile;

	public void setProviderDirectory(final File providerDirecotry) {
		this.providerDirecotry = providerDirecotry;
	}

	public void setAdminFacade(final AdminFacade adminFacade) {
		this.adminFacade = adminFacade;
	}

	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	public void setProgressInSeconds(final int progressInSeconds) {
		this.progressInSeconds = progressInSeconds;
	}

	public void setForce(final boolean force) {
		this.force = force;
	}

	private static String nodePrefix(final MachineDetails node) {
		return "[" + node.getMachineId() + "] ";
	}

	private static void logServerDetails(final MachineDetails server) {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine(nodePrefix(server) + "Cloud Server was created.");

			logger.fine(nodePrefix(server) + "Public IP: "
					+ (server.getPublicAddress() == null ? "" : server.getPublicAddress()));
			logger.fine(nodePrefix(server) + "Private IP: "
					+ (server.getPrivateAddress() == null ? "" : server.getPrivateAddress()));

		}
	}

	/**
	 * Closes the provisioning driver.
	 */
	public void close() {
		if (this.provisioning != null) {
			this.provisioning.close();
		}
	}

	/**
	 * Bootstraps and waits until the management machines are running, or until the timeout is reached.
	 * 
	 * @param timeout The number of {@link TimeUnit}s to wait before timing out
	 * @param timeoutUnit The time unit to use (seconds, minutes etc.)
	 * @throws InstallerException Indicates the provisioning driver failed to start management machines or that the
	 *         management processes failed to start
	 * @throws CLIException Indicates a basic failure or a time out. a detailed message is included
	 * @throws InterruptedException Indicates a thread was interrupted while waiting
	 */
	public void boostrapCloudAndWait(final long timeout, final TimeUnit timeoutUnit)
			throws InstallerException,
			CLIException, InterruptedException {

		final long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);

		createProvisioningDriver();

			// Start the cloud machines!!!
			MachineDetails[] servers;
			try {
				servers = provisioning.startManagementMachines(timeout, timeoutUnit);
			} catch (final CloudProvisioningException e) {
				throw new InstallerException("Failed to start managememnt servers. Reason: " + e.getMessage(), e);
			} catch (final TimeoutException e) {
				throw new CLIException("Cloudify bootstrap on provider " + this.cloud.getProvider().getProvider()
						+ " timed-out. " + "Please try to run again using the –timeout option.", e);
			}

			if (servers.length == 0) {
				throw new IllegalArgumentException("Received zero management servers from provisioning implementation");
			}

			//from this point on - close machines if an exception is thrown (to avoid leaks).
			try {
				if (logger.isLoggable(Level.INFO)) {
					for (final MachineDetails server : servers) {
						logServerDetails(server);
					}
				}

				// Start the management agents and other processes
				if (servers[0].isAgentRunning()) {
					// must be using existing machines.
					// TODO - check if management machines are running properly. If so - use them, like connect.
					throw new IllegalStateException(
						"Cloud bootstrapper found existing management machines with the same name. "
								+ "Please shut them down before continuing");
				}
			
				startManagememntProcesses(servers, end);

				// Wait for rest to become available
				// When the rest gateway is up and running, the cloud is ready to go
				for (final MachineDetails server : servers) {
					String ipAddress = null;
					if (cloud.getConfiguration().isBootstrapManagementOnPublicIp()) {
						ipAddress = server.getPublicAddress();
					} else {
						ipAddress = server.getPrivateAddress();
					}

					final URL restAdminUrl = new URI("http", null, ipAddress, REST_GATEWAY_PORT, null, null, null)
						.toURL();
					final URL webUIUrl = new URI("http", null, ipAddress, WEBUI_PORT, null, null, null).toURL();

					// We are relying on start-management command to be run on the
					// new machine, so everything should be up if the rest admin is up
					waitForConnection(restAdminUrl, Utils.millisUntil(end), TimeUnit.MILLISECONDS);

					logger.info("Rest service is available at: " + restAdminUrl + '.');
					logger.info("Webui service is available at: " + webUIUrl + '.');
				}
			} catch (final IOException e) {
				stopManagementMachines();
				throw new CLIException("Cloudify bootstrap on provider " + this.cloud.getProvider().getProvider()
						+ " failed. Reason: " + e.getMessage(), e);
			} catch (final URISyntaxException e) {
				stopManagementMachines();
				throw new CLIException("Bootstrap-cloud failed. Reason: " + e.getMessage(), e);
			} catch (final TimeoutException e) {
				stopManagementMachines();
				throw new CLIException("Cloudify bootstrap on provider " + this.cloud.getProvider().getProvider()
						+ " timed-out. " + "Please try to run again using the –timeout option.", e);
			} catch (CLIException e) {
				stopManagementMachines();
				throw e;
			} catch (InstallerException e) {
				stopManagementMachines();
				throw e;
			} catch (InterruptedException e) {
				stopManagementMachines();
				throw e;
			}
	}
	
	private void stopManagementMachines() {
		try {
			provisioning.stopManagementMachines();
		} catch (CloudProvisioningException e) {
			//log a warning, don't throw an exception on this failure
			logger.warning("Failed to clean management machines after provisioning failure, reported error: " + e.getMessage());
		} catch (TimeoutException e) {
			//log a warning, don't throw an exception on this failure
			logger.warning("Failed to clean management machines after provisioning failure, the operation timed out (" + e.getMessage() + ")");
		}
	}

	/**
	 * loads the provisioning driver class and sets it up.
	 * 
	 * @throws CLIException Indicates the configured could not be found and instantiated
	 */
	private void createProvisioningDriver()
			throws CLIException {
		try {
			provisioning = (ProvisioningDriver) Class.forName(cloud.getConfiguration().getClassName())
					.newInstance();
		} catch (final ClassNotFoundException e) {
			throw new CLIException("Failed to load provisioning class for cloud: " + cloud.getName()
					+ ". Class not found: " + cloud.getConfiguration().getClassName(), e);
		} catch (final Exception e) {
			throw new CLIException("Failed to load provisioning class for cloud: " + cloud.getName(), e);
		}
		if (provisioning instanceof ProvisioningDriverClassContextAware) {
			final ProvisioningDriverClassContextAware contextAware = (ProvisioningDriverClassContextAware) provisioning;
			contextAware.setProvisioningDriverClassContext(new DefaultProvisioningDriverClassContext());
		}

		provisioning.addListener(new CliProvisioningDriverListener());
		provisioning.setConfig(cloud, cloud.getConfiguration().getManagementMachineTemplate(), true);
	}

	/**
	 * 
	 * @param timeout The number of {@link TimeUnit}s to wait before timing out
	 * @param timeoutUnit The time unit to use (seconds, minutes etc.)
	 * @throws TimeoutException Indicates the time out was reached before the tear-down completed
	 * @throws CLIException Indicates a basic failure tear-down the cloud. a detailed message is included
	 * @throws InterruptedException Indicates a thread was interrupted while waiting
	 */
	public void teardownCloudAndWait(final long timeout, final TimeUnit timeoutUnit)
			throws TimeoutException,
			CLIException, InterruptedException {

		final long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);

		createProvisioningDriver();

		ShellUtils.checkNotNull("providerDirectory", providerDirecotry);

		destroyManagementServers(Utils.millisUntil(end), TimeUnit.MILLISECONDS);

	}

	private void destroyManagementServers(final long timeout, final TimeUnit timeoutUnit)
			throws CLIException,
			InterruptedException, TimeoutException {

		final long end = System.currentTimeMillis() + timeoutUnit.toMillis(timeout);

		if (!force) {

			if (!adminFacade.isConnected()) {
				throw new CLIException("Please connect to the cloud before tearing down");
			}
			uninstallApplications(end);

		} else {

			if (adminFacade.isConnected()) {
				try {
					uninstallApplications(end);
				} catch (final InterruptedException e) {
					throw e;
				} catch (final TimeoutException e) {
					logger.fine("Failed to uninstall applications. Shut down of managememnt machines will continue");
				} catch (final CLIException e) {
					logger.fine("Failed to uninstall applications. Shut down of managememnt machines will continue");
				}
			} else {
				logger.info("Teardown performed without connection to the cloud, only management machines will be "
						+ "terminated.");
			}

		}

		logger.info("Terminating cloud machines");

		try {
			provisioning.stopManagementMachines();
		} catch (final CloudProvisioningException e) {
			throw new CLIException("Failed to shut down management machine during tear down of cloud: "
					+ e.getMessage(), e);
		}
		adminFacade.disconnect();

	}

	private void uninstallApplications(final long end)
			throws CLIException, InterruptedException, TimeoutException {
		List<String> applicationsList = adminFacade.getApplicationsList();
		if (applicationsList.size() > 0){
			logger.info("Uninstalling the currently deployed applications");
			for (final String application : applicationsList) {
				if (!application.equals(MANAGEMENT_APPLICATION)) {
					adminFacade.uninstallApplication(application, (int) end);
				}
			}
		}

		waitForUninstallApplications(Utils.millisUntil(end), TimeUnit.MILLISECONDS);
	}

	private MachineDetails[] startManagememntProcesses(final MachineDetails[] machines, final long endTime)
			throws InterruptedException, TimeoutException, InstallerException, IOException {

		final AgentlessInstaller installer = new AgentlessInstaller();
		installer.addListener(new CliAgentlessInstallerListener(this.verbose));

		// Update the logging level of jsch used by the AgentlessInstaller
		Logger.getLogger(AgentlessInstaller.SSH_LOGGER_NAME).setLevel(
				Level.parse(cloud.getProvider().getSshLoggingLevel()));

		final CloudTemplate template = cloud.getTemplates().get(cloud.getConfiguration().getManagementMachineTemplate());
		
		fixConfigRelativePaths(cloud, template);

		final int numOfManagementMachines = machines.length;

		
		
		final InstallationDetails[] installations = createInstallationDetails(numOfManagementMachines, machines, template);
		// only one machine should try and deploy the WebUI and Rest Admin
		for (int i = 1; i < installations.length; i++) {
			installations[i].setNoWebServices(true);
		}

		final String lookup = createLocatorsString(installations);
		for (final InstallationDetails detail : installations) {
			detail.setLocator(lookup);
		}
		
		

		// copy cloud file to local upload directory so that cloud settings
		// will be available when rest gateway is deployed
		// TODO - this is a bit of a hack. Makes more sense to add the option for the installer
		// to copy additional files to the target.
		final File uploadDir = new File(template.getLocalDirectory());
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
			final int numOfManagementMachines, final InstallationDetails[] installations)
			throws InterruptedException,
			TimeoutException, InstallerException {
		final ExecutorService executors = Executors.newFixedThreadPool(numOfManagementMachines);

		final BootstrapLogsFilters bootstrapLogs = new BootstrapLogsFilters(verbose);
		try {

			bootstrapLogs.applyLogFilters();

			final List<Future<Exception>> futures = new ArrayList<Future<Exception>>();

			for (final InstallationDetails detail : installations) {
				final Future<Exception> future = executors.submit(new Callable<Exception>() {

					@Override
					public Exception call() {
						try {
							installer.installOnMachineWithIP(detail, Utils.millisUntil(endTime), TimeUnit.MILLISECONDS);
						} catch (final TimeoutException e) {
							logger.log(Level.INFO, "Failed accessing management VM " + detail.getPublicIp()
									+ " Reason: " + e.getMessage(), e);
							return e;
						} catch (final InterruptedException e) {
							logger.log(Level.INFO, "Failed accessing management VM " + detail.getPublicIp()
									+ " Reason: " + e.getMessage(), e);
							return e;
						} catch (final InstallerException e) {
							logger.log(Level.INFO, "Failed accessing management VM " + detail.getPublicIp()
									+ " Reason: " + e.getMessage(), e);
							return e;
						}
						return null;
					}
				});
				futures.add(future);

			}

			for (final Future<Exception> future : futures) {
				try {
					final Exception e = future.get(Utils.millisUntil(endTime), TimeUnit.MILLISECONDS);
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
						throw new InstallerException("Failed creating machines.", e);
					}
				} catch (final ExecutionException e) {
					throw new InstallerException("Failed creating machines.", e);
				}
			}

		} finally {
			executors.shutdown();
			bootstrapLogs.restoreLogFilters();
		}
	}

	private String createLocatorsString(final InstallationDetails[] installations) {
		final StringBuilder lookupSb = new StringBuilder();
		for (final InstallationDetails detail : installations) {
			final String ip = cloud.getConfiguration().isConnectToPrivateIp() ? detail.getPrivateIp() : detail
					.getPublicIp();
			lookupSb.append(ip).append(',');
		}

		lookupSb.setLength(lookupSb.length() - 1);

		return lookupSb.toString();
	}

	// TODO: This code should be places in a Util package somewhere. It is used both
	// here and in the esc project, for starting new agent machines.
	private InstallationDetails[] createInstallationDetails(final int numOfManagementMachines,
			final MachineDetails[] machineDetails, final CloudTemplate template)
			throws FileNotFoundException {
		final InstallationDetails[] details = new InstallationDetails[numOfManagementMachines];

		

		for (int i = 0; i < details.length; i++) {
			ExactZonesConfig zones = new ExactZonesConfigurer().addZone(MANAGEMENT_GSA_ZONE).create();
			details[i] = Utils.createInstallationDetails(machineDetails[i], cloud,
					template, zones, null, null, true, this.cloudFile);
		}

		return details;
		// final InstallationDetails template = createInstallationDetails(cloud);
		//
		// for (int i = 0; i < details.length; i++) {
		// final MachineDetails machine = machineDetails[i];
		// final InstallationDetails installationDetails = template.clone();
		// installationDetails.setUsername(machine.getRemoteUsername());
		// installationDetails.setPassword(machine.getRemotePassword());
		// installationDetails.setPrivateIp(machine.getPrivateAddress());
		// installationDetails.setPublicIp(machine.getPublicAddress());
		// // Bootstrapping is usually done from a different network
		// installationDetails.setConnectedToPrivateIp(!cloud.getConfiguration().isBootstrapManagementOnPublicIp());
		// installationDetails.setBindToPrivateIp(cloud.getConfiguration().isConnectToPrivateIp());
		// installationDetails.setCloudFile(this.cloudFile);
		// installationDetails.setRemoteExecutionMode(machine.getRemoteExecutionMode());
		// installationDetails.setFileTransferMode(machine.getFileTransferMode());
		// details[i] = installationDetails;
		// }
		// return details;
	}

	private void fixConfigRelativePaths(final Cloud config, final CloudTemplate template) {
		if (template.getLocalDirectory() != null
				&& !new File(template.getLocalDirectory()).isAbsolute()) {
			logger.fine("Assuming " + template.getLocalDirectory() + " is in "
					+ Environment.getHomeDirectory());
			template.setLocalDirectory(
					new File(Environment.getHomeDirectory(), template.getLocalDirectory())
							.getAbsolutePath());
		}
	}

	private void waitForUninstallApplications(final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone()
					throws CLIException, InterruptedException {
				final List<String> applications = adminFacade.getApplicationsList();

				boolean done = true;

				for (final String application : applications) {
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
			public boolean isDone()
					throws CLIException, InterruptedException {

				try {
					adminFacade.connect(null, null, restAdminUrl.toString());
					return true;
				} catch (final CLIException e) {
					if (verbose) {
						logger.log(Level.INFO, "Error connecting to rest service.", e);
					}
				}
				logger.log(Level.INFO, "Connecting to rest service.");
				return false;
			}
		});
	}

	private ConditionLatch createConditionLatch(final long timeout, final TimeUnit timeunit) {
		return new ConditionLatch().timeout(timeout, timeunit).pollingInterval(progressInSeconds, TimeUnit.SECONDS)
				.timeoutErrorMessage(OPERATION_TIMED_OUT).verbose(verbose);
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;

	}

	public void setCloudFile(final File cloudFile) {
		this.cloudFile = cloudFile;
	}

}
