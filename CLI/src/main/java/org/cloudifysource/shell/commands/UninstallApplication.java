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
import java.util.concurrent.TimeoutException;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.util.Properties.PropertiesReader;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.cloudifysource.shell.rest.RestAdminFacade;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Uninstalls an application.
 *		
 *        Required arguments:
 *         applicationName - The name of the application
 *        
 *        Optional arguments:
 *         timeout - The number of minutes to wait until the operation is completed (default: 5).
 *  
 *        Command syntax: uninstall-application [-timeout timeout] applicationName
 * 
 */
@Command(scope = "cloudify", name = "uninstall-application", description = "Uninstalls an application.")
public class UninstallApplication extends AdminAwareCommand {

	private static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out. "
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
		private int timeoutInMinutes = 5;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {

		if (!askUninstallConfirmationQuestion()) {
			return getFormattedMessage("uninstall_aborted");
		}

		// we need to look at all containers since the application already undeployed and we cannot get only
		// the application containers
		final Set<String> containerIdsOfApplication = ((RestAdminFacade) adminFacade)
		.getGridServiceContainerUidsForApplication(applicationName);
		if (verbose) {
			logger.info("Containers running PUs of application " + applicationName + ":" + containerIdsOfApplication);
		}

		Map<String, String> uninstallApplicationResponse = this.adminFacade
		.uninstallApplication(this.applicationName, timeoutInMinutes);

		if (uninstallApplicationResponse.containsKey(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID)) {
			String pollingID = uninstallApplicationResponse.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);
			boolean waitForLifecycleEvents = this.adminFacade.waitForLifecycleEvents(pollingID, timeoutInMinutes);
			if (!waitForLifecycleEvents) {
				throw new TimeoutException(TIMEOUT_ERROR_MESSAGE);
			}
		} else {
			throw new CLIException("Failed to retrieve lifecycle logs from rest. " 
			+ "Check logs for more details.");
		}

		session.put(Constants.ACTIVE_APP, "default");
		GigaShellMain.getInstance().setCurrentApplicationName("default");
		return getFormattedMessage("application_uninstalled_succesfully", this.applicationName);
	}

	// returns true if the answer to the question was 'Yes'.
	/**
	 * Asks the user for confirmation to uninstall the application.
	 * 
	 * @return true if the user confirmed, false otherwise
	 * @throws IOException
	 *             Reporting a failure to get the user's confirmation
	 */
	private boolean askUninstallConfirmationQuestion() throws IOException {

		// we skip question if the shell is running a script.
		if ((Boolean) session.get(Constants.INTERACTIVE_MODE)) {
			final String confirmationQuestion = getFormattedMessage("application_uninstall_confirmation",
					applicationName);
			System.out.print(confirmationQuestion);
			System.out.flush();
			final PropertiesReader pr = new PropertiesReader(new InputStreamReader(System.in));
			String readLine = "";
			while (!readLine.equalsIgnoreCase("y") && !readLine.equalsIgnoreCase("n")) {
				readLine = pr.readProperty();
			}
			System.out.println();
			System.out.flush();
			return "y".equalsIgnoreCase(readLine);
		}
		// Shell is running in nonInteractive mode. we skip the question.
		return true;
	}
}
