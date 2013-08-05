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
package org.cloudifysource.esc.driver.provisioning;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudifysource.domain.cloud.Cloud;
import org.cloudifysource.domain.cloud.ScriptLanguages;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.utils.IPUtils;
import org.cloudifysource.esc.installer.EnvironmentFileBuilder;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.util.InstallationDetailsBuilder;

/*********************************
 * Implementation of the provisioning context interface for both agent and manager machines. Provides access to the
 * provisioning related functionality.
 * 
 * @author barakme
 * @since 2.6.1
 */
public class ProvisioningContextImpl implements ProvisioningContext, ManagementProvisioningContext {

	private String locationId;
	private final InstallationDetailsBuilder installationDetailsBuilder = new InstallationDetailsBuilder();
	private final List<InstallationDetails> createdDetails = new LinkedList<InstallationDetails>();

	public ProvisioningContextImpl() {
	}

	public void setLocationId(final String locationId) {
		this.locationId = locationId;

	}

	public String getLocationId() {
		return this.locationId;
	}

	public InstallationDetailsBuilder getInstallationDetailsBuilder() {
		return this.installationDetailsBuilder;
	}

	@Override
	public String createEnvironmentScript(final MachineDetails md, final ComputeTemplate template)
			throws FileNotFoundException {

		if (md == null) {
			throw new IllegalArgumentException("Machine Details were not set");
		}
		final ScriptLanguages scriptLanguage = template.getScriptLanguage();

		installationDetailsBuilder.setMachineDetails(md);
		final InstallationDetails installationDetails = installationDetailsBuilder.build();
		createdDetails.add(installationDetails);

		final EnvironmentFileBuilder fileBuilder =
				new EnvironmentFileBuilder(scriptLanguage, installationDetails.getExtraRemoteEnvironmentVariables());
		fileBuilder.loadEnvironmentFileFromDetails(installationDetails);
		fileBuilder.build();
		final String fileContents = fileBuilder.toString();

		return fileContents;
	}

	public List<InstallationDetails> getCreatedDetails() {
		return this.createdDetails;
	}

	@Override
	public String[] createManagementEnvironmentScript(final MachineDetails[] mds, final ComputeTemplate template)
			throws FileNotFoundException {

		if (mds == null || mds.length == 0) {
			throw new IllegalArgumentException("mds must have at least one element");
		}

		final String[] result = new String[mds.length];
		final ScriptLanguages scriptLanguage = template.getScriptLanguage();

		final Cloud cloud = this.installationDetailsBuilder.getCloud();
		final String lookupLocatorsString = createLocatorsString(mds, cloud);

		for (int i = 0; i < mds.length; i++) {
			final MachineDetails md = mds[i];

			final InstallationDetailsBuilder currentBuilder = installationDetailsBuilder.clone();
			currentBuilder.setMachineDetails(md);
			currentBuilder.setLookupLocators(lookupLocatorsString);

			final InstallationDetails installationDetails = currentBuilder.build();
			createdDetails.add(installationDetails);

			final Map<String, String> externalEnvVars = installationDetails.getExtraRemoteEnvironmentVariables();
			final EnvironmentFileBuilder fileBuilder = new EnvironmentFileBuilder(scriptLanguage, externalEnvVars);

			installationDetails.setNoWebServices(i != 0);
			fileBuilder.loadEnvironmentFileFromDetails(installationDetails);
			fileBuilder.build();
			final String fileContents = fileBuilder.toString();
			result[i] = fileContents;

		}

		return result;
	}

	// TODO - this is copy/paste from CloudGridAgentBootstrapper
	private String createLocatorsString(
			final MachineDetails[] mds, final Cloud cloud) {

		final Integer port = cloud.getConfiguration().getComponents().getDiscovery().getDiscoveryPort();
		final StringBuilder lookupSb = new StringBuilder();
		for (final MachineDetails detail : mds) {
			final String ip = cloud.getConfiguration().isConnectToPrivateIp() ? IPUtils.getSafeIpAddress(detail
					.getPrivateAddress()) : IPUtils.getSafeIpAddress(detail.getPublicAddress());

			lookupSb.append(ip).append(":").append(port).append(',');
		}

		lookupSb.setLength(lookupSb.length() - 1);

		return lookupSb.toString();
	}

}
