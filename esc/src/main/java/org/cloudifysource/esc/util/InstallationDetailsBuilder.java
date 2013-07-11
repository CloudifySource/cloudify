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

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.driver.provisioning.MachineDetails;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.openspaces.admin.Admin;
import org.openspaces.admin.gsa.GSAReservationId;
import org.openspaces.admin.zone.config.ExactZonesConfig;

/***********************
 * Builder for the InstallationDetails object. Useful when some parts of the installation details are known in advance
 * and you only need to set up the last few.
 * 
 * @author barakme
 * @since 2.7.0
 * 
 */
public class InstallationDetailsBuilder {

	private Cloud cloud;
	private ComputeTemplate template;
	private MachineDetails md;
	private ExactZonesConfig zones;
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

		/*
		 * public static InstallationDetails createInstallationDetails(final MachineDetails md, final Cloud cloud, final
		 * ComputeTemplate template, final ExactZonesConfig zones, final String lookupLocatorsString, final Admin admin,
		 * final boolean isManagement, final File cloudFile, final GSAReservationId reservationId, final String
		 * templateName, final String securityProfile, final String keystorePassword, final String authGroups, final
		 * boolean rebootstrapping) throws FileNotFoundException
		 */
	}

	public void machineDetails(final MachineDetails md) {
		this.md = md;
	}

	public void cloud(final Cloud cloud) {
		this.cloud = cloud;
	}

	public void template(final ComputeTemplate template) {
		this.template = template;
	}

	public void zones(final ExactZonesConfig zones) {
		this.zones = zones;
	}

	public void lookupLocators(final String lookupLocators) {
		this.lookupLocatorsString = lookupLocators;
	}

	public void admin(final Admin admin) {
		this.admin = admin;
	}

	public void management(final boolean isManagement) {
		this.isManagement = isManagement;
	}

	public void cloudFile(final File cloudFile) {
		this.cloudFile = cloudFile;
	}

	public void reservationId(final GSAReservationId reservation) {
		this.reservationId = reservation;
	}

	public void templateName(final String templateName) {
		this.templateName = templateName;
	}

	public void securityProfile(final String securityProfile) {
		this.securityProfile = securityProfile;
	}

	public void keystorePassword(final String keystorePassword) {
		this.keystorePassword = keystorePassword;
	}

	public void authGroups(final String authGroups) {
		this.authGroups = authGroups;
	}

	public void rebootstrapping(final boolean isRebootstrapping) {
		this.isRebootstrapping = isRebootstrapping;
	}

	public InstallationDetails build() throws FileNotFoundException {
		final InstallationDetails details =
				Utils.createInstallationDetails(md, cloud, template, zones, lookupLocatorsString, admin, isManagement,
						cloudFile, reservationId, templateName, securityProfile, keystorePassword, authGroups,
						this.isRebootstrapping);

		return details;
	}
}
