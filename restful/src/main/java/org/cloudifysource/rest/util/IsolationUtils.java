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

package org.cloudifysource.rest.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.Application;
import org.cloudifysource.dsl.Service;
import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * Utility class for service deployment methods.
 * @author elip
 *
 */
public final class IsolationUtils {
	
	private IsolationUtils() {
		
	}
	
	/**
	 * 
	 * @param service .
	 * @return .
	 */
	public static boolean isDedicated(final Service service) {
		return (service.getIsolationSLA() == null || service.getIsolationSLA().getDedicated() != null);
	}
	
	/**
	 * 
	 * @param service .
	 * @return .
	 */
	public static boolean isAppShared(final Service service) {
		return (service.getIsolationSLA() != null && service.getIsolationSLA().getAppShared() != null);
	}

	/**
	 * 
	 * @param service .
	 * @return .
	 */
	public static boolean isTenantShared(final Service service) {
		return (service.getIsolationSLA() != null && service.getIsolationSLA().getTenantShared() != null);
	}
	
	/**
	 * 
	 * @param service .
	 * @return .
	 */
	public static boolean isGlobal(final Service service) {
		return (service.getIsolationSLA() != null && service.getIsolationSLA().getGlobal() != null) ;
	}
	
	/**
	 * 
	 * @param service .
	 * @return .
	 */
	public static double getInstanceCpuCores(final Service service) {
		
		if (IsolationUtils.isAppShared(service)) {
			return service.getIsolationSLA().getAppShared().getInstanceCpuCores();
		}
		if (IsolationUtils.isTenantShared(service)) {
			return service.getIsolationSLA().getTenantShared().getInstanceCpuCores();
		}
		if (IsolationUtils.isGlobal(service)) {
			return service.getIsolationSLA().getGlobal().getInstanceCpuCores();
		}
		return 0; // dedicated provisioning. no CPU requirements
	}
	
	/**
	 * 
	 * @param service .
	 * @return .
	 */
	public static long getInstanceMemoryMB(final Service service) {
		
		if (IsolationUtils.isAppShared(service)) {
			return service.getIsolationSLA().getAppShared().getInstanceMemoryMB();
		}
		if (IsolationUtils.isTenantShared(service)) {
			return service.getIsolationSLA().getTenantShared().getInstanceMemoryMB();
		}
		if (IsolationUtils.isGlobal(service)) {
			return service.getIsolationSLA().getGlobal().getInstanceMemoryMB();
		} else {
			throw new IllegalArgumentException("cannot get instanceMemoryMB for a dedicated provisioning service");
		}
	}

	/**
	 *
	 * @param service .
	 * @return whether or not instances of this service may be installed on a management machine.
	 */
	public static boolean isUseManagement(final Service service) {

		if (IsolationUtils.isAppShared(service)) {
			return service.getIsolationSLA().getAppShared().isUseManagement();
		}
		if (IsolationUtils.isTenantShared(service)) {
			return service.getIsolationSLA().getTenantShared().isUseManagement();
		}
		if (IsolationUtils.isGlobal(service)) {
			return service.getIsolationSLA().getGlobal().isUseManagement();
		}
		return false; // dedicated cannot use management machine

	}
	
	/**
	 * Make sure the cloud template used to install the service has enough memory to accommodate
	 * at least one instance. 
	 * @param service
	 * @param cloud
	 * @throws RestErrorException 
	 */
	public static void validateInstanceMemory(final Service service, final Cloud cloud) throws RestErrorException {
		
		if (service == null) {
			return;
		}
		if (isDedicated(service)) {
			return;
		}
		String serviceTemplate = null;
		if (service.getCompute() != null) {
			serviceTemplate = service.getCompute().getTemplate();
		}
		if (serviceTemplate == null) {
			serviceTemplate = cloud.getTemplates().entrySet().iterator().next().getKey();
		}
		int machineTemplateMemory = cloud.getTemplates().get(serviceTemplate).getMachineMemoryMB();
		int reservedMachineMemory = cloud.getProvider().getReservedMemoryCapacityPerMachineInMB();
		long instanceMemoryMB = getInstanceMemoryMB(service);
		if (instanceMemoryMB > (machineTemplateMemory - reservedMachineMemory)) {
			
			throw new RestErrorException(CloudifyErrorMessages.INSUFFICIENT_MEMORY.getName()
					,"Cannot install service " + service.getName() + ". The requested meomry was " + instanceMemoryMB 
						+ ", while the machine memory is " + machineTemplateMemory + " and the reserved is " + reservedMachineMemory);
		}
	}
	
	/**
	 * Make sure the cloud template used to install each service belonging to the application has enough memory to accommodate
	 * at least one instance. 
	 * @param service
	 * @param cloud
	 * @throws RestErrorException 
	 */
	public static void validateInstanceMemory(final Application application, final Cloud cloud) throws RestErrorException {
		List<String> errorMessages = new ArrayList<String>();
		for (Service service : application.getServices()) {
			try {
				validateInstanceMemory(service, cloud);
			} catch (RestErrorException e) {
				errorMessages.add(e.getMessage());
			}
		}
		if (!errorMessages.isEmpty()) {
			throw new RestErrorException(CloudifyErrorMessages.INSUFFICIENT_MEMORY.getName(),
					StringUtils.join(errorMessages, ","));
		}
	}
}
