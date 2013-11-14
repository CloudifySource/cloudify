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

	/**
	 * For some reason, the Nova VM request mix up properties with camel case and properties with lower case with
	 * underscores. The "keyname" name property uses underscores while the other fields use camel case. The
	 * <code>JsonProperty</code> annotation is here to force the translation to lower case with underscores. The
	 * (de)serialization of the other fields will keep camel case.
	 */
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

	public void setName(final String name) {
		this.name = name;
	}

	public String getFlavorRef() {
		return flavorRef;
	}

	public void setFlavorRef(final String flavorRef) {
		this.flavorRef = flavorRef;
	}

	public String getImageRef() {
		return imageRef;
	}

	public void setImageRef(final String imageRef) {
		this.imageRef = imageRef;
	}

	public String getKeyName() {
		return keyName;
	}

	public void setKeyName(final String keyName) {
		this.keyName = keyName;
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
