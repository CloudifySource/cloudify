/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.validators;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.cloudifysource.rest.util.IsolationUtils;
import org.springframework.stereotype.Component;

@Component
public class ValidateInstanceMemory implements InstallServiceValidator {

	@Override
	public void validate(final InstallServiceValidationContext validationContext) throws RestErrorException {
		final Service service = validationContext.getService();
		final Cloud cloud = validationContext.getCloud();
		if (service == null) {
			return;
		}
		if (IsolationUtils.isDedicated(service)) {
			return;
		}
		String serviceTemplate = null;
		if (service.getCompute() != null) {
			serviceTemplate = service.getCompute().getTemplate();
		}
		if (serviceTemplate == null) {
			serviceTemplate = cloud.getCloudCompute().getTemplates().entrySet().iterator().next().getKey();
		}
		final int machineTemplateMemory =
				cloud.getCloudCompute().getTemplates().get(serviceTemplate).getMachineMemoryMB();
		final int reservedMachineMemory = cloud.getProvider().getReservedMemoryCapacityPerMachineInMB();
		final long instanceMemoryMB = IsolationUtils.getInstanceMemoryMB(service);
		if (instanceMemoryMB > machineTemplateMemory - reservedMachineMemory) {
			throw new RestErrorException(CloudifyErrorMessages.INSUFFICIENT_MEMORY.getName(),
					service.getName(), instanceMemoryMB, machineTemplateMemory, reservedMachineMemory);
		}
	}

}
