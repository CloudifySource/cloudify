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
package org.cloudifysource.shell.installer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.discovery.LookupLocator;
import net.jini.discovery.Constants;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.context.IsLocalCloudUtils;
import org.cloudifysource.dsl.internal.packaging.CloudConfigurationHolder;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.EnvironmentUtils;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.AgentGridComponent;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.gsa.InternalGridServiceAgent;
import org.openspaces.admin.internal.support.NetworkExceptionHelper;
import org.openspaces.admin.lus.LookupService;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.vm.VirtualMachineAware;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.util.MemoryUnit;

import com.gigaspaces.grid.gsa.GSA;
import com.gigaspaces.internal.client.spaceproxy.ISpaceProxy;
import com.gigaspaces.internal.utils.StringUtils;
import com.j_spaces.kernel.Environment;

/**
 * @author rafi, barakm, adaml, noak
 * @since 2.0.0
 * 
 *        This class handles the start up and shut down of the cloud components
 *        - management components (LUS, GSM, ESM), containers (GSCs) and an
 *        agent.
 */
public class LocalhostGridAgentBootstrapper {

	private static final String GSM_EXCLUDE_GSC_ON_FAILED_INSTACE_BOOL = "true";
	private static final String GSM_PENDING_REQUESTS_DELAY = "-Dorg.jini.rio.monitor.pendingRequestDelay=1000";

	private static final int MIN_PROC_ERROR_TIME = 2000;
	// isolate localcloud from default lookup settings
	/**
	 * Default localcloud lookup group.
	 */
	public static final String LOCALCLOUD_LOOKUPGROUP = "localcloud";

	private static final String MANAGEMENT_APPLICATION = ManagementWebServiceInstaller.MANAGEMENT_APPLICATION_NAME;
	private static final String GSM_EXCLUDE_GSC_ON_FAILED_INSTANCE = "gsm.excludeGscOnFailedInstance.disabled";
	private static final String ZONES_PROPERTY = "com.gs.zones";
	private static final String AUTO_SHUTDOWN_COMMANDLINE_ARGUMENT = "-Dcom.gs.agent.auto-shutdown-enabled=true";
	private static final int WAIT_AFTER_ADMIN_CLOSED_MILLIS = 10 * 1000;
	private static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out waiting for the agent to start."
			+ " Configure the timeout using the -timeout flag.";
	private static final String REST_FILE = "tools" + File.separator + "rest" + File.separator + "rest.war";
	private static final String REST_NAME = "rest";
	private static final String WEBUI_FILE = EnvironmentUtils.findWebuiWar();
	private static final String WEBUI_NAME = "webui";
	private static final String MANAGEMENT_SPACE_NAME = CloudifyConstants.MANAGEMENT_SPACE_NAME;

	private static final String LINUX_SCRIPT_PREFIX = "#!/bin/bash\n";
	private static final String MANAGEMENT_ZONE = "management";
	private static final String LOCALCLOUD_GSA_ZONES = MANAGEMENT_ZONE + ",localcloud";
	private static final long WAIT_EXISTING_AGENT_TIMEOUT_SECONDS = 10;

	// management agent starts 1 global esm, 1 gsm,1 lus
	private static final String[] CLOUD_MANAGEMENT_ARGUMENTS = new String[] { "gsa.global.lus", "0", "gsa.lus", "1",
			"gsa.gsc", "0", "gsa.global.gsm", "0", "gsa.gsm", "1", "gsa.global.esm", "1" };

	// localcloud management agent starts 1 esm, 1 gsm,1 lus
	private static final String[] LOCALCLOUD_WIN_MANAGEMENT_ARGUMENTS = new String[] { "start", "startLH", "startGSM",
			"startESM", "startGSA", "gsa.global.lus", "0", "gsa.lus", "0", "gsa.gsc", "0", "gsa.global.gsm", "0",
			"gsa.gsm_lus", "0", "gsa.global.esm", "0", "gsa.esm", "0" };
	// localcloud management agent starts 1 esm, 1 gsm,1 lus
	private static final String[] LOCALCLOUD_LINUX_MANAGEMENT_ARGUMENTS = new String[] { "start",
			"\"com.gigaspaces.start.services=\\\"LH,GSM,GSA,ESM\\\"\"", "gsa.global.lus", "0", "gsa.lus", "0",
			"gsa.gsc", "0", "gsa.global.gsm", "0", "gsa.gsm_lus", "0", "gsa.global.esm", "0", "gsa.esm", "0" };

	private static final String[] AGENT_ARGUMENTS = new String[] { "gsa.global.lus", "0", "gsa.gsc", "0",
			"gsa.global.gsm", "0", "gsa.global.esm", "0" };

	// script must spawn a daemon process (that is not a child process)
	private static final String[] WINDOWS_LOCALCLOUD_COMMAND = new String[] { "cmd.exe", "/c", "@call", "\"gs.bat\"" };
	private static final String[] LINUX_LOCALCLOUD_COMMAND = new String[] { "gs.sh" };

	// script must spawn a daemon process (that is not a child process)
	private static final String[] WINDOWS_CLOUD_COMMAND = new String[] { "cmd.exe", "/c", "gs-agent.bat" };
	private static final String[] LINUX_CLOUD_COMMAND = new String[] { "gs-agent.sh" };

	// script must suppress output, since this process is not consuming it and
	// so any output could block it.
	private static final String[] WINDOWS_ARGUMENTS_POSTFIX = new String[] { ">nul", "2>&1" };

	private static final String[] LINUX_ARGUMENTS_POSTFIX = new String[] { ">/dev/null", "2>&1" };

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private boolean verbose;
	private String lookupGroups;
	private String lookupLocators;
	private String nicAddress;
	private String gsaZones;
	private int progressInSeconds;
	private AdminFacade adminFacade;
	private boolean noWebServices;
	private boolean noManagementSpace;
	private boolean notHighlyAvailableManagementSpace;
	private int lusPort = CloudifyConstants.DEFAULT_LUS_PORT;
	private boolean autoShutdown;
	private boolean waitForWebUi;

	private String cloudFilePath;
	private boolean force;
	private final List<LocalhostBootstrapperListener> eventsListenersList =
			new ArrayList<LocalhostBootstrapperListener>();
	private boolean isLocalCloud;

	/**
	 * Sets verbose mode.
	 * 
	 * @param verbose
	 *            mode (true - on, false - off)
	 */
	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Sets the lookup groups.
	 * 
	 * @param lookupGroups
	 *            lookup groups
	 */
	public void setLookupGroups(final String lookupGroups) {
		this.lookupGroups = lookupGroups;
	}

	/**
	 * Sets the lookup locators.
	 * 
	 * @param lookupLocators
	 *            lookup locators
	 */
	public void setLookupLocators(final String lookupLocators) {
		this.lookupLocators = lookupLocators;
	}

	/**
	 * Sets the nic address.
	 * 
	 * @param nicAddress
	 *            nic address
	 */
	public void setNicAddress(final String nicAddress) {
		this.nicAddress = nicAddress;
	}

	/**
	 * Sets the zone.
	 * 
	 * @param zone
	 *            Zone name
	 */
	public void setGridServiceAgentZone(final String zone) {
		this.gsaZones = zone;
	}

	/**
	 * Sets the number of minutes between each progress check.
	 * 
	 * @param progressInSeconds
	 *            number of seconds
	 */
	public void setProgressInSeconds(final int progressInSeconds) {
		this.progressInSeconds = progressInSeconds;
	}

	/**
	 * Sets the admin facade to work with.
	 * 
	 * @param adminFacade
	 *            Admin facade object
	 */
	public void setAdminFacade(final AdminFacade adminFacade) {
		this.adminFacade = adminFacade;
	}

	/**
	 * Sets web services limitation mode (i.e. activation of webui and REST).
	 * 
	 * @param noWebServices
	 *            web services limitation mode (true - not active, false -
	 *            active web services)
	 */
	public void setNoWebServices(final boolean noWebServices) {
		this.noWebServices = noWebServices;
	}

	/**
	 * Sets management space limitation mode.
	 * 
	 * @param noManagementSpace
	 *            noManagementSpace limitation mode (true - management space
	 *            will not be installed, false - it will be installed)
	 */
	public void setNoManagementSpace(final boolean noManagementSpace) {
		this.noManagementSpace = noManagementSpace;
	}

	/**
	 * Sets automatic shutdown on the agent.
	 * 
	 * @param autoShutdown
	 *            automatic shutdown mode (true - on, false - off)
	 */
	public void setAutoShutdown(final boolean autoShutdown) {
		this.autoShutdown = autoShutdown;
	}

	/**
	 * Sets whether to wait for the web UI installation to complete when
	 * starting management components.
	 * 
	 * @param waitForWebui
	 *            waitForWebui mode (true - wait, false - return without
	 *            waiting)
	 */
	public void setWaitForWebui(final boolean waitForWebui) {
		this.waitForWebUi = waitForWebui;
	}

	/**
	 * Sets the availability mode of the space - if a backup space is required
	 * for the space to become available.
	 * 
	 * @param notHighlyAvailableManagementSpace
	 *            high-availability mode (true - the space will be available
	 *            without a backup space, false - a backup space is required)
	 */
	public void setNotHighlyAvailableManagementSpace(final boolean notHighlyAvailableManagementSpace) {
		this.notHighlyAvailableManagementSpace = notHighlyAvailableManagementSpace;
	}

	/**
	 * Gets the availability mode of the space.
	 * 
	 * @return high-availability mode (true - the space is available when a
	 *         single instance is ready, false - a backup space is required for
	 *         the space to become available).
	 */
	public boolean isNotHighlyAvailableManagementSpace() {
		return notHighlyAvailableManagementSpace;
	}

	/**
	 * Enables force teardown. The force flag will terminate the gs agent
	 * without forcing uninstall on the currently deployed applications.
	 * 
	 * @param force
	 *            Boolean flag.
	 */
	public void setForce(final boolean force) {
		this.force = force;
	}

	/**
	 * Starts management processes (LUS, GSM, ESM) on a local cloud, and waits
	 * until the requested service installations complete (space, webui, REST),
	 * or until the timeout is reached.
	 * 
	 * @param securityProfile
	 *            set security profile (nonsecure/secure/ssl)
	 * @param securityFilePath
	 *            path to the security configuration file
	 * @param username
	 *            The username for a secure connection to the server
	 * @param password
	 *            The password for a secure connection to the server
	 * @param keystoreFilePath
	 *            path to the keystore file
	 * @param keystorePassword
	 *            The password to the keystore to set on the rest server
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @throws CLIException
	 *             Reporting a failure to start the processes and services
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 */
	public void startLocalCloudOnLocalhostAndWait(final String securityProfile, final String securityFilePath, 
			final String username, final String password, final String keystoreFilePath, final String keystorePassword,
			final int timeout, final TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {

		setGridServiceAgentZone(LOCALCLOUD_GSA_ZONES);

		setDefaultNicAddress();

		setDefaultLocalcloudLookup();

		if (isWindows()) {
			startManagementOnLocalhostAndWaitInternal(LOCALCLOUD_WIN_MANAGEMENT_ARGUMENTS, securityProfile, 
					securityFilePath, username, password, keystoreFilePath, keystorePassword, timeout, timeunit, true);
		} else {
			startManagementOnLocalhostAndWaitInternal(LOCALCLOUD_LINUX_MANAGEMENT_ARGUMENTS, securityProfile, 
					securityFilePath, username, password, keystoreFilePath, keystorePassword, timeout, timeunit, true);
		}
	}

	private void setDefaultNicAddress() throws CLIException {

		if (nicAddress == null) {
			try {
				nicAddress = Constants.getHostAddress();
			} catch (final UnknownHostException e) {
				throw new CLIException(e);
			}
		}

		if (verbose) {
			publishEvent("NIC Address=" + nicAddress);
		}
	}

	private static String getLocalcloudLookupGroups() {
		return LOCALCLOUD_LOOKUPGROUP;
	}

	private String getLocalcloudLookupLocators() {
		if (nicAddress == null) {
			throw new IllegalStateException("nicAddress cannot be null");
		}
		return nicAddress + ":" + lusPort;
	}

	/**
	 * Starts management processes (LUS, GSM, ESM) and waits until the requested
	 * service installations complete (space, webui, REST), or until the timeout
	 * is reached. The cloud is not a local cloud.
	 * 
	 * @param securityProfile
	 *            set security profile (nonsecure/secure/ssl)
	 * @param securityFilePath
	 *            path to the security configuration file
	 * @param username
	 *            The username for a secure connection to the server
	 * @param password
	 *            The password for a secure connection to the server
	 * @param keystoreFilePath
	 *            path to the keystore file
	 * @param keystorePassword
	 *            The password to the keystore set on the rest server
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @throws CLIException
	 *             Reporting a failure to start the processes and services
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 */
	public void startManagementOnLocalhostAndWait(final String securityProfile, final String securityFilePath,
			final String username, final String password, final String keystoreFilePath, final String keystorePassword,
			final int timeout, final TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {

		setGridServiceAgentZone(MANAGEMENT_ZONE);

		setDefaultNicAddress();

		startManagementOnLocalhostAndWaitInternal(CLOUD_MANAGEMENT_ARGUMENTS, securityProfile, securityFilePath,
				username, password, keystoreFilePath, keystorePassword, timeout, timeunit, false);
	}

	/**
	 * Shuts down the local agent, if exists, and waits until shutdown is
	 * complete or until the timeout is reached. If management processes (GSM,
	 * ESM, LUS) are still active, the agent is not shutdown and a CLIException
	 * is thrown.
	 * 
	 * @param force
	 *            Force the agent to shut down even if the GSC still runs active
	 *            services
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @throws CLIException
	 *             Reporting a failure to shutdown the agent
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 */
	public void shutdownAgentOnLocalhostAndWait(final boolean force, final int timeout, final TimeUnit timeunit)
			throws CLIException, InterruptedException, TimeoutException {

		setDefaultNicAddress();

		shutdownAgentOnLocalhostAndWaitInternal(false, force, timeout, timeunit);
	}

	/**
	 * Shuts down the local agent, management processes (GSM, ESM, LUS) and GSC.
	 * Waits until shutdown is complete or until the timeout is reached. Active
	 * services are forced to shut down.
	 * 
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @throws CLIException
	 *             Reporting a failure to shutdown the agent
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 */
	public void shutdownManagementOnLocalhostAndWait(final int timeout, final TimeUnit timeunit) throws CLIException,
			InterruptedException, TimeoutException {

		setDefaultNicAddress();

		shutdownAgentOnLocalhostAndWaitInternal(true, true, timeout, timeunit);
	}

	/**
	 * Shuts down the local cloud, and waits until shutdown is complete or until
	 * the timeout is reached.
	 * 
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 * @throws CLIException
	 *             Reporting a failure to shutdown the agent
	 */
	public void teardownLocalCloudOnLocalhostAndWait(final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {

		setDefaultNicAddress();

		setDefaultLocalcloudLookup();

		uninstallApplications(timeout, timeunit);

		shutdownAgentOnLocalhostAndWaitInternal(true, true, timeout, timeunit);
	}

	private void uninstallApplications(final long timeout, final TimeUnit timeunit) throws InterruptedException,
			TimeoutException, CLIException {

		Collection<String> applicationsList = null;
		boolean applicationsExist = false;
		try {
			if (!adminFacade.isConnected()) {
				throw new CLIException("Failed to fetch applications list. "
						+ "Client is not connected to the rest server.");
			}

			applicationsList = adminFacade.getApplicationNamesList();
			// If there existed other applications besides the management.
			applicationsExist = applicationsList.size() > 1;
		} catch (final CLIException e) {
			if (!force) {
				throw new CLIStatusException(e, "failed_to_access_rest_before_teardown");
			}
			final String errorMessage = "Failed to fetch the currently deployed applications list."
					+ " Continuing teardown-localcloud.";
			if (verbose) {
				logger.log(Level.FINE, errorMessage, e);
				publishEvent(errorMessage + System.getProperty("line.separator") + e.toString());
			} else {
				logger.log(Level.FINE, errorMessage);
				publishEvent(errorMessage);
			}
			// Suppress exception. continue with teardown.
			return;
		}

		if (applicationsExist && !force) {
			throw new CLIStatusException("apps_deployed_before_teardown_localcloud", applicationsList.toString());
		}
		final String uninstallMessage = ShellUtils.getMessageBundle().getString(
				"uninstalling_applications_before_teardown");
		publishEvent(uninstallMessage);
		for (final String appName : applicationsList) {
			try {
				if (!appName.equals(MANAGEMENT_APPLICATION)) {
					logger.fine("Uninstalling application " + appName);
					final Map<String, String> uninstallApplicationResponse = adminFacade.uninstallApplication(appName,
							(int) timeout);
					if (uninstallApplicationResponse.containsKey(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID)) {
						final String pollingID = uninstallApplicationResponse
								.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);
						this.adminFacade.waitForLifecycleEvents(pollingID, (int) timeout,
                                CloudifyConstants.TIMEOUT_ERROR_MESSAGE);
					} else {
						publishEvent("Failed to retrieve lifecycle logs from rest. " + "Check logs for more details.");
					}
				}
			} catch (final CLIException e) {
				final String errorMessage = "Application " + appName + " faild to uninstall."
						+ " Continuing teardown-localcloud.";
				if (!force) {
					throw new CLIStatusException(e, "failed_to_uninstall_app_before_teardown", appName);
				}
				if (verbose) {
					logger.log(Level.FINE, errorMessage, e);
					publishEvent(errorMessage);
				} else {
					logger.log(Level.FINE, errorMessage);
				}
			}
		}
		if (applicationsExist) {
			waitForUninstallApplications(timeout, timeunit);
			publishEvent(ShellUtils.getMessageBundle().getString("all_apps_removed_before_teardown"));
			logger.fine(ShellUtils.getMessageBundle().getString("all_apps_removed_before_teardown"));
		}
	}

	private void waitForUninstallApplications(final long timeout, final TimeUnit timeunit) throws InterruptedException,
			TimeoutException, CLIException {
		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {

			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				final Collection<String> applications = adminFacade.getApplicationNamesList();

				boolean done = true;
				for (final String applicationName : applications) {
					if (!MANAGEMENT_APPLICATION.equals(applicationName)) {
						done = false;
						break;
					}
				}
				publishEvent(null);
				logger.fine("Waiting for all applications to uninstall");

				return done;
			}
		});
	}

	private void setDefaultLocalcloudLookup() {

		lusPort = CloudifyConstants.DEFAULT_LOCALCLOUD_LUS_PORT;

		if (lookupLocators == null) {
			setLookupLocators(getLocalcloudLookupLocators());
		}

		if (lookupGroups == null) {
			setLookupGroups(getLocalcloudLookupGroups());
		}
	}

	/**
	 * Shuts down the local agent, if exists, and waits until shutdown is
	 * complete or until the timeout is reached.
	 * 
	 * @param allowManagement
	 *            Allow the agent to shut down even the management processes
	 *            (GSM, ESM, LUS) it started are still active
	 * @param allowContainers
	 *            Allow the agent to shut down even the GSC still runs active
	 *            services
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @throws CLIException
	 *             Reporting a failure to shutdown the agent, or the
	 *             management/services components still require it
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 */
	public void shutdownAgentOnLocalhostAndWaitInternal(final boolean allowManagement, final boolean allowContainers,
			final long timeout, final TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {

		final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);
		final ConnectionLogsFilter connectionLogs = new ConnectionLogsFilter();
		connectionLogs.supressConnectionErrors();
		adminFacade.disconnect();
		final Admin admin = createAdmin();
		GridServiceAgent agent = null;
		try {
			setLookupDefaults(admin);
			try {
				agent = waitForExistingAgent(admin, WAIT_EXISTING_AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			} catch (final TimeoutException e) {
				// continue
			}

			if (agent == null) {
				logger.fine("Agent not running on local machine");
				if (verbose) {
					final String agentNotFoundMessage = ShellUtils.getMessageBundle().getString(
							"agent_not_found_on_teardown_command");
					publishEvent(agentNotFoundMessage);
				}
				throw new CLIStatusException("teardown_failed_agent_not_found");
			} else {
				// If the agent we attempt to shutdown is of a GSC that has
				// active services, allowContainers
				// must be true or an exception will be thrown.
				if (!allowContainers) {
					for (final ProcessingUnit pu : admin.getProcessingUnits()) {
						for (final ProcessingUnitInstance instance : pu) {
							if (agent.equals(instance.getGridServiceContainer().getGridServiceAgent())) {
								throw new CLIException("Cannot shutdown agent since " + pu.getName()
										+ " service is still running on this machine. Use -force flag.");
							}
						}
					}
				}

				// If the agent we attempt to shutdown is a GSM, ESM or LUS,
				// allowManagement must be true or
				// an exception will be thrown.
				if (!allowManagement) {
					final String message = "Cannot shutdown agent since management processes running on this machine. "
							+ "Use the shutdown-management command instead.";

					for (final GridServiceManager gsm : admin.getGridServiceManagers()) {
						if (agent.equals(gsm.getGridServiceAgent())) {
							throw new CLIException(message);
						}
					}

					for (final ElasticServiceManager esm : admin.getElasticServiceManagers()) {
						if (agent.equals(esm.getGridServiceAgent())) {
							throw new CLIException(message);
						}
					}

					for (final LookupService lus : admin.getLookupServices()) {
						if (agent.equals(lus.getGridServiceAgent())) {
							throw new CLIException(message);
						}
					}
				}
				// Close admin before shutting down the agent to avoid false
				// warning messages the admin will
				// create if it concurrently monitor things that are shutting
				// down.
				admin.close();
				shutdownAgentAndWait(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end), TimeUnit.MILLISECONDS);
			}
		} finally {
			// close in case of exception, admin support double close if already
			// closed
			admin.close();
			if (agent != null) {
				// admin.close() command does not verify that all of the
				// internal lookup threads are actually
				// terminated
				// therefore we need to suppress connection warnings a little
				// while longer
				Thread.sleep(WAIT_AFTER_ADMIN_CLOSED_MILLIS);
			}
			connectionLogs.restoreConnectionErrors();
		}
	}

	/**
	 * Shuts down the given agent, and waits until shutdown is complete or until
	 * the timeout is reached.
	 * 
	 * @param agent
	 *            The agent to shutdown
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 * @throws CLIException
	 *             Reporting a failure to shutdown the agent
	 */
	private void shutdownAgentAndWait(final GridServiceAgent agent, final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {

		// We need to shutdown the agent after we close the admin to avoid
		// closed exception since the admin
		// still monitors
		// the deployment behind the scenes, we call the direct proxy to the gsa
		// since the admin is closed and
		// we don't
		// want to use objects it generated
		final GSA gsa = ((InternalGridServiceAgent) agent).getGSA();
		try {
			gsa.shutdown();
		} catch (final RemoteException e) {
			if (!NetworkExceptionHelper.isConnectOrCloseException(e)) {
				logger.log(Level.FINER, "Failed to shutdown GSA", e);
				throw new AdminException("Failed to shutdown GSA", e);
			}
		}

		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {

			private boolean messagePublished = false;

			/**
			 * Pings the agent to verify it's not available, indicating it was
			 * shut down.
			 */
			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				if (!messagePublished) {
					final String shuttingDownAgentMessage = ShellUtils.getMessageBundle().getString(
							"shutting_down_cloudify_agent_teardown_localcloud");
					publishEvent(shuttingDownAgentMessage);

					final String shuttingDownManagmentMessage = ShellUtils.getMessageBundle().getString(
                            "shutting_down_cloudify_management");
					publishEvent(shuttingDownManagmentMessage);

					messagePublished = true;
				}
				logger.fine("Waiting for agent to shutdown");
				try {
					gsa.ping();
				} catch (final RemoteException e) {
					// Probably NoSuchObjectException meaning the GSA is going
					// down
					return true;
				}
				publishEvent(null);
				return false;
			}

		});
	}

	private void runGsAgentOnLocalHost(final String name, final String[] gsAgentArguments, 
			final String securityProfile, final String securityFilePath, final String keystoreFilePath, 
			final String keystorePassword) throws CLIException, InterruptedException {

		final List<String> args = new ArrayList<String>();
		args.addAll(Arrays.asList(gsAgentArguments));

		String[] command;
		if (isLocalCloud) {
			publishEvent(ShellUtils.getMessageBundle().getString("starting_bootstrap_localcloud"));
			if (isWindows()) {
				command = Arrays.copyOf(WINDOWS_LOCALCLOUD_COMMAND, WINDOWS_LOCALCLOUD_COMMAND.length);
				args.addAll(Arrays.asList(WINDOWS_ARGUMENTS_POSTFIX));
			} else {
				command = Arrays.copyOf(LINUX_LOCALCLOUD_COMMAND, LINUX_LOCALCLOUD_COMMAND.length);
				args.addAll(Arrays.asList(LINUX_ARGUMENTS_POSTFIX));
			}
		} else {
			if (isWindows()) {
				command = Arrays.copyOf(WINDOWS_CLOUD_COMMAND, WINDOWS_CLOUD_COMMAND.length);
				args.addAll(Arrays.asList(WINDOWS_ARGUMENTS_POSTFIX));
			} else {
				command = Arrays.copyOf(LINUX_CLOUD_COMMAND, LINUX_CLOUD_COMMAND.length);
				args.addAll(Arrays.asList(LINUX_ARGUMENTS_POSTFIX));
			}
		}
		if (verbose) {
			final String message = "Starting "
					+ name
					+ (verbose ? ":\n" + StringUtils.collectionToDelimitedString(Arrays.asList(command), " ") + " "
							+ StringUtils.collectionToDelimitedString(args, " ") : "");
			publishEvent(message);
			logger.fine(message);
		}

		publishEvent(ShellUtils.getMessageBundle().getString("starting_cloudify_management"));
		runCommand(command, args.toArray(new String[args.size()]), securityProfile, securityFilePath, keystoreFilePath,
				keystorePassword);

	}

	private void setIsLocalCloud(final boolean isLocalCloud) {
		this.isLocalCloud = isLocalCloud;
	}

	/**
	 * Starts management processes (LUS, GSM, ESM), and waits until the
	 * requested service installations complete (space, webui, REST), or until
	 * the timeout is reached.
	 * 
	 * @param gsAgentArgs
	 *            GS agent start-up switches
	 * @param securityProfile
	 *            set security profile (nonsecure/secure/ssl)
	 * @param securityFilePath
	 *            path to the security configuration file
	 * @param username
	 *            The username for a secure connection to the server
	 * @param password
	 *            The password for a secure connection to the server
	 * @param keystoreFilePath
	 *            path to the keystore file
	 * @param keystorePassword
	 *            The password to the keystore set on the rest server
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @param isLocalCloud
	 *            Is this a local cloud (true - yes, false - no)
	 * @throws CLIException
	 *             Reporting a failure to start the processes and services
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 */
	private void startManagementOnLocalhostAndWaitInternal(final String[] gsAgentArgs, final String securityProfile, 
			final String securityFilePath, final String username, final String password, final String keystoreFilePath,
			final String keystorePassword, final int timeout, final TimeUnit timeunit, final boolean isLocalCloud)
			throws CLIException, InterruptedException, TimeoutException {

		setIsLocalCloud(isLocalCloud);

		final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);

		if (gsaZones == null || gsaZones.isEmpty()) {
			throw new CLIException("Agent must be started with a zone");
		}

		final ConnectionLogsFilter connectionLogs = new ConnectionLogsFilter();
		connectionLogs.supressConnectionErrors();
		final Admin admin = createAdmin();
		try {
			setLookupDefaults(admin);
			GridServiceAgent agent;
			try {
				try {
					if (!isLocalCloud || fastExistingAgentCheck()) {
						waitForExistingAgent(admin, progressInSeconds, TimeUnit.SECONDS);
						throw new CLIException("Agent already running on local machine.");
					}
				} catch (final TimeoutException e) {
					// no existing agent running on local machine
				}

				runGsAgentOnLocalHost("agent and management processes", gsAgentArgs, securityProfile, securityFilePath,
						keystoreFilePath, keystorePassword);
				agent = waitForNewAgent(admin, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end),
						TimeUnit.MILLISECONDS);
			} finally {
				connectionLogs.restoreConnectionErrors();
			}

			// waiting for LUS, GSM and ESM services to start
			waitForManagementProcesses(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end),
					TimeUnit.MILLISECONDS);

			final List<AbstractManagementServiceInstaller> waitForManagementServices =
					new LinkedList<AbstractManagementServiceInstaller>();

			if (isLocalCloud) {
				startLocalCloudManagementServicesContainer(agent);
			}

			connectionLogs.supressConnectionErrors();
			try {
				ManagementSpaceServiceInstaller managementSpaceInstaller = null;
				if (!noManagementSpace) {
					final boolean highlyAvailable = !isLocalCloud && !notHighlyAvailableManagementSpace;
					String gscLrmiCommandLineArg = getGscLrmiCommandLineArg();
					managementSpaceInstaller = new ManagementSpaceServiceInstaller();
					managementSpaceInstaller.setAdmin(agent.getAdmin());
					managementSpaceInstaller.setVerbose(verbose);
					managementSpaceInstaller.setProgress(progressInSeconds, TimeUnit.SECONDS);
					managementSpaceInstaller.setMemory(CloudifyConstants.MANAGEMENT_SPACE_MEMORY_IN_MB, MemoryUnit.MEGABYTES);
					managementSpaceInstaller.setServiceName(MANAGEMENT_SPACE_NAME);
					managementSpaceInstaller.setManagementZone(MANAGEMENT_ZONE);
					managementSpaceInstaller.setHighlyAvailable(highlyAvailable);
					managementSpaceInstaller.addListeners(this.eventsListenersList);
					managementSpaceInstaller.setIsLocalCloud(isLocalCloud);
					managementSpaceInstaller.setLrmiCommandLineArgument(gscLrmiCommandLineArg);
					try {
						managementSpaceInstaller.installSpace();
						waitForManagementServices.add(managementSpaceInstaller);
					} catch (final ProcessingUnitAlreadyDeployedException e) {
						if (verbose) {
							logger.fine("Service " + MANAGEMENT_SPACE_NAME + " already installed");
							publishEvent("Service " + MANAGEMENT_SPACE_NAME + " already installed");
						}
					}
				}

				if (!noWebServices) {
					installWebServices(username, password, isLocalCloud, 
							ShellUtils.isSecureConnection(securityProfile), agent, waitForManagementServices);
				}

				for (final AbstractManagementServiceInstaller managementServiceInstaller : waitForManagementServices) {
					managementServiceInstaller.waitForInstallation(adminFacade, agent, 
							ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end), TimeUnit.MILLISECONDS);
					if (managementServiceInstaller instanceof ManagementSpaceServiceInstaller) {
						logger.fine("Writing cloud configuration to space.");
						if (verbose) {
							publishEvent("Writing cloud configuration to space.");
						}
						final GigaSpace gigaspace = managementSpaceInstaller.getGigaSpace();

						final CloudConfigurationHolder holder = new CloudConfigurationHolder(null, getCloudFilePath());
						logger.fine("Writing cloud Configuration to space: " + holder);
						gigaspace.write(holder);
						// Shut down the space proxy so that if the cloud is
						// turned down later, there will not
						// be any discovery errors.
						// Note: in a spring environment, the bean shutdown
						// would clean this up.
						// TODO - Move the space writing part into the
						// management space
						// installer and do the clean up there.
						((ISpaceProxy) gigaspace.getSpace()).close();
					}
				}

			} finally {
				connectionLogs.restoreConnectionErrors();
			}
		} finally {
			admin.close();
		}
	}

	private String getGscLrmiCommandLineArg() {
		String lrmiPortRangeCommandLineArgument = "-D" + CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "=";
		String portRange = System.getenv().get(CloudifyConstants.GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR);
		if (!org.apache.commons.lang.StringUtils.isEmpty(portRange)) {
			lrmiPortRangeCommandLineArgument += portRange; 
		} else {
			lrmiPortRangeCommandLineArgument += CloudifyConstants.DEFAULT_GSC_LRMI_PORT_RANGE; 
		}
		return lrmiPortRangeCommandLineArgument;
	}

	private void installWebServices(final String username, final String password, final boolean isLocalCloud,
			final boolean isSecureConnection, final GridServiceAgent agent, 
			final List<AbstractManagementServiceInstaller> waitForManagementServices)
			throws CLIException {
		String gscLrmiCommandLineArg = getGscLrmiCommandLineArg();
		long webuiMemory = getWebServiceMemory(CloudifyConstants.WEBUI_MAX_MEMORY_ENVIRONMENT_VAR);
		int webuiPort = getWebservicePort(CloudifyConstants.WEBUI_PORT_ENV_VAR, isSecureConnection);
		
		final ManagementWebServiceInstaller webuiInstaller = new ManagementWebServiceInstaller();
		webuiInstaller.setAdmin(agent.getAdmin());
		webuiInstaller.setVerbose(verbose);
		webuiInstaller.setProgress(progressInSeconds, TimeUnit.SECONDS);
		webuiInstaller.setMemory(webuiMemory, MemoryUnit.MEGABYTES);
		webuiInstaller.setPort(webuiPort);
		webuiInstaller.setWarFile(new File(WEBUI_FILE));
		webuiInstaller.setServiceName(WEBUI_NAME);
		webuiInstaller.setManagementZone(MANAGEMENT_ZONE);
		webuiInstaller.addListeners(this.eventsListenersList);
		webuiInstaller.setIsLocalCloud(isLocalCloud);
		webuiInstaller.setIsSecureConnection(isSecureConnection);
		webuiInstaller.setLrmiCommandLineArgument(gscLrmiCommandLineArg);
		
		try {
			webuiInstaller.installWebService();
		} catch (final ProcessingUnitAlreadyDeployedException e) {
			if (verbose) {
				logger.fine("Service " + WEBUI_NAME + " already installed");
				publishEvent("Service " + WEBUI_NAME + " already installed");
			}
		}
		if (waitForWebUi) {
			waitForManagementServices.add(webuiInstaller);
		} else {
			webuiInstaller.logServiceLocation();
		}
		int restPort = getWebservicePort(CloudifyConstants.REST_PORT_ENV_VAR, isSecureConnection);
		long webServiceMemory = getWebServiceMemory(CloudifyConstants.REST_MAX_MEMORY_ENVIRONMENT_VAR);
		
		final ManagementWebServiceInstaller restInstaller = new ManagementWebServiceInstaller();
		restInstaller.setAdmin(agent.getAdmin());
		restInstaller.setProgress(progressInSeconds, TimeUnit.SECONDS);
		restInstaller.setVerbose(verbose);
		restInstaller.setMemory(webServiceMemory, MemoryUnit.MEGABYTES);
		restInstaller.setPort(restPort);
		restInstaller.setUsername(username);
		restInstaller.setPassword(password);
		restInstaller.setWarFile(new File(REST_FILE));
		restInstaller.setServiceName(REST_NAME);
		restInstaller.setManagementZone(MANAGEMENT_ZONE);
		restInstaller.dependencies.add(CloudifyConstants.MANAGEMENT_SPACE_NAME);
		restInstaller.setWaitForConnection();
		restInstaller.addListeners(this.eventsListenersList);
		restInstaller.setIsLocalCloud(isLocalCloud);
		restInstaller.setIsSecureConnection(isSecureConnection);
		restInstaller.setLrmiCommandLineArgument(gscLrmiCommandLineArg);
		
		try {
			restInstaller.installWebService();
		} catch (final ProcessingUnitAlreadyDeployedException e) {
			if (verbose) {
				logger.fine("Service " + REST_NAME + " already installed");
				publishEvent("Service " + REST_NAME + " already installed");
			}
		}
		waitForManagementServices.add(restInstaller);
	}

	private long getWebServiceMemory(final String memoryEnvironmentVar) {
		long memory;
		String memoryString = System.getenv().get(memoryEnvironmentVar);
		if (org.apache.commons.lang.StringUtils.isNotBlank(memoryString)) {
			memory = getMemoryFromMemoryString(memoryString);
		} else {
			if (memoryEnvironmentVar.equals(CloudifyConstants.REST_MAX_MEMORY_ENVIRONMENT_VAR)) {
				memory = getMemoryFromMemoryString(CloudifyConstants.DEFAULT_REST_MAX_MEMORY);
			} else {
				memory = getMemoryFromMemoryString(CloudifyConstants.DEFAULT_REST_MIN_MEMORY);
			}
		}
		return memory;
	}
	
	private long getMemoryFromMemoryString(String memoryString) {
		return Long.parseLong(memoryString.substring(0, memoryString.length() - 1));
	}
	
	private int getWebservicePort(String portEnvVriable, boolean isSecureConnection) {
		String port = System.getenv().get(portEnvVriable);
		if (org.apache.commons.lang.StringUtils.isNotBlank(port)) {
			return Integer.parseInt(port);
		} 
		if (portEnvVriable.equals(CloudifyConstants.WEBUI_PORT_ENV_VAR)) {
			if (isSecureConnection){
				return CloudifyConstants.SECURE_WEBUI_PORT;
			} else {
				return CloudifyConstants.DEFAULT_WEBUI_PORT;
			}
		} else {
			if (isSecureConnection){
				return CloudifyConstants.SECURE_REST_PORT;
			} else {
				return CloudifyConstants.DEFAULT_REST_PORT;
			}
		}
		
	}

	private void startLocalCloudManagementServicesContainer(final GridServiceAgent agent) {
		final GridServiceContainerOptions options = new GridServiceContainerOptions().vmInputArgument(
				"-Xmx" + CloudifyConstants.DEFAULT_LOCALCLOUD_REST_WEBUI_SPACE_MEMORY_IN_MB + "m").vmInputArgument(
				"-Dcom.gs.zones=rest,cloudifyManagementSpace,webui");
		agent.startGridServiceAndWait(options);
	}

	private boolean fastExistingAgentCheck() {
		return !ServiceUtils.isPortFree(lusPort);
	}

	/**
	 * This method assumes that the admin has been supplied with
	 * this.lookupLocators and this.lookupGroups and that it applied the
	 * defaults if these were null.
	 * 
	 * @param admin
	 */
	private void setLookupDefaults(final Admin admin) {
		if (admin.getGroups().length == 0 || admin.getGroups() == null) {
			throw new IllegalStateException("Admin lookup group must be set");
		}
		this.lookupGroups = StringUtils.arrayToDelimitedString(admin.getGroups(), ",");
		final LookupLocator[] locators = admin.getLocators();
		if (locators != null && locators.length > 0) {
			this.lookupLocators = convertLookupLocatorToString(locators);
		}
	}

	/**
	 * Converts the given locators to a String of comma-delimited locator names.
	 * The locator names are of this format: <locator_host>:<locator_port>
	 * 
	 * @param locators
	 *            an array of {@link LookupLocator} objects to convert to String
	 * @return A comma-delimited list of lookup locators
	 */
	public static String convertLookupLocatorToString(final LookupLocator[] locators) {
		final List<String> trimmedLocators = new ArrayList<String>();
		if (locators != null) {
			for (final LookupLocator locator : locators) {
				trimmedLocators.add(locator.getHost() + ":" + locator.getPort());
			}
		}
		return StringUtils.collectionToDelimitedString(trimmedLocators, ",");
	}

	/**
	 * Starts an agent on the local host. If an agent is already running, a
	 * CLIException is thrown.
	 * 
	 * @param securityProfile
	 *            set security profile (nonsecure/secure/ssl)
	 * @param keystorePassword
	 *            the password to the keystore set on the rest server
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @throws CLIException
	 *             Reporting a failure to start the processes and services
	 * @throws InterruptedException
	 *             Reporting the thread was interrupted while waiting
	 * @throws TimeoutException
	 *             Reporting the timeout was reached
	 */
	public void startAgentOnLocalhostAndWait(final String securityProfile, final String keystorePassword,
			final long timeout, final TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {

		setIsLocalCloud(false);

		setDefaultNicAddress();

		final ConnectionLogsFilter connectionLogs = new ConnectionLogsFilter();
		connectionLogs.supressConnectionErrors();
		final Admin admin = createAdmin();
		try {
			setLookupDefaults(admin);

			try {
				waitForExistingAgent(admin, WAIT_EXISTING_AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				throw new CLIException("Agent already running on local machine. Use shutdown-agent first.");
			} catch (final TimeoutException e) {
				// no existing agent running on local machine
			}
			runGsAgentOnLocalHost("agent", AGENT_ARGUMENTS, securityProfile, "" /*securityFilePath*/,
					"" /*keystoreFilePath*/, keystorePassword);

			// wait for agent to start
			waitForNewAgent(admin, timeout, timeunit);
		} finally {
			admin.close();
			connectionLogs.restoreConnectionErrors();
		}
	}

	private void waitForManagementProcesses(final GridServiceAgent agent, final long timeout, final TimeUnit timeunit)
			throws TimeoutException, InterruptedException, CLIException {

		final Admin admin = agent.getAdmin();

		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean isDone() throws CLIException, InterruptedException {

				boolean isDone = true;

				if (!isDone(admin.getLookupServices(), "LUS")) {
					if (verbose) {
						logger.fine("Waiting for Lookup Service");
						publishEvent("Waiting for Lookup Service");
					}
					isDone = false;
				}

				if (!isDone(admin.getGridServiceManagers(), "GSM")) {
					if (verbose) {
						logger.fine("Waiting for Grid Service Manager");
						publishEvent("Waiting for Grid Service Manager");
					}
					isDone = false;
				}

				if (admin.getElasticServiceManagers().isEmpty()) {
					if (verbose) {
						logger.fine("Waiting for Elastic Service Manager");
						publishEvent("Waiting for Elastic Service Manager");
					}
					isDone = false;
				}

				if (verbose) {
					logger.fine("Waiting for Management processes to start.");
					publishEvent("Waiting for Management processes to start.");
				}

				if (!isDone) {
					publishEvent(null);
				}

				return isDone;
			}

			private boolean isDone(final Iterable<? extends AgentGridComponent> components, final String serviceName) {
				boolean found = false;
				if (isLocalCloud) {
					if (components.iterator().hasNext()) {
						found = true;
					}
				} else {
					for (final AgentGridComponent component : components) {
						if (checkAgent(component)) {
							found = true;
							break;
						}
					}
				}

				if (verbose) {
					if (!isLocalCloud) {
						for (final Object component : components) {
							final GridServiceAgent agentThatStartedComponent = ((AgentGridComponent) component)
									.getGridServiceAgent();
							String agentUid = null;
							if (agentThatStartedComponent != null) {
								agentUid = agentThatStartedComponent.getUid();
							}
							String message = "Detected " + serviceName + " management process " + " started by agent "
									+ agentUid + " ";
							if (!checkAgent((AgentGridComponent) component)) {
								message += " expected agent " + agent.getUid();
							}
							logger.fine(message);
							publishEvent(message);
						}
					}
				}
				if (!verbose) {
					publishEvent(null);
				}
				return found;
			}

			private boolean checkAgent(final AgentGridComponent component) {
				return agent.equals(component.getGridServiceAgent());
			}

		});
	}

	private GridServiceAgent waitForExistingAgent(final Admin admin, final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {
		return waitForAgent(admin, true, timeout, timeunit);
	}

	private GridServiceAgent waitForNewAgent(final Admin admin, final long timeout, final TimeUnit timeunit)
			throws InterruptedException, TimeoutException, CLIException {
		return waitForAgent(admin, false, timeout, timeunit);
	}

	private GridServiceAgent waitForAgent(final Admin admin, final boolean existingAgent, final long timeout,
			final TimeUnit timeunit) throws InterruptedException, TimeoutException, CLIException {

		final AtomicReference<GridServiceAgent> agentOnLocalhost = new AtomicReference<GridServiceAgent>();

		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean isDone() throws CLIException, InterruptedException {

				boolean isDone = false;
				for (final GridServiceAgent agent : admin.getGridServiceAgents()) {
					if (checkAgent(agent)) {
						agentOnLocalhost.set(agent);
						isDone = true;
						break;
					}
				}
				if (!isDone) {
					if (existingAgent) {
						logger.fine("Looking for an existing agent running on local machine");
					} else {
						logger.fine("Waiting for the agent on the local machine to start.");
					}
					publishEvent(null);
				}
				return isDone;
			}

			private boolean checkAgent(final GridServiceAgent agent) {
				final String agentNicAddress = agent.getMachine().getHostAddress();
				final String agentLookupGroups = getLookupGroups(agent);
				final boolean checkLookupGroups = lookupGroups != null && lookupGroups.equals(agentLookupGroups);
				final boolean checkNicAddress = nicAddress != null && agentNicAddress.equals(nicAddress)
						|| IsLocalCloudUtils.isThisMyIpAddress(agentNicAddress);
				if (verbose) {
					String message = "Discovered agent nic-address=" + agentNicAddress + " lookup-groups="
							+ agentLookupGroups + ". ";
					if (!checkLookupGroups) {
						message += "Ignoring agent. Filter lookupGroups='" + lookupGroups + "', agent LookupGroups='"
								+ agentLookupGroups + "'";
					}
					if (!checkNicAddress) {
						message += "Ignoring agent. Filter nicAddress='" + nicAddress
								+ "' or local address, agent nicAddress='" + agentNicAddress + "'";
					}
					publishEvent(message);
				}
				return checkLookupGroups && checkNicAddress;
			}

			private String getLookupGroups(final VirtualMachineAware component) {

				final String prefix = "-Dcom.gs.jini_lus.groups=";
				return getCommandLineArgumentRemovePrefix(component, prefix);
			}

			private String getCommandLineArgumentRemovePrefix(final VirtualMachineAware component,
					final String prefix) {
				final String[] commandLineArguments = component.getVirtualMachine().getDetails().getInputArguments();
				String requiredArg = null;
				for (final String arg : commandLineArguments) {
					if (arg.startsWith(prefix)) {
						requiredArg = arg;
					}
				}

				if (requiredArg != null) {
					return requiredArg.substring(prefix.length());
				}
				return null;
			}
		});

		return agentOnLocalhost.get();
	}

	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	private void runCommand(final String[] command, final String[] args, final String securityProfile,
			final String securityFilePath, final String keystoreFilePath, final String keystorePassword) 
					throws CLIException, InterruptedException {

		final File directory = new File(Environment.getHomeDirectory(), "/bin").getAbsoluteFile();

		// gs-agent.sh/bat need full path
		command[command.length - 1] = new File(directory, command[command.length - 1]).getAbsolutePath();

		final List<String> commandLine = new ArrayList<String>();
		commandLine.addAll(Arrays.asList(command));
		commandLine.addAll(Arrays.asList(args));

		final String commandString = StringUtils.collectionToDelimitedString(commandLine, " ");
		final File filename = createScript(commandString);
		final ProcessBuilder pb = new ProcessBuilder().command(filename.getAbsolutePath()).directory(directory);

		String localCloudOptions = "-Xmx" + CloudifyConstants.DEFAULT_LOCALCLOUD_GSA_GSM_ESM_LUS_MEMORY_IN_MB + "m" + " -D" + CloudifyConstants.LUS_PORT_CONTEXT_PROPERTY + "="
				+ lusPort + " -D" + GSM_EXCLUDE_GSC_ON_FAILED_INSTANCE + "=" + GSM_EXCLUDE_GSC_ON_FAILED_INSTACE_BOOL
				+ " " + GSM_PENDING_REQUESTS_DELAY + " -D" + ZONES_PROPERTY + "=" + gsaZones;

		String gsaJavaOptions = "-Xmx" + CloudifyConstants.DEFAULT_AGENT_MAX_MEMORY;
		if (gsaZones != null) {
			gsaJavaOptions += " -D" + ZONES_PROPERTY + "=" + gsaZones;
		}
		if (autoShutdown) {
			gsaJavaOptions += " " + AUTO_SHUTDOWN_COMMANDLINE_ARGUMENT;
		}
		String lusJavaOptions = "-Xmx" + CloudifyConstants.DEFAULT_LUS_MAX_MEMORY + " -D" + CloudifyConstants.LUS_PORT_CONTEXT_PROPERTY + "=" + lusPort
				+ " -D" + ZONES_PROPERTY + "=" + MANAGEMENT_ZONE;
		String gsmJavaOptions = "-Xmx" + CloudifyConstants.DEFAULT_GSM_MAX_MEMORY + " -D" + CloudifyConstants.LUS_PORT_CONTEXT_PROPERTY + "=" + lusPort
				+ " -D" + GSM_EXCLUDE_GSC_ON_FAILED_INSTANCE + "=" + GSM_EXCLUDE_GSC_ON_FAILED_INSTACE_BOOL + " -D"
				+ ZONES_PROPERTY + "=" + MANAGEMENT_ZONE + " " + GSM_PENDING_REQUESTS_DELAY;
		String esmJavaOptions = "-Xmx" + CloudifyConstants.DEFAULT_ESM_MAX_MEMORY + " -D" + ZONES_PROPERTY + "=" + MANAGEMENT_ZONE;
		String gscJavaOptions = "";

		final Map<String, String> environment = pb.environment();
		if (lookupGroups != null) {
			environment.put("LOOKUPGROUPS", lookupGroups);
		}

		if (lookupLocators != null) {
			final String disableMulticast = "-Dcom.gs.multicast.enabled=false";
			environment.put("LOOKUPLOCATORS", lookupLocators);
			gsaJavaOptions += " " + disableMulticast;
			lusJavaOptions += " " + disableMulticast;
			gsmJavaOptions += " " + disableMulticast;
			esmJavaOptions += " " + disableMulticast;
			gscJavaOptions += " " + disableMulticast;
			localCloudOptions += " " + disableMulticast;
		}
		// in case environment vars were defined,
		// They will override the existing component java options.  
		gsaJavaOptions += " " + environment.get("GSA_JAVA_OPTIONS") == null ? "" : environment.get("GSA_JAVA_OPTIONS");
		lusJavaOptions += " " + environment.get("LUS_JAVA_OPTIONS") == null ? "" : environment.get("LUS_JAVA_OPTIONS");
		gsmJavaOptions += " " + environment.get("GSM_JAVA_OPTIONS") == null ? "" : environment.get("GSM_JAVA_OPTIONS");
		esmJavaOptions += " " + environment.get("ESM_JAVA_OPTIONS") == null ? "" : environment.get("ESM_JAVA_OPTIONS");
		gscJavaOptions += " " + environment.get("GSC_JAVA_OPTIONS") == null ? "" : environment.get("GSC_JAVA_OPTIONS");
		
		if (nicAddress != null) {
			environment.put("NIC_ADDR", nicAddress);
		}
		environment.put("RMI_OPTIONS", "");
		logger.fine("Setting env var " + CloudifyConstants.SPRING_ACTIVE_PROFILE_ENV_VAR + " to: " + securityProfile);
		environment.put(CloudifyConstants.SPRING_ACTIVE_PROFILE_ENV_VAR, securityProfile);
		if (ShellUtils.isSecureConnection(securityProfile)) {
			logger.fine("Setting env var " + CloudifyConstants.KEYSTORE_FILE_ENV_VAR + " to: " + keystoreFilePath);
			environment.put(CloudifyConstants.KEYSTORE_FILE_ENV_VAR, keystoreFilePath);
			logger.fine("Setting env var " + CloudifyConstants.KEYSTORE_PASSWORD_ENV_VAR + " to: " + keystorePassword);
			environment.put(CloudifyConstants.KEYSTORE_PASSWORD_ENV_VAR, keystorePassword);	
		}
		logger.fine("Setting env var " + CloudifyConstants.SPRING_SECURITY_CONFIG_FILE_ENV_VAR + " to: " + securityFilePath);
		environment.put(CloudifyConstants.SPRING_SECURITY_CONFIG_FILE_ENV_VAR, securityFilePath);

		if (isLocalCloud) {
			logger.fine("Setting env vars COMPONENT_JAVA_OPTIONS: " + localCloudOptions);
			environment.put("COMPONENT_JAVA_OPTIONS", localCloudOptions);
			environment.put(CloudifyConstants.GIGASPACES_CLOUD_HARDWARE_ID, "localcloud");
			environment.put(CloudifyConstants.GIGASPACES_CLOUD_IMAGE_ID, "localcloud");
			environment.put(CloudifyConstants.GIGASPACES_CLOUD_TEMPLATE_NAME, "localcloud");
			environment.put(CloudifyConstants.GIGASPACES_CLOUD_MACHINE_ID, "localcloud");
			if (nicAddress != null) {
				environment.put(CloudifyConstants.GIGASPACES_AGENT_ENV_PRIVATE_IP, nicAddress);
				environment.put(CloudifyConstants.GIGASPACES_AGENT_ENV_PUBLIC_IP, nicAddress);
			}
		} else {
			logger.fine("Setting env vars " + "GSA_JAVA_OPTIONS: gsaJavaOptions" + gsaJavaOptions 
					+ "; LUS_JAVA_OPTIONS: " + lusJavaOptions + "; GSM_JAVA_OPTIONS: " + gsmJavaOptions 
					+ "; ESM_JAVA_OPTIONS: " + esmJavaOptions + "; GSC_JAVA_OPTIONS: " + gscJavaOptions);
			environment.put("GSA_JAVA_OPTIONS", gsaJavaOptions);
			environment.put("LUS_JAVA_OPTIONS", lusJavaOptions);
			environment.put("GSM_JAVA_OPTIONS", gsmJavaOptions);
			environment.put("ESM_JAVA_OPTIONS", esmJavaOptions);
			environment.put("GSC_JAVA_OPTIONS", gscJavaOptions);
		}
		// start process
		// there is no need to redirect output, since the process suppresses
		// output
		try {
			logger.fine("Executing command: " + commandString);
			final Process proc = pb.start();
			Thread.sleep(MIN_PROC_ERROR_TIME);
			try {
				// The assumption is that if the script contains errors,
				// the processBuilder will finish by the end of the above sleep
				// period.
				if (proc.exitValue() != 0) {
					String errorMessage = "Error while starting agent. "
							+ "Please make sure that another agent is not already running. ";
					if (verbose) {
						errorMessage = errorMessage.concat("Command executed: " + commandString);
					}
					throw new CLIException(errorMessage);
				}
				// ProcessBuilder is still running. We assume the agent script
				// is running fine.
			} catch (final IllegalThreadStateException e) {
				logger.fine("agent is starting...");
			}
		} catch (final IOException e) {
			throw new CLIException("Error while starting agent", e);
		}
	}

	private Admin createAdmin() {
		final AdminFactory adminFactory = new AdminFactory();
		adminFactory.useGsLogging(false);
		if (lookupGroups != null) {
			adminFactory.addGroups(lookupGroups);
		}

		if (lookupLocators != null) {
			adminFactory.addLocators(lookupLocators);
		}

		final Admin admin = adminFactory.create();
		if (verbose) {
			if (admin.getLocators().length > 0) {
				logger.fine("Lookup Locators=" + convertLookupLocatorToString(admin.getLocators()));
				publishEvent("Lookup Locators=" + convertLookupLocatorToString(admin.getLocators()));
			}

			if (admin.getGroups().length > 0) {
				logger.fine("Lookup Groups=" + StringUtils.arrayToDelimitedString(admin.getGroups(), ","));
				publishEvent("Lookup Groups=" + StringUtils.arrayToDelimitedString(admin.getGroups(), ","));
			}
		}
		return admin;
	}

	private ConditionLatch createConditionLatch(final long timeout, final TimeUnit timeunit) {
		return new ConditionLatch().timeout(timeout, timeunit).pollingInterval(progressInSeconds, TimeUnit.SECONDS)
				.timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE).verbose(verbose);
	}

	private File createScript(final String text) throws CLIException {
		File tempFile;
		try {
			tempFile = File.createTempFile("run-gs-agent", isWindows() ? ".bat" : ".sh");
		} catch (final IOException e) {
			throw new CLIException(e);
		}
		tempFile.deleteOnExit();
		BufferedWriter out = null;
		try {
			try {
				out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile)));
				if (!isWindows()) {
					out.write(LINUX_SCRIPT_PREFIX);
				}
				out.write(text);
			} finally {
				if (out != null) {
					out.close();
				}
			}
		} catch (final IOException e) {
			throw new CLIException(e);
		}
		if (!isWindows()) {
			tempFile.setExecutable(true, true);
		}
		return tempFile;
	}

	/**********
	 * Registers an event listener for installation events.
	 * 
	 * @param listener
	 *            the listener.
	 */
	public void addListener(final LocalhostBootstrapperListener listener) {
		this.eventsListenersList.add(listener);
	}

	private void publishEvent(final String event) {
		for (final LocalhostBootstrapperListener listener : this.eventsListenersList) {
			listener.onLocalhostBootstrapEvent(event);
		}
	}

	public String getCloudFilePath() {
		return cloudFilePath;
	}

	public void setCloudFilePath(final String cloudFilePath) {
		this.cloudFilePath = cloudFilePath;
	}

}
