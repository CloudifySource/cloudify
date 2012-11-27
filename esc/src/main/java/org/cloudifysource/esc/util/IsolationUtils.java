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
		
		if (service.getIsolationSLA() == null) {
			return true;
		}
		
		if (service.getIsolationSLA().getDedicated() != null) {
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param service .
	 * @return .
	 */
	public static boolean isAppShared(final Service service) {
		
		if (service.getIsolationSLA() != null && service.getIsolationSLA().getAppShared() != null) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param service .
	 * @return .
	 */
	public static boolean isTenantShared(final Service service) {
		
		if (service.getIsolationSLA() != null && service.getIsolationSLA().getTenantShared() != null) {
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param service .
	 * @return .
	 */
	public static boolean isGlobal(final Service service) {
		
		if (service.getIsolationSLA() != null && service.getIsolationSLA().getGlobal() != null) {
			return true;
		}
		return false;
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
}
