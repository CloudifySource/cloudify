/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.domain.cloud;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

/********
 * DSL POJO for template specific installer settings, like connection timeouts and retries. Uses sensible default - most
 * templates should not need to set this.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
@CloudifyDSLEntity(name = "installer", clazz = CloudTemplateInstallerConfiguration.class,
		allowInternalNode = true, allowRootNode = true, parent = "computeTemplate")
public class CloudTemplateInstallerConfiguration {

	private static final int DEFAULT_ROUTE_RESOLUTION_TIMEOUT = 2 * 60 * 1000; // 2 minutes

	/*****
	 * Default interval between consecutive connection attempts.
	 */
	public static final int DEFAULT_CONNECTION_RETRY_INTERVAL_MILLIS = 5000;

	/***
	 * Indicates that installer should use default port for protocol used.
	 */
	public static final int DEFAULT_PORT = -1;

	/***
	 * Number of file transfer retries before file transfer is considered failed.
	 */
	public static final int DEFAULT_FILE_TRANSFER_RETRIES = 3;

	/********
	 * Connection timeout for connection attempts.
	 */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 10 * 1000;

	private int connectionTestRouteResolutionTimeoutMillis = DEFAULT_ROUTE_RESOLUTION_TIMEOUT;
	private int connectionTestIntervalMillis = DEFAULT_CONNECTION_RETRY_INTERVAL_MILLIS;
	private int connectionTestConnectTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT;

	private int fileTransferConnectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT;
	private int fileTransferRetries = DEFAULT_FILE_TRANSFER_RETRIES;
	private int fileTransferPort = DEFAULT_PORT;
	private int fileTransferConnectionRetryIntervalMillis = DEFAULT_CONNECTION_RETRY_INTERVAL_MILLIS;

	private int remoteExecutionPort = DEFAULT_PORT;
	private int remoteExecutionConnectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT;


	public CloudTemplateInstallerConfiguration() {

	}

	/******
	 * Number of connection retries before file transfer is considered failed. Defaults to 3.
	 *
	 * @return number of retries.
	 *
	 */
	public int getFileTransferRetries() {
		return fileTransferRetries;
	}

	public void setFileTransferRetries(final int fileTransferRetries) {
		this.fileTransferRetries = fileTransferRetries;
	}

	/*********
	 * Port number to use for file transfer. Defaults to standard protocol port.
	 *
	 * @return port number for file transfer.
	 */
	public int getFileTransferPort() {
		return fileTransferPort;
	}

	public void setFileTransferPort(final int fileTransferPort) {
		this.fileTransferPort = fileTransferPort;
	}

	/*********
	 * The port number for remote execution. Default to standard protocol port.
	 *
	 * @return the port number for remote exection.
	 */
	public int getRemoteExecutionPort() {
		return remoteExecutionPort;
	}

	/*******
	 * File transfer connection timeout, in milliseconds. Default to 10 seconds.
	 *
	 * @return the file tranfer connection timeout.
	 */
	public int getFileTransferConnectionTimeoutMillis() {
		return fileTransferConnectionTimeoutMillis;
	}

	public void setFileTransferConnectionTimeoutMillis(final int fileTransferConnectionTimeoutMillis) {
		this.fileTransferConnectionTimeoutMillis = fileTransferConnectionTimeoutMillis;
	}

	public void setRemoteExecutionPort(final int remoteExecutionPort) {
		this.remoteExecutionPort = remoteExecutionPort;
	}

	/*******
	 * Timeout for remote execution connection, in milliseconds. Defaults to 10 seconds.
	 *
	 * @return the timeout for remote execution connections.
	 */
	public int getRemoteExecutionConnectionTimeoutMillis() {
		return remoteExecutionConnectionTimeoutMillis;
	}

	/******
	 * Interval between consecutive file transfer connection attempts, in milliseconds. Defaults to 5 seconds.
	 *
	 * @return interval between connection attempts.
	 */
	public int getFileTransferConnectionRetryIntervalMillis() {
		return fileTransferConnectionRetryIntervalMillis;
	}

	public void setFileTransferConnectionRetryIntervalMillis(final int fileTransferConnectionRetryIntervalMillis) {
		this.fileTransferConnectionRetryIntervalMillis = fileTransferConnectionRetryIntervalMillis;
	}

	/********
	 * Connection test route resolution timeout in millis. Defaults to 2 minutes.
	 *
	 * @return router resolution timeout.
	 */
	public int getConnectionTestRouteResolutionTimeoutMillis() {
		return connectionTestRouteResolutionTimeoutMillis;
	}

	public void setConnectionTestRouteResolutionTimeoutMillis(final int connectionTestRouteResolutionTimeoutMillis) {
		this.connectionTestRouteResolutionTimeoutMillis = connectionTestRouteResolutionTimeoutMillis;
	}

	/*******
	 * Connection test interval in millis. Defaults to 5 seconds.
	 *
	 * @return the connection test interval.
	 */
	public int getConnectionTestIntervalMillis() {
		return connectionTestIntervalMillis;
	}

	public void setConnectionTestIntervalMillis(final int connectionTestIntervalMillis) {
		this.connectionTestIntervalMillis = connectionTestIntervalMillis;
	}

	/*********
	 * connection test connect timeout, in millis. Defaults to 10 seconds.
	 *
	 * @return connection test connect timeout.
	 */
	public int getConnectionTestConnectTimeoutMillis() {
		return connectionTestConnectTimeoutMillis;
	}

	public void setConnectionTestConnectTimeoutMillis(final int connectionTestConnectTimeoutMillis) {
		this.connectionTestConnectTimeoutMillis = connectionTestConnectTimeoutMillis;
	}

	public void setRemoteExecutionConnectionTimeoutMillis(final int remoteExecutionConnectionTimeoutMillies) {
		this.remoteExecutionConnectionTimeoutMillis = remoteExecutionConnectionTimeoutMillies;
	}

}
