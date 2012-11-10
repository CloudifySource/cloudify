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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.rest.ApplicationDescription;

/**
 * @author noak, adaml
 * @since 2.0.1
 * 
 *        Lists all deployed applications
 * 
 *        Command syntax: list-applications
 * 
 */
@Command(scope = "cloudify", name = "list-applications", description = "Lists all deployed applications")
public class ListApplications extends AbstractListCommand {

	/**
	 * Gets a list of all deployed applications' names.
	 * @return Object A map of applications' descriptions and authorization groups
	 * @throws Exception Reporting a failure to get the applications' names from the REST server
	 */
@Override
	protected Object doExecute() throws Exception {
		Map<ApplicationDescription, String> origMap = adminFacade.getApplicationsDescriptionAndAuthGroups();
		Map<String, String> displayMap = new HashMap<String, String>(origMap.size());
		for (ApplicationDescription applicationDescription : origMap.keySet()) {
			String appDescStr = getApplicationDescriptionAsString(applicationDescription);
			String authGroups = origMap.get(applicationDescription);
			displayMap.put(appDescStr, authGroups);
		}
		return displayMap;
	}
}
