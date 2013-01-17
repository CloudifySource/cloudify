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

import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_INVOKE_INSTANCE;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_APP;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_LUS;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOCATE_SERVICE;
import static org.cloudifysource.rest.ResponseConstants.FAILED_TO_LOGIN;
import static org.cloudifysource.rest.ResponseConstants.HTTP_INTERNAL_SERVER_ERROR;
import static org.cloudifysource.rest.ResponseConstants.HTTP_OK;
import static org.cloudifysource.rest.ResponseConstants.SERVICE_INSTANCE_UNAVAILABLE;
import static org.cloudifysource.rest.util.RestUtils.successStatus;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import net.jini.core.discovery.LookupLocator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.cloudifysource.dsl.ComputeDetails;
import org.cloudifysource.dsl.DataGrid;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.Sla;
import org.cloudifysource.dsl.StatefulProcessingUnit;
import org.cloudifysource.dsl.StatelessProcessingUnit;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.internal.CloudTemplateHolder;
import org.cloudifysource.dsl.internal.CloudTemplatesReader;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLApplicationCompilatioResult;
import org.cloudifysource.dsl.internal.DSLException;
import org.cloudifysource.dsl.internal.DSLReader;
import org.cloudifysource.dsl.internal.DSLServiceCompilationResult;
import org.cloudifysource.dsl.internal.DSLUtils;
import org.cloudifysource.dsl.internal.ServiceReader;
import org.cloudifysource.dsl.internal.packaging.CloudConfigurationHolder;
import org.cloudifysource.dsl.internal.packaging.PackagingException;
import org.cloudifysource.dsl.internal.packaging.ZipUtils;
import org.cloudifysource.dsl.internal.tools.ServiceDetailsHelper;
import org.cloudifysource.dsl.rest.ApplicationDescription;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.esc.driver.provisioning.CloudifyMachineProvisioningConfig;
import org.cloudifysource.esc.util.IsolationUtils;
import org.cloudifysource.rest.ResponseConstants;
import org.cloudifysource.rest.security.CloudifyAuthorizationDetails;
import org.cloudifysource.rest.security.CustomPermissionEvaluator;
import org.cloudifysource.rest.util.ApplicationDescriptionFactory;
import org.cloudifysource.rest.util.ApplicationInstallerRunnable;
import org.cloudifysource.rest.util.LifecycleEventsContainer;
import org.cloudifysource.rest.util.RestPollingRunnable;
import org.cloudifysource.rest.util.RestUtils;
import org.cloudifysource.restDoclet.annotations.JsonRequestExample;
import org.cloudifysource.restDoclet.annotations.JsonResponseExample;
import org.cloudifysource.restDoclet.annotations.PossibleResponseStatus;
import org.cloudifysource.restDoclet.annotations.PossibleResponseStatuses;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.restclient.RestException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminException;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.Applications;
import org.openspaces.admin.dump.DumpResult;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.internal.admin.InternalAdmin;
import org.openspaces.admin.internal.pu.DefaultProcessingUnitInstance;
import org.openspaces.admin.internal.pu.InternalProcessingUnitInstance;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitAlreadyDeployedException;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.ElasticStatelessProcessingUnitDeployment;
import org.openspaces.admin.pu.elastic.config.AutomaticCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfig;
import org.openspaces.admin.pu.elastic.config.DiscoveredMachineProvisioningConfigurer;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfig;
import org.openspaces.admin.pu.elastic.config.ManualCapacityScaleConfigurer;
import org.openspaces.admin.pu.elastic.topology.ElasticDeploymentTopology;
import org.openspaces.admin.space.ElasticSpaceDeployment;
import org.openspaces.admin.zone.config.AtLeastOneZoneConfig;
import org.openspaces.admin.zone.config.AtLeastOneZoneConfigurer;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.context.GigaSpaceContext;
import org.openspaces.core.util.MemoryUnit;
import org.openspaces.pu.service.CustomServiceDetails;
import org.openspaces.pu.service.ServiceDetails;
import org.openspaces.pu.service.ServiceDetailsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

import com.gigaspaces.internal.dump.pu.ProcessingUnitsDumpProcessor;
import com.gigaspaces.log.LastNLogEntryMatcher;
import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatchers;
import com.j_spaces.kernel.PlatformVersion;

/**
 * @author rafi, barakm, adaml, noak
 * @since 2.0.0
 */
@Controller
@RequestMapping("/service")
public class ServiceController implements ServiceDetailsProvider {

	private static final int MAX_NUMBER_OF_LINES_TO_TAIL_ALLOWED = 1000;
	private static final int DEFAULT_TIME_EXTENTION_POLLING_TASK = 5;
	private static final int TIMEOUT_WAITING_FOR_GSM_SEC = 10;
	private static final int THREAD_POOL_SIZE = 20;
	private static final int PU_DISCOVERY_TIMEOUT_SEC = 8;
	private static final int LIFECYCLE_EVENT_POLLING_INTERVAL_SEC = 4;
	private static final long LIFECYCLE_EVENT_CLEANUP_INTERVAL_SEC = 60;
	private static final long MINIMAL_POLLING_TASK_EXPIRATION = 5 * 60 * 1000;

	private static final String LOCALCLOUD_ZONE = "localcloud";
	private static final String SHARED_ISOLATION_ID = "public";

	private final Map<UUID, RestPollingRunnable> lifecyclePollingThreadContainer =
			new ConcurrentHashMap<UUID, RestPollingRunnable>();
	private final ExecutorService serviceUndeployExecutor = Executors
			.newFixedThreadPool(10);
	private static final long TEN_K = 10 * FileUtils.ONE_KB;
	private static final String FAILED_TO_ADD_TEMPLATES_KEY = "failed to add templates";
	private static final String SUCCESSFULLY_ADDED_TEMPLATES_KEY = "successfully added templates";
	private static final String SECURITY_PROFILE = System.getenv(CloudifyConstants.SPRING_ACTIVE_PROFILE_ENV_VAR);
	private static final boolean IS_SECURE_CONNECTION = 
			CloudifyConstants.SPRING_PROFILE_SECURE.equalsIgnoreCase(SECURITY_PROFILE);

	/**
	 * A set containing all of the executed lifecycle events. used to avoid
	 * duplicate prints.
	 */
	private final Set<String> eventsSet = new HashSet<String>();

	@Autowired(required = true)
	private Admin admin;
	@Autowired(required = false)
	private CustomPermissionEvaluator permissionEvaluator;
	@GigaSpaceContext(name = "gigaSpace")
	private GigaSpace gigaSpace;

	private Cloud cloud = null;
	private CloudConfigurationHolder cloudConfigurationHolder;
	private File cloudConfigurationDir;
	private CloudTemplate managementTemplate;
	private AtomicInteger lastTemplateFileNum = new AtomicInteger(0);

	private static final Logger logger = Logger
			.getLogger(ServiceController.class.getName());
	private static final long DEFAULT_DUMP_FILE_SIZE_LIMIT = 5 * 1024 * 1024;

	private static final String DEFAULT_DUMP_PROCESSORS = "summary, network, thread, log";
	
	// private static final String[] DEFAULT_DUMP_PROCESSORS = new String[] {
	// "summary", "network", "thread", "log", "processingUnits;"
	// };
	private String defaultTemplateName;

	@Value("${restful.temporaryFolder}")
	private String temporaryFolder;

	/**
	 * Initializing the cloud configuration. Executed by Spring after the object
	 * is instantiated and the dependencies injected.
	 */
	@PostConstruct
	public void init() {
		logger.info("Initializing service controller cloud configuration");
		this.cloud = readCloud();
		if (cloud != null) {
			initCloudTemplates();
			if (this.cloud.getTemplates().isEmpty()) {
				throw new IllegalArgumentException(
						"No templates defined in cloud configuration!");
			}
			this.defaultTemplateName = this.cloud.getTemplates().keySet()
					.iterator().next();
			logger.info("Setting default template name to: "
					+ defaultTemplateName
					+ ". This template will be used for services that do not specify an explicit template");

			this.managementTemplate = this.cloud.getTemplates().get(
					this.cloud.getConfiguration()
					.getManagementMachineTemplate());
		} else {
			logger.info("Service Controller is running in local cloud mode");
		}

		/**
		 * Sets the folder used for temporary files. The value can be set in the
		 * configuration file ("config.properties"), otherwise the system's
		 * default setting will apply.
		 */
		try {
			if (StringUtils.isBlank(temporaryFolder)) {
				temporaryFolder = getTempFolderPath();
			}
		} catch (final IOException e) {
			logger.log(Level.SEVERE,
					"ServiceController failed to locate temp directory", e);
			throw new IllegalStateException(
					"ServiceController failed to locate temp directory", e);
		}

		startLifecycleLogsCleanupTask();
	}

	private void startLifecycleLogsCleanupTask() {
		this.lifecycleEventsCleaner.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {
				if (lifecyclePollingThreadContainer != null) {
					final Iterator<Entry<UUID, RestPollingRunnable>> it = lifecyclePollingThreadContainer
							.entrySet().iterator();
					while (it.hasNext()) {
						final Map.Entry<UUID, RestPollingRunnable> entry = it
								.next();
						final RestPollingRunnable restPollingRunnable = entry
								.getValue();
						if (restPollingRunnable.isDone()) {
							logger.log(Level.INFO, "Polling Task with UUID "
									+ entry.getKey().toString()
									+ " has expired");
							it.remove();
						}
					}
				}

			}
		}, 0, LIFECYCLE_EVENT_CLEANUP_INTERVAL_SEC, TimeUnit.MINUTES);
	}

	/**
	 * terminate all running threads.
	 */
	@PreDestroy
	public void destroy() {
		this.executorService.shutdownNow();
		this.scheduledExecutor.shutdownNow();
		this.lifecycleEventsCleaner.shutdownNow();
	}

	/**
	 * Get the dump of all the machines.
	 * 
	 * @param processors
	 *            The list of processors to be used.
	 * @param fileSizeLimit
	 *            .
	 * @return A map contains byte array of the dump file for each machine.
	 * @throws IOException .
	 * @throws RestErrorException
	 *             Machine not found, machine dump generation failed, dump file
	 *             is too large.
	 */
	@JsonRequestExample(requestBody = "{\"fileSizeLimit\" : 50000000, \"processors\" : \"summary, thread, log\"}")
	@JsonResponseExample(status = "success", responseBody = "{\"192.168.2.100\":\"&ltbyte array of the dump file&gt;\""
			+ ", \"192.168.2.200\":\"&ltbyte array of the dump file&gt;\""
			+ ", \"192.168.2.300\":\"&ltbyte array of the dump file&gt;\"}")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description =
			ResponseConstants.MACHINE_NOT_FOUND),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description =
			ResponseConstants.DUMP_FILE_TOO_LARGE),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "Failed to generate dump"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "IOException") })
	@RequestMapping(value = "/dump/machines", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CLOUDADMINS')")
	@ResponseBody
	public Map<String, Object> getMachineDumpFile(
			@RequestParam(defaultValue = DEFAULT_DUMP_PROCESSORS) final String processors,
			@RequestParam(defaultValue = "" + DEFAULT_DUMP_FILE_SIZE_LIMIT) final long fileSizeLimit)
					throws IOException, RestErrorException {
		return getMachineDumpFile(null, processors, fileSizeLimit);
	}

	/**
	 * Get the dump of a given machine, by its ip.
	 * 
	 * @param ip
	 *            .
	 * @param processors
	 *            The list of processors to be used.
	 * @param fileSizeLimit
	 *            .
	 * @return A byte array of the dump file in case ip is not null and a map
	 *         contains byte array of the dump file for each machine otherwise.
	 * @throws IOException .
	 * @throws RestErrorException .
	 * 
	 */
	@JsonRequestExample(requestBody = "{\"fileSizeLimit\" : 50000000, \"processors\" : \"summary, network, log\"}")
	@JsonResponseExample(status = "success", responseBody =
			"{\"&ltmachine's ip&gt;\":\"&ltbyte array of the dump file&gt;\"}")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description =
			ResponseConstants.MACHINE_NOT_FOUND),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description =
			ResponseConstants.DUMP_FILE_TOO_LARGE),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "Failed to generate dump"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "IOException") })
	@RequestMapping(value = "/dump/machine/{ip}/", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CLOUDADMINS')")
	@ResponseBody
	public Map<String, Object> getMachineDumpFile(
			@PathVariable final String ip,
			@RequestParam(defaultValue = DEFAULT_DUMP_PROCESSORS) final String processors,
			@RequestParam(defaultValue = "" + DEFAULT_DUMP_FILE_SIZE_LIMIT) final long fileSizeLimit)
					throws IOException, RestErrorException {
		// check for non-default processors
		final String[] actualProcessors = getProcessorsFromRequest(processors);

		try {
			if (ip != null && ip.length() > 0) {
				// first find the relevant agent
				Machine machine = this.admin.getMachines().getHostsByAddress()
						.get(ip);
				if (machine == null) {
					machine = this.admin.getMachines().getHostsByName().get(ip);
					throw new RestErrorException(
							ResponseConstants.MACHINE_NOT_FOUND, ip);
				}
				final byte[] dumpBytes = generateMachineDumpData(fileSizeLimit,
						machine, actualProcessors);

				return successStatus(dumpBytes);

			}
			long totalSize = 0;
			final Iterator<Machine> iterator = this.admin.getMachines()
					.iterator();
			final Map<String, Object> map = new HashMap<String, Object>();
			while (iterator.hasNext()) {
				final Machine machine = iterator.next();

				final byte[] dumpBytes = generateMachineDumpData(fileSizeLimit,
						machine, actualProcessors);
				totalSize += dumpBytes.length;
				if (totalSize > fileSizeLimit) {
					throw new RestServiceException(
							ResponseConstants.DUMP_FILE_TOO_LARGE,
							Long.toString(dumpBytes.length),
							Long.toString(totalSize));
				}
				map.put(machine.getHostAddress(), dumpBytes);
			}
			return successStatus(map);
		} catch (final RestServiceException e) {
			throw new RestErrorException(e.getMessageName(), e.getParams());
		}

	}

	/**
	 * Get the dump of all the processing units.
	 * 
	 * @param fileSizeLimit
	 *            .
	 * @return the dump of all the processing units
	 * @throws IOException .
	 * @throws RestErrorException
	 *             Machine not found, dump file is too large, machine dump
	 *             generation failed.
	 */
	@JsonRequestExample(requestBody = "{\"fileSizeLimit\" : 50000000}")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description =
			ResponseConstants.DUMP_FILE_TOO_LARGE),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "Failed to generate dump"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "IOException") })
	@RequestMapping(value = "/dump/processing-units/", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CLOUDADMINS')")
	@ResponseBody
	public Map<String, Object> getPUDumpFile(@RequestParam(defaultValue = ""
			+ DEFAULT_DUMP_FILE_SIZE_LIMIT) final long fileSizeLimit)
					throws IOException, RestErrorException {

		// if ((name == null) || (name.isEmpty())) {
		// throw new IllegalArgumentException("PU Name is missing");
		// }
		//
		// // first find the relevant PU
		// ProcessingUnit pu =
		// this.admin.getProcessingUnits().getNames().get(name);
		// if (pu == null) {
		// throw new RestErrorException("dump_pu_not_found", name);
		// }

		final DumpResult dump = admin.generateDump("Rest Service user request",
				null, ProcessingUnitsDumpProcessor.NAME);
		byte[] data;
		try {
			data = getDumpRawData(dump, fileSizeLimit);
			return successStatus(data);

		} catch (final RestServiceException e) {
			throw new RestErrorException(e.getMessageName(), e.getParams());
		}

	}

	private String[] getProcessorsFromRequest(final String processors) {
		final String[] parts = processors.split(",");

		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}

		return parts;
	}

	private byte[] generateMachineDumpData(final long fileSizeLimit,
			final Machine machine, final String[] actualProcessors)
					throws IOException, RestServiceException {
		// generator the dump
		final DumpResult dump = machine.generateDump("Rest_API", null,
				actualProcessors);

		final byte[] data = getDumpRawData(dump, fileSizeLimit);
		return data;

	}

	private byte[] getDumpRawData(final DumpResult dump,
			final long fileSizeLimit) throws IOException, RestServiceException {
		final File target = File.createTempFile("dump", ".zip", new File(
				this.temporaryFolder));
		target.deleteOnExit();

		dump.download(target, null);

		try {
			// check for maximum file size limit
			long actualFileSizeLimit = DEFAULT_DUMP_FILE_SIZE_LIMIT;
			if (fileSizeLimit != 0) {
				actualFileSizeLimit = fileSizeLimit;
			}

			if (target.length() >= actualFileSizeLimit) {
				throw new RestServiceException(
						ResponseConstants.DUMP_FILE_TOO_LARGE,
						Long.toString(target.length()),
						Long.toString(actualFileSizeLimit));
			}

			// load file contents into memory
			final byte[] dumpBytes = FileUtils.readFileToByteArray(target);
			return dumpBytes;

		} finally {
			final boolean tempFileDeleteResult = target.delete();
			if (!tempFileDeleteResult) {
				logger.warning("Failed to download temporary dump file: "
						+ target);
			}

		}

	}

	private CloudConfigurationHolder getCloudConfigurationFromManagementSpace() {
		logger.info("Waiting for cloud configuration to become available in management space");
		final CloudConfigurationHolder config = gigaSpace.read(
				new CloudConfigurationHolder(), 1000 * 60);
		if (config == null) {

			logger.warning("Could not find the expected Cloud Configuration Holder in Management space!"
					+ " Defaulting to local cloud!");
			return null;
		}
		return config;
	}

	private Cloud readCloud() {
		logger.info("Loading cloud configuration");

		cloudConfigurationHolder = getCloudConfigurationFromManagementSpace();
		logger.info("Cloud Configuration: " + cloudConfigurationHolder);
		String cloudConfigurationFilePath = cloudConfigurationHolder.getCloudConfigurationFilePath();
		if (cloudConfigurationFilePath == null) {
			// must be local cloud or azure
			return null;

		}
		Cloud cloudConfiguration = null;
		try {
			File cloudConfigurationFile = new File(cloudConfigurationFilePath);
			cloudConfigurationDir = cloudConfigurationFile.getParentFile();
			cloudConfiguration = ServiceReader.readCloud(cloudConfigurationFile);
		} catch (final DSLException e) {
			throw new IllegalArgumentException(
					"Failed to read cloud configuration file: "
							+ cloudConfigurationHolder + ". Error was: "
							+ e.getMessage(), e);
		} catch (final IOException e) {
			throw new IllegalArgumentException(
					"Failed to read cloud configuration file: "
							+ cloudConfigurationHolder + ". Error was: "
							+ e.getMessage(), e);
		}

		logger.info("Successfully loaded cloud configuration file from management space");
		return cloudConfiguration;
	}

	private void initCloudTemplates() {
		File additionalTemplatesFolder = new File(cloudConfigurationDir,
				CloudifyConstants.ADDITIONAL_TEMPLATES_FOLDER_NAME);
		logger.info("initCloudTemplates - Adding templates from folder: "
				+ additionalTemplatesFolder.getAbsolutePath());
		if (!additionalTemplatesFolder.exists()) {
			logger.info("initCloudTemplates - no templates to add from folder: "
					+ additionalTemplatesFolder.getAbsolutePath());
			return;
		}
		File[] listFiles = additionalTemplatesFolder.listFiles();
		CloudTemplatesReader reader = new CloudTemplatesReader();
		List<CloudTemplate> addedTemplates = reader.addAdditionalTemplates(cloud, listFiles);
		logger.info("initCloudTemplates - Added the following templates: " + addedTemplates);
		lastTemplateFileNum.addAndGet(listFiles.length);

	}

	private final ScheduledExecutorService lifecycleEventsCleaner = Executors
			.newScheduledThreadPool(1, new ThreadFactory() {
				private final AtomicInteger threadNumber = new AtomicInteger(1);

				@Override
				public Thread newThread(final Runnable r) {
					final Thread thread = new Thread(r,
							"LifecycleEventsPollingExecutor-"
									+ threadNumber.getAndIncrement());
					thread.setDaemon(true);
					return thread;
				}
			});

	private final ScheduledExecutorService scheduledExecutor = Executors
			.newScheduledThreadPool(10, new ThreadFactory() {
				private final AtomicInteger threadNumber = new AtomicInteger(1);

				@Override
				public Thread newThread(final Runnable r) {
					final Thread thread = new Thread(r,
							"LifecycleEventsPollingExecutor-"
									+ threadNumber.getAndIncrement());
					thread.setDaemon(true);
					return thread;
				}
			});

	// Set up a small thread pool with daemon threads.
	private final ExecutorService executorService = Executors
			.newFixedThreadPool(THREAD_POOL_SIZE, new ThreadFactory() {
				private final AtomicInteger threadNumber = new AtomicInteger(1);

				@Override
				public Thread newThread(final Runnable r) {
					final Thread thread = new Thread(r,
							"ServiceControllerExecutor-"
									+ threadNumber.getAndIncrement());
					thread.setDaemon(true);
					return thread;
				}
			});

	/**
	 * Tests whether the restful service is able to locate the service grid
	 * using the admin API.
	 * <p>
	 * The admin API searches for a LUS (Lookup Service) according to the lookup
	 * groups/locators defined.
	 * 
	 * @return - Map<String, Object> object containing the test results.
	 * @throws RestErrorException
	 *             When lookup service not found.
	 */
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = FAILED_TO_LOCATE_LUS) })
	@JsonResponseExample(status = "error", responseBody = "{\"error\":\"failed_to_locate_lookup_service\","
			+ " \"error_args\":[[\"localcloud\"],[\"jini://127.0.0.1:4172/\"]]}",
			comments = "response status is success if the restful service located the service grid"
					+ ", otherwise it is error and the response's body will contain error description"
					+ ", the groups and locators.")
	@RequestMapping(value = "/testrest", method = RequestMethod.GET)
	@ResponseBody
	public Object test() throws RestErrorException {

		if (admin.getLookupServices().getSize() > 0) {
			return successStatus();
		}
		final String groups = Arrays.toString(admin.getGroups());
		final String locators = Arrays.toString(admin.getLocators());
		throw new RestErrorException(FAILED_TO_LOCATE_LUS, groups, locators);
	}

	/**
	 * Tests whether the authentication was successful.
	 * 
	 * @return - Map<String, Object> object containing the login results.
	 * @throws RestErrorException
	 *             When login fails.
	 */
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "Login failed") })
	@JsonResponseExample(status = "error", responseBody = "{\"error\":\"Login failed\","
			+ " \"error_args\":[[\"localcloud\"],[\"jini://127.0.0.1:4172/\"]]}",
			comments = "response status is success if the user authentication was successful"
					+ ", otherwise it is error and the response's body will contain the error description.")
	@RequestMapping(value = "/testlogin", method = RequestMethod.GET)
	@ResponseBody
	public Object testLogin() throws RestErrorException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new RestErrorException(FAILED_TO_LOGIN);
		}

		logger.finer("User " + authentication.getName() + " logged in.");
		return successStatus();
	}
	
	/**
	 * Verifies the authenticated user has role ROLE_CLOUDADMIINS.
	 * 
	 * @return - Map<String, Object> object containing the test results.
	 * @throws RestErrorException
	 *             When the calling user does not have role ROLE_CLOUDADMIINS.
	 */
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "User is not a CloudAdmin") })
	@JsonResponseExample(status = "error", responseBody = "{\"error\":\"User is not a CloudAdmin\","
			+ " \"error_args\":[[\"localcloud\"],[\"jini://127.0.0.1:4172/\"]]}",
			comments = "response status is success if the user user has role ROLE_CLOUDADMIINS"
					+ ", otherwise it is error and the response's body will contain the error description.")
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(value = "/verifyCloudAdmin", method = RequestMethod.GET)
	@ResponseBody
	public Object verifyCloudAdmin() throws RestErrorException {
		return successStatus();
	}

	/**
	 * deprecated
	 * 
	 * @deprecated
	 * @param applicationName
	 * @param srcFile
	 * @param authGroups
	 * @return the name of the pu
	 * @throws IOException
	 * @throws RestErrorException
	 *             GSM not found, service not found after deployment.
	 */
	/*
	 * @Deprecated
	 * 
	 * @RequestMapping(value = "/cloudcontroller/deploy", method =
	 * RequestMethod.POST) public @ResponseBody
	 * 
	 * @PreAuthorize(
	 * "isFullyAuthenticated() and hasPermission(#authGroups, 'deploy')")
	 * Map<String, Object> deploy(
	 * 
	 * @RequestParam(value = "applicationName", defaultValue = "default") final
	 * String applicationName,
	 * 
	 * @RequestParam(value = "file") final MultipartFile srcFile,
	 * 
	 * @RequestParam(value = "authGroups", required = false) String authGroups)
	 * throws IOException, RestErrorException {
	 * logger.finer("Deploying a service"); final File tmpfile =
	 * File.createTempFile("gs___", null); final File dest = new
	 * File(tmpfile.getParent(), srcFile.getOriginalFilename());
	 * tmpfile.delete(); srcFile.transferTo(dest);
	 * 
	 * final GridServiceManager gsm = getGsm(); if (gsm == null) { throw new
	 * RestErrorException(FAILED_TO_LOCATE_GSM); } final ProcessingUnit pu =
	 * gsm.deploy(new ProcessingUnitDeployment(dest).setContextProperty(
	 * CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME, applicationName)
	 * .setContextProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS,
	 * authGroups)); dest.delete();
	 * 
	 * if (pu == null) { throw new RestErrorException(
	 * FAILED_TO_LOCATE_SERVICE_AFTER_DEPLOYMENT, applicationName); } return
	 * successStatus(pu.getName()); }
	 */

	/**
	 * Creates and returns a list containing all of the deployed application
	 * details.
	 * 
	 * @return a list of all the deployed applications in the service grid.
	 * @throws RestErrorException .
	 */
	@JsonResponseExample(status = "success", responseBody = "[\"petclinic\", \"travel\"]",
			comments = "In the example, the deployed applications in the service grid are petclinic and travel")
	@PossibleResponseStatuses(responseStatuses = { @PossibleResponseStatus(code = HTTP_OK, description = "success") })
	@RequestMapping(value = "/applications/description", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated()")
	@PostFilter("hasPermission(filterObject, 'view')")
	@ResponseBody
	public Map<String, Object> getApplicationDescriptionsList() throws RestErrorException {

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list application descriptions");
		}

		final Applications apps = admin.getApplications();
		List<ApplicationDescription> appDescriptions = new ArrayList<ApplicationDescription>();
		ApplicationDescriptionFactory applicationDescriptionFactory = new ApplicationDescriptionFactory(admin);
		for (final Application app : apps) {
			if (!app.getName().equals(CloudifyConstants.MANAGEMENT_APPLICATION_NAME)) {
				ApplicationDescription applicationDescription = applicationDescriptionFactory
						.getApplicationDescription(app);
				appDescriptions.add(applicationDescription);
			}
		}

		return successStatus(appDescriptions);
	}

	/**
	 * Creates and returns a map containing all of the deployed service names
	 * installed under a specific application context.
	 * 
	 * @param applicationName
	 *            .
	 * @return a list of the deployed services in the service grid that were
	 *         deployed as a part of a specific application.
	 * @throws RestErrorException
	 *             When application is not found.
	 */
	@JsonResponseExample(status = "sucess", responseBody = "[\"service1\",\"service2\"]")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_app") })
	@RequestMapping(value = "/applications/{applicationName}/services/description", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated()")
	@PostFilter("hasPermission(filterObject, 'view')")
	@ResponseBody
	public Map<String, Object> getServicesDescriptionList(
			@PathVariable final String applicationName)
					throws RestErrorException {
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list applications");
		}
		final Application app = admin.getApplications().waitFor(
				applicationName, 5, TimeUnit.SECONDS);
		if (app == null) {
			throw new RestErrorException(FAILED_TO_LOCATE_APP, applicationName);
		}
		ApplicationDescriptionFactory appDescriptionFactory = new ApplicationDescriptionFactory(
				admin);
		ApplicationDescription applicationDescription = appDescriptionFactory
				.getApplicationDescription(applicationName);
		List<ApplicationDescription> applicationDescriptionList = new ArrayList<ApplicationDescription>();
		applicationDescriptionList.add(applicationDescription);
		return successStatus(applicationDescriptionList);
	}

	/**
	 * 
	 * Creates a list of all service instances in the specified application.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @return a Map containing all service instances of the specified
	 *         application
	 * @throws RestErrorException
	 *             When service is not found.
	 */
	@JsonResponseExample(status = "success", responseBody = "{\"1\":\"127.0.0.1\"}", comments = "In the example"
			+ " instance id is 1 and the HOST is 127.0.0.1")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_service") })
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/instances", method =
	RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated()")
	@ResponseBody
	public Map<String, Object> getServiceInstanceList(
			@PathVariable final String applicationName,
			@PathVariable final String serviceName) throws RestErrorException {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list instances for service "
					+ absolutePuName + " of application " + applicationName);
		}
		// todo: application awareness
		final ProcessingUnit pu = admin.getProcessingUnits().waitFor(
				absolutePuName, PU_DISCOVERY_TIMEOUT_SEC, TimeUnit.SECONDS);
		
		if (pu == null) {
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}
		
		if (permissionEvaluator != null) {
			String puAuthGroups = pu.getBeanLevelProperties().getContextProperties().
					getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			CloudifyAuthorizationDetails authDetails = new CloudifyAuthorizationDetails(authentication);
			permissionEvaluator.verifyPermission(authDetails, puAuthGroups, "view");
		}
		
		final Map<Integer, String> instanceMap = new HashMap<Integer, String>();
		final ProcessingUnitInstance[] instances = pu.getInstances();
		for (final ProcessingUnitInstance instance : instances) {
			instanceMap.put(instance.getInstanceId(), instance
					.getVirtualMachine().getMachine().getHostName());
		}

		return successStatus(instanceMap);
	}

	/**
	 * Creates and returns a map containing all of the deployed application
	 * names.
	 * 
	 * @return a list of all the deployed applications in the service grid.
	 */
	@JsonResponseExample(status = "success", responseBody = "[\"petclinic\", \"travel\"]", comments =
			"In the example, the deployed applications in the service grid are petclinic and travel")
	@PossibleResponseStatuses(responseStatuses = { @PossibleResponseStatus(code = HTTP_OK, description = "success") })
	@RequestMapping(value = "/applications", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated()")
	@PostFilter("hasPermission(filterObject, 'view')")
	@ResponseBody
	public Map<String, Object> getApplicationNamesList() {
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list applications");
		}

		final Applications apps = admin.getApplications();
		Map<String, Object> resultsMap = new HashMap<String, Object>();
		for (final Application app : apps) {
			if (!app.getName().equals(CloudifyConstants.MANAGEMENT_APPLICATION_NAME)) {
				for (ProcessingUnit pu : app.getProcessingUnits().getProcessingUnits()) {
					if (pu != null) {
						String authGroups = pu.getBeanLevelProperties().getContextProperties().
								getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
						resultsMap.put(app.getName(), authGroups);
						break;
					}
				}
			}
		}

		return successStatus(resultsMap);
	}

	/**
	 * Creates and returns a map containing all of the deployed service names
	 * installed under a specific application context.
	 * 
	 * @param applicationName
	 *            .
	 * @return a list of the deployed services in the service grid that were
	 *         deployed as a part of a specific application.
	 * @throws RestErrorException
	 *             When application is not found.
	 */
	@JsonResponseExample(status = "sucess", responseBody = "[\"service1\",\"service2\"]")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_app") })
	@RequestMapping(value = "/applications/{applicationName}/services", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated()")
	@PostFilter("hasPermission(filterObject, 'view')")
	@ResponseBody
	public Map<String, Object> getServicesList(
			@PathVariable final String applicationName)
					throws RestErrorException {
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to list applications");
		}
		final Application app = admin.getApplications().waitFor(
				applicationName, 5, TimeUnit.SECONDS);
		if (app == null) {
			throw new RestErrorException(FAILED_TO_LOCATE_APP, applicationName);
		}
		final ProcessingUnits pus = app.getProcessingUnits();
		final List<String> serviceNames = new ArrayList<String>(pus.getSize());
		for (final ProcessingUnit pu : pus) {
			serviceNames.add(ServiceUtils.getApplicationServiceName(
					pu.getName(), applicationName));
		}
		return successStatus(serviceNames);
	}

	/**
	 * 
	 * Invokes a custom command on all of the specified service instances.
	 * Custom parameters are passed as a map using the POST method and contain
	 * the command name and parameter values for the specified command.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @param beanName
	 *            deprecated.
	 * @param params
	 *            The command parameters.
	 * @return a Map containing the result of each invocation on a service
	 *         instance.
	 * @throws RestErrorException
	 *             When lookup service not found or no processing unit instance
	 *             is found for the requested service.
	 */
	@JsonRequestExample(requestBody = "{\"param1 name\":\"param1\",\"param2 name\":\"param2\"}")
	@JsonResponseExample(status = "success", responseBody =
	"{\"instance #1@127.0.0.1\":{\"Invocation_Instance_Name\":\"instance #1@127.0.0.1\""
			+ ",\"Invocation_Instance_ID\":\"1\""
			+ ",\"Invocation_Result\":\"the invocation result as specified in the service file\""
			+ ",\"Invocation_Success\":\"true\","
			+ "\"Invocation_Exception\":null,\"Invocation_Command_Name\":\"custom command name\"}}")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_service"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR,
			description = "no_processing_unit_instances_found_for_invocation") })
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/beans/{beanName}/invoke",
	method = RequestMethod.POST)
	@PreAuthorize("isFullyAuthenticated()")
	@ResponseBody
	public Map<String, Object> invoke(@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@PathVariable final String beanName,
			@RequestBody final Map<String, Object> params)
					throws RestErrorException {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to invoke bean " + beanName
					+ " of service " + absolutePuName + " of application "
					+ applicationName);
		}

		// Get the PU
		final ProcessingUnit pu = admin.getProcessingUnits().waitFor(
				absolutePuName, PU_DISCOVERY_TIMEOUT_SEC, TimeUnit.SECONDS);
		if (pu == null) {
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}

		if (permissionEvaluator != null) {
			String puAuthGroups = pu.getBeanLevelProperties().getContextProperties().
					getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			CloudifyAuthorizationDetails authDetails = new CloudifyAuthorizationDetails(authentication);
			permissionEvaluator.verifyPermission(authDetails, puAuthGroups, "deploy");
		}

		// result, mapping service instances to results
		final Map<String, Object> invocationResult = new HashMap<String, Object>();
		final ProcessingUnitInstance[] instances = pu.getInstances();

		if (instances.length == 0) {
			throw new RestErrorException(
					ResponseConstants.NO_PROCESSING_UNIT_INSTANCES_FOUND_FOR_INVOCATION,
					serviceName);
		}

		// Why a map? TODO: Use an array here instead.
		// map between service name and its future
		final Map<String, Future<Object>> futures = new HashMap<String, Future<Object>>(
				instances.length);
		for (final ProcessingUnitInstance instance : instances) {
			// key includes instance ID and host name
			final String serviceInstanceName = buildServiceInstanceName(instance);
			try {
				final Future<Object> future = ((DefaultProcessingUnitInstance) instance)
						.invoke(beanName, params);
				futures.put(serviceInstanceName, future);
			} catch (final Exception e) {
				logger.severe("Error invoking service "
						+ serviceName
						+ ":"
						+ instance.getInstanceId()
						+ " on host "
						+ instance.getVirtualMachine().getMachine().getHostName());
				invocationResult.put(serviceInstanceName,
						"pu_instance_invocation_failure");
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
				invocationResult.put(entry.getKey(),
						"Invocation failure: " + e.getMessage());
			}
		}

		return successStatus(invocationResult);
	}

	private Object postProcessInvocationResult(final Object result,
			final String instanceName) {
		Object formattedResult;
		if (result instanceof Map<?, ?>) {
			final Map<String, String> modifiedMap = new HashMap<String, String>();
			@SuppressWarnings("unchecked")
			final Set<Entry<String, Object>> entries = ((Map<String, Object>) result).entrySet();
			for (final Entry<String, Object> subEntry : entries) {

				modifiedMap.put(subEntry.getKey(),
						subEntry.getValue() == null ? null : subEntry
								.getValue().toString());
			}
			modifiedMap.put(
					CloudifyConstants.INVOCATION_RESPONSE_INSTANCE_NAME,
					instanceName);
			formattedResult = modifiedMap;
		} else {
			formattedResult = result.toString();
		}
		return formattedResult;
	}

	/**
	 * 
	 * Invokes a custom command on a specific service instance. Custom
	 * parameters are passed as a map using POST method and contain the command
	 * name and parameter values for the specified command.
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
	 *            a Map containing the result of each invocation on a service
	 *            instance.
	 * @return a Map containing the invocation result on the specified instance.
	 * @throws RestErrorException
	 *             When failed to locate service/service instance or invocation
	 *             failed.
	 */
	@JsonRequestExample(requestBody = "{\"param1 name\":\"param1\",\"param2 name\":\"param2\"}")
	@JsonResponseExample(status = "success", responseBody = "{\"Invocation_Instance_Name\":\"instance #1@127.0.0.1\""
			+ ",\"Invocation_Instance_ID\":\"1\""
			+ ",\"Invocation_Result\":\"the invocation result as specified in the service file\""
			+ ",\"Invocation_Success\":\"true\","
			+ "\"Invocation_Exception\":null,\"Invocation_Command_Name\":\"custom command name\"}")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_service"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "service_instance_unavailable"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_invoke_instance") })
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/instances"
			+ "/{instanceId}/beans/{beanName}/invoke", method = RequestMethod.POST)
	@PreAuthorize("isFullyAuthenticated()")
	@ResponseBody
	public Map<String, Object> invokeInstance(
			@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@PathVariable final int instanceId,
			@PathVariable final String beanName,
			@RequestBody final Map<String, Object> params)
					throws RestErrorException {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("received request to invoke bean " + beanName
					+ " of service " + serviceName + " of application "
					+ applicationName);
		}

		// Get PU
		final ProcessingUnit pu = admin.getProcessingUnits().waitFor(
				absolutePuName, PU_DISCOVERY_TIMEOUT_SEC, TimeUnit.SECONDS);

		if (pu == null) {
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}

		if (permissionEvaluator != null) {
			String puAuthGroups = pu.getBeanLevelProperties().getContextProperties().
					getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			CloudifyAuthorizationDetails authDetails = new CloudifyAuthorizationDetails(authentication);
			permissionEvaluator.verifyPermission(authDetails, puAuthGroups, "deploy");
		}

		// Get PUI
		final InternalProcessingUnitInstance pui = findInstanceById(pu,
				instanceId);

		if (pui == null) {
			logger.severe("Could not find service instance " + instanceId
					+ " for service " + absolutePuName);
			throw new RestErrorException(
					ResponseConstants.SERVICE_INSTANCE_UNAVAILABLE,
					applicationName, absolutePuName,
					Integer.toString(instanceId));
		}
		final String instanceName = buildServiceInstanceName(pui);
		// Invoke the remote service
		try {
			final Future<?> future = pui.invoke(beanName, params);
			final Object invocationResult = future.get();
			final Object finalResult = postProcessInvocationResult(
					invocationResult, instanceName);

			return successStatus(finalResult);
		} catch (final Exception e) {
			logger.severe("Error invoking pu instance " + absolutePuName + ":"
					+ instanceId + " on host "
					+ pui.getVirtualMachine().getMachine().getHostName());
			throw new RestErrorException(FAILED_TO_INVOKE_INSTANCE,
					absolutePuName, Integer.toString(instanceId),
					e.getMessage());
		}
	}

	private InternalProcessingUnitInstance findInstanceById(
			final ProcessingUnit pu, final int id) {
		final ProcessingUnitInstance[] instances = pu.getInstances();
		for (final ProcessingUnitInstance instance : instances) {
			if (instance.getInstanceId() == id) {
				return (InternalProcessingUnitInstance) instance;
			}
		}
		return null;
	}

	private String buildServiceInstanceName(
			final ProcessingUnitInstance instance) {
		return "instance #" + instance.getInstanceId() + "@"
				+ instance.getVirtualMachine().getMachine().getHostName();
	}

	private Map<String, Object> unavailableServiceError(final String serviceName)
			throws RestErrorException {
		// TODO: Consider telling the user he might be using the wrong
		// application name.
		throw new RestErrorException(FAILED_TO_LOCATE_SERVICE, ServiceUtils
				.getFullServiceName(serviceName).getServiceName());
	}

	private GridServiceManager getGsm(final String id) {
		if (id == null) {
			return admin.getGridServiceManagers().waitForAtLeastOne(5000,
					TimeUnit.MILLISECONDS);
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
	 * @param timeoutInMinutes
	 *            .
	 * @return success status if service was undeployed successfully, else
	 *         returns failure status.
	 * @throws RestErrorException
	 *             When failed to locate service.
	 */
	@JsonResponseExample(status = "success", responseBody =
			"{\"lifecycleEventContainerID\":\"bfae0a89-b5a0-4250-b393-6cedbf63ac76\"}")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_service") })
	@RequestMapping(value =
	"applications/{applicationName}/services/{serviceName}/timeout/{timeoutInMinutes}/undeploy",
	method = RequestMethod.DELETE)
	public @ResponseBody
	@PreAuthorize("isFullyAuthenticated()")
	Map<String, Object> undeploy(@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@PathVariable final int timeoutInMinutes) throws RestErrorException {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		final ProcessingUnit processingUnit = admin.getProcessingUnits()
				.waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
						TimeUnit.SECONDS);
		if (processingUnit == null) {
			return unavailableServiceError(absolutePuName);
		}

		if (permissionEvaluator != null) {
			String puAuthGroups = processingUnit.getBeanLevelProperties().getContextProperties().
					getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			CloudifyAuthorizationDetails authDetails = new CloudifyAuthorizationDetails(authentication);
			permissionEvaluator.verifyPermission(authDetails, puAuthGroups, "deploy");
		}

		final FutureTask<Boolean> undeployTask = new FutureTask<Boolean>(
				new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return processingUnit.undeployAndWait(timeoutInMinutes,
								TimeUnit.MINUTES);
					}

				});
		serviceUndeployExecutor.execute(undeployTask);
		final UUID lifecycleEventContainerID = startPollingForServiceUninstallLifecycleEvents(
				applicationName, serviceName, timeoutInMinutes, undeployTask);

		final Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID,
				lifecycleEventContainerID);
		return successStatus(returnMap);
	}

	private UUID startPollingForServiceUninstallLifecycleEvents(
			final String applicationName, final String serviceName,
			final int timeoutInMinutes, final FutureTask<Boolean> undeployTask) {
		RestPollingRunnable restPollingRunnable;
		final LifecycleEventsContainer lifecycleEventsContainer = new LifecycleEventsContainer();
		final UUID lifecycleEventsContainerID = UUID.randomUUID();
		lifecycleEventsContainer.setEventsSet(this.eventsSet);

		restPollingRunnable = new RestPollingRunnable(applicationName,
				timeoutInMinutes, TimeUnit.MINUTES);
		restPollingRunnable.addService(serviceName, 0);
		restPollingRunnable.setIsServiceInstall(false);
		restPollingRunnable.setAdmin(admin);
		restPollingRunnable.setLifecycleEventsContainer(lifecycleEventsContainer);
		restPollingRunnable.setIsUninstall(true);
		restPollingRunnable.setUndeployTask(undeployTask);
		restPollingRunnable.setEndTime(timeoutInMinutes, TimeUnit.MINUTES);
		final ScheduledFuture<?> scheduleWithFixedDelay = scheduledExecutor
				.scheduleWithFixedDelay(restPollingRunnable, 0,
						LIFECYCLE_EVENT_POLLING_INTERVAL_SEC, TimeUnit.SECONDS);
		restPollingRunnable.setFutureTask(scheduleWithFixedDelay);
		logger.log(Level.INFO,
				"Starting to poll for uninstall lifecycle events.");
		this.lifecyclePollingThreadContainer.put(lifecycleEventsContainerID,
				restPollingRunnable);
		logger.log(Level.INFO, "polling container UUID is "
				+ lifecycleEventsContainerID.toString());
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
	 * @throws RestErrorException
	 *             When service processing unit not found or failed to add the
	 *             instance.
	 */
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/addinstance",
			method = RequestMethod.POST)
	@PreAuthorize("isFullyAuthenticated()")
	@ResponseBody
	public Map<String, Object> addInstance(@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@RequestBody final Map<String, String> params)
					throws RestErrorException {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		final int timeout = Integer.parseInt(params.get("timeout"));
		// TODO how to set that as a context property on the new instance? is it
		// needed?
		String authGroups = params.get("authGroups");
		if (StringUtils.isBlank(authGroups)) {
			authGroups = permissionEvaluator.getUserAuthGroupsString();
		}

		final ProcessingUnit processingUnit =
				admin.getProcessingUnits().waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC, TimeUnit.SECONDS);
		if (processingUnit == null) {
			return unavailableServiceError(absolutePuName);
		}

		if (permissionEvaluator != null) {
			String puAuthGroups = processingUnit.getBeanLevelProperties().getContextProperties().
					getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			CloudifyAuthorizationDetails authDetails = new CloudifyAuthorizationDetails(authentication);
			permissionEvaluator.verifyPermission(authDetails, puAuthGroups, "deploy");
		}

		final int before = processingUnit.getNumberOfInstances();
		processingUnit.incrementInstance();
		final boolean result = processingUnit.waitFor(before + 1, timeout,
				TimeUnit.SECONDS);
		if (result) {
			return successStatus();
		}
		throw new RestErrorException(ResponseConstants.FAILED_TO_ADD_INSTANCE,
				applicationName, serviceName);
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
	 * @throws RestErrorException
	 *             When failed to locate the service or if the service instance
	 *             is not available.
	 */
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_service"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "service_instance_unavailable") })
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/instances/{instanceId}/remove",
	method = RequestMethod.DELETE)
	@PreAuthorize("isFullyAuthenticated()")
	@ResponseBody
	public Map<String, Object> removeInstance(
			@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@PathVariable final int instanceId) throws RestErrorException {
		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		// todo: application awareness
		final ProcessingUnit processingUnit = admin.getProcessingUnits()
				.waitFor(absolutePuName, PU_DISCOVERY_TIMEOUT_SEC,
						TimeUnit.SECONDS);
		if (processingUnit == null) {
			return unavailableServiceError(absolutePuName);
		}

		if (permissionEvaluator != null) {
			String puAuthGroups = processingUnit.getBeanLevelProperties().getContextProperties().
					getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			CloudifyAuthorizationDetails authDetails = new CloudifyAuthorizationDetails(authentication);
			permissionEvaluator.verifyPermission(authDetails, puAuthGroups, "deploy");
		}

		for (final ProcessingUnitInstance instance : processingUnit
				.getInstances()) {
			if (instance.getInstanceId() == instanceId) {
				instance.decrement();
				return successStatus();
			}
		}
		throw new RestErrorException(SERVICE_INSTANCE_UNAVAILABLE);
	}

	private void deployAndWait(final String serviceName,
			final ElasticSpaceDeployment deployment) throws TimeoutException {
		final ProcessingUnit pu = getGridServiceManager().deploy(deployment,
				60, TimeUnit.SECONDS);
		if (pu == null) {
			throw new TimeoutException("Timed out waiting for Service "
					+ serviceName + " deployment.");
		}
	}

	private ElasticServiceManager getESM() {
		return admin.getElasticServiceManagers().waitForAtLeastOne(5000,
				TimeUnit.MILLISECONDS);
	}

	private void deployAndWait(final String serviceName,
			final ElasticStatelessProcessingUnitDeployment deployment)
					throws TimeoutException, RestErrorException {
		try {
			final ProcessingUnit pu = getGridServiceManager().deploy(deployment,
					60, TimeUnit.SECONDS);
			if (pu == null) {
				throw new TimeoutException("Timed out waiting for Service "
						+ serviceName + " deployment.");
			}
		} catch (ProcessingUnitAlreadyDeployedException e) {
			throw new RestErrorException(CloudifyErrorMessages.SERVICE_ALREADY_INSTALLED.getName(), serviceName);
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
	 * @param response
	 *            The response object to edit, if not committed yet.
	 * @param e
	 *            The exception that occurred, from which data is read for
	 *            logging and for the response error message.
	 * @throws IOException
	 *             Reporting failure to edit the response object
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public void resolveDocumentNotFoundException(
			final HttpServletResponse response, final Exception e)
					throws IOException {

		if (response.isCommitted()) {
			logger.log(
					Level.WARNING,
					"Caught exception, but response already commited. Not sending error message based on exception",
					e);
		} else {
			String message;
			if (e instanceof AccessDeniedException || e instanceof BadCredentialsException) {
				message = "{\"status\":\"error\", \"error\":\""	
						+ CloudifyErrorMessages.NO_PERMISSION_ACCESS_DENIED.getName() + "\"}";
				logger.log(Level.INFO, e.getMessage(), e);
			} else {
				// Some sort of unhandled application exception.
				logger.log(Level.WARNING, "An unexpected error was thrown: " + e.getMessage(), e);

				Map<String, Object> restErrorMap =
						RestUtils.verboseErrorStatus(CloudifyErrorMessages.GENERAL_SERVER_ERROR.getName(),
								ExceptionUtils.getStackTrace(e), e.getMessage());
				message = new ObjectMapper().writeValueAsString(restErrorMap);
			}

			final ServletOutputStream outputStream = response.getOutputStream();
			final byte[] messageBytes = message.getBytes();
			outputStream.write(messageBytes);
		}
	}

	/**
	 * Exception handler for all of known internal server exceptions.
	 * 
	 * @param response
	 *            The response object to edit, if not committed yet.
	 * @param e
	 *            The exception that occurred, from which data is read for
	 *            logging and for the response error message.
	 * @throws IOException
	 *             Reporting failure to edit the response object
	 */
	@ExceptionHandler(RestErrorException.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public void handleServerErrors(final HttpServletResponse response,
			final RestErrorException e) throws IOException {

		if (response.isCommitted()) {
			logger.log(
					Level.WARNING,
					"Caught exception, but response already commited. Not sending error message based on exception",
					e);
		} else {
			final Map<String, Object> errorDescriptionMap = e
					.getErrorDescription();
			final String errorMap = new ObjectMapper()
			.writeValueAsString(errorDescriptionMap);
			logger.log(Level.INFO,
					"caught exception. Sending response message "
							+ (String) errorDescriptionMap.get("error"), e);
			final byte[] messageBytes = errorMap.getBytes();
			final ServletOutputStream outputStream = response.getOutputStream();
			outputStream.write(messageBytes);
		}
	}

	/**
	 * Converts a Map<String, ?> to a json String.
	 * 
	 * @param map
	 *            a map to convert to String
	 * @return a json-format String based on the given map
	 * @throws IOException
	 *             Reporting failure to read the map or convert it
	 */
	public static String mapToJson(final Map<String, ?> map) throws IOException {
		return new ObjectMapper().writeValueAsString(map);
	}

	/******************
	 * Uninstalls an application by uninstalling all of its services. Order of
	 * uninstallations is determined by the context property
	 * 'com.gs.application.services' which should exist in all service PUs.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param timeoutInMinutes
	 *            .
	 * @return Map with return value.
	 * @throws RestErrorException
	 *             When application not found or when attempting to remove
	 *             management services.
	 */
	@JsonResponseExample(status = "success", responseBody =
			"{\"lifecycleEventContainerID\":\"bfae0a89-b5a0-4250-b393-6cedbf63ac76\"}")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_app") })
	@RequestMapping(value = "applications/{applicationName}/timeout/{timeoutInMinutes}", method = RequestMethod.DELETE)
	@PreAuthorize("isFullyAuthenticated()")
	@ResponseBody
	public Map<String, Object> uninstallApplication(
			@PathVariable final String applicationName,
			@PathVariable final int timeoutInMinutes) throws RestErrorException {

		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		
		// Check that Application exists
		final Application app = this.admin.getApplications().waitFor(
				applicationName, 10, TimeUnit.SECONDS);
		if (app == null) {
			logger.log(Level.INFO, "Cannot uninstall application "
					+ applicationName
					+ " since it has not been discovered yet.");
			throw new RestErrorException(FAILED_TO_LOCATE_APP, applicationName);
		}
		if (app.getName().equals(CloudifyConstants.MANAGEMENT_APPLICATION_NAME)) {
			logger.log(Level.INFO,
					"Cannot uninstall the Management application.");
			throw new RestErrorException(
					ResponseConstants.CANNOT_UNINSTALL_MANAGEMENT_APP);
		}

		final ProcessingUnit[] pus = app.getProcessingUnits()
				.getProcessingUnits();
		
		if (pus.length > 0) {
			if (permissionEvaluator != null) {
				final CloudifyAuthorizationDetails authDetails = new CloudifyAuthorizationDetails(authentication);
				//all the application PUs are supposed to have the same auth-groups setting 
				String puAuthGroups = pus[0].getBeanLevelProperties().getContextProperties().
						getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
				permissionEvaluator.verifyPermission(authDetails, puAuthGroups, "deploy");
			}
		}

		final StringBuilder sb = new StringBuilder();
		final List<ProcessingUnit> uninstallOrder = createUninstallOrder(pus,
				applicationName);
		// TODO: Add timeout.
		FutureTask<Boolean> undeployTask = null;
		logger.log(Level.INFO,
				"Starting to poll for uninstall lifecycle events.");
		if (uninstallOrder.size() > 0) {

			undeployTask = new FutureTask<Boolean>(new Runnable() {
				private final long startTime = System.currentTimeMillis();

				@Override
				public void run() {
					for (final ProcessingUnit processingUnit : uninstallOrder) {
						if (permissionEvaluator != null) {
							final CloudifyAuthorizationDetails authDetails = 
									new CloudifyAuthorizationDetails(authentication);
							String puAuthGroups = processingUnit.getBeanLevelProperties().getContextProperties(). 
									getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
							permissionEvaluator.verifyPermission(authDetails, puAuthGroups, "deploy");
						}

						final long undeployTimeout = TimeUnit.MINUTES.toMillis(timeoutInMinutes)
								- (System.currentTimeMillis() - startTime);
						try {
							if (processingUnit.waitForManaged(TIMEOUT_WAITING_FOR_GSM_SEC,
									TimeUnit.SECONDS) == null) {
								logger.log(Level.WARNING,
										"Failed to locate GSM that is managing Processing Unit "
												+ processingUnit.getName());
							} else {
								logger.log(Level.INFO,
										"Undeploying Processing Unit "
												+ processingUnit.getName());
								processingUnit.undeployAndWait(undeployTimeout,
										TimeUnit.MILLISECONDS);

							}
						} catch (final Exception e) {
							final String msg = "Failed to undeploy processing unit: "
									+ processingUnit.getName()
									+ " while uninstalling application "
									+ applicationName
									+ ". Uninstall will continue, but service "
									+ processingUnit.getName()
									+ " may remain in an unstable state";

							logger.log(Level.SEVERE, msg, e);
						}
					}
					logger.log(Level.INFO, "Application " + applicationName
							+ " undeployment complete");
				}
			}, Boolean.TRUE);

			((InternalAdmin) admin).scheduleAdminOperation(undeployTask);

		}
		final UUID lifecycleEventContainerID = startPollingForApplicationUninstallLifecycleEvents(
				applicationName, uninstallOrder, timeoutInMinutes, undeployTask);

		final String errors = sb.toString();
		if (errors.length() == 0) {
			final Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID,
					lifecycleEventContainerID);
			return RestUtils.successStatus(returnMap);
		}
		throw new RestErrorException(errors);
	}

	private List<ProcessingUnit> createUninstallOrder(
			final ProcessingUnit[] pus, final String applicationName) {

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
			final String dependsOn = (String) processingUnit
					.getBeanLevelProperties().getContextProperties()
					.get(CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON);
			if (dependsOn == null) {
				logger.warning("Could not find the "
						+ CloudifyConstants.CONTEXT_PROPERTY_DEPENDS_ON
						+ " property for processing unit "
						+ processingUnit.getName());

			} else {
				final String[] dependencies = dependsOn.replace("[", "")
						.replace("]", "").split(",");
				for (final String puName : dependencies) {
					final String normalizedPuName = puName.trim();
					if (normalizedPuName.length() > 0) {
						final ProcessingUnit dependency = puByName
								.get(normalizedPuName);
						if (dependency == null) {
							logger.severe("Could not find Processing Unit "
									+ normalizedPuName
									+ " that Processing Unit "
									+ processingUnit.getName() + " depends on");
						} else {
							// the reverse to the install order.
							graph.addEdge(processingUnit, dependency);
						}
					}
				}
			}
		}

		final CycleDetector<ProcessingUnit, DefaultEdge> cycleDetector =
				new CycleDetector<ProcessingUnit, DefaultEdge>(
						graph);
		final boolean containsCycle = cycleDetector.detectCycles();

		if (containsCycle) {
			logger.warning("Detected a cycle in the dependencies of application: "
					+ applicationName
					+ " while preparing to uninstall."
					+ " The service in this application will be uninstalled in a random order");

			return Arrays.asList(pus);
		}

		final TopologicalOrderIterator<ProcessingUnit, DefaultEdge> iterator =
				new TopologicalOrderIterator<ProcessingUnit, DefaultEdge>(graph);

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
	 * Deploys an application to the service grid. An application is consisted
	 * of a group of services that might have dependencies between themselves.
	 * The application will be deployed according to the dependency order
	 * defined in the application file and deployed asynchronously if possible.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param timeout
	 *            .
	 * @param srcFile
	 *            The compressed application file.
	 * @param recipeOverridesFile
	 *            The application overrides file - to overrides the application
	 *            properties.
	 * @param cloudOverrides File of overriding cloud properties
	 * @param selfHealing
	 *            if true, there will be an attempt to restart the recipe in
	 *            case a problem occurred in its life-cycle, otherwise, if the
	 *            recipe fails to execute, no attempt to recover will made.
	 * @param authGroups
	 *            The authorization groups for which this deployment will be
	 *            available.
	 * @return Map with return value.
	 * @throws IOException
	 *             Reporting failure to create a file while opening the packaged
	 *             application file
	 * @throws DSLException
	 *             Reporting failure to parse the application file
	 * @throws RestErrorException .
	 */
	@JsonRequestExample(requestBody = "{\"applicationName\" : \"petclinic\" , \"srcFile\" :"
			+ " \"packaged application file\" "
			+ ", \"recipeOverridesFile\" : \"recipe overrides file\"}")
	@JsonResponseExample(status = "success", responseBody = "{\"srviceOrder\":\"[mongod,mongoConfig,"
			+ "apacheLB,mongos,tomcat]\""
			+ ",\"lifecycleEventContainerID\":\"07db2a16-62f8-4669-ac41-ed9afe3a3b02\"}", comments = "")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "DSLException"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "IOException") })
	@RequestMapping(value = "applications/{applicationName}/timeout/{timeout}", method = RequestMethod.POST)
	@PreAuthorize("isFullyAuthenticated() and hasPermission(#authGroups, 'deploy')")
	@ResponseBody
	public Object deployApplication(
			@PathVariable final String applicationName,
			@PathVariable final int timeout,
			@RequestParam(value = "file", required = true) final MultipartFile srcFile,
			@RequestParam(value = "authGroups", required = false) final String authGroups,
			@RequestParam(value = "recipeOverridesFile", required = false) final MultipartFile recipeOverridesFile,
			@RequestParam(value = "cloudOverridesFile", required = false) final MultipartFile cloudOverrides,
			@RequestParam(value = "selfHealing", required = false) final Boolean selfHealing)
					throws IOException, DSLException, RestErrorException {
		boolean actualSelfHealing = true;
		if (selfHealing != null && !selfHealing) {
			actualSelfHealing = false;
		}
		final File applicationFile = copyMultipartFileToLocalFile(srcFile);

		String effectiveAuthGroups = authGroups;
		if (StringUtils.isBlank(effectiveAuthGroups)) {
			if (permissionEvaluator != null) {
				effectiveAuthGroups = permissionEvaluator.getUserAuthGroupsString();
			} else {
				effectiveAuthGroups = "";
			}
		}

		final File applicationOverridesFile = copyMultipartFileToLocalFile(recipeOverridesFile);
		final File cloudOverridesFile = copyMultipartFileToLocalFile(cloudOverrides);
		final Object returnObject = doDeployApplication(
				applicationName,
				applicationFile,
				applicationOverridesFile,
				effectiveAuthGroups,
				timeout,
				actualSelfHealing,
				cloudOverridesFile);
		FileUtils.deleteQuietly(applicationOverridesFile);
		applicationFile.delete();
		return returnObject;
	}

	private List<Service> createServiceDependencyOrder(
			final org.cloudifysource.dsl.Application application) {
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
						throw new IllegalArgumentException("Dependency '"
								+ depends + "' of service: "
								+ service.getName() + " was not found");
					}

					graph.addEdge(dependency, service);
				}
			}
		}

		final CycleDetector<Service, DefaultEdge> cycleDetector = new CycleDetector<Service, DefaultEdge>(
				graph);
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
			throw new IllegalArgumentException(
					"The dependency graph of application: "
							+ application.getName()
							+ " contains one or more cycles. "
							+ "The services that form a cycle are part of the following group: "
							+ cycleString);
		}

		final TopologicalOrderIterator<Service, DefaultEdge> iterator =
				new TopologicalOrderIterator<Service, DefaultEdge>(graph);

		final List<Service> orderedList = new ArrayList<Service>();
		while (iterator.hasNext()) {
			orderedList.add(iterator.next());
		}
		return orderedList;

	}

	private UUID startPollingForApplicationUninstallLifecycleEvents(
			final String applicationName,
			final List<ProcessingUnit> uninstallOrder,
			final int timeoutInMinutes, final FutureTask<Boolean> undeployTask) {

		RestPollingRunnable restPollingRunnable;
		final LifecycleEventsContainer lifecycleEventsContainer = new LifecycleEventsContainer();
		final UUID lifecycleEventsContainerID = UUID.randomUUID();
		lifecycleEventsContainer.setEventsSet(this.eventsSet);

		restPollingRunnable = new RestPollingRunnable(applicationName,
				timeoutInMinutes, TimeUnit.MINUTES);
		for (final ProcessingUnit processingUnit : uninstallOrder) {
			final String processingUnitName = processingUnit.getName();
			final String serviceName = ServiceUtils.getApplicationServiceName(
					processingUnitName, applicationName);
			restPollingRunnable.addService(serviceName, 0);
		}
		restPollingRunnable.setIsServiceInstall(false);
		restPollingRunnable.setAdmin(admin);
		restPollingRunnable.setLifecycleEventsContainer(lifecycleEventsContainer);
		restPollingRunnable.setIsUninstall(true);
		restPollingRunnable.setUndeployTask(undeployTask);
		restPollingRunnable.setEndTime(timeoutInMinutes, TimeUnit.MINUTES);
		this.lifecyclePollingThreadContainer.put(lifecycleEventsContainerID,
				restPollingRunnable);
		final ScheduledFuture<?> scheduleWithFixedDelay = scheduledExecutor
				.scheduleWithFixedDelay(restPollingRunnable, 0,
						LIFECYCLE_EVENT_POLLING_INTERVAL_SEC, TimeUnit.SECONDS);
		restPollingRunnable.setFutureTask(scheduleWithFixedDelay);

		logger.log(Level.INFO, "polling container UUID is "
				+ lifecycleEventsContainerID.toString());
		return lifecycleEventsContainerID;

	}

	// TODO: Start executer service
	private UUID startPollingForLifecycleEvents(final String serviceName,
			final String applicationName, final int plannedNumberOfInstances,
			final boolean isServiceInstall, final int timeout,
			final TimeUnit minutes) {
		RestPollingRunnable restPollingRunnable;
		logger.info("starting poll on service : " + serviceName + " app: "
				+ applicationName);

		final LifecycleEventsContainer lifecycleEventsContainer = new LifecycleEventsContainer();
		final UUID lifecycleEventsContainerID = UUID.randomUUID();
		lifecycleEventsContainer.setEventsSet(this.eventsSet);

		restPollingRunnable = new RestPollingRunnable(applicationName, timeout,
				minutes);
		restPollingRunnable.addService(serviceName, plannedNumberOfInstances);
		restPollingRunnable.setAdmin(admin);
		restPollingRunnable.setIsServiceInstall(isServiceInstall);
		restPollingRunnable.setLifecycleEventsContainer(lifecycleEventsContainer);
		restPollingRunnable.setEndTime(timeout, TimeUnit.MINUTES);
		restPollingRunnable.setIsSetInstances(true);
		this.lifecyclePollingThreadContainer.put(lifecycleEventsContainerID,
				restPollingRunnable);
		final ScheduledFuture<?> scheduleWithFixedDelay = scheduledExecutor
				.scheduleWithFixedDelay(restPollingRunnable, 0,
						LIFECYCLE_EVENT_POLLING_INTERVAL_SEC, TimeUnit.SECONDS);
		restPollingRunnable.setFutureTask(scheduleWithFixedDelay);

		logger.log(Level.INFO, "polling container UUID is "
				+ lifecycleEventsContainerID.toString());
		return lifecycleEventsContainerID;
	}

	// TODO: Start executer service
	private UUID startPollingForLifecycleEvents(
			final org.cloudifysource.dsl.Application application, final String applicationName,
			final int timeout, final TimeUnit timeUnit) {

		final LifecycleEventsContainer lifecycleEventsContainer = new LifecycleEventsContainer();
		final UUID lifecycleEventsContainerUUID = UUID.randomUUID();
		lifecycleEventsContainer.setEventsSet(this.eventsSet);

		final RestPollingRunnable restPollingRunnable = new RestPollingRunnable(
				applicationName, timeout, timeUnit);
		for (final Service service : application.getServices()) {
			restPollingRunnable.addService(service.getName(),
					service.getNumInstances());
		}
		restPollingRunnable.setIsServiceInstall(false);
		restPollingRunnable.setLifecycleEventsContainer(lifecycleEventsContainer);
		restPollingRunnable.setAdmin(admin);
		restPollingRunnable.setEndTime(timeout, TimeUnit.MINUTES);
		this.lifecyclePollingThreadContainer.put(lifecycleEventsContainerUUID,
				restPollingRunnable);
		final ScheduledFuture<?> scheduleWithFixedDelay = scheduledExecutor
				.scheduleWithFixedDelay(restPollingRunnable, 0,
						LIFECYCLE_EVENT_POLLING_INTERVAL_SEC, TimeUnit.SECONDS);
		restPollingRunnable.setFutureTask(scheduleWithFixedDelay);

		logger.log(Level.INFO, "polling container UUID is "
				+ lifecycleEventsContainerUUID.toString());
		return lifecycleEventsContainerUUID;
	}

	/**
	 * Returns the lifecycle events according to the lifecycleEventContainerID
	 * id that is returned as a response when installing/un-installing a
	 * service/application and according to the cursor position.
	 * 
	 * @param lifecycleEventContainerID
	 *            the unique task ID.
	 * @param cursor
	 *            event entry cursor
	 * @return a map containing the events and the task state.
	 * @throws RestErrorException
	 *             When polling task has expired or if the task ended
	 *             unexpectedly.
	 */
	@JsonResponseExample(status = "success", responseBody = 
			"{\"isDone\":false,\"lifecycleLogs\":[\"[service1] Deployed 1 planned 1\","
					+ "\"Service &#92&#34service1&#92&#34 successfully installed (1 Instances)\"],"
					+ "\"PollingTaskExpirationTimeMillis\":\"575218\",\"curserPos\":12}")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, 
			description = "Lifecycle events container with UUID ... does not exist or expired"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "execution exception message") })
	@RequestMapping(value = "/lifecycleEventContainerID/{lifecycleEventContainerID}/cursor/{cursor}", 
	method = RequestMethod.GET)
	@ResponseBody
	public Object getLifecycleEvents(
			@PathVariable final String lifecycleEventContainerID,
			@PathVariable final int cursor) throws RestErrorException {
		final Map<String, Object> resultsMap = new HashMap<String, Object>();

		if (!this.lifecyclePollingThreadContainer.containsKey(UUID
				.fromString(lifecycleEventContainerID))) {
			throw new RestErrorException(
					"Lifecycle events container with UUID: "
							+ lifecycleEventContainerID
							+ " does not exist or expired.");
		}
		final RestPollingRunnable restPollingRunnable = this.lifecyclePollingThreadContainer
				.get(UUID.fromString(lifecycleEventContainerID));

		final LifecycleEventsContainer container = restPollingRunnable
				.getLifecycleEventsContainer();
		final boolean done = restPollingRunnable.isDone();
		if (!done) {
			extendThreadTimeout(restPollingRunnable,
					DEFAULT_TIME_EXTENTION_POLLING_TASK);
		} else {
			final Throwable t = restPollingRunnable.getExecutionException();
			if (t != null) {
				//TODO [noak] : The real cause might be the cause of the cause here, e.g. Access Denied. Use it.
				logger.log(Level.INFO,
						"Lifecycle events polling ended unexpectedly.", t);
				throw new RestErrorException(t.getMessage());
			} else {
				logger.log(Level.FINE,
						"Lifecycle events polling ended successfully.");
			}
		}
		resultsMap.put(CloudifyConstants.IS_TASK_DONE, done);
		final List<String> lifecycleEvents = container
				.getLifecycleEvents(cursor);

		if (lifecycleEvents != null) {
			final int newCursorPos = cursor + lifecycleEvents.size();
			resultsMap.put(CloudifyConstants.CURSOR_POS, newCursorPos);
			resultsMap.put(CloudifyConstants.LIFECYCLE_LOGS, lifecycleEvents);
		} else {
			resultsMap.put(CloudifyConstants.CURSOR_POS, cursor);
		}
		final long timeBeforeTaskTerminationMillis = restPollingRunnable
				.getEndTime() - System.currentTimeMillis();
		resultsMap.put(CloudifyConstants.SERVER_POLLING_TASK_EXPIRATION_MILLI,
				Long.toString(timeBeforeTaskTerminationMillis));

		return successStatus(resultsMap);
	}

	private void extendThreadTimeout(final RestPollingRunnable pollingRunnable,
			final int timeoutInMinutes) {
		final long taskExpiration = pollingRunnable.getEndTime()
				- System.currentTimeMillis();
		if (taskExpiration < MINIMAL_POLLING_TASK_EXPIRATION) {
			pollingRunnable.increaseEndTimeBy(timeoutInMinutes,
					TimeUnit.MINUTES);
		}
	}

	private Object doDeployApplication(
			final String applicationName,
			final File applicationFile,
			final File applicationOverridesFile,
			final String authGroups,
			final int timeout,
			final boolean selfHealing,
			final File cloudOverrides) throws IOException,
			DSLException, RestErrorException {
		final DSLApplicationCompilatioResult result = ServiceReader
				.getApplicationFromFile(applicationFile,
						applicationOverridesFile);

		validateTemplate(result.getApplication());

		final List<Service> services = createServiceDependencyOrder(result
				.getApplication());

		// validate the template specified by each server (if specified) is
		// available on this cloud
		// cloud could be null in case of eager mode deployment (such as in
		// azure)
		if (!isLocalCloud() && cloud != null) {
			for (final Service service : services) {
				final ComputeDetails compute = service.getCompute();
				if (compute != null
						&& StringUtils.isNotBlank(compute.getTemplate())) {
					getComputeTemplate(cloud, compute.getTemplate());
				}
			}
		}

		final ApplicationInstallerRunnable installer = new ApplicationInstallerRunnable(
				this,
				result,
				applicationName,
				applicationOverridesFile,
				authGroups,
				services,
				this.cloud,
				selfHealing,
				cloudOverrides);

		logger.log(Level.INFO,
				"Starting to poll for installation lifecycle events.");
		final UUID lifecycleEventContainerID = startPollingForLifecycleEvents(
				result.getApplication(), applicationName, timeout, TimeUnit.MINUTES);

		installer.setTaskPollingId(lifecycleEventContainerID);

		if (installer.isAsyncInstallPossibleForApplication()) {
			installer.run();
		} else {
			this.executorService.execute(installer);
		}

		installer.setTaskPollingId(lifecycleEventContainerID);

		final String[] serviceOrder = new String[services.size()];
		for (int i = 0; i < serviceOrder.length; i++) {
			serviceOrder[i] = services.get(i).getName();
		}
		final Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put(CloudifyConstants.SERVICE_ORDER,
				Arrays.toString(serviceOrder));
		returnMap.put(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID,
				lifecycleEventContainerID);

		return successStatus(returnMap);

	}

	private File copyMultipartFileToLocalFile(final MultipartFile srcFile)
			throws IOException {
		if (srcFile == null) {
			return null;
		}
		final File tempFile = new File(temporaryFolder,
				srcFile.getOriginalFilename());
		srcFile.transferTo(tempFile);
		tempFile.deleteOnExit();
		return tempFile;
	}

	/**
	 * Creates a randomly-named file in the system's default temp folder, just
	 * to get the path. The file is deleted immediately.
	 * 
	 * @return The path to the system's default temp folder
	 */
	private String getTempFolderPath() throws IOException {
		/*
		 * long tmpNum = new SecureRandom().nextLong(); if (tmpNum ==
		 * Long.MIN_VALUE) { tmpNum = 0; // corner case } else { tmpNum =
		 * Math.abs(tmpNum); }
		 */
		final File tempFile = File.createTempFile("GS__", null);
		tempFile.delete();
		tempFile.mkdirs();
		return tempFile.getParent();
	}

	private void doDeploy(
			final String applicationName,
			final String serviceName,
			final String authGroups,
			final String templateName,
			final String[] agentZones,
			final File serviceFile,
			final Properties contextProperties,
			final Service service,
			final byte[] serviceCloudConfigurationContents,
			final boolean selfHealing,
			final File cloudOverrides) throws TimeoutException, DSLException, IOException, RestErrorException {

		boolean locationAware = false;
		boolean dedicated = true;
		if (service != null) {
			locationAware = service.isLocationAware();
			dedicated = IsolationUtils.isDedicated(service);
		}

		final int externalProcessMemoryInMB = 512;
		final int containerMemoryInMB = 128;
		final int reservedMemoryCapacityPerMachineInMB = 256;

		contextProperties.setProperty(CloudifyConstants.CONTEXT_PROPERTY_ASYNC_INSTALL,
				"true");
		if (!selfHealing) {
			contextProperties.setProperty(
					CloudifyConstants.CONTEXT_PROPERTY_DISABLE_SELF_HEALING,
					"false");
		}

		final ElasticStatelessProcessingUnitDeployment deployment =
				new ElasticStatelessProcessingUnitDeployment(serviceFile)
		.memoryCapacityPerContainer(externalProcessMemoryInMB, MemoryUnit.MEGABYTES)
		.addCommandLineArgument("-Xmx" + containerMemoryInMB + "m")
		.addCommandLineArgument("-Xms" + containerMemoryInMB + "m")
		.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME, applicationName)
		.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS, authGroups)
		.name(serviceName);
		if (cloud == null) { // Azure or local-cloud
			if (!isLocalCloud()) {
				// Azure: Eager scale (1 container per machine per PU)
				setSharedMachineProvisioning(deployment, agentZones,
						reservedMemoryCapacityPerMachineInMB);
				deployment.scale(ElasticScaleConfigFactory
						.createEagerScaleConfig());
			} else {
				// local cloud
				setPublicMachineProvisioning(deployment, agentZones,
						reservedMemoryCapacityPerMachineInMB);
				if (service == null || service.getScalingRules() == null) {
					final int totalMemoryInMB = calculateTotalMemoryInMB(
							serviceName, service, externalProcessMemoryInMB);
					final ManualCapacityScaleConfig scaleConfig = new ManualCapacityScaleConfigurer()
					.memoryCapacity(totalMemoryInMB, MemoryUnit.MEGABYTES).create();
					deployment.scale(scaleConfig);
				} else {
					final AutomaticCapacityScaleConfig scaleConfig = ElasticScaleConfigFactory
							.createAutomaticCapacityScaleConfig(serviceName,
									service, externalProcessMemoryInMB, false, false);
					deployment.scale(scaleConfig);
				}
			}
		} else {
			final CloudTemplate template = getComputeTemplate(cloud, templateName);


			long cloudExternalProcessMemoryInMB = 0;

			if (dedicated) {
				cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(cloud, template);
			} else {
				cloudExternalProcessMemoryInMB = IsolationUtils.getInstanceMemoryMB(service);
			}


			logger.info("Creating cloud machine provisioning config. Template remote directory is: "
					+ template.getRemoteDirectory());
			final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(
					cloud, template, templateName, this.managementTemplate.getRemoteDirectory());
			config.setAuthGroups(authGroups);
			if (cloudOverrides != null) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Recieved request for installation of "
							+ serviceName + " with cloud overrides parameters [ "
							+ FileUtils.readFileToString(cloudOverrides) + "]");
				}
				config.setCloudOverridesPerService(cloudOverrides);
			} else {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("No cloud overrides parameters were requested for the installation of "
							+ serviceName);
				}
			}
			if (serviceCloudConfigurationContents != null) {
				config.setServiceCloudConfiguration(serviceCloudConfigurationContents);
			}

			final String locators = extractLocators(admin);
			config.setLocator(locators);

			// management machine should be isolated from other services. no
			// matter of the deployment mode.
			config.setDedicatedManagementMachines(true);
			if (!dedicated) {

				// check what mode of isolation we should use
				if (IsolationUtils.isGlobal(service)) {
					logger.info("public mode is on. will use public machine provisioning for "
							+ serviceName + " deployment.");
					// service instances can be deployed across all agents
					setPublicMachineProvisioning(deployment, config);
				} else if (IsolationUtils.isAppShared(service)) {
					logger.info("app shared mode is on. will use shared machine provisioning for "
							+ serviceName + " deployment. isolation id = " + applicationName);
					// service instances can be deployed across all agents with the correct isolation id
					setSharedMachineProvisioning(deployment, config, applicationName);
				} else if (IsolationUtils.isTenantShared(service)) {
					if (authGroups == null) {
						throw new IllegalStateException("authGroups cannot be null when using tenant shared isolation");
					}
					logger.info("tenant shared mode is on. will use shared machine provisioning for "
							+ serviceName + " deployment. isolation id = " + authGroups);
					// service instances can be deployed across all agents with the correct isolation id
					setSharedMachineProvisioning(deployment, config, authGroups);
				}
			} else {
				// service deployment will have a dedicated agent per instance
				setDedicatedMachineProvisioning(deployment, config);
			}

			deployment.memoryCapacityPerContainer((int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);
			if (service == null || service.getScalingRules() == null) {
				final int totalMemoryInMB = calculateTotalMemoryInMB(
						serviceName, service,
						(int) cloudExternalProcessMemoryInMB);
				final double totalCpuCores = calculateTotalCpuCores(service);
				final ManualCapacityScaleConfig scaleConfig = ElasticScaleConfigFactory
						.createManualCapacityScaleConfig(totalMemoryInMB,
								totalCpuCores, locationAware, dedicated);
				deployment.scale(scaleConfig);
			} else {
				final AutomaticCapacityScaleConfig scaleConfig = ElasticScaleConfigFactory
						.createAutomaticCapacityScaleConfig(serviceName,
								service, (int) cloudExternalProcessMemoryInMB,
								locationAware, dedicated);
				deployment.scale(scaleConfig);
			}
		}

		// add context properties
		setContextProperties(deployment, contextProperties);
		verifyEsmExistsInCluster();
		deployAndWait(serviceName, deployment);
	}

	/**
	 * @param serviceName
	 *            - the absolute name of the service
	 * @param service
	 *            - the service DSL or null if not exists
	 * @param externalProcessMemoryInMB
	 *            - MB memory allocated for the GSC plus the external service.
	 * @return a @{link ManualCapacityScaleConfig} based on the specified
	 *         service and memory.
	 */
	public static int calculateTotalMemoryInMB(final String serviceName,
			final Service service, final int externalProcessMemoryInMB)
					throws DSLException {

		if (externalProcessMemoryInMB <= 0) {
			throw new IllegalArgumentException(
					"externalProcessMemoryInMB must be positive");
		}

		int numberOfInstances = 1;
		if (service == null) {
			logger.info("Deploying service " + serviceName
					+ " without a recipe. Assuming number of instances is 1");
		} else if (service.getNumInstances() > 0) {
			numberOfInstances = service.getNumInstances();
			logger.info("Deploying service " + serviceName + " with "
					+ numberOfInstances + " instances.");
		} else {
			throw new DSLException("Number of instances must be at least 1");
		}

		return externalProcessMemoryInMB * numberOfInstances;
	}

	private static double calculateTotalCpuCores(final Service service) {

		if (service == null) { // deploying without a service. assuming CPU requirements is 0
			return 0;
		}

		double instanceCpuCores = IsolationUtils.getInstanceCpuCores(service);

		if (instanceCpuCores < 0) {
			throw new IllegalArgumentException(
					"instanceCpuCores must be positive");
		}

		final int numberOfInstances = service.getNumInstances();

		return numberOfInstances * instanceCpuCores;
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

	private long calculateExternalProcessMemory(final Cloud cloud,
			final CloudTemplate template) throws DSLException {
		// TODO remove hardcoded number
		logger.info("Calculating external proc mem for template: " + template);
		final int machineMemoryMB = template.getMachineMemoryMB();
		final int reservedMemoryCapacityPerMachineInMB = cloud.getProvider()
				.getReservedMemoryCapacityPerMachineInMB();
		final int safteyMargin = 100; // get rid of this constant. see
		// CLOUDIFY-297
		final long cloudExternalProcessMemoryInMB = machineMemoryMB
				- reservedMemoryCapacityPerMachineInMB - safteyMargin;
		if (cloudExternalProcessMemoryInMB <= 0) {
			throw new DSLException("Cloud template machineMemoryMB ("
					+ machineMemoryMB + "MB) must be bigger than "
					+ "reservedMemoryCapacityPerMachineInMB+" + safteyMargin
					+ " ("
					+ (reservedMemoryCapacityPerMachineInMB + safteyMargin)
					+ ")");
		}
		logger.fine("template.machineMemoryMB = "
				+ template.getMachineMemoryMB() + "MB\n"
				+ "cloud.provider.reservedMemoryCapacityPerMachineInMB = "
				+ reservedMemoryCapacityPerMachineInMB + "MB\n"
				+ "cloudExternalProcessMemoryInMB = "
				+ cloudExternalProcessMemoryInMB + "MB"
				+ "cloudExternalProcessMemoryInMB = cloud.machineMemoryMB - "
				+ "cloud.reservedMemoryCapacityPerMachineInMB" + " = "
				+ cloudExternalProcessMemoryInMB);
		return cloudExternalProcessMemoryInMB;
	}

	/**
	 * Local-Cloud has one agent without any zone.
	 */
	private boolean isLocalCloud() {
		final GridServiceAgent[] agents = admin.getGridServiceAgents()
				.getAgents();
		final boolean isOnlyOneAgent = agents.length == 1;
		final GridServiceAgent agent = agents[0];
		final AtLeastOneZoneConfig requiredContainerZone = new AtLeastOneZoneConfigurer()
		.addZone(LOCALCLOUD_ZONE).create();

		final boolean isLocalCloudZone = agent.getExactZones().isStasfies(
				requiredContainerZone);
		final boolean isLocalCloud = isOnlyOneAgent && isLocalCloudZone;
		if (logger.isLoggable(Level.FINE)) {
			if (!isOnlyOneAgent) {
				logger.fine("Not local cloud since there are " + agents.length
						+ " agents");
			} else if (!isLocalCloudZone) {
				logger.fine("Not local cloud since no " + LOCALCLOUD_ZONE
						+ " in agent zones " + agent.getExactZones());
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
	public boolean waitForServiceInstance(final String applicationName,
			final String serviceName, final long timeout,
			final TimeUnit timeUnit) {

		// this should be a very fast lookup, since the service was already
		// successfully deployed
		final String absolutePUName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		final ProcessingUnit pu = this.admin.getProcessingUnits().waitFor(
				absolutePUName, timeout, timeUnit);
		if (pu == null) {
			return false;
		}

		// ignore the time spent on PU lookup, as it should be failry short.
		return pu.waitFor(1, timeout, timeUnit);

	}

	/**
	 * 
	 * @param serviceName
	 *            .
	 * @param applicationName
	 *            .
	 * @param authGroups
	 *            The authorization groups for which this deployment will be
	 *            available. The the group for which this deployment will be
	 *            available.
	 * @param zone
	 *            .
	 * @param srcFile
	 *            .
	 * @param propsFile
	 *            properties file.
	 * @param originalTemplateName
	 *            .
	 * @param isApplicationInstall
	 *            .
	 * @param timeout
	 *            .
	 * @param timeUnit
	 *            .
	 * @param serviceCloudConfigurationContents
	 *            .
	 * @param selfHealing
	 *            if true, there will be an attempt to restart the recipe in
	 *            case a problem occurred in its life-cycle, otherwise, if the
	 *            recipe fails to execute, no attempt to recover will made.
	 * @param cloudOverrides
	 *            - A file containing cloud override properties to be used by
	 *            the cloud driver.
	 * @return lifecycleEventContainerID.
	 * @throws RestErrorException .
	 * @throws TimeoutException .
	 * @throws IOException .
	 * @throws DSLException .
	 */
	public String deployElasticProcessingUnit(
			final String serviceName,
			final String applicationName,
			final String authGroups,
			final String zone,
			final File srcFile,
			final Properties propsFile,
			final String originalTemplateName,
			final boolean isApplicationInstall,
			final int timeout,
			final TimeUnit timeUnit,
			final byte[] serviceCloudConfigurationContents,
			final boolean selfHealing,
			final File cloudOverrides) throws TimeoutException, IOException,
			DSLException, RestErrorException {

		if (cloudOverrides != null) {
			if (cloudOverrides.length() >= TEN_K) {
				throw new RestErrorException(CloudifyErrorMessages.CLOUD_OVERRIDES_TO_LONG.getName());
			}
		}

		String templateName;
		if (originalTemplateName == null) {
			templateName = this.defaultTemplateName;
		} else {
			templateName = originalTemplateName;
		}

		if (templateName != null) {
			propsFile.setProperty(CloudifyConstants.CONTEXT_PROPERTY_TEMPLATE,
					templateName);
		}

		String effectiveAuthGroups = authGroups;
		if (StringUtils.isBlank(effectiveAuthGroups)) {
			if (permissionEvaluator != null) {
				effectiveAuthGroups = permissionEvaluator.getUserAuthGroupsString();
			} else {
				effectiveAuthGroups = "";
			}
		}

		Service service = null;
		File projectDir = null;
		if (srcFile.getName().endsWith(".zip")) {

			projectDir = ServiceReader.extractProjectFile(srcFile);
			final File workingProjectDir = new File(projectDir, "ext");
			final String serviceFileName = propsFile
					.getProperty(CloudifyConstants.CONTEXT_PROPERTY_SERVICE_FILE_NAME);
			DSLServiceCompilationResult result;
			if (serviceFileName != null) {
				result = ServiceReader.getServiceFromFile(new File(
						workingProjectDir, serviceFileName), workingProjectDir);
			} else {
				result = ServiceReader
						.getServiceFromDirectory(workingProjectDir);
			}
			service = result.getService();
		}

		validateTemplate(templateName);

		String[] agentZones;
		if (isLocalCloud()) {
			agentZones = new String[] { zone, LOCALCLOUD_ZONE };
		} else {
			agentZones = new String[] { zone };
		}

		if (service == null) {
			doDeploy(applicationName, serviceName, effectiveAuthGroups, templateName, agentZones,
					srcFile, propsFile, selfHealing, cloudOverrides);
		} else if (service.getLifecycle() != null) {
			doDeploy(applicationName, serviceName, effectiveAuthGroups, templateName, agentZones,
					srcFile, propsFile, service,
					serviceCloudConfigurationContents, selfHealing, cloudOverrides);
		} else if (service.getDataGrid() != null) {
			deployDataGrid(applicationName, serviceName, effectiveAuthGroups, agentZones, srcFile,
					propsFile, service.getDataGrid(), templateName,
					service.isLocationAware(), cloudOverrides);
		} else if (service.getStatelessProcessingUnit() != null) {
			deployStatelessProcessingUnitAndWait(applicationName, serviceName, effectiveAuthGroups,
					agentZones, new File(projectDir, "ext"), propsFile,
					service.getStatelessProcessingUnit(), templateName,
					service.getNumInstances(), service.isLocationAware(), cloudOverrides);
		} else if (service.getMirrorProcessingUnit() != null) {
			deployStatelessProcessingUnitAndWait(applicationName, serviceName, effectiveAuthGroups,
					agentZones, new File(projectDir, "ext"), propsFile,
					service.getMirrorProcessingUnit(), templateName,
					service.getNumInstances(), service.isLocationAware(), cloudOverrides);
		} else if (service.getStatefulProcessingUnit() != null) {
			deployStatefulProcessingUnit(applicationName, serviceName, effectiveAuthGroups,
					agentZones, new File(projectDir, "ext"), propsFile,
					service.getStatefulProcessingUnit(), templateName,
					service.isLocationAware(), cloudOverrides);
		} else {
			throw new IllegalStateException("Unsupported service type");
		}
		if (projectDir != null) {
			try {
				FileUtils.deleteDirectory(projectDir);
			} catch (final IOException e) {
				// this may happen if a classloader is holding unto a jar file
				// in the usmlib directory
				// the files are temp files, so it should be ok if they remain
				// on the disk
				logger.log(Level.WARNING, "Failed to delete project files: "
						+ e.getMessage(), e);
			}
		}
		srcFile.delete();

		String lifecycleEventContainerID = "";
		if (!isApplicationInstall) {
			logger.log(Level.INFO,
					"Starting to poll for installation lifecycle events.");
			if (service == null) {
				lifecycleEventContainerID = startPollingForLifecycleEvents(
						ServiceUtils.getApplicationServiceName(serviceName,
								applicationName), applicationName, 1, true,
								timeout, timeUnit).toString();
			} else {
				lifecycleEventContainerID = startPollingForLifecycleEvents(
						service.getName(), applicationName,
						service.getNumInstances(), true, timeout, timeUnit)
						.toString();
			}
		}
		return lifecycleEventContainerID;
	}

	private void doDeploy(final String applicationName,
			final String serviceName, final String authGroups, final String templateName,
			final String[] agentZones, final File srcFile,
			final Properties propsFile, final boolean selfHealing,
			final File cloudOverrides)
					throws TimeoutException, DSLException, IOException, RestErrorException {
		doDeploy(applicationName, serviceName, authGroups, templateName, agentZones,
				srcFile, propsFile, null, null, selfHealing, cloudOverrides);
	}

	// TODO: add getters for service processing units in the service class that
	// does the cast automatically.
	/**
	 * 
	 * @param applicationName
	 *            .
	 * @param serviceName
	 *            .
	 * @param timeout
	 * @param authGroups
	 *            The authorization groups for which this deployment will be
	 *            available.
	 * @param templateName
	 *            .
	 * @param zone
	 *            .
	 * @param srcFile
	 *            .
	 * @param propsFile
	 * 
	 * @param selfHealing
	 * @throws DSLException
	 * @throws RestErrorException . .
	 * @param selfHealing
	 *            .
	 * @param cloudOverridesFile
	 *            - A file containing override parameters to be used by the
	 *            cloud driver.
	 * @return status - success (error) and response - lifecycle events
	 *         container id (error description)
	 * @throws DSLException
	 * @throws RestErrorException
	 * @throws TimeoutException .
	 * @throws PackagingException .
	 * @throws IOException .
	 * @throws DSLException .
	 */
	@JsonRequestExample(requestBody = "{\"zone\":5,\"template\":\"SMALL_LINUX\","
			+ "\"file\":\"packaged service file\",\"props\":\"packaged properties file\"}")
	@JsonResponseExample(status = "success", responseBody = "\"b41febb7-f48e-48d4-b14a-a6000d402d93\"")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "TimeoutException"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "PackagingException"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "IOException"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "AdminException"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "DSLException") })
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/timeout/{timeout}",
	method = RequestMethod.POST)
	@PreAuthorize("isFullyAuthenticated() and hasPermission(#authGroups, 'deploy')")
	@ResponseBody
	public Object deployElastic(
			@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@PathVariable final int timeout,
			@RequestParam(value = "template", required = false) final String templateName,
			@RequestParam(value = "zone", required = true) final String zone,
			@RequestParam(value = "file", required = true) final MultipartFile srcFile,
			@RequestParam(value = "props", required = true) final MultipartFile propsFile,
			@RequestParam(value = "authGroups", required = false) final String authGroups,
			@RequestParam(value = "cloudOverridesFile", required = false) final MultipartFile cloudOverridesFile,
			@RequestParam(value = "selfHealing", required = false, defaultValue = "true") final Boolean selfHealing)
					throws TimeoutException, PackagingException, IOException,
					DSLException, RestErrorException {

		logger.info("Deploying service with template: " + templateName);
		String actualTemplateName = templateName;

		if (cloud != null) {
			if (templateName == null || templateName.length() == 0) {
				if (cloud.getTemplates().isEmpty()) {
					throw new IllegalStateException(
							"Cloud configuration has no compute template defined!");
				}
				actualTemplateName = cloud.getTemplates().keySet().iterator()
						.next();
				logger.warning("Compute Template name missing from service deployment request."
						+ " Defaulting to first template: "
						+ actualTemplateName);

			}
		}

		final String absolutePuName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		final byte[] propsBytes = propsFile.getBytes();
		final Properties props = new Properties();
		final InputStream is = new ByteArrayInputStream(propsBytes);
		props.load(is);
		final File dest = copyMultipartFileToLocalFile(srcFile);
		final File cloudOverrides = copyMultipartFileToLocalFile(cloudOverridesFile);
		final File destFile = new File(dest.getParent(), absolutePuName + "."
				+ FilenameUtils.getExtension(dest.getName()));
		if (destFile.exists()) {
			FileUtils.deleteQuietly(destFile);
		}

		String effectiveAuthGroups = authGroups;
		if (StringUtils.isBlank(effectiveAuthGroups)) {
			if (permissionEvaluator != null) {
				effectiveAuthGroups = permissionEvaluator.getUserAuthGroupsString();
			} else {
				effectiveAuthGroups = "";
			}
		}

		String lifecycleEventsContainerID = "";
		if (dest.renameTo(destFile)) {
			FileUtils.deleteQuietly(dest);

			final File cloudConfigurationFile = ZipUtils
					.unzipEntry(
							destFile,
							"ext/"
									+ CloudifyConstants.SERVICE_CLOUD_CONFIGURATION_FILE_NAME,
									CloudifyConstants.SERVICE_CLOUD_CONFIGURATION_FILE_NAME);
			byte[] cloudConfigurationContents = null;
			if (cloudConfigurationFile != null) {
				cloudConfigurationContents = FileUtils
						.readFileToByteArray(cloudConfigurationFile);
			}

			lifecycleEventsContainerID = deployElasticProcessingUnit(
					absolutePuName, applicationName, effectiveAuthGroups, zone, destFile, props,
					actualTemplateName, false, timeout, TimeUnit.MINUTES,
					cloudConfigurationContents, selfHealing,
					cloudOverrides);
			destFile.deleteOnExit();
		} else {
			logger.warning("Deployment file could not be renamed to the absolute pu name."
					+ " Deploaying using the name " + dest.getName());
			lifecycleEventsContainerID = deployElasticProcessingUnit(
					absolutePuName,
					applicationName,
					effectiveAuthGroups,
					zone,
					dest,
					props,
					actualTemplateName,
					false,
					timeout,
					TimeUnit.MINUTES,
					null,
					selfHealing,
					cloudOverrides);
			dest.deleteOnExit();
		}

		// TODO: move this Key String to the DSL project as a constant.
		// Map<String, String> serviceDetails = new HashMap<String, String>();
		// serviceDetails.put("lifecycleEventsContainerID",
		// lifecycleEventsContainerID.toString());
		return successStatus(lifecycleEventsContainerID);
	}

	private File getJarFileFromDir(File serviceFileOrDir,
			final File serviceDirectory, final String jarName)
					throws IOException {
		if (!serviceFileOrDir.isAbsolute()) {
			serviceFileOrDir = new File(serviceDirectory,
					serviceFileOrDir.getPath());
		}
		final File destJar = new File(serviceDirectory.getParent(), jarName
				+ ".jar");
		FileUtils.deleteQuietly(destJar);
		if (serviceFileOrDir.isDirectory()) {
			final File jarFile = File.createTempFile(
					serviceFileOrDir.getName(), ".jar");
			ZipUtils.zip(serviceFileOrDir, jarFile);
			// rename the jar so would appear as 'Absolute pu name' in the
			// deploy folder.
			jarFile.renameTo(destJar);
			jarFile.deleteOnExit();
			return destJar;
		} else if (serviceFileOrDir.isFile()) {
			// rename the jar so would appear as 'Absolute pu name' in the
			// deploy folder.
			serviceFileOrDir.renameTo(destJar);
			return destJar;
		}

		throw new FileNotFoundException("The file " + serviceFileOrDir
				+ " was not found in the service folder");
	}

	private CloudTemplate getComputeTemplate(final Cloud cloud,
			final String templateName) {
		if (templateName == null) {
			final Entry<String, CloudTemplate> entry = cloud.getTemplates()
					.entrySet().iterator().next();

			logger.warning("Service does not specify template name! Defaulting to template: "
					+ entry.getKey());
			return entry.getValue();
		}
		final CloudTemplate template = cloud.getTemplates().get(templateName);
		if (template == null) {
			throw new IllegalArgumentException(
					"Could not find compute template: " + templateName);
		}
		return template;
	}

	private void validateTemplate(
			final org.cloudifysource.dsl.Application application)
					throws RestErrorException {
		final List<Service> services = application.getServices();
		for (Service service : services) {
			validateTemplate(service);
		}
	}

	private void validateTemplate(final String templateName)
			throws RestErrorException {

		if (cloud == null) {
			// no template validation for local cloud
			return;
		}
		final CloudTemplate template = cloud.getTemplates().get(
				templateName);
		if (template == null) {
			throw new RestErrorException(
					CloudifyErrorMessages.MISSING_TEMPLATE.getName(),
					templateName);
		}
	}

	private void validateTemplate(final Service service)
			throws RestErrorException {

		if (service == null) {
			return;
		}
		ComputeDetails compute = service.getCompute();
		String templateName = null;
		if (compute != null) {
			templateName = compute.getTemplate();
		}

		if (this.cloud != null) {
			if (templateName != null) {
				validateTemplate(templateName);
			}
		}

	}

	// TODO: consider adding MemoryUnits to DSL
	// TODO: add memory unit to names
	private void deployDataGrid(final String applicationName, final String serviceName,
			final String authGroups, final String[] agentZones,
			final File srcFile, final Properties contextProperties,
			final DataGrid dataGridConfig, final String templateName,
			final boolean locationAware,
			final File cloudOverrides) throws AdminException,
			TimeoutException, DSLException, IOException {

		final int containerMemoryInMB = dataGridConfig.getSla()
				.getMemoryCapacityPerContainer();
		final int maxMemoryInMB = dataGridConfig.getSla()
				.getMaxMemoryCapacity();
		final int reservedMemoryCapacityPerMachineInMB = 256;

		logger.finer("received request to install datagrid");

		final ElasticSpaceDeployment deployment = new ElasticSpaceDeployment(
				serviceName)
		.memoryCapacityPerContainer(containerMemoryInMB,
				MemoryUnit.MEGABYTES)
				.maxMemoryCapacity(maxMemoryInMB, MemoryUnit.MEGABYTES)
				.addContextProperty(
						CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME,
						applicationName)
						.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS, authGroups)
						.highlyAvailable(dataGridConfig.getSla().getHighlyAvailable())
						// allow single machine for local development purposes
						.singleMachineDeployment();

		setContextProperties(deployment, contextProperties);

		if (cloud == null) {
			if (isLocalCloud()) {
				setPublicMachineProvisioning(deployment, agentZones,
						reservedMemoryCapacityPerMachineInMB);
				deployment.scale(new ManualCapacityScaleConfigurer()
				.memoryCapacity(
						dataGridConfig.getSla().getMemoryCapacity(),
						MemoryUnit.MEGABYTES).create());

			} else {
				setSharedMachineProvisioning(deployment, agentZones,
						reservedMemoryCapacityPerMachineInMB);
				// eager scaling. 1 container per machine
				deployment.scale(ElasticScaleConfigFactory
						.createEagerScaleConfig());
			}

		} else {

			final CloudTemplate template = getComputeTemplate(cloud,
					templateName);

			validateAndPrepareStatefulSla(serviceName, dataGridConfig.getSla(),
					cloud, template);

			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(
					cloud, template);

			final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(
					cloud, template, templateName,
					this.managementTemplate.getRemoteDirectory());
			config.setAuthGroups(authGroups);
			
			if (cloudOverrides != null) {
				config.setCloudOverridesPerService(cloudOverrides);
			}

			final String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);
			deployment.memoryCapacityPerContainer(
					(int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);

			// TODO: [itaif] Why only capacity of one container ?
			deployment.scale(ElasticScaleConfigFactory
					.createManualCapacityScaleConfig(
							(int) cloudExternalProcessMemoryInMB, 0,
							locationAware, true));
		}

		deployAndWait(serviceName, deployment);

	}

	private void setContextProperties(
			final ElasticDeploymentTopology deployment,
			final Properties contextProperties) {
		final Set<Entry<Object, Object>> contextPropsEntries = contextProperties
				.entrySet();
		for (final Entry<Object, Object> entry : contextPropsEntries) {
			deployment.addContextProperty((String) entry.getKey(),
					(String) entry.getValue());
		}
	}

	private void setSharedMachineProvisioning(
			final ElasticDeploymentTopology deployment,
			final String[] agentZones,
			final int reservedMemoryCapacityPerMachineInMB) {
		// All PUs on this role share the same machine. Machines
		// are identified by zone.
		final DiscoveredMachineProvisioningConfig machineProvisioning = new DiscoveredMachineProvisioningConfigurer()
		.reservedMemoryCapacityPerMachine(
				reservedMemoryCapacityPerMachineInMB,
				MemoryUnit.MEGABYTES).create();
		machineProvisioning.setGridServiceAgentZones(agentZones);

		if (isLocalCloud()) {
			deployment.publicMachineProvisioning(machineProvisioning);
		} else {
			deployment.sharedMachineProvisioning(SHARED_ISOLATION_ID,
					machineProvisioning);
		}
	}

	private void setSharedMachineProvisioning(
			final ElasticDeploymentTopology deployment,
			final CloudifyMachineProvisioningConfig config,
			final String isolationId) {
		deployment.sharedMachineProvisioning(isolationId, config);
	}

	private void setPublicMachineProvisioning(
			final ElasticDeploymentTopology deployment,
			final String[] agentZones,
			final int reservedMemoryCapacityPerMachineInMB) {
		// All PUs on this role share the same machine. Machines
		// are identified by zone.
		final DiscoveredMachineProvisioningConfig machineProvisioning = new DiscoveredMachineProvisioningConfigurer()
		.reservedMemoryCapacityPerMachine(
				reservedMemoryCapacityPerMachineInMB,
				MemoryUnit.MEGABYTES).create();
		machineProvisioning.setGridServiceAgentZones(agentZones);

		deployment.publicMachineProvisioning(machineProvisioning);
	}

	private void setPublicMachineProvisioning(
			final ElasticDeploymentTopology deployment,
			final CloudifyMachineProvisioningConfig config) {
		deployment.publicMachineProvisioning(config);
	}

	private void setDedicatedMachineProvisioning(
			final ElasticDeploymentTopology deployment,
			final ElasticMachineProvisioningConfig config) {
		deployment.dedicatedMachineProvisioning(config);
	}

	private void deployStatelessProcessingUnitAndWait(
			final String applicationName, final String serviceName, final String authGroups,
			final String[] agentZones, final File extractedServiceFolder,
			final Properties contextProperties,
			final StatelessProcessingUnit puConfig, final String templateName,
			final int numberOfInstances, final boolean locationAware,
			final File cloudOverride)
					throws IOException, AdminException, TimeoutException, DSLException, RestErrorException {

		final File jarFile = getJarFileFromDir(
				new File(puConfig.getBinaries()), extractedServiceFolder,
				serviceName);
		// TODO:if not specified use machine memory defined in DSL
		final int containerMemoryInMB = puConfig.getSla()
				.getMemoryCapacityPerContainer();
		// TODO:Read from cloud DSL
		final int reservedMemoryCapacityPerMachineInMB = 256;
		final ElasticStatelessProcessingUnitDeployment deployment = new ElasticStatelessProcessingUnitDeployment(
				jarFile)
		.memoryCapacityPerContainer(containerMemoryInMB,
				MemoryUnit.MEGABYTES)
				.addContextProperty(
						CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME,
						applicationName)
						.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS, authGroups)
						.name(serviceName);
		// TODO:read from cloud DSL

		setContextProperties(deployment, contextProperties);

		if (cloud == null) {
			verifyEsmExistsInCluster();

			if (isLocalCloud()) {
				setPublicMachineProvisioning(deployment, agentZones,
						reservedMemoryCapacityPerMachineInMB);
				deployment.scale(new ManualCapacityScaleConfigurer()
				.memoryCapacity(
						containerMemoryInMB * numberOfInstances,
						MemoryUnit.MEGABYTES).create());
			} else {
				setSharedMachineProvisioning(deployment, agentZones,
						reservedMemoryCapacityPerMachineInMB);
				// eager scaling. one container per machine
				deployment.scale(ElasticScaleConfigFactory
						.createEagerScaleConfig());
			}
		} else {
			final CloudTemplate template = getComputeTemplate(cloud,
					templateName);
			validateAndPrepareStatelessSla(puConfig.getSla(), cloud, template);
			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(
					cloud, template);

			final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(
					cloud, template, templateName,
					this.managementTemplate.getRemoteDirectory());
			config.setAuthGroups(authGroups);
			if (cloudOverride != null) {
				config.setCloudOverridesPerService(cloudOverride);
			}

			final String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);
			deployment.memoryCapacityPerContainer(
					(int) cloudExternalProcessMemoryInMB, MemoryUnit.MEGABYTES);

			deployment.scale(ElasticScaleConfigFactory
					.createManualCapacityScaleConfig(containerMemoryInMB
							* numberOfInstances, 0, locationAware, true));
		}
		deployAndWait(serviceName, deployment);
		jarFile.delete();

	}

	private void verifyEsmExistsInCluster() throws IllegalStateException {
		final ElasticServiceManager esm = getESM();
		if (esm == null) {
			// TODO - Add locators
			throw new IllegalStateException(
					"Could not find an ESM in the cluster. Groups: "
							+ Arrays.toString(this.admin.getGroups()));
		}

	}

	// TODO:Clean this class it has a lot of code duplications
	private void deployStatefulProcessingUnit(final String applicationName,
			final String serviceName, final String authGroups, final String[] agentZones,
			final File extractedServiceFolder,
			final Properties contextProperties,
			final StatefulProcessingUnit puConfig, final String templateName,
			final boolean locationAware,
			final File cloudOverrides) throws IOException, AdminException,
			TimeoutException, DSLException {

		final File jarFile = getJarFileFromDir(
				new File(puConfig.getBinaries()), extractedServiceFolder,
				serviceName);
		final int containerMemoryInMB = puConfig.getSla()
				.getMemoryCapacityPerContainer();
		final int maxMemoryCapacityInMB = puConfig.getSla()
				.getMaxMemoryCapacity();
		final int reservedMemoryCapacityPerMachineInMB = 256;

		final ElasticStatefulProcessingUnitDeployment deployment = new ElasticStatefulProcessingUnitDeployment(
				jarFile)
		.name(serviceName)
		.memoryCapacityPerContainer(containerMemoryInMB,
				MemoryUnit.MEGABYTES)
				.maxMemoryCapacity(maxMemoryCapacityInMB + "m")
				.addContextProperty(
						CloudifyConstants.CONTEXT_PROPERTY_APPLICATION_NAME,
						applicationName)
						.addContextProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS, authGroups)
						.highlyAvailable(puConfig.getSla().getHighlyAvailable())
						.singleMachineDeployment();

		setContextProperties(deployment, contextProperties);

		if (cloud == null) {
			verifyEsmExistsInCluster();
			if (isLocalCloud()) {
				setPublicMachineProvisioning(deployment, agentZones,
						reservedMemoryCapacityPerMachineInMB);
				deployment.scale(new ManualCapacityScaleConfigurer()
				.memoryCapacity(puConfig.getSla().getMemoryCapacity(),
						MemoryUnit.MEGABYTES).create());
			} else {
				setSharedMachineProvisioning(deployment, agentZones,
						reservedMemoryCapacityPerMachineInMB);
				// eager scaling. one container per machine
				deployment.scale(ElasticScaleConfigFactory
						.createEagerScaleConfig());
			}
		} else {

			final CloudTemplate template = getComputeTemplate(cloud,
					templateName);

			validateAndPrepareStatefulSla(serviceName, puConfig.getSla(),
					cloud, template);

			final CloudifyMachineProvisioningConfig config = new CloudifyMachineProvisioningConfig(
					cloud, template, templateName,
					this.managementTemplate.getRemoteDirectory());
			config.setAuthGroups(authGroups);
			if (cloudOverrides != null) {
				config.setCloudOverridesPerService(cloudOverrides);
			}

			final String locators = extractLocators(admin);
			config.setLocator(locators);

			setDedicatedMachineProvisioning(deployment, config);

			deployment.scale(ElasticScaleConfigFactory
					.createManualCapacityScaleConfig(puConfig.getSla()
							.getMemoryCapacity(), 0, locationAware, true));

		}

		deployAndWait(serviceName, deployment);
		jarFile.delete();

	}

	private void validateAndPrepareStatefulSla(final String serviceName,
			final Sla sla, final Cloud cloud, final CloudTemplate template)
					throws DSLException {

		validateMemoryCapacityPerContainer(sla, cloud, template);

		if (sla.getMaxMemoryCapacity() != null
				&& sla.getMemoryCapacity() != null
				&& sla.getMaxMemoryCapacity() < sla.getMemoryCapacity()) {

			throw new DSLException(
					"Max memory capacity is smaller than the memory capacity."
							+ sla.getMaxMemoryCapacity() + " < "
							+ sla.getMemoryCapacity());
		}

		final int minimumNumberOfContainers = sla.getHighlyAvailable() ? 2 : 1;
		final int minMemoryInMB = minimumNumberOfContainers
				* sla.getMemoryCapacityPerContainer();

		if (sla.getMemoryCapacity() == null
				|| sla.getMemoryCapacity() < minMemoryInMB) {

			logger.info("Setting memoryCapacity for service " + serviceName
					+ " to minimum " + minMemoryInMB + "MB");
			sla.setMemoryCapacity(minMemoryInMB);
		}

		if (sla.getMaxMemoryCapacity() == null
				|| sla.getMaxMemoryCapacity() < sla.getMemoryCapacity()) {

			logger.info("Setting maxMemoryCapacity for service " + serviceName
					+ " to memoryCapacity " + sla.getMemoryCapacity() + "MB");
			sla.setMaxMemoryCapacity(sla.getMemoryCapacity());
		}
	}

	private void validateAndPrepareStatelessSla(final Sla sla,
			final Cloud cloud, final CloudTemplate template)
					throws DSLException {

		validateMemoryCapacityPerContainer(sla, cloud, template);

		if (sla.getMemoryCapacity() != null) {
			throw new DSLException(
					"memoryCapacity SLA is not supported in this service");
		}

		if (sla.getMaxMemoryCapacity() != null) {
			throw new DSLException(
					"maxMemoryCapacity SLA is not supported in this service");
		}

	}

	private void validateMemoryCapacityPerContainer(final Sla sla,
			final Cloud cloud, final CloudTemplate template)
					throws DSLException {
		if (cloud == null) {
			// No cloud, must specify memory capacity per container explicitly
			if (sla.getMemoryCapacityPerContainer() == null) {
				throw new DSLException(
						"Cannot determine memoryCapacityPerContainer SLA");
			}
		} else {
			// Assuming one container per machine then container memory =
			// machine memory
			final int availableMemoryOnMachine = (int) calculateExternalProcessMemory(
					cloud, template);
			if (sla.getMemoryCapacityPerContainer() != null
					&& sla.getMemoryCapacityPerContainer() > availableMemoryOnMachine) {
				throw new DSLException(
						"memoryCapacityPerContainer SLA is larger than available memory on machine\n"
								+ sla.getMemoryCapacityPerContainer() + " > "
								+ availableMemoryOnMachine);
			}

			if (sla.getMemoryCapacityPerContainer() == null) {
				sla.setMemoryCapacityPerContainer(availableMemoryOnMachine);
			}
		}
	}

	private void deployAndWait(final String serviceName,
			final ElasticStatefulProcessingUnitDeployment deployment)
					throws TimeoutException, AdminException {
		final ProcessingUnit pu = getGridServiceManager().deploy(deployment,
				60, TimeUnit.SECONDS);
		if (pu == null) {
			throw new TimeoutException("Timed out waiting for Service "
					+ serviceName + " deployment.");
		}
	}

	/**
	 * 
	 * @param applicationName
	 *            .
	 * @param serviceName
	 *            .
	 * @param timeout
	 *            .
	 * @param count
	 *            .
	 * @param locationAware
	 *            .
	 * @return lifecycleEventContainerID .
	 * @throws DSLException .
	 * @throws RestErrorException
	 *             When failed to locate service or in the case where the
	 *             service is not elastic.
	 */
	@JsonRequestExample(requestBody = "{\"count\":1,\"location-aware\":true}")
	@JsonResponseExample(status = "success", responseBody = "{\"lifecycleEventContainerID\":\"eventContainerID\"}")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR,
			description = ResponseConstants.FAILED_TO_LOCATE_SERVICE),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR,
			description = ResponseConstants.SERVICE_NOT_ELASTIC) })
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}/timeout/{timeout}/set-instances",
	method = RequestMethod.POST)
	@PreAuthorize("isFullyAuthenticated() and hasPermission(#authGroups, 'deploy')")
	@ResponseBody
	public Map<String, Object> setServiceInstances(
			@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@PathVariable final int timeout,
			@RequestParam(value = "count", required = true) final int count,
			@RequestParam(value = "location-aware", required = true) final boolean locationAware)
					throws DSLException, RestErrorException {

		final Map<String, Object> returnMap = new HashMap<String, Object>();
		final String puName = ServiceUtils.getAbsolutePUName(applicationName,
				serviceName);
		final ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit(
				puName);
		if (pu == null) {
			throw new RestErrorException(
					ResponseConstants.FAILED_TO_LOCATE_SERVICE, serviceName);
		}

		if (permissionEvaluator != null) {
			String puAuthGroups = pu.getBeanLevelProperties().getContextProperties().
					getProperty(CloudifyConstants.CONTEXT_PROPERTY_AUTH_GROUPS);
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			CloudifyAuthorizationDetails authDetails = new CloudifyAuthorizationDetails(authentication);
			permissionEvaluator.verifyPermission(authDetails, puAuthGroups, "deploy");
		}

		final Properties contextProperties = pu.getBeanLevelProperties()
				.getContextProperties();
		final String elasticProp = contextProperties
				.getProperty(CloudifyConstants.CONTEXT_PROPERTY_ELASTIC);
		final String templateName = contextProperties
				.getProperty(CloudifyConstants.CONTEXT_PROPERTY_TEMPLATE);

		if (elasticProp == null || !Boolean.parseBoolean(elasticProp)) {
			throw new RestErrorException(ResponseConstants.SERVICE_NOT_ELASTIC,
					serviceName);
		}

		logger.info("Scaling " + puName + " to " + count + " instances");

		UUID eventContainerID;
		if (cloud == null) {
			if (isLocalCloud()) {
				// Manual scale by number of instances
				pu.scale(new ManualCapacityScaleConfigurer().memoryCapacity(
						512 * count, MemoryUnit.MEGABYTES).create());
			} else {
				// Eager scale (1 container per machine per PU)
				throw new RestErrorException(
						ResponseConstants.SET_INSTANCES_NOT_SUPPORTED_IN_EAGER);
			}
		} else {

			final CloudTemplate template = getComputeTemplate(cloud,
					templateName);
			final long cloudExternalProcessMemoryInMB = calculateExternalProcessMemory(
					cloud, template);
			// TODO - set-instances is not supported when the "shared" flag is
			// (CLOUDIFY-1158)
			// currently we fall back to the previous impl
			// CPU = 0 , memoery is calculated as usual. shared = false
			pu.scale(ElasticScaleConfigFactory.createManualCapacityScaleConfig(
					(int) (cloudExternalProcessMemoryInMB * count), 0,
					locationAware, true));
		}

		logger.log(Level.INFO, "Starting to poll for lifecycle events.");
		eventContainerID = startPollingForLifecycleEvents(serviceName,
				applicationName, count, false, timeout, TimeUnit.MINUTES);
		returnMap.put(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID,
				eventContainerID);

		return successStatus(returnMap);
	}

	/**
	 * Retrieves the tail of a service log. This method used the service name
	 * and instance id To retrieve the the instance log tail.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @param instanceId
	 *            The service instance id.
	 * @param numLines
	 *            The number of lines to tail.
	 * @return The last n lines of log of the requested service.
	 * @throws RestErrorException .
	 */
	@JsonRequestExample(requestBody = "{\"numLines\":10}")
	@JsonResponseExample(status = "success", responseBody = "\"log tail from container\"")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = ""),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_service") })
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}"
			+ "/instances/{instanceId}/tail", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getLogTailByInstanceId(
			@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@PathVariable final int instanceId,
			@RequestParam(value = "numLines", required = true) final int numLines)
					throws RestErrorException {

		final GridServiceContainer container = getContainerAccordingToInstanceId(
				applicationName, serviceName, instanceId);

		if (container == null) {
			final String absolutePuName = ServiceUtils.getAbsolutePUName(
					applicationName, serviceName);
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}
		final String logTailFromContainer = getLogTailFromContainer(container,
				numLines);

		return successStatus(logTailFromContainer);
	}

	/**
	 * Retrieves the tail of a service log. This method uses the service name
	 * and the instance host address to retrieve the instance log tail.
	 * Important: a machine might hold more than one service instance. In such a
	 * scenario, only one of the service instance logs will be tailed and
	 * returned.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @param hostAddress
	 *            The service instance's host address.
	 * @param numLines
	 *            The number of lines to tail.
	 * @return The last n lines of log of the requested service.
	 * @throws RestErrorException .
	 */
	@JsonRequestExample(requestBody = "{\"numLines\" : 10}")
	@JsonResponseExample(status = "success", responseBody = "\"numLines lines of log tail\"")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = ""),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_service") })
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}"
			+ "/address/{hostAddress}/tail", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getLogTailByHostAddress(
			@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@PathVariable final String hostAddress,
			@RequestParam(value = "numLines", required = true) final int numLines)
					throws RestErrorException {

		final GridServiceContainer container = getContainerAccordingToHostAddress(
				applicationName, serviceName, hostAddress);
		if (container == null) {
			final String absolutePuName = ServiceUtils.getAbsolutePUName(
					applicationName, serviceName);
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}
		final String logTail = getLogTailFromContainer(container, numLines);

		return successStatus(logTail);
	}

	/**
	 * Retrieves the log tail from all of the specified service's instances. To
	 * retrieve the the instance log tail.
	 * 
	 * @param applicationName
	 *            The application name.
	 * @param serviceName
	 *            The service name.
	 * @param numLines
	 *            The number of lines to tail.
	 * @return The last n lines of log from each service instance.
	 * @throws RestErrorException .
	 */
	@JsonRequestExample(requestBody = "{\"numLines\":10}")
	@JsonResponseExample(status = "success", responseBody = "\"instance log tail\"")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = ""),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, description = "failed_to_locate_service") })
	@RequestMapping(value = "applications/{applicationName}/services/{serviceName}"
			+ "/tail", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> getLogTailByServiceName(
			@PathVariable final String applicationName,
			@PathVariable final String serviceName,
			@RequestParam(value = "numLines", required = true) final int numLines)
					throws RestErrorException {

		final StringBuilder stringBuilder = new StringBuilder();
		final ProcessingUnit processingUnit = getProcessingUnit(
				applicationName, serviceName);
		if (processingUnit == null) {
			final String absolutePuName = ServiceUtils.getAbsolutePUName(
					applicationName, serviceName);
			logger.severe("Could not find service " + absolutePuName);
			return unavailableServiceError(absolutePuName);
		}

		String instanceLogTail;
		for (final ProcessingUnitInstance processingUnitInstance : processingUnit) {
			stringBuilder.append("service instance id #"
					+ processingUnitInstance.getInstanceId()
					+ System.getProperty("line.separator"));
			instanceLogTail = getLogTailFromContainer(
					processingUnitInstance.getGridServiceContainer(), numLines);
			stringBuilder.append(instanceLogTail);
		}

		return successStatus(stringBuilder.toString());
	}

	private String getLogTailFromContainer(
			final GridServiceContainer container, final int numLines) {
		int numberOfLinesToTail;
		final String msg = "tail is limited to no more than "
				+ MAX_NUMBER_OF_LINES_TO_TAIL_ALLOWED + " lines.";
		final boolean tailThresholdBreached = numLines > MAX_NUMBER_OF_LINES_TO_TAIL_ALLOWED;
		if (tailThresholdBreached) {
			logger.log(Level.INFO, msg);
			numberOfLinesToTail = MAX_NUMBER_OF_LINES_TO_TAIL_ALLOWED;
		} else {
			numberOfLinesToTail = numLines;
		}
		final LastNLogEntryMatcher matcher = LogEntryMatchers
				.lastN(numberOfLinesToTail);
		final LogEntries logEntries = container.logEntries(matcher);
		final StringBuilder sb = new StringBuilder();
		for (final LogEntry logEntry : logEntries) {
			sb.append(logEntry.getText());
			sb.append(System.getProperty("line.separator"));
		}
		if (tailThresholdBreached) {
			sb.append(msg);
		}
		return sb.toString();
	}

	private GridServiceContainer getContainerAccordingToInstanceId(
			final String applicationName, final String serviceName,
			final int instanceId) {

		final ProcessingUnit processingUnit = getProcessingUnit(
				applicationName, serviceName);
		for (final ProcessingUnitInstance processingUnitInstance : processingUnit) {
			if (processingUnitInstance.getInstanceId() == instanceId) {
				return processingUnitInstance.getGridServiceContainer();
			}
		}
		return null;
	}

	private ProcessingUnit getProcessingUnit(final String applicationName,
			final String serviceName) {
		final String absolutePUName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		final ProcessingUnit processingUnit = admin.getProcessingUnits()
				.getProcessingUnit(absolutePUName);
		if (processingUnit == null) {
			logger.log(Level.FINE, "a Processing unit with the name "
					+ absolutePUName + " was not found");
			return null;
		}
		return processingUnit;
	}

	private GridServiceContainer getContainerAccordingToHostAddress(
			final String applicationName, final String serviceName,
			final String hostAddress) {

		final Machine machine = admin.getMachines().getHostsByAddress()
				.get(hostAddress);
		if (machine == null) {
			logger.log(Level.FINE, "a machine with host address " + hostAddress
					+ " was not found in the cluster.");
			return null;
		}
		final String absolutePUName = ServiceUtils.getAbsolutePUName(
				applicationName, serviceName);
		final ProcessingUnitInstance[] processingUnitInstances = machine
				.getProcessingUnitInstances(absolutePUName);
		if (processingUnitInstances == null) {
			logger.log(Level.FINE, "a Processing unit instance with the name "
					+ absolutePUName + " was not found");
			return null;
		}

		for (final ProcessingUnitInstance instance : processingUnitInstances) {
			if (instance.getOperatingSystem().getDetails().getHostAddress()
					.equals(hostAddress)) {
				return instance.getGridServiceContainer();
			}
		}
		return null;
	}

	@Override
	public ServiceDetails[] getServicesDetails() {
		logger.info("Creating service details");
		String bindHost;
		try {
			bindHost = InetAddress.getLocalHost().getHostAddress();
		} catch (final UnknownHostException e) {
			logger.log(Level.SEVERE,
					"Failed to get local host, defaulting to 127.0.0.1", e);
			bindHost = "127.0.0.1";
		}

		final Map<String, Object> map = ServiceDetailsHelper
				.createCloudDetailsMap(bindHost);

		@SuppressWarnings("deprecation")
		final CustomServiceDetails csd = new CustomServiceDetails(
				CloudifyConstants.USM_DETAILS_SERVICE_ID,
				CustomServiceDetails.SERVICE_TYPE, "REST", "REST", "REST");

		final ServiceDetails[] res = new ServiceDetails[] { csd };

		final Map<String, Object> result = csd.getAttributes();
		result.putAll(map);

		logger.info("Service details created: " + result);
		return res;

	}

	/**
	 * Handle exceptions that originated from the deployment process.
	 * 
	 * @param e
	 *            The exception thrown
	 * @param pollingTaskId
	 *            The polling task Id
	 */
	public void handleDeploymentException(final Exception e, final UUID pollingTaskId) {
		if (pollingTaskId == null) {
			logger.log(Level.INFO, "No polling task was set for the deployment task. "
					+ "Aborting deployment exception handling.");
			return;
		}
		if (!this.lifecyclePollingThreadContainer.containsKey(pollingTaskId)) {
			logger.log(Level.FINE, "Polling task with UUID " + pollingTaskId.toString()
					+ " is no longer active.");
		} else {
			RestPollingRunnable restPollingRunnable = lifecyclePollingThreadContainer.get(pollingTaskId);
			restPollingRunnable.setDeploymentExecutionException(e);
		}
	}

	/**
	 * Add templates to the cloud.
	 * 
	 * @param templatesFolder
	 *            The templates zip file.
	 * @return a map containing the added templates and a success status if
	 *         succeeded, else returns an error status.
	 * @throws RestErrorException
	 *             in case of failing to add the template to the space.
	 * @throws IOException
	 *             in case of reading error.
	 * @throws DSLException
	 *             in case of failing to read a DSL object.
	 */
	@JsonRequestExample(requestBody = "{\"templatesFolder\" : \"templates folder\"}")
	@JsonResponseExample(status = "success", responseBody = "[\"template1\", \"template2\", \"template3\"]", 
	comments = "In case of failure a RestErrorException will be thrown and its args will contain two maps: " 
			+ "a map of hosts and foreach host its failed to add templates with their error reasons " 
			+ "(which is a map of template name and error description) " 
			+ "and a map of hosts and for each host its list of successfuly added templates.")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, 
			description = "Failed to add all the templates to all the REST instances.") })
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(value = "templates", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object> addTemplates(
			@RequestParam
			(value = CloudifyConstants.TEMPLATES_DIR_PARAM_NAME, required = true) final MultipartFile templatesFolder)
					throws IOException, DSLException, RestErrorException {
		if (cloud == null) {
			throw new RestErrorException("local_cloud_not_support_tempaltes_operations", "add-templates");
		}
		logger.log(Level.INFO, "[addTemplates] - starting add templates.");
		File loaclTemplatesZipFile = null;
		File unzippedTemplatesFolder = null;
		try {
			loaclTemplatesZipFile = copyMultipartFileToLocalFile(templatesFolder);
			unzippedTemplatesFolder = new CloudTemplatesReader().unzipCloudTemplatesFolder(loaclTemplatesZipFile);
			List<String> expectedTemplates = readCloudTemplatesNames(unzippedTemplatesFolder);

			Map<String, Map<String, String>> failedToAddTemplatesByHost = new HashMap<String, Map<String, String>>();
			Map<String, List<String>> addedTemplatesByHost = new HashMap<String, List<String>>();
			// add the templates to the remote PUs, update addedTemplatesByHost
			// and missingTemplatesByHost.
			sendAddTemplatesToRestInstances(loaclTemplatesZipFile, expectedTemplates,
					addedTemplatesByHost, failedToAddTemplatesByHost);

			// If some templates failed to be added, throw an exception
			if (!failedToAddTemplatesByHost.isEmpty()) {
				if (addedTemplatesByHost.isEmpty()) {
					logger.log(Level.WARNING, "[addTemplates] - Failed to add the following templates (by host): "
							+ failedToAddTemplatesByHost);
					throw new RestErrorException(CloudifyErrorMessages.FAILED_TO_ADD_TEMPLATES.getName(),
							failedToAddTemplatesByHost);
					
				} else {
					logger.log(Level.WARNING, "[addTemplates] - Failed to add the following templates (by host): "
							+ failedToAddTemplatesByHost + ".\nSuccessfully added templates (by host): " 
							+ addedTemplatesByHost);
					throw new RestErrorException(CloudifyErrorMessages.PARTLY_FAILED_TO_ADD_TEMPLATES.getName(),
							failedToAddTemplatesByHost, addedTemplatesByHost);
				}
			}

			logger.log(Level.INFO, "[addTemplates] - Successfully added templates: " + addedTemplatesByHost.toString());
			return successStatus(expectedTemplates);

		} finally {
			FileUtils.deleteQuietly(unzippedTemplatesFolder);
			FileUtils.deleteQuietly(loaclTemplatesZipFile);
		}
	}

	/**
	 * For each puInstance - send the templates folder.
	 * 
	 * @param templatesFolder
	 *            .
	 * @param expectedTemplates
	 *            The expected templates to add.
	 * @param addedTemplatesByHost
	 *            a map updates by this method to specify the failed to add
	 *            templates for each instance.
	 * @param failedToAddTemplatesByHost
	 *            a map updates by this method to specify the failed to add
	 *            templates for each instance.
	 */
	private void sendAddTemplatesToRestInstances(final File templatesFolder,
			final List<String> expectedTemplates, final Map<String, List<String>> addedTemplatesByHost,
			final Map<String, Map<String, String>> failedToAddTemplatesByHost) {

		// get the instances
		ProcessingUnitInstance[] instances = admin.getProcessingUnits().
				waitFor("rest", RestUtils.TIMEOUT_IN_SECOND, TimeUnit.SECONDS).getInstances();
		logger.log(Level.INFO, "[sendAddTemplatesToRestInstances] - sending templates folder to " 
				+ instances.length + " instances.");

		// send the templates folder to each rest instance (except the local
		// one)
		for (ProcessingUnitInstance puInstance : instances) {
			String hostAddress = puInstance.getMachine().getHostAddress();
			String host = puInstance.getMachine().getHostName() + "/" + hostAddress;
			Map<String, Object> response;
			try {
				// send the post request
				response = executePostRestRequest(templatesFolder, puInstance, "/service/templates/internal");
			} catch (Exception e) {
				logger.log(Level.WARNING, "[sendAddTemplatesToRestInstances] - failed to execute http request to " 
						+ host + ". Error: " + e, e);
				Map<String, String> expectedMap = new HashMap<String, String>();
				for (String expectedTemplate : expectedTemplates) {
					expectedMap.put(expectedTemplate, e.getMessage());
				}
				failedToAddTemplatesByHost.put(host, expectedMap);
				continue;
			}
			// update maps
			@SuppressWarnings("unchecked")
			Map<String, String> failedMap = (Map<String, String>) response.get(FAILED_TO_ADD_TEMPLATES_KEY);
			if (!failedMap.isEmpty()) {
				failedToAddTemplatesByHost.put(host, failedMap);
			}
			@SuppressWarnings("unchecked")
			List<String> addedTemplates = (List<String>) response.get(SUCCESSFULLY_ADDED_TEMPLATES_KEY);
			if (!addedTemplates.isEmpty()) {
				addedTemplatesByHost.put(host, addedTemplates);
			}
			// validate response list and
			if (!expectedTemplates.equals(addedTemplates)) {
				logger.log(Level.WARNING, "[sendAddTemplatesToRestInstances] - failed to add templates to " + host 
						+ ", expected: " + expectedTemplates.toString() + ", actual: " + addedTemplates.toString());
			}
			logger.log(Level.INFO, "[sendAddTemplatesToRestInstances] - successfully added " + addedTemplates.size()
					+ " templates to " + host + ": " + addedTemplates);
		}
	}

	/**
	 * Sends a delete request to puInstance.
	 * 
	 * @param puInstance
	 *            .
	 * @param hostAddress
	 *            .
	 * @param url
	 *            .
	 * @throws RestErrorException
	 *             If failed to execute the request or the response is not
	 *             successful.
	 */
	private void executeDeleteRestRequest(final ProcessingUnitInstance puInstance, 
			final String hostAddress, final String relativeUrl) 
					throws RestErrorException, RestException, MalformedURLException {
				
		String port = Integer.toString(puInstance.getJeeDetails().getPort());
		GSRestClient restClient = createRestClient(hostAddress, port, ""/*username*/, ""/*password*/);
		restClient.delete(relativeUrl);
	}

	/**
	 * Sends a post request to puInstance, posts the template folder.
	 * 
	 * @param templatesFolder
	 *            .
	 * @param puInstance
	 *            .
	 * @param hostAddress
	 *            .
	 * @param url
	 *            .
	 * @return the response.
	 * @throws RestErrorException
	 *             If failed to execute the request, the response is not
	 *             successful or the response is not a map.
	 * @throws IOException
	 *             If failed to post the folder.
	 */
	private Map<String, Object> executePostRestRequest(final File templatesFolder,
			final ProcessingUnitInstance puInstance, final String relativeUrl)
					throws RestErrorException, RestException, IOException {
		
		Object response = null;
		
		String hostAddress = puInstance.getMachine().getHostAddress();
		String host = puInstance.getMachine().getHostName() + "/" + hostAddress;
		String port = Integer.toString(puInstance.getJeeDetails().getPort());
		GSRestClient restClient = createRestClient(hostAddress, port, ""/*username*/, ""/*password*/);
		Map<String, File> fileMap = new HashMap<String, File>();
		fileMap.put(CloudifyConstants.TEMPLATES_DIR_PARAM_NAME, templatesFolder);
		response = restClient.postFiles(relativeUrl, fileMap);
		if (!(response instanceof Map)) {
			throw new RestErrorException("The response from host address " + host
					+ " is not a map as expected. " + "response: " + response.toString() + '.');				
		}
		
		return (Map<String, Object>) response;
	}

	/**
	 * Internal method. Add template files to the cloud configuration directory
	 * and to the cloud object. This method supposed to be invoked from
	 * addTemplates of a REST instance.
	 * 
	 * @param templatesFolder
	 *            The templates zip file.
	 * @return a map containing the added templates and a success status if
	 *         succeeded, else returns an error status.
	 * @throws RestErrorException
	 *             in case of failing to add the template to the space.
	 * @throws IOException
	 *             in case of reading error.
	 * @throws DSLException
	 *             in case of failing to read a DSL object.
	 */

	//@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(value = "templates/internal", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, Object>
	addTemplatesInternal(
			@RequestParam
			(value = CloudifyConstants.TEMPLATES_DIR_PARAM_NAME, required = true) final MultipartFile templatesFolder)
					throws IOException, DSLException, RestErrorException {
		CloudTemplatesReader reader = new CloudTemplatesReader();
		File localTemplatesFolder = reader.unzipCloudTemplatesFolder(copyMultipartFileToLocalFile(templatesFolder));
		try {
			logger.log(Level.INFO, "[addTemplatesInternal] - adding templates from templates folder: "
					+ localTemplatesFolder.getAbsolutePath());
			// add templates to the cloud and return the added templates.
			return successStatus(addTemplatesToCloud(localTemplatesFolder));
		} finally {
			FileUtils.deleteQuietly(localTemplatesFolder);
		}
	}

	/**
	 * Adds templates to cloud's templates. Adds templates' files to cloud
	 * configuration directory.
	 * 
	 * @param templatesFolder
	 *            .
	 * @return a map contains the added templates list and the failed to add
	 *         templates list.
	 * @throws RestErrorException
	 *             If failed to add templates. If failed to copy templates'
	 *             files to a new directory under the cloud configuration
	 *             directory.
	 * @throws DSLException
	 *             If failed to read templates files.
	 */
	private Map<String, Object> addTemplatesToCloud(final File templatesFolder)
			throws RestErrorException, DSLException {

		logger.log(Level.FINE, "[addTemplatesToCloud] - Adding templates to cloud.");

		// read cloud templates from templates folder
		List<CloudTemplateHolder> cloudTemplatesHolders = readCloudTemplates(templatesFolder);
		logger.log(Level.FINE, "[addTemplatesToCloud] - Successfully read " + cloudTemplatesHolders.size()
				+ " templates from folder - " + templatesFolder);

		// adds the templates to the cloud's templates list, deletes the failed to added templates from the folder.
		Map<String, String> failedToAddTemplates = new HashMap<String, String>();
		List<String> addedTemplates = new LinkedList<String>();
		addTemplatesToCloudList(templatesFolder, cloudTemplatesHolders, addedTemplates, failedToAddTemplates);
		// if no templates were added, throw an exception
		if (addedTemplates.isEmpty()) {
			logger.log(Level.WARNING, "[addTemplatesToCloud] - Failed to add templates files from "
					+ templatesFolder.getAbsolutePath());
		} else {
			// at least one template was added, copy files from template folder to cloudTemplateFolder
			logger.log(Level.FINE, "[addTemplatesToCloud] - Coping templates files from " 
					+ templatesFolder.getAbsolutePath() + " to " + cloudConfigurationDir.getAbsolutePath());
			try {
				File localTemplatesDir = copyTemplateFilesToCloudConfigDir(templatesFolder);
				updateCloudTemplatesUploadPath(addedTemplates, localTemplatesDir);
			} catch (IOException e) {
				// failed to copy files - remove all added templates from cloud and them to the failed map.
				logger.log(Level.WARNING, "[addTemplatesToCloud] - Failed to copy templates files, error: " 
						+ e.getMessage(), e);
				for (String templateName : addedTemplates) {
					cloud.getTemplates().remove(templateName);
					failedToAddTemplates.put(templateName, e.getMessage());
				}
			}
		}

		// return the added templates and the failed to add templates lists.
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put(FAILED_TO_ADD_TEMPLATES_KEY, failedToAddTemplates);
		resultMap.put(SUCCESSFULLY_ADDED_TEMPLATES_KEY, addedTemplates);
		if (!failedToAddTemplates.isEmpty()) {
			logger.log(Level.INFO, "[addTemplatesToCloud] - Failed to add the following templates: "
					+ failedToAddTemplates.toString());
		}
		return resultMap;
	}

	/**
	 * Updates the upload local path in all added cloud templates.
	 * 
	 * @param addedTemplates
	 *            the added templates.
	 * @param localTemplatesDir
	 *            the directory where the upload directory expected to be found.
	 */
	private void updateCloudTemplatesUploadPath(final List<String> addedTemplates, final File localTemplatesDir) {
		for (String templateName : addedTemplates) {
			CloudTemplate cloudTemplate = cloud.getTemplates().get(templateName);
			String localUploadPath = new File(localTemplatesDir, cloudTemplate.getLocalDirectory()).getAbsolutePath();
			cloudTemplate.setAbsoluteUploadDir(localUploadPath);
		}

	}

	/**
	 * Scans the cloudTemplatesHolders list and adds each template that doesn't
	 * already exist. Rename template's file if needed (if its prefix is not the
	 * template's name).
	 * 
	 * @param templatesFolder
	 *            the folder contains templates files.
	 * @param cloudTemplates
	 *            the list of cloud templates.
	 * @param addedTemplates
	 *            a list for this method to update with all the added templates.
	 * @param failedToAddTemplates
	 *            a list for this method to update with all the failed to add
	 *            templates.
	 */
	private void addTemplatesToCloudList(final File templatesFolder, final List<CloudTemplateHolder> cloudTemplates,
			final List<String> addedTemplates, final Map<String, String> failedToAddTemplates) {
		for (CloudTemplateHolder holder : cloudTemplates) {
			String templateName = holder.getName();
			String originalTemplateFileName = holder.getTemplateFileName();
			// check if template already exist
			if (cloud.getTemplates().containsKey(templateName)) {
				logger.log(Level.WARNING, "[addTemplatesToCloudList] - Template already exists: " + templateName);
				failedToAddTemplates.put(templateName, "template already exists");
				new File(templatesFolder, originalTemplateFileName).delete();
				continue;
			}
			// rename template file to <templateName>-template.groovy if needed
			// rename the proeprties and overrides files as well.
			try {
				renameTemplateFileIfNeeded(templatesFolder, holder);
			} catch (IOException e) {
				logger.log(Level.WARNING, "[addTemplatesToCloudList] - Failed to rename template's file, template: "
						+ templateName + ", error: " + e.getMessage(), e);
				failedToAddTemplates.put(templateName, "failed to rename template's file. error: " + e.getMessage());
				new File(templatesFolder, originalTemplateFileName).delete();
				continue;
			}
			// add template to cloud templates list
			CloudTemplate cloudTemplate = holder.getCloudTemplate();
			cloud.getTemplates().put(templateName, cloudTemplate);
			addedTemplates.add(templateName);
		}
	}

	/**
	 * If the original template's file name prefix is not the template's name,
	 * rename it.
	 * Also, rename the properties and overrides files if exist.
	 * 
	 * @param templatesFolder
	 *            the folder that contains the template's file.
	 * @param originalTemplateFileName
	 *            the original file name.
	 * @param templateName
	 *            the template's name.
	 * @throws IOException
	 *             If failed to rename.
	 */

	private void renameTemplateFileIfNeeded(final File templatesFolder, final CloudTemplateHolder holder) 
			throws IOException {
		String templateName = holder.getName();

		String templateFileName = holder.getTemplateFileName();
		File templateFile = new File(templatesFolder, templateFileName);
		String propertiesFileName = holder.getPropertiesFileName();
		String overridesFileName = holder.getOverridesFileName();

		try {
			String newName = DSLUtils.renameCloudTemplateFileNameIfNeeded(templateFile, templateName, 
					DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX);
			if (newName != null) {
				logger.log(Level.INFO, "[renameTemplateFileIfNeeded] - Renamed template file name from "
						+ templateFileName + " to " + newName + ".");
			}
			if (propertiesFileName != null) {
				File propertiesFile = new File(templatesFolder, propertiesFileName);
				newName = DSLUtils.renameCloudTemplateFileNameIfNeeded(propertiesFile, templateName, 
						DSLUtils.TEMPLATES_PROPERTIES_FILE_NAME_SUFFIX);
				if (newName != null) {
					logger.log(Level.INFO, "[renameTemplateFileIfNeeded] - Renamed template's properties file name from"
							+ " " + propertiesFileName + " to " + newName + ".");
				}
			}
			if (overridesFileName != null) {
				File overridesFile = new File(templatesFolder, overridesFileName);		
				newName = DSLUtils.renameCloudTemplateFileNameIfNeeded(overridesFile, templateName, 
						DSLUtils.TEMPLATES_OVERRIDES_FILE_NAME_SUFFIX);
				if (newName != null) {
					logger.log(Level.INFO, "[renameTemplateFileIfNeeded] - Renamed template's overrides file name from "
							+ overridesFileName + " to " + newName + ".");
				}
			}
		} catch (IOException e) {
			logger.log(Level.WARNING, "[renameTemplateFileIfNeeded] - Failed to rename template file name ["
					+ templateFile.getName() + "] to "
					+ templateName + DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX
					+ ". The file will be deleted. Error:" + e);
			// delete the groovy file to ensure the template file wont be
			// copied.
			templateFile.delete();
			throw e;
		}
	}

	/**
	 * Gets the {@link CloudifyConstants#ADDITIONAL_TEMPLATES_FOLDER_NAME}
	 * folder. Creates it if needed.
	 * 
	 * @return the folder.
	 */
	private File getTemplatesFolder() {
		File templatesFolder = new File(cloudConfigurationDir,
				CloudifyConstants.ADDITIONAL_TEMPLATES_FOLDER_NAME);
		if (!cloudConfigurationDir.exists()) {
			templatesFolder.mkdir();
		}
		return templatesFolder;
	}

	/**
	 * Reads the templates from templatesFolder.
	 * 
	 * @param templatesFolder
	 *            .
	 * @return the list of the read cloud templates.
	 * @throws RestErrorException
	 *             If no templates files were found.
	 * @throws DSLException
	 *             If failed to read templates.
	 */
	private List<CloudTemplateHolder> readCloudTemplates(final File templatesFolder)
			throws RestErrorException, DSLException {
		List<CloudTemplateHolder> cloudTemplatesHolders;
		CloudTemplatesReader reader = new CloudTemplatesReader();
		cloudTemplatesHolders = reader.readCloudTemplatesFromDirectory(templatesFolder);
		if (cloudTemplatesHolders.isEmpty()) {
			throw new RestErrorException("no_template_files", "templates folder missing templates files." , 
					templatesFolder.getAbsolutePath());
		}
		return cloudTemplatesHolders;
	}

	/**
	 * Reads the templates from templatesFolder.
	 * 
	 * @param templatesFolder
	 *            .
	 * @return the list of the read cloud templates.
	 * @throws RestErrorException
	 *             If no templates files were found.
	 * @throws DSLException
	 *             If failed to read templates.
	 */
	private List<String> readCloudTemplatesNames(final File templatesFolder)
			throws RestErrorException, DSLException {
		List<CloudTemplateHolder> cloudTemplatesHolders = readCloudTemplates(templatesFolder);
		List<String> cloudTemplateNames = new LinkedList<String>();
		for (CloudTemplateHolder cloudTemplateHolder : cloudTemplatesHolders) {
			cloudTemplateNames.add(cloudTemplateHolder.getName());
		}
		return cloudTemplateNames;
	}

	/**
	 * Copies all the files from templatesFolder to a new directory under cloud
	 * configuration directory.
	 * 
	 * @param templatesDirToCopy
	 *            the directory contains all the files to copy.
	 * @param templatesDirParent
	 *            the {@link CloudifyConstants#ADDITIONAL_TEMPLATES_FOLDER_NAME}
	 *            directory that will be the parent directory of the new
	 *            template folder.
	 * @throws IOException
	 *             If failed to copy files.
	 */
	private File copyTemplateFilesToCloudConfigDir(final File templatesDirToCopy)
			throws IOException {
		File templatesDirParent = getTemplatesFolder();
		// create new templates folder - increment folder number until no folder
		// with that name exist.
		String folderName = "templates_" + lastTemplateFileNum.incrementAndGet();
		File copiedtemplatesFolder = new File(templatesDirParent, folderName);
		while (copiedtemplatesFolder.exists()) {
			folderName = "templates_" + lastTemplateFileNum.incrementAndGet();
			copiedtemplatesFolder = new File(templatesDirParent, folderName);
		}
		copiedtemplatesFolder.mkdir();
		try {
			FileUtils.copyDirectory(templatesDirToCopy, copiedtemplatesFolder);
			return copiedtemplatesFolder;
		} catch (IOException e) {
			FileUtils.deleteDirectory(copiedtemplatesFolder);
			lastTemplateFileNum.decrementAndGet();
			throw e;
		}
	}

	/**
	 * Get the cloud's templates.
	 * 
	 * @return a map containing the cloud's templates and a success status.
	 * @throws RestErrorException
	 *             If cloud is a local cloud.
	 */
	@RequestMapping(value = "templates", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS', 'ROLE_APPMANAGERS')")
	public @ResponseBody
	Map<String, Object>
	listTemplates() throws RestErrorException {
		if (cloud == null) {
			throw new RestErrorException("local_cloud_not_support_tempaltes_operations", "list-templates");
		}
		return successStatus(cloud.getTemplates());
	}

	/**
	 * Get template from the cloud.
	 * 
	 * @param templateName
	 *            The name of the template to remove.
	 * @return a map containing the template and a success status if succeeded,
	 *         else returns an error status.
	 * @throws RestErrorException
	 *             if the cloud is a local cloud or the template doesn't exist.
	 */
	@RequestMapping(value = "templates/{templateName}", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS', 'ROLE_APPMANAGERS')")
	public @ResponseBody
	Map<String, Object>
	getTemplate(@PathVariable final String templateName)
			throws RestErrorException {

		if (cloud == null) {
			throw new RestErrorException("local_cloud_not_support_tempaltes_operations", "get-template");
		}

		// get template from cloud
		CloudTemplate cloudTemplate = cloud.getTemplates().get(templateName);

		if (cloudTemplate == null) {
			logger.log(Level.WARNING, "[getTemplate] - template [" + templateName 
					+ "] not found. cloud templates list: " + cloud.getTemplates());
			throw new RestErrorException("template_not_exist", templateName);
		}
		return successStatus(cloudTemplate);
	}

	/**
	 * Removes a template from the cloud.
	 * 
	 * @param templateName
	 *            The name of the template to remove.
	 * @return success status map if succeeded.
	 * @throws RestErrorException
	 *             If cloud is a local cloud or one of the REST instances failed
	 *             to remove the template.
	 */
	@JsonResponseExample(status = "success", 
	comments = "In case of failure a RestErrorException will be thrown " 
			+ "and its args will contain the list of all the host that failed to remove the template.")
	@PossibleResponseStatuses(responseStatuses = {
			@PossibleResponseStatus(code = HTTP_OK, description = "success"),
			@PossibleResponseStatus(code = HTTP_INTERNAL_SERVER_ERROR, 
			description = "Failed to remove the template from all the REST instances.") })
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(value = "templates/{templateName}", method = RequestMethod.DELETE)
	public @ResponseBody
	Map<String, Object>
	removeTemplate(@PathVariable final String templateName)
			throws RestErrorException {

		if (cloud == null) {
			throw new RestErrorException("local_cloud_not_support_tempaltes_operations", "remove-template");
		}
		logger.log(Level.INFO, "[removeTemplate] - removing template " + templateName);

		// check if the template is being used by at least one service, so it cannot be removed.
		final List<String> templateServices = getTemplateServices(templateName);
		if (!templateServices.isEmpty()) {
			logger.log(Level.WARNING, "[removeTemplate] - failed to remove template [" + templateName 
					+ "]. The template is being used by " + templateServices.size() + " services: " + templateServices);
			throw new RestErrorException(CloudifyErrorMessages.TEMPLATE_IN_USE.getName(), 
					templateName, templateServices);
		}

		// remove template from REST instances (including local MNG).
		List<String> successfullyRemoved = new LinkedList<String>();
		List<String> failedToRemoveHosts = new LinkedList<String>();
		removeTemplateFromRestInstances(templateName, successfullyRemoved, failedToRemoveHosts);

		// check if some REST instances failed to remove the template
		if (!failedToRemoveHosts.isEmpty()) {
			String message = "[removeTemplate] - failed to remove template [" + templateName + "] from: "
					+ failedToRemoveHosts;
			if (!successfullyRemoved.isEmpty()) {
				message += ". Succeeded to remove the template from: " + successfullyRemoved;
			}
			
			logger.log(Level.WARNING, message);
			throw new RestErrorException("failed_to_remove_template", templateName, failedToRemoveHosts.toString());
		}

		// return success
		logger.log(Level.INFO, "[removeTemplate] - Succeeded to remove template [" + templateName + "] from: " 
				+ successfullyRemoved);
		
		return successStatus();
	}

	/**
	 * For each REST instance- remove the template.
	 * 
	 * @param templateName
	 *            the name of the template.
	 * @param successfullyRemoved
	 *            a list that this method updates with the host that
	 *            successfully remove the template.
	 * @param failedToRemoveHosts
	 *            a list that this method updates with the host that failed to
	 *            remove the template.
	 */
	private void removeTemplateFromRestInstances(final String templateName,
			final List<String> successfullyRemoved, final List<String> failedToRemoveHosts) {
		// get rest instances
		ProcessingUnit processingUnit =
				admin.getProcessingUnits().waitFor("rest", RestUtils.TIMEOUT_IN_SECOND, TimeUnit.SECONDS);
		ProcessingUnitInstance[] instances = processingUnit.getInstances();

		// send the template's name to remove to each rest instance (except the
		// local one)
		logger.log(Level.INFO, "[removeTemplateFromRestInstances] - sending remove request to "
				+ instances.length + " REST instances. Template's name is " + templateName);
		for (ProcessingUnitInstance puInstance : instances) {
			String hostAddress = puInstance.getMachine().getHostAddress();
			String host = puInstance.getMachine().getHostName() + "/" + hostAddress;
			// execute the http request
			try {
				executeDeleteRestRequest(puInstance, hostAddress, "/service/templates/internal/" + templateName);
			} catch (Exception e) {
				failedToRemoveHosts.add(host);
				logger.log(Level.WARNING, "[removeTemplateFromRestInstances] - Failed to execute http request to " 
						+ host + ". Error: " + e.getMessage(), e);
				continue;
			}
			successfullyRemoved.add(host);
			logger.log(Level.INFO, "[removeTemplateFromRestInstances] - Successfully removed template ["
					+ templateName + "] from " + host);
		}
	}

	/**
	 * Internal method. Remove template file from the cloud configuration
	 * directory and from the cloud's templates map. This method supposed to be
	 * invoked from removeTemplate of a REST instance.
	 * 
	 * @param templateName
	 *            the name of the template to remove.
	 * @return success map.
	 * @throws RestErrorException
	 *             If failed to remove template.
	 */
	//@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	@RequestMapping(value = "templates/internal/{templateName}", method = RequestMethod.DELETE)
	public @ResponseBody
	Map<String, Object>
	removeTemplateInternal(@PathVariable final String templateName)
			throws RestErrorException {
		logger.log(Level.INFO, "removeTemplateInternal - removing template [" + templateName + "].");
		// check if the template is being used by at least one service, so it cannot be removed.
		final List<String> templateServices = getTemplateServices(templateName);
		if (!templateServices.isEmpty()) {
			logger.log(Level.WARNING, "[removeTemplate] - failed to remove template [" + templateName 
					+ "]. The template is being used by the following services: " + templateServices);
			throw new RestErrorException(CloudifyErrorMessages.TEMPLATE_IN_USE.getName(), 
					templateName, templateServices);
		}
		// try to remove the template
		try {
			removeTemplateFromCloud(templateName);
		} catch (RestErrorException e) {
			logger.log(Level.WARNING, "[removeTemplateInternal] - failed to remove template [" + templateName + "]."
					+ " Error: " + e.getMessage(), e);
			throw e;
		}
		logger.log(Level.INFO, "removeTemplateInternal- Successfully removed template [" + templateName + "].");
		return successStatus();

	}

	/**
	 * Removes the template from the cloud. Deletes the template's file.
	 * 
	 * @param templateName
	 *            the template's name.
	 * @throws RestErrorException
	 *             If failed to remove the template.
	 */
	private void removeTemplateFromCloud(final String templateName)
			throws RestErrorException {

		logger.log(Level.INFO, "[removeTemplateFromCloud] - removing template [" + templateName + "] from cloud.");

		// delete template's file from the cloud configuration directory.
		deleteTemplateFile(templateName);

		// remove template from cloud
		Map<String, CloudTemplate> cloudTemplates = cloud.getTemplates();
		if (!cloudTemplates.containsKey(templateName)) {
			throw new RestErrorException("tamplate_not_exist", templateName);
		}
		cloudTemplates.remove(templateName);
	}

	/**
	 * Deletes the tempalte's file. Deletes the templates folder if no other
	 * templates files exist in the folder. Deletes the
	 * {@link CloudifyConstants#ADDITIONAL_TEMPLATES_FOLDER_NAME} folder if
	 * empty.
	 * 
	 * @param templateName
	 * @throws RestErrorException
	 */
	private void deleteTemplateFile(final String templateName) throws RestErrorException {
		File templateFile = getTemplateFile(templateName);
		if (templateFile == null) {
			throw new RestErrorException("failed_to_remove_template_file", templateName,
					"template file doesn't exist");
		}
		// delete the file from the directory.
		String templatesPath = templateFile.getAbsolutePath();
		logger.log(Level.FINE, "[deleteTemplateFile] - removing template file " + templatesPath);
		boolean deleted = false;
		try {
			deleted = templateFile.delete();
		} catch (SecurityException e) {
			logger.log(Level.WARNING, "[deleteTemplateFile] - Failed to deleted template file " + templatesPath
					+ ", Error: " + e.getMessage(), e);
			throw new RestErrorException("failed_to_remove_template_file", templatesPath, e.getMessage());
		}
		if (!deleted) {
			throw new RestErrorException("failed_to_remove_template_file", templatesPath,
					"template file was not deleted.");
		}
		logger.log(Level.FINE, "[deleteTemplateFile] - Successfully deleted template file [" + templatesPath + "].");
		File templateFolder = templateFile.getParentFile();
		File[] templatesFiles = DSLReader.findDefaultDSLFiles(DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX, templateFolder);
		if (templatesFiles == null || templatesFiles.length == 0) {
			try {
				logger.log(Level.FINE, "[deleteTemplateFile] - templates folder is empty, deleting the folder [" 
						+ templatesPath + "].");
				FileUtils.deleteDirectory(templateFolder);
			} catch (IOException e) {
				logger.log(Level.WARNING, "[deleteTemplateFile] - Failed to delete templates folder" 
						+ templateFolder, e);
			}
		} else {
			// delete properties and overrides files if exist.
			CloudTemplatesReader.removeTemplateFiles(templateFolder, templateName);
		}
		File templatesFolder = getTemplatesFolder();
		if (templatesFolder.list().length == 0) {
			templateFolder.delete();
		}
	}

	/**
	 * Gets the template's file. Scans all templates folders in
	 * {@link CloudifyConstants#ADDITIONAL_TEMPLATES_FOLDER_NAME} directory,
	 * searches for a file with file name templateName-template.groovy.
	 * 
	 * @param templateName
	 *            the name of the template (also the prefix of the wanted file).
	 * @return the found file or null.
	 */
	private File getTemplateFile(final String templateName) {
		final String templateFileName = templateName + DSLUtils.TEMPLATE_DSL_FILE_NAME_SUFFIX;

		File templatesFolder = getTemplatesFolder();
		File[] templatesFolders = templatesFolder.listFiles();
		for (final File templateFolder : templatesFolders) {
			logger.log(Level.FINE, "Searching for template file " + templateFileName + " in "
					+ templateFolder.getAbsolutePath());
			File[] listFiles = templateFolder.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(final File dir, final String name) {
					return templateFileName.equals(name);
				}
			});
			int length = listFiles.length;
			if (length == 0) {
				logger.log(Level.WARNING, "Didn't find template file with name " + templateName + " at "
						+ templateFolder.getAbsolutePath());
				continue;
			}
			if (length > 1) {
				logger.log(Level.WARNING, "Found " + length + " templates files with name " + templateName
						+ ": " + Arrays.toString(listFiles) + ". Returning the first one found.");
			}
			return listFiles[0];
		}
		return null;
	}

	private List<String> getTemplateServices(final String templateName) {
		List<String> services = new LinkedList<String>();
		ProcessingUnits processingUnits = admin.getProcessingUnits();
		for (ProcessingUnit processingUnit : processingUnits) {
			Properties puProps = processingUnit.getBeanLevelProperties().getContextProperties();
			final String puTemplateName = puProps.getProperty(CloudifyConstants.CONTEXT_PROPERTY_TEMPLATE);
				if (puTemplateName != null && puTemplateName.equals(templateName)) {
					services.add(processingUnit.getName());
				}
		}
		return services;
	}
	
	/**
	 * Returns a valid response if the user is fully authorized and has permissions
	 * for installing an application.
	 * 
	 * @param applicationName 
	 * 		the application name.
	 * @return 
	 * 			a valid response if the user is fully authorized and has permissions
	 * 			for installing an application.
	 * @throws RestErrorException 
	 * 			in-case the application name is already taken by a different group.
	 */
	@RequestMapping(value = "application/{applicationName}/install/permissions", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS', 'ROLE_APPMANAGERS')")
	@ResponseBody public Map<String, Object> hasInstallPermissions(
			@PathVariable final String applicationName) throws RestErrorException {
		if (admin.getApplications().getNames().containsKey(applicationName)) {
			throw new RestErrorException(ResponseConstants.APPLICATION_NAME_IS_ALREADY_IN_USE, applicationName);
		}
		return successStatus();
	}
	
	/**
	 * Returns the name of the protocol used for communication with the rest server.
	 * If the security is secure (SSL) returns "https", otherwise returns "http".
	 * @param isSecureConnection Indicates whether SSL is used or not.
	 * @return "https" if this is a secure connection, "http" otherwise.
	 */
	private static String getRestProtocol(final boolean isSecureConnection) {
		if (isSecureConnection) {
			return "https";
		} else {
			return "http";
		}
	}
	
	private static GSRestClient createRestClient(final String host, final String port, final String username, 
			final String password) throws RestException, MalformedURLException {
		String protocol = getRestProtocol(IS_SECURE_CONNECTION);
		String baseUrl = protocol + "://" + host + ":" + port;
		String versionName = PlatformVersion.getVersion() + "-Cloudify-" + PlatformVersion.getMilestone();
		return new GSRestClient(new UsernamePasswordCredentials(username, password), new URL(baseUrl), versionName);
	}
}
