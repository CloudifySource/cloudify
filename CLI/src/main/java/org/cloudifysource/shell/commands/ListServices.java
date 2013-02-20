/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
 ******************************************************************************/
package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.ApplicationDescription;

/**
 * @author noak, adaml
 * @since 2.0.1
 * 
 *        Lists the services deployed on the current application
 * 
 *        Command syntax: list-services
 * 
 */
@Command(scope = "cloudify", name = "list-services", description = "Lists all deployed services on the current"
		+ " application")
public class ListServices extends AbstractListCommand {

	/**
	 * Gets a list of service names, deployed on the current application.
	 * 
	 * @return Object A list of Strings, representing the services' names
	 * @throws Exception
	 *             Reporting a failure to get the services' names from the REST server
	 */
	@Override
	protected Object doExecute() throws Exception {
		ApplicationDescription applicationDescription = null;
		try {
			String applicationName = getCurrentApplicationName();
			applicationDescription = adminFacade.getServicesDescriptionList(applicationName);
			if (applicationDescription == null) {
				return "";
			}
		} catch (final CLIStatusException e) {
			// if this message indicates the *default* app is not found - don't throw exception, return an
			// empty list
			if (getCurrentApplicationName().equalsIgnoreCase(CloudifyConstants.DEFAULT_APPLICATION_NAME)
					&& CloudifyConstants.ERR_REASON_CODE_FAILED_TO_LOCATE_APP.equalsIgnoreCase(e.getReasonCode())) {
				return "";
			} else {
				throw e;
			}
		}
		
		return getApplicationDescriptionAsString(applicationDescription);
	}
}
