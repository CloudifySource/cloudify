package com.gigaspaces.cloudify.dsl;

import java.util.logging.Level;
import java.util.List;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name="cloud", clazz=Cloud.class, allowInternalNode = false, allowRootNode = true)
public class Cloud {

	private String user;
	private String apiKey;
	

	private String provider;
	private String localDirectory;
	private String remoteDirectory;
	
	private String imageId;
	private long machineMemoryMB;
	private String hardwareId;
	private String locationId;
	
	private String cloudifyUrl;
	private String machineNamePrefix;

	private boolean dedicatedManagementMachines;
	private List<String> managementOnlyFiles;
	private boolean connectedToPrivateIp;

	private int numberOfManagementMachines;

	// this is the group that jclouds uses when starting management machines
	private String managementGroup;
	
	private String securityGroup;
	
	private String keyFile;	
	
	// this is the name aws uses for the name of the (public/private) keys pair
	private String keyPair;
	
	private Level sshLoggingLevel;
	
	private List<String> zones;
	
	private long reservedMemoryCapacityPerMachineInMB;

	public int getNumberOfManagementMachines() {
		return numberOfManagementMachines;
	}

	public void setNumberOfManagementMachines(int numberOfManagementMachines) {
		this.numberOfManagementMachines = numberOfManagementMachines;
	}

	public String getManagementGroup() {
		return managementGroup;
	}

	public void setManagementGroup(String managementGroup) {
		this.managementGroup = managementGroup;
	}


	public Level getSshLoggingLevel() {
		return sshLoggingLevel;
	}

	public void setSshLoggingLevel(Level sshLoggingLevel) {
		this.sshLoggingLevel = sshLoggingLevel;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getLocalDirectory() {
		return localDirectory;
	}

	public void setLocalDirectory(String localDirectory) {
		this.localDirectory = localDirectory;
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public long getMachineMemoryMB() {
		return machineMemoryMB;
	}

	public void setMachineMemoryMB(long machineMemoryMB) {
		this.machineMemoryMB = machineMemoryMB;
	}

	public String getHardwareId() {
		return hardwareId;
	}

	public void setHardwareId(String hardwareId) {
		this.hardwareId = hardwareId;
	}

	public String getKeyFile() {
		return keyFile;
	}

	public void setKeyFile(String keyFile) {
		this.keyFile = keyFile;
	}

	public String getCloudifyUrl() {
		return cloudifyUrl;
	}

	public void setCloudifyUrl(String cloudifyUrl) {
		this.cloudifyUrl = cloudifyUrl;
	}

	public String getMachineNamePrefix() {
		return machineNamePrefix;
	}

	public void setMachineNamePrefix(String machineNamePrefix) {
		this.machineNamePrefix = machineNamePrefix;
	}

	public boolean isDedicatedManagementMachines() {
		return dedicatedManagementMachines;
	}

	public void setDedicatedManagementMachines(boolean dedicatedManagementMachines) {
		this.dedicatedManagementMachines = dedicatedManagementMachines;
	}

	public List<String> getManagementOnlyFiles() {
		return managementOnlyFiles;
	}

	public void setManagementOnlyFiles(List<String> managementOnlyFiles) {
		this.managementOnlyFiles = managementOnlyFiles;
	}

	public boolean isConnectedToPrivateIp() {
		return connectedToPrivateIp;
	}

	public void setConnectedToPrivateIp(boolean connectedToPrivateIp) {
		this.connectedToPrivateIp = connectedToPrivateIp;
	}

	public void setKeyPair(String keyPair) {
		this.keyPair = keyPair;
	}

	public String getKeyPair() {
		return keyPair;
	}

	public void setSecurityGroup(String securityGroup) {
		this.securityGroup = securityGroup;
	}

	public String getSecurityGroup() {
		return securityGroup;
	}

	public void setZones(List<String> zones) {
		this.zones = zones;
	}

	public List<String> getZones() {
		return zones;
	}

	public void setReservedMemoryCapacityPerMachineInMB(
			long reservedMemoryCapacityPerMachineInMB) {
		this.reservedMemoryCapacityPerMachineInMB = reservedMemoryCapacityPerMachineInMB;
	}

	public long getReservedMemoryCapacityPerMachineInMB() {
		return reservedMemoryCapacityPerMachineInMB;
	}

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationId() {
        return locationId;
    }

}
