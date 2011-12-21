package com.gigaspaces.cloudify.esc.driver.provisioning;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openspaces.admin.Admin;

import com.gigaspaces.cloudify.dsl.cloud.Cloud2;



/*****************
 * The main interface for cloud driver implementations. All calls to scale-out/scale-in/bootstrap are executed via this interface.
 * A single instance of the implementing class will exist for each service in the cluster. An instance will also be created
 * when bootstrapping or tearing down a cloud environment.
 * 
 * @author barakme
 *
 */
public interface CloudifyProvisioning {

	/**************
	 * Passes a configuration map for all setting defined for this cloud.
	 * @param cloudTemplate 
	 * @param cloud 
	 * 
	 * @param config The configuration settings.
	 */
	void setConfig(Cloud2 cloud, String cloudTemplate, boolean management);
	
	/**************
	 * Passes an Admin API object that can be used to query the current cluster state.
	 * IMPORTANT: do not perform any blocking operations on this Admin instance, 
	 * 
	 * @param config The configuration settings.
	 */
	void setAdmin(Admin admin);
	
	/*************
	 * Sets up a cloud environment. This includes (at-least) setting up cloudify management servers and running the required processes on them.
	 * Additional steps may apply for some clouds.
	 * 
	 * @param cloud The cloud settings.
	 * @throws CloudProvisioningException If provisioning of the cloud environment failed.
	 */	 
//	void bootstrapCloud(Cloud2 cloud) throws CloudProvisioningException; 
	
	/*************
	 * Tears down a running cloud environment. It is the responsibility of the cloud driver to release all cloud resources relevant
	 * to this environment. 
	 * 
	 * Implementations may choose to uninstall all running applications and services before shutting down the machines, or just to close down
	 * all running instances. It is recommended to shut down all applications and services before tearing down the cloud environment. 
	 * @param cloud The cloud settings.
	 * @throws CloudProvisioningException If a problem was encountered while shutting down the cloud environment.
	 */
//	void teardownCloud(Cloud2 cloud) throws CloudProvisioningException;
	  
	
	/***************
	 * Starts an additional machine on the cloud to scale out this specific service.
	 *   
	 * @param duration Time duration to wait for the instance.
	 * @param unit Time unit to wait for the instance.
	 * @return The details of the started instance.
	 * @throws TimeoutException In case the instance was not started in the allotted time.
	 * @throws CloudProvisioningException If a problem was encountered while starting the machine.
	 */
	MachineDetails startMachine(long duration, TimeUnit unit) throws TimeoutException, CloudProvisioningException; 
	
	MachineDetails[] startManagementMachines(long duration, TimeUnit unit) throws TimeoutException, CloudProvisioningException;
	void  stopManagementMachines() throws TimeoutException, CloudProvisioningException;
	
	/****************
	 * Stops a specific machine for scaling in or shutting down a specific service.
	 * @throws CloudProvisioningException
	 */
	boolean stopMachine(final String machineIp) throws CloudProvisioningException;

	
	/************
	 * Returns the name of this cloud.
	 * @return the name of the cloud.
	 */
	String getCloudName();

	/*************
	 * Called when the service that this provisioning implementation is responsible for scaling
	 * is undeployed. The implementation is expected to release/close all relevant resources,
	 * such as thread pools, sockets, files, etc.
	 */
	void close();
}
