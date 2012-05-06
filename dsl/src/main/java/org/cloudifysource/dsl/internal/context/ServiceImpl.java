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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.cloudifysource.dsl.context.Service;
import org.cloudifysource.dsl.context.ServiceInstance;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

//TODO - RENAME THIS CLASS! IT IS CONSTANTLY COLLIDING WITH THE SERVICE DSL CLASS
/**
 * 
 * @author barakme
 * @since 1.0
 */
public class ServiceImpl implements Service {

	private static final int DEFAULT_INVOKE_TIMEOUT = 60 * 1000; // one minute
	private final ProcessingUnit pu;
	private final String name;

	// only used for debugging in IntegratedContainer
	private final int planned;

	/**
	 * Constructor.
	 * 
	 * @param pu
	 *            underline processing unit that represents this service
	 */
	ServiceImpl(final ProcessingUnit pu) {
		this.pu = pu;
		this.name = pu.getName();
		planned = 0;
	}

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            service name
	 * @param planned
	 *            number of planned instances
	 */
	ServiceImpl(final String name, final int planned) {
		this.name = name;
		this.pu = null;
		this.planned = planned;

	}

	/**
	 * Invokes a given life cycle command on the service
	 * 
	 * @param commandName
	 *            command name
	 */
	// public void invoke(final String commandName) {
	// throw new UnsupportedOperationException("Invoke not implemented yet!");
	// }

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.context.IService#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.context.IService#getNumberOfPlannedInstances()
	 */
	@Override
	public int getNumberOfPlannedInstances() {
		if (this.pu != null) {
			return pu.getNumberOfInstances();
		} else {
			return planned;
		}
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.context.IService#getNumberOfActualInstances()
	 */
	@Override
	public int getNumberOfActualInstances() {
		return getInstances().length;
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.context.IService#waitForInstances(int, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public ServiceInstance[] waitForInstances(final int howmany, final long timeout, final TimeUnit timeUnit) {

		if (this.pu == null) {
			return new ServiceInstance[] { new ServiceInstanceImpl(null) };
		}
		final boolean result = pu.waitFor(howmany, timeout, timeUnit);
		if (result) {
			return getInstances();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.context.IService#getInstances()
	 */
	@Override
	public ServiceInstanceImpl[] getInstances() {

		if (this.pu != null) {
			final ProcessingUnitInstance[] puis = pu.getInstances();
			final ServiceInstanceImpl[] sis = new ServiceInstanceImpl[puis.length];
			for (int i = 0; i < sis.length; i++) {
				final ProcessingUnitInstance pui = puis[i];
				final ServiceInstanceImpl serviceInstance = new ServiceInstanceImpl(pui);
				sis[i] = serviceInstance;
			}

			return sis;
		} else {
			return new ServiceInstanceImpl[] { new ServiceInstanceImpl(null) };
		}

	}

	/* (non-Javadoc)
	 * @see org.cloudifysource.dsl.context.IService#invoke(java.lang.String, java.lang.Object[])
	 */
	@Override
	public Object[] invoke(final String commandName, final Object[] params)
			throws Exception {
		final ServiceInstanceImpl[] instances = this.getInstances();

		final List<Future<Object>> futures = new ArrayList<Future<Object>>();

		for (final ServiceInstanceImpl instance : instances) {
			final Future<Object> future = instance.invokeAsync(commandName,
					params);
			futures.add(future);
		}

		final long start = System.currentTimeMillis();
		final long end = start + DEFAULT_INVOKE_TIMEOUT;

		Exception firstException = null;
		final Object[] results = new Object[instances.length];
		for (int i = 0; i < results.length; i++) {
			final Future<Object> future = futures.get(i);
			try {
				results[i] = future.get(end - System.currentTimeMillis(),
						TimeUnit.MILLISECONDS);
			} catch (final Exception e) {
				results[i] = e;
				if (firstException == null) {
					firstException = e;
				}
			}
		}

		if (firstException != null) {
			throw firstException;
		}
		return results;

	}

	@Override
	public String toString() {
		return "Service [getName()=" + getName() + ", getNumberOfPlannedInstances()=" + getNumberOfPlannedInstances()
				+ ", getNumberOfActualInstances()=" + getNumberOfActualInstances() + ", getInstances()="
				+ Arrays.toString(getInstances()) + "]";
	}

}
