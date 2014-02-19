/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.driver.provisioning.storage.openstack;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriverListener;
import org.cloudifysource.esc.driver.provisioning.openstack.OpenStackCloudifyDriver;
import org.cloudifysource.esc.driver.provisioning.storage.BaseStorageDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.domain.Volume;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.options.CreateVolumeOptions;
import org.jclouds.rest.RestContext;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.inject.Module;

/**
 * Storage Provisioning implementation on Openstack Grizzly.
 * @author noak
 * @since 2.7.0
 */
public class OpenstackStorageDriver extends BaseStorageDriver implements StorageProvisioningDriver {
	
	private static final String ENDPOINT_KEY = "jclouds.endpoint";
	private static final String API_VERSION_KEY = "jclouds.api-version";
	private static final String API_VERSION_VALUE = "2";
	
	private static final int VOLUME_POLLING_INTERVAL_MILLIS = 10 * 1000; // 10 seconds
	private static final String VOLUME_DESCRIPTION = "Cloudify generated volume";
	private static final String EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API = "try_to_connect_to_cloud_api";
	private static final String EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API = "connection_to_cloud_api_succeeded";
	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(OpenstackStorageDriver.class.getName());
	
	private ComputeTemplate computeTemplate;
	private ComputeServiceContext computeContext;
	private JCloudsDeployer deployer;
	private RestContext<NovaApi, NovaAsyncApi> novaContext;
	private String region;
	private Cloud cloud;
	
	protected final List<ProvisioningDriverListener> eventsListenersList = new LinkedList<ProvisioningDriverListener>();

	
	@Override
	public void setComputeContext(final Object computeContext) {
		// expected to be null since the Openstack compute provisioning driver sets this to null 
		// and does not use jclouds
	}
	
	@Override
	public void setConfig(final Cloud cloud, final String computeTemplateName) {
		logger.fine("Initializing storage provisioning on Openstack");
		this.cloud = cloud;
		final String provider = cloud.getProvider().getProvider();
        
        computeTemplate = cloud.getCloudCompute().getTemplates().get(computeTemplateName);
        		
        publishEvent(EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API, provider);
		publishEvent(EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API, provider);
		
        logger.fine("Creating JClouds context");
		initDeployer();
		computeContext = deployer.getContext();
		novaContext = this.computeContext.unwrap();

		region = getRegionFromHardwareId(computeTemplate.getHardwareId());
	}


	@Override
	public VolumeDetails createVolume(final String templateName, final String availabilityZone, 
			final long duration, final TimeUnit timeUnit) throws 
		TimeoutException, StorageProvisioningException {
		
		final long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);
		final VolumeDetails volumeDetails = new VolumeDetails();
		Volume volume;
		
		//ignoring the passed location, it's a wrong format, taking the compute location instead
		Optional<? extends VolumeApi> volumeApi = getVolumeApi();

		if (!volumeApi.isPresent()) {
			throw new StorageProvisioningException("Failed to create volume, Openstack API is not initialized.");
		}
		
		if (computeContext == null) {
			throw new StorageProvisioningException("Failed to create volume, compute context is not initialized.");
		}
		
		StorageTemplate storageTemplate = this.cloud.getCloudStorage().getTemplates().get(templateName);
		String volumeName = storageTemplate.getNamePrefix() + System.currentTimeMillis();
		int size = storageTemplate.getSize();
		logger.fine("Creating new volume in availability zone \"" + availabilityZone + "\" of size " + size
                + " GB, with name \"" + volumeName + "\"");
		
		CreateVolumeOptions options = CreateVolumeOptions.Builder
				.name(volumeName)
				.description(VOLUME_DESCRIPTION)
				.availabilityZone(availabilityZone);
		
		volume = volumeApi.get().create(size, options);
		
		try {
			waitForVolumeToReachStatus(Volume.Status.AVAILABLE, volumeApi, volume.getId(), endTime);
			volume = volumeApi.get().get(volume.getId());
			volumeDetails.setId(volume.getId());
			volumeDetails.setName(volume.getName());
			volumeDetails.setSize(volume.getSize());
			volumeDetails.setLocation(volume.getZone());
			logger.fine("Volume provisioned: " + volumeDetails.toString());
		} catch (final Exception e) {
			logger.log(Level.WARNING, "volume: " + volume.getId() + " failed to start up correctly. Shutting it down."
					+ " Error was: " + e.getMessage(), e);
			try {
				deleteVolume(region, volume.getId(), duration, timeUnit);
			} catch (final Exception e2) {
				logger.log(Level.WARNING, "Error while deleting volume: " + volume.getId() 
						+ ". Error was: " + e.getMessage() + ". It may be leaking.", e);
			}
			if (e instanceof TimeoutException) {
				throw (TimeoutException) e;
			} else {
				throw new StorageProvisioningException(e);				
			}
		}
		
		
		return volumeDetails;
	}

	@Override
	public void attachVolume(final String volumeId, final String device, final String machineIp, final long duration, 
			final TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException {
		
		
		final long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);
		NodeMetadata node = deployer.getServerWithIP(machineIp);
		if (node == null) {
			throw new StorageProvisioningException("Failed to attach volume " + volumeId + " to server. Server "
					+ "with ip: " + machineIp + " not found");
		}
		
		Optional<? extends VolumeAttachmentApi> volumeAttachmentApi = getAttachmentApi();
		Optional<? extends VolumeApi> volumeApi = getVolumeApi();
		
		if (!volumeApi.isPresent() || !volumeAttachmentApi.isPresent()) {
			throw new StorageProvisioningException("Failed to attach volume " + volumeId 
					+ ", Openstack API is not initialized.");
		}
		logger.info("Attaching volume on Openstack");
		
		volumeAttachmentApi.get().attachVolumeToServerAsDevice(volumeId, node.getProviderId(), device);
		
		try {
			waitForVolumeToReachStatus(Volume.Status.IN_USE, volumeApi, volumeId, endTime);
			logger.fine("Volume " + volumeId + " attached successfully to machine : " + machineIp);
		} catch (final Exception e) {
			logger.log(Level.WARNING, "volume: " + volumeId + " failed to attach to machine " + machineIp
					+ ". Error was: " + e.getMessage(), e);
			try {
				detachVolume(region, volumeId, duration, timeUnit);
			} catch (final Exception e2) {
				logger.log(Level.WARNING, "Error while detaching volume: " + volumeId 
						+ " after a failed attachment. Error was: " + e.getMessage() + ". It may be leaking.", e);
			}
			throw new StorageProvisioningException(e);
		}
		
	}

	@Override
	public void detachVolume(final String volumeId, final String machineIp, final long duration, 
			final TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException {
		
		final long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);
		NodeMetadata node = deployer.getServerWithIP(machineIp);
		if (node == null) {
			throw new StorageProvisioningException("Failed to detach volume " + volumeId + " from server " + machineIp
					+ ". Server not found.");
		}
		
		//TODO might be faster without the location at all
		Optional<? extends VolumeApi> volumeApi = getVolumeApi();
		Optional<? extends VolumeAttachmentApi> volumeAttachmentApi = getAttachmentApi();
		
		if (!volumeApi.isPresent() || !volumeAttachmentApi.isPresent()) {
			throw new StorageProvisioningException("Failed to detach volume " + volumeId 
					+ ", Openstack API is not initialized.");
		}
		
		volumeAttachmentApi.get().detachVolumeFromServer(volumeId, node.getProviderId());
		
		try {
			waitForVolumeToReachStatus(Volume.Status.AVAILABLE, volumeApi, volumeId, endTime);
			logger.fine("Volume " + volumeId + " detached successfully from machine : " + machineIp);
		} catch (final Exception e) {
			logger.log(Level.WARNING, "volume: " + volumeId + " failed to detach from machine " + machineIp
					+ ". Error was: " + e.getMessage(), e);
			throw new StorageProvisioningException(e);
		}

	}

	@Override
	public void deleteVolume(final String location, final String volumeId, final long duration, 
			final TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException {

		Optional<? extends VolumeApi> volumeApi = getVolumeApi();
		if (!volumeApi.isPresent()) {
			throw new StorageProvisioningException("Failed to delete volume " + volumeId + ", Openstack API is not "
					+ "initialized.");
		}
		
		if (!volumeApi.get().delete(volumeId)) {
			logger.log(Level.WARNING, "Error while deleting volume: " + volumeId + ".It may be leaking.");
		}
		
		// TODO: wait for state "Deleting"?

	}

	@Override
	public Set<VolumeDetails> listVolumes(final String machineIp, final long duration, final TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {
		
		Set<VolumeDetails> volumeDetailsSet = new HashSet<VolumeDetails>();
		
		NodeMetadata node = deployer.getServerWithIP(machineIp);
		if (node == null) {
			throw new StorageProvisioningException("Failed to list volumes attached to " + machineIp 
					+ ". Server not found");
		}
		
		Optional<? extends VolumeAttachmentApi> volumeAttachmentApi = getAttachmentApi();
		
		if (!volumeAttachmentApi.isPresent()) {
			throw new StorageProvisioningException("Failed to list volumes, Openstack API is not initialized.");
		}
		
		FluentIterable<? extends VolumeAttachment> volumesAttachmentsList = 
				volumeAttachmentApi.get().listAttachmentsOnServer(node.getProviderId());
		
		if (volumesAttachmentsList != null) {
			Volume volume;
			for (VolumeAttachment attachment : volumesAttachmentsList) {
				VolumeDetails details = new VolumeDetails();
				volume = getVolume(attachment.getVolumeId());
				details.setId(volume.getId());
				details.setName(volume.getName());
				details.setSize(volume.getSize());
				details.setLocation(volume.getZone());
				volumeDetailsSet.add(details);
			}
		}
		
		return volumeDetailsSet;
	}
	
	/**
	 * @return .
	 * @throws StorageProvisioningException .
	 */
	@Override
	public Set<VolumeDetails> listAllVolumes() throws StorageProvisioningException {
	
		Set<VolumeDetails> volumeDetailsSet = new HashSet<VolumeDetails>();
		Optional<? extends VolumeApi> volumeApi = getVolumeApi();
		if (!volumeApi.isPresent()) {
			throw new StorageProvisioningException("Failed to list all volumes.");
		}
		
		FluentIterable<? extends Volume> volumesList = volumeApi.get().list();
		if (volumesList != null) {
			for (Volume volume : volumesList) {
				VolumeDetails details = new VolumeDetails();
				details.setId(volume.getId());
				details.setName(volume.getName());
				details.setSize(volume.getSize());
				details.setLocation(volume.getZone());
				volumeDetailsSet.add(details);
			}
		}
		
		return volumeDetailsSet;
	}
	
	@Override
	public void terminateAllVolumes(final long duration, final TimeUnit timeUnit) throws TimeoutException, 
		StorageProvisioningException {
		
		Set<String> cloudifyVolumes = new HashSet<String>();
		Set<String> volumePrefixes = new HashSet<String>();
		
		Optional<? extends VolumeApi> volumeApi = getVolumeApi();
		if (!volumeApi.isPresent()) {
			throw new StorageProvisioningException("Failed to terminate volumes. Openstack API is not initialized.");
		}
		
		// get volume prefixes from all storage templates
		Collection<StorageTemplate> storageTemplates = this.cloud.getCloudStorage().getTemplates().values();
		for (StorageTemplate template : storageTemplates) {
			volumePrefixes.add(template.getNamePrefix());
		}
		
		// filter - keep only the Cloudify generated volumes
		FluentIterable<? extends Volume> volumesList = volumeApi.get().list();
		if (volumesList != null) {
			for (Volume volume : volumesList) {
				for (String volumePrefix: volumePrefixes) {
					if (volume.getName().startsWith(volumePrefix)) {
						cloudifyVolumes.add(volume.getId());
						break;
					}
				}
			}
		}
		
		// call to terminate all Cloudify volumes
		for (String volumeId: cloudifyVolumes) {
			if (!volumeApi.get().delete(volumeId)) {
				logger.log(Level.WARNING, "Error while deleting volume: " + volumeId + ".It may be leaking.");
			}
		}
		
		// verify volumes reach a "DELETING" status or not found (meaning they were probably deleted already)
		final long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);
		for (String volumeId: cloudifyVolumes) {
			waitForVolumeToBeDeleted(volumeApi, volumeId, endTime);
		}
		
	}
	
	
	private void waitForVolumeToReachStatus(final Volume.Status targetStatus, 
			final Optional<? extends VolumeApi> volumeApi, final String volumeId, final long endTime) 
					throws StorageProvisioningException, TimeoutException, InterruptedException {

		boolean statusReached = false;
		Volume.Status volumeStatus = null;
		
		if (!volumeApi.isPresent()) {
			throw new StorageProvisioningException("Failed to get volume status, Openstack API is not initialized.");
		}
		
		logger.fine("waiting for volume " + volumeId + " to reach status: " + targetStatus.toString());
		
		while (System.currentTimeMillis() < endTime) {
			final Volume volume = volumeApi.get().get(volumeId);
			if (volume != null) {
				volumeStatus = volume.getStatus();
				if (volumeStatus == targetStatus) {
					//volume has reach required status
					statusReached = true;
					break;
				} else if (volumeStatus == Volume.Status.ERROR) {
					throw new StorageProvisioningException("Storage volume provisioning encountered an error. "
							+ "Volume id: " + volumeId + " is in status ERROR");
				} else {
					logger.fine("Volume[" + volumeId + "] is in status " + volume.getStatus());
				}
			}

			Thread.sleep(VOLUME_POLLING_INTERVAL_MILLIS);
		}
		
		if (!statusReached) {
			throw new TimeoutException("timeout while waiting for volume to reach status \"" + targetStatus 
					+ "\". Current status is: " + volumeStatus);
		}
	}
	
	
	private void waitForVolumeToBeDeleted(final Optional<? extends VolumeApi> volumeApi, 
			final String volumeId, final long endTime) throws StorageProvisioningException, 
			TimeoutException {
		
		boolean statusReached = false;
		Volume.Status volumeStatus = null;
		
		logger.fine("waiting for volume " + volumeId + " to reach status: \"" + Volume.Status.DELETING + "\"");
		
		while (System.currentTimeMillis() < endTime) {
			final Volume volume = volumeApi.get().get(volumeId);
			if (volume == null) {
				// volume not found, considering it deleted
				statusReached = true;
				break;
			} else {
				volumeStatus = volume.getStatus();
				if (volumeStatus.equals(Volume.Status.DELETING)) {
					//volume has reach required status
					statusReached = true;
					break;
				} else if (volumeStatus == Volume.Status.ERROR) {
					throw new StorageProvisioningException("Volume termination failed, volume " + volumeId + " is "
							+ "in status ERROR");
				} else {
					logger.fine("Volume " + volumeId + " is in status: " + volume.getStatus());
				}
			}

			try {
				Thread.sleep(VOLUME_POLLING_INTERVAL_MILLIS);
			} catch (InterruptedException e) {
				// does it matter?
			}
			
		}
		
		if (!statusReached) {
			throw new TimeoutException("timed out while waiting for volume to reach status \"" 
					+ Volume.Status.DELETING + "\". Current status is: " + volumeStatus);	
		}
	}
	
	/**
	 * Publish a storage provisioning event occurred for the listeners registered on
	 * this class.
	 * 
	 * @param eventName
	 *            The name of the event (must be in the message bundle)
	 * @param args
	 *            Arguments that complement the event message
	 */
	protected void publishEvent(final String eventName, final Object... args) {
		for (final ProvisioningDriverListener listener : this.eventsListenersList) {
			listener.onProvisioningEvent(eventName, args);
		}
	}
	
	private void initDeployer() {
		
		if (deployer != null) {
			return;
		}
		
		try {
			logger.fine("Creating JClouds context deployer for Openstack with user: " + cloud.getUser().getUser());
						
			final Properties props = new Properties();
			// the existence of this property has been validated already by the compute driver
			String endpoint = (String) computeTemplate.getOverrides().get(OpenStackCloudifyDriver.OPENSTACK_ENDPOINT);
			props.put(API_VERSION_KEY, API_VERSION_VALUE);
			props.put(ENDPOINT_KEY, endpoint);

			deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(),
					cloud.getUser().getApiKey(), props, new HashSet<Module>());
		} catch (final Exception e) {
			publishEvent("connection_to_cloud_api_failed", cloud.getProvider().getProvider());
			throw new IllegalStateException("Failed to create cloud Deployer", e);
		}
	}

	@Override
	public void close() {
		if (novaContext != null) {
			novaContext.close();
		}
	}

	@Override
	public String getVolumeName(final String volumeId) throws StorageProvisioningException {
		Volume volume = getVolume(volumeId);
		if (volume == null) {
			throw new StorageProvisioningException("Failed to get volume with id: " + volumeId + ", volume not found");
		}
		
		return volume.getName();
	}
	
	/**
	 * Returns the volume by its id if exists or null otherwise.
	 * @param volumeId The of the requested volume
	 * @return The Volume matching the given id
	 * @throws StorageProvisioningException Indicates the storage APIs are not available
	 */
	private Volume getVolume(final String volumeId) throws StorageProvisioningException {
		Optional<? extends VolumeApi> volumeApi = getVolumeApi();
		if (!volumeApi.isPresent()) {
			throw new StorageProvisioningException("Failed to get volume by id " + volumeId + ", Openstack API is not "
					+ "initialized.");
		}
		
		final Volume volume = volumeApi.get().get(volumeId);
		
		return volume;
	}
	
	
	private Optional<? extends VolumeApi> getVolumeApi() {
		if (novaContext == null) {
			throw new IllegalStateException("Nova context is null");
		}
		
		return novaContext.getApi().getVolumeExtensionForZone(region);
	}
	
	private Optional<? extends VolumeAttachmentApi> getAttachmentApi() {
		if (novaContext == null) {
			throw new IllegalStateException("Nova context is null");
		}
		
		return novaContext.getApi().getVolumeAttachmentExtensionForZone(region);
	}

	
	private String getRegionFromHardwareId(final String hardwareId) {
		String region = "";
		if (hardwareId.indexOf("/") == -1) {
			logger.info("HardwareId is: " + hardwareId + ". It must be formatted "
					+ "as region / profile id");
			throw new IllegalArgumentException("HardwareId is: " + hardwareId + ". It must be formatted "
					+ "as region / profile id");
		}
		
		region = StringUtils.substringBefore(hardwareId, "/");
		if (StringUtils.isBlank(region)) {
			logger.info("HardwareId " + hardwareId + " is missing the region name. It must be formatted "
					+ "as region / profile id");
			throw new IllegalArgumentException("HardwareId is: " + hardwareId + ". It must be formatted "
					+ "as region / profile id");
		}
		
		logger.fine("region: " + region);		
		return region;
	}

	
}
