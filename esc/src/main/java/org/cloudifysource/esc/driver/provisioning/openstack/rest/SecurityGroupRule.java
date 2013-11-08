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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * @author victor
 * @since 2.7.0
 */
@JsonRootName("security_group_rule")
public class SecurityGroupRule {
	private String remoteGroupId;
	private String direction;
	private String remoteIpPrefix;
	private String protocol;
	private String ethertype;
	private String tenantId;
	private String portRangeMax;
	private String portRangeMin;
	private String id;
	private String securityGroupId;

	public String getRemoteGroupId() {
		return remoteGroupId;
	}

	public void setRemoteGroupId(final String remoteGroupId) {
		this.remoteGroupId = remoteGroupId;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(final String direction) {
		this.direction = direction;
	}

	public String getRemoteIpPrefix() {
		return remoteIpPrefix;
	}

	public void setRemoteIpPrefix(final String remoteIpPrefix) {
		this.remoteIpPrefix = remoteIpPrefix;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(final String protocol) {
		this.protocol = protocol;
	}

	public String getEthertype() {
		return ethertype;
	}

	public void setEthertype(final String ethertype) {
		this.ethertype = ethertype;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(final String tenantId) {
		this.tenantId = tenantId;
	}

	public String getPortRangeMax() {
		return portRangeMax;
	}

	public void setPortRangeMax(final String portRangeMax) {
		this.portRangeMax = portRangeMax;
	}

	public String getPortRangeMin() {
		return portRangeMin;
	}

	public void setPortRangeMin(final String portRangeMin) {
		this.portRangeMin = portRangeMin;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getSecurityGroupId() {
		return securityGroupId;
	}

	public void setSecurityGroupId(final String securityGroupId) {
		this.securityGroupId = securityGroupId;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
