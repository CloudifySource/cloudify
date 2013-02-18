/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.esc.driver.provisioning.openstack;

/************
 * A Floating IP in an openstack cloud deployment, which can be attached to an openstack compute node. 
 * @author barakme
 * @since 2.1
 *
 */
public class FloatingIP {

	private String instanceId;
	private String ip;
	private String fixedIp;
	private String id;

	@Override
	public String toString() {
		return "FloatingIP [id=" + getId() + ", ip=" + getIp() + ", instanceId=" + getInstanceId() + ", fixedIp="
				+ getFixedIp() + "]";
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(final String instanceId) {
		this.instanceId = instanceId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(final String ip) {
		this.ip = ip;
	}

	public String getFixedIp() {
		return fixedIp;
	}

	public void setFixedIp(final String fixedIp) {
		this.fixedIp = fixedIp;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

}