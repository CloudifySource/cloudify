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

import org.cloudifysource.dsl.internal.CloudifyConstants;

/******************
 * ServiceUtils exposes a range of methods that recipes can use in closures, including TCP port checks, HTTP requests
 * and OS tests. The ServiceUtils is always imported into a recipe, so there is no need to add an import statement to a
 * service recipe for it.
 * 
 * @author barakme, adaml
 * @since 1.0
 * 
 */
public final class ServiceUtils {

	private static final int DEFAULT_HTTP_READ_TIMEOUT = 1000;
	private static final int DEFAULT_HTTP_CONNECTION_TIMEOUT = 1000;

	private ServiceUtils() {
		// private constructor to prevent initialization.
	}

	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ServiceUtils.class.getName());

	/***********
	 * Tests if a port of the localhost interface is in use.
	 * @param port the port number.
	 * @return true if the port is not in use, false otherwise.
	 */
	public static boolean isPortFree(final int port) {
		return !isPortOccupied(port);
	}

	/**
	 * Checks that the specified ports are free.
	 * 
	 * @param portList - list of ports to check.
	 * @return - true if all ports are free
	 */
	public static boolean arePortsFree(final List<Integer> portList) {
		int portCounter = 0;
		for (final int port : portList) {
			if (!isPortOccupied(port)) {
				logger.fine("port: " + port + " is open.");
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
	 * @param port - port to check.
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
	 * @param portList list of port to check.
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
	 * @param url the HTTP URL.
	 * @return the HTTP return code. If an error occured while sending the request, for instance if a connection could
	 *         not be made, returns 500
	 */
	public static boolean isHttpURLAvailable(final String url) {
		return getHttpReturnCode(url) == HttpURLConnection.HTTP_OK;
	}

	/*********
	 * Executes an HTTP GET Request to the given URL, using a one second connect timeout and read timeout.
	 * 
	 * @param url the HTTP URL.
	 * @return the HTTP return code. If an error occured while sending the request, for instance if a connection could
	 *         not be made, returns 500
	 */
	public static int getHttpReturnCode(final String url) {
		return getHttpReturnCode(url, DEFAULT_HTTP_CONNECTION_TIMEOUT, DEFAULT_HTTP_READ_TIMEOUT);
	}

	/*********
	 * Executes an HTTP GET Request to the given URL.
	 * 
	 * @param url the HTTP URL.
	 * @param connectTimeout the connection timeout.
	 * @param readTimeout the read timeout.
	 * @return the HTTP return code. If an error occured while sending the request, for instance if a connection could
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
			return HttpURLConnection.HTTP_INTERNAL_ERROR;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/************
	 * Returns the PU name for a service.
	 * 
	 * Important: when changing this method you must also change the getApplicationServiceName method that extracts the
	 * service name from the absolute processing unit's name.
	 * 
	 * @param applicationName the service's application name.
	 * @param serviceName the service name.
	 * @return the PU name.
	 */
	// .
	public static String getAbsolutePUName(final String applicationName, final String serviceName) {
		return applicationName + "." + serviceName;
	}

	/**********
	 * Full name details of a service.
	 * 
	 * @author adaml, barakme
	 * 
	 */
	public static class FullServiceName {

		private final String serviceName;
		private final String applicationName;

		/************
		 * Constructor.
		 * 
		 * @param applicationName .
		 * @param serviceName .
		 */
		public FullServiceName(final String applicationName, final String serviceName) {
			super();
			this.serviceName = serviceName;
			this.applicationName = applicationName;
		}

		public String getServiceName() {
			return serviceName;
		}

		public String getApplicationName() {
			return applicationName;
		}

		@Override
		public String toString() {
			return "FullServiceName [applicationName=" + applicationName + ", serviceName=" + serviceName + "]";
		}

	}

	/***************
	 * Return the service name of a PU.
	 * 
	 * @param absolutePuName the PU name.
	 * @param applicationName the application name.
	 * @return the service name.
	 */
	public static String getApplicationServiceName(final String absolutePuName, final String applicationName) {
		// Management services do no have the application prefix in their processing unit's name.
		if (applicationName.equalsIgnoreCase(CloudifyConstants.MANAGEMENT_APPLICATION_NAME)) {
			return absolutePuName;
		}

		final boolean legitPuNamePrefix = absolutePuName.startsWith(applicationName + ".");
		if (legitPuNamePrefix) {
			return absolutePuName.substring(applicationName.length() + 1);
		}
		logger.severe("Application name " + applicationName
				+ " is not contained in the absolute processing unit's name " + absolutePuName
				+ ". returning absolute pu name");
		return absolutePuName;
	}

	/***********
	 * Returns the application name and service name of a PU.
	 * 
	 * @param puName the pu name.
	 * @return the application and service names.
	 */
	public static FullServiceName getFullServiceName(final String puName) {
		final int index = puName.lastIndexOf('.');
		if (index < 0) {
			throw new IllegalArgumentException("Could not parse PU name: " + puName
					+ " to read service and application names.");
		}

		final String applicationName = puName.substring(0, index);
		final String serviceName = puName.substring(index + 1);
		return new FullServiceName(applicationName, serviceName);
	}

	/***********
	 * Returns true if the current operating system is some variant of Windows.
	 * 
	 * @return true if running on Windows.
	 */
	public static boolean isWindows() {
		final String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	/***********
	 * Returns true if the current operating system is NOT some variant of Windows.
	 * 
	 * 
	 * @return true if not running on Windows.
	 */
	public static boolean isLinuxOrUnix() {
		return !isWindows();
	}

}
