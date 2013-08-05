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
package org.cloudifysource.esc.driver.provisioning.storage.aws;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.domain.cloud.storage.StorageTemplate;
import org.cloudifysource.esc.driver.provisioning.storage.BaseStorageDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningDriver;
import org.cloudifysource.esc.driver.provisioning.storage.StorageProvisioningException;
import org.cloudifysource.esc.driver.provisioning.storage.VolumeDetails;
import org.cloudifysource.esc.jclouds.JCloudsDeployer;
import org.cloudifysource.esc.util.JCloudsUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.ec2.compute.AWSEC2ComputeServiceContext;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.ec2.EC2ApiMetadata;
import org.jclouds.ec2.EC2AsyncClient;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.domain.Tag;
import org.jclouds.ec2.domain.Volume;
import org.jclouds.ec2.domain.Volume.Status;
import org.jclouds.ec2.features.TagApi;
import org.jclouds.ec2.options.DetachVolumeOptions;
import org.jclouds.ec2.services.ElasticBlockStoreClient;
import org.jclouds.ec2.util.TagFilterBuilder;
import org.jclouds.rest.RestContext;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

/*****
 * 
 * An implementation of elastic block Store storage driver. 
 * 
 * In-order to attach a storage volume to a specific machine, the storage volume and the machine
 * must be in the same availability zone and a device name for the specific machines kernel should be set.   
 * @see http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-attaching-volume.html
 * 
 * formatting and mounting of the volume will be done using the bootstrap-management script. 
 * @see http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ebs-using-volumes.html
 * 
 * @author adaml
 * @since 2.5.0
 */
public class EbsStorageDriver extends BaseStorageDriver implements StorageProvisioningDriver {
	
	protected static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(EbsStorageDriver.class.getName());
	
	private static final int MAX_VOLUME_SIZE = 1024;
	private static final int MIN_VOLUME_SIZE = 1;
	private static final String NAME_TAG_KEY = "Name";
	private static final int WAIT_FOR_STATUS_RETRY_INTERVAL_MILLIS = 3 * 1000; // Three seconds
	
	private Cloud cloud;
	private String region;
	private ComputeServiceContext context;
	private ElasticBlockStoreClient ebsClient;
	private TagApi tagApi;
	private ComputeTemplate computeTemplate;
    private JCloudsDeployer deployer;

	
	@Override
	public void setConfig(final Cloud cloud, final String computeTemplateName) {
		this.cloud = cloud;
		this.computeTemplate = cloud.getCloudCompute().getTemplates().get(computeTemplateName);
		initContext();
		initRegion();
		initEbsClient();
        try {
            initDeployer();
        } catch (IOException e) {
            throw new RuntimeException("Failed initializing JCloudsDeployer : " + e.getMessage());
        }
    }

    private void initDeployer() throws IOException {
        this.deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(),
                cloud.getUser().getApiKey(), new Properties());
    }

    private void initRegion() {
		String locationId = this.computeTemplate.getLocationId();
		RestContext<EC2Client, EC2AsyncClient> unwrapped = this.context.unwrap();
		try {
			EC2Client ec2ClientApi = unwrapped.getApi();
			this.region = JCloudsUtils.getEC2region(ec2ClientApi, locationId);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Unable to determine region according to location id: " + locationId);
			throw new IllegalStateException("Unable to determine region according to location id: " + locationId, e);
		}
	}

	@Override
	public VolumeDetails createVolume(final String templateName, final String availabilityZone, 
			final long duration, final TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {
		
		
		StorageTemplate storageTemplate = cloud.getCloudStorage().getTemplates().get(templateName);

		
		final long end = System.currentTimeMillis() + timeUnit.toMillis(duration);
		int size = storageTemplate.getSize();
		if (size < MIN_VOLUME_SIZE || size > MAX_VOLUME_SIZE) {
			throw new StorageProvisioningException("Volume size must be set to a value between " 
					+ MIN_VOLUME_SIZE + " and " + MAX_VOLUME_SIZE);
		}
		Volume volume = null;
		try {
			logger.fine("Creating new volume in availability zone " + availabilityZone + " of size " + size
                    + " with prefix " + cloud.getCloudStorage().getTemplates().get(templateName).getNamePrefix());
			volume = this.ebsClient.createVolumeInAvailabilityZone(availabilityZone, size);

			String volumeId = volume.getId();
			logger.fine("Waiting for volume to become available.");
			waitForVolumeToReachStatus(Status.AVAILABLE, end, volumeId);
			
			logger.fine("Naming created volume with id " + volumeId);
			TagApi tagApi = getTagsApi();
			Map<String, String> tagsMap = createTagsMap(templateName);
			tagApi.applyToResources(tagsMap, Arrays.asList(volumeId));
			logger.fine("Volume created successfully. volume id is: " + volumeId);
		} catch (final Exception e) {
			if (volume != null) {
				handleExceptionAfterVolumeCreated(volume.getId());
			}
			if (e instanceof TimeoutException) {
				throw (TimeoutException) e;
			} else {
				throw new StorageProvisioningException("Failed creating volume of size " 
							+ size + " in availability zone. " 
						+ availabilityZone + "Reason: " + e.getMessage(), e);				
			}
		}
		return createVolumeDetails(volume);
	}

	private void handleExceptionAfterVolumeCreated(final String volumeId) {
		try {
			deleteVolume(volumeId);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Volume Provisioning failed. "
							+ "An error was encountered while trying to delete the new volume ( "
							+ volumeId + "). Error was: " + e.getMessage(), e);
		}
		
	}

	@Override
	public void attachVolume(final String volumeId, final String device, final String ip, final long duration,
			final TimeUnit timeUnit) throws TimeoutException,
			StorageProvisioningException {

		final long end = System.currentTimeMillis() + timeUnit.toMillis(duration);
		NodeMetadata nodeMetadata = deployer.getServerWithIP(ip);
		try {
			String instanceId = nodeMetadata.getProviderId();
			logger.log(Level.FINE, "Attaching volume with id " + volumeId 
					+ " to machine with id " + instanceId);
			this.ebsClient.attachVolumeInRegion(this.region, 
					volumeId, instanceId, device);
		} catch (final Exception e) {
			throw new StorageProvisioningException("Failed attaching volume to machine. Reason: " + e.getMessage(), e);
		}
		waitForVolumeToReachStatus(Status.IN_USE, end, volumeId);
	}
	
	@Override
	public void detachVolume(final String volumeId, final String ip, final long duration,
			final TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException {
		
		final long end = System.currentTimeMillis() + timeUnit.toMillis(duration);
		NodeMetadata nodeMetadata = deployer.getServerWithIP(ip);

        try {
            logger.fine("Detaching volume with id " + volumeId + " from machine with id "
                                + nodeMetadata.getId());
            this.ebsClient.detachVolumeInRegion(this.region, volumeId, false,
                    DetachVolumeOptions.Builder.fromInstance(nodeMetadata.getProviderId()));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed detaching node with id " + volumeId
                    + " Reason: " + e.getMessage(), e);
            throw new StorageProvisioningException("Failed detaching node with id " + volumeId
                    + " Reason: " + e.getMessage(), e);
        }
        try {
            waitForVolumeToReachStatus(Status.AVAILABLE, end, volumeId);
        } catch (final TimeoutException e) {
            logger.warning("Timed out while waiting for volume[" + volumeId + "] to "
                    + "become available after detachment. this may cause this volume to leak");
            throw e;
        }
	}

	@Override
	public void deleteVolume(final String location, final String volumeId, final long duration, 
			final TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException {
		final long end = System.currentTimeMillis() + timeUnit.toMillis(duration);
		deleteVolume(volumeId);
		try {
			// according to the documentation, the volume should stay
			// in 'deleting' status for a few minutes. 
			waitForVolumeToReachStatus(Status.DELETING, end, volumeId);
		} catch (final StorageProvisioningException e) {
			// Volume was not found. Do nothing.
		}
		logger.fine("Volume with id " + volumeId + " deleted successfully");
	}

	private void deleteVolume(final String volumeId)
			throws StorageProvisioningException {
		try {
			logger.fine("Deleting volume with id " + volumeId);
			this.ebsClient.deleteVolumeInRegion(this.region, volumeId);
		} catch (final Exception e) {
			logger.log(Level.WARNING, "Failed deleting volume with ID " + volumeId 
					+ " Reason: " + e.getMessage());
			throw new StorageProvisioningException("Failed deleting volume with ID " + volumeId 
					+ " Reason: " + e.getMessage(), e);
		}
	}
	
	@Override
	public Set<VolumeDetails> listVolumes(final String ip, final long duration,
			final TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException {
		
		Set<VolumeDetails> volumeDetailsSet = new HashSet<VolumeDetails>();
		Set<String> machineVolumeIds = getMachineVolumeIds(ip);
		logger.fine("Listing all volumes on machine with ip " + ip);
		Set<VolumeDetails> allVolumes = listAllVolumes();
		
		for (VolumeDetails volume : allVolumes) {
			String volumeId = volume.getId();
			if (machineVolumeIds.contains(volumeId)) {
				volumeDetailsSet.add(volume);
			}
		}
		return volumeDetailsSet;
	}

	@Override
	public Set<VolumeDetails> listAllVolumes()
			throws StorageProvisioningException {
		Set<VolumeDetails> volumeDetails = new HashSet<VolumeDetails>();
		try {
			Set<Volume> allVolumes = this.ebsClient.describeVolumesInRegion(this.region, (String[]) null);
			for (Volume volume : allVolumes) {
				volumeDetails.add(createVolumeDetails(volume));
			}

		} catch (Exception e) {
			throw new StorageProvisioningException("Failed listing volumes. Reason: " + e.getMessage(), e);
		}
		return volumeDetails;
	}
	
	@Override
	public String getVolumeName(final String volumeId) throws StorageProvisioningException {
		String volumeNameTag = "";
		try {
			TagApi tagApi = getTagsApi();
			logger.fine("Filtering tags using volumeId " + volumeId + " to find the 'Name' tag");
			FluentIterable<Tag> filter = tagApi.filter(new TagFilterBuilder().resourceId(volumeId).build());
			ImmutableList<Tag> immutableList = filter.toImmutableList();
			for (Tag tag : immutableList) {
				if (tag.getKey().equals(NAME_TAG_KEY)) {
					volumeNameTag = tag.getValue().get();
					break;
				}
			}
			return volumeNameTag;
		} catch (Exception e) {
			throw new StorageProvisioningException("Failed getting volume name. Reason: " + e.getMessage(), e);
		}
	}
	
	@Override
	public void setComputeContext(final Object computeContext)
			throws StorageProvisioningException {
		if (computeContext != null && !(computeContext instanceof AWSEC2ComputeServiceContext)) {
			throw new StorageProvisioningException("JClouds context does not match storage driver. "
					+ "expecting context of type: " + AWSEC2ComputeServiceContext.class.getName());
		}
		logger.fine("Setting compute context for storage driver");
		this.context = (AWSEC2ComputeServiceContext) computeContext;
	}

	private TagApi getTagsApi() {
		if (this.tagApi == null) {
			this.tagApi = EC2Client.class.cast(this.context.unwrap(EC2ApiMetadata.CONTEXT_TOKEN)
					.getApi()).getTagApiForRegion(this.region).get();
		} 
		return this.tagApi;
	}
	
	@Override
	public void close() {
		if (this.context != null) {
			context.close();
		}
	}
	
	private VolumeDetails createVolumeDetails(final Volume volume) {
		String availabilityZone = volume.getAvailabilityZone();
		String id = volume.getId();
		int size = volume.getSize();
		String volumeName = ""; 
		try {
			volumeName = getVolumeName(id);
		} catch (StorageProvisioningException e) {
			// Native volumes do not have a name only id.
			logger.info("Could not obtain volume name for node with id: " + id + ". Reason: " + e.getMessage());
		}
		
		VolumeDetails volumeDetails = new VolumeDetails();
		volumeDetails.setLocation(availabilityZone);
		volumeDetails.setId(id);
		volumeDetails.setSize(size);
		volumeDetails.setName(volumeName); 
		return volumeDetails;
	}

	private void initEbsClient() {
		try {
			ElasticBlockStoreClient ebsClient = EC2Client.class.cast(this.context.unwrap(EC2ApiMetadata.CONTEXT_TOKEN)
					.getApi()).getElasticBlockStoreServices();
			this.ebsClient = ebsClient;
		} catch (Exception e) {
			throw new IllegalStateException("Failed creating ebs client. Reason: " + e.getMessage(), e);
		}
	}
	
	private void initContext() {
		
		if (this.context != null) {
			return;
		}
		String userName = cloud.getUser().getUser();
		String apiKey = cloud.getUser().getApiKey();
		String cloudProvider = cloud.getProvider().getProvider();
		try {
			logger.fine("Creating compute context with user: " + userName);
			ContextBuilder contextBuilder = ContextBuilder.newBuilder(cloudProvider);
			contextBuilder.credentials(userName, apiKey);
			this.context = contextBuilder.buildView(ComputeServiceContext.class);
		} catch (Exception e) {
			throw new IllegalStateException("Failed creating cloud native context. Reason: " + e.getMessage(), e);
		}
	}
	
	private Map<String, String> createTagsMap(final String templateName) {
		HashMap<String, String> tagsMap = new HashMap<String, String>();
		String volumeName = this.cloud.getCloudStorage().getTemplates().get(templateName).getNamePrefix() + "_" 
								+ System.currentTimeMillis();
		tagsMap.put(NAME_TAG_KEY, volumeName);
		return tagsMap;
	}

	@Override
	public Set<String> getMachineVolumeIds(final String ip) 
					throws StorageProvisioningException {
		
		NodeMetadata nodeMetadata = deployer.getServerWithIP(ip);
		Hardware nodeHardware = nodeMetadata.getHardware();
		List<? extends org.jclouds.compute.domain.Volume> machineVolumes = nodeHardware.getVolumes();
		Set<String> machineVolumeIds = new HashSet<String>();
		for (org.jclouds.compute.domain.Volume machineVolume : machineVolumes) {
			String id = machineVolume.getId();
			// Some storage devices that start with the machine have no id.
			// These devices are certainly not ebs volumes.
			if (!StringUtils.isEmpty(id)) {
				machineVolumeIds.add(machineVolume.getId());
			}
		}
		return machineVolumeIds;
	}
	
	private void waitForVolumeToReachStatus(final Status status, final long end , final String volumeId) 
			throws TimeoutException, StorageProvisioningException {

        logger.fine("Waiting for volume '" + volumeId + "' to reach status " + status);

		Set<Volume> volumes;
		while (System.currentTimeMillis() < end) {
			try {
				volumes = this.ebsClient.describeVolumesInRegion(this.region, volumeId);
				Thread.sleep(WAIT_FOR_STATUS_RETRY_INTERVAL_MILLIS);
			} catch (Exception e) {
				throw new StorageProvisioningException("Failed getting volume description."
						+ " Reason: " + e.getMessage(), e);
			}
			Volume volume = volumes.iterator().next();
			Status volumeStatus = volume.getStatus();
			if (volumeStatus.equals(status)) {
				return;
			} else {
                logger.fine("Volume[" + volumeId + "] is in status " + volume.getStatus());
            }
		}
		throw new TimeoutException("Timed out waiting for storage status to become " + status.toString());
	}
}
