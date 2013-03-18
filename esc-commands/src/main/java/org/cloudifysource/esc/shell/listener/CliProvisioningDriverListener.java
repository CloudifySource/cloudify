/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.shell.listener;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.esc.driver.provisioning.ProvisioningDriverListener;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.fusesource.jansi.Ansi.Color;

/**
 * Event listener for events originated in the DefaultProvisioningDriver.
 * 
 * @author adaml
 *
 */
public class CliProvisioningDriverListener extends AbstractEventListener implements ProvisioningDriverListener {

	private static final Logger logger = Logger.getLogger(CliProvisioningDriverListener.class.getName());
	
	@Override
	public void onProvisioningEvent(final String eventName, final Object... args) {
		String formattedMessage = getFormattedMessage(eventName, args);
		System.out.println(formattedMessage);
		System.out.flush();
		logger.log(Level.FINE, formattedMessage);
	}
	
	@Override
	public void onProvisioningOngoingEvent(final String eventName, final Object... args) {
		String formattedMessage = getFormattedMessage(eventName, args);
		System.out.print(formattedMessage);
		System.out.flush();
		logger.log(Level.FINE, formattedMessage);
	}
	
	@Override
	public void onProvisioningEventEnd(final String status) {
		CLIEventsDisplayer displayer = new CLIEventsDisplayer();
		if (StringUtils.isBlank(status)) {
			return;
		}
		
		if (StringUtils.containsIgnoreCase(status, "ok")) {
			displayer.printColoredMessage(status, Color.GREEN);	
		} else {
			displayer.printColoredMessage(status, Color.RED);	
		}
		System.out.flush();
		logger.log(Level.FINE, status);
	}
	
}
