package org.cloudifysource.esc.shell.listner;

import java.util.logging.Logger;

import org.cloudifysource.esc.driver.provisioning.ProvisioningDriverListener;

/**
 * Event listener for events originated in the DefaultProvisioningDriver.
 * 
 * @author adaml
 *
 */
public class CliProvisioningDriverListner extends AbstractEventListener implements ProvisioningDriverListener {

	private Logger logger = Logger.getLogger(CliProvisioningDriverListner.class.getName());
	
	@Override
	public void onProvisioningEvent(String eventName, Object... args) {
		logger.info(getFormattedMessage(eventName, args));
	}

}
