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

/*******
 * A node running in an OpenStack cloud.
 * 
 * @author barakme
 * @since 2.1
 * 
 */
public class Node {

	private String id;
	private String status;
	private String name;
	private String privateIp;
	private String publicIp;

	@Override
	public String toString() {
		return "Node [id=" + getId() + ", name=" + getName() + ", status=" + getStatus() + ", privateIp="
				+ getPrivateIp() + ", publicIp=" + getPublicIp() + "]";
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

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

}