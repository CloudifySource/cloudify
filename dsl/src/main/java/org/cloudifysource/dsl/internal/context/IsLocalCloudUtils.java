package org.cloudifysource.dsl.internal.context;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jini.core.discovery.LookupLocator;

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
			isLocalCloud = isThisMyIpAddress(locatorIP);
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("isLocalCloud() = " + isLocalCloud);			
		}
		return isLocalCloud;
	}

	/**
	 * @param ip - the ip to check if it refers to the local machine.
	 * @return true - if the specified ip is the local mahcine
	 * @see "http://stackoverflow.com/questions/2406341/how-to-check-if-an-ip-address-is-the-local-host-on-a-multi-homed-system"
	 */
	public static boolean isThisMyIpAddress(final String ip) {
		InetAddress addr;
		try {
			addr = InetAddress.getByName(ip);
		} catch (final UnknownHostException e) {
			return false;
		}

		// Check if the address is a valid special local or loop back
		if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
			return true;
		}

		// Check if the address is defined on any interface
		try {
			return NetworkInterface.getByInetAddress(addr) != null;
		} catch (final SocketException e) {
			return false;
		}
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
