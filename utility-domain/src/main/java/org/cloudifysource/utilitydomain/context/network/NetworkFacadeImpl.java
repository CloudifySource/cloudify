/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.utilitydomain.context.network;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.domain.context.network.NetworkFacade;
import org.cloudifysource.domain.context.network.RemoteNetworkOperationException;
import org.cloudifysource.dsl.internal.context.RemoteNetworkProvisioningDriver;

/**
 * 
 * @author adaml
 * @since 2.7.0
 *
 */
public class NetworkFacadeImpl implements NetworkFacade {

	private RemoteNetworkProvisioningDriver remoteNetworkProvisioningDriver;

	public NetworkFacadeImpl(final RemoteNetworkProvisioningDriver remoteNetworkApi) {
		this.remoteNetworkProvisioningDriver = remoteNetworkApi;
	}
	
	@Override
	public void assignFloatingIP(final String instanceIPAddress, final String floatingIP, final Map<String, Object> context)
			throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		provisioningDriver.assignFloatingIP(instanceIPAddress, floatingIP, context);
	}

	private RemoteNetworkProvisioningDriver getRemoteNetworkProvisioningDriver() {
		if (this.remoteNetworkProvisioningDriver == null) {
			throw new IllegalStateException(
					"No network provisioning driver configured. "
							 + "remote network provisioning calls are not possible");
		}
		return this.remoteNetworkProvisioningDriver;
		
	}
	
	@Override
	public void unassignFloatingIP(final String instanceIPAddress, final String floatingIP, final Map<String, Object> context)
			throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		provisioningDriver.unassignFloatingIP(instanceIPAddress, floatingIP, context);
	}

	@Override
	public String allocateFloatingIP(final String poolName, final Map<String, Object> context)
			throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		return provisioningDriver.allocateFloatingIP(poolName, context);
	}

	@Override
	public void releaseFloatingIP(final String ip, final Map<String, Object> context) throws RemoteNetworkOperationException,
			TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		provisioningDriver.releaseFloatingIP(ip, context);
		
	}

	@Override
	public void assignFloatingIP(final String instanceIPAddress, final String floatingIP, final Map<String, Object> context,
			final long duration, final TimeUnit timeUnit)
			throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		provisioningDriver.assignFloatingIP(instanceIPAddress, floatingIP, context, duration, timeUnit);
	}

	@Override
	public void unassignFloatingIP(final String instanceIPAddress, final String floatingIP, final Map<String, Object> context,
			final long duration, final TimeUnit timeUnit)
			throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		provisioningDriver.unassignFloatingIP(instanceIPAddress, floatingIP, context, duration, timeUnit);
	}

	@Override
	public String allocateFloatingIP(String poolName, final Map<String, Object> context, final  long duration,
			final TimeUnit timeUnit) throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		return provisioningDriver.allocateFloatingIP(poolName, context, duration, timeUnit);
	}

	@Override
	public void releaseFloatingIP(String ip, final Map<String, Object> context, long duration, TimeUnit timeUnit)
			throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		provisioningDriver.releaseFloatingIP(ip, context, duration, timeUnit);
		
	}

}
