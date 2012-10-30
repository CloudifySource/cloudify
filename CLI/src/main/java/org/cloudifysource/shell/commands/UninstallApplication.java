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
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Uninstalls an application.
 * 
 *        Required arguments: applicationName - The name of the application
 * 
 *        Optional arguments: timeout - The number of minutes to wait until the operation is completed (default: 5).
 * 
 *        Command syntax: uninstall-application [-timeout timeout] applicationName
 * 
 */
@Command(scope = "cloudify", name = "uninstall-application", description = "Uninstalls an application.")
public class UninstallApplication extends AdminAwareCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 5;

	/****
	 * this is public as used elsewhere.
	 */
	public static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out. "
			+ "Try to increase the timeout using the -timeout flag";

	@Argument(index = 0, required = true, name = "The name of the application")
	private String applicationName;

	/**
	 * Gets all deployed applications' names.
	 * 
	 * @return Collection of applications' names.
	 */
	@CompleterValues(index = 0)
	public Collection<String> getCompleterValues() {
		try {
			return getRestAdminFacade().getApplicationsList();
		} catch (final CLIException e) {
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
		
		if (CloudifyConstants.MANAGEMENT_APPLICATION_NAME.equalsIgnoreCase(applicationName)) {
			throw new CLIStatusException("cannot_uninstall_management_application");
		}

		if (verbose) {
			// we need to look at all containers since the application already undeployed and we cannot get only
			// the application containers
			final Set<String> containerIdsOfApplication = ((RestAdminFacade) adminFacade)
					.getGridServiceContainerUidsForApplication(applicationName);
			logger.info("Containers running PUs of application " + applicationName + ":" + containerIdsOfApplication);
		}

		Map<String, String> uninstallApplicationResponse = this.adminFacade
				.uninstallApplication(this.applicationName, timeoutInMinutes);

		String pollingID = uninstallApplicationResponse.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);
		this.adminFacade.waitForLifecycleEvents(pollingID, timeoutInMinutes, TIMEOUT_ERROR_MESSAGE);

		session.put(Constants.ACTIVE_APP, "default");
		GigaShellMain.getInstance().setCurrentApplicationName("default");
		return getFormattedMessage("application_uninstalled_succesfully", Color.GREEN, this.applicationName);
	}

	// returns true if the answer to the question was 'Yes'.
	/**
	 * Asks the user for confirmation to uninstall the application.
	 * 
	 * @return true if the user confirmed, false otherwise
	 * @throws IOException Reporting a failure to get the user's confirmation
	 */
	private boolean askUninstallConfirmationQuestion()
			throws IOException {
		return true;
//		return ShellUtils.promptUser(session, "application_uninstall_confirmation", applicationName);
	}
}
