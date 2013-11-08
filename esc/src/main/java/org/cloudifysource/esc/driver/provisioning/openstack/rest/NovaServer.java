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
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.openstack.rest;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.cloudifysource.esc.driver.provisioning.openstack.rest.deserializer.AddressesDeserializer;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * @author victor
 * @since 2.7.0
 */
@JsonRootName("server")
public class NovaServer {

	private String id;
	private String name;
	private Status status;
	private Date updated;
	private String hostId;
	private String adminPass;

	@JsonDeserialize(using = AddressesDeserializer.class)
	private List<NovaServerAddress> addresses;

	private NovaServerSecurityGroup[] securityGroups;

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(final Date updated) {
		this.updated = updated;
	}

	public String getHostId() {
		return hostId;
	}

	public void setHostId(final String hostId) {
		this.hostId = hostId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(final Status status) {
		this.status = status;
	}

	public NovaServerSecurityGroup[] getSecurityGroups() {
		return securityGroups;
	}

	public void setSecurityGroups(final NovaServerSecurityGroup[] securityGroups) {
		this.securityGroups = securityGroups;
	}

	public List<NovaServerAddress> getAddresses() {
		return addresses;
	}

	public void setAddresses(final List<NovaServerAddress> addresses) {
		this.addresses = addresses;
	}

	public String getAdminPass() {
		return adminPass;
	}

	public void setAdminPass(final String adminPass) {
		this.adminPass = adminPass;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
