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
@Command(scope = "grid", name = "uninstall-service", description = "undeploy a service")
public class UninstallService extends AdminAwareCommand {

	private static final int UNINSTALL_POOLING_INTERVAL = 5;

	private static final String DEFAULT_APPLICATION_NAME = "default";

	private static final String TIMEOUT_ERROR_MESSAGE = "Timeout waiting for service to uninstall";

	private String lastMessage;

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
			return getRestAdminFacade().getServicesList(DEFAULT_APPLICATION_NAME);
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
		if (timeoutInMinutes > 0) {
			printStatusMessage(containerIdsOfService.size(), containerIdsOfService.size(), containerIdsOfService);
		}
		adminFacade.undeploy(getCurrentApplicationName(), serviceName);
		if (timeoutInMinutes > 0) {
			createConditionLatch(timeoutInMinutes, TimeUnit.MINUTES).waitFor(new Predicate() {
				/**
				 * {@inheritDoc}
				 */
				@Override
				public boolean isDone() throws CLIException {
					final Set<String> allContainerIds = ((RestAdminFacade) adminFacade).getGridServiceContainerUids();
					final Set<String> remainingContainersForService = new HashSet<String>(containerIdsOfService);
					remainingContainersForService.retainAll(allContainerIds);

					final boolean isDone = remainingContainersForService.isEmpty();

					if (!isDone) {
						printStatusMessage(remainingContainersForService.size(), containerIdsOfService.size(),
								remainingContainersForService);
					}
					// TODO: container has already been removed by uninstall.
					// adminFacade.printEventLogs(getCurrentApplicationName(), serviceName);
					return isDone;
				}
			});
		}
		return getFormattedMessage("undeployed_successfully", serviceName);
	}

	/**
	 * Logs a status message, if not identical to the last message written to the log.
	 * 
	 * @param remainingServiceContainers
	 *            The number of service containers still running
	 * @param allServiceContainers
	 *            Total number of service container
	 * @param remainingContainerIDs
	 *            The IDs of the service containers that are still running
	 */
	private void printStatusMessage(final int remainingServiceContainers, final int allServiceContainers,
			final Set<String> remainingContainerIDs) {
		final String message = "Waiting for all service instances to uninstall. " + "Currently "
				+ remainingServiceContainers + " instances of " + allServiceContainers + " are still running.";

		if (!StringUtils.equals(message, lastMessage)) {
			logger.info(message + (verbose ? " " + remainingContainerIDs : ""));
			this.lastMessage = message;
		}

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
