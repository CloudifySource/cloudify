package org.cloudifysource.dsl.cloud;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


@CloudifyDSLEntity(name = "configuration", clazz = CloudConfiguration.class, allowInternalNode = true, allowRootNode = false, parent = "cloud")
public class CloudConfiguration {

	private String className = "com.gigaspaces.cloudify.esc.driver.provisioning.jclouds.DefaultCloudProvisioning";
	private String nicAddress;
	private String lookupGroups;
	private String lookupLocators;
	private String managementMachineTemplate;
	private boolean bootstrapManagementOnPublicIp = true;
	private boolean connectToPrivateIp = true;
	
	// Remote access credentials for a cloud machine.
	// These credentials will be used if the cloud does not provide machine specific details.
	// The password is only used if a key file is not supplied.
	private String remoteUsername;
	private String remotePassword;
	
	
	public String getNicAddress() {
		return nicAddress;
	}
	public void setNicAddress(String nicAddress) {
		this.nicAddress = nicAddress;
	}
	public String getLookupGroups() {
		return lookupGroups;
	}
	public void setLookupGroups(String lookupGroups) {
		this.lookupGroups = lookupGroups;
	}
	public String getLookupLocators() {
		return lookupLocators;
	}
	public void setLookupLocators(String lookupLocators) {
		this.lookupLocators = lookupLocators;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	public String getManagementMachineTemplate() {
		return managementMachineTemplate;
	}
	public void setManagementMachineTemplate(String managementMachineTemplate) {
		this.managementMachineTemplate = managementMachineTemplate;
	}

	public boolean isBootstrapManagementOnPublicIp() {
		return bootstrapManagementOnPublicIp;
	}
	public void setBootstrapManagementOnPublicIp(
			boolean bootstrapManagementOnPublicIp) {
		this.bootstrapManagementOnPublicIp = bootstrapManagementOnPublicIp;
	}
	public boolean isConnectToPrivateIp() {
		return connectToPrivateIp;
	}
	public void setConnectToPrivateIp(boolean connectToPrivateIp) {
		this.connectToPrivateIp = connectToPrivateIp;
	}
	public String getRemoteUsername() {
		return remoteUsername;
	}
	public void setRemoteUsername(String remoteUsername) {
		this.remoteUsername = remoteUsername;
	}
	public String getRemotePassword() {
		return remotePassword;
	}
	public void setRemotePassword(String remotePassword) {
		this.remotePassword = remotePassword;
	}
	@Override
	public String toString() {
		return "CloudConfiguration [className=" + className + ", nicAddress=" + nicAddress + ", lookupGroups="
				+ lookupGroups + ", lookupLocators=" + lookupLocators + ", managementMachineTemplate="
				+ managementMachineTemplate + ", bootstrapManagementOnPublicIp=" + bootstrapManagementOnPublicIp
				+ ", connectToPrivateIp=" + connectToPrivateIp + ", remoteUsername=" + remoteUsername
				+ ", remotePassword=***]";
	}

	
	
	
}
