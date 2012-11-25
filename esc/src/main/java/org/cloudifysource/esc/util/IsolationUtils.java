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
	
	public static boolean isGlobal(final Service service) {
		
		if (service.getIsolationSLA() != null && service.getIsolationSLA().getGlobal() != null) {
			return true;
		}
		return false;
	}
}
