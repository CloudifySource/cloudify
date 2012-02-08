package org.cloudifysource.esc.driver.provisioning.context;

import java.util.concurrent.Callable;

public interface ProvisioningDriverContext {

	Object getOrCreate(String key, Callable<Object> factory) throws Exception;
}
