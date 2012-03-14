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

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/**
 * Defines an elastic deployment of a processing unit that contains an embedded space. The steteful Processing unit
 * configuration POJO is initialized by the service groovy DSL and holds all of the required information regarding the
 * deployment of stateful processing units.
 * 
 * @see org.openspaces.admin.pu.elastic.ElasticStatefulProcessingUnitDeployment
 * 
 * @author adaml
 * 
 */
@CloudifyDSLEntity(name = "statefulProcessingUnit", clazz = StatefulProcessingUnit.class, allowInternalNode = true,
		allowRootNode = false, parent = "service")
public class StatefulProcessingUnit extends ServiceProcessingUnit {

	private String binaries;

	/**
	 * can be a folder, or a jar/war file.
	 * 
	 * @return - a String containing the folder path or the jar file name.
	 */
	public String getBinaries() {
		return binaries;
	}

	public void setBinaries(final String binaries) {
		this.binaries = binaries;
	}
}
