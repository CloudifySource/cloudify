package org.cloudifysource.esc.driver.provisioning.context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class DefaultProvisioningDriverClassContext implements ProvisioningDriverClassContext {
	
    private final Map<String, Object> context = new HashMap<String, Object>();
    
    @Override
	public Object getOrCreate(String key, Callable<Object> factory) throws Exception {
        synchronized (context) {
            if (!context.containsKey(key)) {
                Object value = factory.call();
                context.put(key, value);
                return value;
            }
            return context.get(key);
        }
    }
    

}
