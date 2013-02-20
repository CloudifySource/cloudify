/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.dsl;

import java.io.Serializable;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/*********
 * Domain POJO for a plugin descriptor. A Service plugin is a class that implements one or more of the USM event
 * interfaces. The Plugin mechanism allows services to customize the execution of the USM using actual classes and jars,
 * rather then relying on closures. The closure syntax works best for fairly simple and short bits of code. For more
 * complicates scenarios, the plugin mechanism should be used.
 * 
 * @author barakme.
 * @since 2.0.0
 * 
 */
@CloudifyDSLEntity(name = "plugin", clazz = PluginDescriptor.class, allowInternalNode = true, allowRootNode = false,
		parent = "service")
public class PluginDescriptor implements Serializable {

	/**
     * 
     */
	private static final long serialVersionUID = 1L;
	private String className;
	private Map<String, Object> config;
	private String name;

	public Map<String, Object> getConfig() {
		return config;
	}

	public void setConfig(final Map<String, Object> config) {
		this.config = config;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(final String className) {
		this.className = className;
	}

}
