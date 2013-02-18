/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
 ******************************************************************************/
package org.cloudifysource.esc.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;

/**
 * A utility class for IP manipulation and validation.
 * 
 * @author noak
 * @since 2.1.0
 */
public final class IPUtils {

	// hidden constructor
	private IPUtils() { }

	// timeout in seconds, waiting for a socket to connect
	private static final int DEFAULT_CONNECTION_TIMEOUT = 10;
	
	private static final int IP_BYTE_RANGE = 256;
	private static final int IP_PARTS = 4;

	/**
	 * Converts a standard IP address to a long-format IP address.
	 * 
	 * @param ipAddress
	 *            A standard IP address
	 * @return IP address as a long value
	 * @throws CloudProvisioningException
	 *             Indicates the given IP is invalid
	 */
	public static long ip2Long(final String ipAddress) throws CloudProvisioningException {
		if (!validateIPAddress(ipAddress)) {
			throw new CloudProvisioningException("Invalid IP address: " + ipAddress);
		}

		final byte[] ipBytes = getBytes(ipAddress);
		return ((long) (byte2int(ipBytes[0]) * IP_BYTE_RANGE + byte2int(ipBytes[1])) * IP_BYTE_RANGE 
				+ byte2int(ipBytes[2]))
				* IP_BYTE_RANGE + byte2int(ipBytes[3]);
	}

	/**
	 * Converts a long value representing an IP address to a standard IP address (dotted decimal format).
	 * 
	 * @param ip
	 *            long value representing an IP address
	 * @return A standard IP address
	 */
	public static String long2String(final long ip) {
		final long a = (ip & 0xff000000) >> 24;
		final long b = (ip & 0x00ff0000) >> 16;
		final long c = (ip & 0x0000ff00) >> 8;
		final long d = ip & 0xff;

		return a + "." + b + "." + c + "." + d;
	}

	/**
	 * Converts a standard IP address to a byte array.
	 * 
	 * @param ipAddress
	 *            IP address as a standard IP address (dotted decimal format)
	 * @return IP as a 4-element byte array
	 * @throws CloudProvisioningException
	 *             Indicates the given IP is invalid
	 */
	public static byte[] getBytes(final String ipAddress) throws CloudProvisioningException {
		//This implementation is commented out because it involves resolving the host, which we want to avoid.
		//return InetAddress.getByName(ipAddress).getAddress();
		byte[] addrArr = new byte[IP_PARTS];
		final String[] ipParts = ipAddress.split("\\.");
		if (ipParts.length == IP_PARTS) {
			for (int i = 0; i < IP_PARTS; i++) {
				addrArr[i] = (byte) Integer.parseInt(ipParts[i]);
			}
		} else {
			throw new CloudProvisioningException("Invalid IP address: " + ipAddress);
		}
		
		return addrArr;
	}

	/**
	 * Converts (unsigned) byte to int.
	 * 
	 * @param b
	 *            byte to convert
	 * @return int value representing the given byte
	 */
	public static int byte2int(final byte b) {
		int i = b;
		if (b < 0) {
			i = b & 0x7f + 128;
		}

		return i;
	}

	/**
	 * Converts a CIDR IP format to an IP range format (e.g. 192.168.9.60/31 becomes 192.168.9.60 -
	 * 192.168.9.61)
	 * 
	 * @param ipCidr
	 *            IP addresses formatted as CIDR
	 * @return IP addresses formatted as a simple range
	 * @throws UnknownHostException
	 *             Indicates the given IP cannot be resolved
	 * @throws CloudProvisioningException
	 *             Indicates the given IP is invalid
	 */
	public static String ipCIDR2Range(final String ipCidr) throws UnknownHostException, CloudProvisioningException {

		final String[] parts = ipCidr.split("/");
		final String ipAddress = parts[0];
		int maskBits;
		if (parts.length < 2) {
			maskBits = 0;
		} else {
			maskBits = Integer.parseInt(parts[1]);
		}

		if (!validateIPAddress(ipAddress)) {
			throw new CloudProvisioningException("Invalid IP address: " + ipAddress);
		}

		// Convert IPs into ints (32 bits).
		// E.g. 157.166.224.26 becomes 10011101 10100110 11100000 00011010
		// a simple split by dots (.), escaped.
		final String[] ipParts = ipAddress.split("\\.");
		final int addr = Integer.parseInt(ipParts[0]) << 24 & 0xFF000000 | Integer.parseInt(ipParts[1]) << 16
				& 0xFF0000 | Integer.parseInt(ipParts[2]) << 8 & 0xFF00 | Integer.parseInt(ipParts[3]) & 0xFF;

		// Get CIDR mask
		final int mask = 0xffffffff << 32 - maskBits;

		// Find lowest IP address
		final int lowest = addr & mask;
		final String lowestIP = buildIPString(toArray(lowest));

		// Find highest IP address
		final int highest = lowest + ~mask;
		final String highestIP = buildIPString(toArray(highest));

		return lowestIP + "-" + highestIP;
	}

	/**
	 * Convert a packed integer IP address into a 4-element array.
	 * 
	 * @param ip
	 *            IP address as an int
	 * @return IP as a 4-element array
	 */
	public static int[] toArray(final int ip) {
		final int[] ret = new int[IP_PARTS];
		for (int j = 3; j >= 0; --j) {
			ret[j] |= ip >>> 8 * (3 - j) & 0xff;
		}
		return ret;
	}

	/**
	 * Converts a 4-element array into a standard IP address (dotted decimal format).
	 * 
	 * @param ipBytes
	 *            as array of IP bytes
	 * @return A standard IP address
	 */
	public static String buildIPString(final int[] ipBytes) {
		final StringBuilder str = new StringBuilder();
		for (int i = 0; i < ipBytes.length; ++i) {
			str.append(ipBytes[i]);
			if (i != ipBytes.length - 1) {
				str.append(".");
			}
		}
		return str.toString();
	}

	/**
	 * Gets the next IP address as a standard IP address (dotted decimal format).
	 * 
	 * @param ipAddress
	 *            IP address (dotted decimal format)
	 * @return The following IP address
	 * @throws CloudProvisioningException
	 *             Indicates the given IP is invalid
	 */
	public static String getNextIP(final String ipAddress) throws CloudProvisioningException {
		return long2String(ip2Long(ipAddress) + 1);
	}

	/**
	 * Validates a standard IP address (dotted decimal format).
	 * 
	 * @param ipAddress
	 *            IP address to validate (in a dotted decimal format)
	 * @return true if valid, false if invalid
	 */
	public static boolean validateIPAddress(final String ipAddress) {
		boolean valid = false;

		// a simple split by dots (.), escaped.
		final String[] ipParts = ipAddress.split("\\.");
		if (ipParts.length == IP_PARTS) {
			for (final String part : ipParts) {
				final int intValue = Integer.parseInt(part);
				if (intValue < 0 || intValue > (IP_BYTE_RANGE - 1)) {
					break;
				}
				valid = true;
			}
		}
		return valid;
	}
	

	/**
	 * Validates a connection can be made to the given address and port, within the given time limit.
	 * 
	 * @param ipAddress The IP address to connect to
	 * @param port The port number to use
	 * @throws IOException Reports a failure to connect or resolve the given address.
	 */
	public static void validateConnection(final String ipAddress, final int port)
			throws IOException {
		validateConnection(ipAddress, port, DEFAULT_CONNECTION_TIMEOUT);
	}

	/**
	 * Validates a connection can be made to the given address and port, within the given time limit.
	 * 
	 * @param ipAddress The IP address to connect to
	 * @param port The port number to use
	 * @param timeout The time to wait before timing out, in seconds
	 * @throws IOException Reports a failure to connect or resolve the given address.
	 */
	public static void validateConnection(final String ipAddress, final int port, final int timeout)
			throws IOException {

		final Socket socket = new Socket();

		try {
			final InetSocketAddress endPoint = new InetSocketAddress(ipAddress, port);
			if (endPoint.isUnresolved()) {
				throw new UnknownHostException(ipAddress);
			}

			socket.connect(endPoint, CalcUtils.safeLongToInt(TimeUnit.SECONDS.toMillis(timeout), true));
		} finally {
			try {
				socket.close();
			} catch (final IOException ioe) {
				// ignore
			}
		}
	}
	
	/**
	 * Resolves the host name and returns its IP address.
	 * 
	 * @param hostName
	 *            The name of the host
	 * @return The IP address of the host
	 * @throws UnknownHostException
	 *             Indicates the host doesn't represent an available network object
	 */
	public static String resolveHostName(final String hostName)  throws UnknownHostException {

		InetAddress address = null;
		address = InetAddress.getByName(hostName);

		return address.getHostAddress();
	}

}
