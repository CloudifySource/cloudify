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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.internal.CloudifyDSLEntity;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.openspaces.maven.support.OutputVersion;

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

	private String cloudifyUrl = getCloudifyUrlAccordingToPlatformVersion();

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
	private int reservedMemoryCapacityPerManagementMachineInMB;

	public String getProvider() {
		return provider;
	}

	public void setProvider(final String provider) {
		this.provider = provider;
	}

	public String getCloudifyUrl() {
		return this.cloudifyUrl;
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

	/**********
	 * The reservedMemoryCapacityPerMachineInMB is the estimated amount of RAM
	 * used by the operating system and the agent running on the machine.
	 * It is not relevant when you install one instance per machine (which is the
	 * default Global mode).
	 * It is relevant when you enable multiple instances
	 * on the same machine. In that case the plan needs to take the amount of
	 * estimated memory used by each instance plus the reserved memory estimate
	 * and compares it to the memory of the machine.
	 *
	 * @return .
	 */
	public int getReservedMemoryCapacityPerMachineInMB() {
		return reservedMemoryCapacityPerMachineInMB;
	}

	public void setReservedMemoryCapacityPerMachineInMB(final int reservedMemoryCapacityPerMachineInMB) {
		this.reservedMemoryCapacityPerMachineInMB = reservedMemoryCapacityPerMachineInMB;
	}

	public int getReservedMemoryCapacityPerManagementMachineInMB() {
		return reservedMemoryCapacityPerManagementMachineInMB;
	}

	public void setReservedMemoryCapacityPerManagementMachineInMB(
			final int reservedMemoryCapacityPerManagementMachineInMB) {
		this.reservedMemoryCapacityPerManagementMachineInMB = reservedMemoryCapacityPerManagementMachineInMB;
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

		String cloudifyUrlPattern;
		String productUri;
		String editionUrlVariable;

		if (PlatformVersion.getEdition().equalsIgnoreCase(PlatformVersion.EDITION_CLOUDIFY)) {
			productUri = "org/cloudifysource";
			editionUrlVariable = "cloudify";
			cloudifyUrlPattern = "http://repository.cloudifysource.org/"
					+ "%s/" + OutputVersion.computeCloudifyVersion() + "/gigaspaces-%s-"
					+ PlatformVersion.getVersion() + "-" + PlatformVersion.getMilestone()
					+ "-b" + PlatformVersion.getBuildNumber();
			return String.format(cloudifyUrlPattern, productUri, editionUrlVariable);
		} else {
			productUri = "com/gigaspaces/xap";
			editionUrlVariable = "xap-premium";
			cloudifyUrlPattern = "http://repository.cloudifysource.org/"
					+ "%s/" + OutputVersion.computeXapVersion() + "/gigaspaces-%s-"
					+ PlatformVersion.getVersion() + "-" + PlatformVersion.getMilestone()
					+ "-b" + PlatformVersion.getBuildNumber();
			return String.format(cloudifyUrlPattern, productUri, editionUrlVariable);
		}
	}
	
	@DSLValidation
	void validateProviderName(final DSLValidationContext validationContext)
			throws DSLValidationException {

		if (StringUtils.isBlank(provider)) {
			throw new DSLValidationException("Provider cannot be empty");
		}
	}
	
	@DSLValidation
	void validateCloudifyUrl(final DSLValidationContext validationContext)
			throws DSLValidationException {
		
		/*String[] schema = {"http"};
		UrlValidator urlValidator = new UrlValidator(schema);
		if (!urlValidator.isValid(cloudifyUrl)) {
			throw new DSLValidationException("Invalid cloudify url: \"" + cloudifyUrl + "\"");
		}*/
		
		try {
	        new URI(cloudifyUrl);
		} catch (URISyntaxException e) {
			throw new DSLValidationException("Invalid cloudify url: \"" + cloudifyUrl + "\"");
		}
		
		//TODO request "head" to see if the url is accessible. If not - warning.		
	}
	
	@DSLValidation
	void validateNumberOfManagementMachines(final DSLValidationContext validationContext)
			throws DSLValidationException {

		if (numberOfManagementMachines != 1 && numberOfManagementMachines != 2) {
			throw new DSLValidationException("Invalid numberOfManagementMachines: \"" + numberOfManagementMachines 
					+ "\". Valid values are 1 or 2");
		}
		
		//TODO request "head" to see if the url is accessible. If not - warning.
	}
	
	@DSLValidation
	void validateSshLoggingLevel(final DSLValidationContext validationContext)
			throws DSLValidationException {

		if (!sshLoggingLevel.matches("INFO|FINE|WARNING|DEBUG")) {
			throw new DSLValidationException("sshLoggingLevel \"" + sshLoggingLevel + "\" is invalid, "
					+ "supported values are: INFO, FINE, FINER, FINEST, WARNING, DEBUG");
		}
	}

}
