package org.cloudifysource.esc.driver.provisioning;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class BaseComputeDriver {

	private ComputeDriverConfiguration configuration;
	protected final List<ProvisioningDriverListener> eventsListenersList = new LinkedList<ProvisioningDriverListener>();

	/**************
	 * Called once on startup of the cloud driver, passing the cloud configuration to it.
	 * 
	 * @throws CloudProvisioningException
	 *             Indicates invalid cloud configuration
	 */
	public void setConfig(final ComputeDriverConfiguration configuration)
			throws CloudProvisioningException {
		this.configuration = configuration;
	}

	/**
	 * Returns the compute context.
	 * 
	 * @return Compute context object or null if not set
	 */
	public Object getComputeContext() {
		return null;
	}

	/***************
	 * Starts an additional machine on the cloud , on the specific location, to scale out this specific service. In case
	 * of an error while provisioning the machine, any allocated resources should be freed before throwing a
	 * CloudProvisioningException or TimeoutException to the caller.
	 * 
	 * @param duration
	 *            Time duration to wait for the instance.
	 * @param unit
	 *            Time unit to wait for the instance.
	 * @param locationId
	 *            the location to allocate the machine to.
	 * @return The details of the started instance.
	 * @throws TimeoutException
	 *             In case the instance was not started in the allotted time.
	 * @throws CloudProvisioningException
	 *             If a problem was encountered while starting the machine.
	 */
	public MachineDetails startMachine(final ProvisioningContext context, final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		unsupported();
		return null;
	}

	private void unsupported() {
		throw new UnsupportedOperationException("Method not implemented");
	}

	/******************
	 * Start the management machines for this cluster. This method is called once by the cloud administrator when
	 * bootstrapping a new cluster.
	 * 
	 * @param duration
	 *            timeout duration.
	 * @param unit
	 *            timeout unit.
	 * @return The created machine details.
	 * @throws TimeoutException
	 *             If creating the new machines exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the machines needed for management could not be provisioned.
	 */
	public MachineDetails[] startManagementMachines(final ManagementProvisioningContext context, final long duration,
			final TimeUnit unit)
			throws TimeoutException,
			CloudProvisioningException {
		unsupported();
		return null;
	}

	/**************************
	 * Stops a specific machine for scaling in or shutting down a specific service.
	 * 
	 * @param machineIp
	 *            host-name/IP of the machine to shut down.
	 * @param duration
	 *            time to wait for the shutdown operation.
	 * @param unit
	 *            time unit for the shutdown operations
	 * @return true if the operation succeeded, false otherwise.
	 * 
	 * @throws InterruptedException
	 *             If the operation was interrupted.
	 * @throws TimeoutException
	 *             If the operation exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the operation encountered an error.
	 */
	public boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit)
			throws InterruptedException,
			TimeoutException, CloudProvisioningException {
		unsupported();
		return false;
	}

	/*************
	 * Stops the management machines.
	 * 
	 * @throws TimeoutException
	 *             in case the stop operation exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the stop operation failed.
	 */
	public void stopManagementMachines()
			throws TimeoutException, CloudProvisioningException {
		unsupported();
	}

	/************
	 * Returns the name of this cloud.
	 * 
	 * @return the name of the cloud.
	 */
	public String getCloudName() {
		{
			unsupported();
			return null;
		}
	}

	/*************
	 * Called when this bean is no longer needed. Close any internal bean resources.
	 * 
	 * @see cleanupCloud() - for cleaning up cloud resources.
	 */
	public void close() {
		// leave unimplemented. It is customary for close() implementations to also close() parent objects.
	}

	/**************
	 * Adds a new listener. It is the responsibility of the cloud driver developer to publish a list of supported
	 * events.
	 * 
	 * @param listener
	 *            A class that implements ProvisioningDriverListner.
	 */
	public void addListener(final ProvisioningDriverListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener argument may not be null");
		}
		this.eventsListenersList.add(listener);
	}

	/**************
	 * Called after service has uninstalled. Used to implement cloud resource cleanup for this service.
	 * 
	 * @param duration
	 *            time to wait for the shutdown operation.
	 * @param unit
	 *            time unit for the shutdown operations
	 * 
	 * @throws InterruptedException
	 *             If the operation was interrupted.
	 * @throws TimeoutException
	 *             If the operation exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the operation encountered an error.
	 */
	public void onServiceUninstalled(final long duration, final TimeUnit unit)
			throws InterruptedException, TimeoutException, CloudProvisioningException {

	}

}
