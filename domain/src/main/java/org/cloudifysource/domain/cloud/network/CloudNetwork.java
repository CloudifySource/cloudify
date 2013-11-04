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
import java.util.Map;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

/*******
 * Domain objects relating to cloud network configuration. Start with
 * {@link org.cloudifysource.domain.cloud.network.CloudNetwork}
 * 
 * @since 2.7.0
 * @author barakme
 */

@CloudifyDSLEntity(name = "cloudNetwork", clazz = CloudNetwork.class, allowInternalNode = true, allowRootNode = true,
		parent = "cloud")
public class CloudNetwork {

	private ManagementNetwork management = new ManagementNetwork();
	private Map<String, NetworkConfiguration> templates = new LinkedHashMap<String, NetworkConfiguration>();
	private Map<String, String> custom = new LinkedHashMap<String, String>();

	public ManagementNetwork getManagement() {
		return management;
	}

	public void setManagement(final ManagementNetwork management) {
		this.management = management;
	}

	public Map<String, NetworkConfiguration> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, NetworkConfiguration> templates) {
		this.templates = templates;
	}

	public Map<String, String> getCustom() {
		return custom;
	}

	public void setCustom(final Map<String, String> custom) {
		this.custom = custom;
	}

}
