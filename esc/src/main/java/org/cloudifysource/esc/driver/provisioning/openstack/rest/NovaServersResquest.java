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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author victor
 * @since 2.7.0
 */
public class NovaServersResquest {

	private String name;
	private String flavorRef;
	private String imageRef;

	private String keyName;
	private Set<String> securityGroups = new HashSet<String>();
	private Set<String> networks = new HashSet<String>();

	// private String userData;
	// private String availabilityZone;
	// private String metadata;
	// private String personality;
	// private String blockDeviceMapping;
	// private String nic;
	// private String configDrive;

	public String getName() {
		return name;
	}

	public NovaServersResquest setName(final String name) {
		this.name = name;
		return this;
	}

	public String getFlavorRef() {
		return flavorRef;
	}

	public NovaServersResquest setFlavorRef(final String flavorRef) {
		this.flavorRef = flavorRef;
		return this;
	}

	public String getImageRef() {
		return imageRef;
	}

	public NovaServersResquest setImageRef(final String imageRef) {
		this.imageRef = imageRef;
		return this;
	}

	public String getKeyName() {
		return keyName;
	}

	public NovaServersResquest setKeyName(final String keyName) {
		this.keyName = keyName;
		return this;
	}

	public void addSecurityGroup(final String securityGroup) {
		this.securityGroups.add(securityGroup);
	}

	public void addNetworks(final String idNetwork) {
		this.networks.add(idNetwork);
	}

	public String computeRequest() {
		final StringBuilder sb = new StringBuilder();

		sb.append("{\"server\":{");

		// name
		if (StringUtils.isEmpty(name)) {
			throw new IllegalStateException("The 'name' field is mandatory.");
		}
		sb.append("\"name\":\"").append(name).append("\"");

		// imageRef
		if (StringUtils.isEmpty(imageRef)) {
			throw new IllegalStateException("The 'imageRef' field is mandatory.");
		}
		sb.append(",\"imageRef\":\"").append(imageRef).append("\"");

		// flavorRef
		if (StringUtils.isEmpty(flavorRef)) {
			throw new IllegalStateException("The 'flavorRef' field is mandatory.");
		}
		sb.append(",\"flavorRef\":\"").append(flavorRef).append("\"");

		// keyName
		if (StringUtils.isNotEmpty(keyName)) {
			sb.append(",\"key_name\":\"").append(keyName).append("\"");
		}

		// Security groups
		if (!securityGroups.isEmpty()) {
			sb.append(",\"security_groups\":[");
			for (final String securityGroup : securityGroups) {
				sb.append("{\"name\":\"").append(securityGroup).append("\"},");
			}
			sb.setLength(sb.length() - 1); // remove the last comma
			sb.append("]");
		}

		// Networks
		if (!networks.isEmpty()) {
			sb.append(",\"networks\":[");
			for (final String idNetwork : networks) {
				sb.append("{\"uuid\":\"").append(idNetwork).append("\"},");
			}
			sb.setLength(sb.length() - 1); // remove the last comma
			sb.append("]");
		}

		sb.append("}}");
		return sb.toString();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
