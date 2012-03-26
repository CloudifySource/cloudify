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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
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

	// ////////////////////////////
	// A field that is toggled when the external process is started/stopped
	// Used by the start detection implementation to abort start detection
	// if the process failed to start
	private volatile boolean processIsRunning;
	// This thread will be interrupted if the process died
	private volatile Thread livenessDetectorThread;

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

	public void logProcessStartEvent() {
		if (eventLogger.isLoggable(Level.INFO)) {
			eventLogger.info(eventPrefix + "START invoked");
		}
	}

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

	public void externalProcessStarted() {
		this.processIsRunning = true;
	}

	public void externalProcessDied() {
		this.processIsRunning = false;
		final Thread thread = this.livenessDetectorThread;
		if (thread != null) {
			thread.interrupt();
		}

	}

	public boolean isProcessLivenessTestPassed()
			throws USMException, TimeoutException {
		if (this.livenessDetectors.length == 0) {
			logger.warning("No Start Detectors have been set for this service. "
					+ "This may cause the USM to monitor the parent of the actual process. "
					+ "Consider adding a start detector before going to production");
		}

		final long startTime = System.currentTimeMillis();
		final long endTime = startTime + configuration.getStartDetectionTimeoutMSecs();
		int currentTestIndex = 0;

		// save the thread in a field, so it can be interrupted if the process
		// died.
		this.livenessDetectorThread = Thread.currentThread();
		try {
			while (System.currentTimeMillis() < endTime && currentTestIndex < this.livenessDetectors.length) {

				logger.info("Executing iteration of liveness detection test");
				if (!this.processIsRunning) {
					logger.warning("While executing the Process Start Detection, process failure was detected. "
							+ "Aborting start detection test.");
					return false;
				}
				final LivenessDetector detector = this.livenessDetectors[currentTestIndex];

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
					++currentTestIndex;
				} else {
					try {
						Thread.sleep(configuration.getStartDetectionIntervalMSecs());
					} catch (final InterruptedException ie) {
						// ignore
					}
				}

			}

			return currentTestIndex == this.livenessDetectors.length;
		} finally {
			this.livenessDetectorThread = null;
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

	public Map<String, String> getCustomProperties() {
		return ((DSLConfiguration) this.configuration).getService().getCustomProperties();
	}
}
