/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
 ******************************************************************************/
package org.cloudifysource.esc.driver.provisioning.storage;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.esc.driver.provisioning.CloudProvisioningException;
import org.cloudifysource.esc.driver.provisioning.ProvisioningContext;

/*****
 * an abstraction for a base storage driver to extend driver functionality.
 * 
 * @author adaml
 *
 */
public abstract class BaseStorageDriver implements StorageProvisioningDriver {

	/**
	 * sets the jClouds context.
	 * @param computeContext
	 * 			jClouds context object.
	 * @throws StorageProvisioningException
	 * 			if context is not instance of the expected storage driver context.
	 */
	public abstract void setComputeContext(final Object computeContext) 
					throws StorageProvisioningException;
	
	/**
	 * returns a set of volume IDs attached with a machine.
	 * 
	 * @param ip 
	 * 			the machine ip.
	 * @return
	 * 			a set of volume IDs.
	 * @throws StorageProvisioningException
	 * 			if action failed.
	 */
	public Set<String> getMachineVolumeIds(final String ip) 
					throws StorageProvisioningException {
		return null;
	}
	
	/**
	 * lists all existing volumes. 
	 * 
	 * @return 
	 * 			a set containing all volumes. 
	 * @throws StorageProvisioningException
	 * 			if list action failed.
	 */
	public abstract Set<VolumeDetails> listAllVolumes()
			throws StorageProvisioningException; 
	
	/**
	 * terminates all existing volumes.
	 * 
	 * @param duration
	 *            Duration until times out.
	 * @param timeUnit
	 *            The duration timeout units.
	 * @throws StorageProvisioningException
	 * 			if searching or terminating volumes failed.
	 * @throws TimeoutException
	 * 			if timeout was reached before all volumes terminated.
	 */
	public void terminateAllVolumes(final long duration, final TimeUnit timeUnit) 
			throws StorageProvisioningException, TimeoutException {
		throw new UnsupportedOperationException("Method not implemented");
	}
	
	
	/**
	 * Called after machine failure occurred. Useful for storage resource cleanup.
	 * 
	 * @param context
	 *            the provisioning context for the failed machine.
	 * @param templateName
	 *            the name of the storage template used
	 * @param duration
	 *            Time duration to wait for the operation to complete
	 * @param unit
	 *            Time unit to wait for the operation to complete
	 * @throws TimeoutException
	 *             If the operation exceeded the given timeout.
	 * @throws CloudProvisioningException
	 *             If a compute related operation encountered an error.
	 * @throws StorageProvisioningException
	 *             If a storage related operation encountered an error.
	 */
	public void onMachineFailure(final ProvisioningContext context, final String templateName, final long duration, 
			final TimeUnit unit)
			throws TimeoutException, CloudProvisioningException, StorageProvisioningException {		
	}
	
}
