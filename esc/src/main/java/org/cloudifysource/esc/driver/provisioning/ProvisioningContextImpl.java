package org.cloudifysource.esc.driver.provisioning;

import java.io.FileNotFoundException;
import java.util.Map;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.ScriptLanguages;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.installer.EnvironmentFileBuilder;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.util.InstallationDetailsBuilder;

public class ProvisioningContextImpl implements ProvisioningContext, ManagementProvisioningContext {

	private String locationId;
	private InstallationDetailsBuilder installationDetailsBuilder = new InstallationDetailsBuilder();

	public ProvisioningContextImpl() {
	}

	public void setLocationId(String locationId) {
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
		ScriptLanguages scriptLanguage = template.getScriptLanguage();

		Map<String, String> externalEnvVars = template.getEnv();
		EnvironmentFileBuilder fileBuilder = new EnvironmentFileBuilder(scriptLanguage, externalEnvVars);
		installationDetailsBuilder.machineDetails(md);
		InstallationDetails installationDetails = installationDetailsBuilder.build();

		fileBuilder.loadEnvironmentFileFromDetails(installationDetails);
		fileBuilder.build();
		final String fileContents = fileBuilder.toString();

		return fileContents;
	}

	@Override
	public String[] createManagementEnvironmentScript(final MachineDetails[] mds, final ComputeTemplate template)
			throws FileNotFoundException {

		if (mds == null || mds.length == 0) {
			throw new IllegalArgumentException("mds must have at least one element");
		}

		final String[] result = new String[mds.length];
		ScriptLanguages scriptLanguage = template.getScriptLanguage();
		InstallationDetails installationDetails = installationDetailsBuilder.build();
		
		Cloud cloud = this.installationDetailsBuilder.getCloud();
		final String lookupLocatorsString = createLocatorsString(mds, cloud);
		installationDetails.setLocator(lookupLocatorsString);
				
		for (int i = 0; i < mds.length; i++) {
			MachineDetails md = mds[i];

			Map<String, String> externalEnvVars = template.getEnv();
			EnvironmentFileBuilder fileBuilder = new EnvironmentFileBuilder(scriptLanguage, externalEnvVars);
			
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
			final MachineDetails[] mds, Cloud cloud) {

		final Integer port = cloud.getConfiguration().getComponents().getDiscovery().getDiscoveryPort();
		final StringBuilder lookupSb = new StringBuilder();
		for (final MachineDetails detail : mds) {
			final String ip = cloud.getConfiguration().isConnectToPrivateIp() ? detail
					.getPrivateAddress() : detail.getPublicAddress();

			lookupSb.append(ip).append(":").append(port).append(',');
		}

		lookupSb.setLength(lookupSb.length() - 1);

		return lookupSb.toString();
	}

}
