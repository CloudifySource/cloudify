/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.controllers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.dsl.rest.response.GetMachineDumpFileResponse;
import org.cloudifysource.dsl.rest.response.GetMachinesDumpFileResponse;
import org.cloudifysource.dsl.rest.response.GetPUDumpFileResponse;
import org.cloudifysource.dsl.rest.response.ShutdownManagementResponse;
import org.cloudifysource.rest.ResponseConstants;
import org.cloudifysource.rest.RestConfiguration;
import org.cloudifysource.rest.validators.DumpMachineValidationContext;
import org.cloudifysource.rest.validators.DumpMachineValidator;
import org.hyperic.sigar.Sigar;
import org.openspaces.admin.Admin;
import org.openspaces.admin.dump.DumpResult;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.gigaspaces.internal.dump.pu.ProcessingUnitsDumpProcessor;
import com.gigaspaces.internal.sigar.SigarHolder;

/**
 * This controller is responsible for retrieving information about management machines. It is also the entry point for
 * shutdown managers. <br>
 * <br>
 * The response body will always return in a JSON representation of the
 * {@link org.cloudifysource.dsl.rest.response.Response} Object. <br>
 * A controller method may return the {@link org.cloudifysource.dsl.rest.response.Response} Object directly. in this
 * case this return value will be used as the response body. Otherwise, an implicit wrapping will occur. the return
 * value will be inserted into {@code Response#setResponse(Object)}. other fields of the
 * {@link org.cloudifysource.dsl.rest.response.Response} object will be filled with default values. <br>
 * <h1>Important</h1> {@code @ResponseBody} annotations are not permitted. <br>
 * <br>
 * <h1>Possible return values</h1> 200 - OK<br>
 * 400 - controller throws an exception<br>
 * 500 - Unexpected exception<br>
 * <br>
 * 
 * @see {@link org.cloudifysource.rest.interceptors.ApiVersionValidationAndRestResponseBuilderInterceptor}
 * @author yael
 * @since 2.7.0
 */
@Controller
@RequestMapping(value = "/{version}/management")
public class ManagementController extends BaseRestController {

	private static final Logger logger = Logger.getLogger(ManagementController.class.getName());

	private static final int MANAGEMENT_PUI_LOOKUP_TIMEOUT = 10;
	protected static final int MANAGEMENT_AGENT_SHUTDOWN_INTERNAL_SECONDS = 5;

	@Autowired
	private RestConfiguration restConfig;

	@Autowired
	private DumpMachineValidator[] dumpValidators = new DumpMachineValidator[0];

	private Admin admin;
	private Cloud cloud;

	/**
	 * Initialization.
	 */
	@PostConstruct
	public void init() {
		this.admin = restConfig.getAdmin();
		this.cloud = restConfig.getCloud();
	}

	/**
	 * Schedules termination of all agents running the cloudify manager.
	 * 
	 * @return {@link org.cloudifysource.dsl.rest.response.ShutdownManagementResponse}
	 * @throws RestErrorException 
	 */
	@RequestMapping(value = "/controllers", method = RequestMethod.DELETE)
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	public ShutdownManagementResponse shutdownManagers() throws RestErrorException {
		if (this.cloud == null) {
			throw new RestErrorException(
					CloudifyErrorMessages.MANAGEMENT_SERVERS_SHUTDOWN_NOT_ALLOWED_ON_LOCALCLOUD.getName());
		}

		final ProcessingUnitInstance[] instances = getManagementInstances();

		final ControllerDetails[] controllers = createControllerDetails(instances);
		log(Level.INFO, "[shutdownManagers] - Controllers will be shut down in the following order: "
				+ Arrays.toString(instances) + ". IP of current node is: " + System.getenv("NIC_ADDR"));

		final ShutdownManagementResponse resposne = new ShutdownManagementResponse();
		resposne.setControllers(controllers);

		// IMPORTANT: we are using a new thread and not the thread pool so that in case
		// of the thread pool being overtaxed, this action will still be executed.
		final GridServiceAgent[] agents = getAgents(instances);
		new Thread(new Runnable() {

			@Override
			public void run() {
				log(Level.INFO, "[shutdownManagers] - Shutdown of management agent will commence in: "
						+ MANAGEMENT_AGENT_SHUTDOWN_INTERNAL_SECONDS + " seconds");
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(MANAGEMENT_AGENT_SHUTDOWN_INTERNAL_SECONDS));
				} catch (final InterruptedException e) {
					// ignore
				}

				log(Level.INFO, "[shutdownManagers] - Initiating shutdown of management agents");
				for (final GridServiceAgent agent : agents) {
					log(Level.INFO, "[shutdownManagers] - Shutting down agent: " + agent.getUid() + " at "
							+ agent.getMachine().getHostAddress() + "/" + agent.getMachine().getHostAddress());
					try {
						agent.shutdown();
					} catch (final Exception e) {
						log(Level.WARNING, "[shutdownManagers] - Attempt to shutdown management agent failed: "
								+ e.getMessage(), e);
					}
				}

			}

		}).start();

		return resposne;
	}

	private GridServiceAgent[] getAgents(final ProcessingUnitInstance[] instances) {
		final GridServiceAgent[] agents = new GridServiceAgent[instances.length];
		for (int i = 0; i < instances.length; i++) {
			final ProcessingUnitInstance instance = instances[i];
			final GridServiceAgent agent = instance.getGridServiceContainer().getGridServiceAgent();
			if (agent == null) {
				throw new IllegalStateException("Failed to find agent for management instance: "
						+ instance.getProcessingUnitInstanceName());
			}
			agents[i] = agent;
		}

		return agents;
	}

	private ControllerDetails[] createControllerDetails(final ProcessingUnitInstance[] instances) {
		final ControllerDetails[] controllers = new ControllerDetails[instances.length];

		boolean bootstrapToPublicIp = false;
		if (this.cloud != null) {
			bootstrapToPublicIp = this.cloud.getConfiguration().isBootstrapManagementOnPublicIp();
		}

		for (int i = 0; i < instances.length; i++) {
			controllers[i] = new ControllerDetails();
			final ProcessingUnitInstance instance = instances[i];
			final Map<String, String> env = instance.getVirtualMachine().getDetails().getEnvironmentVariables();
			final String privateIp = env.get(CloudifyConstants.GIGASPACES_AGENT_ENV_PRIVATE_IP);
			final String publicIp = env.get(CloudifyConstants.GIGASPACES_AGENT_ENV_PUBLIC_IP);

			controllers[i].setPrivateIp(privateIp);
			controllers[i].setPublicIp(publicIp);
			controllers[i].setInstanceId(instance.getInstanceId());

			controllers[i].setBootstrapToPublicIp(bootstrapToPublicIp);
		}
		return controllers;
	}

	private ProcessingUnitInstance[] getManagementInstances() throws RestErrorException {
		int expectedManagers = 1;
		if (this.cloud != null) {
			expectedManagers = this.cloud.getProvider().getNumberOfManagementMachines();
		}

		if (this.admin == null) {
			throw new IllegalStateException("Admin is null");
		}

		final ProcessingUnit pu = admin.getProcessingUnits().getProcessingUnit("rest");
		if (pu == null) {
			throw new IllegalStateException("Cannot find rest PU in admin API");
		}

		pu.waitFor(expectedManagers, MANAGEMENT_PUI_LOOKUP_TIMEOUT, TimeUnit.SECONDS);

		final ProcessingUnitInstance[] instances = pu.getInstances();
		if (instances.length != expectedManagers) {
			throw new RestErrorException(CloudifyErrorMessages.MANAGEMENT_SERVERS_NUMBER_NOT_MATCH.getName(),
					expectedManagers, instances.length);
		}

		// Sort the instances so the last element it the PUI for the current PUI, so it will be the last to be shut
		// down.
		sortInstances(instances);

		if (logger.isLoggable(Level.INFO)) {
			logger.info("[getManagementInstances] - Shutdown Order is: ");
			for (final ProcessingUnitInstance instance : instances) {
				logger.info(instance.getMachine().getHostAddress());
			}
		}
		return instances;
	}

	private void sortInstances(final ProcessingUnitInstance[] instances) {
		final Sigar sigar = SigarHolder.getSigar();
		final long myPid = sigar.getPid();
		logger.fine("PID of current process is: " + myPid);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("[sortInstances] - Original Order is: ");
			for (final ProcessingUnitInstance instance : instances) {
				logger.fine(instance.getMachine().getHostAddress());
			}
		}

		// sort instances so last one is the current one
		Arrays.sort(instances, new Comparator<ProcessingUnitInstance>() {

			@Override
			public int compare(final ProcessingUnitInstance o1, final ProcessingUnitInstance o2) {
				final long pid1 =
						o1.getGridServiceContainer().getVirtualMachine().getDetails().getPid();
				final long pid2 =
						o2.getGridServiceContainer().getVirtualMachine().getDetails().getPid();

				if (pid1 == myPid) {
					return 1;
				} else if (pid2 == myPid) {
					return -1;
				} else {
					return 0;
				}
			}

		});
	}

	/**
	 * 
	 */
	@RequestMapping(value = "/controllers", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	public void getManagers() {
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param fileSizeLimit
	 *            the file size limit.
	 * @return GetPUDumpFileResponse containing the dump of all the processing units
	 * @throws RestErrorException 
	 */
	@RequestMapping(value = "/dump/processing-units", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CLOUDADMINS')")
	public GetPUDumpFileResponse getPUDumpFile(
			@RequestParam(defaultValue = "" + CloudifyConstants.DEFAULT_DUMP_FILE_SIZE_LIMIT) final long fileSizeLimit) 
			throws RestErrorException {
		log(Level.INFO, "[getPUDumpFile] - generating dump file of all the processing units");
		final DumpResult dump = admin.generateDump("Rest Service user request",
				null, ProcessingUnitsDumpProcessor.NAME);
		byte[] data = getDumpRawData(dump, fileSizeLimit);
		final GetPUDumpFileResponse response = new GetPUDumpFileResponse();
		response.setDumpData(data);
		return response;
	}

	
	/**
	 * Get the dump of a given machine, by its IP.
	 *
	 * @param ip
	 *            The machine IP.
	 * @param processors
	 *            The list of processors to be used.
	 * @param fileSizeLimit
	 *            The dump file size limit.
	 * @return A byte array of the dump file.
	 * @throws RestErrorException .
	 *
	 */
	@RequestMapping(value = "/dump/machine/{ip}", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CLOUDADMINS')")
	public GetMachineDumpFileResponse getMachineDumpFile(
			@PathVariable final String ip,
			@RequestParam(required = false) final String[] processors,
			@RequestParam(defaultValue = "" + CloudifyConstants.DEFAULT_DUMP_FILE_SIZE_LIMIT) final long fileSizeLimit)
					throws RestErrorException {
		
		// validate
		validateGetMachineDump(processors);

		String[] actualProcessors = processors;
		if (processors == null) {
			actualProcessors = CloudifyConstants.DEFAULT_DUMP_PROCESSORS;
		}
		
		// first find the relevant agent
		Machine machine = this.admin.getMachines().getHostsByAddress().get(ip);
		if (machine == null) {
			throw new RestErrorException(
					CloudifyErrorMessages.MACHINE_NOT_FOUND.getName(), ip);
		}
		final byte[] dumpBytes = generateMachineDumpData(fileSizeLimit,
				machine, actualProcessors);

		GetMachineDumpFileResponse response = new GetMachineDumpFileResponse();
		response.setDumpBytes(dumpBytes);
		return response;
	}

	/**
	 * Get the dump of all machines.
	 *
	 * @param processors
	 *            The list of processors to be used.
	 * @param fileSizeLimit
	 *            The dump file size limit.
	 * @return GetMachinesDumpFileResponse containing a map from machine IP to its dump file in byte array.
	 * @throws RestErrorException 
	 *
	 */
	@RequestMapping(value = "/dump/machines", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasRole('ROLE_CLOUDADMINS')")
	public GetMachinesDumpFileResponse getMachinesDumpFile(
					@RequestParam final String[] processors,
					@RequestParam(defaultValue = "" + CloudifyConstants.DEFAULT_DUMP_FILE_SIZE_LIMIT) 
					final long fileSizeLimit) throws RestErrorException {

		validateGetMachineDump(processors);

		String[] actualProcessors = processors;
		if (processors == null) {
			actualProcessors = CloudifyConstants.DEFAULT_DUMP_PROCESSORS;
		}

		long totalSize = 0;
		final Iterator<Machine> iterator = this.admin.getMachines()
				.iterator();
		final Map<String, byte[]> map = new HashMap<String, byte[]>();
		while (iterator.hasNext()) {
			final Machine machine = iterator.next();

			final byte[] dumpBytes = generateMachineDumpData(fileSizeLimit,
					machine, actualProcessors);
			totalSize += dumpBytes.length;
			if (totalSize > fileSizeLimit) {
				throw new RestErrorException(
						ResponseConstants.DUMP_FILE_TOO_LARGE,
						Long.toString(dumpBytes.length),
						Long.toString(totalSize));
			}
			map.put(machine.getHostAddress(), dumpBytes);
		}

		GetMachinesDumpFileResponse response = new GetMachinesDumpFileResponse();
		response.setDumpBytesPerIP(map);
		return response;
	}
	
	private byte[] generateMachineDumpData(final long fileSizeLimit,
			final Machine machine, final String[] actualProcessors)
					throws RestErrorException {
		// generator the dump
		final DumpResult dump = machine.generateDump("Rest_API", null,
				actualProcessors);

		final byte[] data = getDumpRawData(dump, fileSizeLimit);
		return data;

	}

	private String[] getProcessorsFromRequest(final String processors) {
		final String[] parts = processors.split(",");

		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}

		return parts;
	}

	private byte[] getDumpRawData(final DumpResult dump,
			final long fileSizeLimit) throws RestErrorException {
		File target;
		log(Level.INFO, "[getDumpRawData] - downloading the dump into a temporary file");
		try {
			target = File.createTempFile("dump", ".zip", restConfig.getRestTempFolder());
		} catch (IOException e) {
			log(Level.INFO, "[getDumpRawData] - failed to create temp file for storing the dump file. error was: " 
					+ e.getMessage());
			throw new RestErrorException(CloudifyErrorMessages.FAILED_CREATE_DUMP_FILE.getName(), 
					"failed to create temporary file [" + e.getMessage() + "]");
		}
		target.deleteOnExit();

		dump.download(target, null);

		try {
			if (target.length() >= fileSizeLimit) {
				throw new RestErrorException(
						CloudifyErrorMessages.DUMP_FILE_TOO_LARGE.getName(),
						Long.toString(target.length()),
						Long.toString(fileSizeLimit));
			}

			// load file contents into memory
			log(Level.INFO, "[getDumpRawData] - reading file content into byte array");
			final byte[] dumpBytes = FileUtils.readFileToByteArray(target);
			return dumpBytes;

		} catch (IOException e) {
			log(Level.WARNING, "[getDumpRawData] - failed to read the dump file into byte array");
			throw new RestErrorException(CloudifyErrorMessages.FAILED_CREATE_DUMP_FILE.getName(), 
					"failed to read file to byte array [" + e.getMessage() + "]");
		} finally {
			final boolean tempFileDeleteResult = target.delete();
			if (!tempFileDeleteResult) {
				log(Level.WARNING, "[getDumpRawData] - Failed to download temporary dump file: " + target);
			}

		}

	}
	
	private void validateGetMachineDump(final String[] processors) 
			throws RestErrorException {
		DumpMachineValidationContext validationContext = new DumpMachineValidationContext();
		validationContext.setProcessors(processors);
		for (DumpMachineValidator validator : dumpValidators) {
			validator.validate(validationContext);
		}
	}
	
	private void log(final Level level, final String msg) {
		if (logger.isLoggable(level)) {
			logger.log(level, msg);
		}
	}

	private void log(final Level level, final String msg, final Throwable e) {
		if (logger.isLoggable(level)) {
			logger.log(level, msg, e);
		}
	}
}
