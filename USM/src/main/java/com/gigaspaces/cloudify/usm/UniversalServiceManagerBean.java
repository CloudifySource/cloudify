package com.gigaspaces.cloudify.usm;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.jini.rio.boot.ServiceClassLoader;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.openspaces.core.cluster.MemberAliveIndicator;
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
import com.gigaspaces.cloudify.usm.details.Details;
import com.gigaspaces.cloudify.usm.details.DetailsException;
import com.gigaspaces.cloudify.usm.dsl.DSLConfiguration;
import com.gigaspaces.cloudify.usm.dsl.DSLEntryExecutor;
import com.gigaspaces.cloudify.usm.events.EventResult;
import com.gigaspaces.cloudify.usm.events.StartReason;
import com.gigaspaces.cloudify.usm.events.StopReason;
import com.gigaspaces.cloudify.usm.events.USMEvent;
import com.gigaspaces.cloudify.usm.launcher.USMException;
import com.gigaspaces.cloudify.usm.monitors.Monitor;
import com.gigaspaces.cloudify.usm.monitors.MonitorException;
import com.gigaspaces.internal.sigar.SigarHolder;

@Component
public class UniversalServiceManagerBean implements ApplicationContextAware,
		ClusterInfoAware, ServiceMonitorsProvider, ServiceDetailsProvider,
		InvocableService, MemberAliveIndicator {

	private static final int DEFAULT_POST_LAUNCH_WAIT_PERIOD_MILLIS = 2000;
	private static final int DEFAULT_POST_DEATH_WAIT_PERIOD_MILLIS = 2000;

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(UniversalServiceManagerBean.class.getName());

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

	private long childProcessID;

	private long actualProcessID;

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

	@PostConstruct
	public void init() throws USMException, TimeoutException {

		createUniqueFileNames();
		// This is a hack to the USM Utils class knows if we are running in a
		// USM process
		// or if this is an external script
		USMUtils.setRunningInGigaSpaceContainer(true);
		synchronized (this.stateMutex) {

			this.state = USMState.INITIALIZING;
			logger.info("USM Started. Configuration is: "
					+ usmLifecycleBean.getConfiguration());

			this.myPid = this.sigar.getPid();
			this.executors = Executors.newScheduledThreadPool(5);

			checkForRecipeTestEnvironment();

			initEvents();
			try {
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

			initShutdownHook();

			//this.state = USMState.RUNNING;

		}
		// TODO - remove this
		// getServicesDetails();
		// getServicesMonitors();
		// shutdown();

	}

	private void createUniqueFileNames() {
		try {
			this.uniqueFileNamePrefix = this.clusterName + "_"
					+ this.instanceId + "@"
					+ InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new IllegalStateException(
					"Failed to get host name of local host: " + e.getMessage(),
					e);
		}

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
		final Comparator<USMEvent> comp = new Comparator<USMEvent>() {

			@Override
			public int compare(final USMEvent arg0, final USMEvent arg1) {
				return arg0.getOrder() - arg1.getOrder();
			}
		};
		final Set<USMEvent> allEvents = new HashSet<USMEvent>();

		initEvents(allEvents, usmLifecycleBean.getInitListeners(), comp);
		initEvents(allEvents, usmLifecycleBean.getPreInstallListeners(), comp);
		initEvents(allEvents, usmLifecycleBean.getPostInstallListeners(), comp);
		initEvents(allEvents, usmLifecycleBean.getPreStartListeners(), comp);
		initEvents(allEvents, usmLifecycleBean.getPostStartListeners(), comp);
		initEvents(allEvents, usmLifecycleBean.getPreStopListeners(), comp);
		initEvents(allEvents, usmLifecycleBean.getPostStopListeners(), comp);
		initEvents(allEvents, usmLifecycleBean.getShutdownListeners(), comp);
		initEvents(allEvents, usmLifecycleBean.getPreServiceStartListeners(),
				comp);
		initEvents(allEvents, usmLifecycleBean.getPreServiceStopListeners(),
				comp);

		for (final USMEvent usmEvent : allEvents) {
			usmEvent.init(this);
		}

	}

	private USMEvent[] initEvents(final Set<USMEvent> allEvents,
			final USMEvent[] events, final Comparator<USMEvent> eventsComparator) {
		if (events.length > 0) {
			allEvents.addAll(Arrays.asList(events));
			if (events.length > 1) {
				Arrays.sort(events, eventsComparator);
			}
		}
		return events;

	}

	@PreDestroy
	public void shutdown() {

		logger.info("USM is shutting down!");

		synchronized (this.stateMutex) {
			this.state = USMState.SHUTTING_DOWN;
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

		usmLifecycleBean.install();

		launch();

	}

	private void launch() throws USMException, TimeoutException {

		// This method can be called during init, and from onProcessDeath.
		// The mutex is required as onProcessDeath fires launch in a separate
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
					this.puExtDir);

			// At this point, we assume that the main process has starts, so we
			// start reading its output
			// the output is also used to detect process death, which will be
			// fired on a different thread.
			startStreamReaders(true);

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
			
			this.state = USMState.RUNNING;

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

		// if (pidFile != null) {
		// // TODO - this is a problem! This code is not executed
		// logger.info("Looking for actual process ID in pidFile: " + pidFile);
		// this.actualProcessID = findProcessIDInPidFile(pidFile);
		// } else {
		logger.info("Looking for actual process ID in process tree");
		this.actualProcessID = findLeafProcessID(this.childProcessID, procTree);

		checkForConsoleProcess(this.actualProcessID);

		// }

		try {
			logProcessDetails();
		} catch (final SigarException e) {
			logger.log(Level.SEVERE, "Failed to log process details", e);
		}
	}

	final static String[] SHELL_PROCESS_NAMES = { "cmd.exe", "bash", "/bin/sh" };

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
	 * Starts two executor tasks responsible for reading the sysout and syserr
	 * of the external process.
	 * 
	 * @param useNotifier
	 *            if true, the tasks will notify the USM of process death by
	 *            calling onProcessDeath
	 */
	private void startStreamReaders(final boolean useNotifier) {
		final ProcessDeathNotifier notifier;

		if (useNotifier) {
			notifier = new ProcessDeathNotifier(this);
		} else {
			notifier = null;
		}

		final Level level = Level.parse(this.streamLoggerLevel);

		inputReader = new ProcessStreamReaderTask(process.getInputStream(),
				notifier, level, usmLifecycleBean.getOutputReaderLoggerName());
		errorReader = new ProcessStreamReaderTask(process.getErrorStream(),
				notifier, level, usmLifecycleBean.getErrorReaderLoggerName());

		executors.submit(inputReader);
		executors.submit(errorReader);
	}

	/********
	 * Kill process chain, starting from the actual process and going up the
	 * process tree. Does not kill the current process, of-course.
	 * 
	 * @throws USMException
	 *             if there was a problem accessing the process table
	 */
	private void killMonitoredProcessChain() throws USMException {
		final List<Long> list = USMUtils.getProcessParentChain(
				this.actualProcessID, this.myPid);

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
			logger.info("The slashpath URL is: " + url);
			URI uri;
			try {
				uri = url.toURI();
			} catch (final URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IllegalArgumentException(e);
			}
			logger.info("The slashpath URI is: " + uri);
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

	// crappy method name
	public void onProcessDeath() {
		// we use this to cancel start detection, if it is still running
		usmLifecycleBean.externalProcessDied();
		synchronized (this.stateMutex) {

			if (this.state == USMState.SHUTTING_DOWN) {
				// process has terminated due to USM shutdown
				// TODO - why fire post stop here?
				// usmLifecycleBean.firePostStop(StopReason.UNDEPLOY);
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
		logger.fine("Executing getServiceDetails()");
		final CustomServiceDetails csd = new CustomServiceDetails(
				CloudifyConstants.USM_DETAILS_SERVICE_ID, this.serviceType,
				this.serviceSubType, this.serviceDescription,
				this.serviceLongDescription);
		final ServiceDetails[] res = new ServiceDetails[] { csd };

		// if the underlying process is not running, do not execute the details
		if (!this.getState().equals(USMState.RUNNING)) {
			return res;
		}

		final Details[] alldetails = usmLifecycleBean.getDetails();
		final Map<String, Object> result = csd.getAttributes();
		for (final Details details : alldetails) {

			try {
				logger.fine("Executing details: " + details);
				final Map<String, Object> temp = details.getDetails(this,
						usmLifecycleBean.getConfiguration());
				result.putAll(temp);
			} catch (final DetailsException e) {
				logger.log(Level.SEVERE, "Failed to execute service details", e);
			}

		}

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Details are: " + Arrays.toString(res));
		}
		return res;

	}

	@Override
	public ServiceMonitors[] getServicesMonitors() {
		logger.fine("Executing getServiceMonitors()");

		final CustomServiceMonitors csm = new CustomServiceMonitors(
				CloudifyConstants.USM_MONITORS_SERVICE_ID);

		// always return the current state of this USM instance.
		csm.getMonitors().put(CloudifyConstants.USM_MONITORS_STATE_ID,
				getState());

		final ServiceMonitors[] res = new ServiceMonitors[] { csm };

		// If the underlying service is not running
		if (!getState().equals(USMState.RUNNING)) {
			return res;
		}

		final Map<String, Object> map = csm.getMonitors();
		map.put(CloudifyConstants.USM_MONITORS_CHILD_PROCESS_ID,
				this.childProcessID);
		map.put(CloudifyConstants.USM_MONITORS_ACTUAL_PROCESS_ID,
				this.actualProcessID);
		// map.put("Command Line", this.startCommandLine);

		for (final Monitor monitor : usmLifecycleBean.getMonitors()) {
			try {
				logger.fine("Executing monitor: " + monitor);
				map.putAll(monitor.getMonitorValues(this,
						usmLifecycleBean.getConfiguration()));
			} catch (final MonitorException e) {
				logger.log(Level.SEVERE,
						"Failed to execute a USM service monitor", e);
			}
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Monitors are: " + Arrays.toString(res));
		}

		return res;
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
					this.usmLifecycleBean.getLauncher(), this.getPuExtDir(), namedArgs)
					.run();

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

}
