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
 *******************************************************************************/

package org.cloudifysource.domain.context.blockstorage;

import java.util.concurrent.TimeoutException;

import org.cloudifysource.domain.cloud.storage.StorageTemplate;


/**
 * 
 * @author elip
 *
 */
public interface StorageFacade {
	
	/*************************
	 * Attaches a volume storage device to the local machine.
	 * @param volumeId - the id of the volume to be attached.
	 * @param device - the device name of the volume.
	 * @throws RemoteStorageOperationException - thrown in case something went wrong during the remote call.
	 * @throws LocalStorageOperationException - 
	 * 					thrown in case a local operation on the storage volume failed (mount, format..)
	 */
	void attachVolume(final String volumeId, final String device) 
			throws RemoteStorageOperationException, LocalStorageOperationException;
	
	
	/***************************
	 * Creates a volume from the given template name. the template must be pre defined in the cloud configuration file
	 * the bootstrap was performed with.
	 * will use a default timeout of 1 minute.
	 * @param templateName - the storage template name to be used when creating the volume.
	 * @return the volume id.
	 * @throws RemoteStorageOperationException - thrown in case something went wrong during the remote call.
	 * @throws TimeoutException - thrown when there was a timeout in creating the volume.
	 */
	String createVolume(final String templateName) 
			throws RemoteStorageOperationException, TimeoutException;
	
	/***************************
	 * Creates a volume from the given template name. the template must be pre defined in the cloud configuration file
	 * the bootstrap was performed with.
	 * @param templateName - the storage template name to be used when creating the volume.
	 * @param timeoutInMillis - the timeout for the createVolume API call. 
	 * 				if the timeout is exceeded but the volume was created
	 * 				a call to {@link StorageFacade#deleteVolume(String)} will be executed.
	 * @return the volume id.
	 * @throws RemoteStorageOperationException - thrown in case something went wrong during the remote call.
	 * @throws TimeoutException - thrown when there was a timeout in creating the volume.
	 */
	String createVolume(final String templateName, final long timeoutInMillis) 
			throws RemoteStorageOperationException, TimeoutException;
	
	/***************************
	 * detaches the volume from the machine.
	 * @param volumeId - the volume id to detach.
	 * @throws RemoteStorageOperationException - thrown in case something went wrong during the remote call.
	 * @throws LocalStorageOperationException -  
	 * 				thrown in case a local operation on the storage volume failed (mount, format..)
	 */
	void detachVolume(final String volumeId) throws RemoteStorageOperationException, 
			LocalStorageOperationException;
	
	/**************************
	 * deletes the volume with the given id.
	 * @param volumeId - the volume id to delete.
	 * @throws RemoteStorageOperationException - thrown in case something went wrong during the remote call.
	 */
	void deleteVolume(final String volumeId) throws RemoteStorageOperationException;
	
	/**
	 * partition the device according to the following - create a new, single, primary partition, on the entire volume.
	 * @param volumeId - the id of the volume to be partitioned. 
	 * @param device - device name.
	 * @param timeoutInMillis - the timeout after which the process will be terminated forcefully.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void partition(final String volumeId, final String device, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException;
	

	/**
	 * partition the device according to the following - create a new, single, primary partition, on the entire volume.
	 * @param device - device name.
	 * @param timeoutInMillis - the timeout after which the process will be terminated forcefully.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void partition(final String device, final long timeoutInMillis)
			throws LocalStorageOperationException, TimeoutException;
	
	/**
	 * partition the device according to the following - create a new, single, primary partition, on the entire volume.
	 * @param device - device name.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void partition(final String device)
			throws LocalStorageOperationException, TimeoutException;

	/**
	 * partition the device according to the following - create a new, single, primary partition, on the entire volume.
	 * uses a default timeout of 30 seconds. 
	 * to use a custom timeout use {@link StorageFacade#partition(String, long)}.
	 * @param volumeId - the id of the volume to be partitioned.
	 * @param device - device name.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void partition(final String volumeId, final String device) 
			throws LocalStorageOperationException, TimeoutException;
	
	/**
	 * format a device to a given file system.
	 * @param volumeId - the id of the volume to be formatted.
	 * @param device - device name.
	 * @param fileSystem - file system type.
	 * @param timeoutInMillis - the timeout after which the process will be terminated forcefully.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void format(final String volumeId, final String device, final String fileSystem, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException;
	
	/**
	 * format a device to a given file system.
	 * @param device - device name.
	 * @param fileSystem - file system type.
	 * @param timeoutInMillis - the timeout after which the process will be terminated forcefully.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void format(final String device, final String fileSystem, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException;
	
	/**
	 * format a device to a given file system.
	 * @param device - device name.
	 * @param fileSystem - file system type.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void format(final String device, final String fileSystem)
			throws LocalStorageOperationException, TimeoutException;

	/**
	 * format a device to a given file system.
	 * uses a default timeout of 5 minutes. 
	 * to use a custom timeout use {@link StorageFacade#format(String, String, long)}.
	 * @param volumeId - the id of the volume to be formatted.
	 * @param device - device name.
	 * @param fileSystem - file system type.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void format(final String volumeId, final String device, final String fileSystem) 
			throws LocalStorageOperationException, TimeoutException;

	/**
	 * mount a device to a local mounting point.
	 * @param volumeId - the id of the volume to be mounted.
	 * @param device - device name.
	 * @param path - mounting point (will be created automatically).
	 * @param timeoutInMillis - the timeout after which the process will be terminated forcefully.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void mount(final String volumeId, final String device, final String path, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException;
	
	/**
	 * mount a device to a local mounting point.
	 * @param device - device name.
	 * @param path - mounting point (will be created automatically).
	 * @param timeoutInMillis - the timeout after which the process will be terminated forcefully.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void mount(final String device, final String path, final long timeoutInMillis)
			throws LocalStorageOperationException, TimeoutException;
	
	/**
	 * mount a device to a local mounting point.
	 * @param device - device name.
	 * @param path - mounting point (will be created automatically).
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void mount(final String device, final String path)
			throws LocalStorageOperationException, TimeoutException;
	
	/**
	 * mount a device to a local mounting point.
	 * uses a default timeout of 30 seconds. to use a custom timeout 
	 * 	use {@link StorageFacade#mount(String, String, long)}.
	 * @param volumeId - the id of the volume to be mounted.
	 * @param device - device name.
	 * @param path - mounting point (will be created automatically).
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void mount(final String volumeId, final String device, final String path) 
			throws LocalStorageOperationException, TimeoutException;
	
	/**
	 * unmounts a device from the file system. usually used before detaching it.	
	 * @param device - the device to unmount
	 * @param timeoutInMillis - the timeout for executing the process.
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void unmount(final String device, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException;
	
	/**
	 * unmounts a device from the file system. usually used before detaching it.
	 * uses default timeout of 15 seconds. to use a custom timeout use {@link StorageFacade#unmount(String, long)}.
	 * @param device - the device to unmount
	 * @throws LocalStorageOperationException - thrown when the command fails.
	 * @throws TimeoutException - in case of a timeout.
	 */
	void unmount(final String device) 
			throws LocalStorageOperationException, TimeoutException;

    /**
     * get the storage template defined in the cloud driver.
     * @param templateName - the template name.
     * @return {@link StorageTemplate}.
     */
    StorageTemplate getTemplate(final String templateName);
}
