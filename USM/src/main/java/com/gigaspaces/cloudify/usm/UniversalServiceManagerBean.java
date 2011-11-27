package com.gigaspaces.cloudify.usm;

import groovy.lang.Closure;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.jini.rio.boot.ServiceClassLoader;
import org.openspaces.admin.Admin;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.openspaces.core.cluster.MemberAliveIndicator;
import org.openspaces.core.properties.BeanLevelProperties;
import org.openspaces.core.properties.BeanLevelPropertiesAware;
import org.openspaces.pu.container.support.ResourceApplicationContext;
import org.openspaces.pu.service.CustomServiceDetails;
import org.openspaces.pu.service.CustomServiceMonitors;
import org.openspaces.pu.service.InvocableService;
import org.openspaces.pu.service.ServiceDetails;
import org.openspaces.pu.service.ServiceDetailsProvider;
import org.openspaces.pu.service.ServiceMonitors;
import org.openspaces.pu.service.ServiceMonitorsProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants.USMState;
import com.gigaspaces.cloudify.usm.details.Details;
import com.gigaspaces.cloudify.usm.dsl.DSLConfiguration;
import com.gigaspaces.cloudify.usm.dsl.DSLEntryExecutor;
import com.gigaspaces.cloudify.usm.events.EventResult;
import com.gigaspaces.cloudify.usm.events.StartReason;
import com.gigaspaces.cloudify.usm.events.StopReason;
import com.gigaspaces.cloudify.usm.monitors.Monitor;
import com.gigaspaces.cloudify.usm.tail.RollingFileAppenderTailer;
import com.gigaspaces.cloudify.usm.tail.RollingFileAppenderTailer.LineHandler;
import com.gigaspaces.internal.sigar.SigarHolder;
import com.j_spaces.kernel.Environment;

@Component
public class UniversalServiceManagerBean implements ApplicationContextAware,
		ClusterInfoAware, ServiceMonitorsProvider, ServiceDetailsProvider,
		InvocableService, MemberAliveIndicator, BeanLevelPropertiesAware {

	private static final String ERROR_FILE_NAME_SUFFFIX = ".err";
	private static final String OUTPUT_FILE_NAME_SUFFIX = ".out";
	private static final int WAIT_FOR_DEPENDENCIES_INTERVAL_MILLIS = 5000;
	private static final int WAIT_FOR_DEPENDENCIES_TIMEOUT_MILLIS = 1000 * 60 * 30;
	// TODO: change to false
	private static final String ASYNC_INSTALL_DEFAULT_VALUE = "false";
	private static final int FILE_TAILER_INTERVAL_SECS_DEFAULT = 5;
	private static final int DEFAULT_POST_LAUNCH_WAIT_PERIOD_MILLIS = 2000;
	private static final int DEFAULT_POST_DEATH_WAIT_PERIOD_MILLIS = 2000;

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(UniversalServiceManagerBean.class.getName());

	// process names for well-known shell
	// used to check if the monitored process is a shell, and not an
	// 'interesting' process
	final static String[] SHELL_PROCESS_NAMES = { "cmd.exe", "bash", "/bin/sh" };

	private final Sigar sigar = SigarHolder.getSigar();
	private File puWorkDir;

	private Object stateMutex = new Object();
	private USMState state = USMState.INITIALIZING;

	public USMState getState() {
		return state;
	}

	@Autowired(required = true)
	public USMLifecycleBean usmLifecycleBean;

	private Process process;

	private ProcessStreamReaderTask inputReader;

	private String streamLoggerLevel = Level.INFO.getName();

	private ProcessStreamReaderTask errorReader;

	private ScheduledExecutorService executors;

	private long myPid;

	// these values will change after an unexpected process death, and other
	// threads may read them, so they need to be volatile
	private volatile long childProcessID;
	private volatile long actualProcessID;

	private File puExtDir;

	private String serviceType = CustomServiceDetails.SERVICE_TYPE;
	private String serviceSubType = "USM";
	private String serviceDescription = "USM";
	private String serviceLongDescription = "USM";

	// TODO - remote this, replaced with liveness detector
	private long postLaunchWaitPeriodMillis = DEFAULT_POST_LAUNCH_WAIT_PERIOD_MILLIS;
	private final long postDeathWaitPeriodMillis = DEFAULT_POST_DEATH_WAIT_PERIOD_MILLIS;
	private Thread shutdownHookThread;
	private int instanceId;
	private boolean runningInGSC = true;
	private ApplicationContext applicationContext;
	private String clusterName;
	private String uniqueFileNamePrefix;
	private ProcessDeathNotifier processDeathNotifier;
	private RollingFileAppenderTailer tailer;

	private int fileTailerIntervalSecs = FILE_TAILER_INTERVAL_SECS_DEFAULT;

	// asynchronous installation
	private boolean asyncInstall = false;

	// called on USM startup, or if the process died unexpectedly and is being
	// restarted
	private void reset(boolean existingProcessFound) throws USMException,
			TimeoutException {
		synchronized (this.stateMutex) {

			this.state = USMState.INITIALIZING;
			logger.info("USM Started. Configuration is: "
					+ usmLifecycleBean.getConfiguration());

			this.executors = Executors.newScheduledThreadPool(5);

			// check for PID file
			if (existingProcessFound) {
				// found an existing process, so no need to launch
				startAsyncTasks();
				this.state = USMState.RUNNING;

				return;
			} else {
				try {
					// Launch the process
					startProcessLifecycle();
				} catch (final USMException usme) {
					logger.severe("Process lifecycle failed to start. Shutting down the USM instance");
					try {
						this.shutdown();
					} catch (final Exception e) {
						logger.log(
								Level.SEVERE,
								"While shutting down the USM due to a failure in initialization, the following exception occured: "
										+ e.getMessage(), e);
					}
					throw usme;
				}
			}

		}
	}

	@PostConstruct
	public void init() throws USMException, TimeoutException {

		initUniqueFileName();
		initCustomProperties();
		this.myPid = this.sigar.getPid();

		boolean existingProcessFound = checkForPIDFile();

		// Initialize and sort events
		initEvents();

		reset(existingProcessFound);

		// Auto shutdown if this is a Test-Recipe run
		checkForRecipeTestEnvironment();

		initShutdownHook();

		// Map<String, Object> map = new HashMap<String, Object>();
		// map.put(CloudifyConstants.INVOCATION_PARAMETER_COMMAND_NAME, "cmd1");
		// invoke(map);
		//getServicesDetails();
		// getServicesMonitors();
	}

	private void initCustomProperties() {
		Map<String, String> props = usmLifecycleBean.getCustomProperties();
		if (props.containsKey(CloudifyConstants.USM_PARAMETERS_TAILER_INTERVAL)) {
			this.fileTailerIntervalSecs = Integer.parseInt(props
					.get(CloudifyConstants.USM_PARAMETERS_TAILER_INTERVAL));
		}

	}

	private File getPidFile() {
		return new File(this.uniqueFileNamePrefix + ".pid");
	}

	private File getOutputFile() {
		return new File(this.uniqueFileNamePrefix + OUTPUT_FILE_NAME_SUFFIX);
	}

	private File getErrorFile() {
		return new File(this.uniqueFileNamePrefix + ERROR_FILE_NAME_SUFFFIX);
	}

	private String getLogsDir() {
		return Environment.getHomeDirectory() + "logs";
	}

	private String createUniqueFileName() {
		final String username = System.getProperty("user.name");
		final String clusterName = (this.clusterName == null ? "USM"
				: this.clusterName);

		try {
			return clusterName + "_" + this.instanceId + "_" + username + "@"
					+ InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new IllegalStateException("Failed to get localhost name", e);
		}
	}

	private void initUniqueFileName() {

		this.uniqueFileNamePrefix = getLogsDir() + File.separator
				+ createUniqueFileName();

	}

	private void checkForRecipeTestEnvironment() {

		String timeoutValue = System
				.getProperty("com.gs.usm.RecipeShutdownTimeout");
		if (timeoutValue == null) {
			return;
		}
		int timeout = Integer.parseInt(timeoutValue);

		logger.info("USM is running in Test mode. USM will shut down in: "
				+ timeout + " seconds");
		this.executors.schedule(new TestRecipeShutdownRunnable(
				this.applicationContext, this), timeout, TimeUnit.SECONDS);
	}

	private void shutdownHook() {

		try {
			logger.info("USM shutting down unexpectedly via JVM Shutdown Hook.");
			this.state = USMState.SHUTTING_DOWN;
			stop(StopReason.SHUTDOWN_HOOK);
			executors.shutdown();
		} catch (final Exception e) {
			logger.log(
					Level.SEVERE,
					"Failed to execute shutdown hook. Managed process may not have shut down!",
					e);
		}

	}

	private void initShutdownHook() {
		this.shutdownHookThread = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				shutdownHook();
			}
		});
		Runtime.getRuntime().addShutdownHook(this.shutdownHookThread);

	}

	private void removeShutdownHook() {
		if (this.shutdownHookThread != null) {
			final boolean result = Runtime.getRuntime().removeShutdownHook(
					this.shutdownHookThread);
			if (!result) {
				logger.severe("Failed to remove JVM shutdown hook during shutdown! ");
			}
		}

	}

	private void initEvents() {

		usmLifecycleBean.initEvents(this);

	}

	@PreDestroy
	public void shutdown() {

		logger.info("USM is shutting down!");

		synchronized (this.stateMutex) {
			this.state = USMState.SHUTTING_DOWN;

			if (!FileUtils.deleteQuietly(getPidFile())) {
				logger.severe("Attempted to delete PID file: "
						+ getPidFile()
						+ " but failed. The file may remain and be picked up by a new GSC");
			}

			stop(StopReason.UNDEPLOY);

			executors.shutdown();
			removeShutdownHook();

			if (this.runningInGSC) {
				deleteExtDirContents(); // avoid deleting contents in Integrated
										// PU
			}

			try {
				usmLifecycleBean.fireShutdown();
			} catch (USMException e) {
				logger.log(Level.SEVERE, "Failed to execute shutdown event: "
						+ e.getMessage(), e);
			}
		}
		logger.info("USM shut down completed!");
	}

	private void deleteExtDirContents() {
		deleteDir(this.puExtDir);
	}

	private void deleteDir(final File dir) {
		logger.fine("Deleting contents of Directory: " + dir);
		final File[] files = dir.listFiles();
		if (files != null) {
			for (final File file : files) {
				if (file.isDirectory()) {
					deleteDir(file);
				}
				logger.finer("Deleting: " + file);
				final boolean result = file.delete();
				if (!result) {
					logger.warning("Failed to delete file: "
							+ file
							+ ". This may indicate that another process is using this file, "
							+ "possibly because the USM managed process did not terminate correctly.");
				}
			}
		}
	}

	protected void startProcessLifecycle() throws USMException,
			TimeoutException {

		if (this.instanceId == 1) {
			usmLifecycleBean.firePreServiceStart();
		}

		usmLifecycleBean.fireInit();

		logger.info("start lifecycle. async = " + this.asyncInstall);
		if (this.asyncInstall) {

			if (USMUtils.isRunningInGSC(this.applicationContext)) {
				logger.info("async install running in GSC");
				// install and run USM after this PUI has started.
				registerPostPUILifecycleTask();
			} else {
				logger.info("async install running in integrated container");
				// running in Integrated container - Admin API is not available.
				// instead, just run the install/run sequence asynchronously on
				// another thread.
				registerAsynchLifecycleTask();
			}
		} else {
			installAndRun();
		}

	}

	private void registerAsynchLifecycleTask() {
		this.executors.schedule(new Runnable() {

			@Override
			public void run() {
				try {
					installAndRun();
				} catch (Exception e) {
					logger.log(
							Level.SEVERE,
							"Asynchronous install failed with message: "
									+ e.getMessage()
									+ ". Instance will shut down", e);
					shutdownUSMException = e;
				}

			}
		}, 5, TimeUnit.SECONDS);
	}

	private void registerPostPUILifecycleTask() {
		final Admin admin = USMUtils.getAdmin();
		ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit(
				this.clusterName);
		final int instanceIdToMatch = this.instanceId;
		pu.getProcessingUnitInstanceAdded().add(
				new ProcessingUnitInstanceAddedEventListener() {

					@Override
					public void processingUnitInstanceAdded(
							ProcessingUnitInstance processingUnitInstance) {
						if (processingUnitInstance.getInstanceId() == instanceIdToMatch) {
							// my PUI is ready
							try {
								// TODO: should this be run on a
								// separate thread???
								installAndRun();
								admin.removeEventListener(this);
							} catch (Exception e) {
								logger.log(Level.SEVERE,
										"Asynchronous install failed with message: "
												+ e.getMessage()
												+ ". Instance will shut down",
										e);
								shutdownUSMException = e;
							}
						}

					}
				});
	}

	private void installAndRun() throws USMException, TimeoutException {
		usmLifecycleBean.install();
		if (this.asyncInstall) {
			waitForDependencies();
		}
		launch();
	}

	private void waitForDependencies() {
		logger.info("Waiting for dependencies");
		final long startTime = System.currentTimeMillis();
		final long endTime = startTime + WAIT_FOR_DEPENDENCIES_TIMEOUT_MILLIS;
		Admin admin = USMUtils.getAdmin();
		for (String dependantService : this.dependencies) {

			logger.info("Waiting for dependency: " + dependantService);
			final ProcessingUnit pu = waitForPU(endTime, admin,
					dependantService);

			waitForPUI(endTime, dependantService, pu);
			logger.info("Dependency " + dependantService + " is available");

		}

		logger.info("All dependencies are available");
	}

	private void waitForPUI(final long endTime, String dependantService,
			final ProcessingUnit pu) {
		while (true) {
			final long waitForPUIPeriod = endTime - System.currentTimeMillis();
			if (waitForPUIPeriod <= 0) {
				throw new IllegalStateException("Could not find dependency "
						+ dependantService + " required for this service");
			}

			logger.info("Waiting for PUI of service: " + dependantService + " for " + waitForPUIPeriod + " Milliseconds");

			final boolean found = pu.waitFor(1, waitForPUIPeriod,
					TimeUnit.MILLISECONDS);
			
			logger.info("Timeout ended. processing unit " + dependantService + " found result is " + found);
			if (!found) {
				throw new IllegalStateException(
						"Could not find instance of dependency "
								+ dependantService
								+ " required for this service");
			}

			ProcessingUnitInstance[] puis = pu.getInstances();
			logger.info("Found " + puis.length + " instances");
			for (ProcessingUnitInstance pui : puis) {
				ServiceMonitors sm = pui.getStatistics().getMonitors()
						.get(CloudifyConstants.USM_MONITORS_SERVICE_ID);
				if (sm != null) {
					Object stateObject = sm.getMonitors().get(
							CloudifyConstants.USM_MONITORS_STATE_ID);
					logger.info("USM state is: " + stateObject);
					if (stateObject == null) {
						logger.warning("Could not find the instance state in the PUI monitors");
					} else {
						int stateIndex = (Integer) stateObject;
						USMState usmState = USMState.values()[stateIndex];
						logger.info("PUI is in state: " + usmState);
						if (usmState.equals(USMState.RUNNING)) {
							logger.info("Found a running instance of dependant service: "
									+ dependantService);
							return;
						}
					}

				}
			}

			logger.info("Could not find a running instance of service: "
					+ dependantService + ". Sleeping before trying again");
			try {
				// TODO - make this configurable
				Thread.sleep(WAIT_FOR_DEPENDENCIES_INTERVAL_MILLIS);
			} catch (InterruptedException e) {
				// ignore.
			}

		}
	}

	private ProcessingUnit waitForPU(final long endTime, Admin admin,
			String dependantService) {
		final long waitForPUPeriod = endTime - System.currentTimeMillis();
		if (waitForPUPeriod <= 0) {
			throw new IllegalStateException("Could not find dependency "
					+ dependantService + " required for this service");
		}
		// TODO - PU name should use application name plus service name
		// using new format.
		logger.info("Waiting for PU: " + dependantService);
		ProcessingUnit pu = admin.getProcessingUnits().waitFor(
				dependantService, waitForPUPeriod, TimeUnit.MILLISECONDS);
		if (pu == null) {
			throw new IllegalStateException("Could not find dependency "
					+ dependantService + " required for this service");
		}
		return pu;
	}

	/**********
	 * Checks if a PID file exists from a previous execution of this service and
	 * instance on this host.
	 * 
	 * @return true if a valid pid file matching a running process was found.
	 * 
	 * @throws IOException
	 *             if the PID file could not be read.
	 * @throws USMException
	 *             if there was a problem checking for a PID file.
	 */
	private boolean checkForPIDFile() throws USMException {

		File file = getPidFile();
		if (file.exists()) {
			// Found a PID file
			String pidString;
			try {
				pidString = FileUtils.readFileToString(file);
			} catch (IOException e) {
				throw new USMException(
						"Failed to read pid file contents from file: " + file,
						e);
			}

			long pid = 0;
			try {
				pid = Long.parseLong(pidString);
			} catch (NumberFormatException nfe) {
				throw new USMException(
						"The contents of the PID file: "
								+ file
								+ " cannot be parsed to a long value: "
								+ pidString
								+ ". Check the file contents and delete the file before retrying.",
						nfe);
			}

			if (USMUtils.isProcessAlive(pid)) {
				logger.info("Found Active Process with PID: " + pid
						+ " from file: " + file);

				this.actualProcessID = pid;
				this.childProcessID = 0; // can't be sure of child process PID -
											// better to leave empty
				try {
					logProcessDetails();
				} catch (final SigarException e) {
					logger.log(Level.SEVERE, "Failed to log process details", e);
				}
				return true;
			} else {
				logger.warning("PID file: " + file + " was found with PID: "
						+ pid + " but this process does not exist. PID Fi.");
				deleteProcessFiles(file);
			}

		}
		// just in case an output/error file remained on the file system.
		deleteProcessFiles(file);
		return false;
	}

	private void deleteProcessFiles(File file) {
		FileUtils.deleteQuietly(file);
		FileUtils.deleteQuietly(getErrorFile());
		FileUtils.deleteQuietly(getOutputFile());
	}

	private void launch() throws USMException, TimeoutException {

		// This method can be called during init, and from onProcessDeath.
		// The mutex is required as onProcessDeath fires on a separate
		// thread.
		synchronized (this.stateMutex) {
			this.state = USMState.LAUNCHING;
			usmLifecycleBean.firePreStart(StartReason.DEPLOY);

			final Set<Long> childrenBefore = getChildProcesses(this.myPid);

			// final File pidFile = acquirePidFile();

			// bit of a hack, but not that bad.
			usmLifecycleBean.logProcessStartEvent();

			usmLifecycleBean.externalProcessStarted();

			this.process = usmLifecycleBean.getLauncher().launchProcessAsync(
					usmLifecycleBean.getConfiguration().getStartCommand(),
					this.puExtDir, getOutputFile(), getErrorFile());

			// After the main process starts, wait for a short interval so if
			// the process failed to
			// start it will fail quickly here.

			// TODO - Do we still need this?
			try {
				Thread.sleep(this.postLaunchWaitPeriodMillis);
			} catch (final InterruptedException e) {
				// ignore
			}

			// After the timeout, check if process started correctly
			if (!isProcessAlive(this.process)) {
				throw new USMException(
						"Process has shut down or failed to start. Check logs for errors");
			}

			try {
				// Now we check start detection - waiting for the application
				// level
				// tests to indicate
				// that the process is ready for work. Usually means that the
				// server
				// ports are open
				// or that a specific string was printed to a log file.
				if (!usmLifecycleBean.isProcessLivenessTestPassed()) {
					throw new USMException(
							"The Start Detection test failed! Shutting down this instance.");
				}

				usmLifecycleBean.firePostStart(StartReason.DEPLOY);
			}

			finally {
				// It is better to find the process IDs after post start, just
				// in
				// case someone chose
				// not to use start detection and to use post start to block
				// until
				// the
				// process is ready
				findProcessIDs(childrenBefore, null); // pidFile);
			}

			// At this point, we assume that the main process has started, so we
			// start reading its output
			// the output is also used to detect process death, which will be
			// fired on a different thread.
			// IMPORTANT NOTE: this call must be made AFTER findprocessIDs has
			// executed successfully.
			// otherwise, the PID values will be missing, or out of date, and
			// cause the process death detection
			// to fire even though the process is running
			startAsyncTasks();

			this.state = USMState.RUNNING;
			// notify threads that are waiting for process to start
			this.stateMutex.notifyAll();

		}

	}

	// DO NOT DELETE - PID FILE HANDLING SHOULD BE RETURNED
	// private void waitForPidFile(final File pidFile) throws USMException {
	// // TODO review this
	// final int retries = 10;
	// final long delayInterval = 500;
	// for (int i = 0; i < retries; i++) {
	// if (pidFile.exists()) {
	// return;
	// } else {
	// try {
	// Thread.sleep(delayInterval);
	// } catch (final InterruptedException e) {
	// logger.log(Level.SEVERE, "interrupted", e);
	// }
	// }
	// }
	// throw new USMException("pidFile " + pidFile.getAbsolutePath() +
	// " does not exist");
	// }
	//
	// private void monitorProcessUsingSigar(final long pid) throws USMException
	// {
	// // TODO add SigarProcessStateTask sleep interval to configuration
	// final SigarProcessStateTask stateTask =
	// new SigarProcessStateTask(pid, new ProcessDeathNotifier(this), 5000);
	// executors.submit(stateTask);
	// }

	// private File acquirePidFile() {
	// if (!USMUtils.isLinuxOrUnix()) {
	// return null;
	// }
	// File pidFile = null;
	// if ((usmLifecycleBean.configuration.getPidFile() != null) &&
	// (usmLifecycleBean.configuration.getPidFile().length() > 0)) {
	// pidFile = new File(this.puWorkDir,
	// usmLifecycleBean.configuration.getPidFile());
	// if (pidFile.exists()) {
	// logger.warning("PID File: " + pidFile +
	// " exists before process was launched. File will be deleted.");
	// final boolean deleteResult = pidFile.delete();
	// if (!deleteResult) {
	// logger.warning("Failed to delete PID file: " + pidFile);
	// }
	// }
	// }
	// return pidFile;
	// }

	protected void findProcessIDs(final Set<Long> childrenBefore,
			final File pidFile) throws USMException {
		if (pidFile != null) {
			return;
		}
		final long[] allPids = getAllPids();
		final Map<Long, Set<Long>> procTree = createProcessTree(allPids);
		this.childProcessID = findNewChildProcessID(childrenBefore, procTree);

		logger.info("Looking for actual process ID in process tree");
		this.actualProcessID = findLeafProcessID(this.childProcessID, procTree);

		checkForConsoleProcess(this.actualProcessID);

		// Write the pid of the actual process to a file, so that in
		// case of a GSC crash, the new GSC will pick up the existing
		// process
		try {
			writePidToFile(this.actualProcessID);
		} catch (IOException e) {
			throw new USMException("Failed to write Process ID: "
					+ this.actualProcessID + " to file: " + getPidFile(), e);
		}

		try {
			logProcessDetails();
		} catch (final SigarException e) {
			logger.log(Level.SEVERE, "Failed to log process details", e);
		}
	}

	private void writePidToFile(final long pid) throws IOException {
		FileUtils.writeStringToFile(getPidFile(), Long.toString(pid));

	}

	private void checkForConsoleProcess(long pid) {
		try {
			final String procName = this.sigar.getProcExe(pid).getName();
			for (String shellName : SHELL_PROCESS_NAMES) {
				if (procName.indexOf(shellName) >= 0) {
					logger.warning("The monitored process is "
							+ procName
							+ ", which may be a console process. "
							+ "This is usually a configuration problem. USM Statistics will be collected for this process, "
							+ "and not for the child process it probably has. Are you missing a Start Detector?");
					return;
				}

			}
		} catch (SigarException e) {
			logger.log(
					Level.SEVERE,
					"While checking if process is a console, failed to read the process name for process: "
							+ pid, e);
		}

	}

	protected void logProcessDetails() throws SigarException {
		if (logger.isLoggable(Level.INFO)) {
			if (this.childProcessID != 0) {
				logger.info("Process ID of Child Process is: "
						+ this.childProcessID + ", Executable is: "
						+ this.sigar.getProcExe(this.childProcessID).getName());
			}
			if (this.actualProcessID != 0) {
				logger.info("Actual Monitored Process ID is: "
						+ this.actualProcessID + ", Executable is: "
						+ this.sigar.getProcExe(this.actualProcessID).getName());
			}
		}
	}

	// private long findProcessIDInPidFile(final File pidFile) {
	// BufferedReader reader = null;
	// try {
	// reader = new BufferedReader(new FileReader(pidFile));
	// final String line = reader.readLine();
	// logger.info("PID File Contents First Line: " + line);
	// final long pid = Long.parseLong(line);
	// return pid;
	// } catch (final IOException ioe) {
	// logger.log(Level.SEVERE, "Failed to read PID File: " + pidFile, ioe);
	// return 0;
	// } catch (final NumberFormatException nfe) {
	// logger.log(Level.SEVERE, "Failed to parse PID File contents: "
	// + pidFile, nfe);
	// return 0;
	// } finally {
	// if (reader != null) {
	// try {
	// reader.close();
	// } catch (final IOException e) {
	// // ignore
	// }
	// }
	// }
	// }

	private long findLeafProcessID(final long parentProcessID,
			final Map<Long, Set<Long>> procTree) {

		final Set<Long> pids = procTree.get(parentProcessID);
		if ((pids == null) || pids.isEmpty()) {
			return parentProcessID;
		}

		final long pid = pids.iterator().next();

		if (pids.size() > 1) {
			logger.warning("Process ID " + parentProcessID
					+ " has multiple child processes. Process " + pid
					+ " selected as child!");
		}

		// Recursive call
		return findLeafProcessID(pid, procTree);
	}

	protected long findNewChildProcessID(final Set<Long> childrenBefore,
			final Map<Long, Set<Long>> procTree) throws USMException {
		final Set<Long> childrenAfter = procTree.get(this.myPid);
		if (childrenAfter == null) {
			throw new USMException("Could not find container process ("
					+ this.myPid + ") in generated process tree");
		}
		childrenAfter.removeAll(childrenBefore);

		if (childrenAfter.size() == 0) {
			throw new USMException("New process could not be found!");
		}

		if (childrenAfter.size() > 1) {
			logger.warning("Multiple new processes have been found: "
					+ childrenAfter.toString()
					+ ". Using the first as child process ID!");
		}

		final long newChildProcessID = childrenAfter.iterator().next();

		return newChildProcessID;
	}

	private boolean isProcessAlive(final Process processToCheck) {
		try {
			processToCheck.exitValue();
			return false;
		} catch (final Exception e) {
			return true;
		}
	}

	private Set<Long> getChildProcesses(final long ppid) throws USMException {
		final long[] pids = getAllPids();
		return getChildProcesses(ppid, pids);
	}

	public long getContainerPid() {
		return this.myPid;
	}

	private long[] getAllPids() throws USMException {
		long[] pids;
		try {
			pids = this.sigar.getProcList();
		} catch (final SigarException se) {
			throw new USMException("Failed to look up process IDs. Error was: "
					+ se.getMessage(), se);
		}
		return pids;
	}

	/**********
	 * Creates a process tree of the given Process IDs. Each entry in the map
	 * maps a PID to the set of its child PIDs.
	 * 
	 * @param pids
	 * @return
	 */
	private Map<Long, Set<Long>> createProcessTree(final long[] pids) {

		final HashMap<Long, Set<Long>> map = new HashMap<Long, Set<Long>>();
		for (final long pid : pids) {
			final Set<Long> childSet = map.get(pid);
			if (childSet == null) {
				map.put(pid, new HashSet<Long>());
			}

			try {
				final long ppid = this.sigar.getProcState(pid).getPpid();

				Set<Long> set = map.get(ppid);
				if (set == null) {
					set = new HashSet<Long>();
					map.put(ppid, set);
				}
				set.add(pid);
			} catch (final SigarException e) {
				logger.log(Level.WARNING,
						"Failed to get Parent Process for process: " + pid, e);
			}
		}

		return map;

	}

	private Set<Long> getChildProcesses(final long ppid, final long[] pids) {

		final Set<Long> children = new HashSet<Long>();
		for (final long pid : pids) {
			try {
				if (ppid == this.sigar.getProcState(pid).getPpid()) {
					children.add(pid);
				}
			} catch (final SigarException e) {
				logger.log(Level.WARNING,
						"While scanning for child processes of process " + ppid
								+ ", could not read process state of Process: "
								+ pid + ". Ignoring.", e);

			}

		}
		return children;

	}

	/*******
	 * Starts async tasks responsible for: - reading the output and error files
	 * generated by the external process. - waits for process to die, if in the
	 * foreground - runs stop detection
	 * 
	 * 
	 **/
	private void startAsyncTasks() {

		logger.info("Starting async tasks");
		this.processDeathNotifier = new ProcessDeathNotifier(this);
		// make sure all death notifications are applied to the current process
		final ProcessDeathNotifier notifier = this.processDeathNotifier;

		// We should switch this to the file system notifier when Java 7 comes
		// out
		// Schedule task for reading output and error files.
		if (this.tailer == null) {
			this.tailer = createFileTailerTask();
		}
		logger.info("Launching tailer task");
		executors.scheduleWithFixedDelay(tailer, 1, fileTailerIntervalSecs,
				TimeUnit.SECONDS);

		// Launch thread that waits for foreground process to die.
		if (this.process != null) {
			executors.submit(createProcessWaitTask(notifier));
		}

		// Schedule Stop Detector task
		Runnable task = new Runnable() {

			@Override
			public void run() {
				final boolean serviceIsStopped = usmLifecycleBean
						.runStopDetection();
				if (serviceIsStopped) {
					notifier.processDeathDetected();
				}

			}
		};
		executors.scheduleWithFixedDelay(task, 2, 5, TimeUnit.SECONDS);

	}

	private Runnable createProcessWaitTask(final ProcessDeathNotifier notifier) {
		return new Runnable() {

			@Override
			public void run() {
				try {
					process.waitFor();
					notifier.processDeathDetected();

				} catch (InterruptedException e) {
					logger.warning("The thread waiting for the underlying process to end has been interruped!");
				}

			}
		};
	}

	private RollingFileAppenderTailer createFileTailerTask() {
		final String filePattern = createUniqueFileName() + "("
				+ OUTPUT_FILE_NAME_SUFFIX + "|" + ERROR_FILE_NAME_SUFFFIX + ")";

		final Logger outputLogger = Logger.getLogger(usmLifecycleBean
				.getOutputReaderLoggerName());
		final Logger errorLogger = Logger.getLogger(usmLifecycleBean
				.getErrorReaderLoggerName());

		logger.info("Creating tailer for dir: " + getLogsDir()
				+ ", with regex: " + filePattern);
		RollingFileAppenderTailer tailer = new RollingFileAppenderTailer(
				getLogsDir(), filePattern, new LineHandler() {

					@Override
					public void handleLine(final String fileName,
							final String line) {
						//
						if (fileName.endsWith(".out")) {
							outputLogger.info(line);
						} else {
							errorLogger.info(line);
						}

					}
				});
		return tailer;
	}

	/********
	 * Kill process chain, starting from the actual process and going up the
	 * process tree. Does not kill the current process, of-course.
	 * 
	 * @throws USMException
	 *             if there was a problem accessing the process table
	 */
	private void killMonitoredProcessChain() throws USMException {

		List<Long> list = null;
		if (this.childProcessID == 0) {
			// the actual process was launched by a previous GSC which must
			// have crashed. So we only kill the actual process ID,
			// and hope that the other processes die as well
			// (which they are supposed to do).
			list = Arrays.asList(this.actualProcessID);
		} else {
			// kill all processes in the chain, starting from the actual process
			// and moving up
			// to its parents, until we reach the GSC (not including the GSC).
			list = USMUtils.getProcessParentChain(this.actualProcessID,
					this.myPid);
		}
		logger.info("Killing child processes in chain: " + list);
		for (final Long pid : list) {
			if (USMUtils.isProcessAlive(pid)) {
				usmLifecycleBean.getProcessKiller().killProcess(pid);
			} else {
				logger.info("Process: " + pid + " is dead.");
			}
		}

	}

	public void stop(final StopReason reason) {
		try {
			usmLifecycleBean.firePreStop(reason);
		} catch (USMException e) {
			logger.log(Level.SEVERE,
					"Failed to execute pre stop event: " + e.getMessage(), e);
		}

		// First kill the monitored process, then all is parents, up to and not
		// including this one
		try {
			// this.processKiller.killProcess(this.actualProcessID);
			killMonitoredProcessChain();
		} catch (final USMException e) {
			logger.log(Level.SEVERE, "Failed to shut down actual process ("
					+ this.actualProcessID + ")", e);
		}

		if ((this.actualProcessID != this.childProcessID)
				&& (this.childProcessID != 0)) {
			try {
				if (USMUtils.isProcessAlive(this.childProcessID)) {
					usmLifecycleBean.getProcessKiller().killProcess(
							this.childProcessID);
				}
			} catch (final USMException e) {
				logger.log(Level.SEVERE, "Failed to shut down child process ("
						+ this.childProcessID + ")", e);
			}
		}

		try {
			usmLifecycleBean.firePostStop(reason);
		} catch (USMException e) {
			logger.log(Level.SEVERE,
					"Failed to execute post stop event: " + e.getMessage(), e);
		}
	}

	@Override
	public void setApplicationContext(final ApplicationContext arg0)
			throws BeansException {

		this.applicationContext = arg0;

		if (arg0.getClassLoader() instanceof ServiceClassLoader) {// running in
																	// GSC
			this.runningInGSC = true;
			final ServiceClassLoader scl = (ServiceClassLoader) arg0
					.getClassLoader();

			final URL url = scl.getSlashPath();
			logger.fine("The slashpath URL is: " + url);
			URI uri;
			try {
				uri = url.toURI();
			} catch (final URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
			logger.fine("The slashpath URI is: " + uri);
			this.puWorkDir = new File(uri);
			this.puExtDir = new File(this.puWorkDir, "ext");
			return;

		}

		final ResourceApplicationContext rac = (ResourceApplicationContext) arg0;

		try {
			this.runningInGSC = false;
			final Field resourcesField = rac.getClass().getDeclaredField(
					"resources");
			final boolean accessibleBefore = resourcesField.isAccessible();

			resourcesField.setAccessible(true);
			final Resource[] resources = (Resource[]) resourcesField.get(rac);
			for (final Resource resource : resources) {
				// find META-INF/spring/pu.xml
				final File file = resource.getFile();
				if (file.getName().equals("pu.xml")
						&& file.getParentFile().getName().equals("spring")
						&& file.getParentFile().getParentFile().getName()
								.equals("META-INF")) {
					puWorkDir = resource.getFile().getParentFile()
							.getParentFile().getParentFile();
					puExtDir = new File(puWorkDir, "ext");
					break;
				}

			}

			resourcesField.setAccessible(accessibleBefore);
		} catch (final Exception e) {
			throw new IllegalArgumentException(
					"Could not find pu.xml in the ResourceApplicationContext",
					e);
		}
		if (puWorkDir == null) {
			throw new IllegalArgumentException(
					"Could not find pu.xml in the ResourceApplicationContext");
		}

	}

	private boolean firstTime = true;

	// crappy method name
	public void onProcessDeath() {
		// we use this to cancel start detection, if it is still running
		usmLifecycleBean.externalProcessDied();
		synchronized (this.stateMutex) {

			if (this.state != USMState.RUNNING) {
				// process is shutting down or restarting, nothing to do now.
				return;
			}

			try {
				// unexpected process failure
				usmLifecycleBean.firePostStop(StopReason.PROCESS_FAILURE);
			} catch (Exception e) {
				logger.log(
						Level.SEVERE,
						"The Post Stop event failed to execute after an anexpected failure of the process: "
								+ e.getMessage(), e);
			}

			// kill all current tasks, and create new thread pool for tasks
			this.executors.shutdownNow();

			this.state = USMState.LAUNCHING;
			this.executors = Executors.newScheduledThreadPool(5);

			// Restart USM
			new Thread(new Runnable() {

				@Override
				public void run() {

					try {
						Thread.sleep(postDeathWaitPeriodMillis);
					} catch (final InterruptedException e) {
						// ignore
					}
					try {
						// TODO - if process launch failed?????????/
						launch();
					} catch (final USMException e) {
						logger.log(Level.SEVERE,
								"Failed to re-launch the external process after a previous failure: "
										+ e.getMessage(), e);
						logger.severe("Marking this USM as failed so it will be recycled by GSM");
						markUSMAsFailed(e);
					} catch (TimeoutException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}

	}

	public String getStreamLoggerLevel() {
		return streamLoggerLevel;
	}

	public void setStreamLoggerLevel(final String streamLoggerLevel) {
		this.streamLoggerLevel = streamLoggerLevel;
	}

	@Override
	public void setClusterInfo(final ClusterInfo clusterInfo) {

		this.instanceId = clusterInfo.getInstanceId();
		this.clusterName = clusterInfo.getName();

	}

	@Override
	public ServiceDetails[] getServicesDetails() {
		logger.info("Executing getServiceDetails()");
		final CustomServiceDetails csd = new CustomServiceDetails(
				CloudifyConstants.USM_DETAILS_SERVICE_ID, this.serviceType,
				this.serviceSubType, this.serviceDescription,
				this.serviceLongDescription);
		final ServiceDetails[] res = new ServiceDetails[] { csd };

		// boolean deleteme = true;
		// if (deleteme) {
		// return res;
		// }
		// waitForServiceToStart();

		// if the underlying process is not running, do not execute the details
		// if (!this.getState().equals(USMState.RUNNING)) {
		// // throw new IllegalStateException("USM has not started yet!");
		// return res;
		// }

		final Details[] alldetails = usmLifecycleBean.getDetails();
		final Map<String, Object> result = csd.getAttributes();
		for (final Details details : alldetails) {

			try {
				logger.fine("Executing details: " + details);
				final Map<String, Object> detailsValues = details.getDetails(
						this, usmLifecycleBean.getConfiguration());
				removeNonSerializableObjectsFromMap(detailsValues);
				result.putAll(detailsValues);
			} catch (final Exception e) {
				logger.log(Level.SEVERE, "Failed to execute service details", e);
			}

		}

		if (logger.isLoggable(Level.INFO)) { // TODO - change to FINER
			logger.info("Details are: " + Arrays.toString(res));
		}
		return res;

	}

	private void waitForServiceToStart() {

		boolean firstTime = true;
		while (true) {
			synchronized (this.stateMutex) {
				switch (this.state) {
				case INITIALIZING:
				case LAUNCHING:
					if (!firstTime) {
						throw new IllegalStateException(
								"The Service failed to start. The current service state is: "
										+ this.state);
					}
					try {
						logger.info("Waiting for service to start. Current service state: "
								+ this.state);
						// TODO - make wait timeout configurable
						this.stateMutex
								.wait(WAIT_FOR_DEPENDENCIES_TIMEOUT_MILLIS); // wait
																				// for
																				// 30
																				// mins
																				// max
					} catch (InterruptedException e) {
						// ignore
					}

					// run this code again
					firstTime = false;

					break;

				case SHUTTING_DOWN:
					logger.warning("While waiting for service to start, the USM changed its state to: "
							+ USMState.SHUTTING_DOWN);
					return;
				case RUNNING:
					// this should be the common case
					return;
				default:
					logger.warning("Unexpected service state: " + this.state);
					return;

				}
			}
		}
	}

	@Override
	public ServiceMonitors[] getServicesMonitors() {
		logger.info("Executing getServiceMonitors()");

		// This wait is removed. If an async install fails, the shutdown methd will not be called until all blocked threads are 
		// removed. So we get a deadlock.
		// waitForServiceToStart();
		final CustomServiceMonitors csm = new CustomServiceMonitors(
				CloudifyConstants.USM_MONITORS_SERVICE_ID);

		final ServiceMonitors[] res = new ServiceMonitors[] { csm };

		USMState currentState = getState();
		// If the underlying service is not running
		if (!currentState.equals(USMState.RUNNING)) {
			csm.getMonitors().put(CloudifyConstants.USM_MONITORS_STATE_ID, currentState.ordinal());
			return res;
		}

		final Map<String, Object> map = csm.getMonitors();
		// default monitors
		putDefaultMonitorsInMap(map);

		for (final Monitor monitor : usmLifecycleBean.getMonitors()) {
			try {
				logger.fine("Executing monitor: " + monitor);
				Map<String, Number> monitorValues = monitor.getMonitorValues(
						this, usmLifecycleBean.getConfiguration());
				removeNonSerializableObjectsFromMap(monitorValues);
				// add monitor values to Monitors map
				map.putAll(monitorValues);
			} catch (final Exception e) {
				logger.log(Level.SEVERE,
						"Failed to execute a USM service monitor", e);
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Monitors are: " + Arrays.toString(res));
		}

		return res;
	}

	private void removeNonSerializableObjectsFromMap(Map<?, ?> map) {

		if (map.keySet().isEmpty()) {
			return;
		}
		Iterator<?> entries = map.entrySet().iterator();
		while (entries.hasNext()) {
			Entry<?, ?> entry = (Entry<?, ?>) entries.next();

			// a closure can not be serialized
			if (!(entry.getValue() instanceof java.io.Serializable)
					|| entry.getValue() instanceof Closure<?>) {
				logger.info("Entry "
						+ entry.getKey()
						+ " is not serializable and was not inserted to the monitors map");
				map.remove(entry.getKey());
			}
		}
	}

	private void putDefaultMonitorsInMap(final Map<String, Object> map) {
		map.put(CloudifyConstants.USM_MONITORS_CHILD_PROCESS_ID,
				this.childProcessID);
		map.put(CloudifyConstants.USM_MONITORS_ACTUAL_PROCESS_ID,
				this.actualProcessID);
		map.put(CloudifyConstants.USM_MONITORS_STATE_ID, getState().ordinal());
	}

	public long getActualProcessID() {
		return actualProcessID;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(final String serviceType) {
		this.serviceType = serviceType;
	}

	public String getServiceSubType() {
		return serviceSubType;
	}

	public void setServiceSubType(final String serviceSubType) {
		this.serviceSubType = serviceSubType;
	}

	public String getServiceDescription() {
		return serviceDescription;
	}

	public void setServiceDescription(final String serviceDescription) {
		this.serviceDescription = serviceDescription;
	}

	public String getServiceLongDescription() {
		return serviceLongDescription;
	}

	public void setServiceLongDescription(final String serviceLongDescription) {
		this.serviceLongDescription = serviceLongDescription;
	}

	public long getPostLaunchWaitPeriodMillis() {
		return postLaunchWaitPeriodMillis;
	}

	public void setPostLaunchWaitPeriodMillis(
			final long postLaunchWaitPeriodMillis) {
		this.postLaunchWaitPeriodMillis = postLaunchWaitPeriodMillis;
	}

	public File getPuExtDir() {
		return this.puExtDir;
	}

	public File getPuDir() {
		return this.puWorkDir;
	}

	@Override
	public Object invoke(final Map<String, Object> namedArgs) {

		logger.fine("Invoke called with parameters: " + namedArgs);

		// final InvocationResult invocationResult = new InvocationResult();
		// invocationResult.setInstanceId(instanceId);

		Map<String, Object> result = new HashMap<String, Object>();
		result.put(CloudifyConstants.INVOCATION_RESPONSE_INSTANCE_ID,
				this.instanceId);

		if (namedArgs == null) {
			logger.severe("recieved empty named arguments map");
			throw new IllegalArgumentException("Invoke recieved null as input");
		}

		final String commandName = (String) namedArgs
				.get(CloudifyConstants.INVOCATION_PARAMETER_COMMAND_NAME);

		if (commandName == null) {

			logger.severe("Command Name parameter in invoke is missing");
			throw new IllegalArgumentException(
					"Command Name parameter in invoke is missing");

		}

		result.put(CloudifyConstants.INVOCATION_RESPONSE_COMMAND_NAME,
				commandName);

		final Service service = ((DSLConfiguration) usmLifecycleBean
				.getConfiguration()).getService();
		final Object customCommand = service.getCustomCommands().get(
				commandName);

		if (customCommand == null) {
			throw new IllegalArgumentException("Command: " + commandName
					+ " does not exist in service: " + service.getName());
		}

		try {

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Executing custom command: " + commandName
						+ ". Custom command is: " + customCommand);
			}
			EventResult executionResult = new DSLEntryExecutor(customCommand,
					this.usmLifecycleBean.getLauncher(), this.getPuExtDir(),
					namedArgs).run();

			result.put(CloudifyConstants.INVOCATION_RESPONSE_STATUS,
					executionResult.isSuccess());
			result.put(CloudifyConstants.INVOCATION_RESPONSE_EXCEPTION,
					executionResult.getException());
			result.put(CloudifyConstants.INVOCATION_RESPONSE_RESULT,
					executionResult.getResult());

		} catch (final Exception e) {
			logger.log(Level.SEVERE,
					"Failed to execute the executeOnAllInstances section of custom command "
							+ commandName + " on instance " + instanceId, e);
			result.put(CloudifyConstants.INVOCATION_RESPONSE_STATUS, false);
			result.put(CloudifyConstants.INVOCATION_RESPONSE_EXCEPTION, e);
		}
		return result;

	}

	private Exception shutdownUSMException;
	private String[] dependencies;

	public void markUSMAsFailed(final Exception ex) {
		this.shutdownUSMException = ex;
	}

	@Override
	public boolean isMemberAliveEnabled() {
		return true;
	}

	@Override
	public boolean isAlive() throws Exception {
		if (shutdownUSMException == null) {
			return true;
		} else {
			logger.severe("USM is Alive() exiting with exception due to previous failure. Exception message was: "
					+ shutdownUSMException.getMessage());
			throw shutdownUSMException;
		}
	}

	@Override
	public void setBeanLevelProperties(BeanLevelProperties beanLevelProperties) {

		final String value = beanLevelProperties.getContextProperties()
				.getProperty(CloudifyConstants.CONTEXT_PROPERTY_ASYNC_INSTALL,
						ASYNC_INSTALL_DEFAULT_VALUE);
		logger.info("Async Install Setting: " + value);
		this.asyncInstall = Boolean.parseBoolean(value);

		final String dependenciesString = beanLevelProperties
				.getContextProperties().getProperty(
						CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, "[]");
		this.dependencies = parseDependenciesString(
				dependenciesString);
		logger.info("Dependencies for this service: "
				+ Arrays.toString(this.dependencies));

	}

	private String[] parseDependenciesString(final String dependenciesString) {
		// remove brackets
		final String internalString = dependenciesString.replace("[", "")
				.replace("]", "").trim();
		if (internalString.length() == 0) {
			return new String[0];
		}

		String[] splitResult = internalString.split(Pattern.quote(","));
		for (int i = 0; i < splitResult.length; i++) {
			splitResult[i] = splitResult[i].trim();
		}

		return splitResult;
	}

}
