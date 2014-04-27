/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.utilitydomain.admin;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.jini.core.discovery.LookupLocator;

import org.apache.commons.lang.StringUtils;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.space.Space;
import org.openspaces.security.AdminFilter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wraps the {@link Admin} object in order to monitor the object usage and close it after it is no longer in use.
 * This is intended to minimize memory and network utilization by unused {@link Admin} objects.
 * 
 * @author noak
 * @since 9.7.1
 */
public class TimedAdmin {

	private static Logger logger = Logger.getLogger(TimedAdmin.class.getName());
	private volatile boolean running = true; 
	// TODO noak: make configurable
	private static final long MAX_IDLE_TIME_MILLIS = 60 * 1000; // defaults to 60 seconds
	private static final long POLLING_INTERVAL_MILLIS = 10 * 1000; // defaults to 1 second

	private long lastUsed = System.currentTimeMillis();
	private Admin admin;
	
	private boolean discoverUnmanagedSpaces;
	private int statisticsHistorySize = Admin.DEFAULT_HISTORY_SIZE;
	private String groups;
	private String locators;
	private Class[] discoveryServices;
	private AdminFilter adminFilter;
	private ExecutorService executor;
	
	
	public void setDiscoveryServices(final Class[] discoveryServices) {
		this.discoveryServices = discoveryServices;
	}
	
	public void setStatisticsHistorySize(int statisticsHistorySize) {
		this.statisticsHistorySize = statisticsHistorySize;
	}
	
	public String[] getAdminGroups() {
		if (admin != null) {
			return admin.getGroups();
		}
		return null;
	}
	
	public void setGroups(final String groups) {
		this.groups = groups;
	}
	
	
	public LookupLocator[] getAdminLocators() {
		if (admin != null) {
			return admin.getLocators();
		}
		return null;
	}

	public void setLocators(final String locators) {
		this.locators = locators;
	}

	public void setAdminFilter(final AdminFilter adminFilter) {
		this.adminFilter = adminFilter;
	}
	
	
	public void discoverUnmanagedSpaces() {
		this.discoverUnmanagedSpaces = true;
	}
	
	


	/***********
	 * Creates an admin instance if required.
	 * A timing thread is also created to monitor the admin expity time and terminate it if needed. 
	 */
	private synchronized void initAdmin() {
		logger.finest("getting admin object");
		if (admin == null) {
			createAdmin();
		} else {
			logger.info("Using a cached Admin object");
		}
		updateTimestamp();
	}
	
	
	private void createAdmin() {
		logger.info("Creating a new Admin object...");
		
		final AdminFactory factory = new AdminFactory();
		factory.useDaemonThreads(true);
		
		if (StringUtils.isNotBlank(groups)) {
			factory.addGroups(groups);
		}
		
		if (StringUtils.isNotBlank(locators)) {
			factory.addLocators(locators);
		}
		
		if (adminFilter != null) {
			factory.adminFilter(adminFilter);
		}
		
		if (discoveryServices != null) {
			factory.setDiscoveryServices(discoveryServices);	
		}

		if (discoverUnmanagedSpaces) {
			factory.discoverUnmanagedSpaces();
		}
		
		this.admin = factory.createAdmin();		
		this.admin.setStatisticsHistorySize(statisticsHistorySize);
				
		logger.info("Created new Admin Object with groups: " + Arrays.toString(this.admin.getGroups()) + " and Locators: "
				+ Arrays.toString(this.admin.getLocators()));
		
		updateTimestamp();
		startTimingThread();
	}

	
	/**
	 * Creates and starts a thread that monitors the admin object usage - if the object was not used for longer than 
	 * the maximum idle time, the object is closed and nullified.
	 */
	private synchronized void startTimingThread() {
		executor = Executors.newFixedThreadPool(1);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				while (running) {
					try {
						if (admin != null && (lastUsed + MAX_IDLE_TIME_MILLIS < System.currentTimeMillis())) {
							logger.info("Closing expired admin object");
							admin.close();
							admin = null;
						}
						Thread.sleep(POLLING_INTERVAL_MILLIS);
					} catch (final InterruptedException e) {
						// ignore
					}
				}
			}
		});
		
		executor.shutdown();
	}
	
	
	/**
	 * Waits until a space by the specified name is found, or the timeout is reached.
	 * @param spaceName The name of the requested space
	 * @param timeout The timeout length
	 * @param timeunit The timeout length time unit
	 * @return The space, if found in the given time frame; null otherwise
	 */
	public Space waitForSpace(final String spaceName, final long timeout, final TimeUnit timeunit) {
		validateTimeout(timeout, timeunit, "waiting for space instance");
		initAdmin();
		return admin.getSpaces().waitFor(spaceName, timeout, timeunit);
	}
	
	
	/**
	 * Returns a space based on its name.
	 * @param spaceName The name of the requested space
	 * @return The space if found; null otherwise
	 */
	public Space getSpaceByName(final String spaceName) {
		initAdmin();
		return admin.getSpaces().getSpaceByName(spaceName);
	}
	

	/**
	 * Waits until a processing unit by the specified name is found, or the timeout is reached.
	 * @param puName The name of the requested space
	 * @param timeout The timeout length
	 * @param timeunit The timeout length time unit
	 * @return The processing unit, if found in the given time frame; null otherwise
	 */
	public ProcessingUnit waitForPU(final String puName, final long timeout, final TimeUnit timeunit) {
		validateTimeout(timeout, timeunit, "waiting for PU instance");
		initAdmin();
		return admin.getProcessingUnits().waitFor(puName, timeout, timeunit);
	}
	
	
	/**
	 * Waits until all lookup services are found, or the timeout is reached.
	 * @param numberOfLookupServices The number of requested lookup services
	 * @param timeout The timeout length
	 * @param timeunit The timeout length time unit
	 * @return True if all lookup services were found; false otherwise
	 */
	public boolean waitForLookupServices(int numberOfLookupServices, long timeout, TimeUnit timeunit) {
		validateTimeout(timeout, timeunit, "waiting for lookup service");
		initAdmin();
		return admin.getLookupServices().waitFor(numberOfLookupServices, timeout, timeunit);
	}
	
	
	/**
	 * Waits until an {@link ElasticServiceManager} is found, or the timeout is reached.
	 * @return The ElasticServiceManager if found in the given time frame; null otherwise
	 */
	public ElasticServiceManager waitForElasticServiceManager() {
		initAdmin();
		return admin.getElasticServiceManagers().waitForAtLeastOne();
	}
	
	
	/**
	 * Returns the Admin Object the underlies the TimedAdmin. Note: this is intended as a debugging aid only!
	 * This bypasses the timing mechanism and might result in a premature termination of the object!
	 *
	 * @return the admin.
	 */
	public Admin getInnerAdminObject() {
		logger.warning("Accessing the admin object underlying a timed admin is not recommended!"
				+ " This action is bypasses the timing mechanism and might result in a premature admin termination!");
		initAdmin();
		return admin;
	}
	
	
	/**
	 * Closes the admin object and stops the timing thread.
	 */
	public void close() {
		logger.info("Closing the admin object and stopping the timing thread");
		if (admin != null) {
			admin.close();
			admin = null;
		}
		
		executor.shutdown();
	}
	
	
	/**
	 * Returns the state of the underlying admin object: if it's set - return true, otherwise return false.
	 * @return If the admin is set (not null) - return true, otherwise return false
	 */
	public boolean isAdminObjectAlive() {
		if (admin == null) {
			return false;
		} else {
			return true;
		}
	}
	
	
	private synchronized void updateTimestamp() {
		lastUsed = System.currentTimeMillis();
	}
	
	
	/**
	 * Validates the given action timeout is not shorter than the admin timeout, and issues a warning if it is.
	 */
	private void validateTimeout(final long timeout, final TimeUnit timeunit, final String actionDescription) {
		if (timeunit.toMillis(timeout) >= MAX_IDLE_TIME_MILLIS) {
			logger.warning("Admin object might expire prematurely! The specified timeout for " + actionDescription 
					+ " was set to " + timeout + " " + timeunit.toString() + " while the admin timeout is " 
					+ MAX_IDLE_TIME_MILLIS/1000 + " seconds");
		}
	}

}
