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

import java.util.List;
import java.util.logging.Level;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name = "provider", clazz = CloudProvider.class, allowInternalNode = true, allowRootNode = false,
		parent = "cloud")
public class CloudProvider {

	private String provider;
	private String localDirectory;
	private String remoteDirectory;
	private String cloudifyUrl;
	
	// location of zip file where additional cloudify files are places. They will be copied on top of the cloudify distribution.
	private String cloudifyOverridesUrl;
	
	private String machineNamePrefix;

	private boolean dedicatedManagementMachines = true;

	private List<String> managementOnlyFiles;

	private String sshLoggingLevel = Level.INFO.toString();

	private List<String> zones;

	private String managementGroup;
	private int numberOfManagementMachines;
	private int reservedMemoryCapacityPerMachineInMB;

	public String getProvider() {
		return provider;
	}

	public void setProvider(final String provider) {
		this.provider = provider;
	}

	public String getLocalDirectory() {
		return localDirectory;
	}

	public void setLocalDirectory(final String localDirectory) {
		this.localDirectory = localDirectory;
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteDirectory(final String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	public String getCloudifyUrl() {
		return cloudifyUrl;
	}

	public void setCloudifyUrl(final String cloudifyUrl) {
		this.cloudifyUrl = cloudifyUrl;
	}

	public String getMachineNamePrefix() {
		return machineNamePrefix;
	}

	public void setMachineNamePrefix(final String machineNamePrefix) {
		this.machineNamePrefix = machineNamePrefix;
	}

	public boolean isDedicatedManagementMachines() {
		return dedicatedManagementMachines;
	}

	public void setDedicatedManagementMachines(final boolean dedicatedManagementMachines) {
		this.dedicatedManagementMachines = dedicatedManagementMachines;
	}

	public List<String> getManagementOnlyFiles() {
		return managementOnlyFiles;
	}

	public void setManagementOnlyFiles(final List<String> managementOnlyFiles) {
		this.managementOnlyFiles = managementOnlyFiles;
	}

	public String getSshLoggingLevel() {
		return sshLoggingLevel;
	}

	public void setSshLoggingLevel(final String sshLoggingLevel) {
		this.sshLoggingLevel = sshLoggingLevel;
	}

	public List<String> getZones() {
		return zones;
	}

	public void setZones(final List<String> zones) {
		this.zones = zones;
	}

	// TODO - move to configuration
	public String getManagementGroup() {
		return managementGroup;
	}

	public void setManagementGroup(final String managementGroup) {
		this.managementGroup = managementGroup;
	}

	// TODO - move to configuration
	public int getNumberOfManagementMachines() {
		return numberOfManagementMachines;
	}

	public void setNumberOfManagementMachines(final int numberOfManagementMachines) {
		this.numberOfManagementMachines = numberOfManagementMachines;
	}

	public int getReservedMemoryCapacityPerMachineInMB() {
		return reservedMemoryCapacityPerMachineInMB;
	}

	public void setReservedMemoryCapacityPerMachineInMB(final int reservedMemoryCapacityPerMachineInMB) {
		this.reservedMemoryCapacityPerMachineInMB = reservedMemoryCapacityPerMachineInMB;
	}

	@Override
	public String toString() {
		return "CloudProvider [provider=" + provider + ", localDirectory=" + localDirectory + ", remoteDirectory="
				+ remoteDirectory + ", cloudifyUrl=" + cloudifyUrl + ", machineNamePrefix=" + machineNamePrefix
				+ ", dedicatedManagementMachines=" + dedicatedManagementMachines + ", managementOnlyFiles="
				+ managementOnlyFiles + ",  sshLoggingLevel=" + sshLoggingLevel + ", zones=" + zones
				+ ", managementGroup=" + managementGroup + ", numberOfManagementMachines=" + numberOfManagementMachines
				+ ", reservedMemoryCapacityPerMachineInMB=" + reservedMemoryCapacityPerMachineInMB + "]";
	}

	public String getCloudifyOverridesUrl() {
		return cloudifyOverridesUrl;
	}

	public void setCloudifyOverridesUrl(String cloudifyOverridesUrl) {
		this.cloudifyOverridesUrl = cloudifyOverridesUrl;
	}

}
