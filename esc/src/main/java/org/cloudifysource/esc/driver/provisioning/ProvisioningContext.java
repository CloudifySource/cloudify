package org.cloudifysource.esc.driver.provisioning;

import java.io.FileNotFoundException;

import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;

public interface ProvisioningContext {

	public abstract String createEnvironmentScript(final ComputeTemplate template) throws FileNotFoundException;

}
