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
package org.cloudifysource.domain.cloud.compute;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds compute templates.
 * 
 * @see {@link ComputeTemplate}
 * @author elip
 *
 */
@CloudifyDSLEntity(name = "cloudCompute", clazz = CloudCompute.class, allowInternalNode = true, allowRootNode = true,
	parent = "cloud")
public class CloudCompute {
	
	private Map<String, ComputeTemplate> templates = new LinkedHashMap<String, ComputeTemplate>();

	public Map<String, ComputeTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(final Map<String, ComputeTemplate> templates) {
		this.templates = templates;
	}

}
