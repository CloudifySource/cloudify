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
package org.cloudifysource.rest.util;

import static com.gigaspaces.log.LogEntryMatchers.regex;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.USMState;
import org.cloudifysource.dsl.internal.EventLogConstants;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.rest.controllers.RestServiceException;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.internal.pu.DefaultProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitInstanceStatistics;
import org.openspaces.admin.pu.ProcessingUnitType;
import org.openspaces.admin.zone.Zone;
import org.openspaces.pu.service.ServiceMonitors;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;

/**
 * the RestPollingRunnable provides a service installation polling mechanism for
 * lifecycle and instance count changes events. the events will be saved in a
 * dedicated LifecycleEventsContainer that will be sampled by the client.
 * 
 * Initialize the Runnable with service names their planned number of instances.
 * 
 * @author adaml
 * 
 */
public class RestPollingRunnable implements Runnable {

	private static final int ONE_SEC = 1;

	private static final int FIVE_SECONDS_MILLI = 5000;

	// a map containing all of the application services and their planned number
	// of instances.
	// The services are ordered according to the installation order defined by
	// the application.
	private final LinkedHashMap<String, Integer> serviceNames;

	private final String applicationName;

	private Admin admin;

	private long endTime;

	private static final String USM_EVENT_LOGGER_NAME = ".*.USMEventLogger.{0}\\].*";

	private boolean isUninstall = false;

	private boolean isSetInstances = false;

	private boolean isServiceInstall = false;

	private LifecycleEventsContainer lifecycleEventsContainer;

	/**
	 * indicates whether thread threw an exception.
	 */
	private Throwable executionException;

	/**
	 * future container polling task.
	 */
	private Future<?> futureTask;

	private boolean isDone = false;

	private final Map<String, Date> gscStartTimeMap = new HashMap<String, Date>();

	private final Object lock = new Object();

	private FutureTask<Boolean> undeployTask;

	private Exception deploymentExecutionException;

	private static final Logger logger = Logger
			.getLogger(RestPollingRunnable.class.getName());

	/**
	 * Create a rest polling runnable to poll for a specific service's
	 * installation lifecycle events with the application name set to the
	 * "default" application name.
	 * 
	 * Use this constructor if polling a single service installation.
	 * 
	 * @param serviceName
	 *            the service name to deploy
	 * @param timeout
	 *            timeout polling timeout.
	 * @param plannedNumberOfInstances
	 *            the planned number of instances.
	 * @param timeunit
	 *            polling timeout timeunit.
	 */
	public RestPollingRunnable(final String applicationName,
			final long timeout, final TimeUnit timeunit) {

		this.serviceNames = new LinkedHashMap<String, Integer>();
		this.applicationName = applicationName;
	}

	/**
	 * sets the admin.
	 * 
	 * @param admin
	 *            admin instance.
	 */
	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

	/**
	 * sets the current lifecycleEventsContainer to be updated by the callable
	 * task.
	 * 
	 * @param lifecycleEventsContainer
	 *            ref to a lifecycleEventsContainer.
	 */
	public void setLifecycleEventsContainer(
			final LifecycleEventsContainer lifecycleEventsContainer) {
		this.lifecycleEventsContainer = lifecycleEventsContainer;
	}

	/**
	 * gets the lifecycle event container.
	 * 
	 * @return the lifecycle event container of the runnable thread
	 */
	public LifecycleEventsContainer getLifecycleEventsContainer() {
		return this.lifecycleEventsContainer;
	}

	/**
	 * Add a service to the polling callable. the service will be sampled until
	 * it reaches it's planned number of instances or until a timeout exception
	 * is thrown.
	 * 
	 * @param serviceName
	 *            The absoulute pu name.
	 * @param plannedNumberOfInstances
	 *            planned number of instances.
	 */
	public void addService(final String serviceName,
			final int plannedNumberOfInstances) {
		this.serviceNames.put(serviceName, plannedNumberOfInstances);
	}

	/**
	 * sets the services to poll and their planned number of instances.
	 * 
	 * @param isServiceInstall
	 *            true if installation is for a single service.
	 */
	public void setIsServiceInstall(final boolean isServiceInstall) {
		this.isServiceInstall = isServiceInstall;
	}

	/**
	 * Set to true if the action invoked was setInstances.
	 * 
	 * @param isSetInstances
	 *            is the action a set instances action. default is set to false.
	 */
	public void setIsSetInstances(final boolean isSetInstances) {
		this.isSetInstances = isSetInstances;

	}

	/**
	 * Sets the runnable's lifetime lease.
	 * 
	 * @param timeout
	 *            timeout period
	 * @param timeunit
	 *            timeout timeunit
	 */
	public synchronized void setEndTime(final long timeout,
			final TimeUnit timeunit) {
		this.endTime = System.currentTimeMillis() + timeunit.toMillis(timeout);
	}

	/**
	 * returns true if the task is done.
	 * 
	 * @return true if thread has ended, false otherwise.
	 */
	public boolean isDone() {
		return this.isDone;
	}

	/**
	 * Extends the runnable's lifetime lease.
	 * 
	 * @param timeout
	 *            timeout period
	 * @param timeunit
	 *            timeout timeunit
	 */
	public synchronized void increaseEndTimeBy(final long timeout,
			final TimeUnit timeunit) {
		this.endTime = endTime + timeunit.toMillis(timeout);
	}

	/**
	 * Returns the thread's end time.
	 * 
	 * @return the thread's end time.
	 */
	public synchronized long getEndTime() {
		return endTime;
	}

	private void setExecutionException(final Throwable e) {
		synchronized (this.lock) {
			this.executionException = e;
		}
	}

	/**
	 * gets the execution exception that occurred on the polling thread.
	 * 
	 * @return the execution exception that occurred on the polling thread or
	 *         null
	 */
	public ExecutionException getExecutionException() {
		synchronized (this.lock) {
			if (this.executionException == null) {
				return null;
			}
			return new ExecutionException(this.executionException);
		}
	}

	public void setFutureTask(final Future<?> future) {
		this.futureTask = future;
	}

	@Override
	public void run() {

		try {
			if (this.serviceNames.isEmpty()) {
				logger.log(Level.INFO,
						"Polling for lifecycle events has ended successfully."
								+ " Terminating the polling task");
				// We stop the thread from being scheduled again.
				throw new RestServiceException("Polling has ended successfully");
			}

			if (System.currentTimeMillis() > this.endTime) {
				throw new TimeoutException("Timed out");
			}
			
			if (this.deploymentExecutionException != null) {
				throw new Exception(deploymentExecutionException);
			}

			pollForLogs();

		} catch (final Throwable e) {
			if (!(e instanceof RestServiceException)) {
				logger.log(
						Level.INFO,
						"Polling task ended unexpectedly. Reason: "
								+ e.getMessage(), e);
				setExecutionException(e);
			} else {
				logger.log(Level.INFO, "Polling task ended successfully.");
			}
			terminateTaskGracefully();
			// this exception should not be caught. it is meant to make the
			// scheduler stop
			// the thread execution.
			throw new RuntimeException(e);

		}

	}

	private void terminateTaskGracefully() {
		this.isDone = true;
		if (this.futureTask != null) {
			this.futureTask.cancel(true);
		}
	}

	/**
	 * Goes over each service defined prior to the callable execution and polls
	 * it for lifecycle and instance count events.
	 * 
	 * @throws ExecutionException
	 */
	private void pollForLogs() throws ExecutionException {

		final LinkedHashMap<String, Integer> serviceNamesClone = new LinkedHashMap<String, Integer>();
		serviceNamesClone.putAll(this.serviceNames);

		for (final String serviceName : serviceNamesClone.keySet()) {

			addServiceLifecycleLogs(serviceName);

			final int plannedNumberOfInstances = getPlannedNumberOfInstances(serviceName);
			final int numberOfServiceInstances = getNumberOfServiceInstances(serviceName);
			final int numberOfFailedInstances = getNumberOfFailedInstances(serviceName);

			addServiceInstanceCountEvents(serviceName,
					plannedNumberOfInstances, numberOfServiceInstances, numberOfFailedInstances);

			removeEndedServicesFromPollingList(serviceName,
					plannedNumberOfInstances, numberOfServiceInstances,
					numberOfFailedInstances);
		}
	}

	private void removeEndedServicesFromPollingList(final String serviceName,
			final int plannedNumberOfInstances,
			final int numberOfServiceInstances,
			final int numberOfFailedInstances) throws ExecutionException {

		if (isUninstall) {
			final String absolutePuName = ServiceUtils.getAbsolutePUName(
					applicationName, serviceName);
			try {
				final Boolean undeployedSuccessfully = this.undeployTask
						.get(ONE_SEC, TimeUnit.SECONDS);
				if (undeployedSuccessfully) {
					logger.info("undeployAndWait for processing unit " + absolutePuName + " has finished");
					this.serviceNames.remove(serviceName);
					this.lifecycleEventsContainer
						.addNonLifecycleEvents("Service \"" + serviceName
							+ "\" uninstalled successfully");
				}
			} catch (final Exception e) {
				if (e instanceof TimeoutException) {
					logger.info("undeployAndWait for processing unit " + absolutePuName + " has not finished yet");
				} else {
					final String message = "undeploy task has ended unsuccessfully. "
							+ "Some machines may not have been terminated!";
					logger.log(Level.WARNING, message, e);
					lifecycleEventsContainer.addNonLifecycleEvents(message);
					throw new ExecutionException(message, e);
				}
			}
			
		} else {
			if (plannedNumberOfInstances == numberOfServiceInstances
					+ numberOfFailedInstances) {
				this.serviceNames.remove(serviceName);
			}
		}
	}

	private void addServiceLifecycleLogs(final String serviceName) {
		List<Map<String, String>> servicesLifecycleEventDetailes;
		servicesLifecycleEventDetailes = new ArrayList<Map<String, String>>();
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				this.applicationName, serviceName);
		logger.log(Level.FINE, "Polling for lifecycle events on service: "
				+ absolutePuName);
		final Zone zone = admin.getZones().getByName(absolutePuName);
		if (zone == null) {
			logger.log(Level.FINE, "Zone " + absolutePuName + " does not exist. not polling for logs");
			return;
		}
		// TODO: this is not very efficient. Maybe possible to move the
		// LogEntryMatcher
		// as field add to init to call .
		final String regex = MessageFormat.format(USM_EVENT_LOGGER_NAME,
				absolutePuName);
		final LogEntryMatcher matcher = regex(regex);
		for (final GridServiceContainer container : zone
				.getGridServiceContainers()) {
			logger.log(Level.FINE,
					"Polling GSC with uid: " + container.getUid());

			final Date pollingStartTime = getGSCSamplingStartTime(container);
			final LogEntries logEntries = container.logEntries(matcher);
			// Get lifecycle events.
			for (final LogEntry logEntry : logEntries) {
				if (logEntry.isLog()) {
					if (pollingStartTime.before(new Date(logEntry
							.getTimestamp()))) {
						final Map<String, String> serviceEventsMap = getEventDetailes(
								logEntry, container, absolutePuName);
						servicesLifecycleEventDetailes.add(serviceEventsMap);
					}
				}
			}

			this.lifecycleEventsContainer
					.addLifecycleEvents(servicesLifecycleEventDetailes);
		}
	}

	// Returns the time the polling started for the specific gsc.
	private Date getGSCSamplingStartTime(final GridServiceContainer gsc) {
		final String uid = gsc.getUid();
		if (this.gscStartTimeMap.containsKey(uid)) {
			return this.gscStartTimeMap.get(uid);
		} else {
			final Date date = new Date(new Date().getTime()
					+ gsc.getOperatingSystem().getTimeDelta()
					- FIVE_SECONDS_MILLI);
			this.gscStartTimeMap.put(uid, date);
			return date;
		}
	}

	private void addServiceInstanceCountEvents(final String serviceName,
			final int plannedNumberOfInstances,
			final int numberOfServiceInstances,
			final int numberOfFailedInstances) {

		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);

		if (numberOfServiceInstances == 0) {
			if (!isUninstall) {
				this.lifecycleEventsContainer
						.addNonLifecycleEvents("Deploying " + serviceName
								+ " with " + plannedNumberOfInstances
								+ " planned instances.");
			}
		} else {
			String event = "[" + serviceName + "] " + "Deployed "
					+ numberOfServiceInstances + " planned "
					+ plannedNumberOfInstances;
			if (numberOfFailedInstances > 0) {
				event += " failed " + numberOfFailedInstances;
			}
			this.lifecycleEventsContainer.addNonLifecycleEvents(event);
		}

		if (plannedNumberOfInstances == numberOfServiceInstances) {
			if (!isServiceInstall) {
				if (!isUninstall && !isSetInstances) {
					this.lifecycleEventsContainer
							.addNonLifecycleEvents("Service \"" + serviceName
									+ "\" successfully installed ("
									+ numberOfServiceInstances + " Instances)");
				}
			}
		}

		final Zone zone = admin.getZones().getByName(absolutePuName);
		if (zone == null) {
			logger.log(Level.FINE, "Zone " + absolutePuName + " does not exist. " 
					+ "this means processing unit instance was removed.");
			// now waiting for machine to shutdown
			if (isUninstall) {
				this.lifecycleEventsContainer
					.addNonLifecycleEvents("Service \"" + serviceName
						+ "\" was stopped successfully , releasing cloud resources...");				
			}
		}
	}

	/**
	 * The planned number of service instances is saved to the serviceNames map
	 * during initialization of the callable. in case of datagrid deployment,
	 * the planned number of instances will be reviled only after it's PU has
	 * been created and so we need to poll the pu to get the correct number of
	 * planned instances in case the pu is of type datagrid.
	 * 
	 * @param serviceName
	 *            The service name
	 * @return planned number of service instances
	 */
	private int getPlannedNumberOfInstances(final String serviceName) {
		if (isUninstall) {
			return 0;
		}
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		final ProcessingUnit processingUnit = admin.getProcessingUnits()
				.getProcessingUnit(absolutePuName);
		if (processingUnit != null) {
			final Map<String, String> elasticProperties = ((DefaultProcessingUnit) processingUnit)
					.getElasticProperties();
			if (elasticProperties.containsKey("schema")) {
				final String clusterSchemaValue = elasticProperties
						.get("schema");
				if ("partitioned-sync2backup".equals(clusterSchemaValue)) {
					return processingUnit.getTotalNumberOfInstances();
				}
			}
		}

		if (serviceNames.containsKey(serviceName)) {
			return serviceNames.get(serviceName);
		}
		throw new IllegalStateException(
				"Service planned number of instances is undefined");

	}

	/**
	 * Gets the service instance count for every type of service (USM/Other). if
	 * the service pu is not running yet, returns 0.
	 * 
	 * @param absolutePuName
	 *            The absolute service name.
	 * @return the service's number of running instances.
	 */
	private int getNumberOfServiceInstances(final String serviceName) {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		final ProcessingUnit processingUnit = admin.getProcessingUnits()
				.getProcessingUnit(absolutePuName);

		if (processingUnit != null) {
			if (processingUnit.getType() == ProcessingUnitType.UNIVERSAL) {
				return getNumberOfUSMServicesWithState(absolutePuName,
						USMState.RUNNING);
			}

			return admin.getProcessingUnits().getProcessingUnit(absolutePuName)
					.getInstances().length;
		}
		return 0;
	}

	private int getNumberOfFailedInstances(final String serviceName) {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		final ProcessingUnit processingUnit = admin.getProcessingUnits()
				.getProcessingUnit(absolutePuName);

		if (processingUnit != null) {
			if (processingUnit.getType() == ProcessingUnitType.UNIVERSAL) {
				return getNumberOfUSMServicesWithState(absolutePuName,
						USMState.ERROR);
			}

			return 0;
		}
		return 0;
	}

	// returns the number of RUNNING processing unit instances.

	private int getNumberOfUSMServicesWithState(final String absolutePUName,
			final USMState state) {
		int puInstanceCounter = 0;
		final ProcessingUnit processingUnit = admin.getProcessingUnits()
				.getProcessingUnit(absolutePUName);

		for (final ProcessingUnitInstance pui : processingUnit) {
			// TODO: get the instanceState step
			if (isUsmInState(pui, state)) {
				puInstanceCounter++;
			}
		}
		return puInstanceCounter;
	}

	private boolean isUsmInState(final ProcessingUnitInstance pui,
			final USMState state) {
		final ProcessingUnitInstanceStatistics statistics = pui.getStatistics();
		if (statistics == null) {
			return false;
		}
		final Map<String, ServiceMonitors> puMonitors = statistics
				.getMonitors();
		if (puMonitors == null) {
			return false;
		}
		final ServiceMonitors serviceMonitors = puMonitors.get("USM");
		if (serviceMonitors == null) {
			return false;
		}
		final Map<String, Object> monitors = serviceMonitors.getMonitors();
		if (monitors == null) {
			return false;
		}

		@SuppressWarnings("boxing")
		final int instanceState = (Integer) monitors
				.get(CloudifyConstants.USM_MONITORS_STATE_ID);
		if (CloudifyConstants.USMState.values()[instanceState] == state) {
			return true;
		}
		return false;
	}

	/**
	 * tells the polling task to expect uninstall or install of service. the
	 * default value is set to false.
	 * 
	 * @param isUninstall
	 *            is the task being preformed an uninstall task.
	 */
	public void setIsUninstall(final boolean isUninstall) {
		this.isUninstall = isUninstall;
	}

	/**
	 * generates a map containing all of the event's details.
	 * 
	 * @param logEntry
	 *            The event log entry originated from the GSC log
	 * @param container
	 *            the GSC of the specified event
	 * @param absolutePuName
	 *            the absolute processing unit name.
	 * @return returns a details map containing all of an events details.
	 */
	private Map<String, String> getEventDetailes(final LogEntry logEntry,
			final GridServiceContainer container, final String absolutePuName) {

		final Map<String, String> returnMap = new HashMap<String, String>();

		returnMap.put(EventLogConstants.getTimeStampKey(),
				Long.toString(logEntry.getTimestamp()));
		returnMap.put(EventLogConstants.getMachineHostNameKey(), container
				.getMachine().getHostName());
		returnMap.put(EventLogConstants.getMachineHostAddressKey(), container
				.getMachine().getHostAddress());
		returnMap.put(EventLogConstants.getServiceNameKey(),
				ServiceUtils.getApplicationServiceName(absolutePuName,
						this.applicationName));
		// The string replacement is done since the service name that is
		// received from the USM logs derived from actual PU name.
		final String serviceName = returnMap.get(EventLogConstants.getServiceNameKey()) + "-";
		final String originalText = logEntry.getText();
		final String modifiedText = originalText.replaceFirst(
				absolutePuName + "-",
				serviceName);
		returnMap.put(
				EventLogConstants.getEventTextKey(),
				modifiedText);

		return returnMap;
	}

	public void setUndeployTask(final FutureTask<Boolean> undeployTask) {
		this.undeployTask = undeployTask;

	}

	/**
	 * Sets a deployment exception for a specific deployment process.
	 * @param deploymentExecutionException
	 * 		the deployment exception.
	 */
	public void setDeploymentExecutionException(
			final Exception deploymentExecutionException) {
		this.deploymentExecutionException = deploymentExecutionException;
	}
}
