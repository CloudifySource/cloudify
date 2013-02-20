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

import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.CloudifyConstants;
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
	 * @return Object A list of Strings, representing the applications' names
	 * @throws Exception Reporting a failure to get the applications' names from the REST server
	 */
	@Override
	protected Object doExecute() throws Exception {
		//return adminFacade.getApplicationsMap();
		List<ApplicationDescription> applicationsList = adminFacade.getApplicationDescriptionsList();
		String appsDescription = getApplicationDescriptionFromListAsString(applicationsList);
		return appsDescription;
	}

	private String getApplicationDescriptionFromListAsString(
			final List<ApplicationDescription> applicationsList) {
		StringBuilder sb = new StringBuilder();
		for (ApplicationDescription applicationDescription : applicationsList) {
			sb.append(getApplicationDescriptionAsString(applicationDescription));
			sb.append(CloudifyConstants.NEW_LINE);
		}
		return sb.toString();
	}
}
