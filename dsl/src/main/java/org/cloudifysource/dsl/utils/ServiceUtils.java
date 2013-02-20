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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessFinder;

import com.gigaspaces.internal.sigar.SigarHolder;

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
	 * 
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
	 * Checks whether the specified port is in use on the the localhost ("127.0.0.1") interface.
	 * 
	 * @param port the port to check.
	 * @return true if in use, false otherwise.
	 */
	public static boolean isPortOccupied(final int port) {
		return isPortOccupied("127.0.0.1", port);
	}

	/**
	 * Checks whether a specified port is occupied.
	 * 
	 * @param host - the interface/host to test.
	 * @param port - port to check.
	 * @return - true if port is occupied.
	 */
	public static boolean isPortOccupied(final String host, final int port) {
		final Socket sock = new Socket();
		logger.fine("Checking port " + port);
		try {
			sock.connect(new InetSocketAddress(host, port));
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
		return os.contains("win");
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

	/******
	 * Returns the primary local-host address. If the local-host interface could not be resolved,
	 * "localhost" is returned.
	 * 
	 * @return the primary local-host address.
	 */
	public static String getPrimaryInetAddress() {
		try {
			return java.net.InetAddress.getLocalHost().toString();
		} catch (UnknownHostException e) {
			logger.severe(e.getMessage());
			return "localhost";
		}
	}

	/***********
	 * 
	 * Tests if a port of some host is in use.
	 * 
	 * @param host the host to check.
	 * 
	 * @param port the port number.
	 * 
	 * @return true if the port is not in use, false otherwise.
	 */

	public static boolean isPortFree(final String host, final int port) {

		return !isPortOccupied(host, port);

	}

	/**********
	 * Utility method related to operating system processes..
	 * 
	 * @author barakme
	 * @since 2.1.1
	 * 
	 */
	public static final class ProcessUtils {

		private ProcessUtils() {
			//
		}

		/***********
		 * Retrieves an instance of SIGAR, which offers access to Operating System level information not typically
		 * available in the JDK.
		 * 
		 * Important note: Not all SIGAR functions are implemented on all operating systems and architecture platforms.
		 * If you use SIGAR directly, make sure to test first on your target platform.
		 * 
		 * For more information on SIGAR, please see: http://support.hyperic.com/display/SIGAR/Home
		 * 
		 * @return the sigar instance.
		 */
		public static Sigar getSigar() {
			return SigarHolder.getSigar();
		}

		/*************
		 * Executes a SIGAR PTQL query, returning the PIDs of the processes that match the query. For more info on
		 * SIGAR's PTQL - Process Table Query Language, see: http://support.hyperic.com/display/SIGAR/PTQL
		 * 
		 * @param query the PTQL query.
		 * @return the pids.
		 * @throws SigarException in case of an error.
		 */
		public static List<Long> getPidsWithQuery(final String query)
				throws SigarException {
			final Sigar sigar = getSigar();
			final ProcessFinder finder = new ProcessFinder(sigar);

			final long[] results = finder.find(query);
			final List<Long> list = new ArrayList<Long>(results.length);
			for (final long result : results) {
				list.add(result);
			}

			return list;

		}

		/*********
		 * Returns the pids of Java processes where the name specified in in the process arguments. This will usually be
		 * the java process' main class, though that may not always be the case.
		 * 
		 * PTQL Query: "State.Name.eq=java,Args.*.eq=" + name
		 * 
		 * @param name the java main class or jar file name.
		 * @return the pids that match the query, may be zero, one or more.
		 * @throws SigarException in case of an error.
		 */
		public static List<Long> getPidsWithMainClass(final String name)
				throws SigarException {
			return getPidsWithQuery("State.Name.eq=java,Args.*.eq=" + name);
		}

		/*************
		 * Returns the pids of processes where the base name of the process executable is as specified. PTQL Query:
		 * "State.Name.eq=" + name
		 * 
		 * @param name the process name.
		 * @return the matching PIDs.
		 * @throws SigarException in case of an error.
		 */
		public static List<Long> getPidsWithName(final String name)
				throws SigarException {
			return getPidsWithQuery("State.Name.eq=" + name);
		}

		/*************
		 * Returns the pids of processes where the full name of the process executable is as specified. PTQL Query:
		 * "Exe.Name.eq=" + name.
		 * 
		 * @param name the process name.
		 * @return the matching PIDs.
		 * @throws SigarException in case of an error.
		 */
		public static List<Long> getPidsWithFullName(final String name)
				throws SigarException {
			return getPidsWithQuery("Exe.Name.eq=" + name);
		}

		/*************
		 * Returns PID of process that has the specified port open.
		 * 
		 * @param port the port number.
		 * @return pid of the process.
		 * @throws SigarException in case of an error.
		 */
		// TODO - this does not work - SIGAR has not implemented getProPort on Win 7, Win 2008 and Solaris, no we can't
		// really use this.
		// public static List<Long> getPidWithPort(final int port)
		// throws SigarException {
		// final Sigar sigar = getSigar();
		// final long pid = sigar.getProcPort(NetFlags.CONN_TCP, port);
		// if (pid > 0) {
		// return Arrays.asList(pid);
		// } else {
		// return Arrays.asList();
		// }
		//
		// // NetConnection[] list = sigar.getNetConnectionList(NetFlags.CONN_SERVER | NetFlags.CONN_PROTOCOLS);
		// // for (NetConnection netConnection : list) {
		// // int localPort = (int) netConnection.getLocalPort();
		// // int state = netConnection.getState();
		// //
		// // if (state == NetFlags.TCP_LISTEN) {
		// // if (localPort == port) {
		// // netConnection.get
		// // return true;
		// // }
		// // }
		// // }
		// }
	}

}
