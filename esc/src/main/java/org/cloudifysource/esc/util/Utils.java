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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManagers;

/**
 * Utilities class.
 * 
 * @author noak
 * @since 2.0.0
 */
public final class Utils {

	// timeout in seconds, for waiting for the admin API to load.
	private static final int ADMIN_API_TIMEOUT = 90;

	private Utils() {
	}

	/**
	 * Calculates the milliseconds remaining until the given end time.
	 * 
	 * @param end
	 *            The end time, in milliseconds
	 * @return Number of milliseconds remaining until the given end time
	 * @throws TimeoutException
	 *             Thrown when the end time is in the past
	 */
	public static long millisUntil(final long end)
			throws TimeoutException {
		final long millisUntilEnd = end - System.currentTimeMillis();
		if (millisUntilEnd < 0) {
			throw new TimeoutException("Cloud operation timed out");
		}
		return millisUntilEnd;
	}

	/**
	 * Safely casts long to int.
	 * 
	 * @param longValue
	 *            The long to cast
	 * @param roundIfNeeded
	 *            Indicating whether to change the value of the number if it exceeds int's max/min values. If set to
	 *            false and the long is too large/small, an {@link IllegalArgumentException} is thrown.
	 * @return int representing of the given long.
	 */
	public static int safeLongToInt(final long longValue, final boolean roundIfNeeded) {
		int intValue;
		if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
			if (roundIfNeeded) {
				if (longValue < Integer.MIN_VALUE) {
					intValue = Integer.MIN_VALUE;
				} else {
					intValue = Integer.MAX_VALUE;
				}
			} else {
				throw new IllegalArgumentException(longValue + " cannot be cast to int without changing its value.");
			}
		} else {
			intValue = (int) longValue;
		}
		return intValue;
	}

	/**
	 * Validates a connection can be made to the given address and port, within the given time limit.
	 * 
	 * @param ipAddress
	 *            The IP address to connect to
	 * @param port
	 *            The port number to use
	 * @param timeout
	 *            The end time, in milliseconds
	 * @throws IOException
	 *             Reports a failure to connect or resolve the given address.
	 */
	public static void validateConnection(final String ipAddress, final int port, final long timeout)
			throws IOException {

		final Socket socket = new Socket();

		try {
			final InetSocketAddress endPoint = new InetSocketAddress(ipAddress, port);
			if (endPoint.isUnresolved()) {
				throw new IllegalArgumentException("Failed to connect to: " + ipAddress + ":" + port
						+ ", address could not be resolved.");
			} else {
				socket.connect(
						endPoint, Utils.safeLongToInt(
								timeout, true));
			}
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (final IOException ioe) {
					// ignore
				}
			}
		}
	}

	/**
	 * Returns the content of a given input stream, as a String object.
	 * 
	 * @param is
	 *            the input stream to read.
	 * @return the content of the given input stream
	 * @throws IOException
	 *             Reporting failure to read from the InputStream
	 */
	public static String getStringFromStream(final InputStream is)
			throws IOException {
		final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
		final StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

	/**
	 * Converts a json String to a Map<String, Object>.
	 * 
	 * @param response
	 *            a json-format String to convert to a map
	 * @return a Map<String, Object> based on the given String
	 * @throws IOException
	 *             Reporting failure to read or map the String
	 */
	public static Map<String, Object> jsonToMap(final String response)
			throws IOException {
		@SuppressWarnings("deprecation")
		final JavaType javaType = TypeFactory.type(Map.class);
		return new ObjectMapper().readValue(
				response, javaType);
	}

	
	public static Admin getAdminObject(final String managementIP, final int expectedGsmCount)
			throws TimeoutException, InterruptedException {
		final AdminFactory adminFactory = new AdminFactory();
		adminFactory.addLocator(managementIP + ":" + CloudifyConstants.DEFAULT_LUS_PORT);
		final Admin admin = adminFactory.createAdmin();
		GridServiceManagers gsms = admin.getGridServiceManagers();
		final long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ADMIN_API_TIMEOUT);
		while (admin.getLookupServices() == null || gsms == null || expectedGsmCount > 0
				&& gsms.getSize() < expectedGsmCount) {
			if (System.currentTimeMillis() > end) {
				throw new TimeoutException("Admin API timed out");
			}
			Thread.sleep(TimeUnit.SECONDS.toMillis(1));
			gsms = admin.getGridServiceManagers();
		}

		return admin;
	}

}
