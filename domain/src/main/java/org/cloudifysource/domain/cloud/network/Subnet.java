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
 * Details of a subnet in Cloudify.
 * 
 * @since 2.7.0
 * @author barakme
 */
@CloudifyDSLEntity(name = "subnet", clazz = Subnet.class,
		allowInternalNode = true, allowRootNode = true, parent = "networkConfiguration")
public class Subnet {
	private String range = null;
	private Map<String, String> options = new LinkedHashMap<String, String>();
	private String name;

	public String getRange() {
		return range;
	}

	public void setRange(final String range) {
		this.range = range;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(final Map<String, String> options) {
		this.options = options;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}
}
