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
public class SecurityGroupsRequest {

	private String name;
	private String description;
	private String portRangeMax;
	private String portRangeMin;
	private String protocol;
	private String remoteGroupId;
	private String remoteIpPrefix;

	public String getName() {
		return name;
	}

	public SecurityGroupsRequest setName(final String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public SecurityGroupsRequest setDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getPortRangeMax() {
		return portRangeMax;
	}

	public SecurityGroupsRequest setPortRangeMax(final String portRangeMax) {
		this.portRangeMax = portRangeMax;
		return this;
	}

	public String getPortRangeMin() {
		return portRangeMin;
	}

	public SecurityGroupsRequest setPortRangeMin(final String portRangeMin) {
		this.portRangeMin = portRangeMin;
		return this;
	}

	public String getProtocol() {
		return protocol;
	}

	public SecurityGroupsRequest setProtocol(final String protocol) {
		this.protocol = protocol;
		return this;
	}

	public String getRemoteGroupId() {
		return remoteGroupId;
	}

	public SecurityGroupsRequest setRemoteGroupId(final String remoteGroupId) {
		this.remoteGroupId = remoteGroupId;
		return this;
	}

	public String getRemoteIpPrefix() {
		return remoteIpPrefix;
	}

	public SecurityGroupsRequest setRemoteIpPrefix(final String remoteIpPrefix) {
		this.remoteIpPrefix = remoteIpPrefix;
		return this;
	}

	public String computeRequest() {
		final StringBuilder sb = new StringBuilder();

		sb.append("{\"security_group\":{");

		// name
		if (StringUtils.isEmpty(name)) {
			throw new IllegalStateException("The 'name' field is mandatory.");
		}
		sb.append("\"name\":\"").append(name).append("\"");

		// description
		if (StringUtils.isEmpty(description)) {
			throw new IllegalStateException("The 'description' field is mandatory.");
		}
		sb.append(",\"description\":\"").append(description).append("\"");

		// port_range_max
		if (StringUtils.isNotEmpty(portRangeMax)) {
			sb.append(",\"port_range_max\":\"").append(portRangeMax).append("\"");
		}

		// port_range_min
		if (StringUtils.isNotEmpty(portRangeMin)) {
			sb.append(",\"port_range_min\":\"").append(portRangeMin).append("\"");
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
