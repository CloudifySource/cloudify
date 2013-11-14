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
package org.cloudifysource.domain;

import java.io.Serializable;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;
import org.cloudifysource.domain.network.AccessRules;

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

	@Deprecated
	private int port = -1;
	@Deprecated
	private String protocolDescription = "tcp";

	private AccessRules accessRules = null;

	private String template = null;
	/********
	 * Default public constructor.
	 */
	public ServiceNetwork() {

	}

	/**
	 * @return the port number opened by this service.
	 */
	@Deprecated
	public int getPort() {
		return port;
	}

	@Deprecated
	public void setPort(final int port) {
		this.port = port;
	}

	@Deprecated
	public String getProtocolDescription() {
		return protocolDescription;
	}

	@Deprecated
	public void setProtocolDescription(final String protocolDescription) {
		this.protocolDescription = protocolDescription;
	}

	public AccessRules getAccessRules() {
		return accessRules;
	}

	public void setAccessRules(final AccessRules accessRules) {
		this.accessRules = accessRules;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(final String template) {
		this.template = template;
	}
}
