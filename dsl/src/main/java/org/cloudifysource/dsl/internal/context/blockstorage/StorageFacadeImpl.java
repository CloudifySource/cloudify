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

package org.cloudifysource.dsl.internal.context.blockstorage;

import com.gigaspaces.client.ChangeSet;
import org.cloudifysource.dsl.cloud.storage.ServiceVolume;
import org.cloudifysource.dsl.cloud.storage.StorageTemplate;
import org.cloudifysource.dsl.cloud.storage.VolumeState;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.dsl.context.blockstorage.RemoteStorageOperationException;
import org.cloudifysource.dsl.context.blockstorage.StorageFacade;
import org.cloudifysource.dsl.context.utils.VolumeUtils;
import org.cloudifysource.dsl.internal.context.RemoteStorageProvisioningDriver;
import org.openspaces.core.GigaSpace;

import java.util.concurrent.TimeoutException;

/**
 * 
 * @author elip
 *
 */
public class StorageFacadeImpl implements StorageFacade {

	private static final long AFTER_ATTACH_TIMEOUT = 10 * 1000;
	
	private ServiceContext serviceContext;
	private RemoteStorageProvisioningDriver remoteStorageProvisioningDriver;
    private GigaSpace managementSpace;
	
	
	public StorageFacadeImpl(final ServiceContext serviceContext,
			final RemoteStorageProvisioningDriver storageApi) {
		this.serviceContext = serviceContext;
		this.remoteStorageProvisioningDriver = storageApi;
        this.managementSpace = serviceContext.getAttributes().getManagementSpace();
        // this object is initialized in a lazy manner.
        // which means this constructor is called only when we actually use the storage API.
        // so lets go ahead and write our empty ServiceVolume instance to the management space.
        // this will allow us to only update this instance in the future, without worrying if it exists or not.
        writeEmptyServiceVolume();
	}

    private void writeEmptyServiceVolume() {
        ServiceVolume serviceVolume = new ServiceVolume(serviceContext.getApplicationName(), serviceContext.getServiceName(), serviceContext.getInstanceId());
        managementSpace.write(serviceVolume);
    }

    @Override
	public void attachVolume(final String volumeId, final String device) 
					throws RemoteStorageOperationException, LocalStorageOperationException {
		validateNotWindows();
		remoteStorageProvisioningDriver.attachVolume(volumeId, device, serviceContext.getBindAddress());
        changeVolumeState(VolumeState.ATTACHED);
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
        String volumeId = remoteStorageProvisioningDriver.createVolume(templateName, serviceContext.getLocationId());
        setVolumeId(volumeId);
        changeVolumeState(VolumeState.CREATED);
        return volumeId;
	}
	
	@Override
	public String createVolume(final String templateName, final long timeoutInMillis)
			throws RemoteStorageOperationException, TimeoutException {
        String volumeId = remoteStorageProvisioningDriver.
                createVolume(templateName, serviceContext.getLocationId(), timeoutInMillis);
        setVolumeId(volumeId);
        changeVolumeState(VolumeState.CREATED);
        return volumeId;
	}

	@Override
	public void detachVolume(final String volumeId) 
			throws RemoteStorageOperationException, LocalStorageOperationException {
		validateNotWindows();
		remoteStorageProvisioningDriver.detachVolume(volumeId, serviceContext.getBindAddress());
	}

	@Override
	public void deleteVolume(final String volumeId) throws RemoteStorageOperationException {
		validateNotWindows();
		remoteStorageProvisioningDriver.deleteVolume(serviceContext.getLocationId(), volumeId);
	}
	

	@Override
	public void mount(final String device, final String path, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot mount when not running in privileged mode");
		}
		VolumeUtils.mount(device, path, timeoutInMillis);
        changeVolumeState(VolumeState.MOUNTED);
		
	}

	@Override
	public void mount(final String device, final String path) 
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot mount when not running in privileged mode");
		}
		VolumeUtils.mount(device, path);
        changeVolumeState(VolumeState.MOUNTED);
	}

	@Override
	public void unmount(final String device, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot unmount when not running in privileged mode");
		}
		VolumeUtils.unmount(device, timeoutInMillis);
	}

	@Override
	public void unmount(final String device) 
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot unmount when not running in privileged mode");
		}
		VolumeUtils.unmount(device);
	}

    @Override
    public StorageTemplate getTemplate(final String templateName) {
        return remoteStorageProvisioningDriver.getTemplate(templateName);
    }

    @Override
	public void format(final String device, final String fileSystem, final long timeoutInMillis)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot format when not running in privileged mode");
		}
		VolumeUtils.format(device, fileSystem, timeoutInMillis);
        changeVolumeState(VolumeState.FORMATTED);
		
	}

	@Override
	public void format(final String device, final String fileSystem) 
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot format when not running in privileged mode");
		}
		VolumeUtils.format(device, fileSystem);
        changeVolumeState(VolumeState.FORMATTED);
	}

    private void validateNotWindows() {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			throw new UnsupportedOperationException("Windows OS is not supported for Storage API");
		}
	}

    private void changeVolumeState(final VolumeState volumeState) {
        ServiceVolume serviceVolume = new ServiceVolume(serviceContext.getApplicationName(), serviceContext.getServiceName(), serviceContext.getInstanceId());
        managementSpace.change(serviceVolume, new ChangeSet().set("state", volumeState));
    }

    private void setVolumeId(final String volumeId) {
        ServiceVolume serviceVolume = new ServiceVolume(serviceContext.getApplicationName(), serviceContext.getServiceName(), serviceContext.getInstanceId());
        managementSpace.change(serviceVolume, new ChangeSet().set("id", volumeId));
    }
}
