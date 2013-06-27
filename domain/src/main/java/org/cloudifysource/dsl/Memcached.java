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

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


/**********
 * Domain POJO for a XAP Memcached deployment.
 * @author barakme.
 * @since 2.0.0
 *
 */
@CloudifyDSLEntity(name = "memcached", clazz = Memcached.class, allowInternalNode = true, allowRootNode = false,
		parent = "service")
public class Memcached extends ServiceProcessingUnit {

	private Integer port;
	private Integer portRetries;
	private boolean threaded;
	private String binaries;

	public void setThreaded(final boolean threaded) {
		this.threaded = threaded;
	}

	public boolean isThreaded() {
		return threaded;
	}

	public void setPortRetries(final Integer portRetries) {
		this.portRetries = portRetries;
	}

	public Integer getPortRetries() {
		return portRetries;
	}

	public void setPort(final Integer port) {
		this.port = port;
	}

	public Integer getPort() {
		return port;
	}

	public void setBinaries(final String binaries) {
		this.binaries = binaries;
	}

	public String getBinaries() {
		return binaries;
	}
}
