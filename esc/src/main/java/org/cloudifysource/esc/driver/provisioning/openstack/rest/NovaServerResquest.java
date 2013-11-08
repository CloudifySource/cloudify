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
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonRootName;

/**
 * @author victor
 * @since 2.7.0
 */
@JsonRootName("server")
public class NovaServerResquest {

	private String name;
	private String flavorRef;
	private String imageRef;

	@JsonProperty("key_name")
	private String keyName;

	private List<NovaServerSecurityGroup> securityGroups = new ArrayList<NovaServerSecurityGroup>();
	private List<NovaServerNetwork> networks = new ArrayList<NovaServerNetwork>();

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

	public NovaServerResquest setName(final String name) {
		this.name = name;
		return this;
	}

	public String getFlavorRef() {
		return flavorRef;
	}

	public NovaServerResquest setFlavorRef(final String flavorRef) {
		this.flavorRef = flavorRef;
		return this;
	}

	public String getImageRef() {
		return imageRef;
	}

	public NovaServerResquest setImageRef(final String imageRef) {
		this.imageRef = imageRef;
		return this;
	}

	public String getKeyName() {
		return keyName;
	}

	public NovaServerResquest setKeyName(final String keyName) {
		this.keyName = keyName;
		return this;
	}

	public void addSecurityGroup(final String securityGroup) {
		this.securityGroups.add(new NovaServerSecurityGroup(securityGroup));
	}

	public void addNetworks(final String idNetwork) {
		this.networks.add(new NovaServerNetwork(idNetwork));
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
