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
package org.cloudifysource.usm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.annotation.PostConstruct;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.usm.details.Details;
import org.cloudifysource.usm.dsl.DSLCommandsLifecycleListener;
import org.cloudifysource.usm.dsl.DSLConfiguration;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.InitListener;
import org.cloudifysource.usm.events.InstallListener;
import org.cloudifysource.usm.events.LifecycleEvents;
import org.cloudifysource.usm.events.PostInstallListener;
import org.cloudifysource.usm.events.PostStartListener;
import org.cloudifysource.usm.events.PostStopListener;
import org.cloudifysource.usm.events.PreInstallListener;
import org.cloudifysource.usm.events.PreServiceStartListener;
import org.cloudifysource.usm.events.PreServiceStopListener;
import org.cloudifysource.usm.events.PreStartListener;
import org.cloudifysource.usm.events.PreStopListener;
import org.cloudifysource.usm.events.ShutdownListener;
import org.cloudifysource.usm.events.StartReason;
import org.cloudifysource.usm.events.StopReason;
import org.cloudifysource.usm.events.USMEvent;
import org.cloudifysource.usm.installer.USMInstaller;
import org.cloudifysource.usm.launcher.ProcessLauncher;
import org.cloudifysource.usm.liveness.LivenessDetector;
import org.cloudifysource.usm.locator.ProcessLocator;
import org.cloudifysource.usm.monitors.Monitor;
import org.cloudifysource.usm.shutdown.ProcessKiller;
import org.cloudifysource.usm.stopDetection.StopDetector;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class USMLifecycleBean implements ClusterInfoAware {

	@Autowired(required = true)
	private UniversalServiceManagerConfiguration configuration;
	@Autowired(required = true)
	private USMComponent[] components = new USMComponent[0];
	// /////////////////////////////
	// Lifecycle Implementations //
	// /////////////////////////////
	@Autowired(required = true)
	private USMInstaller installer = null;
	@Autowired(required = true)
	private ProcessLauncher launcher = null;
	@Autowired(required = true)
	private ProcessKiller processKiller = null;
	@Autowired(required = false)
	private LivenessDetector[] livenessDetectors = new LivenessDetector[0];
	@Autowired(required = false)
	private final StopDetector[] stopDetectors = new StopDetector[0];
	@Autowired(required = false)
	private final ProcessLocator[] processLocators = new ProcessLocator[0];

	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(USMLifecycleBean.class
			.getName());
	// Initialized in getClusterInfo
	private java.util.logging.Logger eventLogger;

	private String puName;
	private Integer instanceId;

	// ////////////////////////
	// Lifecycle Events //////
	// ////////////////////////
	@Autowired(required = false)
	private InitListener[] initListeners = new InitListener[0];
	@Autowired(required = false)
	private PreInstallListener[] preInstallListeners = new PreInstallListener[0];
	@Autowired(required = false)
	private final InstallListener[] installListeners = new InstallListener[0];
	@Autowired(required = false)
	private PostInstallListener[] postInstallListeners = new PostInstallListener[0];
	@Autowired(required = false)
	private PreStartListener[] preStartListeners = new PreStartListener[0];
	@Autowired(required = false)
	private PostStartListener[] postStartListeners = new PostStartListener[0];
	@Autowired(required = false)
	private PreStopListener[] preStopListeners = new PreStopListener[0];
	@Autowired(required = false)
	private PostStopListener[] postStopListeners = new PostStopListener[0];
	@Autowired(required = false)
	private ShutdownListener[] shutdownListeners = new ShutdownListener[0];
	@Autowired(required = false)
	private final PreServiceStartListener[] preServiceStartListeners = new PreServiceStartListener[0];
	@Autowired(required = false)
	private final PreServiceStopListener[] preServiceStopListeners = new PreServiceStopListener[0];

	// ////////////
	// Monitors //
	// ////////////
	@Autowired(required = false)
	private Monitor[] monitors = new Monitor[0];

	// ///////////////////
	// Service Details //
	// ///////////////////
	@Autowired(required = false)
	private Details[] details = new Details[0];
	private String eventPrefix;

	/**********
	 * Post construct method.
	 */
	@PostConstruct
	public void init() {
		if (this.eventLogger == null) {
			// This can only happen in the integrated container
			this.eventLogger =
					java.util.logging.Logger.getLogger(USMLifecycleBean.class.getPackage().getName()
							+ ".USMEventLogger.USM");

		}
		if (puName != null) {
			this.eventPrefix = puName + "-" + this.instanceId + " ";
		} else {
			this.eventPrefix = "USM-1 ";
		}

	}

	private boolean isLoggableEvent(final LifecycleEvents event, final USMEvent[] listeners) {

		if (eventLogger.isLoggable(Level.INFO)) {
			if (listeners != null) {
				if (listeners.length > 0) {
					if (listeners.length == 1) {
						// check for a DSL lifecycle listener
						if (listeners[0] instanceof DSLCommandsLifecycleListener) {
							final DSLCommandsLifecycleListener dslListener =
									(DSLCommandsLifecycleListener) listeners[0];
							if (!dslListener.isEventExists(event)) {
								return false;
							}
						}
					}
					return true;
				}
			}

		}

		return false;
	}

	private void logEventStart(final LifecycleEvents event, final USMEvent[] listeners) {
		if (isLoggableEvent(
				event, listeners)) {
			eventLogger.info(eventPrefix + event.toString() + " invoked");
		}
	}

	/********
	 * logs the start event.
	 */
	public void logProcessStartEvent() {
		if (eventLogger.isLoggable(Level.INFO)) {
			eventLogger.info(eventPrefix + "START invoked");
		}
	}

	/*******
	 * Logs the start failed event.
	 * 
	 * @param exceptionMessage start failure description.
	 */
	public void logProcessStartFailureEvent(final String exceptionMessage) {
		if (eventLogger.isLoggable(Level.INFO)) {
			eventLogger.info(eventPrefix + "START failed. Reason: " + exceptionMessage);
		}
	}

	private void logEventSuccess(final LifecycleEvents event, final USMEvent[] listeners) {
		if (isLoggableEvent(
				event, listeners)) {
			eventLogger.info(eventPrefix + event + CloudifyConstants.USM_EVENT_EXEC_SUCCESSFULLY);
		}
	}

	private void logEventFailure(final LifecycleEvents event, final USMEvent[] listeners, final EventResult er) {
		if (eventLogger.isLoggable(Level.INFO)) {
			eventLogger.info(eventPrefix + event + CloudifyConstants.USM_EVENT_EXEC_FAILED + ". Reason: "
					+ er.getException().getMessage());

		}
	}

	/*********
	 * Fires the pre-stop event
	 * 
	 * @param reason .
	 * @throws USMException .
	 */
	public void firePreStop(final StopReason reason)
			throws USMException {
		fireEvent(
				LifecycleEvents.PRE_STOP, this.preStopListeners, reason);

	}

	public String getOutputReaderLoggerName() {
		return configuration.getServiceName() + "-Output";
	}

	public String getErrorReaderLoggerName() {
		return configuration.getServiceName() + "-Error";
	}

	public void firePostStart(final StartReason reason)
			throws USMException {
		fireEvent(
				LifecycleEvents.POST_START, this.postStartListeners, reason);
	}

	public void firePreStart(final StartReason reason)
			throws USMException {
		fireEvent(
				LifecycleEvents.PRE_START, this.preStartListeners, reason);

	}

	public void install()
			throws USMException {
		firePreInstall();
		if (this.installer != null) {
			this.installer.install();
		}
		fireInstall();
		firePostInstall();

	}

	private void fireInstall()
			throws USMException {
		fireEvent(
				LifecycleEvents.INSTALL, this.installListeners, null);
	}

	public void firePostInstall()
			throws USMException {
		fireEvent(
				LifecycleEvents.POST_INSTALL, this.postInstallListeners, null);
	}

	public void fireShutdown()
			throws USMException {
		fireEvent(
				LifecycleEvents.SHUTDOWN, this.shutdownListeners, null);
	}

	public void firePreInstall()
			throws USMException {
		fireEvent(
				LifecycleEvents.PRE_INSTALL, this.preInstallListeners, null);
	}

	private void fireEvent(final LifecycleEvents event, final USMEvent[] listeners, final Object reason)
			throws USMException {
		if (listeners != null && listeners.length > 0) {
			logEventStart(
					event, listeners);
			for (final USMEvent listener : listeners) {
				EventResult er = null;
				switch (event) {
				case PRE_SERVICE_START:
					er = ((PreServiceStartListener) listener).onPreServiceStart();
					break;
				case INIT:
					er = ((InitListener) listener).onInit();
					break;
				case PRE_INSTALL:
					er = ((PreInstallListener) listener).onPreInstall();
					break;
				case INSTALL:
					er = ((InstallListener) listener).onInstall();
					break;
				case POST_INSTALL:
					er = ((PostInstallListener) listener).onPostInstall();
					break;
				case PRE_START:
					er = ((PreStartListener) listener).onPreStart((StartReason) reason);
					break;
				case POST_START:
					er = ((PostStartListener) listener).onPostStart((StartReason) reason);
					break;
				case PRE_STOP:
					er = ((PreStopListener) listener).onPreStop((StopReason) reason);
					break;
				case POST_STOP:
					er = ((PostStopListener) listener).onPostStop((StopReason) reason);
					break;
				case SHUTDOWN:
					er = ((ShutdownListener) listener).onShutdown();
					break;
				case PRE_SERVICE_STOP:
					er = ((PreServiceStopListener) listener).onPreServiceStop();
					break;

				default:
					break;
				}

				if (er == null) {
					throw new IllegalStateException("An event execution returned a null value!");
				}
				if (!er.isSuccess()) {
					logEventFailure(
							event, listeners, er);
					throw new USMException("Failed to execute event: " + event + ". Error was: "
							+ er.getException());
				}
			}
			logEventSuccess(
					event, listeners);
		}
	}

	public void fireInit()
			throws USMException {
		fireEvent(
				LifecycleEvents.INIT, this.initListeners, null);
	}

	public void firePostStop(final StopReason reason)
			throws USMException {
		fireEvent(
				LifecycleEvents.POST_STOP, this.postStopListeners, reason);
	}

	public void firePreServiceStart()
			throws USMException {
		fireEvent(
				LifecycleEvents.PRE_SERVICE_START, this.preServiceStartListeners, null);
	}

	public void firePreServiceStop()
			throws USMException {
		fireEvent(
				LifecycleEvents.PRE_SERVICE_STOP, this.preServiceStopListeners, null);
	}

	// /////////////
	// Accessors //
	// /////////////
	public USMInstaller getInstaller() {
		return this.installer;
	}

	public void setInstaller(final USMInstaller installer) {
		this.installer = installer;
	}

	public ProcessLauncher getLauncher() {
		return this.launcher;
	}

	public void setLauncher(final ProcessLauncher launcher) {
		this.launcher = launcher;
	}

	public ProcessKiller getProcessKiller() {
		return this.processKiller;
	}

	public void setProcessKiller(final ProcessKiller processKiller) {
		this.processKiller = processKiller;
	}

	public void setPostDeployListeners(final InitListener[] postDeployListeners) {
		this.initListeners = postDeployListeners;
	}

	public PreInstallListener[] getPreInstallListeners() {
		return this.preInstallListeners;
	}

	public InstallListener[] getInstallListeners() {
		return this.installListeners;
	}

	public void setPreInstallListeners(final PreInstallListener[] preInstallListeners) {
		this.preInstallListeners = preInstallListeners;
	}

	public PostInstallListener[] getPostInstallListeners() {
		return this.postInstallListeners;
	}

	public void setPostInstallListeners(final PostInstallListener[] postInstallListeners) {
		this.postInstallListeners = postInstallListeners;
	}

	public PreStartListener[] getPreStartListeners() {
		return this.preStartListeners;
	}

	public void setPreStartListeners(final PreStartListener[] preStartListeners) {
		this.preStartListeners = preStartListeners;
	}

	public PostStartListener[] getPostStartListeners() {
		return this.postStartListeners;
	}

	public void setPostStartListeners(final PostStartListener[] postStartListeners) {
		this.postStartListeners = postStartListeners;
	}

	public PreStopListener[] getPreStopListeners() {
		return this.preStopListeners;
	}

	public void setPreStopListeners(final PreStopListener[] preStopListeners) {
		this.preStopListeners = preStopListeners;
	}

	public PostStopListener[] getPostStopListeners() {
		return this.postStopListeners;
	}

	public void setPostStopListeners(final PostStopListener[] postStopListeners) {
		this.postStopListeners = postStopListeners;
	}

	public Monitor[] getMonitors() {
		return this.monitors;
	}

	public void setMonitors(final Monitor[] monitors) {
		this.monitors = monitors;
	}

	public UniversalServiceManagerConfiguration getConfiguration() {
		return this.configuration;
	}

	public ShutdownListener[] getPreUndeployListeners() {
		return this.shutdownListeners;
	}

	public void setPreUndeployListeners(final ShutdownListener[] preUndeployListeners) {
		this.shutdownListeners = preUndeployListeners;
	}

	public USMComponent[] getComponents() {
		return this.components;
	}

	public void setComponents(final USMComponent[] components) {
		this.components = components;
	}

	public InitListener[] getInitListeners() {
		return this.initListeners;
	}

	public ShutdownListener[] getShutdownListeners() {
		return this.shutdownListeners;
	}

	public void setDetails(final Details[] details) {
		this.details = details;
	}

	public Details[] getDetails() {
		return details;
	}

	public LivenessDetector[] getLivenessDetectors() {
		return livenessDetectors;
	}

	public void setLivenessDetectors(final LivenessDetector[] livenessDetectors) {
		this.livenessDetectors = livenessDetectors;
	}

	public PreServiceStartListener[] getPreServiceStartListeners() {
		return this.preServiceStartListeners;
	}

	public PreServiceStopListener[] getPreServiceStopListeners() {
		return this.preServiceStopListeners;
	}

	public Map<String, String> getCustomProperties() {
		return ((DSLConfiguration) this.configuration).getService().getCustomProperties();
	}

	public ProcessLocator[] getProcessLocators() {
		return processLocators;
	}

	private Integer getProcessExitValue(final Process processToCheck) {
		try {
			return processToCheck.exitValue();
		} catch (final Exception e) {
			return null;
		}
	}

	/**********
	 * Returns the list of Process IDs calculated by this service's process locators.
	 * @return the list of PIDs.
	 * @throws USMException if one of the locators failed to execute.
	 */
	public List<Long> getServiceProcesses() throws USMException {
		Set<Long> set = new HashSet<Long>();
		ProcessLocator[] locators = this.processLocators;
		for (ProcessLocator processLocator : locators) {
			set.addAll(processLocator.getProcessIDs());
		}

		return new ArrayList<Long>(set);
	}

	/********
	 * Executes all start detection implementations, until all have passed or a timeout is reached.
	 * Once a start detector passes, it is not executed again.
	 * 
	 * @param launchedProcess the process launched by the service's 'start' implementation.
	 * @return true if liveness test passed, false if the timeout is reached without the tests passing.
	 * @throws USMException if a start detector failed, or if the 'start' process exited with a non-zero exit code.
	 * @throws TimeoutException if a start detector implementation timed out.
	 */
	public boolean isProcessLivenessTestPassed(final Process launchedProcess)
			throws USMException, TimeoutException {
		if (this.livenessDetectors.length == 0) {
			logger.warning("No Start Detectors have been set for this service. "
					+ "This may cause the USM to monitor an irrelevant process.");
			return true;
		}

		final long startTime = System.currentTimeMillis();
		final long endTime = startTime + configuration.getStartDetectionTimeoutMSecs();
		int currentTestIndex = 0;

		boolean processIsRunning = true;
		while (System.currentTimeMillis() < endTime && currentTestIndex < this.livenessDetectors.length) {

			// first check if process ended
			if (processIsRunning) {
				processIsRunning = checkProcessIsRunning(launchedProcess);
			}

			logger.info("Executing iteration of liveness detection test");
			int index = currentTestIndex;
			logger.info("Executing liveness detectors from index: " + index);
			logger.info("Liveness detectors: " + Arrays.toString(this.livenessDetectors));
			logger.info("detectors length: " + this.livenessDetectors.length);
			while (index < this.livenessDetectors.length) {
				logger.info("getting detector at index: " + index);
				final LivenessDetector detector = this.livenessDetectors[index];

				boolean testResult = false;
				try {
					testResult = detector.isProcessAlive();
					logger.info("Detection Test results are: " + testResult);
				} catch (final USMException e) {
					// may indicate that the underlying process has terminated
					if (e.getCause() instanceof InterruptedException) {
						// ignore
						logger.info("A start detector failed due to an InterruptedException");
					} else {
						throw e;
					}
				}

				if (testResult) {
					// this liveness detector has succeeded.
					++index;
				} else {
					break;
				}

			}

			if (index == this.livenessDetectors.length) {
				// all tests passed
				return true;
			} else  { 
				currentTestIndex = index;
			}

			try {
				Thread.sleep(configuration.getStartDetectionIntervalMSecs());
			} catch (final InterruptedException e) {
				throw new USMException("Interruped while waiting for start detection", e);
			}
		}
		return false;

	}

	private boolean checkProcessIsRunning(final Process launchedProcess)
			throws USMException {
		final Integer exitCode = getProcessExitValue(launchedProcess);
		if (exitCode == null) {
			return true;
		} else {
			// the process terminated!
			if (exitCode != 0) {
				// A non-zero exit code indicates an error
				final String msg = "The launched process exited with the error exit code: " + exitCode
						+ ". Consult the logs for more details.";
				logProcessStartFailureEvent(msg);
				throw new USMException(msg);
			} else {
				// launched process terminated with no error. The actual process may be running in the
				// background, or as an OS service.
				return false;
			}
		}
	}

	@Override
	public void setClusterInfo(final ClusterInfo clusterInfo) {
		this.puName = clusterInfo.getName();
		this.instanceId = clusterInfo.getInstanceId();
		if (puName != null) {
			this.eventLogger =
					java.util.logging.Logger.getLogger(USMLifecycleBean.class.getPackage().getName()
							+ ".USMEventLogger." + puName);
		} else {
			this.eventLogger =
					java.util.logging.Logger.getLogger(USMLifecycleBean.class.getPackage().getName()
							+ ".USMEventLogger.USM");

		}

	}

	public StopDetector[] getStopDetectors() {
		return stopDetectors;
	}

	/**********
	 * Executes all of the registered stop detectors, stopping if one of them indicates that the service has stopped.
	 * 
	 * @return true if a detector discovered that the service is stopped, false otherwise.
	 */
	public boolean runStopDetection() {
		for (final StopDetector detector : this.stopDetectors) {

			try {
				if (detector.isServiceStopped()) {
					return true;
				}
			} catch (final USMException e) {
				logger.log(
						Level.SEVERE, "A Stop detector failed to execute. The detector was: " + detector, e);
			}
		}

		return false;
	}

	private USMEvent[] initEvents(final Set<USMEvent> allEvents, final USMEvent[] events,
			final Comparator<USMEvent> eventsComparator) {
		if (events.length > 0) {
			allEvents.addAll(Arrays.asList(events));
			if (events.length > 1) {
				Arrays.sort(
						events, eventsComparator);

			}
		}
		return events;

	}

	/********
	 * Sorts the event arrays and initializes them.
	 * 
	 * @param usm .
	 */
	public void initEvents(final UniversalServiceManagerBean usm) {

		final Comparator<USMEvent> comp = new Comparator<USMEvent>() {

			@Override
			public int compare(final USMEvent arg0, final USMEvent arg1) {
				return arg0.getOrder() - arg1.getOrder();
			}
		};
		final Set<USMEvent> allEvents = new HashSet<USMEvent>();

		initEvents(
				allEvents, getInitListeners(), comp);
		initEvents(
				allEvents, getPreInstallListeners(), comp);
		initEvents(
				allEvents, getInstallListeners(), comp);
		initEvents(
				allEvents, getPostInstallListeners(), comp);
		initEvents(
				allEvents, getPreStartListeners(), comp);
		initEvents(
				allEvents, getPostStartListeners(), comp);
		initEvents(
				allEvents, getPreStopListeners(), comp);
		initEvents(
				allEvents, getPostStopListeners(), comp);
		initEvents(
				allEvents, getShutdownListeners(), comp);
		initEvents(
				allEvents, getPreServiceStartListeners(), comp);
		initEvents(
				allEvents, getPreServiceStopListeners(), comp);

		initEvents(
				allEvents, getLivenessDetectors(), comp);
		initEvents(
				allEvents, getStopDetectors(), comp);

		for (final USMEvent usmEvent : allEvents) {
			usmEvent.init(usm);
		}

	}

	/********
	 * Executes all process locators, returning a list of all PIDs located, minus any duplicates.
	 * 
	 * @return the pid list.
	 * @throws USMException if any of the locators failed to execute.
	 */
	public List<Long> executeProcessLocators()
			throws USMException {
		final Set<Long> resultSet = new HashSet<Long>();
		for (ProcessLocator locator : this.processLocators) {
			resultSet.addAll(locator.getProcessIDs());
		}

		final List<Long> list = new ArrayList<Long>(resultSet.size());
		list.addAll(resultSet);
		return list;
	}
}
