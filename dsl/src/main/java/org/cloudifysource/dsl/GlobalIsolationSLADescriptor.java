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
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;

/**
 * This class defines a service deployment which is shared across all machines in the cluster.
 * Each service instance will be allocated according to the requirements specified.
 * @author elip
 *
 */
@CloudifyDSLEntity(name = "global", clazz = GlobalIsolationSLADescriptor.class, allowInternalNode = true,
allowRootNode = false, parent = "isolationSLA")
public class GlobalIsolationSLADescriptor {

	private static final int DEFAULT_SERVICE_INSTNACE_MEMORY = 128;

	private int instanceMemoryMB = DEFAULT_SERVICE_INSTNACE_MEMORY; // default to 128MB
	private int instanceCpuCores = 0; // default to 0, no CPU requirements
	private boolean useManagement = false; // don't install on management machines by default
	
	public boolean isUseManagement() {
		return useManagement;
	}

	public void setUseManagement(final boolean useManagement) {
		this.useManagement = useManagement;
	}

	public int getInstanceCpuCores() {
		return instanceCpuCores;
	}

	public void setInstanceCpuCores(final int instanceCpuCores) {
		this.instanceCpuCores = instanceCpuCores;
	}

	public int getInstanceMemoryMB() {
		return instanceMemoryMB;
	}

	public void setInstanceMemoryMB(final int instanceMemoryMB) {
		this.instanceMemoryMB = instanceMemoryMB;
	}

	@DSLValidation
	void validateDefaultValues(final DSLValidationContext validationContext)
			throws DSLValidationException {

		if (instanceMemoryMB < 0) {
			throw new DSLValidationException("instanceMemoryInMB cannot be negative");
		}
		if (instanceCpuCores < 0) {
			throw new DSLValidationException("instanceCpuCores cannot be negative");
		}
	}
	
	@Override
	public String toString() {
		return "GlobalIsolationSLADescriptor [instanceMemoryMB="
				+ instanceMemoryMB + ", instanceCpuCores=" + instanceCpuCores
				+ ", useManagement=" + useManagement + "]";
	}
}
