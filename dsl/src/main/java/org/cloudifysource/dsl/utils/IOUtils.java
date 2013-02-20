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
package org.cloudifysource.dsl.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.util.List;

/**********
 * A utility class for IO related functions commonly used by recipes and groovy scripts.
 * 
 * @author barakme
 * 
 */
public final class IOUtils {

	private static final int DEFAULT_HTTP_READ_TIMEOUT = 1000;
	private static final int DEFAULT_HTTP_CONNECTION_TIMEOUT = 1000;
	private static final int HTTP_ERROR = 500;
	private static final int HTTP_SUCCESS = 200;

	private IOUtils() {
		// private constructor to prevent initialization.
	}

	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ServiceUtils.class.getName());

	/*******
	 * Checks if a port is available on localhost.
	 * 
	 * @param port
	 *            the port number.
	 * @return true if the port is available, false if it is in use.
	 */
	public static boolean isPortFree(final int port) {
		return !isPortOccupied(port);
	}

	/**
	 * Checks that the specified ports are free.
	 * 
	 * @param portList
	 *            - list of ports to check.
	 * @return - true if all ports are free
	 */
	public static boolean arePortsFree(final List<Integer> portList) {
		int portCounter = 0;
		for (final int port : portList) {
			if (!isPortOccupied(port)) {
				logger.info("port: " + port + " is open.");
				portCounter++;
			}
			if (portCounter == portList.size()) {
				// All ports are free.
				return true;
			}

		}
		return false;
	}

	/**
	 * Checks whether a specified port is occupied.
	 * 
	 * @param port
	 *            - port to check.
	 * @return - true if port is occupied
	 */
	public static boolean isPortOccupied(final int port) {
		final Socket sock = new Socket();
		logger.fine("Checking port " + port);
		try {
			sock.connect(new InetSocketAddress("127.0.0.1", port));
			logger.fine("Connected to port " + port);
			sock.close();
			return true;
		} catch (final IOException e1) {
			logger.fine("Port " + port + " is free.");
			return false;
		} finally {
			try {
				sock.close();
			} catch (final IOException e) {
				// ignore
			}
		}
	}

	/**
	 * arePortsOccupied will repeatedly test the connection to the ports defined in the groovy configuration file to see
	 * whether the ports are open. Having all the tested ports opened means that the process has completed loading
	 * successfully and is up and running.
	 * 
	 * @param portList
	 *            list of port to check.
	 * @return true if all ports are in use, false otherwise.
	 * 
	 * 
	 */
	public static boolean arePortsOccupied(final List<Integer> portList) {
		int portCounter = 0;
		for (final int port : portList) {
			if (isPortOccupied(port)) {
				portCounter++;
			}
			if (portCounter == portList.size()) {
				// connection succeeded - the port is not free
				return true;
			}
		}
		return false;
	}

	/*********
	 * Executes an HTTP GET Request to the given URL, using a one second connect timeout and read timeout.
	 * 
	 * @param url
	 *            the HTTP URL.
	 * @return the HTTP return code. If an error occured while sending the request, for instance if a connection could
	 *         not be made, returns 500
	 */
	public static boolean isHttpURLAvailable(final String url) {
		return getHttpReturnCode(url) == HTTP_SUCCESS;
	}

	/*********
	 * Executes an HTTP GET Request to the given URL, using a one second connect timeout and read timeout.
	 * 
	 * @param url
	 *            the HTTP URL.
	 * @return the HTTP return code. If an error occured while sending the request, for instance if a connection could
	 *         not be made, returns 500
	 */
	public static int getHttpReturnCode(final String url) {
		return getHttpReturnCode(
				url, DEFAULT_HTTP_CONNECTION_TIMEOUT, DEFAULT_HTTP_READ_TIMEOUT);
	}

	/*********
	 * Executes an HTTP GET Request to the given URL.
	 * 
	 * @param url
	 *            the HTTP URL.
	 * @param connectTimeout
	 *            the connection timeout.
	 * @param readTimeout
	 *            the read timeout.
	 * @return the HTTP return code. If an error occurred while sending the request, for instance if a connection could
	 *         not be made, returns 500
	 */
	public static int getHttpReturnCode(final String url, final int connectTimeout, final int readTimeout) {

		HttpURLConnection connection = null;

		try {
			try {
				connection = (HttpURLConnection) new URL(url).openConnection();
			} catch (final MalformedURLException e) {
				throw new IllegalArgumentException("Failed to parse url: " + url, e);
			}
			try {
				connection.setRequestMethod("GET");
			} catch (final ProtocolException e) {
				throw new IllegalArgumentException(e);
			}

			connection.setConnectTimeout(connectTimeout);
			connection.setReadTimeout(readTimeout);

			final int responseCode = connection.getResponseCode();
			return responseCode;
		} catch (final IOException ioe) {
			return HTTP_ERROR;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
