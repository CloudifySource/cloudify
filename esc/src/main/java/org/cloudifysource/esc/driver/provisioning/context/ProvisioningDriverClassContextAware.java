package org.cloudifysource.esc.driver.provisioning.context;

import org.cloudifysource.esc.driver.provisioning.context.ProvisioningDriverClassContext;

/**
 * Injects context information that is shared to all objects from the same concrete class.
 * @author itaif
 *
 */
public interface ProvisioningDriverClassContextAware {
	   
	  public void setProvisioningDriverClassContext(ProvisioningDriverClassContext context);

}
