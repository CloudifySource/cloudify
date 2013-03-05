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

import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/******
 * Grid lookup discovery configuration POJO.
 * 
 * @author adaml
 * @since 2.5.0
 */
@CloudifyDSLEntity(name = "discovery", clazz = DiscoveryComponent.class, allowInternalNode = true,
allowRootNode = false, parent = "components")
public class DiscoveryComponent extends GridComponent {
	
	private Integer discoveryPort;
	private Integer port;
	
	public DiscoveryComponent() {
		this.setMaxMemory("128m");
		this.setMinMemory("128m");
		this.setDiscoveryPort(CloudifyConstants.DEFAULT_LUS_PORT);
		this.setPort(7001);
	}

	public Integer getDiscoveryPort() {
		return discoveryPort;
	}

	public void setDiscoveryPort(final Integer discoveryPort) {
		this.discoveryPort = discoveryPort;
	} 
	
	public Integer getPort() {
		return port;
	}

	public void setPort(final Integer port) {
		this.port = port;
	}
	
	@DSLValidation
	void validatePorts(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validatePort(this.port);
		super.validatePort(this.discoveryPort);
	}
	
	@DSLValidation
	void validateMemory(final DSLValidationContext validationContext) throws DSLValidationException {
		super.validateMemorySyntax();
	}
}
