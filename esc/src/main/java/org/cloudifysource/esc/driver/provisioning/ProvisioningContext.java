package org.cloudifysource.esc.driver.provisioning;

import java.io.FileNotFoundException;

import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;

public interface ProvisioningContext {

		String createEnvironmentScript(final MachineDetails md, final ComputeTemplate template) throws FileNotFoundException;

}
