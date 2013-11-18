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
	public void assignFloatingIP(final String ip, final String instanceID)
			throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		provisioningDriver.assignFloatingIP(ip, instanceID);
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
	public void unassignFloatingIP(final String ip, final String instanceID)
			throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		provisioningDriver.unassignFloatingIP(ip, instanceID);
	}

	@Override
	public String allocateFloatingIP(final String poolName)
			throws RemoteNetworkOperationException, TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		return provisioningDriver.allocateFloatingIP(poolName);
	}

	@Override
	public void releaseFloatingIP(final String ip) throws RemoteNetworkOperationException,
			TimeoutException {
		final RemoteNetworkProvisioningDriver provisioningDriver = getRemoteNetworkProvisioningDriver();
		provisioningDriver.releaseFloatingIP(ip);
		
	}

}
