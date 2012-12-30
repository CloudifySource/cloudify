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

package org.cloudifysource.esc.util;

import org.cloudifysource.dsl.Service;

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
}
