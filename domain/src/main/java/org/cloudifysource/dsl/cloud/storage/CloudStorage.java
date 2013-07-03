/*******************************************************************************
 ' * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.cloud.storage;

import java.util.HashMap;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * 
 * @author elip
 *
 */
@CloudifyDSLEntity(name = "cloudStorage", clazz = CloudStorage.class, allowInternalNode = true, allowRootNode = true,
	parent = "cloud")
public class CloudStorage {
	
	private String className;
	
	public String getClassName() {
		return className;
	}

	public void setClassName(final String className) {
		this.className = className;
	}

	private Map<String, StorageTemplate> templates = new HashMap<String, StorageTemplate>();

	public Map<String, StorageTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, StorageTemplate> templates) {
		this.templates = templates;
	}
}
