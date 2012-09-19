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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;

import com.j_spaces.kernel.PlatformVersion;

/**********************
 * A POJO for the provider specific configuration of a cloud driver.
 * 
 * @author barakme
 * @since 2.0.0
 * 
 */
@CloudifyDSLEntity(name = "provider", clazz = CloudProvider.class, allowInternalNode = true, allowRootNode = false,
		parent = "cloud")
public class CloudProvider {

	private String provider;
	
	private String cloudifyUrl = "http://repository.cloudifysource.org/" 
			+ "%s/" + PlatformVersion.getVersion() + "%s/gigaspaces-%s-" 
			+ PlatformVersion.getVersion() + "-" + PlatformVersion.getMilestone() 
			+ "-b" + PlatformVersion.getBuildNumber();

	// location of zip file where additional cloudify files are places.
	// They will be copied on top of the cloudify distribution.
	private String cloudifyOverridesUrl;

	private String machineNamePrefix;

	@Deprecated
	private boolean dedicatedManagementMachines = true;

	private List<String> managementOnlyFiles;

	private String sshLoggingLevel = Level.INFO.toString();

	@Deprecated
	private List<String> zones = Arrays.asList("agent");

	private String managementGroup;
	private int numberOfManagementMachines;
	private int reservedMemoryCapacityPerMachineInMB;

	public String getProvider() {
		return provider;
	}

	public void setProvider(final String provider) {
		this.provider = provider;
	}	

	public String getCloudifyUrl() {
		return getCloudifyUrlAccordingToPlatformVersion();
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

	@Deprecated
	public boolean isDedicatedManagementMachines() {
		return dedicatedManagementMachines;
	}

	@Deprecated
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

	@Deprecated
	public List<String> getZones() {
		return zones;
	}
	
	@Deprecated
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
		return "CloudProvider [provider=" + provider 
				+ ", cloudifyUrl=" + cloudifyUrl
				+ ", machineNamePrefix=" + machineNamePrefix
				+ ", dedicatedManagementMachines=" + dedicatedManagementMachines + ", managementOnlyFiles="
				+ managementOnlyFiles + ",  sshLoggingLevel=" + sshLoggingLevel + ", zones=" + zones
				+ ", managementGroup=" + managementGroup + ", numberOfManagementMachines=" + numberOfManagementMachines
				+ ", reservedMemoryCapacityPerMachineInMB=" + reservedMemoryCapacityPerMachineInMB + "]";
	}

	public String getCloudifyOverridesUrl() {
		return cloudifyOverridesUrl;
	}

	public void setCloudifyOverridesUrl(final String cloudifyOverridesUrl) {
		this.cloudifyOverridesUrl = cloudifyOverridesUrl;
	}
	
	private String getCloudifyUrlAccordingToPlatformVersion() {

		String productUri;
		String versionPostFix;
		String editionUrlVariable;
		
		if (PlatformVersion.getEdition().equalsIgnoreCase(CloudifyConstants.CLOUDIFY_EDITION)) {
			productUri = "org/cloudifysource";
			versionPostFix = "";
			editionUrlVariable = "cloudify";
			return String.format(this.cloudifyUrl, productUri, versionPostFix, editionUrlVariable);
		} else {
			productUri = "com/gigaspaces/xap";
			editionUrlVariable = "xap-premium";
			if (PlatformVersion.getBuildNumber().contains("-")) {
				versionPostFix = ".SNAPSHOT";
			} else {
				versionPostFix = ".RELEASE";
			}
			return String.format(cloudifyUrl, productUri, versionPostFix, editionUrlVariable);
		}
	}
	
	public static void main(String[] args) {
		CloudProvider cp = new CloudProvider();
		String cloudifyUrl2 = cp.getCloudifyUrl();
		System.out.println(cloudifyUrl2);
	}
	
	

}
