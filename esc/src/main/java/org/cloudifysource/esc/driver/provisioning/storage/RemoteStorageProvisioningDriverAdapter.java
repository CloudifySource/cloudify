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

package org.cloudifysource.esc.driver.provisioning.storage;

import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;
import org.cloudifysource.dsl.internal.context.RemoteStorageProvisioningDriver;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author elip
 *
 */
public class RemoteStorageProvisioningDriverAdapter implements RemoteStorageProvisioningDriver {

    private Logger logger = java.util.logging.Logger
            .getLogger(RemoteStorageProvisioningDriverAdapter.class.getName());

	private static final long DEFAULT_STORAGE_OPERATION_TIMEOUT = 60 * 1000;
	
	private StorageProvisioningDriver storageProvisioningDriver;
    private StorageTemplate storageTemplate;
	
	public RemoteStorageProvisioningDriverAdapter(final StorageProvisioningDriver driver,
                                                  final StorageTemplate storageTemplate) {
		this.storageProvisioningDriver = driver;
        this.storageTemplate = storageTemplate;
	}

	@Override
	public void attachVolume(final String volumeId, final String device, final String ip) 
			throws RemoteStorageOperationException {
		try {
			storageProvisioningDriver
				.attachVolume(volumeId, device, ip, DEFAULT_STORAGE_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (final Exception e) {
         logSevere(e);
			throw new RemoteStorageOperationException("Failed attaching volume with id " + volumeId 
					+ " to instance with ip " + ip + " : " + e.getMessage(), e);
		}
	}

	@Override
	public String createVolume(final String templateName, final String locationId)
			throws RemoteStorageOperationException, TimeoutException {
		return createVolume(templateName, locationId, DEFAULT_STORAGE_OPERATION_TIMEOUT);
	}
	
	@Override
	public String createVolume(final String templateName, final String locationId,
			final long timeoutInMillis) throws RemoteStorageOperationException, TimeoutException {
		try {
			VolumeDetails volumeDetails = storageProvisioningDriver
					.createVolume(templateName, locationId, timeoutInMillis, 
							TimeUnit.MILLISECONDS);
			return volumeDetails.getId();
		} catch (final StorageProvisioningException e) {
         logSevere(e);
			throw new RemoteStorageOperationException("Failed creating volume in location " 
						+ locationId + " : " + e.getMessage(), e);			
		}

	}

    @Override
	public void detachVolume(final String volumeId, final String ip) throws RemoteStorageOperationException {		
		try {
			storageProvisioningDriver
				.detachVolume(volumeId, ip, DEFAULT_STORAGE_OPERATION_TIMEOUT * 2, TimeUnit.MILLISECONDS);
		} catch (final Exception e) {
         logSevere(e);
			throw new RemoteStorageOperationException("Failed detaching volume with id " 
						+ volumeId + " to instance with ip " + ip, e);
		}
	}

	@Override
	public void deleteVolume(final String location, final String volumeId) throws RemoteStorageOperationException {
		try {
			storageProvisioningDriver
				.deleteVolume(location, volumeId, DEFAULT_STORAGE_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (final Exception e) {
         logSevere(e);
			throw new RemoteStorageOperationException("Failed deleting volume with id " 
					+ volumeId , e);			
		}
		
	}

    @Override
    public StorageTemplate getTemplate(String templateName) {
        return storageTemplate;
    }

   private void logSevere(final Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
   }
}
