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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;
import org.cloudifysource.dsl.internal.context.RemoteStorageProvisioningDriver;

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
			logSevereAndThrow("Failed attaching volume with id " + volumeId + " to instance with ip " + ip + " : " 
					+ e.getMessage(), e);
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
		
		String volumeId = "-1";
		try {
			VolumeDetails volumeDetails = storageProvisioningDriver
					.createVolume(templateName, locationId, timeoutInMillis, 
							TimeUnit.MILLISECONDS);
			volumeId = volumeDetails.getId();
		} catch (final Exception e) {
			logSevereAndThrow("Failed creating volume in location " 
					+ locationId + " : " + e.getMessage(), e);
		}
		return volumeId;
	}

    @Override
	public void detachVolume(final String volumeId, final String ip) throws RemoteStorageOperationException {		
		try {
			storageProvisioningDriver
				.detachVolume(volumeId, ip, DEFAULT_STORAGE_OPERATION_TIMEOUT * 2, TimeUnit.MILLISECONDS);
		} catch (final Exception e) {
			logSevereAndThrow("Failed detaching volume with id " + volumeId + " to instance with ip " + ip 
					+ ", reported error: " + e.getMessage(), e);
		}
	}

	@Override
	public void deleteVolume(final String location, final String volumeId) throws RemoteStorageOperationException {
		try {
			storageProvisioningDriver
				.deleteVolume(location, volumeId, DEFAULT_STORAGE_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (final Exception e) {
			logSevereAndThrow("Failed deleting volume with id " + volumeId + ", reported error: " + e.getMessage(), e);
		}
		
	}

    @Override
    public StorageTemplate getTemplate(final String templateName) {
        return storageTemplate;
    }

    /**
     * Logs the exception as severe and throws a {@link RemoteStorageOperationException}. If the exception is 
     * serializable it is included in the newly thrown exception.
     * @param message The error message to log
     * @param e The exception to log and re-throw if possible
     * @throws RemoteStorageOperationException
     */
   private void logSevereAndThrow(final String message, final Exception e) throws RemoteStorageOperationException {
	   
	   logger.log(Level.SEVERE, message, e);
	   
	   if (isSerializable(e)) {
			throw new RemoteStorageOperationException(message, e);				
		} else {
			throw new RemoteStorageOperationException(message);
		}
   }

   
   /**
    * Checks if the given object can be serialized.
    * @param obj The object to serialize
    * @return True is serialization was successful, False otherwise
    */
   private boolean isSerializable(final Object obj) {
	   
	   boolean serializable = false;
	   try {
		   new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(obj);
		   serializable = true;
	   } catch (Exception e) {
		   // failed to serialize
	   }
	   
	   return serializable;
   }
   
   
}
