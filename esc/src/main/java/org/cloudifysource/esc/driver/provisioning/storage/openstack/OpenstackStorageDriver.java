package org.cloudifysource.esc.driver.provisioning.storage.openstack;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.storage.StorageTemplate;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriverListener;
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

/**
 * Storage Provisioning implementation on Openstack.
 * @author noak
 * @since 2.5.0
 */
public class OpenstackStorageDriver implements StorageProvisioningDriver  {

	private static final int VOLUME_POLLING_INTERVAL_MILLIS = 10 * 1000; // 10 seconds
	private static final String VOLUME_DESCRIPTION = "Cloudify generated volume";
	//private static final String ENDPOINT = "jclouds.endpoint";
	//private static final String AVAILABILITY_ZONE = "nova";
	protected static final String EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API = "try_to_connect_to_cloud_api";
	protected static final String EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API = "connection_to_cloud_api_succeeded";
	protected static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(OpenstackStorageDriver.class.getName());
	
	//private String zone;
	private StorageTemplate storageTemplate;
	private ComputeServiceContext computeContext;
	private Cloud cloud;
	//private String zone;
	private JCloudsDeployer deployer;
	private Optional<? extends VolumeApi> volumeApi;
	private Optional<? extends VolumeAttachmentApi> volumeAttachmentApi;
	private RestContext<NovaApi, NovaAsyncApi> novaContext;
	
	protected final List<ProvisioningDriverListener> eventsListenersList = new LinkedList<ProvisioningDriverListener>();

	
	@Override
	public void setComputeContext(final Object computeContext) {
		if (computeContext != null) {
			if (computeContext instanceof ComputeServiceContext) {
				this.computeContext = (ComputeServiceContext) computeContext;
			} else {
				throw new IllegalArgumentException("ComputeContext object is not instance of " 
						+ ComputeServiceContext.class.getName());
			}
		}
	}
	
	@Override
	public void setConfig(final Cloud cloud, final String computeTemplateName, final String storageTemplateName,
			final Object computeContextObj) {

		logger.fine("Initializing storage provisioning on Openstack. Using template: " + storageTemplateName);
		
		setComputeContext(computeContextObj);
		
		this.cloud = cloud;
		publishEvent(EVENT_ATTEMPT_CONNECTION_TO_CLOUD_API, cloud.getProvider().getProvider());
		initDeployer(cloud, storageTemplateName);
		publishEvent(EVENT_ACCOMPLISHED_CONNECTION_TO_CLOUD_API, cloud.getProvider().getProvider());
		novaContext = this.computeContext.unwrap();
		storageTemplate = cloud.getCloudStorage().getTemplates().get(storageTemplateName);
		//zone = storageTemplate.getLocation();
	}

	@Override
	public VolumeDetails createVolume(final String location, final long duration, final TimeUnit timeUnit) throws 
		TimeoutException, StorageProvisioningException {
		
		final long endTime = System.currentTimeMillis() + timeUnit.toMillis(duration);
		final VolumeDetails volumeDetails;
		final Volume volume;

		volumeApi = novaContext.getApi().getVolumeExtensionForZone(location);

		if (!volumeApi.isPresent()) {
			// TODO do what? throw exception?
		}
		
		if (computeContext == null) {
			//TODO
			//throw exception
		}
		
		String volumeName = storageTemplate.getNamePrefix() + System.currentTimeMillis();
		CreateVolumeOptions options = CreateVolumeOptions.Builder
				.name(volumeName)
				.description(VOLUME_DESCRIPTION)
				.availabilityZone(storageTemplate.getOptions().get(StorageTemplate.OPENSTACK_VOLUME_ZONE).toString());

		volume = volumeApi.get().create(1, options);
		
		try {
			volumeDetails = waitForVolume(volume.getId(), endTime);
		} catch (final Exception e) {
			logger.log(Level.WARNING, "volume: " + volume.getId() + " failed to start up correctly. Shutting it down."
					+ " Error was: " + e.getMessage(), e);
			try {
				deleteVolume(volume.getId(), duration, timeUnit);
			} catch (final Exception e2) {
				logger.log(Level.WARNING, "Error while deleting volume: " + volume.getId() 
						+ ". Error was: " + e.getMessage() + ".It may be leaking.", e);
			}
			throw new StorageProvisioningException(e);
		}
		
		return volumeDetails;
	}

	@Override
	public void attachVolume(final String volumeId, final String machineIp, final long duration, 
			final TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException {
		
		Optional<? extends VolumeAttachmentApi> volumeAttachmentApi;
		NodeMetadata node = deployer.getServerWithIP(machineIp);
		if (node == null) {
			throw new StorageProvisioningException("Failed to attach volume " + volumeId + " to server. Server "
					+ "with ip: " + machineIp + " not found");
		}
		
		volumeAttachmentApi = novaContext.getApi().
				getVolumeAttachmentExtensionForZone(node.getLocation().getParent().getId());
		if (volumeAttachmentApi.isPresent()) {
			volumeAttachmentApi.get().attachVolumeToServerAsDevice(volumeId, node.getId(), 
					storageTemplate.getOptions().get(StorageTemplate.OPTION_ATTACHMENT_DEVICE_PATH).toString());
		} else {
			// TODO what?
		}
	}

	@Override
	public void detachVolume(final String volumeId, final String machineIp, final long duration, 
			final TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException {
		NodeMetadata node = deployer.getServerWithIP(machineIp);
		if (node == null) {
			throw new StorageProvisioningException("Failed to detach volume " + volumeId + " from server " + machineIp
					+ ". Server not found.");
		}
		if (volumeAttachmentApi.isPresent()) {
			volumeAttachmentApi.get().detachVolumeFromServer(volumeId, machineIp);
		} else {
			// TODO what?
		}

	}

	@Override
	public void deleteVolume(final String volumeId, final long duration, final TimeUnit timeUnit) 
			throws TimeoutException, StorageProvisioningException {
		
		if (!volumeApi.isPresent()) {
			//TODO : is this OK? or should we wait?
			throw new IllegalArgumentException("invalid volume id " + volumeId);
		}
		
		if (!volumeApi.get().delete(volumeId)) {
			logger.log(Level.WARNING, "Error while deleting volume: " + volumeId + ".It may be leaking.");
		}

	}

	@Override
	public Set<VolumeDetails> listVolumes(final String machineIp, final long duration, final TimeUnit timeUnit)
			throws TimeoutException, StorageProvisioningException {
		
		Set<VolumeDetails> volumeDetailsSet = new HashSet<VolumeDetails>();
		
		if (!volumeApi.isPresent() || !volumeAttachmentApi.isPresent()) {
			//TODO : is this OK? or should we wait?
			throw new StorageProvisioningException("Failed to list volumes, APIs not available.");
		}
		
		NodeMetadata node = deployer.getServerWithIP(machineIp);
		if (node == null) {
			throw new StorageProvisioningException("Failed to list volumes attached to " + machineIp 
					+ ". Server not found");
		}
		
		FluentIterable<? extends VolumeAttachment> volumesAttachmentsList = 
				volumeAttachmentApi.get().listAttachmentsOnServer(node.getId());
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
	 * 
	 * @param duration .
	 * @param timeUnit .
	 * @return .
	 * @throws TimeoutException .
	 * @throws StorageProvisioningException .
	 */
	public Set<VolumeDetails> listAllVolumes(final long duration, final TimeUnit timeUnit) throws TimeoutException, 
		StorageProvisioningException {
		
		if (!volumeApi.isPresent()) {
			//TODO : is this OK? or should we wait?
			throw new StorageProvisioningException("Failed to list all volumes.");
		}
		
		Set<VolumeDetails> volumeDetailsSet = new HashSet<VolumeDetails>();
		
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
	
	private VolumeDetails waitForVolume(final String volumeId, final long endTime) throws StorageProvisioningException,
		TimeoutException, InterruptedException {
		
		VolumeDetails volumeDetails = new VolumeDetails();
		
		if (!volumeApi.isPresent()) {
			//TODO : is this OK? or should we wait?
			throw new StorageProvisioningException("Failed to provision storage volume on Openstack. ");
		}
		
		while (true) {
			final Volume volume = volumeApi.get().get(volumeId);
			if (volume != null) {
				Volume.Status volumeStatus = volume.getStatus();
				if (volumeStatus == Volume.Status.AVAILABLE) {
					//volume is available for use
					volumeDetails.setId(volume.getId());
					volumeDetails.setName(volume.getName());
					volumeDetails.setSize(volume.getSize());
					volumeDetails.setLocation(volume.getZone());
					logger.fine("Volume provisioned: " + volumeDetails.toString());
					break;
				} else if (volumeStatus == Volume.Status.ERROR) {
					throw new StorageProvisioningException("Failed to create storage volume on Openstack. "
							+ "Volume id: " + volumeId);
				} else if (volumeStatus == Volume.Status.DELETING
					|| volumeStatus == Volume.Status.IN_USE
					|| volumeStatus == Volume.Status.UNRECOGNIZED) {
					throw new StorageProvisioningException("Failed to create storage volume on Openstack, "
							+ "unexpected volume status reported: " + volumeStatus + ". Volume id " + volumeId);
				}

				if (System.currentTimeMillis() > endTime) {
					throw new TimeoutException("timeout creating volume. status:" + volume.getStatus());
				}
			}

			Thread.sleep(VOLUME_POLLING_INTERVAL_MILLIS);
		}
		
		return volumeDetails;
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
	
	private JCloudsDeployer initDeployer(final Cloud cloud, final String storageTemplateName) {
		
		if (deployer != null) {
			return deployer;
		}
		
		try {
			logger.fine("Creating JClouds context deployer for Openstack with user: " + cloud.getUser().getUser());
			final StorageTemplate storageTemplate = cloud.getCloudStorage().getTemplates().get(storageTemplateName);
			final Properties props = new Properties();
			props.putAll(storageTemplate.getOptions());			
			deployer = new JCloudsDeployer(cloud.getProvider().getProvider(), cloud.getUser().getUser(),
					cloud.getUser().getApiKey(), props);
		} catch (final Exception e) {
			publishEvent("connection_to_cloud_api_failed", cloud.getProvider().getProvider());
			throw new IllegalStateException("Failed to create cloud Deployer", e);
		}
		
		return deployer;
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
		if (!volumeApi.isPresent()) {
			//TODO : is this OK? or should we wait?
			throw new StorageProvisioningException("Failed to get volume by id " + volumeId + ", APIs not "
					+ "available.");
		}
		
		final Volume volume = volumeApi.get().get(volumeId);
		
		return volume;
	}

}