/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.utilitydomain.context.blockstorage;

import java.util.concurrent.TimeoutException;

import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.domain.context.ServiceContext;
import org.cloudifysource.domain.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.domain.context.blockstorage.RemoteStorageOperationException;
import org.cloudifysource.domain.context.blockstorage.StorageFacade;
import org.cloudifysource.dsl.internal.context.RemoteStorageProvisioningDriver;
import org.cloudifysource.utilitydomain.context.kvstore.AttributesFacadeImpl;
import org.openspaces.core.GigaSpace;

import com.gigaspaces.client.ChangeSet;

/**
 *
 * @author elip
 *
 */
public class StorageFacadeImpl implements StorageFacade {

	private static final long AFTER_ATTACH_TIMEOUT = 10 * 1000;

	private final ServiceContext serviceContext;
	private final RemoteStorageProvisioningDriver remoteStorageProvisioningDriver;
	private final GigaSpace managementSpace;

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(StorageFacade.class.getName());

	public StorageFacadeImpl(final ServiceContext serviceContext,
			final RemoteStorageProvisioningDriver storageApi) {
		this.serviceContext = serviceContext;
		this.remoteStorageProvisioningDriver = storageApi;
		this.managementSpace = ((AttributesFacadeImpl) serviceContext.getAttributes()).getManagementSpace();
		logger.fine("Storage Facade initiated successfully");
	}

	@Override
	public void attachVolume(final String volumeId, final String device)
			throws RemoteStorageOperationException, LocalStorageOperationException {
		validateNotWindows();
		logger.info("Attaching volume with id " + volumeId + " to device " + device);
		safeGetRemoteStorageProvisioningDriver().attachVolume(volumeId, device, serviceContext.getBindAddress());
		changeStateOfVolumeWithId(volumeId, VolumeState.ATTACHED);
		setDeviceForVolumeWithId(volumeId, device);
		try {
			// ugly as hell. can't really figure out how to wait here properly for the volume to actually be attached.
			// see CLOUDIFY-1551
			Thread.sleep(AFTER_ATTACH_TIMEOUT);
		} catch (InterruptedException e) {
			throw new LocalStorageOperationException(e);
		}
	}

	@Override
	public String createVolume(final String templateName) throws RemoteStorageOperationException, TimeoutException {
		logger.info("Creating volume for service " + serviceContext.getServiceName() + ". Using template : "
				+ templateName);
		String volumeId =
				safeGetRemoteStorageProvisioningDriver().createVolume(templateName, serviceContext.getLocationId());
		writeNewVolume(volumeId);
		return volumeId;
	}

	@Override
	public String createVolume(final String templateName, final long timeoutInMillis)
			throws RemoteStorageOperationException, TimeoutException {
		logger.info("Creating volume for service " + serviceContext.getServiceName() + ". Using template : "
				+ templateName);
		String volumeId = safeGetRemoteStorageProvisioningDriver().
				createVolume(templateName, serviceContext.getLocationId(), timeoutInMillis);
		writeNewVolume(volumeId);
		return volumeId;
	}

	@Override
	public void detachVolume(final String volumeId)
			throws RemoteStorageOperationException, LocalStorageOperationException {
		validateNotWindows();
		logger.info("Detaching volume with id " + volumeId);
		safeGetRemoteStorageProvisioningDriver().detachVolume(volumeId, serviceContext.getBindAddress());
	}

	@Override
	public void deleteVolume(final String volumeId) throws RemoteStorageOperationException {
		validateNotWindows();
		logger.info("Deleting volume with id " + volumeId);
		safeGetRemoteStorageProvisioningDriver().deleteVolume(serviceContext.getLocationId(), volumeId);
		deleteVolumeWithId(volumeId);
	}

	@Override
	public void mount(final String volumeId, final String device, final String path, final long timeoutInMillis)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot mount when not running in privileged mode");
		}
		logger.info("Mounting device " + device + " to mount point " + path);
		VolumeUtils.mount(device, path, timeoutInMillis);
		changeStateOfVolumeWithId(volumeId, VolumeState.MOUNTED);

	}

	@Override
	public void mount(final String volumeId, final String device, final String path)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot mount when not running in privileged mode");
		}
		logger.info("Mounting device " + device + " to mount point " + path);
		VolumeUtils.mount(device, path);
		changeStateOfVolumeWithId(volumeId, VolumeState.MOUNTED);
	}

	@Override
	public void unmount(final String device, final long timeoutInMillis)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot unmount when not running in privileged mode");
		}
		logger.info("Unmounting device " + device);
		VolumeUtils.unmount(device, timeoutInMillis);
	}

	@Override
	public void unmount(final String device)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot unmount when not running in privileged mode");
		}
		logger.info("Unmounting device " + device);
		VolumeUtils.unmount(device);
	}

	@Override
	public StorageTemplate getTemplate(final String templateName) {
		return safeGetRemoteStorageProvisioningDriver().getTemplate(templateName);
	}
	
	@Override
	public void partition(final String volumeId, final String device, final long timeoutInMillis)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot partition when not running in privileged mode");
		}
		logger.info("Partitioning device " + device);
		VolumeUtils.partition(device, timeoutInMillis);
		changeStateOfVolumeWithId(volumeId, VolumeState.PARTITIONED);

	}

	@Override
	public void partition(final String volumeId, final String device)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot partition when not running in privileged mode");
		}
		logger.info("Partitioning device " + device);
		VolumeUtils.partition(device);
		changeStateOfVolumeWithId(volumeId, VolumeState.PARTITIONED);
	}


	@Override
	public void format(final String volumeId, final String device, final String fileSystem, final long timeoutInMillis)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot format when not running in privileged mode");
		}
		logger.info("Formatting device " + device + " to File System " + fileSystem);
		VolumeUtils.format(device, fileSystem, timeoutInMillis);
		changeStateOfVolumeWithId(volumeId, VolumeState.FORMATTED);

	}

	@Override
	public void format(final String volumeId, final String device, final String fileSystem)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot format when not running in privileged mode");
		}
		logger.info("Formatting device " + device + " to File System " + fileSystem);
		VolumeUtils.format(device, fileSystem);
		changeStateOfVolumeWithId(volumeId, VolumeState.FORMATTED);
	}

	private void validateNotWindows() {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			throw new UnsupportedOperationException("Windows OS is not supported for Storage API");
		}
	}

	private void deleteVolumeWithId(final String volumeId) {
		managementSpace.takeById(ServiceVolume.class, volumeId);
	}

	private void writeNewVolume(final String volumeId) {
		ServiceVolume serviceVolume = new ServiceVolume();
		serviceVolume.setApplicationName(serviceContext.getApplicationName());
		serviceVolume.setServiceName(serviceContext.getServiceName());
		serviceVolume.setIp(serviceContext.getBindAddress());
		serviceVolume.setId(volumeId);
		serviceVolume.setState(VolumeState.CREATED);
		logger.fine("Writing new service volume : " + serviceVolume + " to management space");
		managementSpace.write(serviceVolume);
	}

	private void changeStateOfVolumeWithId(final String volumeId, final VolumeState newState) {
		ServiceVolume serviceVolume = new ServiceVolume();
		serviceVolume.setId(volumeId);
		logger.fine("Changing state of volume with id " + volumeId + " to " + newState);
		managementSpace.change(serviceVolume, new ChangeSet().set("state", newState));
	}

	private void setDeviceForVolumeWithId(final String volumeId, final String device) {
		ServiceVolume serviceVolume = new ServiceVolume();
		serviceVolume.setId(volumeId);
		logger.fine("Changing device of volume with id " + volumeId + " to " + device);
		managementSpace.change(serviceVolume, new ChangeSet().set("device", device));
	}

	private RemoteStorageProvisioningDriver safeGetRemoteStorageProvisioningDriver() {
		if (remoteStorageProvisioningDriver == null) {
			throw new IllegalStateException(
					"No storage provisioning driver configured, remote provisioning calls are not possible");
		}
		return remoteStorageProvisioningDriver;
	}

}
