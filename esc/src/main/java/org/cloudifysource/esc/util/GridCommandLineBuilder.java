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
package org.cloudifysource.esc.util;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.AgentComponent;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.DeployerComponent;
import org.cloudifysource.domain.cloud.DiscoveryComponent;
import org.cloudifysource.domain.cloud.OrchestratorComponent;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.utilitydomain.openspaces.OpenspacesConstants;
import org.openspaces.admin.gsa.GSAReservationId;
import org.openspaces.grid.esm.EsmSystemProperties;

/**
 * Service grid system properties utils class.
 *
 * @author adaml
 * @since 2.5.0
 */
public final class GridCommandLineBuilder {

	private static final String GSM_EXCLUDE_GSC_ON_FAILED_INSTACE_BOOL = "true";
	private static final String GSM_EXCLUDE_GSC_ON_FAILED_INSTANCE = "gsm.excludeGscOnFailedInstance.disabled";
	private static final String ZONES_PROPERTY = "com.gs.zones";
	private static final String GSA_RESERVATION_ID_PROPERTY = "com.gs.agent.reservationid";
	private static final String MANAGEMENT_ZONE = "management";
	private static final String GSM_PENDING_REQUESTS_DELAY = "-Dorg.jini.rio.monitor.pendingRequestDelay=1000";
	private static final String DISABLE_MULTICAST = "-Dcom.gs.multicast.enabled=false";

	private static final String AUTO_SHUTDOWN_COMMANDLINE_ARGUMENT = "-Dcom.gs.agent.auto-shutdown-enabled=true";

	/**
	 * Constructs and returns the GSM commandline arguments.
	 *
	 * @param cloud
	 *            .
	 * @param lookupLocatorsString
	 *            .
	 *
	 * @param deployer
	 *            Deployer config
	 * @param discovery
	 *            Discovery config
	 * @return Commandline arguments for the GSM
	 */
	public String getGsmCommandlineArgs(final Cloud cloud, final String lookupLocatorsString,
			final DeployerComponent deployer, final DiscoveryComponent discovery) {

		String gsmCommandLineArgs =
				"-D" + CloudifyConstants.LUS_PORT_CONTEXT_PROPERTY
						+ "=" + OpenspacesConstants.DEFAULT_LUS_PORT
						+ " -D" + GSM_EXCLUDE_GSC_ON_FAILED_INSTANCE + "=" + GSM_EXCLUDE_GSC_ON_FAILED_INSTACE_BOOL
						+ " -D"
						+ ZONES_PROPERTY + "=" + MANAGEMENT_ZONE + " " + GSM_PENDING_REQUESTS_DELAY;

		Integer websterPort = deployer.getWebsterPort();
		if (websterPort != null) {
			gsmCommandLineArgs += " -D" + CloudifyConstants.GSM_HTTP_PORT_CONTEXT_PROPERTY + "=" + websterPort;
		}

		// The discovery port arg must also be added to the GSM commandline.
		if (discovery.getDiscoveryPort() != null) {
			gsmCommandLineArgs += " -D" + CloudifyConstants.LUS_PORT_CONTEXT_PROPERTY
					+ "=" + discovery.getDiscoveryPort();
		}
		gsmCommandLineArgs += getComponentMemoryArgs(deployer.getMaxMemory(), deployer.getMinMemory());
		gsmCommandLineArgs += getComponentRmiArgs(deployer.getPort().toString());

		final String persistentStoragePath = cloud.getConfiguration().getPersistentStoragePath();
		if (persistentStoragePath != null) {
			final String gsmStoragePath = persistentStoragePath + "/gsm";
			final String gsmDeployPath = persistentStoragePath + "/deploy";

			gsmCommandLineArgs = gsmCommandLineArgs + " -Dcom.gs.persistency.logDirectory=" + gsmStoragePath
					+ " -Dcom.gs.deploy=" + gsmDeployPath;
		}

		if (lookupLocatorsString != null) {
			gsmCommandLineArgs += " " + DISABLE_MULTICAST;
		}

		return gsmCommandLineArgs;
	}

	/**
	 * Constructs and returns the LUS commandline arguments.
	 *
	 * @param discovery
	 *            Discovery config
	 * @param lookupLocatorsString .
	 * @return Commandline arguments for the LUS
	 */
	public String getLusCommandlineArgs(final DiscoveryComponent discovery, final String lookupLocatorsString) {

		String lusCommandLineArgs =
				"-D" + CloudifyConstants.LUS_PORT_CONTEXT_PROPERTY
						+ "=" + OpenspacesConstants.DEFAULT_LUS_PORT
						+ " -D" + ZONES_PROPERTY + "=" + MANAGEMENT_ZONE;

		if (lookupLocatorsString != null) {
			lusCommandLineArgs += " " + DISABLE_MULTICAST;
		}

		Integer discoveryPort = discovery.getDiscoveryPort();
		if (discoveryPort != null) {
			lusCommandLineArgs += " -D" + CloudifyConstants.LUS_PORT_CONTEXT_PROPERTY + "="
					+ discoveryPort;
		}
		lusCommandLineArgs += getComponentMemoryArgs(discovery.getMaxMemory(), discovery.getMinMemory());
		lusCommandLineArgs += getComponentRmiArgs(discovery.getPort().toString());

		return lusCommandLineArgs;
	}

	/**
	 * Constructs and returns the GSA commandline arguments.
	 *
	 * @param agent
	 *            Agent config
	 * @param zone
	 *            - the agent zone
	 * @param reservationId
	 *            - the reservation Id
	 * @return Commandline arguments for the GSA
	 */
	public String getAgentCommandlineArgs(final AgentComponent agent, final String zone, 
			final GSAReservationId reservationId) {

		String agentCommandLineArgs = "";
		if (StringUtils.isNotBlank(zone)) {
			agentCommandLineArgs += "-D" + ZONES_PROPERTY + "=" + zone;
		}
		
		if (reservationId != null && StringUtils.isNotBlank(reservationId.toString())) {
			agentCommandLineArgs += " -D" + GSA_RESERVATION_ID_PROPERTY + "=" + reservationId.toString();
		}

		agentCommandLineArgs += " " + AUTO_SHUTDOWN_COMMANDLINE_ARGUMENT;
		agentCommandLineArgs += " ";

		Integer agentPort = agent.getPort();
		agentCommandLineArgs += getComponentMemoryArgs(agent.getMaxMemory(), agent.getMinMemory());
		agentCommandLineArgs += getComponentRmiArgs(agentPort.toString());
		return agentCommandLineArgs;
	}

	/**
	 * Constructs and returns the ESM commandline arguments.
	 *
	 * @param esm
	 *            Esm config.
	 * @param reboostrapping true if this management machine is being re-bootstrapped after a previous bootstrap.
	 * @return Commandline arguments for the ESM
	 */
	public String getEsmCommandlineArgs(final OrchestratorComponent esm, final boolean reboostrapping, final boolean isNoManagementSpace) {
		String esmCommandLineArgs = "";
		esmCommandLineArgs += getComponentMemoryArgs(esm.getMaxMemory(), esm.getMinMemory());
		esmCommandLineArgs += getComponentRmiArgs(esm.getPort().toString());
        esmCommandLineArgs += " -Dorg.openspaces.grid.start-agent-timeout-seconds="
                + esm.getStartMachineTimeoutInSeconds();
        esmCommandLineArgs += " -Dorg.openspaces.grid.stop-agent-timeout-seconds="
                + esm.getStopMachineTimeoutInSeconds();

		if (reboostrapping) {
			esmCommandLineArgs +=
					" -D" + CloudifyConstants.SYSTEM_PROPERTY_ESM_DISCOVERY_POLLING_INTERVAL_SECONDS + "=60 ";
		} else {
			esmCommandLineArgs +=
					" -D" + CloudifyConstants.SYSTEM_PROPERTY_ESM_DISCOVERY_POLLING_INTERVAL_SECONDS + "=20 ";
		}
		
		if (!isNoManagementSpace) {
			//CLOUDIFY-2180
			esmCommandLineArgs += " -D" + CloudifyConstants.SYSTEM_PROPERTY_ESM_BACKUP_MACHINES_STATE_TO_SPACE_FLAG +"=true";
		}

        if (esm.isForceMachineShutdown()) {
            Integer forceMachineShutdownTimeoutInSeconds = esm.getForceMachineShutdownTimeoutInSeconds();
            esmCommandLineArgs += " -D" + EsmSystemProperties.ESM_FORCE_MACHINE_TERMINATION_TIMEOUT_SECONDS + "="
                    + forceMachineShutdownTimeoutInSeconds + " ";
        }

		return esmCommandLineArgs;
	}

	private String getComponentRmiArgs(final String rmiPort) {
		String rmiCommandlineArgs = "";
		if (rmiPort != null) {
			rmiCommandlineArgs += " -D" + CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "=" + rmiPort;
		}
		return rmiCommandlineArgs;
	}

	private String getComponentMemoryArgs(final String maxMemory, final String minMemory) {
		String memoryCommandLineArgs = "";
		if (maxMemory != null) {
			memoryCommandLineArgs += " -Xmx" + maxMemory;
		}

		if (minMemory != null) {
			memoryCommandLineArgs += " -Xms" + minMemory;
		}
		return memoryCommandLineArgs;
	}
}
