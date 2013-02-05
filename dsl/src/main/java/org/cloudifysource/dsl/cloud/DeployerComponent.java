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
		this.setMaxMemory(CloudifyConstants.DEFAULT_GSM_MAX_MEMORY);
		this.setMinMemory(CloudifyConstants.DEFAULT_GSM_MIN_MEMORY);
		this.setWebsterPort(CloudifyConstants.DEFAULT_GSM_WEBSTER_PORT);
		this.setPort(CloudifyConstants.DEFAULT_GSM_LRMI_PORT);
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
	
	@DSLValidation
	void validatePort(final DSLValidationContext validationContext) throws DSLValidationException {
		if (this.websterPort == null) {
			throw new DSLValidationException("webster port can't be null");
		}
		if (this.port == null) {
			throw new DSLValidationException("LRMI port can't be null");
		}
		if (this.websterPort <= 1024 || this.websterPort > 65535) {
			throw new DSLValidationException("webster port must be set to a positive integer between 1024 and 65536");
		}
		if (this.port <= 1024  || this.port > 65535) {
			throw new DSLValidationException("LRMI port must be set to a positive integer between 1024 and 65536");
		}
	}
}
