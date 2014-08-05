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
package org.cloudifysource.usm;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cloudifysource.domain.LifecycleEvents;
import org.cloudifysource.domain.Service;
import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.domain.context.blockstorage.StorageFacade;
import org.cloudifysource.domain.entry.ExecutableDSLEntry;
import org.cloudifysource.dsl.entry.JavaExecutableEntry;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyConstants.USMState;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.dsl.utils.ServiceUtils.FullServiceName;
import org.cloudifysource.usm.commands.USMBuiltInCommand;
import org.cloudifysource.usm.dsl.DSLEntryExecutor;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.StartReason;
import org.cloudifysource.usm.events.StopReason;
import org.cloudifysource.usm.tail.RollingFileAppenderTailer;
import org.cloudifysource.usm.tail.RollingFileAppenderTailer.LineHandler;
import org.cloudifysource.utilitydomain.admin.TimedAdmin;
import org.cloudifysource.utilitydomain.context.blockstorage.ServiceVolume;
import org.cloudifysource.utilitydomain.context.kvstore.AttributesFacadeImpl;
import org.cloudifysource.utilitydomain.data.ServiceInstanceAttemptData;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.jini.rio.boot.ServiceClassLoader;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.events.ProcessingUnitInstanceAddedEventListener;
import org.openspaces.admin.space.Space;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.openspaces.core.cluster.MemberAliveIndicator;
import org.openspaces.core.properties.BeanLevelProperties;
import org.openspaces.core.properties.BeanLevelPropertiesAware;
import org.openspaces.pu.container.support.ResourceApplicationContext;
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

import com.gigaspaces.client.ChangeSet;
import com.gigaspaces.internal.sigar.SigarHolder;
import com.j_spaces.kernel.Environment;

/**********
 * The main component of the USM project - this is the bean that runs the lifecycle of a service, monitors it and
 * interacts with the service grid.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
@Component
public class UniversalServiceManagerBean implements ApplicationContextAware,
		ClusterInfoAware, ServiceMonitorsProvider, ServiceDetailsProvider,
		InvocableService, MemberAliveIndicator, BeanLevelPropertiesAware {

	private static final int MANAGEMENT_SPACE_LOOKUP_TIMEOUT = 10;
	private static final int DEFAULT_MONITORS_CACHE_EXPIRATION_TIMEOUT = 5000;
	private static final int THREAD_POOL_SIZE = 5;
	private static final int STOP_DETECTION_INTERVAL_SECS = 5;
	private static final int STOP_DETECTION_INITIAL_INTERVAL_SECS = 2;
	private static final int INTEGREATED_PU_INIT_TIMEOUT_SECS = 5;
	private static final int PRE_SHUTDOWN_TIMEOUT_MILLIS = 10000;
	private static final String ERROR_FILE_NAME_SUFFFIX = ".err";
	private static final String OUTPUT_FILE_NAME_SUFFIX = ".out";
	private static final int WAIT_FOR_DEPENDENCIES_INTERVAL_MILLIS = 5000;
	private static final int WAIT_FOR_DEPENDENCIES_TIMEOUT_MILLIS = 1000 * 60 * 30;
	private static final String ASYNC_INSTALL_DEFAULT_VALUE = "true";
	private static final int FILE_TAILER_INTERVAL_SECS_DEFAULT = 5;
	private static final int DEFAULT_POST_LAUNCH_WAIT_PERIOD_MILLIS = 2000;
	private static final int DEFAULT_POST_DEATH_WAIT_PERIOD_MILLIS = 2000;

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(UniversalServiceManagerBean.class.getName());

	private final Sigar sigar = SigarHolder.getSigar();
	private File puWorkDir;

	private final Object stateMutex = new Object();
	private final Object deallocationMutext = new Object();
	private USMState state = USMState.INITIALIZING;

	public USMState getState() {
		return state;
	}

	@Autowired(required = true)
	private USMLifecycleBean usmLifecycleBean;
	
	@Autowired(required = true)
	private USMBuiltInCommand[] builtInCommands = null;

	private Process process;

	private String streamLoggerLevel = Level.INFO.getName();

	private ScheduledExecutorService executors;

	private long myPid;

	// these values will change after an unexpected process death, and other
	// threads may read them, so they need to be volatile
	private volatile long childProcessID;

	private File puExtDir;

	private long postLaunchWaitPeriodMillis = DEFAULT_POST_LAUNCH_WAIT_PERIOD_MILLIS;
	private final long postDeathWaitPeriodMillis = DEFAULT_POST_DEATH_WAIT_PERIOD_MILLIS;

	private int instanceId;
	private boolean runningInGSC = true;
	private ApplicationContext applicationContext;
	private String clusterName;
	private String uniqueFileNamePrefix;
	private ProcessDeathNotifier processDeathNotifier;
	private RollingFileAppenderTailer tailer;

	private int fileTailerIntervalSecs = FILE_TAILER_INTERVAL_SECS_DEFAULT;

	// asynchronous installation
	private boolean asyncInstall = true;
	private List<Long> serviceProcessPIDs;

	// monitors accessor and thread-safe cache.
	private MonitorsCache monitorsCache;

	private GigaSpace managementSpace;
	private int retries;
	private int currentAttempt;
	private ServiceInstanceAttemptData instanceAttemptData;

	/********
	 * The USM Bean entry point. This is where processing of a service instance starts.
	 * 
	 * @throws USMException
	 *             if initialization failed.
	 * @throws TimeoutException .
	 */
	@PostConstruct
	public void init() throws USMException, TimeoutException {

		initServiceName();
		initMonitorsCache();

		initUniqueFileName();
		initCustomProperties();
		this.myPid = this.sigar.getPid();

		final boolean existingProcessFound = checkForPIDFile();
		initRetryData();

		initManagementSpace();

		// Initialize and sort events
		initEvents();

		reset(existingProcessFound);
	}

	private void initRetryData() {
		retries = this.usmLifecycleBean.getConfiguration().getService().getRetries();
		logger.info("Retry limit for this service is: " + retries + ". Retry in use: " + isRetryLimitUsed());
	}

	private boolean isRetryLimitUsed() {
		return (retries != -1);
	}

	private void writeRetryData() {
		if (!isRetryLimitUsed()) {
			return;
		}

		if (this.instanceAttemptData == null) {
			this.instanceAttemptData = createInstanceAttemptDataTemplate();
			instanceAttemptData.setCurrentAttemptNumber(2);
		} else {
			this.instanceAttemptData.setCurrentAttemptNumber(this.instanceAttemptData.getCurrentAttemptNumber() + 1);
		}

		logger.info("Writing attempt data to space with instance number: "
				+ this.instanceAttemptData.getCurrentAttemptNumber());
		this.managementSpace.write(this.instanceAttemptData);

	}

	private void clearRetryData() {
		retries = this.usmLifecycleBean.getConfiguration().getService().getRetries();

		if (retries == -1) {
			return;
		}

		ServiceInstanceAttemptData template = createInstanceAttemptDataTemplate();
		this.managementSpace.clear(template);
	}

	private boolean isRetryLimitExceeded() {
		retries = this.usmLifecycleBean.getConfiguration().getService().getRetries();
		logger.info("Retry limit for this service is: " + retries);
		if (retries == -1) {
			return false;
		}

		currentAttempt = getCurrentAttemptNumber();
		logger.info("Current attempt number is: " + currentAttempt);

		return currentAttempt > retries;

	}

	private int getCurrentAttemptNumber() {
		final ServiceInstanceAttemptData template = createInstanceAttemptDataTemplate();

		instanceAttemptData = this.managementSpace.read(template);
		if (instanceAttemptData == null) {
			return 1;
		} else {
			return instanceAttemptData.getCurrentAttemptNumber();
		}

	}

	private ServiceInstanceAttemptData createInstanceAttemptDataTemplate() {
		final ServiceInstanceAttemptData template = new ServiceInstanceAttemptData();

		final FullServiceName fullName = ServiceUtils.getFullServiceName(this.clusterName);

		template.setApplicationName(fullName.getApplicationName());
		template.setGscPid(this.myPid);
		template.setServiceName(fullName.getServiceName());
		template.setInstanceId(this.instanceId);
		return template;
	}

	/**********
	 * Bean shutdown method, responsible for shutting down the external service and releasing all resource.
	 */
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

			if (executors != null) {
				executors.shutdown();
			}

			try {
				getUsmLifecycleBean().fireShutdown();
			} catch (final USMException e) {
				logger.log(Level.SEVERE, "Failed to execute shutdown event: "
						+ e.getMessage(), e);
			}

			// after shutdown, no further events are expected to be
			// executed.
			// So we delete the service folder contents. This is just in
			// case the GSC does
			// not properly clean up the service folder. If an underlying
			// process is leaking,
			// this may cause all sorts of problems, though.
			if (this.runningInGSC) {
				deleteExtDirContents(); // avoid deleting contents in
				// Integrated PU
			}

			try {
				deAllocateStorageSync();
			} catch (final Exception e) {
				getUsmLifecycleBean().log("Failed de-allocating storage volume. this may cause a leak : "
						+ ExceptionUtils.getRootCauseMessage(e));
				logger.log(Level.WARNING, "Failed to deallocate storage: "
						+ e.getMessage(), e);
			}

			USMUtils.shutdownAdmin();
		}
		// Sleep for 10 seconds to allow rest to poll for shutdown lifecycle
		// events
		// form the GSC logs before GSC is destroyed.
		try {
			Thread.sleep(PRE_SHUTDOWN_TIMEOUT_MILLIS);
		} catch (final InterruptedException e) {
			logger.log(
					Level.INFO,
					"Failed to stall GSC shutdown. Some lifecycle logs may not have been recorded.",
					e);
		}
		logger.info("USM shut down completed!");

	}

	private void initManagementSpace() throws USMException {
		// initialize management space, except if running in test-recipe container.
		if (this.isRunningInGSC()) {
			managementSpace =
					((AttributesFacadeImpl) getUsmLifecycleBean().getConfiguration().getServiceContext()
							.getAttributes())
							.getManagementSpace();
		} else {
			logger.info("initManagementSpace is getting timed admin");
			final TimedAdmin timedAdmin = USMUtils.getTimedAdmin();
			Space space = timedAdmin.waitForSpace(CloudifyConstants.MANAGEMENT_SPACE_NAME,
					MANAGEMENT_SPACE_LOOKUP_TIMEOUT, TimeUnit.SECONDS);

			if (space == null) {
				// this is only valid in test-recipe
				final String testRecipeIndicator = System.getProperty(CloudifyConstants.TEST_RECIPE_TIMEOUT_SYSPROP);
				if (StringUtils.isBlank(testRecipeIndicator)) {
					throw new USMException(
							"Failed to locate management space - USM initialization cannot continue");
				}
			} else {
				this.managementSpace = space.getGigaSpace();
			}

		}
	}

	// called on USM startup, or if the process died unexpectedly and is being
	// restarted
	private void reset(final boolean existingProcessFound) throws USMException,
			TimeoutException {
		synchronized (this.stateMutex) {

			this.state = USMState.INITIALIZING;
			// TODO improve the output: "Configuration is: org.cloudifysource.usm.dsl.ServiceConfiguration@69a66ce8"
			logger.info("USM Started. Configuration is: "
					+ getUsmLifecycleBean().getConfiguration());

			this.executors = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
			// Auto shutdown if this is a Test-Recipe run
			checkForRecipeTestEnvironment();

			// check for PID file
			if (existingProcessFound) {
				// found an existing process, so no need to launch
				startAsyncTasks();

				// start file monitoring task too
				startFileMonitoringTask();

				this.state = USMState.RUNNING;

				return;
			}
			try {
				// Launch the process
				startProcessLifecycle();
			} catch (final USMException e) {
				logger.log(
						Level.SEVERE,
						"Process lifecycle failed to start. Shutting down the USM instance",
						e);
				try {
					this.shutdown();
				} catch (final Exception e2) {
					logger.log(
							Level.SEVERE,
							"While shutting down the USM due to a failure in initialization, "
									+ "the following exception occured: "
									+ e.getMessage(), e);
				}
				throw e;
			}
		}
	}

	private void allocateStorage() throws USMException, TimeoutException {

		final Service service = getUsmLifecycleBean().getConfiguration().getService();
		final String storageTemplateName = service.getStorage().getTemplate();

		if (StringUtils.isNotBlank(storageTemplateName)) {

			try {

				logger.info("Allocating static storage for service "
						+ getUsmLifecycleBean().getConfiguration().getServiceContext());
				final StorageFacade storage = getUsmLifecycleBean().getConfiguration().getServiceContext().getStorage();
				final StorageTemplate storageTemplate = storage.getTemplate(storageTemplateName);
				String deviceName = storageTemplate.getDeviceName();
				getUsmLifecycleBean().log("Allocating storage with size " + storageTemplate.getSize() + "GB");
				final ServiceVolume serviceVolume =
						getServiceVolumeFromSpace(getUsmLifecycleBean().getConfiguration().getServiceContext());

				if (serviceVolume == null) {
					logger.fine("service volume is null. this means it hasn't been created yet.");
					getUsmLifecycleBean().log("Creating volume");
					final String volumeId = storage.createVolume(storageTemplateName);
					getUsmLifecycleBean().log("Volume created with id " + volumeId);
					// flag this volume as static. this is to identify it when deallocating on shutdown.
					final ServiceVolume newServiceVolume = managementSpace.readById(ServiceVolume.class, volumeId);
					logger.fine("Flagging service volume with id " + volumeId + " to be static storage.");
					managementSpace.change(newServiceVolume, new ChangeSet().set("dynamic", false));
					getUsmLifecycleBean().log("Attaching volume to device " + deviceName);
					storage.attachVolume(volumeId, deviceName);
					if (storageTemplate.isPartitioningRequired()) {
						getUsmLifecycleBean().log("Partitioning volume at " + deviceName);
						storage.partition(volumeId, deviceName);
					}
					getUsmLifecycleBean().log("Formatting volume to filesystem " + storageTemplate.getFileSystemType());
					storage.format(volumeId, deviceName, storageTemplate.getFileSystemType());
					getUsmLifecycleBean().log("Mounting volume to " + storageTemplate.getPath());
					storage.mount(volumeId, deviceName, storageTemplate.getPath());
				} else {
					String voluemId = serviceVolume.getId();
					logger.fine("Detected an existing volume for this service upon allocation. volume Id: " + voluemId
							+ ", found in state : " + serviceVolume.getState());
					
					switch (serviceVolume.getState()) {

					case CREATED:
						getUsmLifecycleBean().log("Attaching volume with id " + voluemId + " to device " + deviceName);
						storage.attachVolume(voluemId, deviceName);

					case ATTACHED:
						if (storageTemplate.isPartitioningRequired()) {
							getUsmLifecycleBean().log("Partitioning volume with id " + voluemId + " at " + deviceName);
							storage.partition(voluemId, deviceName);
						}
						
					case PARTITIONED:
						getUsmLifecycleBean().log("Formatting volume with id " + voluemId + " to filesystem " 
								+ storageTemplate.getFileSystemType());
						storage.format(voluemId, deviceName, storageTemplate.getFileSystemType());
						
					case FORMATTED:
						getUsmLifecycleBean().log("Mounting volume with id " + voluemId + " to " 
								+ storageTemplate.getPath());
						storage.mount(voluemId, deviceName, storageTemplate.getPath());

					case MOUNTED:
						break;

					default:
						throw new IllegalStateException("Unexpected state of service volume : "
								+ serviceVolume.getState());
					}
				}
			} catch (final Exception e) {
				getUsmLifecycleBean().log("Storage allocation failed : " + e.getMessage());
				if (e instanceof TimeoutException) {
					throw (TimeoutException) e;
				}
				throw new USMException(e);
			}
		}
	}
	

	private void deAllocateStorageSync() throws USMException, TimeoutException {

		logger.fine("Waiting for de-allocation mutex to be released");
		synchronized (this.deallocationMutext) {
			logger.fine("Acquired lock on de-allocation mutex[" + deallocationMutext.hashCode() + "]");
			deAllocateStorage();
		}
	}

	private void deAllocateStorage() throws USMException, TimeoutException {

		final Service service = getUsmLifecycleBean().getConfiguration().getService();
		final String storageTemplateName = service.getStorage().getTemplate();

		if (StringUtils.isNotBlank(storageTemplateName)) {

			final StorageFacade storage = getUsmLifecycleBean().getConfiguration().getServiceContext().getStorage();

			try {

				logger.info("De-Allocating static storage for service "
						+ getUsmLifecycleBean().getConfiguration().getServiceContext());

				final ServiceVolume serviceVolume =
						getServiceVolumeFromSpace(getUsmLifecycleBean().getConfiguration().getServiceContext());
				if (serviceVolume == null) {
					logger.fine("Could not find a volume for this service in the management space. "
							+ "this probably means there was a problem during volume creation, "
							+ "or it has already been de-allocated");
				} else {
					getUsmLifecycleBean().log("De-allocating storage");
					logger.fine("Detected an existing volume for this service upon de-allocation. found in state : "
							+ serviceVolume.getState());
					final StorageTemplate template = storage.getTemplate(storageTemplateName);
					final boolean deleteStorage = template.isDeleteOnExit();
					logger.fine("Storage will be deleted = " + deleteStorage);
					
					switch (serviceVolume.getState()) {

					case MOUNTED:
						getUsmLifecycleBean().log("Unmounting volume from " + template.getPath());
						storage.unmount(serviceVolume.getDevice());

					case ATTACHED:
					case PARTITIONED:
					case FORMATTED:
						getUsmLifecycleBean().log("Detaching volume from  " + serviceVolume.getDevice());
						storage.detachVolume(serviceVolume.getId());

					case CREATED:
						if (deleteStorage) {
							getUsmLifecycleBean().log("Deleting volume with id " + serviceVolume.getId());
							storage.deleteVolume(serviceVolume.getId());
						}
						break;

					default:
						throw new IllegalStateException("Unexpected state of service volume : "
								+ serviceVolume.getState());

					}
				}
			} catch (final Exception e) {
				if (e instanceof TimeoutException) {
					throw (TimeoutException) e;
				}
				throw new USMException(e);
			}
		}
	}

	private ServiceVolume getServiceVolumeFromSpace(final ServiceContext serviceContext) {
		final ServiceVolume serviceVolume = new ServiceVolume();
		serviceVolume.setApplicationName(serviceContext.getApplicationName());
		serviceVolume.setServiceName(serviceContext.getServiceName());
		serviceVolume.setIp(serviceContext.getBindAddress());
		serviceVolume.setDynamic(false);
		logger.info("Retrieving service volume from space for service context " + serviceContext
				+ " . Using template : " + serviceVolume);
		return managementSpace.read(serviceVolume);
	}

	/******
	 * The service name is defined by the clusterInfo, which is set from the recipe parameters during deployment.
	 * However, when running in test mode inside the integrated processing unit container, the PU name is not set. This
	 * 'workaround' makes it look like the ClusterInfo is set correctly.
	 */
	private void initServiceName() {
		if (!this.runningInGSC) {
			if (this.clusterInfo == null) {
				throw new IllegalStateException("ClusterInfo field is null while running in test mode!");
			}
			this.clusterInfo.setName(ServiceUtils.getAbsolutePUName(CloudifyConstants.DEFAULT_APPLICATION_NAME,
					this.usmLifecycleBean.getConfiguration().getService().getName()));
		}

	}

	private void initMonitorsCache() {
		final String tmp = this.usmLifecycleBean
				.getConfiguration()
				.getService()
				.getCustomProperties()
				.get(CloudifyConstants.CUSTOM_PROPERTY_MONITORS_CACHE_EXPIRATION_TIMEOUT);
		long cacheExpirationTimeout = DEFAULT_MONITORS_CACHE_EXPIRATION_TIMEOUT;
		if (tmp != null) {
			cacheExpirationTimeout = Long.parseLong(tmp);
		}
		this.monitorsCache = new MonitorsCache(this, this.usmLifecycleBean,
				cacheExpirationTimeout);
	}

	private void initCustomProperties() {
		final Map<String, String> props = getUsmLifecycleBean()
				.getCustomProperties();
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
		final String clusterName = this.clusterName == null ? ServiceUtils
				.getAbsolutePUName(CloudifyConstants.DEFAULT_APPLICATION_NAME,
						"USM") : this.clusterName;

		try {
			return clusterName + "_" + this.instanceId + "_" + username + "@"
					+ InetAddress.getLocalHost().getHostName();
		} catch (final UnknownHostException e) {
			logger.log(
					Level.WARNING,
					"Failed to resolve localhost name - this usually indicates a problem with the OS configuration",
					e);
			return clusterName + "_" + this.instanceId + "_" + username
					+ "@127.0.0.1";
		}

	}

	private void initUniqueFileName() {

		this.uniqueFileNamePrefix = getLogsDir() + File.separator
				+ createUniqueFileName();

	}

	private void checkForRecipeTestEnvironment() {

		final String timeoutValue = System
				.getProperty(CloudifyConstants.TEST_RECIPE_TIMEOUT_SYSPROP);
		if (timeoutValue == null) {
			return;
		}
		final int timeout = Integer.parseInt(timeoutValue);

		logger.info("USM is running in Test mode. USM will shut down in: "
				+ timeout + " seconds");
		this.executors.schedule(new TestRecipeShutdownRunnable(
				this.applicationContext, this), timeout, TimeUnit.SECONDS);
	}

	private void initEvents() {

		getUsmLifecycleBean().initEvents(this);

	}

	private void deleteExtDirContents() {
		if (this.puExtDir != null) {
			deleteDir(this.puExtDir);
		}
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

	private void startProcessLifecycle() throws USMException, TimeoutException {

		if (this.instanceId == 1) {
			getUsmLifecycleBean().firePreServiceStart();
		}

		getUsmLifecycleBean().fireInit();

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
				} catch (final Exception e) {
					logger.log(
							Level.SEVERE,
							"Asynchronous install failed with message: "
									+ e.getMessage()
									+ ". Instance will shut down", e);
					markUSMAsFailed(e);
				}

			}
		}, INTEGREATED_PU_INIT_TIMEOUT_SECS, TimeUnit.SECONDS);
	}

	private void registerPostPUILifecycleTask() {
		logger.info("registerPostPUILifecycleTask is getting timed admin");
		final TimedAdmin timedAdmin = USMUtils.getTimedAdmin();
		final ProcessingUnit pu = timedAdmin.waitForPU(this.clusterName, 30, TimeUnit.SECONDS);

		final int instanceIdToMatch = this.instanceId;

		if (pu == null) {
			throw new IllegalStateException(
					"Could not find Processing Unit with name: "
							+ this.clusterName
							+ " to register event listener. This may indicate a discovery problem with your network. "
							+ "Please contact the system administrator");
		}

		pu.getProcessingUnitInstanceAdded().add(
				new ProcessingUnitInstanceAddedEventListener() {

					@Override
					public void processingUnitInstanceAdded(
							final ProcessingUnitInstance processingUnitInstance) {
						if (processingUnitInstance.getInstanceId() == instanceIdToMatch) {
							// first, remove the listener
							pu.getProcessingUnitInstanceAdded().remove(this);

							// my PUI is ready
							// on recommendation of Itai, long running code
							// should not run in the admin API's thread pool.
							// So moving this to the USM's thread pool.
							executors.execute(new Runnable() {

								@Override
								public void run() {
									try {
										installAndRun();
									} catch (final Exception e) {
										logger.log(
												Level.SEVERE,
												"Asynchronous install failed with message: "
														+ e.getMessage()
														+ ". Instance will shut down",
												e);
										markUSMAsFailed(e);
									}

								}
							});

						}

					}
				});
	}

	private void installAndRun() throws USMException, TimeoutException {
		try {
			allocateStorage();
			getUsmLifecycleBean().install();
			if (this.asyncInstall) {
				waitForDependencies();
			}
			launch();

			// install finished correctly - clear attempt data
			clearRetryData();

		} catch (final USMException e) {

			if (!this.selfHealing) {
				// This exception is intentionally swallowed so the USM will not be reloaded by the GSM
				// for a retry.
				logger.log(
						Level.SEVERE,
						"An exception was encountered while executing the service lifecycle. "
								+ "Self-healing is disabled so this service will not be restarted.",
						e);
				this.state = USMState.ERROR;
			} else if (isRetryLimitExceeded()) {
				//
				logger.log(
						Level.SEVERE,
						"An exception was encountered while executing the service lifecycle. "
								+ "Current installation attempt is " + this.currentAttempt
								+ " which exceeds the The retry limit(" + this.retries
								+ "), so this service will not be restarted.",
						e);
				this.state = USMState.ERROR;
			} else {
				logger.warning("Lifecycle failed. Self-healing is active and retry limit was not exceeded - instance "
						+ "will be restarted.");
				writeRetryData();
				throw e;
			}
		}

	}

	private void waitForDependencies() {
		logger.info("Waiting for dependencies");
		final long startTime = System.currentTimeMillis();
		final long endTime = startTime + WAIT_FOR_DEPENDENCIES_TIMEOUT_MILLIS;
		if (dependencies.length > 0) {
			logger.fine("waitForDependencies is getting timed admin");
			final TimedAdmin timedAdmin = USMUtils.getTimedAdmin();
			for (final String dependencyService : this.dependencies) {
				logger.info("Waiting for dependency: " + dependencyService);
				final ProcessingUnit pu = waitForPU(endTime, timedAdmin, dependencyService);
				waitForPUI(endTime, timedAdmin, dependencyService, pu);
				logger.info("Dependency " + dependencyService + " is available");
			}
		}

		logger.info("All dependencies are available");
	}

	private void waitForPUI(final long endTime, final TimedAdmin timedAdmin, final String dependencyService, 
			final ProcessingUnit pu) {
		
		while (true) {
			final long waitForPUIPeriod = endTime - System.currentTimeMillis();
			if (waitForPUIPeriod <= 0) {
				throw new IllegalStateException("Could not find dependency "
						+ dependencyService + " required for this service");
			}

			logger.info("Waiting for PUI of service: " + dependencyService + " for " + waitForPUIPeriod 
					+ " Milliseconds");
			// TODO: Switch to waitFor using waitForPUPeriod. this admin
			// sampling routine is a workaround for a
			// possible bug in the admin api where the admin does not recognize
			// the PUI.
			final boolean found = timedAdmin.waitForPUI(pu, 1, 1, TimeUnit.SECONDS);

			logger.info("Timeout ended. processing unit " + dependencyService
					+ " found result is " + found);
			if (found) {
				final ProcessingUnitInstance[] puis = pu.getInstances();
				logger.info("Found " + puis.length + " instances");
				for (final ProcessingUnitInstance pui : puis) {
					final ServiceMonitors sm = pui.getStatistics()
							.getMonitors()
							.get(CloudifyConstants.USM_MONITORS_SERVICE_ID);
					if (sm != null) {
						final Object stateObject = sm.getMonitors().get(
								CloudifyConstants.USM_MONITORS_STATE_ID);
						logger.info("USM state is: " + stateObject);
						if (stateObject == null) {
							logger.warning("Could not find the instance state in the PUI monitors");
						} else {
							final int stateIndex = (Integer) stateObject;
							final USMState usmState = USMState.values()[stateIndex];
							logger.info("PUI is in state: " + usmState);
							if (usmState == USMState.RUNNING) {
								logger.info("Found a running instance of dependency service: " + dependencyService);
								return;
							}
						}

					}
				}
			} else {
				logger.info("Could not find a running instance of service: "
						+ dependencyService + ". Sleeping before trying again");
			}
			try {
				Thread.sleep(WAIT_FOR_DEPENDENCIES_INTERVAL_MILLIS);
			} catch (final InterruptedException e) {
				// ignore.
			}

		}
	}

	private ProcessingUnit waitForPU(final long endTime, final TimedAdmin timedAdmin,
			final String dependantService) {
		ProcessingUnit pu = null;
		while (true) {
			final long waitForPUPeriod = endTime - System.currentTimeMillis();
			if (waitForPUPeriod <= 0) {
				throw new IllegalStateException("Could not find dependency "
						+ dependantService + " required for this service");
			}

			logger.info("Waiting for PU: " + dependantService);
			// TODO: Switch to waitFor using waitForPUPeriod. this admin
			// sampling routine is a workaround for a
			// possible bug in the admin api where the admin does not recognize
			// the PU.
			pu = timedAdmin.waitForPU(dependantService, 2, TimeUnit.MILLISECONDS);
			if (pu != null) {
				return pu;
			}
		}
	}

	/**********
	 * Checks if a PID file exists from a previous execution of this service and instance on this host.
	 * 
	 * @return true if a valid pid file matching a running process was found.
	 * 
	 * @throws IOException
	 *             if the PID file could not be read.
	 * @throws USMException
	 *             if there was a problem checking for a PID file.
	 */
	private boolean checkForPIDFile() throws USMException {

		final File file = getPidFile();
		if (file.exists()) {
			// Found a PID file - read it
			String pidString;
			try {
				pidString = FileUtils.readFileToString(file).trim();
			} catch (final IOException e) {
				throw new USMException(
						"Failed to read pid file contents from file: " + file,
						e);
			}

			// parse it
			final List<Long> pidsFromFile = parsePIDsString(file, pidString);

			// check if any process is running
			this.childProcessID = 0; // can't be sure of child process PID -
										// better to leave empty
			final boolean atLeastOneProcessAlive = isAnyProcessAlive(file,
					pidsFromFile);

			if (atLeastOneProcessAlive) {
				this.serviceProcessPIDs = pidsFromFile;

				// USM will monitor processes started by a previous GSC which
				// must have died unexpectedly
				try {
					logProcessDetails();
				} catch (final SigarException e) {
					logger.log(Level.SEVERE, "Failed to log process details", e);
				}

				return true;
			} else {
				logger.warning("PID file: "
						+ file
						+ " was found with PIDs: "
						+ pidsFromFile
						+ " but these process do not exist. PID Files will be deleted.");
				deleteProcessFiles(file);
				return false;
			}

		}

		// just in case an output/error file remained on the file system.
		deleteProcessFiles(file);
		return false;
	}

	private boolean isAnyProcessAlive(final File file,
			final List<Long> pidsFromFile) throws USMException {
		boolean atLeastOneProcessAlive = false;
		for (final Long pid : pidsFromFile) {
			if (USMUtils.isProcessAlive(pid)) {
				atLeastOneProcessAlive = true;
				logger.info("Found Active Process with PID: " + pid
						+ " from file: " + file);
			}
		}
		return atLeastOneProcessAlive;
	}

	private List<Long> parsePIDsString(final File file, final String pidString)
			throws USMException {
		if (pidString.length() == 0) {
			return new ArrayList<Long>(0);
		}

		final String[] pidParts = pidString.split(",");
		final List<Long> pidsFromFile = new ArrayList<Long>(pidParts.length);
		for (final String part : pidParts) {
			try {
				pidsFromFile.add(Long.parseLong(part));
			} catch (final NumberFormatException nfe) {
				throw new USMException(
						"The contents of the PID file: "
								+ file
								+ " cannot be parsed to long values: "
								+ pidString
								+ ". Check the file contents and delete the file before retrying.",
						nfe);
			}

		}
		return pidsFromFile;
	}

	private void deleteProcessFiles(final File file) {
		FileUtils.deleteQuietly(file);
		FileUtils.deleteQuietly(getErrorFile());
		FileUtils.deleteQuietly(getOutputFile());
	}

	/************
	 * Mode of process executed by the USM.
	 * 
	 * @author barakme
	 * 
	 */
	private enum USMProcessMode {
		NO_PROCESS, FOREGROUND, BACKGROUND
		// Do we need a SERVICE type, indicating processes started as OS
		// services?
	}

	private USMProcessMode processMode = USMProcessMode.FOREGROUND;
	private ClusterInfo clusterInfo;

	private void launch() throws USMException, TimeoutException {

		// This method can be called during init, and from onProcessDeath.
		// The mutex is required as onProcessDeath fires on a separate
		// thread.
		synchronized (this.stateMutex) {
			this.state = USMState.LAUNCHING;
			getUsmLifecycleBean().firePreStart(StartReason.DEPLOY);

			// bit of a hack, but not that bad.
			getUsmLifecycleBean().logProcessStartEvent();

			try {
				// clear the start command process, in case this is a retry of
				// the process launch
				this.process = null;
				// same for the process list.
				this.serviceProcessPIDs = new ArrayList<Long>(0);

				final ExecutableDSLEntry startCommand = getUsmLifecycleBean()
						.getConfiguration().getService().getLifecycle()
						.getStart();
				if (startCommand == null) {
					logger.info("No start command specified in recipe. No processes will be launched or monitored");
					this.serviceProcessPIDs = new ArrayList<Long>(0);
				} else {
					try {

						this.process = getUsmLifecycleBean().getLauncher()
								.launchProcessAsync(startCommand,
										this.puExtDir, getOutputFile(),
										getErrorFile(), LifecycleEvents.START);
					} catch (final USMException e) {
						getUsmLifecycleBean().logProcessStartFailureEvent(
								e.getMessage());
						throw e;
					}
				}

				// read output and error files for launched process, and print
				// to GSC log
				startFileMonitoringTask();

				// Now we check start detection - waiting for the application
				// level
				// tests to indicate
				// that the process is ready for work. Usually means that the
				// server
				// ports are open
				// or that a specific string was printed to a log file.
				logger.info("Executing process liveness test");
				if (!getUsmLifecycleBean().isProcessLivenessTestPassed(
						this.process)) {
					final long startDetectionTimeoutSecs = getUsmLifecycleBean()
							.getConfiguration().getService().getLifecycle()
							.getStartDetectionTimeoutSecs();

					throw new USMException(
							"The Start Detection test failed in the defined time period: "
									+ startDetectionTimeoutSecs + " seconds."
									+ " Shutting down this instance.");
				}
				logger.info("Process liveness test passed");

				final Integer exitCode = USMUtils
						.getProcessExitCode(this.process);
				initServiceProcessMode(this.process, exitCode);
			} finally {
				// in case of an error, process start failure or start detection
				// failure, we still want to run process
				// locators
				// so we know what processes to kill.
				logger.info("Executing Process Locators!");
				this.serviceProcessPIDs = getUsmLifecycleBean()
						.getServiceProcesses();
				logger.info("Monitored processes: " + this.serviceProcessPIDs);
				writePidsToFile();

			}

			getUsmLifecycleBean().firePostStart(StartReason.DEPLOY);

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

	private void initServiceProcessMode(final Process process,
			final Integer exitCode) {
		if (process == null) {
			this.processMode = USMProcessMode.NO_PROCESS;
		} else {
			if (exitCode == null) {
				this.processMode = USMProcessMode.FOREGROUND;
			} else {
				this.processMode = USMProcessMode.BACKGROUND;
				if (exitCode != 0) {
					logger.warning("The service start detection test finished successfully, "
							+ "but the start command exited with an error code of: "
							+ exitCode
							+ ". This may indicate a problem with the service.");
				}
			}
		}
	}

	private void writePidsToFile() throws USMException {
		if (this.serviceProcessPIDs.size() > 0) {
			final String pidsString = StringUtils.join(this.serviceProcessPIDs,
					",");
			try {
				FileUtils.writeStringToFile(getPidFile(), pidsString);
			} catch (final IOException e) {
				throw new USMException("Failed to write PIDs list to file", e);
			}
		}

	}

	private void logProcessDetails() throws SigarException {
		if (logger.isLoggable(Level.INFO)) {
			if (this.childProcessID != 0) {
				logger.info("Child PID: " + this.childProcessID);
				logger.info("Process ID of Child Process is: "
						+ this.childProcessID + ", Executable is: "
						+ this.sigar.getProcExe(this.childProcessID).getName());
			}

			if (this.serviceProcessPIDs.size() > 0) {
				if (logger.isLoggable(Level.INFO)) {
					logger.info("Actual Monitored Process IDs are: ");
					for (final Long pid : this.serviceProcessPIDs) {
						logger.info(this.serviceProcessPIDs + ": "
								+ this.sigar.getProcExe(pid).getName());
					}
				}

			}
		}
	}

	public long getContainerPid() {
		return this.myPid;
	}

	/*******
	 * Starts async tasks responsible for: - reading the output and error files generated by the external process. -
	 * waits for process to die, if in the foreground - runs stop detection
	 * 
	 * 
	 **/
	private void startAsyncTasks() {

		logger.info("Starting async tasks");
		this.processDeathNotifier = new ProcessDeathNotifier(this);
		// make sure all death notifications are applied to the current process
		final ProcessDeathNotifier notifier = this.processDeathNotifier;

		// Schedule Stop Detector task
		final Runnable task = new Runnable() {

			@Override
			public void run() {
				final boolean serviceIsStopped = getUsmLifecycleBean()
						.runStopDetection();
				if (serviceIsStopped) {
					notifier.processDeathDetected();
				}

			}
		};
		executors.scheduleWithFixedDelay(task,
				STOP_DETECTION_INITIAL_INTERVAL_SECS,
				STOP_DETECTION_INTERVAL_SECS, TimeUnit.SECONDS);

	}

	private void startFileMonitoringTask() {
		// We should switch this to the file system notifier when Java 7 comes
		// out
		// Schedule task for reading output and error files.
		if (this.tailer == null) {
			this.tailer = createFileTailerTask();
		}
		logger.info("Launching tailer task");
		executors.scheduleWithFixedDelay(tailer, 1, fileTailerIntervalSecs,
				TimeUnit.SECONDS);
	}

	private RollingFileAppenderTailer createFileTailerTask() {
		final String filePattern = createUniqueFileName() + "("
				+ OUTPUT_FILE_NAME_SUFFIX + "|" + ERROR_FILE_NAME_SUFFFIX + ")";

		final Logger outputLogger = Logger.getLogger(getUsmLifecycleBean()
				.getOutputReaderLoggerName());
		final Logger errorLogger = Logger.getLogger(getUsmLifecycleBean()
				.getErrorReaderLoggerName());

		logger.info("Creating tailer for dir: " + getLogsDir()
				+ ", with regex: " + filePattern);
		final RollingFileAppenderTailer tailer = new RollingFileAppenderTailer(
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

	private void stop(final StopReason reason) {
		try {
			getUsmLifecycleBean().firePreStop(reason);
		} catch (final USMException e) {
			logger.log(Level.SEVERE,
					"Failed to execute pre stop event: " + e.getMessage(), e);
		}

		try {
			getUsmLifecycleBean().fireStop(reason);
		} catch (final USMException e) {
			logger.log(Level.SEVERE,
					"Failed to execute stop event: " + e.getMessage(), e);
		}

		try {
			getUsmLifecycleBean().firePostStop(reason);
		} catch (final USMException e) {
			logger.log(Level.SEVERE,
					"Failed to execute post stop event: " + e.getMessage(), e);
		}
	}

	// CHECKSTYLE:OFF - BeansException is a XAP openspaces interface.
	@Override
	public void setApplicationContext(final ApplicationContext arg0)
			throws BeansException {
		// CHECKSTYLE:ON
		this.applicationContext = arg0;

		if (arg0.getClassLoader() instanceof ServiceClassLoader) {
			// running in GSC
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

	// crappy method name
	/*********
	 * Called to notify USM that the service has stopped. This may be due to a USM shutdown, a USM command, or an
	 * unexpected service failure.
	 */
	public void onProcessDeath() {
		logger.info("Detected death of underlying process");
		// we use this to cancel start detection, if it is still running

		synchronized (this.stateMutex) {

			if (this.state != USMState.RUNNING) {
				// process is shutting down or restarting, nothing to do now.
				return;
			}

			try {
				// unexpected process failure
				getUsmLifecycleBean().firePostStop(StopReason.PROCESS_FAILURE);
			} catch (final Exception e) {
				logger.log(
						Level.SEVERE,
						"The Post Stop event failed to execute after an anexpected failure of the process: "
								+ e.getMessage(), e);
			}

			// kill all current tasks, and create new thread pool for tasks
			this.executors.shutdownNow();

			this.state = USMState.LAUNCHING;
			this.executors = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);

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
						logger.info("Relaunching process");
						launch();
						logger.info("Finished Relaunching process after unexpected process death");
					} catch (final USMException e) {
						logger.log(Level.SEVERE,
								"Failed to re-launch the external process after a previous failure: "
										+ e.getMessage(), e);
						logger.severe("Marking this USM as failed so it will be recycled by GSM");
						markUSMAsFailed(e);
					} catch (final TimeoutException e) {
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

		// only used in IntegratedProcessingUnitContainer during testing.
		this.clusterInfo = clusterInfo;

	}

	@Override
	public ServiceDetails[] getServicesDetails() {
		logger.fine("Executing getServiceDetails()");
		return this.monitorsCache.getServicesDetails();
	}

	@Override
	public ServiceMonitors[] getServicesMonitors() {
		logger.fine("Executing getServiceMonitors()");

		return this.monitorsCache.getMonitors();
	}

	public List<Long> getServiceProcessesList() {
		return this.serviceProcessPIDs;
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

		final Map<String, Object> result = new HashMap<String, Object>();
		result.put(CloudifyConstants.INVOCATION_RESPONSE_INSTANCE_ID,
				this.instanceId);

		if (namedArgs == null) {
			logger.severe("recieved empty named arguments map");
			throw new IllegalArgumentException("Invoke recieved null as input");
		}

		final String commandName = (String) namedArgs
				.get(CloudifyConstants.INVOCATION_PARAMETER_COMMAND_NAME);
		
		invokeCustomCommand(commandName, namedArgs, result);
		return result;
	}
	
	private JavaExecutableEntry getBuiltInCommand(final String commandName) {
		for (USMBuiltInCommand command : getBuiltInCommands()) {
			if (command.getName().equalsIgnoreCase(commandName)) {
				JavaExecutableEntry entry = new JavaExecutableEntry();
				entry.setCommand(command);
				return entry;
			}
		}
		return null;
	}

	private void invokeCustomCommand(final String commandName,
			final Map<String, Object> namedArgs,
			final Map<String, Object> result) {
		if (commandName == null) {

			logger.severe("Command Name parameter in invoke is missing");
			throw new IllegalArgumentException(
					"Command Name parameter in invoke is missing");

		}

		result.put(CloudifyConstants.INVOCATION_RESPONSE_COMMAND_NAME,
				commandName);

		final Service service = getUsmLifecycleBean().getConfiguration()
				.getService();
		
		final ExecutableDSLEntry customCommand;
		if (commandName.startsWith(CloudifyConstants.BUILT_IN_COMMAND_PREFIX)) {
			customCommand = getBuiltInCommand(
					commandName.substring(CloudifyConstants.BUILT_IN_COMMAND_PREFIX.length()));
		} else {
			customCommand = service.getCustomCommands()
					.get(commandName);
			
		}

		if (customCommand == null) {
			logger.warning("Custom Command: " + commandName
					+ " does not exist in service: " + this.clusterName);

			result.put(CloudifyConstants.INVOCATION_RESPONSE_STATUS,
					false);
			result.put(CloudifyConstants.INVOCATION_RESPONSE_EXCEPTION,
					"No such Custom Command: " + commandName);
			result.put(CloudifyConstants.INVOCATION_RESPONSE_RESULT,
					"No such Custom Command: " + commandName);
			return;

		}

		try {

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Executing custom command: " + commandName
						+ ". Custom command is: " + customCommand);
			}
			final EventResult executionResult = new DSLEntryExecutor(
					customCommand, this.getUsmLifecycleBean().getLauncher(),
					this.getPuExtDir(), namedArgs, LifecycleEvents.CUSTOM_COMMAND).run();

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
	}

	private Exception shutdownUSMException;
	private String[] dependencies = new String[0];

	/******************
	 * Self healing allows a service instance to attempt a retry in case of a failure. If self healing is disabled, the
	 * service instance will not shut down if an error is encountered. The service lifecycle will stop at the current
	 * stage and will not proceed. Service monitors will not execute.
	 * 
	 * Defaults to true.
	 */
	private boolean selfHealing = true;

	private void markUSMAsFailed(final Exception ex) {
		try {
			deAllocateStorageSync();
		} catch (final Exception e) {
			logger.warning("Failed to de-allocate storage volume : " + e.getMessage());
		}
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
		}
		logger.severe("USM isAlive() exiting with exception due to previous failure. Exception message was: "
				+ shutdownUSMException.getMessage());
		throw shutdownUSMException;
	}

	@Override
	public void setBeanLevelProperties(
			final BeanLevelProperties beanLevelProperties) {

		final String value = beanLevelProperties.getContextProperties()
				.getProperty(CloudifyConstants.CONTEXT_PROPERTY_ASYNC_INSTALL,
						ASYNC_INSTALL_DEFAULT_VALUE);
		logger.info("Async Install Setting: " + value);
		this.asyncInstall = Boolean.parseBoolean(value);

		final String dependenciesString = beanLevelProperties
				.getContextProperties().getProperty(
						CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON, "[]");
		this.dependencies = parseDependenciesString(dependenciesString);
		logger.info("Dependencies for this service: "
				+ Arrays.toString(this.dependencies));

		final String disableSelfHealingProperty =
				beanLevelProperties.getContextProperties().getProperty(
						CloudifyConstants.CONTEXT_PROPERTY_DISABLE_SELF_HEALING);
		if (disableSelfHealingProperty != null) {
			logger.info("Disable self healing context property is set to: " + disableSelfHealingProperty);
			this.selfHealing = !Boolean.parseBoolean(disableSelfHealingProperty);
		} else {
			logger.info("Disable self healing context property is not set. Selt healing defaulting to true.");
			this.selfHealing = true;
		}

	}

	private String[] parseDependenciesString(final String dependenciesString) {
		// remove brackets
		final String internalString = dependenciesString.replace("[", "")
				.replace("]", "").trim();
		if (internalString.isEmpty()) {
			return new String[0];
		}

		final String[] splitResult = internalString.split(Pattern.quote(","));
		for (int i = 0; i < splitResult.length; i++) {
			splitResult[i] = splitResult[i].trim();
		}

		return splitResult;
	}
	

	public USMLifecycleBean getUsmLifecycleBean() {
		return usmLifecycleBean;
	}
	
	public USMBuiltInCommand[] getBuiltInCommands() {
		return builtInCommands;
	}

	public Process getStartProcess() {
		return this.process;
	}

	public boolean isRunningInGSC() {
		return this.runningInGSC;
	}

	public long getChildProcessID() {
		return childProcessID;
	}

	public USMProcessMode getProcessMode() {
		return processMode;
	}

	public int getInstanceId() {
		return this.instanceId;
	}

	public int getCurrentAttempt() {
		return currentAttempt;
	}
	
}
