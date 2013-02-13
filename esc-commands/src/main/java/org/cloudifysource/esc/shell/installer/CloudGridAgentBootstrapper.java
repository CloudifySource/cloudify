/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.shell.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.driver.provisioning.ManagementLocator;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.context.DefaultProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContextAware;
import org.cloudifysource.esc.driver.provisioning.jclouds.ManagementWebServiceInstaller;
import org.cloudifysource.esc.installer.AgentlessInstaller;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.installer.InstallerException;
import org.cloudifysource.esc.shell.listener.CliAgentlessInstallerListener;
import org.cloudifysource.esc.shell.listener.CliProvisioningDriverListener;
import org.cloudifysource.esc.util.CalcUtils;
import org.cloudifysource.esc.util.Utils;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.openspaces.admin.gsa.GSAReservationId;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.openspaces.admin.zone.config.ExactZonesConfigurer;

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

	private static final String OPERATION_TIMED_OUT = "The operation timed out. "
			+ "Try to increase the timeout using the -timeout flag";

	private static final Logger logger = Logger
			.getLogger(CloudGridAgentBootstrapper.class.getName());

	private File providerDirectory;

	private AdminFacade adminFacade;

	private boolean verbose;

	private boolean force;

	private int progressInSeconds;

	private ProvisioningDriver provisioning;

	private Cloud cloud;

	private File cloudFile;

	private boolean noWebServices;
	private boolean useExistingManagers;
	private File existingManagersFile;

	public void setProviderDirectory(final File providerDirectory) {
		this.providerDirectory = providerDirectory;
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

			logger.fine(nodePrefix(server)
					+ "Public IP: "
					+ (server.getPublicAddress() == null ? "" : server
							.getPublicAddress()));
			logger.fine(nodePrefix(server)
					+ "Private IP: "
					+ (server.getPrivateAddress() == null ? "" : server
							.getPrivateAddress()));

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
	 * @param securityProfile
	 *            set security profile (nonsecure/secure/ssl)
	 * @param username
	 *            The username for a secure connection to the server
	 * @param password
	 *            The password for a secure connection to the server
	 * @param keystorePassword
	 *            The password to the keystore to set on the rest server
	 * @param timeout
	 *            The number of {@link TimeUnit}s to wait before timing out
	 * @param timeoutUnit
	 *            The time unit to use (seconds, minutes etc.)
	 * @throws InstallerException
	 *             Indicates the provisioning driver failed to start management machines or that the management
	 *             processes failed to start
	 * @throws CLIException
	 *             Indicates a basic failure or a time out. a detailed message is included
	 * @throws InterruptedException
	 *             Indicates a thread was interrupted while waiting
	 */
	public void bootstrapCloudAndWait(final String securityProfile, final String username,
			final String password, final String keystorePassword, final long timeout, final TimeUnit timeoutUnit)
			throws InstallerException, CLIException, InterruptedException {

		final long end = System.currentTimeMillis()
				+ timeoutUnit.toMillis(timeout);

		createProvisioningDriver();

		// Start the cloud machines!!!
		MachineDetails[] servers;
		try {
			servers = provisioning.startManagementMachines(timeout, timeoutUnit);
		} catch (final CloudProvisioningException e) {
			final CLIStatusException cliStatusException =
					new CLIStatusException(e, CloudifyErrorMessages.CLOUD_API_ERROR.getName(), e.getMessage());
			throw cliStatusException;
		} catch (final TimeoutException e) {
			throw new CLIException("Cloudify bootstrap on provider "
					+ this.cloud.getProvider().getProvider() + " timed-out. "
					+ "Please try to run again using the –timeout option.", e);
		}

		// from this point on - close machines if an exception is thrown (to
		// avoid leaks).
		try {

			// log details in FINE
			if (logger.isLoggable(Level.FINE)) {
				for (final MachineDetails server : servers) {
					logServerDetails(server);
				}
			}

			validateServers(servers);

			// Start the management agents and other processes
			if (servers[0].isAgentRunning()) {
				// must be using existing machines.
				throw new IllegalStateException(
						"Cloud bootstrapper found existing management machines with the same name. "
								+ "Please shut them down before continuing");
			}

			startManagememntProcesses(servers, securityProfile, keystorePassword, end);

			if (!isNoWebServices()) {
				final Integer restPort = getRestPort(cloud.getConfiguration().getComponents().getRest().getPort(),
						ShellUtils.isSecureConnection(securityProfile));
				final Integer webuiPort = getWebuiPort(cloud.getConfiguration().getComponents().getWebui().getPort(),
						ShellUtils.isSecureConnection(securityProfile));
				waitForManagementWebServices(ShellUtils.isSecureConnection(securityProfile), username, password,
						restPort, webuiPort, end, servers);
			}

		} catch (final IOException e) {
			stopManagementMachines();
			throw new CLIException("Cloudify bootstrap on provider "
					+ this.cloud.getProvider().getProvider()
					+ " failed. Reason: " + e.getMessage(), e);
		} catch (final URISyntaxException e) {
			stopManagementMachines();
			throw new CLIException("Bootstrap-cloud failed. Reason: "
					+ e.getMessage(), e);
		} catch (final TimeoutException e) {
			stopManagementMachines();
			throw new CLIException("Cloudify bootstrap on provider "
					+ this.cloud.getProvider().getProvider() + " timed-out. "
					+ "Please try to run again using the –timeout option.", e);
		} catch (final CLIException e) {
			stopManagementMachines();
			throw e;
		} catch (final InstallerException e) {
			stopManagementMachines();
			throw e;
		} catch (final InterruptedException e) {
			stopManagementMachines();
			throw e;
		}
	}

	private MachineDetails[] getOrCreateManagementServers(final long timeout, final TimeUnit timeoutUnit)
			throws CLIException {

		if (this.existingManagersFile != null) {
			return locateManagementMachinesFromFile();
		} else if (this.useExistingManagers) {
			return locateManagementMachines();
		} else {
			return createManagementServers(timeout, timeoutUnit);
		}

	}

	private MachineDetails[] createManagementServers(final long timeout, final TimeUnit timeoutUnit)
			throws CLIException {
		MachineDetails[] servers;
		try {
			servers = provisioning.startManagementMachines(timeout, timeoutUnit);
		} catch (final CloudProvisioningException e) {
			final CLIStatusException cliStatusException =
					new CLIStatusException(e, CloudifyErrorMessages.CLOUD_API_ERROR.getName(), e.getMessage());
			throw cliStatusException;
		} catch (final TimeoutException e) {
			throw new CLIException("Cloudify bootstrap on provider "
					+ this.cloud.getProvider().getProvider() + " timed-out. "
					+ "Please try to run again using the –timeout option.", e);
		}

		if (servers.length == 0) {
			throw new IllegalArgumentException(
					"Received zero management servers from provisioning implementation");
		}
		return servers;
	}

	private MachineDetails[] locateManagementMachines() throws CLIStatusException {
		if (provisioning instanceof ManagementLocator) {
			final ManagementLocator locator = (ManagementLocator) provisioning;
			MachineDetails[] mds;
			try {
				mds = locator.getExistingManagementServers();
			} catch (final CloudProvisioningException e) {
				throw new CLIStatusException(e, CloudifyErrorMessages.MANAGEMENT_SERVERS_FAILED_TO_READ.getName(),
						e.getMessage());
			}
			if (mds.length == 0) {
				throw new CLIStatusException(CloudifyErrorMessages.MANAGEMENT_SERVERS_NOT_LOCATED.getName());
			}
			if (mds.length != this.cloud.getProvider().getNumberOfManagementMachines()) {
				throw new CLIStatusException(CloudifyErrorMessages.MANAGEMENT_SERVERS_NUMBER_NOT_MATCH.getName(),
						cloud.getProvider().getNumberOfManagementMachines(), mds.length);
			}

			return mds;
		} else {
			throw new CLIStatusException(CloudifyErrorMessages.MANAGEMENT_LOCATOR_NOT_SUPPORTED.getName(),
					this.cloud.getName());
		}
	}

	private MachineDetails[] locateManagementMachinesFromFile() throws CLIStatusException {
		if (provisioning instanceof ManagementLocator) {
			ObjectMapper mapper = new ObjectMapper();
			ControllerDetails[] controllers = null;
			try {
				controllers =
						mapper.readValue(this.existingManagersFile, TypeFactory.arrayType(ControllerDetails.class));
			} catch (IOException e) {
				throw new IllegalArgumentException("Failed to read managers file: "
						+ this.existingManagersFile.getAbsolutePath() + ". Error was: " + e.getMessage(), e);
			}
			final ManagementLocator locator = (ManagementLocator) provisioning;
			MachineDetails[] mds;
			try {
				mds = locator.getExistingManagementServers(controllers);
			} catch (final CloudProvisioningException e) {
				throw new CLIStatusException(e, CloudifyErrorMessages.MANAGEMENT_SERVERS_FAILED_TO_READ.getName(),
						e.getMessage());
			}
			if (mds.length == 0) {
				throw new CLIStatusException(CloudifyErrorMessages.MANAGEMENT_SERVERS_NOT_LOCATED.getName());
			}
			if (mds.length != this.cloud.getProvider().getNumberOfManagementMachines()) {
				throw new CLIStatusException(CloudifyErrorMessages.MANAGEMENT_SERVERS_NUMBER_NOT_MATCH.getName(),
						cloud.getProvider().getNumberOfManagementMachines(), mds.length);
			}

			return mds;
		} else {
			throw new CLIStatusException(CloudifyErrorMessages.MANAGEMENT_LOCATOR_NOT_SUPPORTED.getName(),
					this.cloud.getName());
		}
	}

	private void validateServers(final MachineDetails[] servers) throws CLIException {
		if (servers.length != this.cloud.getProvider().getNumberOfManagementMachines()) {
			throw new CLIException("Bootstrap required " + this.cloud.getProvider().getNumberOfManagementMachines()
					+ " machines, but recieved " + servers.length);
		}

		for (final MachineDetails machineDetails : servers) {
			if (this.cloud.getConfiguration().isBootstrapManagementOnPublicIp()) {
				if (machineDetails.getPublicAddress() == null) {
					throw new CLIException("Missing a public address which is required for bootstrap in node with ID: "
							+ machineDetails.getMachineId());
				}
			} else {
				if (machineDetails.getPrivateAddress() == null) {
					throw new CLIException(
							"Missing a private address which is required for bootstrap in node with ID: "
									+ machineDetails.getMachineId());
				}
			}

			if (this.cloud.getConfiguration().isConnectToPrivateIp()) {
				if (machineDetails.getPrivateAddress() == null) {
					throw new CLIException(
							"Missing a private address which is required for server setup in node with ID: "
									+ machineDetails.getMachineId());
				}
			} else {
				if (machineDetails.getPublicAddress() == null) {
					throw new CLIException(
							"Missing a public address which is required for server setup in node with ID: "
									+ machineDetails.getMachineId());
				}
			}
		}

	}

	private void waitForManagementWebServices(final boolean isSecureConnection, final String username,
			final String password, final Integer restPort, final Integer webuiPort,
			final long end, final MachineDetails[] servers)
			throws MalformedURLException, URISyntaxException,
			InterruptedException, TimeoutException, CLIException {
		// Wait for rest to become available
		// When the rest gateway is up and running, the cloud is ready to go
		for (final MachineDetails server : servers) {
			String ipAddress = null;
			if (cloud.getConfiguration().isBootstrapManagementOnPublicIp()) {
				ipAddress = server.getPublicAddress();
			} else {
				ipAddress = server.getPrivateAddress();
			}

			final URL restAdminUrl = new URI(ShellUtils.getRestProtocol(isSecureConnection), null, ipAddress,
					restPort, null, null, null).toURL();
			final URL webUIUrl = new URI(ShellUtils.getRestProtocol(isSecureConnection), null, ipAddress, webuiPort,
					null, null, null).toURL();

			// We are relying on start-management command to be run on the
			// new machine, so everything should be up if the rest admin is up
			waitForConnection(username, password, restAdminUrl, isSecureConnection, CalcUtils.millisUntil(end),
					TimeUnit.MILLISECONDS);

			logger.info("Rest service is available at: " + restAdminUrl + '.');
			logger.info("Webui service is available at: " + webUIUrl + '.');
		}
	}

	// if rest port was configured we return the config value
	private Integer getRestPort(final Integer configuredRestPort, final boolean isSecureConnection) {
		if (configuredRestPort != null) {
			return configuredRestPort;
		}
		if (isSecureConnection) {
			return CloudifyConstants.SECURE_REST_PORT;
		} else {
			return CloudifyConstants.DEFAULT_REST_PORT;
		}
	}

	// if webui port was configured we return the config value
	private Integer getWebuiPort(final Integer configuredWebuiPort, final boolean isSecureConnection) {
		if (configuredWebuiPort != null) {
			return configuredWebuiPort;
		}
		if (isSecureConnection) {
			return CloudifyConstants.SECURE_WEBUI_PORT;
		} else {
			return CloudifyConstants.DEFAULT_WEBUI_PORT;
		}
	}

	private void stopManagementMachines() {
		try {
			provisioning.stopManagementMachines();
		} catch (final CloudProvisioningException e) {
			// log a warning, don't throw an exception on this failure
			logger.warning("Failed to clean management machines after provisioning failure, reported error: "
					+ e.getMessage());
		} catch (final TimeoutException e) {
			// log a warning, don't throw an exception on this failure
			logger.warning("Failed to clean management machines after provisioning failure, the operation timed out ("
					+ e.getMessage() + ")");
		}
	}

	/**
	 * loads the provisioning driver class and sets it up.
	 *
	 * @throws CLIException
	 *             Indicates the configured could not be found and instantiated
	 */
	private void createProvisioningDriver() throws CLIException {
		try {
			provisioning = (ProvisioningDriver) Class.forName(
					cloud.getConfiguration().getClassName()).newInstance();
		} catch (final ClassNotFoundException e) {
			throw new CLIException(
					"Failed to load provisioning class for cloud: "
							+ cloud.getName() + ". Class not found: "
							+ cloud.getConfiguration().getClassName(), e);
		} catch (final Exception e) {
			throw new CLIException(
					"Failed to load provisioning class for cloud: "
							+ cloud.getName(), e);
		}
		if (provisioning instanceof ProvisioningDriverClassContextAware) {
			final ProvisioningDriverClassContextAware contextAware = (ProvisioningDriverClassContextAware) provisioning;
			contextAware
					.setProvisioningDriverClassContext(new DefaultProvisioningDriverClassContext());
		}

		provisioning.addListener(new CliProvisioningDriverListener());
		final String serviceName = null;
		provisioning.setConfig(cloud, cloud.getConfiguration()
				.getManagementMachineTemplate(), true, serviceName);
	}

	/**
	 *
	 * @param timeout
	 *            The number of {@link TimeUnit}s to wait before timing out
	 * @param timeoutUnit
	 *            The time unit to use (seconds, minutes etc.)
	 * @throws TimeoutException
	 *             Indicates the time out was reached before the tear-down completed
	 * @throws CLIException
	 *             Indicates a basic failure tear-down the cloud. a detailed message is included
	 * @throws InterruptedException
	 *             Indicates a thread was interrupted while waiting
	 */
	public void teardownCloudAndWait(final long timeout,
			final TimeUnit timeoutUnit) throws TimeoutException, CLIException,
			InterruptedException {

		final long end = System.currentTimeMillis()
				+ timeoutUnit.toMillis(timeout);

		createProvisioningDriver();

		ShellUtils.checkNotNull("providerDirectory", providerDirectory);

		destroyManagementServers(CalcUtils.millisUntil(end), TimeUnit.MILLISECONDS);

	}

	private void destroyManagementServers(final long timeout,
			final TimeUnit timeoutUnit) throws CLIException,
			InterruptedException, TimeoutException {

		final long end = System.currentTimeMillis()
				+ timeoutUnit.toMillis(timeout);

		if (!force) {

			if (!adminFacade.isConnected()) {
				throw new CLIException(
						"Please connect to the cloud before tearing down");
			}
			uninstallApplications(end);

		} else {

			if (adminFacade.isConnected()) {
				try {
					uninstallApplications(end);
				} catch (final InterruptedException e) {
					throw e;
				} catch (final TimeoutException e) {
					logger.fine("Failed to uninstall applications. Shut down of management machines will continue");
				} catch (final CLIException e) {
					logger.fine("Failed to uninstall applications. Shut down of management machines will continue");
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
			throw new CLIException(
					"Failed to shut down management machine during tear down of cloud: "
							+ e.getMessage(), e);
		}
		adminFacade.disconnect();

	}

	private void uninstallApplications(final long end) throws CLIException,
			InterruptedException, TimeoutException {
		final Collection<String> applicationsList = adminFacade
				.getApplicationNamesList();

		final long startTime = System.currentTimeMillis();
		final long millisToEnd = end - startTime;
		final int minutesToEnd = (int) TimeUnit.MILLISECONDS
				.toMinutes(millisToEnd);

		final Map<String, String> lifeCycleEventContainersIdsByApplicationName = new HashMap<String, String>();

		if (applicationsList.size() > 0) {
			logger.info("Uninstalling the currently deployed applications");
			for (final String application : applicationsList) {
				if (!application.equals(MANAGEMENT_APPLICATION)) {
					final Map<String, String> uninstallApplicationResponse =
							adminFacade.uninstallApplication(application, minutesToEnd);
					lifeCycleEventContainersIdsByApplicationName.put(
							uninstallApplicationResponse.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID),
							application);
				}
			}
		}

		// now we need to wait for all the application to be uninstalled
		for (final Map.Entry<String, String> entry : lifeCycleEventContainersIdsByApplicationName.entrySet()) {
			logger.info("Waiting for application " + entry.getValue() + " to uninstall.");
			adminFacade.waitForLifecycleEvents(entry.getKey(), minutesToEnd, CloudifyConstants.TIMEOUT_ERROR_MESSAGE);
		}
	}

	private MachineDetails[] startManagememntProcesses(final MachineDetails[] machines, final String securityProfile,
			final String keystorePassword, final long endTime) throws InterruptedException, TimeoutException,
			InstallerException, IOException {

		final AgentlessInstaller installer = new AgentlessInstaller();
		installer.addListener(new CliAgentlessInstallerListener(this.verbose));

		// Update the logging level of jsch used by the AgentlessInstaller
		Logger.getLogger(AgentlessInstaller.SSH_LOGGER_NAME).setLevel(
				Level.parse(cloud.getProvider().getSshLoggingLevel()));

		final ComputeTemplate template = cloud.getCloudCompute().getTemplates().get(
				cloud.getConfiguration().getManagementMachineTemplate());

		// fixConfigRelativePaths(cloud, template);

		final int numOfManagementMachines = machines.length;

		final InstallationDetails[] installations = createInstallationDetails(numOfManagementMachines, machines,
				template, securityProfile, keystorePassword);
		// only one machine should try and deploy the WebUI and Rest Admin unless
		// noWebServices is true
		int i = isNoWebServices() ? 0 : 1;
		for (; i < installations.length; i++) {
			installations[i].setNoWebServices(true);
		}

		final String lookup = createLocatorsString(installations);
		for (final InstallationDetails detail : installations) {
			detail.setLocator(lookup);
		}

		// executes the agentless installer on all of the machines,
		// asynchronously
		installOnMachines(endTime, installer, numOfManagementMachines,
				installations);

		return machines;

	}

	private void installOnMachines(final long endTime,
			final AgentlessInstaller installer,
			final int numOfManagementMachines,
			final InstallationDetails[] installations)
			throws InterruptedException, TimeoutException, InstallerException {
		final ExecutorService executors = Executors
				.newFixedThreadPool(numOfManagementMachines);

		final BootstrapLogsFilters bootstrapLogs = new BootstrapLogsFilters(verbose);
		try {

			bootstrapLogs.applyLogFilters();

			final List<Future<Exception>> futures = new ArrayList<Future<Exception>>();

			for (final InstallationDetails detail : installations) {
				final Future<Exception> future = executors
						.submit(new Callable<Exception>() {

							@Override
							public Exception call() {
								try {
									installer.installOnMachineWithIP(detail,
											CalcUtils.millisUntil(endTime),
											TimeUnit.MILLISECONDS);
								} catch (final TimeoutException e) {
									logger.log(
											Level.INFO,
											"Failed accessing management VM "
													+ detail.getPublicIp()
													+ " Reason: "
													+ e.getMessage(), e);
									return e;
								} catch (final InterruptedException e) {
									logger.log(
											Level.INFO,
											"Failed accessing management VM "
													+ detail.getPublicIp()
													+ " Reason: "
													+ e.getMessage(), e);
									return e;
								} catch (final InstallerException e) {
									logger.log(
											Level.INFO,
											"Failed accessing management VM "
													+ detail.getPublicIp()
													+ " Reason: "
													+ e.getMessage(), e);
									return e;
								}
								return null;
							}
						});
				futures.add(future);

			}

			for (final Future<Exception> future : futures) {
				try {
					final Exception e = future.get(CalcUtils.millisUntil(endTime),
							TimeUnit.MILLISECONDS);
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
						throw new InstallerException(
								"Failed creating machines.", e);
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

	private String createLocatorsString(
			final InstallationDetails[] installations) {

		final Integer port = cloud.getConfiguration().getComponents().getDiscovery().getDiscoveryPort();
		final StringBuilder lookupSb = new StringBuilder();
		for (final InstallationDetails detail : installations) {
			final String ip = cloud.getConfiguration().isConnectToPrivateIp() ? detail
					.getPrivateIp() : detail.getPublicIp();

			lookupSb.append(ip).append(":").append(port).append(',');
		}

		lookupSb.setLength(lookupSb.length() - 1);

		return lookupSb.toString();
	}

	private InstallationDetails[] createInstallationDetails(final int numOfManagementMachines,
			final MachineDetails[] machineDetails, final ComputeTemplate template, final String securityProfile,
			final String keystorePassword) throws FileNotFoundException {
		final InstallationDetails[] details = new InstallationDetails[numOfManagementMachines];

		final GSAReservationId reservationId = null;
		final String managementAuthGroups = null;

		for (int i = 0; i < details.length; i++) {
			final ExactZonesConfig zones = new ExactZonesConfigurer().addZone(
					MANAGEMENT_GSA_ZONE).create();
			details[i] = Utils.createInstallationDetails(machineDetails[i], cloud, template, zones, null, null, true,
					this.cloudFile, reservationId, cloud.getConfiguration().getManagementMachineTemplate(),
					securityProfile, keystorePassword, managementAuthGroups);
		}

		return details;
	}

	/**
	 * Waits for a connection to be established with the service. If the timeout is reached before a connection could be
	 * established, a {@link TimeoutException} is thrown.
	 *
	 * @param username
	 *            The username for a secure connection to the rest server
	 * @param password
	 *            The password for a secure connection to the rest server
	 * @param restAdminUrl
	 *            The URL of the service
	 * @param isSecureConnection
	 *            Is this a secure connection (SSL)
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            The {@link TimeUnit} to use
	 * @throws InterruptedException
	 *             Reporting the thread is interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the time out was reached
	 * @throws CLIException
	 *             Reporting different errors while creating the connection to the service
	 */
	private void waitForConnection(final String username, final String password, final URL restAdminUrl,
			final boolean isSecureConnection, final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {

		adminFacade.disconnect();

		createConditionLatch(timeout, timeunit).waitFor(
				new ConditionLatch.Predicate() {

					@Override
					public boolean isDone() throws CLIException,
							InterruptedException {

						try {
							adminFacade.connect(username, password, restAdminUrl.toString(), isSecureConnection);
							return true;
						} catch (final CLIException e) {
							if (verbose) {
								logger.log(Level.INFO,
										"Error connecting to rest service.", e);
							}
						}
						logger.log(Level.INFO, "Connecting to rest service.");
						return false;
					}
				});
	}

	private ConditionLatch createConditionLatch(final long timeout,
			final TimeUnit timeunit) {
		return new ConditionLatch().timeout(timeout, timeunit)
				.pollingInterval(progressInSeconds, TimeUnit.SECONDS)
				.timeoutErrorMessage(OPERATION_TIMED_OUT).verbose(verbose);
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;

	}

	public void setCloudFile(final File cloudFile) {
		this.cloudFile = cloudFile;
	}

	public boolean isNoWebServices() {
		return noWebServices;
	}

	public void setNoWebServices(final boolean noWebServices) {
		this.noWebServices = noWebServices;
	}

	public void setUseExisting(final boolean useExistingManagers) {
		this.useExistingManagers = useExistingManagers;

	}

	/******
	 * Returns existing cloud managers.
	 *
	 * @return details of existing cloud managers.
	 * @throws CLIException
	 *             if failed to read cloudify managers from Cloud API.
	 */
	public MachineDetails[] getCloudManagers() throws CLIException {
		createProvisioningDriver();
		return locateManagementMachines();

	}

	public void setExistingManagersFile(final File existingManagersFile) {
		this.existingManagersFile = existingManagersFile;

	}
}
