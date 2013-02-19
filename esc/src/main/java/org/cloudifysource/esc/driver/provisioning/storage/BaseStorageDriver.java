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
	
}
