/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning.storage;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.cloudifysource.domain.cloud.Cloud;

/**
 * This interface provides an entry point to provision block storage devices and attach them to specific compute
 * instances nodes created by the {@link org.cloudifysource.esc.driver.provisioning.BaseComputeDriver} .
 * 
 * This is still a work in progress. so there may be changes to the method signatures in the near future. Also, snapshot
 * functionality will be provided.
 * 
 * @author elip
 * @author adaml
 * @since 2.5.0
 * 
 */
public interface StorageProvisioningDriver {

	/**
	 * Called once at initialization. Sets configuration members for later use. use this method to extract all necessary
	 * information needed for future provisioning calls.
	 * 
	 * @param cloud
	 *            - The {@link Cloud} Object.
	 * @param computeTemplateName
	 *            - the compute template name used to provision the machine that this volume is dedicated to.
	 */
	void setConfig(Cloud cloud, String computeTemplateName);

	/**
	 * @param templateName
	 *            The name of the template to be used when starting a new volume.
	 * @param location
	 *            The location where the storage volume will be created.
	 * @param duration
	 *            duration until times out.
	 * @param timeUnit
	 *            the duration timeout units.
	 * @return volume details object
	 * @throws TimeoutException
	 *             if execution exceeds duration.
	 * @throws StorageProvisioningException
	 *             on storage creation error. any started volumes will be removed.
	 */
	VolumeDetails createVolume(final String templateName,
			final String location, final long duration, final TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException;

	/**
	 * 
	 * @param volumeId
	 *            the ID of the volume that will attach instance.
	 * @param device
	 *            the device name of the storage volume.
	 * @param ip
	 *            the designated machine instance IP for attaching the volume.
	 * @param duration
	 *            duration until times out.
	 * @param timeUnit
	 *            the duration timeout units.
	 * @throws TimeoutException
	 *             if execution exceeds duration.
	 * @throws StorageProvisioningException
	 *             if volume is already attached, volume and machine not in same location etc.
	 */
	void attachVolume(final String volumeId, final String device,
			final String ip, final long duration, final TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException;

	/**
	 * 
	 * @param volumeId
	 *            the ID of the volume that will be detached.
	 * @param ip
	 *            the designated machine instance IP to detach from.
	 * @param duration
	 *            duration until times out.
	 * @param timeUnit
	 *            the duration timeout units.
	 * @throws TimeoutException
	 *             if execution exceeds duration.
	 * @throws StorageProvisioningException
	 *             if detaching fails.
	 */
	void detachVolume(final String volumeId, final String ip, final long duration, final TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException;

	/**
	 * 
	 * @param location
	 *            the location of the volume that will be deleted.
	 * @param volumeId
	 *            the ID of the volume that will be deleted.
	 * @param duration
	 *            duration until times out.
	 * @param timeUnit
	 *            the duration timeout units.
	 * @throws TimeoutException
	 *             if execution exceeds duration.
	 * @throws StorageProvisioningException
	 *             if deletion of volume fails.
	 */
	void deleteVolume(final String location, final String volumeId, final long duration, final TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException;

	/**
	 * 
	 * @param ip
	 *            the machine instance IP.
	 * @param duration
	 *            duration until times out.
	 * @param timeUnit
	 *            the duration timeout units.
	 * @return a set of VolumeDetails containing details about the machine volumes.
	 * @throws TimeoutException
	 *             if execution exceeds duration.
	 * @throws StorageProvisioningException
	 *             if list fails.
	 */
	Set<VolumeDetails> listVolumes(final String ip, final long duration, final TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException;

	/**
	 * 
	 * @param volumeId
	 *            the ID of the volume that will be detached.
	 * @return the volume name according to volumeId.
	 * @throws StorageProvisioningException
	 *             if fails retrieving volume name.
	 * 
	 */
	String getVolumeName(final String volumeId)
			throws StorageProvisioningException;

	/**
	 * Sets the jClouds compute context if exists.
	 * 
	 * @param context
	 *            jClouds compute context
	 * 
	 * @throws StorageProvisioningException
	 *             In-case context does not match the specified storage driver.
	 */
	void setComputeContext(final Object context) throws StorageProvisioningException;

	/**
	 * Close all resources.
	 */
	void close();

}
