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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author victor
 * @since 2.7.0
 */
public class SecurityGroupRulesRequest {
	private String direction;
	private String securityGroupId;
	private String portRangeMin;
	private String portRangeMax;
	private String protocol;
	private String remoteGroupId;
	private String remoteIpPrefix;

	public String getDirection() {
		return direction;
	}

	public SecurityGroupRulesRequest setDirection(final String direction) {
		this.direction = direction;
		return this;
	}

	public String getSecurityGroupId() {
		return securityGroupId;
	}

	public SecurityGroupRulesRequest setSecurityGroupId(final String securityGroupId) {
		this.securityGroupId = securityGroupId;
		return this;
	}

	public String getPortRangeMin() {
		return portRangeMin;
	}

	public SecurityGroupRulesRequest setPortRangeMin(final String portRangeMin) {
		this.portRangeMin = portRangeMin;
		return this;
	}

	public String getPortRangeMax() {
		return portRangeMax;
	}

	public SecurityGroupRulesRequest setPortRangeMax(final String portRangeMax) {
		this.portRangeMax = portRangeMax;
		return this;
	}

	public String getProtocol() {
		return protocol;
	}

	public SecurityGroupRulesRequest setProtocol(final String protocol) {
		this.protocol = protocol;
		return this;
	}

	public String getRemoteGroupId() {
		return remoteGroupId;
	}

	public SecurityGroupRulesRequest setRemoteGroupId(final String remoteGroupId) {
		this.remoteGroupId = remoteGroupId;
		return this;
	}

	public String getRemoteIpPrefix() {
		return remoteIpPrefix;
	}

	public SecurityGroupRulesRequest setRemoteIpPrefix(final String remoteIpPrefix) {
		this.remoteIpPrefix = remoteIpPrefix;
		return this;
	}

	public String computeRequest() {
		final StringBuilder sb = new StringBuilder();

		sb.append("{\"security_group_rule\":{");

		// direction
		if (StringUtils.isEmpty(direction)) {
			throw new IllegalStateException("The 'direction' field is mandatory.");
		}
		sb.append("\"direction\":\"").append(direction).append("\"");

		// security_group_id
		if (StringUtils.isEmpty(securityGroupId)) {
			throw new IllegalStateException("The 'securityGroupId' field is mandatory.");
		}
		sb.append(",\"security_group_id\":\"").append(securityGroupId).append("\"");

		// port_range_min
		if (StringUtils.isNotEmpty(portRangeMin)) {
			sb.append(",\"port_range_min\":\"").append(portRangeMin).append("\"");
		}

		// port_range_max
		if (StringUtils.isNotEmpty(portRangeMax)) {
			sb.append(",\"port_range_max\":\"").append(portRangeMax).append("\"");
		}

		// protocol
		if (StringUtils.isNotEmpty(protocol)) {
			sb.append(",\"protocol\":\"").append(protocol).append("\"");
		}

		// remote_group_id
		if (StringUtils.isNotEmpty(remoteGroupId)) {
			sb.append(",\"remote_group_id\":\"").append(remoteGroupId).append("\"");
		}

		// remote_ip_prefix
		if (StringUtils.isNotEmpty(remoteIpPrefix)) {
			sb.append(",\"remote_ip_prefix\":\"").append(remoteIpPrefix).append("\"");
		}

		sb.append("}}");

		return sb.toString();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
