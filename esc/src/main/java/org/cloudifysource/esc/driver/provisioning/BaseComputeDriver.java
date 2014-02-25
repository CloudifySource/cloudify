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
package org.cloudifysource.esc.driver.provisioning;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.rest.response.ControllerDetails;
import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;
import org.cloudifysource.esc.driver.provisioning.context.ValidationContext;

/***********
 * Base class for all compute driver implementations. A compute driver is a class that is responsible for allocating
 * compute resources for Cloudify service instances.
 * 
 * Includes some default implementations. This is the core
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
public abstract class BaseComputeDriver {

	protected ComputeDriverConfiguration configuration;
	protected final List<ProvisioningDriverListener> eventsListenersList = new LinkedList<ProvisioningDriverListener>();
	protected ProvisioningDriverClassContext provisioningContext;
	protected File customDataFile;

	/**************
	 * Called once on startup of the cloud driver, passing the cloud configuration to it.
	 * 
	 * @param configuration
	 *            the driver configuration.
	 * @throws CloudProvisioningException
	 *             Indicates invalid cloud configuration
	 */
	public void setConfig(final ComputeDriverConfiguration configuration)
			throws CloudProvisioningException {
		this.configuration = configuration;
	}

	/**
	 * A compute driver may choose to give access to the internal 'context' which is used to communicate with the cloud
	 * API. This is an optional interface, and defaults to returning null.
	 * 
	 * @return Compute context object or null if not set.
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
	 * @param context
	 *            the provisioning context for this machine.
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

	/******************
	 * Start the management machines for this cluster. This method is called once by the cloud administrator when
	 * bootstrapping a new cluster.
	 * 
	 * @param duration
	 *            timeout duration.
	 * @param unit
	 *            timeout unit.
	 * @param context
	 *            the provisioning context for this request.
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
	
	/*************
	 * Terminates all cloud resources, identified by their prefix.
	 * 
	 * @param duration
	 *            time to wait for the shutdown operation.
	 * @param unit
	 *            time unit for the shutdown operations
	 * @throws TimeoutException
	 *             in case the operation exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the operation failed.
	 */
	public void terminateAllResources(final long duration, final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException {
		unsupported();
	}

	/************
	 * Returns the name of this cloud.
	 * 
	 * @return the name of the cloud.
	 */
	public String getCloudName() {

		unsupported();
		return null;

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

	/*******
	 * Setter for the provisioning context.
	 * 
	 * @param context
	 *            the provisioning context.
	 */
	public void setProvisioningDriverClassContext(final ProvisioningDriverClassContext context) {
		this.provisioningContext = context;
	}

	/*********
	 * Sets the custom data file for the cloud driver instance of a specific service.
	 * 
	 * @param customDataFile
	 *            the custom data file (may be a folder).
	 */
	public void setCustomDataFile(final File customDataFile) {
		this.customDataFile = customDataFile;
	}

	/**********
	 * Return existing management servers.
	 * 
	 * @return the existing management servers, or a 0-length array if non exist.
	 * @throws CloudProvisioningException
	 *             if failed to load servers list from cloud.
	 */
	public MachineDetails[] getExistingManagementServers() throws CloudProvisioningException {
		unsupported();
		return null;
	}

	/**********
	 * Return existing management servers based on controller information saved previously.
	 * 
	 * @param controllers
	 *            the controller information used to locate the machine details.
	 * @return the existing management servers, or a 0-length array if non exist.
	 * @throws CloudProvisioningException
	 *             if failed to load servers list from cloud.
	 * @throws UnsupportedOperationException
	 *             if the cloud driver does not support this operation.
	 */
	public MachineDetails[] getExistingManagementServers(final ControllerDetails[] controllers)
			throws CloudProvisioningException, UnsupportedOperationException {
		unsupported();
		return null;
	}

	/**
	 * Cloud-specific validations called after setConfig and before machines are allocated.
	 * 
	 * @param validationContext
	 *            The object through which writing of validation messages is done
	 * @throws CloudProvisioningException
	 *             Indicates invalid configuration
	 */
	public void validateCloudConfiguration(final ValidationContext validationContext)
			throws CloudProvisioningException {

	}

	private void unsupported() {
		throw new UnsupportedOperationException("Method not implemented");
	}
}
