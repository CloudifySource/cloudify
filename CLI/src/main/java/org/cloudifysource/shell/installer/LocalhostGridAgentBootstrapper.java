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

import org.apache.commons.io.FileUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.dsl.utils.NetworkUtils;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.restclient.utils.NewRestClientUtils;
import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.EnvironmentUtils;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.exceptions.CLIValidationException;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.rest.inspect.CLIApplicationUninstaller;
import org.cloudifysource.shell.validators.CloudifyMachineValidator;
import org.cloudifysource.shell.validators.CloudifyMachineValidatorsFactory;
import org.cloudifysource.utilitydomain.data.CloudConfigurationHolder;
import org.cloudifysource.utilitydomain.openspaces.OpenspacesConstants;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.AgentGridComponent;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceContainerOptions;
import org.openspaces.admin.gsc.GridServiceContainer;
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
import static org.cloudifysource.dsl.internal.CloudifyConstants.MANAGEMENT_SPACE_NAME;
import static org.cloudifysource.dsl.internal.CloudifyConstants.MANAGEMENT_REST_SERVICE_NAME;
import static org.cloudifysource.dsl.internal.CloudifyConstants.MANAGEMENT_WEBUI_SERVICE_NAME;
import static org.cloudifysource.dsl.internal.CloudifyConstants.MANAGEMENT_SPACE_MEMORY_IN_MB;

/**
 * @author rafi, barakm, adaml, noak
 * @since 2.0.0
 * 
 *        This class handles the start up and shut down of the cloud components - management components (LUS, GSM, ESM),
 *        containers (GSCs) and an agent.
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
	private static final int WAIT_AFTER_ADMIN_CLOSED_MILLIS = 10 * 1000;
	private static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out waiting for the agent to start."
			+ " Configure the timeout using the -timeout flag.";
	private static final String CONTAINER_TIMEOUT_ERROR_MESSAGE = "The operation timed out waiting for the container to start."
			+ " Configure the timeout using the -timeout flag.";
	private static final String REST_FILE = "tools" + File.separator + "rest" + File.separator + "rest.war";

	private static final String LINUX_SCRIPT_PREFIX = "#!/bin/bash\n";
	private static final String MANAGEMENT_ZONE = "management";
	private static final String LOCALCLOUD_GSA_ZONES = MANAGEMENT_ZONE + ",localcloud";
	private static final long WAIT_EXISTING_AGENT_TIMEOUT_SECONDS = 10;

	// management agent starts 1 global esm, 1 gsm,1 lus
	private static final String[] CLOUD_MANAGEMENT_ARGUMENTS = new String[] { "gsa.global.lus", "0", "gsa.lus", "1",
			"gsa.gsc", "0", "gsa.global.gsm", "0", "gsa.gsm", "1", "gsa.global.esm", "1" };

	// localcloud management agent starts 1 esm, 1 gsm,1 lus
	private static final String[] LOCALCLOUD_WIN_MANAGEMENT_ARGUMENTS = new String[] { "start", "\"LH,GSM,GSA,ESM\"",
			"gsa.global.lus", "0", "gsa.lus", "0", "gsa.gsc", "0", "gsa.global.gsm", "0",
			"gsa.gsm_lus", "0", "gsa.global.esm", "0", "gsa.esm", "0" };
	// localcloud management agent starts 1 esm, 1 gsm,1 lus
	private static final String[] LOCALCLOUD_LINUX_MANAGEMENT_ARGUMENTS = new String[] { "start",
			"\\\"LH,GSM,GSA,ESM\\\"", "gsa.global.lus", "0", "gsa.lus", "0",
			"gsa.gsc", "0", "gsa.global.gsm", "0", "gsa.gsm_lus", "0", "gsa.global.esm", "0", "gsa.esm", "0" };

	private static final String[] AGENT_ARGUMENTS = new String[] { "gsa.global.lus", "0", "gsa.gsc", "0",
			"gsa.global.gsm", "0", "gsa.global.esm", "0" };

	// script must spawn a daemon process (that is not a child process)
	private static final String[] WINDOWS_LOCALCLOUD_COMMAND = new String[] { "cmd.exe", "/c", "@call", "gs.bat" };
	private static final String[] LINUX_LOCALCLOUD_COMMAND = new String[] { "nohup", "gs.sh" };

	// script must spawn a daemon process (that is not a child process)
	private static final String[] WINDOWS_CLOUD_COMMAND = new String[] { "cmd.exe", "/c", "gs-agent.bat" };
	private static final String[] LINUX_CLOUD_COMMAND = new String[] { "nohup", "gs-agent.sh" };

	// script must suppress output, since this process is not consuming it and
	// so any output could block it.
	private static final String[] WINDOWS_ARGUMENTS_POSTFIX = new String[] { ">nul", "2>&1" };

	private static final String[] LINUX_ARGUMENTS_POSTFIX = new String[] { ">/dev/null", "2>&1" };

	private static final String LOCALCLOUD_REST_MEMORY = "128m";
	private static final String LOCALCLOUD_WEBUI_MEMORY = "256m";

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private boolean verbose;
	private String lookupGroups;
	private String lookupLocators;
	private String nicAddress;
	private int progressInSeconds;
	private AdminFacade adminFacade;
	private boolean noWebServices;
	private boolean noManagementSpace;
	private boolean noManagementSpaceContainer;
	private boolean notHighlyAvailableManagementSpace;
	// private int lusPort = OpenspacesConstants.DEFAULT_LUS_PORT;
	private boolean waitForWebUi;

	private String cloudFilePath;
	private boolean force;
	private final List<LocalhostBootstrapperListener> eventsListenersList =
			new ArrayList<LocalhostBootstrapperListener>();
	private boolean isLocalCloud;
	private Cloud cloud;

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
	 *            web services limitation mode (true - not active, false - active web services)
	 */
	public void setNoWebServices(final boolean noWebServices) {
		this.noWebServices = noWebServices;
	}

	/**
	 * Sets management space limitation mode.
	 * 
	 * @param noManagementSpace
	 *            noManagementSpace limitation mode (true - management space will not be installed, false - it will be
	 *            installed)
	 */
	public void setNoManagementSpace(final boolean noManagementSpace) {
		this.noManagementSpace = noManagementSpace;
	}
	
	public void setNoManagementSpaceContainer(final boolean noManagementSpaceContainer) {
		this.noManagementSpaceContainer = noManagementSpaceContainer;
	}

	/**
	 * Sets whether to wait for the web UI installation to complete when starting management components.
	 * 
	 * @param waitForWebui
	 *            waitForWebui mode (true - wait, false - return without waiting).
	 */
	public void setWaitForWebui(final boolean waitForWebui) {
		this.waitForWebUi = waitForWebui;
	}

	/**
	 * Sets the availability mode of the space - if a backup space is required for the space to become available.
	 * 
	 * @param notHighlyAvailableManagementSpace
	 *            high-availability mode (true - the space will be available without a backup space, false - a backup
	 *            space is required).
	 */
	public void setNotHighlyAvailableManagementSpace(final boolean notHighlyAvailableManagementSpace) {
		this.notHighlyAvailableManagementSpace = notHighlyAvailableManagementSpace;
	}

	/**
	 * Gets the availability mode of the space.
	 * 
	 * @return high-availability mode (true - the space is available when a single instance is ready, false - a backup
	 *         space is required for the space to become available).
	 */
	public boolean isNotHighlyAvailableManagementSpace() {
		return notHighlyAvailableManagementSpace;
	}

	/**
	 * Enables force teardown. The force flag will terminate the gs agent without forcing uninstall on the currently
	 * deployed applications.
	 * 
	 * 
	 * @param force
	 *            Boolean flag.
	 */
	public void setForce(final boolean force) {
		this.force = force;
	}

	/**
	 * Starts management processes (LUS, GSM, ESM) on a local cloud, and waits until the requested service installations
	 * complete (space, webui, REST), or until the timeout is reached.
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
	public void startLocalCloudOnLocalhostAndWait(
			final String securityProfile, final String securityFilePath,
			final String username, final String password, final String keystoreFilePath, final String keystorePassword,
			final int timeout, final TimeUnit timeunit) throws CLIException, InterruptedException, TimeoutException {

		final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);

		final List<AbstractManagementServiceInstaller> managementServicesInstallers =
				new LinkedList<AbstractManagementServiceInstaller>();

		setDefaultNicAddress();

		setDefaultLocalcloudLookup();

		if (isWindows()) {
			startManagementOnLocalhostAndWaitInternal(LOCALCLOUD_WIN_MANAGEMENT_ARGUMENTS, securityProfile,
					securityFilePath, username, password, keystoreFilePath, keystorePassword,
					managementServicesInstallers, end, true);
		} else {
			startManagementOnLocalhostAndWaitInternal(LOCALCLOUD_LINUX_MANAGEMENT_ARGUMENTS, securityProfile,
					securityFilePath, username, password, keystoreFilePath, keystorePassword,
					managementServicesInstallers, end, true);
		}

		// validate the services are still up
		waitForManagementServices(managementServicesInstallers, end);
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
		return IPUtils.getSafeIpAddress(nicAddress) + ":" + OpenspacesConstants.DEFAULT_LOCALCLOUD_LUS_PORT;
	}

	/**
	 * Starts management processes (LUS, GSM, ESM) and waits until the requested service installations complete (space,
	 * webui, REST), or until the timeout is reached. The cloud is not a local cloud.
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

		final long end = System.currentTimeMillis() + timeunit.toMillis(timeout);

		try {
			this.cloud = ServiceReader.readCloud(new File(this.cloudFilePath));
		} catch (final IOException e) {
			throw new CLIException("Failed to read cloud file: " + e.getMessage(), e);
		} catch (final DSLException e) {
			throw new CLIException("Failed to read cloud file: " + e.getMessage(), e);
		}

		setDefaultNicAddress();

		// pre validations
		final List<CloudifyMachineValidator> preValidatorsList =
				CloudifyMachineValidatorsFactory.getValidators(true/* isManagement */, nicAddress);
		for (final CloudifyMachineValidator cloudifyMachineValidator : preValidatorsList) {
			cloudifyMachineValidator.validate();
		}

		// if re-bootstrapping a persistent manager, replace rest and webui with new version
		redeployManagement();

		final List<AbstractManagementServiceInstaller> managementServicesInstallers =
				new LinkedList<AbstractManagementServiceInstaller>();
		startManagementOnLocalhostAndWaitInternal(CLOUD_MANAGEMENT_ARGUMENTS, securityProfile, securityFilePath,
				username, password, keystoreFilePath, keystorePassword, managementServicesInstallers, end, false);

		// validate the services are still up
		waitForManagementServices(managementServicesInstallers, end);

	}

	private void redeployManagement() throws CLIException {
		final ManagementRedeployer redeployer = new ManagementRedeployer();
		try {
			redeployer.run(cloud.getConfiguration().getPersistentStoragePath(), Environment.getHomeDirectory());
		} catch (final IOException e) {
			throw new CLIException("Failed to redeploy management: " + e.getMessage(), e);
		}

	}

	/**
	 * Shuts down the local agent, if exists, and waits until shutdown is complete or until the timeout is reached. If
	 * management processes (GSM, ESM, LUS) are still active, the agent is not shutdown and a CLIException is thrown.
	 * 
	 * @param force
	 *            Force the agent to shut down even if the GSC still runs active services
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
	 * Shuts down the local agent, management processes (GSM, ESM, LUS) and GSC. Waits until shutdown is complete or
	 * until the timeout is reached. Active services are forced to shut down.
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
	 * Shuts down the local cloud, and waits until shutdown is complete or until the timeout is reached.
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

		if (NewRestClientUtils.isNewRestClientEnabled()) {
			uninstallNewRestClient(applicationsList, timeout, timeunit, applicationsExist);
		} else {
			uninstall(applicationsList, timeout, timeunit, applicationsExist);
		}

	}

	private void uninstall(
			final Collection<String> applicationsList,
			final long timeout,
			final TimeUnit timeunit,
			final boolean applicationsExist)
			throws InterruptedException, TimeoutException, CLIException {

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

	private void uninstallNewRestClient(
			final Collection<String> applicationsList,
			final long timeout,
			final TimeUnit timeunit,
			final boolean applicationsExist) throws CLIException {
		for (final String application : applicationsList) {
			if (!application.equals(MANAGEMENT_APPLICATION)) {
				final CLIApplicationUninstaller uninstaller = new CLIApplicationUninstaller();
				uninstaller.setRestClient(((RestAdminFacade) adminFacade).getNewRestClient());
				uninstaller.setApplicationName(application);
				uninstaller.setAskOnTimeout(false);
				uninstaller.setInitialTimeout((int) timeout);
				try {
					uninstaller.uninstall();
				} catch (final Exception e) {
					if (force) {
						logger.warning("Failed uninstalling application " + application
								+ ". Teardown will continue");
					} else {
						throw new CLIException(e.getMessage(), e);
					}
				}
			}
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

		if (lookupLocators == null) {
			setLookupLocators(getLocalcloudLookupLocators());
		}

		if (lookupGroups == null) {
			setLookupGroups(getLocalcloudLookupGroups());
		}
	}

	/**
	 * Shuts down the local agent, if exists, and waits until shutdown is complete or until the timeout is reached.
	 * 
	 * @param allowManagement
	 *            Allow the agent to shut down even the management processes (GSM, ESM, LUS) it started are still active
	 * @param allowContainers
	 *            Allow the agent to shut down even the GSC still runs active services
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            the {@link TimeUnit} to use, to calculate the timeout
	 * @throws CLIException
	 *             Reporting a failure to shutdown the agent, or the management/services components still require it
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
	 * Shuts down the given agent, and waits until shutdown is complete or until the timeout is reached.
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
			 * Pings the agent to verify it's not available, indicating it was shut down.
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

//		cleanPUWorkDirectory();
		publishEvent(ShellUtils.getMessageBundle().getString("starting_cloudify_management"));
		runCommand(command, args.toArray(new String[args.size()]), securityProfile, securityFilePath, keystoreFilePath,
				keystorePassword);

	}

	private void cleanPUWorkDirectory() throws CLIStatusException {

		final String puWorkDirectoryName = Environment.getHomeDirectory() + "/work/processing-units";
		final File workDirectory = new File(puWorkDirectoryName);

		if (!workDirectory.exists()) {
			// probably first time local cloud is bootstrapped.
			return;
		}

		if (!workDirectory.isDirectory()) {
			throw new CLIStatusException(CloudifyErrorMessages.MISSING_WORK_DIRECTORY_BEFORE_BOOTSTRAP_LOCALCLOUD,
					puWorkDirectoryName);
		}

		logger.fine("Deleting directories in: " + workDirectory);
		final File[] filesToDelete = workDirectory.listFiles();
		for (final File file : filesToDelete) {
			if (file.isDirectory()) {
				try {
					FileUtils.deleteDirectory(file);
				} catch (final IOException e) {
					throw new CLIStatusException(e,
							CloudifyErrorMessages.FAILED_CLEANING_WORK_DIRECTORY_BEFORE_BOOTSTRAP_LOCALCLOUD,
							file.getAbsolutePath(), e.getMessage());
				}
			}
		}

	}

	private void setIsLocalCloud(final boolean isLocalCloud) {
		this.isLocalCloud = isLocalCloud;
	}

	/**
	 * Starts management processes (LUS, GSM, ESM), and waits until the requested service installations complete (space,
	 * webui, REST), or until the timeout is reached.
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
	 * @param managementServicesInstallers
	 *            an empty list to be populated with the management services installers
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
			final String keystorePassword, final List<AbstractManagementServiceInstaller> managementServicesInstallers,
			final long end, final boolean isLocalCloud)
			throws CLIException, InterruptedException, TimeoutException {

		setIsLocalCloud(isLocalCloud);

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

			// waiting for LUS, GSM services to start
			waitForGsmLus(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end),
					TimeUnit.MILLISECONDS);

			if (isLocalCloud) {
				// container for cloudifyManagementSpace, webui, rest
				startLocalCloudManagementServicesContainerAndWait(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end),
						TimeUnit.MILLISECONDS);
			}
			else if (!noManagementSpaceContainer) {
				// container for cloudifyManagementSpace
				// cloudifyManagementSpace cannot be elastic PU since the ESM now depends on managementSpace for state backup.
				startManagementSpaceContainerAndWait(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end),
						TimeUnit.MILLISECONDS);
			}

			String cloudName = null;
			if (this.cloud != null) {
				cloudName = this.cloud.getName();
			} else {
				cloudName = CloudifyConstants.LOCAL_CLOUD_NAME;
			}

			connectionLogs.supressConnectionErrors();
			try {
				ManagementSpaceServiceInstaller managementSpaceInstaller = null;
				if (!noManagementSpace) {
					
					final boolean highlyAvailable = !isLocalCloud && !notHighlyAvailableManagementSpace;
					managementSpaceInstaller = new ManagementSpaceServiceInstaller();
					managementSpaceInstaller.setAdmin(agent.getAdmin());
					managementSpaceInstaller.setVerbose(verbose);
					managementSpaceInstaller.setProgress(progressInSeconds, TimeUnit.SECONDS);
					managementSpaceInstaller.setServiceName(MANAGEMENT_SPACE_NAME);
					managementSpaceInstaller.setManagementZone(MANAGEMENT_ZONE);
					managementSpaceInstaller.setHighlyAvailable(highlyAvailable);
					managementSpaceInstaller.addListeners(this.eventsListenersList);
					managementSpaceInstaller.setIsLocalCloud(isLocalCloud);
					managementSpaceInstaller.setCloudName(cloudName);

					if (!this.isLocalCloud) {
						final String persistentStoragePath = this.cloud.getConfiguration().getPersistentStoragePath();
						if (persistentStoragePath != null) {
							final String spaceStoragePath = persistentStoragePath + "/management-space/db.h2";
							managementSpaceInstaller.setPersistentStoragePath(spaceStoragePath);
						}
					}
					try {
						managementSpaceInstaller.install();
						managementServicesInstallers.add(managementSpaceInstaller);
					} catch (final ProcessingUnitAlreadyDeployedException e) {
						if (verbose) {
							logger.fine("Service " + MANAGEMENT_SPACE_NAME + " already installed");
							publishEvent("Service " + MANAGEMENT_SPACE_NAME + " already installed");
						}
					}
				}
				
				//wait for ESM we didn't wait before
				waitForEsm(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end),
						TimeUnit.MILLISECONDS);
				
				if (!noWebServices) {
					installWebServices(username, password, isLocalCloud,
							ShellUtils.isSecureConnection(securityProfile), agent, managementServicesInstallers,
							cloudName);
				}

				for (final AbstractManagementServiceInstaller managementServiceInstaller : managementServicesInstallers) {
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

	private void waitForManagementServices(
			final List<AbstractManagementServiceInstaller> managementServicesInstallers, final long end)
			throws CLIException, InterruptedException, TimeoutException {

		final ConnectionLogsFilter connectionLogs = new ConnectionLogsFilter();
		connectionLogs.supressConnectionErrors();
		final Admin admin = createAdmin();

		logger.fine("Starting waitForManagementServices to verify the management services are still up...");

		try {
			setLookupDefaults(admin);
			GridServiceAgent agent = null;
			try {
				try {
					// find the running agent
					logger.fine("Attempting to find the running agent...");
					if (!isLocalCloud || fastExistingAgentCheck()) {
						agent = waitForExistingAgent(admin, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end),
								TimeUnit.MILLISECONDS);
						if (agent == null) {
							// no existing agent running on local machine
							logger.warning("Error! Failed to find a running agent after bootstrap completed");
							throw new CLIValidationException(131,
									CloudifyErrorMessages.POST_BOOTSTRAP_NO_AGENT_FOUND.getName());
						}
						logger.fine("OK, agent was found.");
					}
				} catch (final Exception e) {
					// no existing agent running on local machine
					logger.warning("Error! Failed to find a running agent after bootstrap completed. Reported error: "
							+ e.getMessage());
					throw new CLIValidationException(e, 131,
							CloudifyErrorMessages.POST_BOOTSTRAP_NO_AGENT_FOUND.getName());
				}
			} finally {
				connectionLogs.restoreConnectionErrors();
			}

			try {
				// wait for LUS, GSM and ESM are running
				logger.fine("Attempting to find the running management componenets (LUS, GSM and ESM)...");
				waitForGsmLus(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end), TimeUnit.MILLISECONDS);
				waitForEsm(agent, ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end), TimeUnit.MILLISECONDS);
				logger.fine("OK, LUS, GSM and ESM are up and running.");
			} catch (final Exception e) {
				// LUS, GSM or ESM not found
				logger.log(Level.WARNING,
						"Error! Some management components (LUS/ESM/GSM) are not available after bootstrap "
								+ "completed. Reported error: " + e.getMessage(), e);
				throw new CLIValidationException(e, 132,
						CloudifyErrorMessages.POST_BOOTSTRAP_MISSING_MGMT_COMPONENT.getName());
			}

			connectionLogs.supressConnectionErrors();
			try {
				for (final AbstractManagementServiceInstaller managementServiceInstaller : managementServicesInstallers) {
					String serviceName = "";
					try {
						serviceName = managementServiceInstaller.getServiceName();
						logger.fine("Attempting to find a running management service " + serviceName + "...");
						// validateManagementService(admin, agent, serviceName,
						// ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end), TimeUnit.MILLISECONDS);
						managementServiceInstaller.validateManagementService(admin, agent,
								ShellUtils.millisUntil(TIMEOUT_ERROR_MESSAGE, end), TimeUnit.MILLISECONDS);
						logger.fine("OK, management service " + serviceName + " is up and running.");
					} catch (final Exception e) {
						// management service not found
						logger.warning("Error! Management service " + serviceName
								+ " is not available after bootstrap "
								+ "completed. Reported error: " + e.getMessage());
						throw new CLIValidationException(e, 133,
								CloudifyErrorMessages.POST_BOOTSTRAP_MISSING_MGMT_SERVICE.getName(),
								serviceName);
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
		String lrmiPortRangeCommandLineArgument = "";
		if (!isLocalCloud) {
			lrmiPortRangeCommandLineArgument = "-D" + CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "=";
			final String portRange = System.getenv().get(CloudifyConstants.GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR);
			if (org.apache.commons.lang.StringUtils.isEmpty(portRange)) {
				throw new IllegalArgumentException("Could not find gsc lrmi port range variable in environment.");
			}
			lrmiPortRangeCommandLineArgument += portRange;
		}
		return lrmiPortRangeCommandLineArgument;
	}

	private void installWebServices(final String username, final String password, final boolean isLocalCloud,
			final boolean isSecureConnection, final GridServiceAgent agent,
			final List<AbstractManagementServiceInstaller> managementServices, final String cloudName)
			throws CLIException {
		final String gscLrmiCommandLineArg = getGscLrmiCommandLineArg();
		final String webuiMemory = getWebServiceMemory(CloudifyConstants.WEBUI_MAX_MEMORY_ENVIRONMENT_VAR);
		final int webuiPort = getWebservicePort(CloudifyConstants.WEBUI_PORT_ENV_VAR, isSecureConnection);

		final String webUiFileName = EnvironmentUtils.findWebuiWar();

		final ManagementWebServiceInstaller webuiInstaller = new ManagementWebServiceInstaller();
		webuiInstaller.setAdmin(agent.getAdmin());
		webuiInstaller.setVerbose(verbose);
		webuiInstaller.setProgress(progressInSeconds, TimeUnit.SECONDS);
		webuiInstaller.setMemory(MemoryUnit.toMegaBytes(webuiMemory), MemoryUnit.MEGABYTES);
		webuiInstaller.setPort(webuiPort);
		webuiInstaller.setWarFile(new File(webUiFileName));
		webuiInstaller.setServiceName(CloudifyConstants.MANAGEMENT_WEBUI_SERVICE_NAME);
		webuiInstaller.setManagementZone(MANAGEMENT_ZONE);
		webuiInstaller.addListeners(this.eventsListenersList);
		webuiInstaller.setIsLocalCloud(isLocalCloud);
		webuiInstaller.setIsSecureConnection(isSecureConnection);
		webuiInstaller.setLrmiCommandLineArgument(gscLrmiCommandLineArg);

		webuiInstaller.setCloudName(cloudName);

		try {
			webuiInstaller.installWebService();
		} catch (final ProcessingUnitAlreadyDeployedException e) {
			if (verbose) {
				logger.fine("Service " + CloudifyConstants.MANAGEMENT_WEBUI_SERVICE_NAME + " already installed");
				publishEvent("Service " + CloudifyConstants.MANAGEMENT_WEBUI_SERVICE_NAME + " already installed");
			}
		}
		if (waitForWebUi) {
			managementServices.add(webuiInstaller);
		} else {
			webuiInstaller.logServiceLocation();
		}
		final int restPort = getWebservicePort(CloudifyConstants.REST_PORT_ENV_VAR, isSecureConnection);
		final String restMemory = getWebServiceMemory(CloudifyConstants.REST_MAX_MEMORY_ENVIRONMENT_VAR);

		final ManagementWebServiceInstaller restInstaller = new ManagementWebServiceInstaller();
		restInstaller.setAdmin(agent.getAdmin());
		restInstaller.setProgress(progressInSeconds, TimeUnit.SECONDS);
		restInstaller.setVerbose(verbose);
		restInstaller.setMemory(MemoryUnit.toMegaBytes(restMemory), MemoryUnit.MEGABYTES);
		restInstaller.setPort(restPort);
		restInstaller.setUsername(username);
		restInstaller.setPassword(password);
		restInstaller.setWarFile(new File(REST_FILE));
		restInstaller.setServiceName(CloudifyConstants.MANAGEMENT_REST_SERVICE_NAME);
		restInstaller.setManagementZone(MANAGEMENT_ZONE);
		restInstaller.dependencies.add(CloudifyConstants.MANAGEMENT_SPACE_NAME);
		restInstaller.setWaitForConnection();
		restInstaller.addListeners(this.eventsListenersList);
		restInstaller.setIsLocalCloud(isLocalCloud);
		restInstaller.setIsSecureConnection(isSecureConnection);
		restInstaller.setLrmiCommandLineArgument(gscLrmiCommandLineArg);
		restInstaller.setCloudName(cloudName);

		try {
			restInstaller.installWebService();
		} catch (final ProcessingUnitAlreadyDeployedException e) {
			if (verbose) {
				logger.fine("Service " + CloudifyConstants.MANAGEMENT_REST_SERVICE_NAME + " already installed");
				publishEvent("Service " + CloudifyConstants.MANAGEMENT_REST_SERVICE_NAME + " already installed");
			}
		}
		managementServices.add(restInstaller);
	}

	private String getWebServiceMemory(final String memoryEnvironmentVar) {
		String memoryString;
		if (isLocalCloud) {
			if (memoryEnvironmentVar.equals(CloudifyConstants.REST_MAX_MEMORY_ENVIRONMENT_VAR)) {
				memoryString = LOCALCLOUD_REST_MEMORY;
			} else {
				memoryString = LOCALCLOUD_WEBUI_MEMORY;
			}
		} else {
			memoryString = System.getenv().get(memoryEnvironmentVar);
			if (org.apache.commons.lang.StringUtils.isBlank(memoryString)) {
				throw new IllegalStateException("Could not find web-service memory capacity variable in environment.");
			}

		}
		return memoryString;
	}

	private int getWebservicePort(final String portEnvVriable, final boolean isSecureConnection) {
		int port;
		if (isLocalCloud) {
			if (portEnvVriable.equals(CloudifyConstants.WEBUI_PORT_ENV_VAR)) {
				if (isSecureConnection) {
					port = CloudifyConstants.SECURE_WEBUI_PORT;
				} else {
					port = CloudifyConstants.DEFAULT_WEBUI_PORT;
				}
			} else {
				if (isSecureConnection) {
					port = CloudifyConstants.SECURE_REST_PORT;
				} else {
					port = CloudifyConstants.DEFAULT_REST_PORT;
				}
			}
		} else {
			final String portAsString = System.getenv().get(portEnvVriable);
			if (org.apache.commons.lang.StringUtils.isBlank(portAsString)) {
				throw new IllegalStateException("Could not find web-service port variable in environment");
			}
			port = Integer.parseInt(portAsString);
		}
		return port;
	}

	private void startLocalCloudManagementServicesContainerAndWait(final GridServiceAgent agent, long timeout, TimeUnit timeunit) throws TimeoutException {
		final GridServiceContainerOptions options = new GridServiceContainerOptions().vmInputArgument(
				"-Xmx" + CloudifyConstants.DEFAULT_LOCALCLOUD_REST_WEBUI_SPACE_MEMORY_IN_MB + "m").vmInputArgument(
				"-Xms" + CloudifyConstants.DEFAULT_LOCALCLOUD_REST_WEBUI_SPACE_MEMORY_IN_MB + "m").vmInputArgument(
				"-Dcom.gs.zones="+MANAGEMENT_REST_SERVICE_NAME + "," + MANAGEMENT_WEBUI_SERVICE_NAME + "," + MANAGEMENT_SPACE_NAME);
		final GridServiceContainer container = agent.startGridServiceAndWait(options, timeout, timeunit);
		if (container == null) {
			throw new TimeoutException(CONTAINER_TIMEOUT_ERROR_MESSAGE);
		}
	}
	
	private void startManagementSpaceContainerAndWait(final GridServiceAgent agent, long timeout, TimeUnit timeunit) throws TimeoutException {
		final GridServiceContainerOptions options = new GridServiceContainerOptions().vmInputArgument(
				"-Xmx" + MANAGEMENT_SPACE_MEMORY_IN_MB + "m").vmInputArgument(
				"-Xms" + MANAGEMENT_SPACE_MEMORY_IN_MB + "m").vmInputArgument(
				"-Dcom.gs.zones="+MANAGEMENT_SPACE_NAME);
		final String gscLrmiCommandLineArg = getGscLrmiCommandLineArg();
		if (!org.apache.commons.lang.StringUtils.isEmpty(gscLrmiCommandLineArg)) {
			options.vmInputArgument(gscLrmiCommandLineArg);
		}
		
		final GridServiceContainer container = agent.startGridServiceAndWait(options, timeout, timeunit);
		if (container == null) {
			throw new TimeoutException(CONTAINER_TIMEOUT_ERROR_MESSAGE);
		}
	}

	private boolean fastExistingAgentCheck() {
		if (isLocalCloud) {
			return !ServiceUtils.isPortFree(nicAddress, OpenspacesConstants.DEFAULT_LOCALCLOUD_LUS_PORT);
		}
		return !ServiceUtils.isPortFree(nicAddress,
				this.cloud.getConfiguration().getComponents().getDiscovery().getDiscoveryPort());
	}

	/**
	 * This method assumes that the admin has been supplied with this.lookupLocators and this.lookupGroups and that it
	 * applied the defaults if these were null.
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
	 * Converts the given locators to a String of comma-delimited locator names. The locator names are of this format:
	 * <locator_host>:<locator_port>
	 * 
	 * @param locators
	 *            an array of {@link LookupLocator} objects to convert to String
	 * @return A comma-delimited list of lookup locators
	 */
	public static String convertLookupLocatorToString(final LookupLocator[] locators) {
		final List<String> trimmedLocators = new ArrayList<String>();
		if (locators != null) {
			for (final LookupLocator locator : locators) {
				trimmedLocators.add(IPUtils.getSafeIpAddress(locator.getHost()) + ":" + locator.getPort());
			}
		}
		return StringUtils.collectionToDelimitedString(trimmedLocators, ",");
	}

	/**
	 * Starts an agent on the local host. If an agent is already running, a CLIException is thrown.
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
		final Admin admin = createAdmin(GridServiceAgent.class);
		try {
			setLookupDefaults(admin);

			// pre validations
			final List<CloudifyMachineValidator> preValidatorsList =
					CloudifyMachineValidatorsFactory.getValidators(false/* isManagement */, nicAddress);
			for (final CloudifyMachineValidator cloudifyMachineValidator : preValidatorsList) {
				cloudifyMachineValidator.validate();
			}

			try {
				waitForExistingAgent(admin, WAIT_EXISTING_AGENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				throw new CLIException("Agent already running on local machine. Use shutdown-agent first.");
			} catch (final TimeoutException e) {
				// no existing agent running on local machine
			}
			runGsAgentOnLocalHost("agent", AGENT_ARGUMENTS, securityProfile, ""/* securityFilePath */,
					""/* keystoreFilePath */, keystorePassword);

			// wait for agent to start
			waitForNewAgent(admin, timeout, timeunit);

		} finally {
			admin.close();
			connectionLogs.restoreConnectionErrors();
		}
	}

	private void waitForGsmLus(final GridServiceAgent agent, final long timeout, final TimeUnit timeunit)
			throws TimeoutException, InterruptedException, CLIException {
		waitForManagementProcesses(agent, true, true, false, timeout, timeunit);
	}
	
	private void waitForEsm(final GridServiceAgent agent, final long timeout, final TimeUnit timeunit)
			throws TimeoutException, InterruptedException, CLIException {
		waitForManagementProcesses(agent, false, false, true, timeout, timeunit);
	}
	
	private void waitForManagementProcesses(final GridServiceAgent agent, final boolean waitForLus, final boolean waitForGsm, 
			final boolean waitForEsm, final long timeout, final TimeUnit timeunit)
			throws TimeoutException, InterruptedException, CLIException {

		final Admin admin = agent.getAdmin();

		createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean isDone() throws CLIException, InterruptedException {

				boolean isDone = true;

				if (waitForLus && !isDone(admin.getLookupServices(), "LUS")) {
					if (verbose) {
						logger.fine("Waiting for Lookup Service");
						publishEvent("Waiting for Lookup Service");
					}
					isDone = false;
				}

				if (waitForGsm && !isDone(admin.getGridServiceManagers(), "GSM")) {
					if (verbose) {
						logger.fine("Waiting for Grid Service Manager");
						publishEvent("Waiting for Grid Service Manager");
					}
					isDone = false;
				}

				if (waitForEsm && admin.getElasticServiceManagers().isEmpty()) {
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
							String message = "Detected " + serviceName + " management process started by agent "
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
						|| NetworkUtils.isThisMyIpAddress(agentNicAddress);
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
		String localCloudOptions =
				"-Xmx" + CloudifyConstants.DEFAULT_LOCALCLOUD_GSA_GSM_ESM_LUS_MEMORY_IN_MB + "m" + " -D"
						+ CloudifyConstants.LUS_PORT_CONTEXT_PROPERTY + "="
						+ OpenspacesConstants.DEFAULT_LOCALCLOUD_LUS_PORT + " -D"
						+ GSM_EXCLUDE_GSC_ON_FAILED_INSTANCE + "="
						+ GSM_EXCLUDE_GSC_ON_FAILED_INSTACE_BOOL
						+ " " + GSM_PENDING_REQUESTS_DELAY
						+ " -D" + ZONES_PROPERTY + "=" + LOCALCLOUD_GSA_ZONES
						+ " -D" + CloudifyConstants.SYSTEM_PROPERTY_ESM_DISCOVERY_POLLING_INTERVAL_SECONDS + "=1";

		final Map<String, String> environment = pb.environment();
		if (lookupGroups != null) {
			environment.put("LOOKUPGROUPS", lookupGroups);
		}

		if (lookupLocators != null) {
			final String disableMulticast = "-Dcom.gs.multicast.enabled=false";
			environment.put("LOOKUPLOCATORS", lookupLocators);
			localCloudOptions += " " + disableMulticast;
		}

		if (isLocalCloud) {
			logger.fine("Setting env vars COMPONENT_JAVA_OPTIONS: " + localCloudOptions);
			environment.put("COMPONENT_JAVA_OPTIONS", localCloudOptions);
			environment.put(CloudifyConstants.GIGASPACES_CLOUD_HARDWARE_ID, "localcloud");
			environment.put(CloudifyConstants.GIGASPACES_CLOUD_IMAGE_ID, "localcloud");
			environment.put(CloudifyConstants.GIGASPACES_CLOUD_TEMPLATE_NAME, "localcloud");
			environment.put(CloudifyConstants.GIGASPACES_CLOUD_MACHINE_ID, "localcloud");
			final String springProfiles = createSpringProfilesList(securityProfile);
			environment.put(CloudifyConstants.SPRING_ACTIVE_PROFILE_ENV_VAR, springProfiles);
			if (ShellUtils.isSecureConnection(securityProfile)) {
				environment.put(CloudifyConstants.KEYSTORE_FILE_ENV_VAR, keystoreFilePath);
				environment.put(CloudifyConstants.KEYSTORE_PASSWORD_ENV_VAR, keystorePassword);
			}
			environment.put(CloudifyConstants.SPRING_SECURITY_CONFIG_FILE_ENV_VAR, securityFilePath);
			if (nicAddress != null) {
				final String publicLocalCloudIp =
						getEnvWithDefault(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP, nicAddress);
				final String privateLocalCloudIp =
						getEnvWithDefault(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP, nicAddress);

				environment.put(CloudifyConstants.CLOUDIFY_AGENT_ENV_PUBLIC_IP, publicLocalCloudIp);
				environment.put(CloudifyConstants.GIGASPACES_AGENT_ENV_PUBLIC_IP, publicLocalCloudIp);

				environment.put(CloudifyConstants.GIGASPACES_AGENT_ENV_PRIVATE_IP, privateLocalCloudIp);
				environment.put(CloudifyConstants.CLOUDIFY_AGENT_ENV_PRIVATE_IP, privateLocalCloudIp);

				environment.put("NIC_ADDR", nicAddress);

			}
			// the RMI_OPTIONS is populated with a default value the first time the setenv script is called
			// when the cli starts. When the bootstrap localcloud command calls the agent script, it loads
			// setenv again, but this time the RMI_OPTIONS var is already loaded, and the NIC_ADDR
			// var is not used to create the correct options. The line below should clear this up.
			environment.put("RMI_OPTIONS", "");
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

	private String getEnvWithDefault(final String key, final String defaultValue) {
		String publicLocalCloudIp = defaultValue;
		if (System.getenv().containsKey(key)) {
			publicLocalCloudIp = System.getenv().get(key);
		}
		return publicLocalCloudIp;
	}

	private String createSpringProfilesList(final String securityProfile) {
		// local cloud is always transient
		return securityProfile + "," + CloudifyConstants.PERSISTENCE_PROFILE_TRANSIENT;
	}

	private Admin createAdmin(Class...serviceTypes) {
		final AdminFactory adminFactory = new AdminFactory();
		adminFactory.useGsLogging(false);
		if (lookupGroups != null) {
			adminFactory.addGroups(lookupGroups);
		}

		if (lookupLocators != null) {
			adminFactory.addLocators(lookupLocators);
		}

		adminFactory.setDiscoveryServices(serviceTypes);
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

	/*
	 * public void validateManagementService(final Admin admin, final GridServiceAgent agent, final String serviceName,
	 * final long timeout, final TimeUnit timeunit) throws InterruptedException, TimeoutException, CLIException {
	 * createConditionLatch(timeout, timeunit).waitFor(new ConditionLatch.Predicate() { private boolean messagePublished
	 * = false;
	 * 
	 * /** {@inheritDoc}
	 */
	/*
	 * @Override public boolean isDone() throws CLIException, InterruptedException { logger.fine("Waiting for " +
	 * serviceName + " service."); if (!messagePublished) { messagePublished = true; } final ProcessingUnit pu =
	 * admin.getProcessingUnits().getProcessingUnit(serviceName); boolean isDone = false; if (pu != null) { for (final
	 * ProcessingUnitInstance instance : pu) { GridServiceContainer gsc = instance.getGridServiceContainer(); if (gsc !=
	 * null) { GridServiceAgent gsa = gsc.getGridServiceAgent(); if (gsa != null && (agent.equals(gsa))) { isDone =
	 * true; break; } } } }
	 * 
	 * if (!isDone) { publishEvent(null); }
	 * 
	 * return isDone; } });
	 * 
	 * return; }
	 */
}
