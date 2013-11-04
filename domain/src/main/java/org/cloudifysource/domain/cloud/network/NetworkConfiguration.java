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
 *******************************************************************************/

package org.cloudifysource.domain.cloud.network;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

/*******
 * Details of a network in Cloudify.
 * 
 * @since 2.7.0
 * @author barakme
 */
@CloudifyDSLEntity(name = "networkConfiguration", clazz = NetworkConfiguration.class,
		allowInternalNode = true, allowRootNode = true)
public class NetworkConfiguration {
	private String name = null;

	private List<Subnet> subnets = new LinkedList<Subnet>();
	private Map<String, String> custom = new LinkedHashMap<String, String>();

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Map<String, String> getCustom() {
		return custom;
	}

	public void setCustom(final Map<String, String> custom) {
		this.custom = custom;
	}

	public List<Subnet> getSubnets() {
		return subnets;
	}

	public void setSubnets(final List<Subnet> subnets) {
		this.subnets = subnets;
	}

}
