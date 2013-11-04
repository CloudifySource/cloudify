/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.dsl.rest.response.GetManagementResponse;
import org.cloudifysource.dsl.rest.response.ShutdownManagementResponse;
import org.cloudifysource.rest.RestConfiguration;
import org.hyperic.sigar.Sigar;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.gigaspaces.internal.sigar.SigarHolder;

/**
 * This controller is responsible for retrieving information about management machines. 
 * It is also the entry point for shutdown managers. 
 * <br>
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

		ShutdownManagementResponse resposne = new ShutdownManagementResponse();
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
			for (ProcessingUnitInstance instance : instances) {
				logger.info(instance.getMachine().getHostAddress());
			}
		}
		return instances;
	}

	private void sortInstances(final ProcessingUnitInstance[] instances) {
		Sigar sigar = SigarHolder.getSigar();
		final long myPid = sigar.getPid();
		logger.fine("PID of current process is: " + myPid);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("[sortInstances] - Original Order is: ");
			for (ProcessingUnitInstance instance : instances) {
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
	 * @return {@link org.cloudifysource.dsl.rest.response.GetManagementResponse}
	 */
	@RequestMapping(value = "/controllers", method = RequestMethod.GET)
	@PreAuthorize("isFullyAuthenticated() and hasAnyRole('ROLE_CLOUDADMINS')")
	public GetManagementResponse getManagers() {
		throw new UnsupportedOperationException();
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
