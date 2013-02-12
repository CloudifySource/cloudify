/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.rest.response;

/********
 * Controller details returned in controller access REST APIs.
 *
 * @author barakme
 * @since 2.5.0
 *
 */

public class ControllerDetails {

	private String privateIp;
	private String publicIp;
	private int instanceId;
	private boolean bootstrapToPublicIp;

	public String getPrivateIp() {
		return privateIp;
	}

	public void setPrivateIp(final String privateIp) {
		this.privateIp = privateIp;
	}

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(final String publicIp) {
		this.publicIp = publicIp;
	}

	public int getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(final int instanceId) {
		this.instanceId = instanceId;
	}

	public boolean isBootstrapToPublicIp() {
		return bootstrapToPublicIp;
	}

	public void setBootstrapToPublicIp(final boolean bootstrapToPublicIp) {
		this.bootstrapToPublicIp = bootstrapToPublicIp;
	}

}
