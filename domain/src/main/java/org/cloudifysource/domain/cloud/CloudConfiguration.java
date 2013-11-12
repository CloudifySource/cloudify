/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.domain.cloud;

import org.cloudifysource.domain.internal.CloudifyDSLEntity;

/***********
 * Domain POJO for the cloud configuration.
 *
 * @author barakme, adaml
 * @since 2.0.0
 *
 */
@CloudifyDSLEntity(name = "configuration", clazz = CloudConfiguration.class, allowInternalNode = true,
		allowRootNode = false, parent = "cloud")
public class CloudConfiguration {

    //admin object loading time (in seconds), defaults to 60 seconds.
	private short adminLoadingTimeInSeconds = 60;
	
	/*******
	 * The default cloud driver name.
	 */
	public static final String DEFAULT_CLOUD_DRIVER_CLASS_NAME =
			"org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver";
	private String className = DEFAULT_CLOUD_DRIVER_CLASS_NAME;
	private String storageClassName;
	private String networkDriverClassName;
	private String nicAddress;
	private String lookupGroups;
	private String lookupLocators;
	private String managementMachineTemplate;
	private String managementStorageTemplate;


	private boolean bootstrapManagementOnPublicIp = true;
	private boolean connectToPrivateIp = true;
	private GridComponents components = new GridComponents();
	private String persistentStoragePath;

	public String getNicAddress() {
		return nicAddress;
	}

	public void setNicAddress(final String nicAddress) {
		this.nicAddress = nicAddress;
	}

	public String getLookupGroups() {
		return lookupGroups;
	}

	public void setLookupGroups(final String lookupGroups) {
		this.lookupGroups = lookupGroups;
	}

	public String getLookupLocators() {
		return lookupLocators;
	}

	public void setLookupLocators(final String lookupLocators) {
		this.lookupLocators = lookupLocators;
	}

	/*********
	 * Full name of the class that implements the cloud driver for this cloud configuration. Class must implement the
	 * <i>org.cloudifysource.esc.driver.provisioning.ProvisioningDriver</i> interface.
	 *
	 * Defaults to the value of the DEFAULT_CLOUD_DRIVER_CLASS_NAME constant.
	 *
	 * @see org.cloudifysource.esc.driver.provisioning.ProvisioningDriver
	 * @return the cloud driver class name.
	 */
	public String getClassName() {
		return className;
	}

	public void setClassName(final String className) {
		this.className = className;
	}

	public String getManagementMachineTemplate() {
		return managementMachineTemplate;
	}

	public void setManagementMachineTemplate(final String managementMachineTemplate) {
		this.managementMachineTemplate = managementMachineTemplate;
	}

	public boolean isBootstrapManagementOnPublicIp() {
		return bootstrapManagementOnPublicIp;
	}

	public void setBootstrapManagementOnPublicIp(final boolean bootstrapManagementOnPublicIp) {
		this.bootstrapManagementOnPublicIp = bootstrapManagementOnPublicIp;
	}

	public boolean isConnectToPrivateIp() {
		return connectToPrivateIp;
	}

	public void setConnectToPrivateIp(final boolean connectToPrivateIp) {
		this.connectToPrivateIp = connectToPrivateIp;
	}

	@Override
	public String toString() {
		return "CloudConfiguration [className=" + className + ", nicAddress=" + nicAddress + ", lookupGroups="
				+ lookupGroups + ", lookupLocators=" + lookupLocators + ", managementMachineTemplate="
				+ managementMachineTemplate + ", bootstrapManagementOnPublicIp=" + bootstrapManagementOnPublicIp
				+ ", connectToPrivateIp=" + connectToPrivateIp
				+ ", remotePassword=***]";
	}

	public GridComponents getComponents() {
		return components;
	}

	public void setComponents(final GridComponents components) {
		this.components = components;
	}

	public String getPersistentStoragePath() {
		return persistentStoragePath;
	}

	public void setPersistentStoragePath(final String persistentStoragePath) {
		this.persistentStoragePath = persistentStoragePath;
	}
	public String getStorageClassName() {
		return storageClassName;
	}

	public void setStorageClassName(final String storageClassName) {
		this.storageClassName = storageClassName;
	}

	public String getManagementStorageTemplate() {
		return managementStorageTemplate;
	}

	public void setManagementStorageTemplate(final String managementStorageTemplate) {
		this.managementStorageTemplate = managementStorageTemplate;
	}

	public short getAdminLoadingTimeInSeconds() {
		return adminLoadingTimeInSeconds;
	}

	public void setAdminLoadingTimeInSeconds(final short adminLoadingTimeInSeconds) {
		this.adminLoadingTimeInSeconds = adminLoadingTimeInSeconds;
	}
	
	public String getNetworkDriverClassName() {
		return networkDriverClassName;
	}

	public void setNetworkDriverClassName(String networkDriverClassName) {
		this.networkDriverClassName = networkDriverClassName;
	}
}
