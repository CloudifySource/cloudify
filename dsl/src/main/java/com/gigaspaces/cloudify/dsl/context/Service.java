package com.gigaspaces.cloudify.dsl.context;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

//TODO - RENAME THIS CLASS! IT IS CONSTANTLY COLLIDING WITH THE SERVICE DSL CLASS
/**
 * 
 * @author barakme
 * @since 1.0
 */
public class Service {

	private final ProcessingUnit pu;
	private final String name;

	// only used for debugging in IntegratedContainer
	private final int planned;

	/**
	 * Constructor
	 * 
	 * @param pu
	 *            underline processing unit that represents this service
	 */
	Service(final ProcessingUnit pu) {
		this.pu = pu;
		this.name = pu.getName();
		planned = 0;
	}

	/**
	 * Constructor
	 * 
	 * @param name
	 *            service name
	 * @param planned
	 *            number of planned instances
	 */
	Service(final String name, final int planned) {
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

	/**
	 * Returns the service name
	 * 
	 * @return service name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the number of planned instances for this service
	 * 
	 * @return number of planned instances
	 */
	public int getNumberOfPlannedInstances() {
		if (this.pu != null) {
			return pu.getNumberOfInstances();
		} else {
			return planned;
		}
	}

	/**
	 * Returns the number of actually running instances for this service
	 * 
	 * @return number of actually running instances
	 */
	public int getNumberOfActualInstances() {
		return getInstances().length;
	}

	/*************
	 * Waits for the specified number of instances to become available.
	 * 
	 * @param howmany
	 *            number of instances to wait for.
	 * @param timeout
	 *            time to wait.
	 * @param timeUnit
	 *            time unit to wait.
	 * @return the available instances, or null if the requested number of
	 *         instances was not found. If found, the returned number of
	 *         instances may be larger then the requested amount.
	 */
	public ServiceInstance[] waitForInstances(final int howmany,
			final long timeout, final TimeUnit timeUnit) {

		if (this.pu != null) {
			boolean result = pu.waitFor(howmany, timeout, timeUnit);
			if (result) {
				return getInstances();
			} else {
				return null;
			}
		} else {
			return new ServiceInstance[] { new ServiceInstance(null) };
		}

	}

	/**
	 * Returns the instances of this service
	 * 
	 * @return array of service instances
	 */
	public ServiceInstance[] getInstances() {

		if (this.pu != null) {
			final ProcessingUnitInstance[] puis = pu.getInstances();
			final ServiceInstance[] sis = new ServiceInstance[puis.length];
			for (int i = 0; i < sis.length; i++) {
				final ProcessingUnitInstance pui = puis[i];
				final ServiceInstance serviceInstance = new ServiceInstance(pui);
				sis[i] = serviceInstance;
			}

			return sis;
		} else {
			return new ServiceInstance[] { new ServiceInstance(null) };
		}

	}

	@Override
	public String toString() {
		return "Service [getName()=" + getName()
				+ ", getNumberOfPlannedInstances()="
				+ getNumberOfPlannedInstances()
				+ ", getNumberOfActualInstances()="
				+ getNumberOfActualInstances() + ", getInstances()="
				+ Arrays.toString(getInstances()) + "]";
	}

}
