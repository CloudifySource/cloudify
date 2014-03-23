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
 * REST configuration POJO.
 * 
 * @author adaml
 * @since 2.5.0
 */
@CloudifyDSLEntity(name = "rest", clazz = RestComponent.class, allowInternalNode = true,
	allowRootNode = false, parent = "components")
public class RestComponent  extends GridComponent {
	
	private Integer port;
	private Integer serviceDiscoveryTimeoutInSeconds;
	
	public RestComponent() {
		this.setMaxMemory("128m");
		this.setMinMemory("128m");
		this.setServiceDiscoveryTimeoutInSeconds(60);
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(final Integer port) {
		this.port = port;
	}
	
	public void setServiceDiscoveryTimeoutInSeconds(Integer serviceDiscoveryTimeoutInSeconds) {
		this.serviceDiscoveryTimeoutInSeconds = serviceDiscoveryTimeoutInSeconds;
	}
	
	public Integer getServiceDiscoveryTimeoutInSeconds() {
		return serviceDiscoveryTimeoutInSeconds;
	}
	
}
