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
package org.cloudifysource.esc.driver.provisioning;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.cloud.Cloud;
import org.openspaces.admin.Admin;

/*****************
 * The main interface for cloud driver implementations. All calls to scale-out/scale-in/bootstrap are executed via this
 * interface. A single instance of the implementing class will exist for each service in the cluster. An instance will
 * also be created when bootstrapping or tearing down a cloud environment.
 *
 * @author barakme
 *
 */
public interface ProvisioningDriver {

	/**************
	 * Called once on startup of the cloud driver, passing the cloud configuration to it.
	 *
	 * @param cloud
	 *            The cloud configuration for this driver
	 * @param cloudTemplate
	 *            The template required for this cloud driver.
	 * @param management
	 *            true if this driver will launch management machines, false otherwise.
	 * @param serviceName
	 *            The name of the service that is planned to be installed on this machine. Could be null if this is a
	 *            management machine hosting more than one service
	 * @param performValidation
	 *            true to perform configuration validations, false otherwise
	 * @throws CloudProvisioningException Indicates invalid cloud configuration
	 */
	void setConfig(Cloud cloud, String cloudTemplate, boolean management, String serviceName, 
			boolean performValidation) throws CloudProvisioningException;

	/**
	 * Returns the compute context.
	 * @return Compute context object or null if not set
	 */
	Object getComputeContext();

	/**************
	 * Passes an Admin API object that can be used to query the current cluster state. The Admin API is typically only
	 * required for advanced use cases, like BYON. Note that this method is only called when the cloud driver is running
	 * in the ESM. The cloud driver instance used to create management machines will not get an Admin instance (in all
	 * likelihood, an Admin instance running in the client is not useful, as it will run on the wrong side of the
	 * firewall.)
	 *
	 * IMPORTANT: do not perform any blocking operations on this Admin instance as it is running in single threaded
	 * mode. Trying to use waitFor() methods on this instance will wither block forever or fail with a timeout.
	 *
	 * @param admin
	 *            an instance of the Admin API, running in single threaded mode.
	 */
	void setAdmin(Admin admin);

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
	MachineDetails startMachine(String locationId, long duration, TimeUnit unit)
			throws TimeoutException, CloudProvisioningException;

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
	MachineDetails[] startManagementMachines(long duration, TimeUnit unit)
			throws TimeoutException,
			CloudProvisioningException;

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
	boolean stopMachine(final String machineIp, final long duration, final TimeUnit unit)
			throws InterruptedException,
			TimeoutException, CloudProvisioningException;

	/*************
	 * Stops the management machines.
	 *
	 * @throws TimeoutException
	 *             in case the stop operation exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If the stop operation failed.
	 */
	void stopManagementMachines()
			throws TimeoutException, CloudProvisioningException;

	/************
	 * Returns the name of this cloud.
	 *
	 * @return the name of the cloud.
	 */
	String getCloudName();

	/*************
	 * Called when the service that this provisioning implementation is responsible for scaling is undeployed. The
	 * implementation is expected to release/close all relevant resources, such as thread pools, sockets, files, etc.
	 */
	void close();

	/**************
	 * Adds a new listener. It is the responsibility of the cloud driver developer to publish a list of supported
	 * events.
	 *
	 * @param listener
	 *            A class that implements ProvisioningDriverListner.
	 */
	void addListener(ProvisioningDriverListener listener);
	
}
