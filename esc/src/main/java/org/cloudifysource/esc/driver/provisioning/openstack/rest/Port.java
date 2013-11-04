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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author victor
 * @since 2.7.0
 */
public class Port {

	protected String id;
	@JsonProperty("device_id")
	protected String deviceId;
	@JsonProperty("network_id")
	protected String networkId;
	protected String status;
	@JsonProperty("fixed_ips")
	protected List<FixedIp> fixedIps = new ArrayList<FixedIp>();
	@JsonProperty("security_groups")
	protected Set<String> securityGroupIds = new HashSet<String>();

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(final String deviceId) {
		this.deviceId = deviceId;
	}

	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(final String networkId) {
		this.networkId = networkId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public void addSecurityGroupId(final String securityGroupId) {
		this.securityGroupIds.add(securityGroupId);
	}

	public Set<String> getSecurityGroupIds() {
		return securityGroupIds;
	}

	public void addFixedIp(final FixedIp fixedIp) {
		this.fixedIps.add(fixedIp);
	}

	public List<FixedIp> getFixedIps() {
		return fixedIps;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
