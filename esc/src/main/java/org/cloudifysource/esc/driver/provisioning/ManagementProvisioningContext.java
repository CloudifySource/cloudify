package org.cloudifysource.esc.driver.provisioning;

import java.io.FileNotFoundException;

import org.cloudifysource.dsl.cloud.compute.ComputeTemplate;

public interface ManagementProvisioningContext {

	public abstract String[] createManagementEnvironmentScript(final MachineDetails[] mds,
			final ComputeTemplate template) throws FileNotFoundException;

}
