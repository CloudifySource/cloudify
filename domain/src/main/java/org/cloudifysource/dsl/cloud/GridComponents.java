/*******************************************************************************
' * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.cloud;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/******
 * Domain service grid configuration POJO for cloud administrators.
 * 
 * @author adaml
 * @since 2.5.0
 */
@CloudifyDSLEntity(name = "components", clazz = GridComponents.class, allowInternalNode = true,
allowRootNode = false, parent = "configuration")
public class GridComponents {
	
	private OrchestratorComponent orchestrator = new OrchestratorComponent();
	
	private DiscoveryComponent discovery = new DiscoveryComponent();
	
	private DeployerComponent deployer = new DeployerComponent();
	
	private WebuiComponent webui = new WebuiComponent();
	
	private UsmComponent usm = new UsmComponent();
	
	private RestComponent rest = new RestComponent();
	
	private AgentComponent agent = new AgentComponent();
	
	public OrchestratorComponent getOrchestrator() {
		return orchestrator;
	}

	public void setOrchestrator(final OrchestratorComponent orchestrator) {
		this.orchestrator = orchestrator;
	}

	public DiscoveryComponent getDiscovery() {
		return discovery;
	}

	public void setDiscovery(final DiscoveryComponent discovery) {
		this.discovery = discovery;
	}

	public DeployerComponent getDeployer() {
		return deployer;
	}

	public void setDeployer(final DeployerComponent deployer) {
		this.deployer = deployer;
	}

	public WebuiComponent getWebui() {
		return webui;
	}

	public void setWebui(final WebuiComponent webui) {
		this.webui = webui;
	}

	public UsmComponent getUsm() {
		return usm;
	}

	public void setUsm(final UsmComponent usm) {
		this.usm = usm;
	}

	public RestComponent getRest() {
		return rest;
	}

	public void setRest(final RestComponent rest) {
		this.rest = rest;
	}

	public AgentComponent getAgent() {
		return agent;
	}

	public void setAgent(final AgentComponent agent) {
		this.agent = agent;
	}
}
