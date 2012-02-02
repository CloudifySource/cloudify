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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.util.Properties.PropertiesReader;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ConditionLatch.Predicate;
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
 *         progress - The polling time interval in seconds, used for checking if the operation is completed
 *         (default: 5).
 *  
 *        Command syntax: uninstall-application [-timeout timeout] [-progress progress] applicationName
 * 
 */
@Command(scope = "cloudify", name = "uninstall-application", description = "Uninstalls an application.")
public class UninstallApplication extends AdminAwareCommand {

	private static final int UNINSTALL_POOLING_INTERVAL = 5;

	private static final String TIMEOUT_ERROR_MESSAGE = "Timeout waiting for application to uninstall";

	private String lastMessage;

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

		if (timeoutInMinutes > 0) {
			printStatusMessage(containerIdsOfApplication.size(), containerIdsOfApplication.size(),
					containerIdsOfApplication);
		}
		this.adminFacade.uninstallApplication(this.applicationName);

		if (timeoutInMinutes > 0) {
			createConditionLatch(timeoutInMinutes, TimeUnit.MINUTES).waitFor(new Predicate() {

				/**
				 * {@inheritDoc}
				 */
				@Override
				public boolean isDone() throws CLIException {

					final Set<String> allContainerIds = ((RestAdminFacade) adminFacade).getGridServiceContainerUids();
					final Set<String> remainingContainersForApplication = new HashSet<String>(
							containerIdsOfApplication);
					remainingContainersForApplication.retainAll(allContainerIds);
					final boolean isDone = remainingContainersForApplication.isEmpty();
					if (!isDone) {
						printStatusMessage(remainingContainersForApplication.size(), containerIdsOfApplication.size(),
								remainingContainersForApplication);
					}
					// TODO: container has already been removed by un-install.
					// printAllServiceEvents();
					return isDone;
				}

			});
		}
		session.put(Constants.ACTIVE_APP, "default");
		GigaShellMain.getInstance().setCurrentApplicationName("default");
		return getFormattedMessage("application_uninstalled_succesfully", this.applicationName);
	}

	/**
	 * Logs a status message, if not identical to the last message written to the log.
	 * 
	 * @param remainingApplicationContainers
	 *            The number of application containers still running
	 * @param allApplicationContainers
	 *            Total number of application container
	 * @param remainingContainerIDs
	 *            The IDs of the application containers that are still running
	 */
	private void printStatusMessage(final int remainingApplicationContainers, final int allApplicationContainers,
			final Set<String> remainingContainerIDs) {
		final String message = "Waiting for all service instances to uninstall. " + "Currently "
				+ remainingApplicationContainers + " instances of " + allApplicationContainers + " are still running.";

		if (!StringUtils.equals(message, lastMessage)) {
			logger.info(message + (verbose ? " " + remainingContainerIDs : ""));
			this.lastMessage = message;
		}
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
			final String readLine = pr.readProperty();
			System.out.println();
			System.out.flush();
			return "y".equalsIgnoreCase(readLine) ? true : false;

		}
		// Shell is running in nonInteractive mode. we skip the question.
		return true;
	}

	// private void printAllServiceEvents() throws CLIException {
	// List<String> serviceNames = adminFacade.getServicesList(applicationName);
	// for (String serviceName : serviceNames) {
	// adminFacade.printEventLogs(applicationName, serviceName);
	// }
	// }

	/**
	 * Creates a condition latch object with the specified timeout. If the condition times out, a
	 * {@link TimeoutException} is thrown.
	 * 
	 * @param timeout
	 *            number of timeunits to wait
	 * @param timeunit
	 *            type of timeunits to wait
	 * @return Configured condition latch
	 */
	private ConditionLatch createConditionLatch(final long timeout, final TimeUnit timeunit) {
		return new ConditionLatch().timeout(timeout, timeunit).pollingInterval(UNINSTALL_POOLING_INTERVAL, TimeUnit.SECONDS)
				.timeoutErrorMessage(TIMEOUT_ERROR_MESSAGE).verbose(verbose);
	}

}
