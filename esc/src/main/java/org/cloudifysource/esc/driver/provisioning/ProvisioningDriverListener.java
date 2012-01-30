package org.cloudifysource.esc.driver.provisioning;

/**
 * a Listener for all published events in the DefaultProvisioningDriver class.
 * 
 * @author adaml
 *
 */
public interface ProvisioningDriverListener {
	
	void onProvisioningEvent(String eventName, Object... args);
	
}
