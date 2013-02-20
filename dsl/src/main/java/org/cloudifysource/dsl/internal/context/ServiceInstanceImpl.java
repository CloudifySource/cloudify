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
package org.cloudifysource.dsl.internal.context;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.cloudifysource.dsl.context.ServiceInstance;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.internal.pu.DefaultProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.pu.service.ServiceDetails;
import org.openspaces.pu.service.ServiceMonitors;

/******************
 * Represents a single instance of a service.
 *
 * @author barakme
 *
 */
public class ServiceInstanceImpl implements ServiceInstance {

	private static java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(ServiceInstanceImpl.class.getName());
	private final ProcessingUnitInstance pui;

	ServiceInstanceImpl(final ProcessingUnitInstance pui) {
		this.pui = pui;
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.internal.context.ServiceInstance#getInstanceID()
	 */
	@Override
	@Deprecated
	public int getInstanceID() {
		if (pui == null) {
			return 1;
		}
		return pui.getInstanceId();
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.internal.context.ServiceInstance#getInstanceId()
	 */
	@Override
	public int getInstanceId() {
		if (pui == null) {
			return 1;
		}
		return pui.getInstanceId();
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.internal.context.ServiceInstance#getHostAddress()
	 */
	@Override
	public String getHostAddress() {
		if (pui != null) {
			return pui.getMachine().getHostAddress();
		} else {
			try {
				return InetAddress.getLocalHost().getHostAddress();
			} catch (final UnknownHostException e) {
				logger.log(Level.SEVERE, "Failed to read local host address", e);
				return null;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.internal.context.ServiceInstance#getHostName()
	 */
	@Override
	public String getHostName() {

		if (pui != null) {
			return pui.getMachine().getHostName();
		} else {
			try {
				return InetAddress.getLocalHost().getHostName();
			} catch (final UnknownHostException e) {
				logger.log(Level.SEVERE, "Failed to read local host address", e);
				return null;
			}
		}
	}

	@Override
	public String toString() {
		return "ServiceInstance [getInstanceID()=" + getInstanceId() + ", getHostAddress()=" + getHostAddress()
				+ ", getHostName()=" + getHostName() + "]";
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.internal.context.ServiceInstance#getDetails(java.lang.String)
	 */
	@Override
	public Object getDetails(final String serviceDetailsKey) {
		if (this.pui == null) { // running in integrated container
			return null;
		}

		final Collection<ServiceDetails> allDetails = this.pui.getServiceDetailsByServiceId().values();
		for (final ServiceDetails serviceDetails : allDetails) {
			final Object res = serviceDetails.getAttributes().get(serviceDetailsKey);
			if (res != null) {
				return res;
			}

		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.internal.context.ServiceInstance#getMonitors(java.lang.String)
	 */
	@Override
	public Object getMonitors(final String serviceMonitorsKey) {
		if (this.pui == null) { // running in integrated container
			return null;
		}

		final Collection<ServiceMonitors> allMonitors = this.pui.getStatistics().getMonitors().values();

		for (final ServiceMonitors serviceMonitors : allMonitors) {
			final Object res = serviceMonitors.getMonitors().get(serviceMonitorsKey);
			if (res != null) {
				return res;
			}

		}

		return null;
	}

	/***********
	 * Invokes a custom command on this instance, returning immediately.
	 *
	 * @param commandName
	 *            the command name.
	 * @param params
	 *            the command parameters, may be zero-length.
	 * @return Future for the invocation result.
	 */
	Future<Object> invokeAsync(final String commandName, final Object[] params) {

		logger.log(Level.FINE, "Invoking command: {0} on instance {1}",
				new Object[] { commandName, this.getInstanceId() });
		final Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put(CloudifyConstants.INVOCATION_PARAMETER_COMMAND_NAME, commandName);

		for (int i = 0; i < params.length; i++) {
			paramsMap.put(CloudifyConstants.INVOCATION_PARAMETERS_KEY + i, params[i]);
		}

		final Future<Object> future = ((DefaultProcessingUnitInstance) pui).invoke("universalServiceManagerBean",
				paramsMap);

		return new InvocationFuture(future);
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.internal.context.ServiceInstance#invoke(java.lang.String, java.lang.Object[])
	 */
	@Override
	public Object invoke(final String commandName, final Object[] params) throws InterruptedException,
			ExecutionException, TimeoutException {

		final Future<Object> future = invokeAsync(commandName, params);
		final Object result = future.get(1, TimeUnit.MINUTES);
		return result;

	}

}
