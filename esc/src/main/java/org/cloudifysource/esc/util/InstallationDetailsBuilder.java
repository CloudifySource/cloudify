/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GSAReservationId;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.openspaces.admin.zone.config.ExactZonesConfigurer;

/***********************
 * Builder for the InstallationDetails object. Useful when some parts of the installation details are known in advance
 * and you only need to set up the last few.
 * 
 * @author barakme
 * @since 2.6.1
 * 
 */
public class InstallationDetailsBuilder {

	private Cloud cloud;
	public ComputeTemplate getTemplate() {
		return template;
	}

	public MachineDetails getMd() {
		return md;
	}

	public Set<String> getZones() {
		return zones;
	}

	public String getLookupLocatorsString() {
		return lookupLocatorsString;
	}

	public Admin getAdmin() {
		return admin;
	}

	public boolean isManagement() {
		return isManagement;
	}

	public File getCloudFile() {
		return cloudFile;
	}

	public GSAReservationId getReservationId() {
		return reservationId;
	}

	public String getTemplateName() {
		return templateName;
	}

	public String getSecurityProfile() {
		return securityProfile;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public String getAuthGroups() {
		return authGroups;
	}

	public boolean isRebootstrapping() {
		return isRebootstrapping;
	}

	private ComputeTemplate template;
	private MachineDetails md;
	private Set<String> zones = new HashSet<String>();
	private String lookupLocatorsString;
	private Admin admin;
	private boolean isManagement;
	private File cloudFile;
	private GSAReservationId reservationId;
	private String templateName;
	private String securityProfile;
	private String keystorePassword;
	private String authGroups;
	private boolean isRebootstrapping;

	public InstallationDetailsBuilder() {
	
	}
	
	public InstallationDetailsBuilder clone() {
		InstallationDetailsBuilder copy = new InstallationDetailsBuilder();
		try {
			BeanUtils.copyProperties(copy, this);
		} catch (IllegalAccessException e) {
			// should not be possible
			throw new RuntimeException("Failed to clone existing installation details builder: " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			// should not be possible
			throw new RuntimeException("Failed to clone existing installation details builder: " + e.getMessage(), e);
		}
		return copy;
	}

	public void setMachineDetails(final MachineDetails md) {
		this.md = md;
	}

	public void setCloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public void setTemplate(final ComputeTemplate template) {
		this.template = template;
	}

	public void setZones(final Set<String> zones) {
		this.zones.addAll(zones);
	}

	public void setLookupLocators(final String lookupLocators) {
		this.lookupLocatorsString = lookupLocators;
	}

	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

	public void setManagement(final boolean isManagement) {
		this.isManagement = isManagement;
	}

	public void setCloudFile(final File cloudFile) {
		this.cloudFile = cloudFile;
	}

	public void setReservationId(final GSAReservationId reservation) {
		this.reservationId = reservation;
	}

	public void setTemplateName(final String templateName) {
		this.templateName = templateName;
	}

	public void setSecurityProfile(final String securityProfile) {
		this.securityProfile = securityProfile;
	}

	public void setKeystorePassword(final String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	public void setAuthGroups(final String authGroups) {
		this.authGroups = authGroups;
	}

	public void setRebootstrapping(final boolean isRebootstrapping) {
		this.isRebootstrapping = isRebootstrapping;
	}

	/*******
	 * 
	 * @return
	 * @throws FileNotFoundException
	 *             if a key file is specified and is not found.
	 */
	public InstallationDetails build() throws FileNotFoundException {
		final ExactZonesConfigurer configurer = new ExactZonesConfigurer()
			.addZones(this.zones);


		final ExactZonesConfig zonesConfig = configurer.create();
		final InstallationDetails details =
				Utils.createInstallationDetails(md, cloud, template, zonesConfig, lookupLocatorsString, admin, isManagement,
						cloudFile, reservationId, templateName, securityProfile, keystorePassword, authGroups,
						this.isRebootstrapping, false);

		return details;
	}

	public Cloud getCloud() {
		return cloud;
	}

	
}
