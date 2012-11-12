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
package org.cloudifysource.dsl.cloud;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

/***********
 * Domain POJO for the cloud configuration.
 * 
 * @author barakme
 * @since 2.0.0
 *
 */
@CloudifyDSLEntity(name = "configuration", clazz = CloudConfiguration.class, allowInternalNode = true,
		allowRootNode = false, parent = "cloud")
public class CloudConfiguration {

	private String className = "org.cloudifysource.esc.driver.provisioning.jclouds.DefaultProvisioningDriver";
	private String nicAddress;
	private String lookupGroups;
	private String lookupLocators;
	private String managementMachineTemplate;
	private boolean bootstrapManagementOnPublicIp = true;
	private boolean connectToPrivateIp = true;

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

}
