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
package org.cloudifysource.shell.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.shell.ShellUtils;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Uninstalls a service. Required arguments: service-name The name of the service to uninstall.
 * 
 *        Optional arguments: timeout - The number of minutes to wait until the operation is completed (default: 5
 *        minutes) progress - The polling time interval in seconds, used for checking if the operation is completed
 *        (default: 5 seconds)
 * 
 *        Command syntax: uninstall-service [-timeout timeout] [-progress progress] service-name
 */
@Command(scope = "cloudify", name = "uninstall-service", description = "undeploy a service")
public class UninstallService extends AdminAwareCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 5;

	private static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out. "
			+ "Try to increase the timeout using the -timeout flag";

	@Argument(index = 0, required = true, name = "service-name")
	private String serviceName;

	/**
	 * Gets all services installed on the default application.
	 * 
	 * @return a collection of services' names
	 */
	@CompleterValues(index = 0)
	public Collection<String> getServiceList() {
		try {
			return getRestAdminFacade().getServicesList(CloudifyConstants.DEFAULT_APPLICATION_NAME);
		} catch (final Exception e) {
			return new ArrayList<String>();
		}
	}

	@Option(required = false, name = "-timeout", description = "The number of minutes to wait until the operation is"
			+ " done. Defaults to 5 minutes.")
	private int timeoutInMinutes = DEFAULT_TIMEOUT_MINUTES;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute()
			throws Exception {

		if (!askUninstallConfirmationQuestion()) {
			return getFormattedMessage("uninstall_aborted");
		}

		Map<String, String> undeployServiceResponse = adminFacade.undeploy(getCurrentApplicationName(),
				serviceName, timeoutInMinutes);

		String pollingID = undeployServiceResponse.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);

		this.adminFacade.waitForLifecycleEvents(pollingID, timeoutInMinutes, TIMEOUT_ERROR_MESSAGE);
		return getFormattedMessage("undeployed_successfully", Color.GREEN, serviceName);
	}

	/**
	 * Asks the user for confirmation to uninstall the service.
	 * 
	 * @return true if the user confirmed, false otherwise
	 * @throws IOException Reporting a failure to get the user's confirmation
	 */
	// returns true if the answer to the question was 'Yes'.
	private boolean askUninstallConfirmationQuestion()
			throws IOException {
		return ShellUtils.promptUser(session, "service_uninstall_confirmation", serviceName);
	}
}
