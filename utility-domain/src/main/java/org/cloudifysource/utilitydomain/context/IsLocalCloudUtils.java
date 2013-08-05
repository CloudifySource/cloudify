package org.cloudifysource.utilitydomain.context;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.discovery.LookupLocator;

import org.cloudifysource.dsl.utils.NetworkUtils;
import org.jini.rio.boot.BootUtil;

/**
 * A helper class for {@link ServiceContextImpl#isLocalCloud()}.
 * @author itaif
 * @since 2.1
 */
public final class IsLocalCloudUtils {

	private IsLocalCloudUtils() { }
	
	private static final Logger logger = Logger.getLogger(IsLocalCloudUtils.class.getName());
	
	/**
	 * @return true if there is a single lookup locator, and its hostname/ipaddress is the local machine
	 */
	public static boolean isLocalCloud() {
	
		boolean isLocalCloud = false;
		LookupLocator[] locators = getLocators();
		
		if (locators.length == 1) {
			String locatorIP = locators[0].getHost();
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("isLocalCloud: Found locator IP ${locatorIP} ...");			
			}
			isLocalCloud = NetworkUtils.isThisMyIpAddress(locatorIP);
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("isLocalCloud() = " + isLocalCloud);			
		}
		return isLocalCloud;
	}

	/**
	 * Method that retrieves the same locators that the Admin API would use.
	 * At this stage we may not have an Admin API initialized yet.
	 * copied from {@link org.openspaces.admin.internal.discovery.DiscoveryService#getLocators()}
	 */
	public static LookupLocator[] getLocators() {
        String locators = null;
	    String locatorsProperty = System.getProperty("com.gs.jini_lus.locators");
        if (locatorsProperty == null) {
            locatorsProperty = System.getenv("LOOKUPLOCATORS");
        }
        if (locatorsProperty != null) {
            locators = locatorsProperty;
        }
        return BootUtil.toLookupLocators(locators);
    }
}
