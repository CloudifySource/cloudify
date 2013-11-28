/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning.network;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.cloudifysource.domain.context.network.RemoteNetworkOperationException;
import org.cloudifysource.dsl.internal.context.RemoteNetworkProvisioningDriver;

/**
 * Adapter class for remote network oporations.
 * 
 * @author adaml
 * @since 2.7.0
 * 
 */
public class RemoteNetworkProvisioningDriverAdapter implements RemoteNetworkProvisioningDriver {

	private Logger logger = java.util.logging.Logger
			.getLogger(RemoteNetworkProvisioningDriverAdapter.class.getName());

	// one minute
	private static final long DEFAULT_NETWORK_OPERATION_TIMEOUT_MILLIS = 60 * 1000;

	private BaseNetworkDriver driver;

	public RemoteNetworkProvisioningDriverAdapter(final BaseNetworkDriver networkDriver) {
		this.driver = networkDriver;
	}

	@Override
	public void assignFloatingIP(final String instanceIPAddress, final String floatingIP,
			final Map<String, Object> context) throws RemoteNetworkOperationException, TimeoutException {
		try {
			this.driver.assignFloatingIP(instanceIPAddress, floatingIP, context,
					DEFAULT_NETWORK_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		} catch (final NetworkProvisioningException e) {
			logger.warning("failed to assign IP " + floatingIP + " to instance with ID " + instanceIPAddress
					+ ". Error was " + e.getMessage());
			throw new RemoteNetworkOperationException("failed to assign IP " + floatingIP + " to instance with ID "
					+ instanceIPAddress + " : " + e.getMessage(), e);
		}
	}

	@Override
	public void unassignFloatingIP(final String instanceIPAddress, final String floatingIP,
			final Map<String, Object> context) throws RemoteNetworkOperationException, TimeoutException {
		try {
			this.driver.unassignFloatingIP(instanceIPAddress, floatingIP, context,
					DEFAULT_NETWORK_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		} catch (final NetworkProvisioningException e) {
			logger.warning("failed to unassign IP " + floatingIP + " to instance with ID " + instanceIPAddress
					+ ". Error was " + e.getMessage());
			throw new RemoteNetworkOperationException("failed to unassign IP " + floatingIP + " to instance with ID "
					+ instanceIPAddress + " : " + e.getMessage(), e);
		}
	}

	@Override
	public String allocateFloatingIP(final String poolName, final Map<String, Object> context)
			throws RemoteNetworkOperationException, TimeoutException {
		try {
			return this.driver.allocateFloatingIP(poolName, context,
					DEFAULT_NETWORK_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		} catch (final NetworkProvisioningException e) {
			logger.warning("failed to allocate a new IP address from pool " + poolName
					+ " : " + e.getMessage());
			throw new RemoteNetworkOperationException("failed to allocate a new IP address from pool " + poolName
					+ " : " + e.getMessage(), e);
		}
	}

	@Override
	public void releaseFloatingIP(final String ip, final Map<String, Object> context)
			throws RemoteNetworkOperationException, TimeoutException {
		try {
			this.driver.releaseFloatingIP(ip, context, DEFAULT_NETWORK_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		} catch (final NetworkProvisioningException e) {
			logger.warning("failed releasing IP address " + ip
					+ " : " + e.getMessage());
			throw new RemoteNetworkOperationException("failed releasing IP address " + ip
					+ " : " + e.getMessage(), e);
		}
	}

	@Override
	public void assignFloatingIP(final String instanceIPAddress, final String floatingIP,
			final Map<String, Object> context, final long duration, final TimeUnit timeUnit)
			throws RemoteNetworkOperationException, TimeoutException {
		try {
			this.driver.assignFloatingIP(instanceIPAddress, floatingIP, context, duration, timeUnit);
		} catch (final NetworkProvisioningException e) {
			logger.warning("failed to assign IP " + floatingIP + " to instance with ID " + instanceIPAddress
					+ ". Error was " + e.getMessage());
			throw new RemoteNetworkOperationException("failed to assign IP " + floatingIP + " to instance with ID "
					+ instanceIPAddress + " : " + e.getMessage(), e);
		}

	}

	@Override
	public void unassignFloatingIP(final String instanceIPAddress, final String floatingIP,
			final Map<String, Object> context, final long duration, final TimeUnit timeUnit)
			throws RemoteNetworkOperationException, TimeoutException {
		try {
			this.driver.unassignFloatingIP(instanceIPAddress, floatingIP, context, duration, timeUnit);
		} catch (final NetworkProvisioningException e) {
			logger.warning("failed to unassign IP " + floatingIP + " to instance with ID " + instanceIPAddress
					+ ". Error was " + e.getMessage());
			throw new RemoteNetworkOperationException("failed to unassign IP " + floatingIP + " to instance with ID "
					+ instanceIPAddress + " : " + e.getMessage(), e);
		}

	}

	@Override
	public String allocateFloatingIP(final String poolName, final Map<String, Object> context, final long duration,
			final TimeUnit timeUnit) throws RemoteNetworkOperationException,
			TimeoutException {
		try {
			return this.driver.allocateFloatingIP(poolName, context, duration, timeUnit);
		} catch (final NetworkProvisioningException e) {
			logger.warning("failed to allocate a new IP address from pool " + poolName
					+ " : " + e.getMessage());
			throw new RemoteNetworkOperationException("failed to allocate a new IP address from pool " + poolName
					+ " : " + e.getMessage(), e);
		}
	}

	@Override
	public void releaseFloatingIP(final String ip, final Map<String, Object> context,
			final long duration, final TimeUnit timeUnit)
			throws RemoteNetworkOperationException, TimeoutException {
		try {
			this.driver.releaseFloatingIP(ip, context, duration, timeUnit);
		} catch (final NetworkProvisioningException e) {
			logger.warning("failed releasing IP address " + ip
					+ " : " + e.getMessage());
			throw new RemoteNetworkOperationException("failed releasing IP address " + ip
					+ " : " + e.getMessage(), e);
		}

	}

}
