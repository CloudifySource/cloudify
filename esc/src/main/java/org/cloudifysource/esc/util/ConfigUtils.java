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
package org.cloudifysource.esc.util;

import org.cloudifysource.dsl.cloud.AgentComponent;
import org.cloudifysource.dsl.cloud.DeployerComponent;
import org.cloudifysource.dsl.cloud.DiscoveryComponent;
import org.cloudifysource.dsl.cloud.OrchestratorComponent;
import org.cloudifysource.dsl.internal.CloudifyConstants;

/**
 * Service grid system properties utils class.
 *
 * @author adaml
 * @since 2.5.0
 */	
public final class ConfigUtils {
	
	//this class should not be instantiated.
	private ConfigUtils() { }
	/**
	 * Constructs and returns the GSM commandline arguments.
	 * 
	 * @param deployer
	 * 		Deployer config
	 * @param discovery
	 * 		Discovery config
	 * @return
	 * 		Commandline arguments for the GSM
	 */
	public static String getGsmCommandlineArgs(final DeployerComponent deployer, final DiscoveryComponent discovery) {
		String gsmCommandLineArgs = "";
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
		return gsmCommandLineArgs;
	}

	/**
	 * Constructs and returns the LUS commandline arguments.
	 * 
	 * @param discovery
	 * 		 Discovery config 
	 * @return 
	 * 		Commandline arguments for the LUS
	 */
	public static String getLusCommandlineArgs(final DiscoveryComponent discovery) {
		String lusCommandLineArgs = "";
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
	 * 		Agent config 
	 * @return
	 * 		Commandline arguments for the GSA
	 */
	public static String getAgentCommandlineArgs(final AgentComponent agent) {
		String agentCommandLineArgs = "";
		Integer agentPort = agent.getPort();
		agentCommandLineArgs += getComponentMemoryArgs(agent.getMaxMemory(), agent.getMinMemory());
		agentCommandLineArgs += getComponentRmiArgs(agentPort.toString());
		return agentCommandLineArgs;
	}
	
	/**
	 * Constructs and returns the ESM commandline arguments.
	 * @param esm
	 * 		Esm config 
	 * @return
	 * 		Commandline arguments for the ESM
	 */
	public static String getEsmCommandlineArgs(final OrchestratorComponent esm) {
		String esmCommandLineArgs = "";
		esmCommandLineArgs += getComponentMemoryArgs(esm.getMaxMemory(), esm.getMinMemory());
		esmCommandLineArgs += getComponentRmiArgs(esm.getPort().toString());
		return esmCommandLineArgs;
	}

	private static String getComponentRmiArgs(final String rmiPort) {
		String rmiCommandlineArgs = "";
		if (rmiPort != null) {
			rmiCommandlineArgs += " -D" + CloudifyConstants.LRMI_BIND_PORT_CONTEXT_PROPERTY + "=" + rmiPort;
		}
		return rmiCommandlineArgs;
	}

	private static String getComponentMemoryArgs(final String maxMemory, final String minMemory) {
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
