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
package org.cloudifysource.domain.cloud;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

/******
 * Grid orchestrator configuration POJO.
 * 
 * @author adaml
 * @since 2.5.0
 */
@CloudifyDSLEntity(name = "orchestrator", clazz = OrchestratorComponent.class, allowInternalNode = true,
allowRootNode = false, parent = "components")
public class OrchestratorComponent extends GridComponent {

	private Integer port;
    private Integer startMachineTimeoutInSeconds;
    private Integer stopMachineTimeoutInSeconds;
    private Integer forceMachineShutdownTimeoutInSeconds;
    private boolean forceMachineShutdown;
	
	public OrchestratorComponent() {
		this.setMaxMemory("128m");
		this.setMinMemory("128m");
        this.setStartMachineTimeoutInSeconds(1800);
        this.setStopMachineTimeoutInSeconds(1800);
		this.setPort(7003);
        this.setForceMachineShutdown(false); // disabled by default.
        this.setForceMachineShutdownTimeoutInSeconds(60 * 5);
	}

    public boolean isForceMachineShutdown() {
        return forceMachineShutdown;
    }

    public void setForceMachineShutdown(boolean isforceMachineShutdown) {
        forceMachineShutdown = isforceMachineShutdown;
    }

    public Integer getForceMachineShutdownTimeoutInSeconds() {
        return forceMachineShutdownTimeoutInSeconds;
    }

    public void setForceMachineShutdownTimeoutInSeconds(Integer forceMachineShutdownTimeoutInSeconds) {
        this.forceMachineShutdownTimeoutInSeconds = forceMachineShutdownTimeoutInSeconds;
    }

    public Integer getStartMachineTimeoutInSeconds() {
        return startMachineTimeoutInSeconds;
    }

    public void setStartMachineTimeoutInSeconds(final Integer startMachineTimeoutInSeconds) {
        this.startMachineTimeoutInSeconds = startMachineTimeoutInSeconds;
    }

    public Integer getStopMachineTimeoutInSeconds() {
        return stopMachineTimeoutInSeconds;
    }

    public void setStopMachineTimeoutInSeconds(final Integer stopMachineTimeoutInSeconds) {
        this.stopMachineTimeoutInSeconds = stopMachineTimeoutInSeconds;
    }

    public Integer getPort() {
		return port;
	}

	public void setPort(final Integer port) {
		this.port = port;
	} 
}
