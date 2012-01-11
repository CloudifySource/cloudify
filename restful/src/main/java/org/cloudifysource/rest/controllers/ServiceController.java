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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import net.jini.core.discovery.LookupLocator;
import org.apache.commons.io.FileUtils;
import org.cloudifysource.rest.ResponseConstants;
import org.cloudifysource.rest.util.ApplicationInstallerRunnable;
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
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.EagerScaleConfigurer;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.topology.ElasticDeploymentTopology;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.space.Space;
import org.openspaces.admin.zone.Zone;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.util.MemoryUnit;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.gigaspaces.cloudify.dsl.DataGrid;
import com.gigaspaces.cloudify.dsl.Service;
import com.gigaspaces.cloudify.dsl.Sla;
import com.gigaspaces.cloudify.dsl.StatefulProcessingUnit;
import com.gigaspaces.cloudify.dsl.StatelessProcessingUnit;
import com.gigaspaces.cloudify.dsl.cloud.Cloud2;
import com.gigaspaces.cloudify.dsl.cloud.CloudTemplate;
import com.gigaspaces.cloudify.dsl.internal.CloudifyConstants;
import com.gigaspaces.cloudify.dsl.internal.DSLApplicationCompilatioResult;
import com.gigaspaces.cloudify.dsl.internal.DSLException;
import com.gigaspaces.cloudify.dsl.internal.DSLServiceCompilationResult;
import com.gigaspaces.cloudify.dsl.internal.EventLogConstants;
import com.gigaspaces.cloudify.dsl.internal.ServiceReader;
import com.gigaspaces.cloudify.dsl.internal.packaging.CloudConfigurationHolder;
import com.gigaspaces.cloudify.dsl.internal.packaging.PackagingException;
import com.gigaspaces.cloudify.dsl.internal.packaging.ZipUtils;
import com.gigaspaces.cloudify.dsl.utils.ServiceUtils;
import com.gigaspaces.cloudify.esc.driver.provisioning.CloudifyMachineProvisioningConfig;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;

/**
 * @author rafi
 * @since 8.0.3
 */
@Controller
@RequestMapping("/service")
public class ServiceController {

	private static final int TEN_MINUTES_MILLISECONDS = 60 * 1000 * 10;
	private static final int THREAD_POOL_SIZE = 2;
	private static final String SHARED_ISOLATION_ID = "public";
	private static final int PU_DISCOVERY_TIMEOUT_SEC = 8;
	private static final String USM_EVENT_LOGGER_NAME = ".*.USMEventLogger.{0}\\].*";
	@Autowired(required = true)
	private Admin admin;

	private Cloud2 cloud = null;

	private final Logger logger = Logger.getLogger(getClass().getName());
	private String cloudFileContents;
	private String defaultTemplateName;

	public ServiceController() throws IOException {

	

	}

	
	@PostConstruct
	public void init() {
		logger.info("Initializing service controller cloud configuration");
		this.cloud = readCloud();
		if (cloud != null) {
			if (this.cloud.getTemplates().size() == 0) {
				throw new IllegalArgumentException("No templates defined in cloud configuration!");
			}
			this.defaultTemplateName = this.cloud.getTemplates().keySet().iterator().next();
			logger.info("Setting default template name to: " + defaultTemplateName
					+ ". This template will be used for services that do not specify an explicit template");
		} else {
			logger.info("Service Controller is running in local cloud mode");
		}
	}
	
	private String getCloudConfigurationFromManagementSpace() {
		GigaSpace gigaSpace = getManagementSpace();
		logger.info("Waiting for cloud configuration to become available in management space");
		CloudConfigurationHolder config = gigaSpace.read( new CloudConfigurationHolder(),1000 *60);
		if(config == null) {
			
			logger.warning("Could not find the expected Cloud Configuration Holder in Management space! Defaulting to local cloud!");
			return null;
		}
		return config.getCloudConfiguration();
	}
	
	private GigaSpace getManagementSpace() {
		Space space = this.admin.getSpaces().waitFor(CloudifyConstants.MANAGEMENT_SPACE_NAME, 1, TimeUnit.MINUTES);
		if(space == null) {
			throw new IllegalStateException("Could not find management space (" + CloudifyConstants.MANAGEMENT_SPACE_NAME + ")");
		}
		
		return space.getGigaSpace();
	}
	
	private Cloud2 readCloud() {
		logger.info("Loading cloud configuration");

		this.cloudFileContents = getCloudConfigurationFromManagementSpace();
		if(this.cloudFileContents == null) {
			// must be local cloud
			return null;

		}
		Cloud2 cloud = null;
		try {
			cloud = ServiceReader.readCloud(cloudFileContents);
		} catch (DSLException e) {
			throw new IllegalArgumentException("Failed to read cloud configuration file: " + cloudFileContents
					+ ". Error was: " + e.getMessage(), e);
		}

		logger.info("Successfully loaded cloud configuration file from management space");

		logger.info("Setting cloud local directory to: " + cloud.getProvider().getRemoteDirectory());
		cloud.getProvider().setLocalDirectory(cloud.getProvider().getRemoteDirectory());
		logger.info("Loaded cloud: " + cloud);

		return cloud;

	}

	// Set up a small thread pool with daemon threads.
	private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE, new ThreadFactory() {

		private int counter = 1;

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "ServiceControllerExecutor-" + (counter++));
			thread.setDaemon(true);
			return thread;
		}
	});

	@RequestMapping(value = "/testrest", method = RequestMethod.GET)
	public @ResponseBody
	Object test() {
		if (admin.getLookupServices().getSize() > 0) {
			return successStatus();
		}
		String groups = Arrays.toString((admin.getGroups()));
		String locators = Arrays.toString((admin.getLocators()));
		return errorStatus(FAILED_TO_LOCATE_LUS, groups, locators);
	}

	@RequestMapping(value = "/cloudcontroller/deploy", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> deploy(
			@RequestParam(value = "applicationName", defaultValue = "default") String applicationName, @RequestParam(
					value = "file") MultipartFile srcFile) throws IllegalStateException, IOException {
		logger.finer("Deploying a service");
		File tmpfile = File.createTempFile("gs___", null);
		File dest = new File(tmpfile.getParent(), srcFile.getOriginalFilename());
		tmpfile.delete();
		srcFile.transferTo(dest);
		GridServiceManager gsm = getGsm();
		ProcessingUnit pu;

		if (gsm != null) {
			pu = gsm.deploy(new ProcessingUnitDeployment(dest).setContextProperty(
					CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME, applicationName));
			dest.delete();
		} else {
			return errorStatus(FAILED_TO_LOCATE_GSM);
		}
		if (pu == null) {
			return errorStatus(FAILED_TO_LOCATE_SERVICE_AFTER_DEPLOYMENT, applicationName);
		} else {
			return successStatus(pu.getName());
		}
	}

	@RequestMapping(value = "/applications", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> getApplicationsList() {
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list applications");
		}
		Applications apps = admin.getApplications();
		List<String> appNames = new ArrayList<String>(apps.getSize());
		for (Application app : apps) {
			appNames.add(app.getName());
		}
		return successStatus(appNames);
	}

	@RequestMapping(value = "/applications/{applicationName}/services", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> getServicesList(@PathVariable String applicationName) {
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list applications");
		}
		Application app = admin.getApplications().waitFor(applicationName, 5, TimeUnit.SECONDS);
		if (app == null) {
			return errorStatus(FAILED_TO_LOCATE_APP, applicationName);
		}
		ProcessingUnits pus = app.getProcessingUnits();
		List<String> puNames = new ArrayList<String>(pus.getSize());
		for (ProcessingUnit pu : pus) {
			puNames.add(pu.getName());
		}
		List<String> serviceNames = new ArrayList<String>(pus.getSize());
		ListIterator<String> listIterator = puNames.listIterator();
		while (listIterator.hasNext()) {
			String absolutePuName = listIterator.next();
			serviceNames.add(ServiceUtils.getApplicationServiceName(absolutePuName, applicationName));
		}
		return successStatus(serviceNames);
	}

	@RequestMapping(value = "/applications/{applicationName}/services/{serviceName}/USMEventsLogs",
			method = RequestMethod.GET)
	public @ResponseBody
	Map<?, ?> getServiceLifecycleLogs(@PathVariable String applicationName, @PathVariable String serviceName) {
		// TODO:not run over
		String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		List<Map<String, String>> serviceEventDetailes = new ArrayList<Map<String, String>>();
		String regex = MessageFormat.format(USM_EVENT_LOGGER_NAME, absolutePuName);
		LogEntryMatcher matcher = regex(regex);
		Zone zone = admin.getZones().getByName(absolutePuName);
		if (zone == null) {
			logger.info("Zone " + absolutePuName + " does not exist");
			return successStatus();
		}
		for (GridServiceContainer container : zone.getGridServiceContainers()) {
			LogEntries logEntries = container.logEntries(matcher);
			for (LogEntry logEntry : logEntries) {
				if (logEntry.isLog()) {
					Date tenMinutesAgoGscTime = new Date(new Date().getTime()
							+ container.getOperatingSystem().getTimeDelta() - TEN_MINUTES_MILLISECONDS);
					if (tenMinutesAgoGscTime.before(new Date(logEntry.getTimestamp()))) {
						Map<String, String> serviceEventsMap = getServiceDetailes(logEntry, container, absolutePuName,
								applicationName);
						serviceEventDetailes.add(serviceEventsMap);
					}
				}
			}
		}
		return successStatus(serviceEventDetailes);
	}

	private Map<String, String> getServiceDetailes(LogEntry logEntry, GridServiceContainer container,
			String absolutePuName, String applicationName) {

		Map<String, String> returnMap = new HashMap<String, String>();

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

	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/instances",
			method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> getServiceInstanceList(@PathVariable String applicationName, @PathVariable String serviceName) {
		String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list instances for service " + absolutePuName + " of application "
					+ applicationName);
		}
		// todo: application awareness
		ProcessingUnit pu = admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
				TimeUnit.SECONDS);
		if (pu == null) {
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}
		Map<Integer, String> instanceMap = new HashMap<Integer, String>();
		ProcessingUnitInstance[] instances = pu.getInstances();
		for (ProcessingUnitInstance instance : instances) {
			instanceMap.put(instance.getInstanceId(), instance.getVirtualMachine().getMachine().getHostName());
		}
		return successStatus(instanceMap);
	}

	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/beans/{beanName}/invoke",
			method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> invoke(@PathVariable String applicationName, @PathVariable String serviceName,
			@PathVariable String beanName, @RequestBody Map<String, Object> params) {
		String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to invoke bean " + beanName + " of service " + absolutePuName
					+ " of application " + applicationName);
		}

		// Get the PU
		ProcessingUnit pu = admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
				TimeUnit.SECONDS);
		if (pu == null) {
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}

		// result, mapping service instances to results
		Map<String, Object> invocationResult = new HashMap<String, Object>();
		ProcessingUnitInstance[] instances = pu.getInstances();

		// Why a map? TODO: Use an array here instead.
		// map between service name and its future
		Map<String, Future<Object>> futures = new HashMap<String, Future<Object>>(instances.length);
		for (ProcessingUnitInstance instance : instances) {
			// key includes instance ID and host name
			String serviceInstanceName = buildServiceInstanceName(instance);
			try {
				Future<Object> future = ((DefaultProcessingUnitInstance) instance).invoke(beanName, params);
				futures.put(serviceInstanceName, future);
			} catch (Exception e) {
				logger.severe("Error invoking service " + serviceName + ":" + instance.getInstanceId() + " on host "
						+ instance.getVirtualMachine().getMachine().getHostName());
				invocationResult.put(serviceInstanceName, "pu_instance_invocation_failure");
			}
		}

		for (Map.Entry<String, Future<Object>> entry : futures.entrySet()) {
			try {
				Object result = entry.getValue().get();
				// use only tostring of collection values, to avoid
				// serialization problems
				result = postProcessInvocationResult(result, entry.getKey());

				invocationResult.put(entry.getKey(), result);
			} catch (Exception e) {
				invocationResult.put(entry.getKey(), "Invocation failure: " + e.getMessage());
			}
		}
		return successStatus(invocationResult);
	}

	private Object postProcessInvocationResult(Object result, String instanceName) {
		if (result instanceof Map<?, ?>) {
			Map<String, String> modifiedMap = new HashMap<String, String>();
			@SuppressWarnings("unchecked")
			Set<Entry<String, Object>> entries = ((Map<String, Object>) result).entrySet();
			for (Entry<String, Object> subEntry : entries) {

				modifiedMap.put(subEntry.getKey(),
						(subEntry.getValue() == null ? null : subEntry.getValue().toString()));
			}
			modifiedMap.put(CloudifyConstants.INVOCATION_RESPONSE_INSTANCE_NAME, instanceName);
			result = modifiedMap;
		} else {
			result = result.toString();
		}
		return result;
	}

	@RequestMapping(
			value = "applications/{applicationName}/services/{serviceName}/instances/{instanceId}/beans/{beanName}/invoke",
			method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> invokeInstance(@PathVariable String applicationName, @PathVariable String serviceName,
			@PathVariable int instanceId, @PathVariable String beanName, @RequestBody Map<String, Object> params) {
		String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to invoke bean " + beanName + " of service " + serviceName
					+ " of application " + applicationName);
		}

		// Get PU
		ProcessingUnit pu = admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
				TimeUnit.SECONDS);
		if (pu == null) {
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}

		// Get PUI
		InternalProcessingUnitInstance pui = findInstanceById(pu, instanceId);
		
		if (pui == null) {
			logger.severe("Could not find service instance " + instanceId + " for service " + absolutePuName);
			return errorStatus(ResponseConstants.SERVICE_INSTANCE_UNAVAILABLE, applicationName, absolutePuName,
					Integer.toString(instanceId));
		}
		final String instanceName = buildServiceInstanceName(pui);
		// Invoke the remote service
		try {
			Future<?> future = pui.invoke(beanName, params);
			Object invocationResult = future.get();
			Object finalResult = postProcessInvocationResult(invocationResult, instanceName);
			return successStatus(finalResult);
		} catch (Exception e) {
			logger.severe("Error invoking pu instance " + absolutePuName + ":" + instanceId + " on host "
					+ pui.getVirtualMachine().getMachine().getHostName());
			return errorStatus(FAILED_TO_INVOKE_INSTANCE, applicationName, absolutePuName, Integer.toString(instanceId));
		}
	}

	private InternalProcessingUnitInstance findInstanceById(ProcessingUnit pu, int id) {
		ProcessingUnitInstance[] instances = pu.getInstances();
		for (ProcessingUnitInstance instance : instances) {
			if (instance.getInstanceId() == id) {
				return (InternalProcessingUnitInstance) instance;
			}
		}
		return null;
	}

	private String buildServiceInstanceName(ProcessingUnitInstance instance) {
		return "instance #" + instance.getInstanceId() + "@" + instance.getVirtualMachine().getMachine().getHostName();
	}

	private Map<String, Object> unavailableServiceError(String serviceName) {
		// TODO: Consider telling the user he might be using the wrong
		// application name.
		return errorStatus(FAILED_TO_LOCATE_SERVICE, ServiceUtils.getFullServiceName(serviceName).getServiceName());
	}

	private GridServiceManager getGsm(String id) {
		if (id == null) {
			return admin.getGridServiceManagers().waitForAtLeastOne(5000, TimeUnit.MILLISECONDS);
		} else {
			return admin.getGridServiceManagers().getManagerByUID(id);
		}
	}

	private GridServiceManager getGsm() {
		return getGsm(null);
	}

	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/undeploy",
			method = RequestMethod.DELETE)
	public @ResponseBody
	Map<String, Object> undeploy(@PathVariable String applicationName, @PathVariable String serviceName) {
		String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
				TimeUnit.SECONDS);
		if (processingUnit == null) {
			return unavailableServiceError(absolutePuName);
		}
		processingUnit.undeploy();
		return successStatus();
	}

	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/addinstance",
			method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> addInstance(@PathVariable String applicationName, @PathVariable String serviceName,
			@RequestBody Map<String, String> params) {
		String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		int timeout = Integer.valueOf(params.get("timeout"));
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
				TimeUnit.SECONDS);
		if (processingUnit == null) {
			return unavailableServiceError(absolutePuName);
		}
		int before = processingUnit.getNumberOfInstances();
		processingUnit.incrementInstance();
		boolean result = processingUnit.waitFor(before + 1, timeout, TimeUnit.SECONDS);
		if (result) {
			return successStatus();
		}
		return errorStatus(ResponseConstants.FAILED_TO_ADD_INSTANCE, applicationName, serviceName);
	}

	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/instances/{instanceId}/remove",
			method = RequestMethod.DELETE)
	public @ResponseBody
	Map<String, Object> removeInstance(@PathVariable String applicationName, @PathVariable String serviceName,
			@PathVariable int instanceId) {
		String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		// todo: application awareness
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
				TimeUnit.SECONDS);
		if (processingUnit == null) {
			return unavailableServiceError(absolutePuName);
		}
		for (ProcessingUnitInstance instance : processingUnit.getInstances()) {
			if (instance.getInstanceId() == instanceId) {
				instance.decrement();
				return successStatus();
			}
		}
		return errorStatus(SERVICE_INSTANCE_UNAVAILABLE);
	}

	private void deployAndWait(final String serviceName, ElasticSpaceDeployment deployment) throws TimeoutException,
			AdminException {
		ProcessingUnit pu = getGridServiceManager().deploy(deployment, 60, TimeUnit.SECONDS);
		if (pu == null) {
			throw new TimeoutException("Timed out waiting for Service " + serviceName + " deployment.");
		}
	}

	private ElasticServiceManager getESM() {
		return admin.getElasticServiceManagers().waitForAtLeastOne(5000, TimeUnit.MILLISECONDS);
	}

	private void deployAndWait(String serviceName, ElasticStatelessProcessingUnitDeployment deployment)
			throws TimeoutException, AdminException {
		ProcessingUnit pu = getGridServiceManager().deploy(deployment, 60, TimeUnit.SECONDS);
		if (pu == null) {
			throw new TimeoutException("Timed out waiting for Service " + serviceName + " deployment.");
		}
	}

	private GridServiceManager getGridServiceManager() {
		if (admin.getGridServiceManagers().getSize() == 0) {
			throw new AdminException("Cannot locate Grid Service Manager");
		}
		return admin.getGridServiceManagers().iterator().next();
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public void resolveDocumentNotFoundException(HttpServletResponse response, Exception e) throws IOException {

		if (response.isCommitted()) {
			logger.log(Level.WARNING,
					"Caught exception, but response already commited. Not sending error message based on exception", e);
		} else {
			Writer writer = response.getWriter();
			String message = "{\"status\":\"error\", \"error\":\"" + e.getMessage() + "\"}";
			logger.log(Level.SEVERE, "caught exception. Sending response message " + message, e);
			writer.write(message);
		}
	}

	/******************
	 * Uninstalls an application by uninstalling all of its services. Order of
	 * uninstallations is determined by the context property
	 * 'com.gs.application.services' which should exist in all service PUs.
	 * 
	 * @param applicationName
	 * @return Map with return value; @ .
	 */
	@RequestMapping(value = "applications/{applicationName}", method = RequestMethod.DELETE)
	public @ResponseBody
	Map<String, Object> uninstallApplication(@PathVariable final String applicationName) {

		// Check that Application exists
		Application app = this.admin.getApplications().waitFor(applicationName, 10, TimeUnit.SECONDS);
		if (app == null) {
			logger.log(Level.INFO, "Cannot uninstall application " + applicationName
					+ " since it has not been discovered yet.");
			return RestUtils.errorStatus(ResponseConstants.FAILED_TO_LOCATE_APP, applicationName);
		}
		ProcessingUnit[] pus = app.getProcessingUnits().getProcessingUnits();

		StringBuilder sb = new StringBuilder();
		final List<ProcessingUnit> uninstallOrder = createUninstallOrder(pus, applicationName);
		if (uninstallOrder.size() > 0) {

			((InternalAdmin) admin).scheduleAdminOperation(new Runnable() {

				@Override
				public void run() {
					for (ProcessingUnit processingUnit : uninstallOrder) {
						try {
							if (processingUnit.waitForManaged(10, TimeUnit.SECONDS) == null) {
								logger.log(Level.WARNING, "Failed to locate GSM that is managing Processing Unit "
										+ processingUnit.getName());
							} else {
								logger.log(Level.INFO, "Undeploying Processing Unit " + processingUnit.getName());
								processingUnit.undeploy();
							}
						} catch (Exception e) {
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
			return RestUtils.successStatus();
		} else {
			return RestUtils.errorStatus(errors);
		}
	}

	private List<ProcessingUnit> createUninstallOrder(ProcessingUnit[] pus, String applicationName) {

		// TODO: Refactor this - merge with createServiceOrder, as methods are
		// very similar
		DirectedGraph<ProcessingUnit, DefaultEdge> graph = new DefaultDirectedGraph<ProcessingUnit, DefaultEdge>(
				DefaultEdge.class);

		for (ProcessingUnit processingUnit : pus) {
			graph.addVertex(processingUnit);
		}

		Map<String, ProcessingUnit> puByName = new HashMap<String, ProcessingUnit>();
		for (ProcessingUnit processingUnit : pus) {
			puByName.put(processingUnit.getName(), processingUnit);
		}

		for (ProcessingUnit processingUnit : pus) {
			final String dependsOn = (String) processingUnit.getBeanLevelProperties().getContextProperties()
					.get(CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON);
			if (dependsOn == null) {
				logger.warning("Could not find the " + CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON
						+ " property for processing unit " + processingUnit.getName());

			} else {
				String[] dependencies = dependsOn.replace("[", "").replace("]", "").split(",");
				for (String puName : dependencies) {
					final String normalizedPuName = puName.trim();
					if (normalizedPuName.length() > 0) {
						ProcessingUnit dependency = puByName.get(normalizedPuName);
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

		CycleDetector<ProcessingUnit, DefaultEdge> cycleDetector = new CycleDetector<ProcessingUnit, DefaultEdge>(graph);
		boolean containsCycle = cycleDetector.detectCycles();

		if (containsCycle) {
			logger.warning("Detected a cycle in the dependencies of application: "
					+ applicationName
					+ " while preparing to uninstall. The service in this application will be uninstalled in a random order");

			return Arrays.asList(pus);
		}

		TopologicalOrderIterator<ProcessingUnit, DefaultEdge> iterator = new TopologicalOrderIterator<ProcessingUnit, DefaultEdge>(
				graph);

		List<ProcessingUnit> orderedList = new ArrayList<ProcessingUnit>();
		while (iterator.hasNext()) {
			ProcessingUnit nextPU = iterator.next();
			if (!orderedList.contains(nextPU)) {
				orderedList.add(nextPU);
			}
		}
		// Collections.reverse(orderedList);
		return orderedList;

	}

	@RequestMapping(value = "applications/{applicationName}", method = RequestMethod.POST)
	public @ResponseBody
	Object deployApplication(@PathVariable final String applicationName,
			@RequestParam(value = "file", required = true) final MultipartFile srcFile) throws IOException,
			PackagingException, DSLException {
		final File applicationFile = copyMultipartFileToLocalFile(srcFile);
		Object returnObject = doDeployApplication(applicationName, applicationFile);
		applicationFile.delete();
		return returnObject;
	}

	private List<Service> createServiceDependencyOrder(com.gigaspaces.cloudify.dsl.Application application) {
		DirectedGraph<Service, DefaultEdge> graph = new DefaultDirectedGraph<Service, DefaultEdge>(DefaultEdge.class);

		Map<String, Service> servicesByName = new HashMap<String, Service>();

		List<Service> services = application.getServices();

		for (Service service : services) {
			// keep a map of names to services
			servicesByName.put(service.getName(), service);
			// and create the graph node
			graph.addVertex(service);
		}

		for (Service service : services) {
			List<String> dependsList = service.getDependsOn();
			if (dependsList != null) {
				for (String depends : dependsList) {
					Service dependency = servicesByName.get(depends);
					if (dependency == null) {
						throw new IllegalArgumentException("Dependency '" + depends + "' of service: "
								+ service.getName() + " was not found");
					}

					graph.addEdge(dependency, service);
				}
			}
		}

		CycleDetector<Service, DefaultEdge> cycleDetector = new CycleDetector<Service, DefaultEdge>(graph);
		boolean containsCycle = cycleDetector.detectCycles();

		if (containsCycle) {
			Set<Service> servicesInCycle = cycleDetector.findCycles();
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Service service : servicesInCycle) {
				if (!first) {
					sb.append(",");
				} else {
					first = false;
				}
				sb.append(service.getName());
			}

			String cycleString = sb.toString();

			// NOTE: This is not exactly how the cycle detector works. The
			// returned list is the vertex set for the subgraph of all cycles.
			// So if there are multiple cycles, the list will contain the
			// members of all of them.
			throw new IllegalArgumentException("The dependency graph of application: " + application.getName()
					+ " contains one or more cycles. The services that form a cycle are part of the following group: "
					+ cycleString);
		}

		TopologicalOrderIterator<Service, DefaultEdge> iterator = new TopologicalOrderIterator<Service, DefaultEdge>(
				graph);

		List<Service> orderedList = new ArrayList<Service>();
		while (iterator.hasNext()) {
			orderedList.add(iterator.next());
		}
		return orderedList;

	}

	private Object doDeployApplication(final String applicationName, final File applicationFile) throws IOException,
			PackagingException, DSLException {
		DSLApplicationCompilatioResult result = ServiceReader.getApplicationFromFile(applicationFile);
		final List<Service> services = createServiceDependencyOrder(result.getApplication());

		ApplicationInstallerRunnable installer = new ApplicationInstallerRunnable(this, result, applicationName,
				services, this.cloud);
		this.executorService.execute(installer);

		String[] serviceOrder = new String[services.size()];
		for (int i = 0; i < serviceOrder.length; i++) {
			serviceOrder[i] = services.get(i).getName();
		}

		Map<String, Object> retval = successStatus(Arrays.toString(serviceOrder));

		return retval;

	}

	private File copyMultipartFileToLocalFile(final MultipartFile srcFile) throws IOException {
		File tempFolder = File.createTempFile("GS__", srcFile.getOriginalFilename());
		tempFolder.delete();
		tempFolder.mkdirs();
		File tempFile = new File(tempFolder.getAbsolutePath(), srcFile.getOriginalFilename());
		srcFile.transferTo(tempFile);
		tempFile.deleteOnExit();
		return tempFile;
	}

	private void doDeploy(final String applicationName, final String serviceName, final String templateName,
			final String zone, final File serviceFile, final Properties contextProperties, Service service)
			throws AdminException, TimeoutException {
		int numberOfInstances = service.getNumInstances();
		if (numberOfInstances > 1) {
			logger.info("Deploying service " + serviceName + " with " + numberOfInstances + " instances.");
		} else {
			logger.info("Deploying service " + serviceName
					+ " with a recipe that does not define number of instances. Assuming number of instances is 1");
			numberOfInstances = 1;
		}
		doDeploy(applicationName, serviceName, templateName, zone, serviceFile, contextProperties, numberOfInstances);
	}

	private void doDeploy(final String applicationName, final String serviceName, final String templateName,
			final String zone, final File serviceFile, final Properties contextProperties) throws AdminException,
			TimeoutException {
		int numberOfInstances = 1;
		logger.info("Deploying service " + serviceName + " without a recipe. Assuming number of instances is 1");
		doDeploy(applicationName, serviceName, templateName, zone, serviceFile, contextProperties, numberOfInstances);
	}

	private void doDeploy(final String applicationName, final String serviceName, final String templateName,
			final String zone, final File serviceFile, final Properties contextProperties, int numberOfInstances)
			throws AdminException, TimeoutException {

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
			if (isLocalCloud()) {

				// Manual scale by number of instances
				deployment.scale(new ManualCapacityScaleConfigurer().memoryCapacity(
						externalProcessMemoryInMB * numberOfInstances, MemoryUnit.MEGABYTES).create());
			} else {
				// Eager scale (1 container per machine per PU)
				deployment.scale(new EagerScaleConfigurer().atMostOneContainerPerMachine().create());
			}
		} else {

			final CloudTemplate template = getComputeTemplate(cloud, templateName);

			long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(cloud, template);

			CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(cloud, template,
					cloudFileContents, templateName);
			// CloudMachineProvisioningConfig config =
			// CloudDSLToCloudMachineProvisioningConfig
			// .convert(cloud);

			String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);
			deployment.memoryCapacityPerContainer((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);

			deployment.scale(new ManualCapacityScaleConfigurer()
					.memoryCapacity((int) cloudExternalProcessMemoryInMB * numberOfInstances, MemoryUnit.MEGABYTES)
					.atMostOneContainerPerMachine().create());

		}

		// add context properties
		setContextProperties(deployment, contextProperties);

		verifyEsmExistsInCluster();
		deployAndWait(serviceName, deployment);

	}

	private static String extractLocators(Admin admin) {

		LookupLocator[] locatorsArray = admin.getLocators();
		StringBuilder locators = new StringBuilder();

		for (LookupLocator locator : locatorsArray) {
			locators.append(locator.getHost() + ":" + locator.getPort() + ",");
		}

		if (locators.length() > 0) {
			locators.setLength(locators.length() - 1);
		}

		return locators.toString();
	}

	private long calculateExternalProcessMemory(final Cloud2 cloud, final CloudTemplate template) {
		// TODO remove hardcoded number
		logger.info("Calculating external proc mem for template: " + template);
		long cloudExternalProcessMemoryInMB = template.getMachineMemoryMB()
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
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		boolean isOnlyOneAgent = agents.length == 1;
		boolean isAgentWithoutZones = agents[0].getZones().size() == 0;
		boolean isLocalCloud = isOnlyOneAgent && isAgentWithoutZones;
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
	 * Waits for a single instance of a service to become available. NOTE:
	 * currently only uses service name as processing unit name.
	 * 
	 * @param applicationName
	 *            not used.
	 * @param serviceName
	 *            the service name.
	 * @param timeout
	 *            the timeout period to wait for the processing unit, and then
	 *            the PU instance.
	 * @param timeUnit
	 *            the time unit used to wait for the processing unit, and then
	 *            the PU instance.
	 * @return true if instance is found, false if instance is not found in the
	 *         specified period.
	 */
	public boolean waitForServiceInstance(final String applicationName, final String serviceName, final long timeout,
			final TimeUnit timeUnit) {

		// this should be a very fast lookup, since the service was already
		// successfully deployed
		String absolutePUName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		ProcessingUnit pu = this.admin.getProcessingUnits().waitFor(absolutePUName, timeout, timeUnit);
		if (pu == null) {
			return false;
		}

		// ignore the time spent on PU lookup, as it should be failry short.
		return pu.waitFor(1, timeout, timeUnit);

	}

	public void deployElasticProcessingUnit(String serviceName, String applicationName, String zone, File srcFile,
			Properties propsFile, final String originalTemplateName) throws TimeoutException, PackagingException,
			IOException, AdminException, DSLException {

		String templateName;
		if (originalTemplateName == null) {
			templateName = this.defaultTemplateName;
		} else {
			templateName = originalTemplateName;
		}

		Service service = null;
		File projectDir = null;
		// Cloud cloud = null;
		if (srcFile.getName().endsWith(".zip")) {
			projectDir = ServiceReader.extractProjectFile(srcFile);
			DSLServiceCompilationResult result = ServiceReader.getServiceFromDirectory(new File(projectDir, "ext"),
					applicationName);
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
			} catch (IOException e) {
				// this may happen if a classloader is holding unto a jar file in the usmlib directory
				// the files are temp files, so it should be ok if they remain on the disk
				logger.log(Level.WARNING, "Failed to delete project files: " + e.getMessage(), e);
			}
		}
		srcFile.delete();
	}

	// TODO: add getters for service processing units in the service class that
	// does the cast automatically.
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}", method = RequestMethod.POST)
	public @ResponseBody
	Object deployElastic(@PathVariable final String applicationName, @PathVariable final String serviceName,
			@RequestParam(value = "template", required = false) final String templateName, @RequestParam(
					value = "zone", required = true) final String zone,
			@RequestParam(value = "file", required = true) final MultipartFile srcFile, @RequestParam(value = "props",
					required = true) final MultipartFile propsFile) throws TimeoutException, PackagingException,
			IOException, AdminException, DSLException {

		logger.finer("received request to deploy");
		logger.info("Deploying service with template: " + templateName);
		String actualTemplateName = templateName;

		if (cloud != null) {
			if ((templateName == null) || templateName.length() == 0) {
				if (cloud.getTemplates().size() == 0) {
					throw new IllegalStateException("Cloud configuration has no compute template defined!");
				}
				actualTemplateName = cloud.getTemplates().keySet().iterator().next();
				logger.warning("Compute Template name missing from service deployment request. Defaulting to first template: "
						+ actualTemplateName);

			}
		}

		String absolutePuName = ServiceUtils.getAbsolutePUName(applicationName, serviceName);
		final byte[] propsBytes = propsFile.getBytes();
		final Properties props = new Properties();
		final InputStream is = new ByteArrayInputStream(propsBytes);
		props.load(is);
		File dest = copyMultipartFileToLocalFile(srcFile);
		dest.deleteOnExit();
		deployElasticProcessingUnit(absolutePuName, applicationName, zone, dest, props, actualTemplateName);

		return successStatus();
	}

	private File getJarFileFromDir(File serviceFileOrDir, File serviceDirectory, String jarName) throws IOException {
		if (!serviceFileOrDir.isAbsolute()) {
			serviceFileOrDir = new File(serviceDirectory, serviceFileOrDir.getPath());
		}
		File destJar = new File(serviceDirectory.getParent(), jarName + ".jar");
		FileUtils.deleteQuietly(destJar);
		if (serviceFileOrDir.isDirectory()) {
			File jarFile = File.createTempFile(serviceFileOrDir.getName(), ".jar");
			ZipUtils.zip(serviceFileOrDir, jarFile);
			//rename the jar so would appear as 'Absolute pu name' in the deploy folder.
			jarFile.renameTo(destJar);
			jarFile.deleteOnExit();
			return destJar;
		} else if (serviceFileOrDir.isFile()) {
			//rename the jar so would appear as 'Absolute pu name' in the deploy folder.
			serviceFileOrDir.renameTo(destJar);
			return destJar;
		}

		throw new FileNotFoundException("The file " + serviceFileOrDir + " was not found in the service folder");
	}

	private CloudTemplate getComputeTemplate(final Cloud2 cloud, final String templateName) {
		if (templateName == null) {
			Entry<String, CloudTemplate> entry = cloud.getTemplates().entrySet().iterator().next();

			logger.warning("Service does not specify template name! Defaulting to template: " + entry.getKey());
			return entry.getValue();
		} else {
			CloudTemplate template = cloud.getTemplates().get(templateName);
			if (template == null) {
				throw new IllegalArgumentException("Could not find compute template: " + templateName);
			}
			return template;
		}
	}

	// TODO: consider adding MemoryUnits to DSL
	// TODO: add memory unit to names
	private void deployDataGrid(String applicationName, String serviceName, String zone, File srcFile,
			Properties contextProperties, DataGrid dataGridConfig, final String templateName) throws AdminException,
			TimeoutException, DSLException {

		final int containerMemoryInMB = dataGridConfig.getSla().getMemoryCapacityPerContainer();
		final int maxMemoryInMB = dataGridConfig.getSla().getMaxMemoryCapacity();
		final int reservedMemoryCapacityPerMachineInMB = 256;

		logger.finer("received request to install datagrid");

		ElasticSpaceDeployment deployment = new ElasticSpaceDeployment(serviceName)
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

			CloudTemplate template = getComputeTemplate(cloud, templateName);

			validateAndPrepareStatefulSla(serviceName, dataGridConfig.getSla(), cloud, template);

			long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(cloud, template);

			CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(cloud, template,
					cloudFileContents, templateName);
			// CloudMachineProvisioningConfig config =
			// CloudDSLToCloudMachineProvisioningConfig.convert(cloud);

			String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);
			deployment.memoryCapacityPerContainer((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);

			deployment.scale(new ManualCapacityScaleConfigurer()
					.memoryCapacity((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES)
					.atMostOneContainerPerMachine().create());
		}

		deployAndWait(serviceName, deployment);

	}

	private void setContextProperties(ElasticDeploymentTopology deployment, final Properties contextProperties) {
		Set<Entry<Object, Object>> contextPropsEntries = contextProperties.entrySet();
		for (Entry<Object, Object> entry : contextPropsEntries) {
			deployment.addContextProperty((String) entry.getKey(), (String) entry.getValue());
		}
	}

	private void setSharedMachineProvisioning(ElasticDeploymentTopology deployment, String zone,
			int reservedMemoryCapacityPerMachineInMB) {
		// All PUs on this role share the same machine. Machines
		// are identified by zone.
		deployment.sharedMachineProvisioning(SHARED_ISOLATION_ID,
				new DiscoveredMachineProvisioningConfigurer().addGridServiceAgentZone(zone)
						.reservedMemoryCapacityPerMachine(reservedMemoryCapacityPerMachineInMB, MemoryUnit.MEGABYTES)
						.create());
	}

	private void setDedicatedMachineProvisioning(ElasticDeploymentTopology deployment,
			ElasticMachineProvisioningConfig config) {
		deployment.dedicatedMachineProvisioning(config);
	}

	private void deployStatelessProcessingUnitAndWait(String applicationName, String serviceName, String zone,
			File extractedServiceFolder, final Properties contextProperties, StatelessProcessingUnit puConfig,
			final String templateName, int numberOfInstances) throws IOException, AdminException, TimeoutException,
			DSLException {

		File jarFile = getJarFileFromDir(new File(puConfig.getBinaries()), extractedServiceFolder, serviceName);
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
			CloudTemplate template = getComputeTemplate(cloud, templateName);
			validateAndPrepareStatelessSla(puConfig.getSla(), cloud, template);
			long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(cloud, template);

			CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(cloud, template,
					cloudFileContents, templateName);
			// CloudMachineProvisioningConfig config =
			// CloudDSLToCloudMachineProvisioningConfig.convert(cloud);

			String locators = extractLocators(admin);
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
	private void deployStatefulProcessingUnit(String applicationName, String serviceName, String zone,
			File extractedServiceFolder, final Properties contextProperties, StatefulProcessingUnit puConfig,
			final String templateName) throws IOException, AdminException, TimeoutException, DSLException {
		
		File jarFile = getJarFileFromDir(new File(puConfig.getBinaries()), extractedServiceFolder, serviceName);
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

			CloudTemplate template = getComputeTemplate(cloud, templateName);

			validateAndPrepareStatefulSla(serviceName, puConfig.getSla(), cloud, template);

			CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(cloud, template,
					cloudFileContents, templateName);

			String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);

			deployment.scale(new ManualCapacityScaleConfigurer()
					.memoryCapacity(puConfig.getSla().getMemoryCapacity(), MemoryUnit.MEGABYTES)
					.atMostOneContainerPerMachine().create());

		}

		deployAndWait(serviceName, deployment);
		jarFile.delete();

	}

	private void validateAndPrepareStatefulSla(String serviceName, Sla sla, Cloud2 cloud, CloudTemplate template)
			throws DSLException {

		validateMemoryCapacityPerContainer(sla, cloud, template);

		if (sla.getMaxMemoryCapacity() != null && sla.getMemoryCapacity() != null
				&& sla.getMaxMemoryCapacity() < sla.getMemoryCapacity()) {

			throw new DSLException("Max memory capacity is smaller than the memory capacity."
					+ sla.getMaxMemoryCapacity() + " < " + sla.getMemoryCapacity());
		}

		int minimumNumberOfContainers = sla.getHighlyAvailable() ? 2 : 1;
		int minMemoryInMB = minimumNumberOfContainers * sla.getMemoryCapacityPerContainer();

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

	private void validateAndPrepareStatelessSla(Sla sla, Cloud2 cloud, CloudTemplate template) throws DSLException {

		validateMemoryCapacityPerContainer(sla, cloud, template);

		if (sla.getMemoryCapacity() != null) {
			throw new DSLException("memoryCapacity SLA is not supported in this service");
		}

		if (sla.getMaxMemoryCapacity() != null) {
			throw new DSLException("maxMemoryCapacity SLA is not supported in this service");
		}

	}

	private void validateMemoryCapacityPerContainer(Sla sla, Cloud2 cloud, CloudTemplate template) throws DSLException {
		if (cloud == null) {
			// No cloud, must specify memory capacity per container explicitly
			if (sla.getMemoryCapacityPerContainer() == null) {
				throw new DSLException("Cannot determine memoryCapacityPerContainer SLA");
			}
		} else {
			// Assuming one container per machine then container memory =
			// machine memory
			int availableMemoryOnMachine = (int) calculateExternalProcessMemory(cloud, template);
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

	private void deployAndWait(String serviceName, ElasticStatefulProcessingUnitDeployment deployment)
			throws TimeoutException, AdminException {
		ProcessingUnit pu = getGridServiceManager().deploy(deployment, 60, TimeUnit.SECONDS);
		if (pu == null) {
			throw new TimeoutException("Timed out waiting for Service " + serviceName + " deployment.");
		}
	}
}
