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
import org.cloudifysource.dsl.internal.DSLException;


public class ServiceUtils {

	private static java.util.logging.Logger logger = java.util.logging.Logger
	.getLogger(ServiceUtils.class.getName());

	public static boolean isPortFree(int port) {
		return !isPortOccupied(port);
	}

	/**
	 * Checks that the specified ports are free.
	 * 
	 * @param portList
	 *            - list of ports to check.
	 * @return
	 * @return - true if all ports are free
	 */
	public static boolean arePortsFree(List<Integer> portList) {
		int portCounter = 0;
		for (int port : portList) {
			if (!isPortOccupied(port)){
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
	 * @param port - port to check.
	 * @return - true if port is occupied
	 */
	public static boolean isPortOccupied(int port) {
		Socket sock = new Socket();
		logger.fine("Checking port " + port );
		try {
			sock.connect(new InetSocketAddress("127.0.0.1", port));
			logger.fine("Connected to port " + port );
			sock.close();
			return true;
		} catch (IOException e1) {
			logger.fine("Port " + port + " is free.");
			return false;
		}finally {
			try {
				sock.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/**
	 * arePortsOccupied will repeatedly test the connection to the ports defined in
	 * the groovy configuration file to see whether the ports are open. Having
	 * all the tested ports opened means that the process has completed loading
	 * successfully and is up and running.
	 * 
	 * @throws DSLException
	 * 
	 */
	public static boolean arePortsOccupied(List<Integer> portList) {
		int portCounter = 0;
		for (int port : portList) {
			if (isPortOccupied(port)){
				logger.info("port: " + port + " is Occupied.");
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
	 * Executes an HTTP GET Request to the given URL, using a one second connect
	 * timeout and read timeout.
	 * 
	 * @param url
	 *            the HTTP URL.
	 * @return the HTTP return code. If an error occured while sending the
	 *         request, for instance if a connection could not be made, returns
	 *         500
	 */
	public static boolean isHttpURLAvailable(final String url) {
		return getHttpReturnCode(url) == 200;
	}

	/*********
	 * Executes an HTTP GET Request to the given URL, using a one second connect
	 * timeout and read timeout.
	 * 
	 * @param url
	 *            the HTTP URL.
	 * @return the HTTP return code. If an error occured while sending the
	 *         request, for instance if a connection could not be made, returns
	 *         500
	 */
	public static int getHttpReturnCode(final String url) {
		return getHttpReturnCode(url, 1000, 1000);
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
	 * @return the HTTP return code. If an error occured while sending the
	 *         request, for instance if a connection could not be made, returns
	 *         500
	 */
	public static int getHttpReturnCode(final String url,
			final int connectTimeout, final int readTimeout) {

		HttpURLConnection connection = null;

		try {
			try {
				connection = (HttpURLConnection) new URL(url).openConnection();
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("Failed to parse url: "
						+ url, e);
			}
			try {
				connection.setRequestMethod("GET");
			} catch (ProtocolException e) {
				throw new IllegalArgumentException(e);
			}

			connection.setConnectTimeout(connectTimeout);
			connection.setReadTimeout(readTimeout);

			final int responseCode = connection.getResponseCode();
			return responseCode;
		} catch (IOException ioe) {
			return 500;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	// Important: when changing this method you must also change the
	// getApplicationServiceName
	// method that extracts the service name from the absolute processing unit's
	// name.
	public static String getAbsolutePUName(String applicationName,
			String serviceName) {
		return (applicationName + "." + serviceName);
	}

	public static class FullServiceName {
		private final String serviceName;
		private final String applicationName;

		public FullServiceName(String applicationName, String serviceName) {
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
			return "FullServiceName [applicationName=" + applicationName
			+ ", serviceName=" + serviceName + "]";
		}

	}

	// extracts the service name from the absolutePuName. correlates with
	// getAbsolutePUName
	public static String getApplicationServiceName(String absolutePuName,
			String applicationName) {
		//Management services do no have the application prefix in their processing unit's name.
		if (applicationName.equalsIgnoreCase(CloudifyConstants.MANAGEMENT_APPLICATION_NAME)){
			return absolutePuName;
		}
		
		boolean legitPuNamePrefix = absolutePuName.startsWith(applicationName + ".");
		if (legitPuNamePrefix){
			return absolutePuName.substring(applicationName.length() + 1);
		}
		logger.severe("Application name " + applicationName 
					+ " is not contained in the absolute processing unit's name " 
					+ absolutePuName 
					+ ". returning absolute pu name");
		return absolutePuName;
	}

	public static FullServiceName getFullServiceName(final String puName) {
		final int index = puName.lastIndexOf('.');
		if (index < 0) {
			throw new IllegalArgumentException("Could not parse PU name: "
					+ puName + " to read service and application names.");
		}

		final String applicationName = puName.substring(0, index);
		final String serviceName = puName.substring(index + 1);
		return new FullServiceName(applicationName, serviceName);
	}
}
