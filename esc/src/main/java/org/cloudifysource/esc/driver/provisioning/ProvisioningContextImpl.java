package org.cloudifysource.esc.driver.provisioning;

import java.io.FileNotFoundException;
import java.util.Map;

import org.cloudifysource.dsl.cloud.ScriptLanguages;
import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;
import org.cloudifysource.esc.installer.EnvironmentFileBuilder;
import org.cloudifysource.esc.installer.InstallationDetails;
import org.cloudifysource.esc.util.InstallationDetailsBuilder;

public class ProvisioningContextImpl implements ProvisioningContext{

	private String locationId;
	private InstallationDetailsBuilder installationDetailsBuilder = new InstallationDetailsBuilder();

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
	public String createEnvironmentScript(final ComputeTemplate template) throws FileNotFoundException {
		
		ScriptLanguages scriptLanguage = template.getScriptLanguage();
		
		Map<String, String> externalEnvVars = template.getEnv();
		EnvironmentFileBuilder fileBuilder = new EnvironmentFileBuilder(scriptLanguage , externalEnvVars );
		InstallationDetails installationDetails = installationDetailsBuilder.build();
		fileBuilder.loadEnvironmentFileFromDetails(installationDetails);
		fileBuilder.build();
		final String fileContents = fileBuilder.toString();
		
		return fileContents;
	}

}
