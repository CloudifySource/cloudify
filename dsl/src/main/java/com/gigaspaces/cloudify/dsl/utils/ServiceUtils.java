package com.gigaspaces.cloudify.dsl.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.gigaspaces.cloudify.dsl.internal.DSLException;

public class ServiceUtils {

	private static final int TIMEOUT_BETWEEN_CONNECTION_ATTEMPTS = 1000;
	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ServiceUtils.class.getName());

	/**
	 * Same as waitForPortToOpen. Wait for ports to be opened by the process
	 * with default set to 127.0.0.1
	 * 
	 * @param portList
	 * @param timeoutInSeconds
	 * @throws TimeoutException
	 */
	public static void waitForPortToOpen(List<Integer> portList,
			int timeoutInSeconds) throws TimeoutException {
		waitForPortToOpen(portList, "127.0.0.1", timeoutInSeconds);
	}

	/**
	 * checkPortsOpen will repeatedly try to connect to the ports defined in the
	 * groovy configuration file every second for the specified timeout period
	 * to see whether the ports are open. Having all the tested ports opened
	 * means that the process has completed loading successfully.
	 * 
	 * @throws DSLException
	 * 
	 */
	public static void waitForPortToOpen(List<Integer> portList,
			String hostName, int timeoutInSeconds) throws TimeoutException {
		if (portList == null || portList.isEmpty()) {
			throw new IllegalArgumentException(
					"No port was defined in the configuration file");
		}
		long start = System.currentTimeMillis();
		int openPortsCounter;
		while (System.currentTimeMillis() < start + timeoutInSeconds * 1000) {
			Socket sock = null;
			openPortsCounter = 0;
			logger.info("Checking connection to " + hostName + " on ports "
					+ portList);
			for (int port : portList) {
				try {
					sock = new Socket();
					sock.connect(new InetSocketAddress(hostName, port));
					sock.close();
					openPortsCounter++;
					logger.fine("Connection to port: " + port + " Succeeded!");
					if (openPortsCounter == portList.size()) {
						return;
					}
				} catch (IOException ioe) {
					logger.fine("Connection to port " + port + " Failed!");
					break;
				} finally {
					try {
						sock.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
			try {

				Thread.sleep(TIMEOUT_BETWEEN_CONNECTION_ATTEMPTS);
			} catch (InterruptedException e) {
			}

		}
	}

	/**
	 * Checks that the specified port is free before the process starts. a
	 * default host name is used 127.0.0.1.
	 * 
	 * @param portList
	 *            - list of ports to check.
	 * @return - true if port is free
	 */
	public static boolean isPortFree(int port) {
		return isPortFree(port, "127.0.0.1");
	}

	/**
	 * Checks that the specified port is free before the process starts.
	 * 
	 * @param portList
	 *            - list of ports to check.
	 * @param the
	 *            host name to check.
	 * @return - true if port is free
	 */
	public static boolean isPortFree(int port, String host) {
		List<Integer> list = new ArrayList<Integer>();
		list.add(port);
		return isPortsFree(list, host);
	}

	/**
	 * Checks that the specified ports are free before the process starts. a
	 * default host name is used 127.0.0.1.
	 * 
	 * @param portList
	 *            - list of ports to check.
	 * @return - true if ports are free
	 */
	public static boolean isPortsFree(List<Integer> portList) {
		return isPortsFree(portList, "127.0.0.1");
	}

	/**
	 * Checks that the specified ports are free before the process starts.
	 * 
	 * @param portList
	 *            - list of ports to check.
	 * @param hostName
	 *            - host.
	 * @return
	 * @return - true if ports are free
	 */
	public static boolean isPortsFree(List<Integer> portList, String hostName) {
		Socket sock = new Socket();
		for (int port : portList) {
			try {
				sock.connect(new InetSocketAddress(hostName, port));
				sock.close();
				// connection succeeded - the port is not free
				return false;
			} catch (IOException e) {
			} finally {
				try {
					sock.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return true;
	}

	/*********
	 * Executes an HTTP GET Request to the given URL, using a one second connect timeout and read timeout.
	 * 
	 * @param url the HTTP URL.
	 * @return the HTTP return code. If an error occured while sending the request, for instance if a connection could not be made, returns 500
	 */
	public static boolean isHttpURLAvailable(final String url) {
		return getHttpReturnCode(url) == 200;
	}
	
	/*********
	 * Executes an HTTP GET Request to the given URL, using a one second connect timeout and read timeout.
	 * 
	 * @param url the HTTP URL.
	 * @return the HTTP return code. If an error occured while sending the request, for instance if a connection could not be made, returns 500
	 */
	public static int getHttpReturnCode(final String url) {
		return getHttpReturnCode(url, 1000, 1000);
	}
	/*********
	 * Executes an HTTP GET Request to the given URL. 
	 * 
	 * @param url the HTTP URL.
	 * @param connectTimeout the connection timeout.
	 * @param readTimeout the read timeout.
	 * @return the HTTP return code. If an error occured while sending the request, for instance if a connection could not be made, returns 500
	 */
	public static int getHttpReturnCode(final String url, final int connectTimeout, final int readTimeout) {

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
}
