package com.gigaspaces.cloudify.dsl;

import java.util.List;
import java.util.logging.Level;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "provider", clazz = CloudProvider.class, allowInternalNode = true, allowRootNode = false, parent = "cloud2")
public class CloudProvider {

	private String provider;
	private String localDirectory;
	private String remoteDirectory;
	private String cloudifyUrl;
	private String machineNamePrefix;

	private String securityGroup;

	private boolean dedicatedManagementMachines = true;

	private List<String> managementOnlyFiles;

	private boolean connectedToPrivateIp;

	private Level sshLoggingLevel = Level.INFO;

	private List<String> zones;

	private String managementGroup;
	private int numberOfManagementMachines;
	private int reservedMemoryCapacityPerMachineInMB;
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
	public String getSecurityGroup() {
		return securityGroup;
	}
	public void setSecurityGroup(String securityGroup) {
		this.securityGroup = securityGroup;
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
	public Level getSshLoggingLevel() {
		return sshLoggingLevel;
	}
	public void setSshLoggingLevel(Level sshLoggingLevel) {
		this.sshLoggingLevel = sshLoggingLevel;
	}
	public List<String> getZones() {
		return zones;
	}
	public void setZones(List<String> zones) {
		this.zones = zones;
	}
	public String getManagementGroup() {
		return managementGroup;
	}
	public void setManagementGroup(String managementGroup) {
		this.managementGroup = managementGroup;
	}
	public int getNumberOfManagementMachines() {
		return numberOfManagementMachines;
	}
	public void setNumberOfManagementMachines(int numberOfManagementMachines) {
		this.numberOfManagementMachines = numberOfManagementMachines;
	}
	public int getReservedMemoryCapacityPerMachineInMB() {
		return reservedMemoryCapacityPerMachineInMB;
	}
	public void setReservedMemoryCapacityPerMachineInMB(
			int reservedMemoryCapacityPerMachineInMB) {
		this.reservedMemoryCapacityPerMachineInMB = reservedMemoryCapacityPerMachineInMB;
	}
	
	
	
}
