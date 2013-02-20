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
package org.cloudifysource.dsl;

import java.io.Serializable;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * Configuration of network elements of a specific service.
 * 
 * @author itaif
 * 
 */
@CloudifyDSLEntity(name = "network", clazz = ServiceNetwork.class, allowInternalNode = true, allowRootNode = false,
		parent = "service")
public class ServiceNetwork implements Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;

	private int port = -1;
	private String protocolDescription = "tcp";

	/********
	 * Default public constructor.
	 */
	public ServiceNetwork() {

	}

	/**
	 * @return the port number opened by this service.
	 */
	public int getPort() {
		return port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public String getProtocolDescription() {
		return protocolDescription;
	}

	public void setProtocolDescription(final String protocolDescription) {
		this.protocolDescription = protocolDescription;
	}

	@DSLValidation
	void checkPortValue(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (port <= 0) {
			throw new DSLValidationException("The port value of the network block must be a positive integer.");
		}
	}

	@DSLValidation
	void checkDescription(final DSLValidationContext validationContext)
			throws DSLValidationException {
		if (this.protocolDescription == null) {
			throw new DSLValidationException("The protocol description can't be an empty value");
		}

		
	}

}
