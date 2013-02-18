package org.cloudifysource.esc.driver.provisioning.context;

import java.util.concurrent.Callable;

/**
 * The shared context of all provisioning drivers from the same concrete class.
 * @author itaif
 *
 */
public interface ProvisioningDriverClassContext {

	Object getOrCreate(String key, Callable<Object> factory) throws Exception;
}
