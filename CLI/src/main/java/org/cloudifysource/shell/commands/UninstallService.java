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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.util.Properties.PropertiesReader;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.rest.RestAdminFacade;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Uninstalls a service. Required arguments: service-name The name of the service to uninstall.
 *        
 *        Optional arguments:
 *        timeout - The number of minutes to wait until the operation is completed (default: 5 minutes)
 *        progress - The polling time interval in seconds, used for checking if the operation is completed
 *         (default: 5 seconds)
 * 
 *        Command syntax: uninstall-service [-timeout timeout] [-progress progress] service-name
 */
@Command(scope = "cloudify", name = "uninstall-service", description = "undeploy a service")
public class UninstallService extends AdminAwareCommand {

	private static final String TIMEOUT_ERROR_MESSAGE = "Timeout waiting for service to uninstall";

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
		private int timeoutInMinutes = 5;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {

		if (!askUninstallConfirmationQuestion()) {
			return getFormattedMessage("uninstall_aborted");
		}

		final Set<String> containerIdsOfService = ((RestAdminFacade) adminFacade)
		.getGridServiceContainerUidsForService(getCurrentApplicationName(), serviceName);
		if (verbose) {
			logger.info("Found containers: " + containerIdsOfService);
		}

		Map<String, String> undeployServiceResponse = adminFacade.undeploy(getCurrentApplicationName(), serviceName, timeoutInMinutes);
		if (undeployServiceResponse.containsKey(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID)){
			String pollingID = undeployServiceResponse.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);
			this.adminFacade.waitForLifecycleEvents(pollingID, timeoutInMinutes, TIMEOUT_ERROR_MESSAGE);
		} else {
			throw new CLIException("Failed to retrieve lifecycle logs from rest. " +
			"Check logs for more details.");
		}
		return getFormattedMessage("undeployed_successfully", serviceName);
	}

	/**
	 * Asks the user for confirmation to uninstall the service.
	 * 
	 * @return true if the user confirmed, false otherwise
	 * @throws IOException
	 *             Reporting a failure to get the user's confirmation
	 */
	// returns true if the answer to the question was 'Yes'.
	private boolean askUninstallConfirmationQuestion() throws IOException {

		// we skip question if the shell is running a script.
		if ((Boolean) session.get(Constants.INTERACTIVE_MODE)) {
			final String confirmationQuestion = getFormattedMessage("service_uninstall_confirmation", serviceName);
			System.out.print(confirmationQuestion);
			System.out.flush();
			final PropertiesReader pr = new PropertiesReader(new InputStreamReader(System.in));
			final String answer = pr.readProperty();
			System.out.println();
			System.out.flush();
			return "y".equalsIgnoreCase(answer);

		}
		// Shell is running in nonInteractive mode. we skip the question.
		return true;
	}
}
