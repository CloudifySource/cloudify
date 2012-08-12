/**
 * 
 */
package org.cloudifysource.esc.driver.provisioning.azure.client;

import org.cloudifysource.esc.driver.provisioning.azure.model.InputEndpoints;

/************************************************************************************************
 * 																							    *
 * A POJO holding all necessary properties for create a new vm to an existing virtual network.  *
 * 																								*
 * @author elip																					*
 *																								*
 ************************************************************************************************/

public class CreatePersistentVMRoleDeploymentDescriptor {
		
	private static final int ROLE_NAME_UID_LENGTH = 4;
	
	private String deploymentName;
	private String deploymentSlot;
	private String imageName;
	private String storageAccountName;
	private String userName;
	private String password;
	private String size;
	private String networkName;
	private String availabilitySetName;
	private String roleName;
	private String affinityGroup;
	private String hostedServiceName;
	
	public String getHostedServiceName() {
		return hostedServiceName;
	}

	public void setHostedServiceName(final String hostedServiceName) {
		this.hostedServiceName = hostedServiceName;
	}

	public String getAffinityGroup() {
		return affinityGroup;
	}

	public void setAffinityGroup(final String affinityGroup) {
		this.affinityGroup = affinityGroup;
	}

	public void setRoleName(final String roleName) {
		this.roleName = roleName + UUIDHelper.generateRandomUUID(ROLE_NAME_UID_LENGTH);
	}
	
	public String getDeploymentName() {
		return deploymentName;
	}

	public void setDeploymentName(final String deploymentName) {
		this.deploymentName = deploymentName;
	}
	
	public String getAvailabilitySetName() {
		return availabilitySetName;
	}

	public void setAvailabilitySetName(final String availabilitySetName) {
		this.availabilitySetName = availabilitySetName;
	}

	public String getNetworkName() {
		return networkName;
	}

	public void setNetworkName(final String networkName) {
		this.networkName = networkName;
	}

	private InputEndpoints inputEndpoints;
	
	
	public String getRoleName() {
		return roleName;
	}
	
	public String getDeploymentSlot() {
		return deploymentSlot;
	}
	
	public void setDeploymentSlot(final String deploymentSlot) {
		this.deploymentSlot = deploymentSlot;
	}
	
	public String getImageName() {
		return imageName;
	}
	
	public void setImageName(final String imageName) {
		this.imageName = imageName;
	}
	
	public String getStorageAccountName() {
		return storageAccountName;
	}
	
	public void setStorageAccountName(final String storageAccountName) {
		this.storageAccountName = storageAccountName;
	}
	
	public String getUserName() {
		return userName;
	}
	
	public void setUserName(final String userName) {
		this.userName = userName;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(final String password) {
		this.password = password;
	}
	
	public String getSize() {
		return size;
	}
	
	public void setSize(final String size) {
		this.size = size;
	}
	
	public InputEndpoints getInputEndpoints() {
		return inputEndpoints;
	}
	
	public void setInputEndpoints(final InputEndpoints inputEndpoints) {
		this.inputEndpoints = inputEndpoints;
	}

	@Override
	public String toString() {
		return "CreatePersistentVMRoleDeploymentDescriptor [deploymentName="
				+ deploymentName + ", deploymentSlot=" + deploymentSlot
				+ ", imageName=" + imageName + ", storageAccountName="
				+ storageAccountName + ", size=" + size + ", networkName="
				+ networkName + ", availabilitySetName=" + availabilitySetName
				+ ", roleName=" + roleName + ", affinityGroup=" + affinityGroup
				+ ", hostedServiceName=" + hostedServiceName
				+ ", inputEndpoints=" + inputEndpoints + "]";
	}
	
	
}
