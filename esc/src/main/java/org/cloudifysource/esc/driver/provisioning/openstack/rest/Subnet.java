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
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * @author victor
 * @since 2.7.0
 */
@JsonRootName("subnet")
public class Subnet {

	private String id;
	private String name;
	private Boolean enableDhcp;
	private String networkId;
	private String tenantId;
	private String ipVersion;
	private String gatewayIp;
	private String cidr;
	private List<String> dnsNameServers = new ArrayList<String>();

	// private List<AllocationPool> allocationPools;
	// private List<HostRoute> hostRoutes;

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Boolean isEnableDhcp() {
		return enableDhcp;
	}

	public void setEnableDhcp(final Boolean enableDhcp) {
		this.enableDhcp = enableDhcp;
	}

	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(final String networkId) {
		this.networkId = networkId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(final String tenantId) {
		this.tenantId = tenantId;
	}

	public List<String> getDnsNameServers() {
		return dnsNameServers;
	}

	public void addDnsNameServers(final String dnsNameServer) {
		this.dnsNameServers.add(dnsNameServer);
	}

	public String getIpVersion() {
		return ipVersion;
	}

	public void setIpVersion(final String ipVersion) {
		this.ipVersion = ipVersion;
	}

	public String getGatewayIp() {
		return gatewayIp;
	}

	public void setGatewayIp(final String gatewayIp) {
		this.gatewayIp = gatewayIp;
	}

	public String getCidr() {
		return cidr;
	}

	public void setCidr(final String cidr) {
		this.cidr = cidr;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
