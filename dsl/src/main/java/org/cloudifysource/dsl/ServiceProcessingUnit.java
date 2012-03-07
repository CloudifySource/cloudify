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

import java.util.Map;

/**
 * The abstract class ServiceProccessingUnit holds all the data shared by the different processing units i.e. DataGrid,
 * StatelessPU, StatefulPU and Memcached. Specifically holds the Processing unit's SLA and it's context properties.
 * 
 * @author adaml
 * 
 */

public abstract class ServiceProcessingUnit {

	private Sla sla;
	private Map<String, String> contextProperties;

	/**
	 * Returns the SLA object as defined in the groovy service file. The SLA holds in it the JVM's memory and
	 * availability definitions.
	 * 
	 * @return a processing unit's SLA object.
	 */
	public Sla getSla() {
		return sla;
	}

	/**
	 * returns a Map that holds all of the processing unit's context properties.
	 * 
	 @return a Map object holding all of the PU context properties.
	 */
	public Map<String, String> getContextProperties() {
		return contextProperties;
	}

	public void setContextProperties(final Map<String, String> contextProperties) {
		this.contextProperties = contextProperties;
	}

	public void setSla(final Sla sla) {
		this.sla = sla;
	}

}
