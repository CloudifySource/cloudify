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
package org.cloudifysource.rest.controllers;

import static com.gigaspaces.log.LogEntryMatchers.regex;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_INVOKE_INSTANCE;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_APP;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_GSM;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_LUS;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_SERVICE;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_SERVICE_AFTER_DEPLOYMENT;
import static org.cloudifysource.rest.ResponseConstants.SERVICE_INSTANCE_UNAVAILABLE;
import static org.cloudifysource.rest.util.RestUtils.errorStatus;
import static org.cloudifysource.rest.util.RestUtils.successStatus;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletResponse;

import net.jini.core.discovery.LookupLocator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.DataGrid;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.Sla;
import org.cloudifysource.dsl.StatefulProcessingUnit;
import org.cloudifysource.dsl.StatelessProcessingUnit;
import org.cloudifysource.dsl.autoscaling.AutoScalingDetails;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.DSLApplicationCompilatioResult;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLServiceCompilationResult;
import org.cloudifysource.dsl.internal.EventLogConstants;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.CloudConfigurationHolder;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.esc.driver.provisioning.CloudifyMachineProvisioningConfig;
import org.cloudifysource.rest.ResponseConstants;
import org.cloudifysource.rest.util.ApplicationInstallerRunnable;
import org.cloudifysource.rest.util.LifecycleEventsContainer;
import org.cloudifysource.rest.util.LifecycleEventsContainer.PollingState;
import org.cloudifysource.rest.util.RestPollingRunnable;
import org.cloudifysource.rest.util.RestUtils;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.Applications;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.internal.pu.DefaultProcessingUnitInstance;
import org.openspaces.admin.internal.pu.InternalProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleRuleConfig;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleRuleConfigurer;
import org.openspaces.admin.pu.elastic.config.CapacityRequirementsConfig;
import org.openspaces.admin.pu.elastic.config.CapacityRequirementsConfigurer;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfig;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.topology.ElasticDeploymentTopology;
import org.openspaces.admin.pu.statistics.LastSampleTimeWindowStatisticsConfig;
import org.openspaces.admin.pu.statistics.ProcessingUnitStatisticsId;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.zone.Zone;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;
import org.openspaces.core.util.MemoryUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;

/**
 * @author rafi, barakm, adaml, noak
 * @since 2.0.0
 */
@Controller
@RequestMapping("/service")
public class ServiceController {

	private static final int POLLING_TASK_TIMEOUT = 20;
    private static final int TEN_MINUTES_MILLISECONDS = 60 * 1000 * 10;
	private static final int THREAD_POOL_SIZE = 2;
	private static final String SHARED_ISOLATION_ID = "public";
	private static final int PU_DISCOVERY_TIMEOUT_SEC = 8;
	private static final String USM_EVENT_LOGGER_NAME = ".*.USMEventLogger.{0}\\].*";
    private final Map<UUID, LifecycleEventsContainer> lifecyclePollingContainer = 
        new ConcurrentHashMap<UUID, LifecycleEventsContainer>();
    private final int LIFECYCLE_EVENT_POLLING_INTERVAL = 4000;
    
    
    /**
     * A set containing all of the executed lifecycle events. used to avoid duplicate prints.
     */
    private Set<String> eventsSet = new HashSet<String>();
    
	@Autowired(required = true)
	private Admin admin;
	@GigaSpaceContext(name = "gigaSpace")
	private GigaSpace gigaSpace;

	private Cloud cloud = null;

	private static final Logger logger = Logger.getLogger(ServiceController.class.getName());
	private String cloudFileContents;
	private String defaultTemplateName;

	@Value("${restful.temporaryFolder}")
	private String temporaryFolder;

	/**
	 * Initializing the cloud configuration. Executed by Spring after the object is instantiated and the
	 * dependencies injected.
	 */
	@PostConstruct
	public void init() {
		logger.info("Initializing service controller cloud configuration");
		this.cloud = readCloud();
		if (cloud != null) {
			if (this.cloud.getTemplates().isEmpty()) {
				throw new IllegalArgumentException("No templates defined in cloud configuration!");
			}
			this.defaultTemplateName = this.cloud.getTemplates().keySet().iterator().next();
			logger.info("Setting default template name to: " + defaultTemplateName
					+ ". This template will be used for services that do not specify an explicit template");
		} else {
			logger.info("Service Controller is running in local cloud mode");
		}

		/**
		 * Sets the folder used for temporary files. The value can be set in the configuration file
		 * ("config.properties"), otherwise the system's default setting will apply.
		 */
		try {
			if (StringUtils.isBlank(temporaryFolder)) {
				temporaryFolder = getTempFolderPath();
			}
		} catch (final IOException e) {
			logger.log(Level.SEVERE, "ServiceController failed to locate temp directory", e);
			throw new IllegalStateException("ServiceController failed to locate temp directory", e);
		}
	}
	
	/**
	 * terminate all running threads.
	 */
   @PreDestroy
    public void destroy() {
       this.executorService.shutdownNow();
       this.scheduledExecutor.shutdownNow();
    }

	private String getCloudConfigurationFromManagementSpace() {
		logger.info("Waiting for cloud configuration to become available in management space");
		final CloudConfigurationHolder config = gigaSpace.read(new CloudConfigurationHolder(), 1000 * 60);
		if (config == null) {

			logger.warning("Could not find the expected Cloud Configuration Holder in Management space!"
					+ " Defaulting to local cloud!");
			return null;
		}
		return config.getCloudConfiguration();
	}

	private Cloud readCloud() {
		logger.info("Loading cloud configuration");

		this.cloudFileContents = getCloudConfigurationFromManagementSpace();
		if (this.cloudFileContents == null) {
			// must be local cloud
			return null;

		}
		Cloud cloudConfiguration = null;
		try {
			cloudConfiguration = ServiceReader.readCloud(cloudFileContents);
		} catch (final DSLException e) {
			throw new IllegalArgumentException("Failed to read cloud configuration file: " + cloudFileContents
					+ ". Error was: " + e.getMessage(), e);
		}

		logger.info("Successfully loaded cloud configuration file from management space");

		logger.info("Setting cloud local directory to: " + cloudConfiguration.getProvider().getRemoteDirectory());
		cloudConfiguration.getProvider().setLocalDirectory(cloudConfiguration.getProvider().getRemoteDirectory());
		logger.info("Loaded cloud: " + cloudConfiguration);

		return cloudConfiguration;

	}
	
	private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(10, new ThreadFactory() {
	    
	    private int counter = 1;
	    
        @Override
        public Thread newThread(Runnable r) {
            final Thread thread = new Thread(r, "LifecycleEventsPollingExecutor-" + counter++);
            thread.setDaemon(true);
            return thread;
        }
    });
	
	// Set up a small thread pool with daemon threads.
	private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE,
			new ThreadFactory() {

				private int counter = 1;

				@Override
				public Thread newThread(final Runnable r) {
					final Thread thread = new Thread(r, "ServiceControllerExecutor-" + counter++);
					thread.setDaemon(true);
					return thread;
				}
			});

	/**
	 * Tests whether the restful service is able to locate the service grid using the admin API. The admin API
	 * searches for a LUS (Lookup Service) according to the lookup groups/locators defined.
	 * 
	 * @return - Map<String, Object> object containing the test results.
	 */
	@RequestMapping(value = "/testrest", method = RequestMethod.GET)
	public @ResponseBody
	Object test() {
		if (admin.getLookupServices().getSize() > 0) {
			return successStatus();
		}
		final String groups = Arrays.toString(admin.getGroups());
		final String locators = Arrays.toString(admin.getLocators());
		return errorStatus(FAILED_TO_LOCATE_LUS, groups, locators);
	}

	@RequestMapping(value = "/cloudcontroller/deploy", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> deploy(
			@RequestParam(value = "applicationName", defaultValue = "default") final String applicationName,
			@RequestParam(value = "file") final MultipartFile srcFile) throws IOException {
		logger.finer("Deploying a service");
		final File tmpfile = File.createTempFile("gs___", null);
		final File dest = new File(tmpfile.getParent(), srcFile.getOriginalFilename());
		tmpfile.delete();
		srcFile.transferTo(dest);
		
		final GridServiceManager gsm = getGsm();
		if (gsm == null) {
			return errorStatus(FAILED_TO_LOCATE_GSM);
		}
		ProcessingUnit pu = gsm.deploy(new ProcessingUnitDeployment(dest).setContextProperty(
				CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME, applicationName));
		dest.delete();
		
		if (pu == null) {
			return errorStatus(FAILED_TO_LOCATE_SERVICE_AFTER_DEPLOYMENT, applicationName);
		}
		return successStatus(pu.getName());
	}

	/**
	 * Creates and returns a map containing all of the deployed application names.
	 * 
	 * @return a list of all the deployed applications in the service grid.
	 */
	@RequestMapping(value = "/applications", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> getApplicationsList() {
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list applications");
		}
		final Applications apps = admin.getApplications();
		final List<String> appNames = new ArrayList<String>(apps.getSize());
		for (final Application app : apps) {
			appNames.add(app.getName());
		}
		return successStatus(appNames);
	}

	/**
	 * Creates and returns a map containing all of the deployed service names installed under a specific
	 * application context.
	 * 
	 * @return a list of the deployed services in the service grid that were deployed as a part of a specific
	 *         application.
	 */
	@RequestMapping(value = "/applications/{applicationName}/services", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> getServicesList(@PathVariable final String applicationName) {
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list applications");
		}
		final Application app = admin.getApplications().waitFor(applicationName, 5, TimeUnit.SECONDS);
		if (app == null) {
			return errorStatus(FAILED_TO_LOCATE_APP, applicationName);
		}
		final ProcessingUnits pus = app.getProcessingUnits();
		final List<String> serviceNames = new ArrayList<String>(pus.getSize());
		for (final ProcessingUnit pu : pus) {
			serviceNames.add(ServiceUtils.getApplicationServiceName(pu.getName(), applicationName));
		}
		return successStatus(serviceNames);
	}

	/**
	 * 
	 * Extracts all of the GSC log entries that were logged by the USM and contain an installation event regex
	 * ".*.USMEventLogger.{0}\\].*". In addition, the entries returned will be only those that were logged in
	 * the last 10 minutes.
	 * 
	 * @param applicationName
	 *            The application name
	 * @param serviceName
	 *            The service name
	 * @return a list of log entry maps containing information regarding the specific entry.
	 */
	@RequestMapping(value = "/applications/{applicationName}/services/{serviceName}/USMEventsLogs", 
			method = RequestMethod.GET)
	public @ResponseBody
	Map<?, ?> getServiceLifecycleLogs(@PathVariable final String applicationName,
			@PathVariable final String serviceName) {
		// TODO:not run over
		final String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		final List<Map<String, String>> serviceEventDetailes = new ArrayList<Map<String, String>>();
		final String regex = MessageFormat.format(USM_EVENT_LOGGER_NAME, absolutePuName);
		final LogEntryMatcher matcher = regex(regex);
		final Zone zone = admin.getZones().getByName(absolutePuName);
		if (zone == null) {
			logger.info("Zone " + absolutePuName + " does not exist");
			return successStatus();
		}
		for (final GridServiceContainer container : zone.getGridServiceContainers()) {
			final LogEntries logEntries = container.logEntries(matcher);
			for (final LogEntry logEntry : logEntries) {
				if (logEntry.isLog()) {
					final Date tenMinutesAgoGscTime = new Date(new Date().getTime()
							+ container.getOperatingSystem().getTimeDelta() - TEN_MINUTES_MILLISECONDS);
					if (tenMinutesAgoGscTime.before(new Date(logEntry.getTimestamp()))) {
						final Map<String, String> serviceEventsMap = getServiceDetailes(logEntry, container,
								absolutePuName, applicationName);
						serviceEventDetailes.add(serviceEventsMap);
					}
				}
			}
		}
		return successStatus(serviceEventDetailes);
	}

	private Map<String, String> getServiceDetailes(final LogEntry logEntry, final GridServiceContainer container,
			final String absolutePuName, final String applicationName) {

		final Map<String, String> returnMap = new HashMap<String, String>();

		returnMap.put(EventLogConstants.getTimeStampKey(), Long.toString(logEntry.getTimestamp()));
		returnMap.put(EventLogConstants.getMachineHostNameKey(), container.getMachine().getHostName());
		returnMap.put(EventLogConstants.getMachineHostAddressKey(), container.getMachine().getHostAddress());
		returnMap.put(EventLogConstants.getServiceNameKey(),
				ServiceUtils.getApplicationServiceName(absolutePuName, applicationName));
		// The string replacement is done since the service name that is
		// received from the USM logs derived from actual PU name.
		returnMap.put(
				EventLogConstants.getEventTextKey(),
				logEntry.getText().replaceFirst(absolutePuName + "-",
						returnMap.get(EventLogConstants.getServiceNameKey()) + "-"));

		return returnMap;
	}

	/**
	 * 
	 * Creates a list of all service instances in the specified application.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @return a Map containing all service instances of the specified application
	 */
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/instances", 
			method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> getServiceInstanceList(@PathVariable final String applicationName,
			@PathVariable final String serviceName) {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list instances for service " + absolutePuName + " of application "
					+ applicationName);
		}
		// todo: application awareness
		final ProcessingUnit pu = admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
				TimeUnit.SECONDS);
		if (pu == null) {
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}
		final Map<Integer, String> instanceMap = new HashMap<Integer, String>();
		final ProcessingUnitInstance[] instances = pu.getInstances();
		for (final ProcessingUnitInstance instance : instances) {
			instanceMap.put(instance.getInstanceId(), instance.getVirtualMachine().getMachine().getHostName());
		}
		return successStatus(instanceMap);
	}

	/**
	 * 
	 * Invokes a custom command on all of the specified service instances. Custom parameters are passed as a
	 * map using the POST method and contain the command name and parameter values for the specified command.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @param beanName
	 *            deprecated.
	 * @param params
	 *            The command parameters.
	 * @return a Map containing the result of each invocation on a service instance.
	 */
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/beans/{beanName}/invoke", 
			method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> invoke(@PathVariable final String applicationName, @PathVariable final String serviceName,
			@PathVariable final String beanName, @RequestBody final Map<String, Object> params) {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to invoke bean " + beanName + " of service " + absolutePuName
					+ " of application " + applicationName);
		}

		// Get the PU
		final ProcessingUnit pu = admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
				TimeUnit.SECONDS);
		if (pu == null) {
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}

		// result, mapping service instances to results
		final Map<String, Object> invocationResult = new HashMap<String, Object>();
		final ProcessingUnitInstance[] instances = pu.getInstances();

		// Why a map? TODO: Use an array here instead.
		// map between service name and its future
		final Map<String, Future<Object>> futures = new HashMap<String, Future<Object>>(instances.length);
		for (final ProcessingUnitInstance instance : instances) {
			// key includes instance ID and host name
			final String serviceInstanceName = buildServiceInstanceName(instance);
			try {
				final Future<Object> future = ((DefaultProcessingUnitInstance) instance).invoke(beanName, params);
				futures.put(serviceInstanceName, future);
			} catch (final Exception e) {
				logger.severe("Error invoking service " + serviceName + ":" + instance.getInstanceId() + " on host "
						+ instance.getVirtualMachine().getMachine().getHostName());
				invocationResult.put(serviceInstanceName, "pu_instance_invocation_failure");
			}
		}

		for (final Map.Entry<String, Future<Object>> entry : futures.entrySet()) {
			try {
				Object result = entry.getValue().get();
				// use only tostring of collection values, to avoid
				// serialization problems
				result = postProcessInvocationResult(result, entry.getKey());

				invocationResult.put(entry.getKey(), result);
			} catch (final Exception e) {
				invocationResult.put(entry.getKey(), "Invocation failure: " + e.getMessage());
			}
		}
		return successStatus(invocationResult);
	}

	private Object postProcessInvocationResult(final Object result, final String instanceName) {
		Object formattedResult;
		if (result instanceof Map<?, ?>) {
			final Map<String, String> modifiedMap = new HashMap<String, String>();
			@SuppressWarnings("unchecked")
			final Set<Entry<String, Object>> entries = ((Map<String, Object>) result).entrySet();
			for (final Entry<String, Object> subEntry : entries) {

				modifiedMap
						.put(subEntry.getKey(), subEntry.getValue() == null ? null : subEntry.getValue().toString());
			}
			modifiedMap.put(CloudifyConstants.INVOCATION_RESPONSE_INSTANCE_NAME, instanceName);
			formattedResult = modifiedMap;
		} else {
			formattedResult = result.toString();
		}
		return formattedResult;
	}

	/**
	 * 
	 * Invokes a custom command on a specific service instance. Custom parameters are passed as a map using
	 * POST method and contain the command name and parameter values for the specified command.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name
	 * @param instanceId
	 *            The service instance number to be invoked.
	 * @param beanName
	 *            depreciated
	 * @param params
	 *            a Map containing the result of each invocation on a service instance.
	 * @return a Map containing the invocation result on the specified instance.
	 */
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/instances/{instanceId}/beans/{beanName}/invoke", 
			method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> invokeInstance(@PathVariable final String applicationName,
			@PathVariable final String serviceName, @PathVariable final int instanceId,
			@PathVariable final String beanName, @RequestBody final Map<String, Object> params) {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to invoke bean " + beanName + " of service " + serviceName
					+ " of application " + applicationName);
		}

		// Get PU
		final ProcessingUnit pu = admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
				TimeUnit.SECONDS);
		if (pu == null) {
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}

		// Get PUI
		final InternalProcessingUnitInstance pui = findInstanceById(pu, instanceId);

		if (pui == null) {
			logger.severe("Could not find service instance " + instanceId + " for service " + absolutePuName);
			return errorStatus(ResponseConstants.SERVICE_INSTANCE_UNAVAILABLE, applicationName, absolutePuName,
					Integer.toString(instanceId));
		}
		final String instanceName = buildServiceInstanceName(pui);
		// Invoke the remote service
		try {
			final Future<?> future = pui.invoke(beanName, params);
			final Object invocationResult = future.get();
			final Object finalResult = postProcessInvocationResult(invocationResult, instanceName);
			return successStatus(finalResult);
		} catch (final Exception e) {
			logger.severe("Error invoking pu instance " + absolutePuName + ":" + instanceId + " on host "
					+ pui.getVirtualMachine().getMachine().getHostName());
			return errorStatus(FAILED_TO_INVOKE_INSTANCE, absolutePuName,
					Integer.toString(instanceId),  e.getMessage());
		}
	}

	private InternalProcessingUnitInstance findInstanceById(final ProcessingUnit pu, final int id) {
		final ProcessingUnitInstance[] instances = pu.getInstances();
		for (final ProcessingUnitInstance instance : instances) {
			if (instance.getInstanceId() == id) {
				return (InternalProcessingUnitInstance) instance;
			}
		}
		return null;
	}

	private String buildServiceInstanceName(final ProcessingUnitInstance instance) {
		return "instance #" + instance.getInstanceId() + "@" + instance.getVirtualMachine().getMachine().getHostName();
	}

	private Map<String, Object> unavailableServiceError(final String serviceName) {
		// TODO: Consider telling the user he might be using the wrong
		// application name.
		return errorStatus(FAILED_TO_LOCATE_SERVICE, ServiceUtils.getFullServiceName(serviceName).getServiceName());
	}

	private GridServiceManager getGsm(final String id) {
		if (id == null) {
			return admin.getGridServiceManagers().waitForAtLeastOne(5000, TimeUnit.MILLISECONDS);
		}
		return admin.getGridServiceManagers().getManagerByUID(id);
	}

	private GridServiceManager getGsm() {
		return getGsm(null);
	}

	/**
	 * undeploys the specified service of the specific application.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @return success status if service was undeployed successfully, else returns failure status.
	 */
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/timeout/{timeoutInMinutes}/undeploy", 
			method = RequestMethod.DELETE)
	public @ResponseBody
	Map<String, Object> undeploy(@PathVariable final String applicationName, @PathVariable final String serviceName, @PathVariable final int timeoutInMinutes) {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		final ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePuName,
				PU_DISCOVERY_TIMEOUT_SEC, TimeUnit.SECONDS);
		if (processingUnit == null) {
			return unavailableServiceError(absolutePuName);
		}
		UUID lifecycleEventContainerID = startPollingForServiceUninstallLifecycleEvents(applicationName, serviceName, timeoutInMinutes);
		processingUnit.undeployAndWait();
        Map<String, Object> returnMap = new HashMap<String, Object>();
        returnMap.put(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID, lifecycleEventContainerID);
		return successStatus(returnMap);
	}

	private UUID startPollingForServiceUninstallLifecycleEvents(
            String applicationName, String serviceName, int timeoutInMinutes) {
	       RestPollingRunnable restPollingRunnable;
	        LifecycleEventsContainer lifecycleEventsContainer = new LifecycleEventsContainer();
	        UUID lifecycleEventsContainerID = UUID.randomUUID();
	        this.lifecyclePollingContainer.put(lifecycleEventsContainerID, lifecycleEventsContainer);
	        lifecycleEventsContainer.setEventsSet(this.eventsSet);
	        
	        restPollingRunnable = new RestPollingRunnable(applicationName, timeoutInMinutes, TimeUnit.MINUTES);
	        restPollingRunnable.addService(serviceName, 0);
	        restPollingRunnable.setIsServiceInstall(false);
	        restPollingRunnable.setAdmin(admin);
	        restPollingRunnable.setLifecycleEventsContainer(lifecycleEventsContainer);
	        restPollingRunnable.setIsUninstall(true);
	        
	        ScheduledFuture<?> scheduleWithFixedDelay = scheduledExecutor
	                .scheduleWithFixedDelay(restPollingRunnable, 0, LIFECYCLE_EVENT_POLLING_INTERVAL, TimeUnit.SECONDS);
	        lifecycleEventsContainer.setFutureTask(scheduleWithFixedDelay);

	        logger.log(Level.INFO, "polling container UUID is " + lifecycleEventsContainerID.toString());
	        return lifecycleEventsContainerID;
    }

    /**
	 * 
	 * Increments the Processing unit instance number of the specified service.
	 * 
	 * @param applicationName
	 *            The application name where the service resides.
	 * @param serviceName
	 *            The service name.
	 * @param params
	 *            map that holds a timeout value for this action.
	 * @return success status map if succeeded, else returns an error status.
	 */
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/addinstance", 
			method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> addInstance(@PathVariable final String applicationName,
			@PathVariable final String serviceName, @RequestBody final Map<String, String> params) {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		final int timeout = Integer.parseInt(params.get("timeout"));
		final ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePuName,
				PU_DISCOVERY_TIMEOUT_SEC, TimeUnit.SECONDS);
		if (processingUnit == null) {
			return unavailableServiceError(absolutePuName);
		}
		final int before = processingUnit.getNumberOfInstances();
		processingUnit.incrementInstance();
		final boolean result = processingUnit.waitFor(before + 1, timeout, TimeUnit.SECONDS);
		if (result) {
			return successStatus();
		}
		return errorStatus(ResponseConstants.FAILED_TO_ADD_INSTANCE, applicationName, serviceName);
	}

	/**
	 * 
	 * Decrements the Processing unit instance number of the specified service.
	 * 
	 * @param applicationName
	 *            The application name where the service resides.
	 * @param serviceName
	 *            The service name.
	 * @param instanceId
	 *            the service instance ID to remove.
	 * @return success status map if succeeded, else returns an error status.
	 */
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/instances/{instanceId}/remove", 
			method = RequestMethod.DELETE)
	public @ResponseBody
	Map<String, Object> removeInstance(@PathVariable final String applicationName,
			@PathVariable final String serviceName, @PathVariable final int instanceId) {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		// todo: application awareness
		final ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePuName,
				PU_DISCOVERY_TIMEOUT_SEC, TimeUnit.SECONDS);
		if (processingUnit == null) {
			return unavailableServiceError(absolutePuName);
		}
		for (final ProcessingUnitInstance instance : processingUnit.getInstances()) {
			if (instance.getInstanceId() == instanceId) {
				instance.decrement();
				return successStatus();
			}
		}
		return errorStatus(SERVICE_INSTANCE_UNAVAILABLE);
	}

	private void deployAndWait(final String serviceName, final ElasticSpaceDeployment deployment)
			throws TimeoutException {
		final ProcessingUnit pu = getGridServiceManager().deploy(deployment, 60, TimeUnit.SECONDS);
		if (pu == null) {
			throw new TimeoutException("Timed out waiting for Service " + serviceName + " deployment.");
		}
	}

	private ElasticServiceManager getESM() {
		return admin.getElasticServiceManagers().waitForAtLeastOne(5000, TimeUnit.MILLISECONDS);
	}

	private void deployAndWait(final String serviceName, final ElasticStatelessProcessingUnitDeployment deployment)
			throws TimeoutException {
		final ProcessingUnit pu = getGridServiceManager().deploy(deployment, 60, TimeUnit.SECONDS);
		if (pu == null) {
			throw new TimeoutException("Timed out waiting for Service " + serviceName + " deployment.");
		}
	}

	private GridServiceManager getGridServiceManager() {
		if (admin.getGridServiceManagers().isEmpty()) {
			throw new AdminException("Cannot locate Grid Service Manager");
		}
		return admin.getGridServiceManagers().iterator().next();
	}

	/**
	 * Exception handler for all of the internal server's exceptions.
	 * 
	 * @param response The response object to edit, if not committed yet.
	 * @param e The exception that occurred, from which data is read for logging and for the response error message.
	 * @throws IOException Reporting failure to edit the response object
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public void resolveDocumentNotFoundException(final HttpServletResponse response, final Exception e)
			throws IOException {

		if (response.isCommitted()) {
			logger.log(Level.WARNING,
					"Caught exception, but response already commited. Not sending error message based on exception", e);
		} else {
			final Writer writer = response.getWriter();
			final String message = "{\"status\":\"error\", \"error\":\"" + e.getMessage() + "\"}";
			logger.log(Level.SEVERE, "caught exception. Sending response message " + message, e);
			writer.write(message);
		}
	}

	/******************
	 * Uninstalls an application by uninstalling all of its services. Order of uninstallations is determined
	 * by the context property 'com.gs.application.services' which should exist in all service PUs.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @return Map with return value; @ .
	 */
	@RequestMapping(value = "applications/{applicationName}/timeout/{timeoutInMinutes}", method = RequestMethod.DELETE)
	public @ResponseBody
	Map<String, Object> uninstallApplication(@PathVariable final String applicationName, @PathVariable final int timeoutInMinutes) {

		// Check that Application exists
		final Application app = this.admin.getApplications().waitFor(applicationName, 10, TimeUnit.SECONDS);
		if (app == null) {
			logger.log(Level.INFO, "Cannot uninstall application " + applicationName
					+ " since it has not been discovered yet.");
			return RestUtils.errorStatus(ResponseConstants.FAILED_TO_LOCATE_APP, applicationName);
		}
		final ProcessingUnit[] pus = app.getProcessingUnits().getProcessingUnits();

		final StringBuilder sb = new StringBuilder();
		final List<ProcessingUnit> uninstallOrder = createUninstallOrder(pus, applicationName);
		//TODO: Add timeout. 
		UUID lifecycleEventContainerID = startPollingForApplicationUninstallLifecycleEvents(applicationName, uninstallOrder, timeoutInMinutes);
		if (uninstallOrder.size() > 0) {

			((InternalAdmin) admin).scheduleAdminOperation(new Runnable() {

				@Override
				public void run() {
					for (final ProcessingUnit processingUnit : uninstallOrder) {
						try {
							if (processingUnit.waitForManaged(10, TimeUnit.SECONDS) == null) {
								logger.log(Level.WARNING, "Failed to locate GSM that is managing Processing Unit "
										+ processingUnit.getName());
							} else {
								logger.log(Level.INFO, "Undeploying Processing Unit " + processingUnit.getName());
								processingUnit.undeploy();
							}
						} catch (final Exception e) {
							final String msg = "Failed to undeploy processing unit: " + processingUnit.getName()
									+ " while uninstalling application " + applicationName
									+ ". Uninstall will continue, but service " + processingUnit.getName()
									+ " may remain in an unstable state";

							logger.log(Level.SEVERE, msg, e);
						}
					}
					logger.log(Level.INFO, "Application " + applicationName + " undeployment complete");
				}
			});

		}

		final String errors = sb.toString();
		if (errors.length() == 0) {
		    Map<String, Object> returnMap = new HashMap<String, Object>();
		    returnMap.put(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID, lifecycleEventContainerID);
			return RestUtils.successStatus(returnMap);
		}
		return RestUtils.errorStatus(errors);
	}

    private List<ProcessingUnit> createUninstallOrder(final ProcessingUnit[] pus, final String applicationName) {

		// TODO: Refactor this - merge with createServiceOrder, as methods are
		// very similar
		final DirectedGraph<ProcessingUnit, DefaultEdge> graph = new DefaultDirectedGraph<ProcessingUnit, DefaultEdge>(
				DefaultEdge.class);

		for (final ProcessingUnit processingUnit : pus) {
			graph.addVertex(processingUnit);
		}

		final Map<String, ProcessingUnit> puByName = new HashMap<String, ProcessingUnit>();
		for (final ProcessingUnit processingUnit : pus) {
			puByName.put(processingUnit.getName(), processingUnit);
		}

		for (final ProcessingUnit processingUnit : pus) {
			final String dependsOn = (String) processingUnit.getBeanLevelProperties().getContextProperties()
					.get(CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON);
			if (dependsOn == null) {
				logger.warning("Could not find the " + CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON
						+ " property for processing unit " + processingUnit.getName());

			} else {
				final String[] dependencies = dependsOn.replace("[", "").replace("]", "").split(",");
				for (final String puName : dependencies) {
					final String normalizedPuName = puName.trim();
					if (normalizedPuName.length() > 0) {
						final ProcessingUnit dependency = puByName.get(normalizedPuName);
						if (dependency == null) {
							logger.severe("Could not find Processing Unit " + normalizedPuName
									+ " that Processing Unit " + processingUnit.getName() + " depends on");
						} else {
							// the reverse to the install order.
							graph.addEdge(processingUnit, dependency);
						}
					}
				}
			}
		}

		final CycleDetector<ProcessingUnit, DefaultEdge> cycleDetector = new CycleDetector<ProcessingUnit, DefaultEdge>(
				graph);
		final boolean containsCycle = cycleDetector.detectCycles();

		if (containsCycle) {
			logger.warning("Detected a cycle in the dependencies of application: " + applicationName
					+ " while preparing to uninstall."
					+ " The service in this application will be uninstalled in a random order");

			return Arrays.asList(pus);
		}

		final TopologicalOrderIterator<ProcessingUnit, DefaultEdge> iterator = 
				new TopologicalOrderIterator<ProcessingUnit, DefaultEdge>(
				graph);

		final List<ProcessingUnit> orderedList = new ArrayList<ProcessingUnit>();
		while (iterator.hasNext()) {
			final ProcessingUnit nextPU = iterator.next();
			if (!orderedList.contains(nextPU)) {
				orderedList.add(nextPU);
			}
		}
		// Collections.reverse(orderedList);
		return orderedList;

	}

	/**
	 * Deploys an application to the service grid. An application is consisted of a group of services that
	 * might have dependencies between themselves. The application will be deployed according to the
	 * dependency order defined in the application file and deployed asynchronously if possible.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param srcFile
	 *            The compressed application file.
	 * @return Map with return value.
	 * @throws IOException Reporting failure to create a file while opening the packaged application file
	 * @throws DSLException Reporting failure to parse the application file
	 */
    @RequestMapping(value = "applications/{applicationName}/timeout/{timeout}", method = RequestMethod.POST)
    public @ResponseBody
    Object deployApplication(@PathVariable final String applicationName, @PathVariable final int timeout,
            @RequestParam(value = "file", required = true) final MultipartFile srcFile) throws IOException, 
            DSLException {
        final File applicationFile = copyMultipartFileToLocalFile(srcFile);
        final Object returnObject = doDeployApplication(applicationName, applicationFile, timeout);
        applicationFile.delete();
        return returnObject;
    }

	private List<Service> createServiceDependencyOrder(final org.cloudifysource.dsl.Application application) {
		final DirectedGraph<Service, DefaultEdge> graph = new DefaultDirectedGraph<Service, DefaultEdge>(
				DefaultEdge.class);

		final Map<String, Service> servicesByName = new HashMap<String, Service>();

		final List<Service> services = application.getServices();

		for (final Service service : services) {
			// keep a map of names to services
			servicesByName.put(service.getName(), service);
			// and create the graph node
			graph.addVertex(service);
		}

		for (final Service service : services) {
			final List<String> dependsList = service.getDependsOn();
			if (dependsList != null) {
				for (final String depends : dependsList) {
					final Service dependency = servicesByName.get(depends);
					if (dependency == null) {
						throw new IllegalArgumentException("Dependency '" + depends + "' of service: "
								+ service.getName() + " was not found");
					}

					graph.addEdge(dependency, service);
				}
			}
		}

		final CycleDetector<Service, DefaultEdge> cycleDetector = new CycleDetector<Service, DefaultEdge>(graph);
		final boolean containsCycle = cycleDetector.detectCycles();

		if (containsCycle) {
			final Set<Service> servicesInCycle = cycleDetector.findCycles();
			final StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (final Service service : servicesInCycle) {
				if (!first) {
					sb.append(",");
				} else {
					first = false;
				}
				sb.append(service.getName());
			}

			final String cycleString = sb.toString();

			// NOTE: This is not exactly how the cycle detector works. The
			// returned list is the vertex set for the subgraph of all cycles.
			// So if there are multiple cycles, the list will contain the
			// members of all of them.
			throw new IllegalArgumentException("The dependency graph of application: " + application.getName()
					+ " contains one or more cycles. The services that form a cycle are part of the following group: "
					+ cycleString);
		}

		final TopologicalOrderIterator<Service, DefaultEdge> iterator = 
				new TopologicalOrderIterator<Service, DefaultEdge>(
				graph);

		final List<Service> orderedList = new ArrayList<Service>();
		while (iterator.hasNext()) {
			orderedList.add(iterator.next());
		}
		return orderedList;

	}
	
    private UUID startPollingForApplicationUninstallLifecycleEvents(
            String applicationName, List<ProcessingUnit> uninstallOrder, int timeoutInMinutes) {
        
        RestPollingRunnable restPollingRunnable;
        LifecycleEventsContainer lifecycleEventsContainer = new LifecycleEventsContainer();
        UUID lifecycleEventsContainerID = UUID.randomUUID();
        this.lifecyclePollingContainer.put(lifecycleEventsContainerID, lifecycleEventsContainer);
        lifecycleEventsContainer.setEventsSet(this.eventsSet);
        
        restPollingRunnable = new RestPollingRunnable(applicationName, timeoutInMinutes, TimeUnit.MINUTES);
        for (ProcessingUnit processingUnit : uninstallOrder) {
            String processingUnitName = processingUnit.getName();
            String serviceName = ServiceUtils.getApplicationServiceName(processingUnitName, applicationName);
            restPollingRunnable.addService(serviceName, 0);
        }
        restPollingRunnable.setIsServiceInstall(false);
        restPollingRunnable.setAdmin(admin);
        restPollingRunnable.setLifecycleEventsContainer(lifecycleEventsContainer);
        restPollingRunnable.setIsUninstall(true);
        
        ScheduledFuture<?> scheduleWithFixedDelay = scheduledExecutor
                .scheduleWithFixedDelay(restPollingRunnable, 0, LIFECYCLE_EVENT_POLLING_INTERVAL, TimeUnit.SECONDS);
        lifecycleEventsContainer.setFutureTask(scheduleWithFixedDelay);

        logger.log(Level.INFO, "polling container UUID is " + lifecycleEventsContainerID.toString());
        return lifecycleEventsContainerID;
        
    }
    
    //TODO: Start executer service
    private UUID startPollingForLifecycleEvents(final String serviceName,
            final String applicationName, final int plannedNumberOfInstances, boolean isServiceInstall, final int timeout,
            final TimeUnit minutes) {
        RestPollingRunnable restPollingRunnable;
        logger.info("starting POLL on service : " + serviceName + " app: " + applicationName);
        
        LifecycleEventsContainer lifecycleEventsContainer = new LifecycleEventsContainer();
        UUID lifecycleEventsContainerID = UUID.randomUUID();
        this.lifecyclePollingContainer.put(lifecycleEventsContainerID, lifecycleEventsContainer);
        lifecycleEventsContainer.setEventsSet(this.eventsSet);
        
        restPollingRunnable = new RestPollingRunnable(applicationName, timeout, minutes);
        restPollingRunnable.addService(serviceName, plannedNumberOfInstances);
        restPollingRunnable.setAdmin(admin);
        restPollingRunnable.setIsServiceInstall(isServiceInstall);
        restPollingRunnable.setLifecycleEventsContainer(lifecycleEventsContainer);

        
        ScheduledFuture<?> scheduleWithFixedDelay = scheduledExecutor
                .scheduleWithFixedDelay(restPollingRunnable, 0, LIFECYCLE_EVENT_POLLING_INTERVAL, TimeUnit.SECONDS);
        lifecycleEventsContainer.setFutureTask(scheduleWithFixedDelay);

        logger.log(Level.INFO, "polling container UUID is " + lifecycleEventsContainerID.toString());
        return lifecycleEventsContainerID;
    }

    //TODO: Start executer service
    private UUID startPollingForLifecycleEvents(final org.cloudifysource.dsl.Application application,
            final int timeout,
            final TimeUnit timeUnit) {
        
        LifecycleEventsContainer lifecycleEventsContainer = new LifecycleEventsContainer();
        UUID lifecycleEventsContainerUUID = UUID.randomUUID();
        lifecycleEventsContainer.setUUID(lifecycleEventsContainerUUID);
        this.lifecyclePollingContainer.put(lifecycleEventsContainerUUID, lifecycleEventsContainer);
        lifecycleEventsContainer.setEventsSet(this.eventsSet);
        
        RestPollingRunnable restPollingRunnable = new RestPollingRunnable(application.getName(), timeout, timeUnit);
        for (Service service : application.getServices()) {
            restPollingRunnable.addService(service.getName(), service.getNumInstances());
        }
        restPollingRunnable.setIsServiceInstall(false);
        restPollingRunnable.setLifecycleEventsContainer(lifecycleEventsContainer);
        restPollingRunnable.setAdmin(admin);

        ScheduledFuture<?> scheduleWithFixedDelay = scheduledExecutor
                .scheduleWithFixedDelay(restPollingRunnable, 0, LIFECYCLE_EVENT_POLLING_INTERVAL, TimeUnit.SECONDS);
        lifecycleEventsContainer.setFutureTask(scheduleWithFixedDelay);

        logger.log(Level.INFO, "polling container UUID is " + lifecycleEventsContainer.toString());
        return lifecycleEventsContainerUUID;
    }
    
    //TODO remove cursor
    @RequestMapping(value = "/lifecycleEventContainerID/{lifecycleEventContainerID}/cursor/{cursor}" 
        , method = RequestMethod.GET)
        public @ResponseBody
        Object getLifecycleEvents(@PathVariable final String lifecycleEventContainerID,
                @PathVariable final int cursor) {
        Map<String, Object> resultsMap = new HashMap<String, Object>();
        resultsMap.put(CloudifyConstants.POLLING_TIMEOUT_EXCEPTION, false);
        resultsMap.put(CloudifyConstants.POLLING_EXCEPTION, false);
        if (!lifecyclePollingContainer.containsKey(UUID.fromString(lifecycleEventContainerID))){
            return errorStatus("Lifecycle events container with UUID: " + lifecycleEventContainerID +
                    " does not exist or expired.");
        }
        LifecycleEventsContainer container = lifecyclePollingContainer.get(UUID.fromString(lifecycleEventContainerID));
        Future<?> futureTask = container.getFutureTask();
        PollingState runnableState = container.getPollingState();
        switch (runnableState) {
        case RUNNING:
            resultsMap.put(CloudifyConstants.IS_TASK_DONE, false);
            break;
        case ENDED:
            Throwable t = container.getExecutionException();
            if (t != null){
                if (t.getCause() instanceof TimeoutException){
                    logger.log(Level.INFO, "Lifecycle events polling task timed out.");
                    resultsMap.put(CloudifyConstants.POLLING_TIMEOUT_EXCEPTION, true);
                    resultsMap.put(CloudifyConstants.IS_TASK_DONE, true);
                } else {
                    logger.log(Level.INFO, "Lifecycle events polling ended unexpectedly.", t);
                    resultsMap.put(CloudifyConstants.POLLING_EXCEPTION, true);
                    resultsMap.put(CloudifyConstants.IS_TASK_DONE, true);
                }
            } else {
                resultsMap.put(CloudifyConstants.IS_TASK_DONE, true);
            }
            futureTask.cancel(true);
            break;
        } 

        List<String> lifecycleEvents = container.getLifecycleEvents(cursor);

        if (lifecycleEvents != null){
            int newCursorPos = cursor + lifecycleEvents.size();
            resultsMap.put(CloudifyConstants.CURSOR_POS, newCursorPos);
            resultsMap.put(CloudifyConstants.LIFECYCLE_LOGS, lifecycleEvents);
        }else{
            resultsMap.put(CloudifyConstants.CURSOR_POS, cursor);
        }
        
        return successStatus(resultsMap);
    }
    
    private Object doDeployApplication(final String applicationName, final File applicationFile, int timeout) throws IOException,
    DSLException {
        final DSLApplicationCompilatioResult result = ServiceReader.getApplicationFromFile(applicationFile);
        final List<Service> services = createServiceDependencyOrder(result.getApplication());

        UUID lifecycleEventContainerID = startPollingForLifecycleEvents(result.getApplication(), timeout, TimeUnit.MINUTES);

        final ApplicationInstallerRunnable installer = new ApplicationInstallerRunnable(this, result, applicationName,
                services, this.cloud);
        this.executorService.execute(installer);

        final String[] serviceOrder = new String[services.size()];
        for (int i = 0; i < serviceOrder.length; i++) {
            serviceOrder[i] = services.get(i).getName();
        }
        Map<String, Object> returnMap = new HashMap<String, Object>();
        returnMap.put(CloudifyConstants.SERVICE_ORDER, Arrays.toString(serviceOrder));
        returnMap.put(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID, lifecycleEventContainerID);
        final Map<String, Object> retval = successStatus(returnMap);

        return retval;

    }
	private File copyMultipartFileToLocalFile(final MultipartFile srcFile) throws IOException {
		final File tempFile = new File(temporaryFolder, srcFile.getOriginalFilename());
		srcFile.transferTo(tempFile);
		tempFile.deleteOnExit();
		return tempFile;
	}

	/**
	 * Creates a randomly-named file in the system's default temp folder, just to get the path. The file is
	 * deleted immediately.
	 * 
	 * @return The path to the system's default temp folder
	 */
	private String getTempFolderPath() throws IOException {
		/*long tmpNum = new SecureRandom().nextLong();
		if (tmpNum == Long.MIN_VALUE) {
			tmpNum = 0; // corner case
		} else {
			tmpNum = Math.abs(tmpNum);
		}*/
		final File tempFile = File.createTempFile("GS__", null);
		tempFile.delete();
		tempFile.mkdirs();
		return tempFile.getParent();
	}

	
	private void doDeploy(final String applicationName, final String serviceName, final String templateName,
			final String zone, final File serviceFile, final Properties contextProperties, Service service)
			throws TimeoutException, DSLException {

		final int externalProcessMemoryInMB = 512;
		final int containerMemoryInMB = 128;
		final int reservedMemoryCapacityPerMachineInMB = 256;

		final ElasticStatelessProcessingUnitDeployment deployment = new ElasticStatelessProcessingUnitDeployment(
				serviceFile).memoryCapacityPerContainer(externalProcessMemoryInMB, MemoryUnit.MEGABYTES)
				.addCommandLineArgument("-Xmx" + containerMemoryInMB + "m")
				.addCommandLineArgument("-Xms" + containerMemoryInMB + "m")
				.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME, applicationName)
				.name(serviceName)
				// All PUs on this role share the same machine. Machines
				// are identified by zone.
				.sharedMachineProvisioning(
						SHARED_ISOLATION_ID,
						new DiscoveredMachineProvisioningConfigurer()
								.addGridServiceAgentZone(zone)
								.reservedMemoryCapacityPerMachine(reservedMemoryCapacityPerMachineInMB,
										MemoryUnit.MEGABYTES).create());
		
		if (cloud == null) {
			if (!isLocalCloud()) {
				
				// Azure: Eager scale (1 container per machine per PU)
				EagerScaleConfig scaleConfig = 
					new EagerScaleConfigurer()
					.atMostOneContainerPerMachine()
					.create();
				
				deployment.scale(scaleConfig);
			}
			else {
				//local cloud
				if (service == null || service.getAutoScaling() == null) {

					final ManualCapacityScaleConfig scaleConfig = 
							createManualCapacityScaleConfig(serviceName, service, externalProcessMemoryInMB);
					deployment.scale(scaleConfig);
				}
				else {
					final AutomaticCapacityScaleConfig scaleConfig = 
							createAutomaticCapacityScaleConfig(serviceName, service, externalProcessMemoryInMB);
					deployment.scale(scaleConfig);	
				}
			}
		} else {

			final CloudTemplate template = getComputeTemplate(cloud, templateName);

			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(cloud, template);

			final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(cloud, template,
					cloudFileContents, templateName);

			final String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);
			deployment.memoryCapacityPerContainer((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);

			if (service == null || service.getAutoScaling() == null) {
				
				final ManualCapacityScaleConfig scaleConfig = 
						createManualCapacityScaleConfig(serviceName, service, (int)cloudExternalProcessMemoryInMB);
				
				scaleConfig.setAtMostOneContainerPerMachine(true);
				deployment.scale(scaleConfig);
			}
			else {
				final AutomaticCapacityScaleConfig scaleConfig = 
						createAutomaticCapacityScaleConfig(serviceName, service, (int)cloudExternalProcessMemoryInMB);
				scaleConfig.setAtMostOneContainerPerMachine(true);
				deployment.scale(scaleConfig);
			}
		}

		// add context properties
		setContextProperties(deployment, contextProperties);

		verifyEsmExistsInCluster();
		deployAndWait(serviceName, deployment);

	}

	/**
	 * @param serviceName - the absolute name of the service
	 * @param service - the service DSL or null if not exists
	 * @param externalProcessMemoryInMB - MB memory allocated for the GSC plus the external service.
	 * @return a @{link ManualCapacityScaleConfig} based on the specified service and memory.
	 */
	private ManualCapacityScaleConfig createManualCapacityScaleConfig(
			final String serviceName, Service service,
			final int externalProcessMemoryInMB) throws DSLException {
		
		int numberOfInstances = 1;
		if (service == null) {
			logger.info("Deploying service " + serviceName + " without a recipe. Assuming number of instances is 1");
		}
		else if (service.getNumInstances() > 0){
			numberOfInstances = service.getNumInstances();
			logger.info("Deploying service " + serviceName + " with " + numberOfInstances + " instances.");
		}
		else {
			throw new DSLException("Number of instances must be at least 1");
		}

		return new ManualCapacityScaleConfigurer()
			   .memoryCapacity(externalProcessMemoryInMB * numberOfInstances, MemoryUnit.MEGABYTES)
			   .create();
	}

	/**
	 * @param serviceName - the absolute name of the service
	 * @param service - the service DSL or null if not exists
	 * @param externalProcessMemoryInMB - MB memory allocated for the GSC plus the external service.
	 * @return a @{link AutomaticCapacityScaleConfig} based on the specified service and memory.
	 */
	private AutomaticCapacityScaleConfig createAutomaticCapacityScaleConfig(
			final String serviceName,
			final Service service, 
			final int externalProcessMemoryInMB) throws DSLException {
		
		AutoScalingDetails autoScaling = service.getAutoScaling();
		
		if (service.getMinNumInstances() <= 0) {
			throw new DSLException("Minimum number of instances (" + service.getMinNumInstances() + ") must be 1 or higher.");
		}

		if (service.getMinNumInstances() > service.getMaxNumInstances()) {
			throw new DSLException("maximum number of instances (" + service.getMaxNumInstances() + ") must be equal or greater than the minimum number of instances (" + service.getMinNumInstances() +")");
		}

		if (service.getMinNumInstances() > service.getNumInstances()) {
			throw new DSLException("number of instances (" + service.getNumInstances() + ") must be equal or greater than the minimum number of instances (" + service.getMinNumInstances() +")");
		}

		if (service.getNumInstances() > service.getMaxNumInstances()) {
			throw new DSLException("number of instances (" + service.getNumInstances() + ") must be equal or less than the maximum number of instances (" + service.getMaxNumInstances() +")");
		}

		ProcessingUnitStatisticsId statisticsId = new ProcessingUnitStatisticsId();
		statisticsId.setMonitor(CloudifyConstants.USM_MONITORS_SERVICE_ID);
		statisticsId.setMetric(autoScaling.getMetric());
		statisticsId.setInstancesStatistics(autoScaling.getInstancesStatistics().toInstancesStatistics());

		if (autoScaling.getTimeWindowSeconds() <= autoScaling.getSamplingPeriodSeconds()) {
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Deploying service " + serviceName + " with auto scaling that monitors the last sample of " + autoScaling.getMetric());
			}
			statisticsId.setTimeWindowStatistics(new LastSampleTimeWindowStatisticsConfig());
		}
		else {
			statisticsId.setTimeWindowStatistics(
					autoScaling
					.getTimeStatistics()
					.toTimeWindowStatistics(autoScaling.getTimeWindowSeconds(), TimeUnit.SECONDS));
		}

		AutomaticCapacityScaleRuleConfig rule = 
			new AutomaticCapacityScaleRuleConfigurer()
			.lowThreshold(autoScaling.getLowThreshold())
			.highThreshold(autoScaling.getHighThreshold())
			.statistics(statisticsId)
			.create();

		CapacityRequirementsConfig minCapacity = 
			new CapacityRequirementsConfigurer()
			.memoryCapacity((int)(service.getMinNumInstances() * externalProcessMemoryInMB), MemoryUnit.MEGABYTES)
			.create();

		CapacityRequirementsConfig initialCapacity = 
			new CapacityRequirementsConfigurer()
			.memoryCapacity((int) (service.getNumInstances() * externalProcessMemoryInMB), MemoryUnit.MEGABYTES)
			.create();

		CapacityRequirementsConfig maxCapacity = 
			new CapacityRequirementsConfigurer()
			.memoryCapacity((int)(service.getMaxNumInstances() * externalProcessMemoryInMB), MemoryUnit.MEGABYTES)
			.create();

		AutomaticCapacityScaleConfig scaleConfig = 
			new AutomaticCapacityScaleConfigurer()
			.minCapacity(minCapacity)
			.initialCapacity(initialCapacity)
			.maxCapacity(maxCapacity)
			.statisticsPollingInterval(autoScaling.getSamplingPeriodSeconds(), TimeUnit.SECONDS)
			.addRule(rule)
			.create();

		return scaleConfig;
	}

	private static String extractLocators(final Admin admin) {

		final LookupLocator[] locatorsArray = admin.getLocators();
		final StringBuilder locators = new StringBuilder();

		for (final LookupLocator locator : locatorsArray) {
			locators.append(locator.getHost() + ":" + locator.getPort() + ",");
		}

		if (locators.length() > 0) {
			locators.setLength(locators.length() - 1);
		}

		return locators.toString();
	}

	private long calculateExternalProcessMemory(final Cloud cloud, final CloudTemplate template) {
		// TODO remove hardcoded number
		logger.info("Calculating external proc mem for template: " + template);
		final long cloudExternalProcessMemoryInMB = template.getMachineMemoryMB()
				- cloud.getProvider().getReservedMemoryCapacityPerMachineInMB() - 100;
		logger.fine("template.machineMemoryMB = " + template.getMachineMemoryMB() + "MB\n"
				+ "cloud.provider.reservedMemoryCapacityPerMachineInMB = "
				+ cloud.getProvider().getReservedMemoryCapacityPerMachineInMB() + "MB\n"
				+ "cloudExternalProcessMemoryInMB = " + cloudExternalProcessMemoryInMB + "MB"
				+ "cloudExternalProcessMemoryInMB = cloud.machineMemoryMB - "
				+ "cloud.reservedMemoryCapacityPerMachineInMB" + " = " + cloudExternalProcessMemoryInMB);
		return cloudExternalProcessMemoryInMB;
	}

	/**
	 * Local-Cloud has one agent without any zone.
	 */
	private boolean isLocalCloud() {
		final GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		final boolean isOnlyOneAgent = agents.length == 1;
		final boolean isAgentWithoutZones = agents[0].getZones().isEmpty();
		final boolean isLocalCloud = isOnlyOneAgent && isAgentWithoutZones;
		if (logger.isLoggable(Level.FINE)) {
			if (!isOnlyOneAgent) {
				logger.fine("Not local cloud since there are " + agents.length + " agents");
			} else if (!isAgentWithoutZones) {
				logger.fine("Not local cloud since agent has zones " + agents[0].getZones());
			}
		}
		return isLocalCloud;
	}

	/******
	 * Waits for a single instance of a service to become available. NOTE: currently only uses service name as
	 * processing unit name.
	 * 
	 * @param applicationName
	 *            not used.
	 * @param serviceName
	 *            the service name.
	 * @param timeout
	 *            the timeout period to wait for the processing unit, and then the PU instance.
	 * @param timeUnit
	 *            the time unit used to wait for the processing unit, and then the PU instance.
	 * @return true if instance is found, false if instance is not found in the specified period.
	 */
	public boolean waitForServiceInstance(final String applicationName, final String serviceName, final long timeout,
			final TimeUnit timeUnit) {

		// this should be a very fast lookup, since the service was already
		// successfully deployed
		final String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		final ProcessingUnit pu = this.admin.getProcessingUnits().waitFor(absolutePUName, timeout, timeUnit);
		if (pu == null) {
			return false;
		}

		// ignore the time spent on PU lookup, as it should be failry short.
		return pu.waitFor(1, timeout, timeUnit);

	}

	public String deployElasticProcessingUnit(final String serviceName, final String applicationName, final String zone,
			final File srcFile, final Properties propsFile, final String originalTemplateName, boolean isApplicationInstall,
			int timeout, TimeUnit timeUnit)
			throws TimeoutException, PackagingException, IOException, AdminException, DSLException {

		String templateName;
		if (originalTemplateName == null) {
			templateName = this.defaultTemplateName;
		} else {
			templateName = originalTemplateName;
		}
		
		if (templateName != null) {
			propsFile.put(CloudifyConstants.CONTEXT_PROPERTY_TEMPLATE, templateName);
		}

		Service service = null;
		File projectDir = null;
		// Cloud cloud = null;
		if (srcFile.getName().endsWith(".zip")) {
			projectDir = ServiceReader.extractProjectFile(srcFile);
			final DSLServiceCompilationResult result = ServiceReader.getServiceFromDirectory(new File(projectDir,
					"ext"), applicationName);
			service = result.getService();
			// cloud = ServiceReader.getCloudFromDirectory(new File(projectDir,
			// "ext"));

		}

		if (service == null) {
			doDeploy(applicationName, serviceName, templateName, zone, srcFile, propsFile);
		} else if (service.getLifecycle() != null) {
			doDeploy(applicationName, serviceName, templateName, zone, srcFile, propsFile, service);
		} else if (service.getDataGrid() != null) {
			deployDataGrid(applicationName, serviceName, zone, srcFile, propsFile, service.getDataGrid(), templateName);
		} else if (service.getStatelessProcessingUnit() != null) {
			deployStatelessProcessingUnitAndWait(applicationName, serviceName, zone, new File(projectDir, "ext"),
					propsFile, service.getStatelessProcessingUnit(), templateName, service.getNumInstances());
		} else if (service.getMirrorProcessingUnit() != null) {
			deployStatelessProcessingUnitAndWait(applicationName, serviceName, zone, new File(projectDir, "ext"),
					propsFile, service.getMirrorProcessingUnit(), templateName, service.getNumInstances());
		} else if (service.getStatefulProcessingUnit() != null) {
			deployStatefulProcessingUnit(applicationName, serviceName, zone, new File(projectDir, "ext"), propsFile,
					service.getStatefulProcessingUnit(), templateName);
		} else {
			throw new IllegalStateException("Unsupported service type");
		}
		if (projectDir != null) {
			try {
				FileUtils.deleteDirectory(projectDir);
			} catch (final IOException e) {
				// this may happen if a classloader is holding unto a jar file in the usmlib directory
				// the files are temp files, so it should be ok if they remain on the disk
				logger.log(Level.WARNING, "Failed to delete project files: " + e.getMessage(), e);
			}
		}
		srcFile.delete();
		
		String lifecycleEventContainerID = "";
		if (!isApplicationInstall){
		    if (service == null){
		        lifecycleEventContainerID = startPollingForLifecycleEvents(ServiceUtils.getApplicationServiceName(serviceName, applicationName), applicationName, 
		                1, true, timeout, timeUnit).toString();
		    }else{
		        lifecycleEventContainerID = startPollingForLifecycleEvents(service.getName(), applicationName ,service.getNumInstances(), true, timeout, timeUnit).toString();
		    }
		}
		return lifecycleEventContainerID;
	}

	private void doDeploy(String applicationName, String serviceName,
			String templateName, String zone, File srcFile, Properties propsFile) throws TimeoutException, DSLException {
		doDeploy(applicationName, serviceName, templateName, zone, srcFile, propsFile, null);
	}

	// TODO: add getters for service processing units in the service class that
	// does the cast automatically.
    @RequestMapping(value = "applications/{applicationName}/services/{serviceName}/timeout/{timeout}", method = RequestMethod.POST)
    public @ResponseBody
    Object deployElastic(@PathVariable final String applicationName, @PathVariable final String serviceName, @PathVariable final int timeout,
            @RequestParam(value = "template", required = false) final String templateName,
            @RequestParam(value = "zone", required = true) final String zone,
            @RequestParam(value = "file", required = true) final MultipartFile srcFile,
            @RequestParam(value = "props", required = true) final MultipartFile propsFile) throws TimeoutException,
            PackagingException, IOException, AdminException, DSLException {

        logger.finer("received request to deploy");
        logger.info("Deploying service with template: " + templateName);
        String actualTemplateName = templateName;

        if (cloud != null) {
            if (templateName == null || templateName.length() == 0) {
                if (cloud.getTemplates().isEmpty()) {
                    throw new IllegalStateException("Cloud configuration has no compute template defined!");
                }
                actualTemplateName = cloud.getTemplates().keySet().iterator().next();
                logger.warning("Compute Template name missing from service deployment request."
                        + " Defaulting to first template: " + actualTemplateName);

            }
        }

        final String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
        final byte[] propsBytes = propsFile.getBytes();
        final Properties props = new Properties();
        final InputStream is = new ByteArrayInputStream(propsBytes);
        props.load(is);
        final File dest = copyMultipartFileToLocalFile(srcFile);
        final File destFile = new File(dest.getParent(), absolutePuName + "."
                + FilenameUtils.getExtension(dest.getName()));
        if (destFile.exists()) {
            FileUtils.deleteQuietly(destFile);
        }



//        UUID lifecycleEventsContainerID = StartPollingForLifecycleEvents(dest, serviceName, timeout, TimeUnit.MINUTES);
        String lifecycleEventsContainerID = "";
        if (dest.renameTo(destFile)) {
            FileUtils.deleteQuietly(dest);
            lifecycleEventsContainerID = deployElasticProcessingUnit(absolutePuName, applicationName, zone, destFile, props, actualTemplateName, false, timeout, TimeUnit.MINUTES);
            destFile.deleteOnExit();
        } else {
            logger.warning("Deployment file could not be renamed to the absolute pu name."
                    + " Deploaying using the name " + dest.getName());
            lifecycleEventsContainerID = deployElasticProcessingUnit(absolutePuName, applicationName, zone, dest, props, actualTemplateName, false, timeout, TimeUnit.MINUTES);
            dest.deleteOnExit();
        }

        //TODO: move this Key String to the DSL project as a constant.
        //      Map<String, String> serviceDetails = new HashMap<String, String>();
        //      serviceDetails.put("lifecycleEventsContainerID", lifecycleEventsContainerID.toString());
        return successStatus(lifecycleEventsContainerID.toString());
    }

	private File getJarFileFromDir(File serviceFileOrDir, final File serviceDirectory, final String jarName)
			throws IOException {
		if (!serviceFileOrDir.isAbsolute()) {
			serviceFileOrDir = new File(serviceDirectory, serviceFileOrDir.getPath());
		}
		final File destJar = new File(serviceDirectory.getParent(), jarName + ".jar");
		FileUtils.deleteQuietly(destJar);
		if (serviceFileOrDir.isDirectory()) {
			final File jarFile = File.createTempFile(serviceFileOrDir.getName(), ".jar");
			ZipUtils.zip(serviceFileOrDir, jarFile);
			// rename the jar so would appear as 'Absolute pu name' in the deploy folder.
			jarFile.renameTo(destJar);
			jarFile.deleteOnExit();
			return destJar;
		} else if (serviceFileOrDir.isFile()) {
			// rename the jar so would appear as 'Absolute pu name' in the deploy folder.
			serviceFileOrDir.renameTo(destJar);
			return destJar;
		}

		throw new FileNotFoundException("The file " + serviceFileOrDir + " was not found in the service folder");
	}

	private CloudTemplate getComputeTemplate(final Cloud cloud, final String templateName) {
		if (templateName == null) {
			final Entry<String, CloudTemplate> entry = cloud.getTemplates().entrySet().iterator().next();

			logger.warning("Service does not specify template name! Defaulting to template: " + entry.getKey());
			return entry.getValue();
		} 
		final CloudTemplate template = cloud.getTemplates().get(templateName);
		if (template == null) {
			throw new IllegalArgumentException("Could not find compute template: " + templateName);
		}
		return template;
	}

	// TODO: consider adding MemoryUnits to DSL
	// TODO: add memory unit to names
	private void deployDataGrid(final String applicationName, final String serviceName, final String zone,
			final File srcFile, final Properties contextProperties, final DataGrid dataGridConfig,
			final String templateName) throws AdminException, TimeoutException, DSLException {

		final int containerMemoryInMB = dataGridConfig.getSla().getMemoryCapacityPerContainer();
		final int maxMemoryInMB = dataGridConfig.getSla().getMaxMemoryCapacity();
		final int reservedMemoryCapacityPerMachineInMB = 256;

		logger.finer("received request to install datagrid");

		final ElasticSpaceDeployment deployment = new ElasticSpaceDeployment(serviceName)
				.memoryCapacityPerContainer(containerMemoryInMB, MemoryUnit.MEGABYTES)
				.maxMemoryCapacity(maxMemoryInMB, MemoryUnit.MEGABYTES)
				.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME, applicationName)
				.highlyAvailable(dataGridConfig.getSla().getHighlyAvailable())
				// allow single machine for local development purposes
				.singleMachineDeployment();

		setContextProperties(deployment, contextProperties);

		if (cloud == null) {
			setSharedMachineProvisioning(deployment, zone, reservedMemoryCapacityPerMachineInMB);

			if (isLocalCloud()) {
				deployment.scale(new ManualCapacityScaleConfigurer().memoryCapacity(
						dataGridConfig.getSla().getMemoryCapacity(), MemoryUnit.MEGABYTES).create());

			} else {
				// eager scaling. 1 container per machine
				deployment.scale(new EagerScaleConfigurer().atMostOneContainerPerMachine().create());
			}

		} else {

			final CloudTemplate template = getComputeTemplate(cloud, templateName);

			validateAndPrepareStatefulSla(serviceName, dataGridConfig.getSla(), cloud, template);

			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(cloud, template);

			final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(cloud, template,
					cloudFileContents, templateName);
			// CloudMachineProvisioningConfig config =
			// CloudDSLToCloudMachineProvisioningConfig.convert(cloud);

			final String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);
			deployment.memoryCapacityPerContainer((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);

			deployment.scale(new ManualCapacityScaleConfigurer()
					.memoryCapacity((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES)
					.atMostOneContainerPerMachine().create());
		}

		deployAndWait(serviceName, deployment);

	}

	private void setContextProperties(final ElasticDeploymentTopology deployment, final Properties contextProperties) {
		final Set<Entry<Object, Object>> contextPropsEntries = contextProperties.entrySet();
		for (final Entry<Object, Object> entry : contextPropsEntries) {
			deployment.addContextProperty((String) entry.getKey(), (String) entry.getValue());
		}
	}

	private void setSharedMachineProvisioning(final ElasticDeploymentTopology deployment, final String zone,
			final int reservedMemoryCapacityPerMachineInMB) {
		// All PUs on this role share the same machine. Machines
		// are identified by zone.
		deployment.sharedMachineProvisioning(SHARED_ISOLATION_ID,
				new DiscoveredMachineProvisioningConfigurer().addGridServiceAgentZone(zone)
						.reservedMemoryCapacityPerMachine(reservedMemoryCapacityPerMachineInMB, MemoryUnit.MEGABYTES)
						.create());
	}

	private void setDedicatedMachineProvisioning(final ElasticDeploymentTopology deployment,
			final ElasticMachineProvisioningConfig config) {
		deployment.dedicatedMachineProvisioning(config);
	}

	private void deployStatelessProcessingUnitAndWait(final String applicationName, final String serviceName,
			final String zone, final File extractedServiceFolder, final Properties contextProperties,
			final StatelessProcessingUnit puConfig, final String templateName, final int numberOfInstances)
			throws IOException, AdminException, TimeoutException, DSLException {

		final File jarFile = getJarFileFromDir(new File(puConfig.getBinaries()), extractedServiceFolder, serviceName);
		// TODO:if not specified use machine memory defined in DSL
		final int containerMemoryInMB = puConfig.getSla().getMemoryCapacityPerContainer();
		// TODO:Read from cloud DSL
		final int reservedMemoryCapacityPerMachineInMB = 256;
		final ElasticStatelessProcessingUnitDeployment deployment = new ElasticStatelessProcessingUnitDeployment(
				jarFile).memoryCapacityPerContainer(containerMemoryInMB, MemoryUnit.MEGABYTES)
				.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME, applicationName)
				.name(serviceName);
		// TODO:read from cloud DSL

		setContextProperties(deployment, contextProperties);

		if (cloud == null) {
			setSharedMachineProvisioning(deployment, zone, reservedMemoryCapacityPerMachineInMB);
			verifyEsmExistsInCluster();

			if (isLocalCloud()) {
				deployment.scale(new ManualCapacityScaleConfigurer().memoryCapacity(
						containerMemoryInMB * numberOfInstances, MemoryUnit.MEGABYTES).create());
			} else {
				// eager scaling. one container per machine
				deployment.scale(new EagerScaleConfigurer().atMostOneContainerPerMachine().create());
			}
		} else {
			final CloudTemplate template = getComputeTemplate(cloud, templateName);
			validateAndPrepareStatelessSla(puConfig.getSla(), cloud, template);
			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(cloud, template);

			final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(cloud, template,
					cloudFileContents, templateName);
			// CloudMachineProvisioningConfig config =
			// CloudDSLToCloudMachineProvisioningConfig.convert(cloud);

			final String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);
			deployment.memoryCapacityPerContainer((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);

			deployment.scale(new ManualCapacityScaleConfigurer()
					.memoryCapacity(containerMemoryInMB * numberOfInstances, MemoryUnit.MEGABYTES)
					.atMostOneContainerPerMachine().create());
		}
		deployAndWait(serviceName, deployment);
		jarFile.delete();

	}

	private void verifyEsmExistsInCluster() throws IllegalStateException {
		final ElasticServiceManager esm = getESM();
		if (esm == null) {
			// TODO - Add locators
			throw new IllegalStateException("Could not find an ESM in the cluster. Groups: "
					+ Arrays.toString(this.admin.getGroups()));
		}

	}

	// TODO:Clean this class it has a lot of code duplications
	private void deployStatefulProcessingUnit(final String applicationName, final String serviceName,
			final String zone, final File extractedServiceFolder, final Properties contextProperties,
			final StatefulProcessingUnit puConfig, final String templateName) throws IOException, AdminException,
			TimeoutException, DSLException {

		final File jarFile = getJarFileFromDir(new File(puConfig.getBinaries()), extractedServiceFolder, serviceName);
		final int containerMemoryInMB = puConfig.getSla().getMemoryCapacityPerContainer();
		final int maxMemoryCapacityInMB = puConfig.getSla().getMaxMemoryCapacity();
		final int reservedMemoryCapacityPerMachineInMB = 256;

		final ElasticStatefulProcessingUnitDeployment deployment = new ElasticStatefulProcessingUnitDeployment(jarFile)
				.name(serviceName).memoryCapacityPerContainer(containerMemoryInMB, MemoryUnit.MEGABYTES)
				.maxMemoryCapacity(maxMemoryCapacityInMB + "m")
				.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME, applicationName)
				.highlyAvailable(puConfig.getSla().getHighlyAvailable()).singleMachineDeployment();

		setContextProperties(deployment, contextProperties);

		if (cloud == null) {
			setSharedMachineProvisioning(deployment, zone, reservedMemoryCapacityPerMachineInMB);
			verifyEsmExistsInCluster();
			if (isLocalCloud()) {
				deployment.scale(new ManualCapacityScaleConfigurer().memoryCapacity(
						puConfig.getSla().getMemoryCapacity(), MemoryUnit.MEGABYTES).create());
			} else {
				// eager scaling. one container per machine
				deployment.scale(new EagerScaleConfigurer().atMostOneContainerPerMachine().create());
			}
		} else {

			final CloudTemplate template = getComputeTemplate(cloud, templateName);

			validateAndPrepareStatefulSla(serviceName, puConfig.getSla(), cloud, template);

			final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(cloud, template,
					cloudFileContents, templateName);

			final String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);

			deployment.scale(new ManualCapacityScaleConfigurer()
					.memoryCapacity(puConfig.getSla().getMemoryCapacity(), MemoryUnit.MEGABYTES)
					.atMostOneContainerPerMachine().create());

		}

		deployAndWait(serviceName, deployment);
		jarFile.delete();

	}

	private void validateAndPrepareStatefulSla(final String serviceName, final Sla sla, final Cloud cloud,
			final CloudTemplate template) throws DSLException {

		validateMemoryCapacityPerContainer(sla, cloud, template);

		if (sla.getMaxMemoryCapacity() != null && sla.getMemoryCapacity() != null
				&& sla.getMaxMemoryCapacity() < sla.getMemoryCapacity()) {

			throw new DSLException("Max memory capacity is smaller than the memory capacity."
					+ sla.getMaxMemoryCapacity() + " < " + sla.getMemoryCapacity());
		}

		final int minimumNumberOfContainers = sla.getHighlyAvailable() ? 2 : 1;
		final int minMemoryInMB = minimumNumberOfContainers * sla.getMemoryCapacityPerContainer();

		if (sla.getMemoryCapacity() == null || sla.getMemoryCapacity() < minMemoryInMB) {

			logger.info("Setting memoryCapacity for service " + serviceName + " to minimum " + minMemoryInMB + "MB");
			sla.setMemoryCapacity(minMemoryInMB);
		}

		if (sla.getMaxMemoryCapacity() == null || sla.getMaxMemoryCapacity() < sla.getMemoryCapacity()) {

			logger.info("Setting maxMemoryCapacity for service " + serviceName + " to memoryCapacity "
					+ sla.getMemoryCapacity() + "MB");
			sla.setMaxMemoryCapacity(sla.getMemoryCapacity());
		}
	}

	private void validateAndPrepareStatelessSla(final Sla sla, final Cloud cloud, final CloudTemplate template)
			throws DSLException {

		validateMemoryCapacityPerContainer(sla, cloud, template);

		if (sla.getMemoryCapacity() != null) {
			throw new DSLException("memoryCapacity SLA is not supported in this service");
		}

		if (sla.getMaxMemoryCapacity() != null) {
			throw new DSLException("maxMemoryCapacity SLA is not supported in this service");
		}

	}

	private void validateMemoryCapacityPerContainer(final Sla sla, final Cloud cloud, final CloudTemplate template)
			throws DSLException {
		if (cloud == null) {
			// No cloud, must specify memory capacity per container explicitly
			if (sla.getMemoryCapacityPerContainer() == null) {
				throw new DSLException("Cannot determine memoryCapacityPerContainer SLA");
			}
		} else {
			// Assuming one container per machine then container memory =
			// machine memory
			final int availableMemoryOnMachine = (int) calculateExternalProcessMemory(cloud, template);
			if (sla.getMemoryCapacityPerContainer() != null
					&& sla.getMemoryCapacityPerContainer() > availableMemoryOnMachine) {
				throw new DSLException("memoryCapacityPerContainer SLA is larger than available memory on machine\n"
						+ sla.getMemoryCapacityPerContainer() + " > " + availableMemoryOnMachine);
			}

			if (sla.getMemoryCapacityPerContainer() == null) {
				sla.setMemoryCapacityPerContainer(availableMemoryOnMachine);
			}
		}
	}

	private void deployAndWait(final String serviceName, final ElasticStatefulProcessingUnitDeployment deployment)
			throws TimeoutException, AdminException {
		final ProcessingUnit pu = getGridServiceManager().deploy(deployment, 60, TimeUnit.SECONDS);
		if (pu == null) {
			throw new TimeoutException("Timed out waiting for Service " + serviceName + " deployment.");
		}
	}
	
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/timeout/{timeout}/set-instances", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> setServiceInstances(@PathVariable final String applicationName, @PathVariable final String serviceName, @PathVariable final int timeout,
			@RequestParam(value = "count", required = true) final int count){

	    Map<String, Object> returnMap = new HashMap<String, Object>();
		final String puName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit(puName);
		if(pu == null) {
			return errorStatus(ResponseConstants.FAILED_TO_LOCATE_SERVICE, serviceName);
		}
		
		Properties contextProperties = pu.getBeanLevelProperties().getContextProperties();
		final String elasticProp = contextProperties.getProperty(CloudifyConstants.CONTEXT_PROPERTY_ELASTIC);
		final String templateName = contextProperties.getProperty(CloudifyConstants.CONTEXT_PROPERTY_TEMPLATE);
		
		if(elasticProp == null || !Boolean.parseBoolean(elasticProp)) {
			return errorStatus(ResponseConstants.SERVICE_NOT_ELASTIC, serviceName);
		}
		
		logger.info("Scaling " + puName + " to " + count + " instances");
		
		UUID eventContainerID;
		if (cloud == null) {
			if (isLocalCloud()) {
				// Manual scale by number of instances			    
				pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(512 * count, MemoryUnit.MEGABYTES).create());
			} else {
				// Eager scale (1 container per machine per PU)
				return errorStatus(ResponseConstants.SET_INSTANCES_NOT_SUPPORTED_IN_EAGER);				
			}
		} else {
			
			final CloudTemplate template = getComputeTemplate(cloud, templateName);
			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(cloud, template);
			pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity((int) (cloudExternalProcessMemoryInMB * count), MemoryUnit.MEGABYTES).atMostOneContainerPerMachine().create());
		}		
		
        eventContainerID = startPollingForLifecycleEvents(serviceName, applicationName, count, false, timeout, TimeUnit.MINUTES);
        returnMap.put(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID, eventContainerID);
        
        return successStatus(returnMap);
	}
}
