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
package org.cloudifysource.dsl.cloud;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/******
 * Service manager configuration POJO.
 * 
 * @author adaml
 * @since 2.5.0
 */
@CloudifyDSLEntity(name = "usm", clazz = UsmComponent.class, allowInternalNode = true,
allowRootNode = false, parent = "components")
public class UsmComponent extends GridComponent {

	private String portRange;

	public UsmComponent() {
		this.setMaxMemory("128m");
		this.setMinMemory("128m");
		this.setPortRange("7010-7110");
	}

	public String getPortRange() {
		return portRange;
	}

	public void setPortRange(final String portRange) {
		this.portRange = portRange;
	}
}
