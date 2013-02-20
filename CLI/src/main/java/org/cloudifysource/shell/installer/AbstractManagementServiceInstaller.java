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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.shell.AdminFacade;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.commands.CLIException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.core.util.MemoryUnit;

/**
 * @author eitany
 * @since 2.0.0
 * 
 *        This abstract is the skeleton of a management service installer, and includes the basic members that
 *        every management service installer use: {@link Admin}, a definition of memory quota, a service
 *        name and a zone name (might be identical to the service name).
 * 
 *        Installers extending this skeleton must implement install() and 
 *        waitForInstallation(AdminFacade, GridServiceAgent, long, TimeUnit)
 */
public abstract class AbstractManagementServiceInstaller {

	protected final Logger logger = Logger.getLogger(this.getClass().getName());
	protected Admin admin;
	protected boolean verbose;
	protected int memoryInMB;
	protected String serviceName;
	protected String agentZone;
	protected long progressInSeconds;
	protected List<String> dependencies = new ArrayList<String>();
	
	/**
	 * The name of the management application.
	 */
	public static final String MANAGEMENT_APPLICATION_NAME = "management";
	private static final String TIMEOUT_ERROR_MESSAGE = "operation timed out waiting for management service to start";
	protected static final int RESERVED_MEMORY_IN_MB = 256;

	/**
	 * Empty Constructor.
	 */
	public AbstractManagementServiceInstaller() {
		super();
	}

	/**
	 * Sets the {@link Admin} object used to access the Admin API.
	 * 
	 * @param admin
	 *            an Admin object
	 */
	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

	/**
	 * Sets the verbose mode for extended logging.
	 * 
	 * @param verbose
	 *            mode (true - on, false - off)
	 */
	public void setVerbose(final boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Set the memory in various memory units.
	 * 
	 * @param memory
	 *            number of memory units
	 * @param unit
	 *            Memory unit to use
	 */
	public void setMemory(final long memory, final MemoryUnit unit) {
		this.memoryInMB = (int) unit.toMegaBytes(memory);
	}

	/**
	 * Sets the name of the service.
	 * 
	 * @param serviceName
	 *            The name of the service
	 */
	public void setServiceName(final String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Sets the zone.
	 * 
	 * @param zone
	 *            Zone name
	 */
	public void setManagementZone(final String zone) {
		this.agentZone = zone;
	}

	/**
	 * Set the progress (polling interval), in various time units.
	 * 
	 * @param progress
	 *            Number of {@link TimeUnit}s
	 * @param timeunit
	 *            The time unit to use
	 */
	public void setProgress(final int progress, final TimeUnit timeunit) {
		this.progressInSeconds = timeunit.toSeconds(progress);
	}

	/**
	 * Installs the management service.
	 * 
	 * @throws CLIException
	 *             Reporting a failure to install the management service
	 * @throws ProcessingUnitAlreadyDeployedException
	 *             The management service is already installed
	 */
	public abstract void install() throws CLIException, ProcessingUnitAlreadyDeployedException;

	/**
	 * Waits for the installation of an PU or management space to complete.
	 * 
	 * @param adminFacade
	 *            Admin facade to use for deployment
	 * @param agent
	 *            A grid service agent
	 * @param timeout
	 *            number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            The time unit to use
	 * @throws InterruptedException
	 *             Thrown when the thread is interrupted
	 * @throws TimeoutException
	 *             Reporting the time out was reached
	 * @throws CLIException
	 *             Reporting a failure to check the installation progress
	 */
	public abstract void waitForInstallation(AdminFacade adminFacade, GridServiceAgent agent, long timeout, 
			TimeUnit timeunit) throws InterruptedException, TimeoutException, CLIException;

	/**
	 * Gets a Grid Service Manager to deploy the service.
	 * 
	 * @return GridServiceManager to deploy the service
	 * @throws CLIException
	 *             Reporting a failure to find a Grid Service Manager
	 */
	protected GridServiceManager getGridServiceManager() throws CLIException {
		final Iterator<GridServiceManager> it = admin.getGridServiceManagers().iterator();
		if (it.hasNext()) {
			return it.next();
		}
		throw new CLIException("No Grid Service Manager found to deploy " + serviceName);
	}

	/**
	 * Create a properties object with the property "com.gs.application=management".
	 * 
	 * @return populated Properties object
	 */
	protected Properties getContextProperties() {
		final Properties props = new Properties();
		props.setProperty("com.gs.application", MANAGEMENT_APPLICATION_NAME);
		return props;
	}

	/**
	 * Creates a {@link ConditionLatch} object, intended to wait for procedures to complete.
	 * 
	 * @param timeout
	 *            The number of {@link TimeUnit}s to wait
	 * @param timeunit
	 *            The type of {@link TimeUnit} to use
	 * @return The configured condition latch object
	 */
	protected ConditionLatch createConditionLatch(final long timeout, final TimeUnit timeunit) {
		return new ConditionLatch().timeout(timeout, timeunit).pollingInterval(progressInSeconds, TimeUnit.SECONDS)
				.timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE).verbose(verbose);
	}

}