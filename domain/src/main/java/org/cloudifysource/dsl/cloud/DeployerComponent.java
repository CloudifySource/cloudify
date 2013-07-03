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
 * Grid deployer configuration POJO.
 * 
 * @author adaml
 * @since 2.5.0
 */
@CloudifyDSLEntity(name = "deployer", clazz = DeployerComponent.class, allowInternalNode = true,
allowRootNode = false, parent = "components")
public class DeployerComponent extends GridComponent {
	
	private Integer websterPort;
	private Integer port;
	
	public DeployerComponent() {
		this.setMaxMemory("128m");
		this.setMinMemory("128m");
		this.setWebsterPort(6666);
		this.setPort(7000);
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(final Integer port) {
		this.port = port;
	} 
	
	public Integer getWebsterPort() {
		return websterPort;
	}

	public void setWebsterPort(final Integer websterPort) {
		this.websterPort = websterPort;
	}
}
