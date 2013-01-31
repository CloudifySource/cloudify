package org.cloudifysource.esc.driver.provisioning.storage;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.cloud.Cloud;

/**
 * This interface provides an entry point to provision block storage devices and attach them to specific
 * compute instances nodes created by the {@link ProvisioningDriver} .
 * 
 * This is still a work in progress. so there may be changes to the method signatures in the near future.
 * Also, snapshot functionality will be provided. 
 * @author elip
 * @since 2.5.0
 *
 */
public interface StorageProvisioningDriver {
	
	/**
	 * Called once at initialization. 
	 * Sets configuration members for later use. 
	 * use this method to extract all necessary information needed for future provisioning calls.
	 * @param cloud - The {@link Cloud} Object.
	 * @param computeTemplateName - the compute template 
	 * 								name used to provision the machine that this volume is dedicated to.
	 * @param storageTemplateName - the storage template name of the volume to be provisioned.
	 * @param serviceName - the name of the service that will use this volume.
	 */
	void setConfig(Cloud cloud, String computeTemplateName , String storageTemplateName, String serviceName);
	
	/**
	 * 
	 * @param duration .
	 * @param timeUnit .
	 * @return
	 * @throws TimeoutException
	 * @throws StorageProvisioningException
	 */
	VolumeDetails createVolume(long duration, TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException;
	
	/**
	 * 
	 * @param volumeId
	 * @param ip
	 * @param duration
	 * @param timeUnit
	 * @throws TimeoutException
	 * @throws StorageProvisioningException
	 */
	void attachVolume(String volumeId, String ip, long duration, TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException;
	
	/**
	 * 
	 * @param volumeId
	 * @param ip
	 * @param duration
	 * @param timeUnit
	 * @throws TimeoutException
	 * @throws StorageProvisioningException
	 */
	void detachVolume(String volumeId, String ip, long duration, TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException;
	
	/**
	 * 
	 * @param volumeId
	 * @param duration
	 * @param timeUnit
	 * @throws TimeoutException
	 * @throws StorageProvisioningException
	 */
	void deleteVolume(String volumeId, long duration, TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException;
	
	/**
	 * 
	 * @param ip
	 * @param duration
	 * @param timeUnit
	 * @return
	 * @throws TimeoutException
	 * @throws StorageProvisioningException
	 */
	Set<VolumeDetails> listVolumes(String ip, long duration, TimeUnit timeUnit) throws TimeoutException, StorageProvisioningException;

}
