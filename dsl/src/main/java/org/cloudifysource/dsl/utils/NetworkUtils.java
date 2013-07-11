package org.cloudifysource.dsl.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * 
 * @author adaml
 *
 */
public class NetworkUtils {

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
}
