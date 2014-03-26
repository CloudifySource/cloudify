/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.request.SetServiceInstancesRequest;
import org.cloudifysource.dsl.rest.response.ServiceDescription;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
import org.cloudifysource.shell.rest.RestAdminFacade;
import org.cloudifysource.shell.rest.RestLifecycleEventsLatch;
import org.cloudifysource.shell.rest.inspect.service.SetInstancesScaledownInstallationProcessInspector;
import org.cloudifysource.shell.rest.inspect.service.SetInstancesScaleupInstallationProcessInspector;
import org.fusesource.jansi.Ansi.Color;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/************
 * Manually sets the number of instances for a specific service.
 * 
 * @author barakme
 * 
 */
@Command(scope = "cloudify", name = "set-instances", description = "Sets the number of services of an elastic service")
public class SetInstances extends AdminAwareCommand implements NewRestClientCommand {

	private static final int DEFAULT_TIMEOUT_MINUTES = 10;

	@Argument(index = 0, name = "service-name", required = true, description = "the service to scale")
	private String serviceName;

	@Argument(index = 1, name = "count", required = true, description = "the target number of instances")
	private int count;

	@Option(required = false, name = "-timeout",
			description = "number of minutes to wait for instances. Default is set to 10 minutes")
	protected int timeout = DEFAULT_TIMEOUT_MINUTES;

	// NOTE: This flag has been disabled as manual scaling is not supported with location aware services in Cloudify
	// 2.2.
	// This issue will be revisited in the future.
	// @Option(required = false, name = "-location-aware", description =
	// "When true re-starts failed machines in the same cloud location. Default is set to false.")
	private boolean locationAware = false;

	private static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out. "
			+ "Try to increase the timeout using the -timeout flag";

	private final CLIEventsDisplayer displayer = new CLIEventsDisplayer();
	
	@Override
	protected Object doExecute() throws Exception {
		String applicationName = this.getCurrentApplicationName();
		if (applicationName == null) {
			applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;
		}

		final int initialNumberOfInstances = adminFacade.getInstanceList(applicationName, serviceName).size();
		if (initialNumberOfInstances == count) {
			return getFormattedMessage("num_instances_already_met", count);
		}

		final Map<String, String> response =
				adminFacade.setInstances(applicationName, serviceName, count, isLocationAware(), timeout);

		final String pollingID = response.get(CloudifyConstants.LIFECYCLE_EVENT_CONTAINER_ID);
		final RestLifecycleEventsLatch lifecycleEventsPollingLatch =
				this.adminFacade.getLifecycleEventsPollingLatch(pollingID, TIMEOUT_ERROR_MESSAGE);
		boolean isDone = false;
		boolean continuous = false;
		while (!isDone) {
			try {
				if (!continuous) {
					lifecycleEventsPollingLatch.waitForLifecycleEvents(timeout, TimeUnit.MINUTES);
				} else {
					lifecycleEventsPollingLatch.continueWaitForLifecycleEvents(timeout, TimeUnit.MINUTES);
				}
				isDone = true;
			} catch (final TimeoutException e) {
				if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
					throw e;
				}
				final boolean continueInstallation = promptWouldYouLikeToContinueQuestion();
				if (!continueInstallation) {
					throw new CLIStatusException(e, "application_installation_timed_out_on_client",
							applicationName);
				}
				continuous = continueInstallation;
			}
		}

		return getFormattedMessage("set_instances_completed_successfully", Color.GREEN, serviceName, count);
	}

	private boolean promptWouldYouLikeToContinueQuestion() throws IOException {
		return ShellUtils.promptUser(session, "would_you_like_to_continue_polling_on_instance_lifecycle_events");
	}

	public boolean isLocationAware() {
		return locationAware;
	}

	public void setLocationAware(final boolean locationAware) {
		this.locationAware = locationAware;
	}

	@Override
	public Object doExecuteNewRestClient() throws Exception {

		final RestClient newRestClient = ((RestAdminFacade) getRestAdminFacade()).getNewRestClient();

		final String applicationName = resolveApplicationName();

		final ServiceDescription serviceDescription = newRestClient.getServiceDescription(applicationName, serviceName);

		final String deploymentId = serviceDescription.getDeploymentId();

		final int initialNumberOfInstances = serviceDescription.getPlannedInstances();
		if (initialNumberOfInstances == count) {
			return getFormattedMessage("num_instances_already_met", count);
		}

		final int lastEventIndex = newRestClient.getLastEvent(deploymentId).getIndex();

		final SetServiceInstancesRequest request = new SetServiceInstancesRequest();
		request.setCount(count);
		request.setLocationAware(false);
		request.setTimeout(this.timeout);
		// REST API call to server
		newRestClient.setServiceInstances(applicationName, serviceName, request);

		if (count > initialNumberOfInstances) {

			return waitForScaleOut(deploymentId, count, lastEventIndex,
					initialNumberOfInstances);
		}
		return waitForScaleIn(deploymentId, count, lastEventIndex, initialNumberOfInstances);

	}

	private String resolveApplicationName() {
		String applicationName = this.getCurrentApplicationName();
		if (applicationName == null) {
			applicationName = CloudifyConstants.DEFAULT_APPLICATION_NAME;
		}
		return applicationName;
	}

	private String waitForScaleOut(final String deploymentID, final int plannedNumberOfInstnaces,
			final int lastEventIndex, final int currentNumberOfInstances) throws InterruptedException,
			CLIException, IOException {
		final SetInstancesScaleupInstallationProcessInspector inspector =
				new SetInstancesScaleupInstallationProcessInspector(
						((RestAdminFacade) adminFacade).getNewRestClient(),
						deploymentID,
						verbose,
						serviceName,
						plannedNumberOfInstnaces,
						getCurrentApplicationName(),
						lastEventIndex,
						currentNumberOfInstances);

		int actualTimeout = this.timeout;
		boolean isDone = false;
		displayer.printEvent("installing_service", serviceName, plannedNumberOfInstnaces);
		displayer.printEvent("waiting_for_lifecycle_of_service", serviceName);
		while (!isDone) {
			try {

				inspector.waitForLifeCycleToEnd(actualTimeout);
				isDone = true;

			} catch (final TimeoutException e) {

				// if non interactive, throw exception
				if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
					throw new CLIException(e.getMessage(), e);
				}

				// ask the user whether to continue viewing the installation or to stop
				displayer.printEvent("");
				final boolean continueViewing = promptWouldYouLikeToContinueQuestion();
				if (continueViewing) {
					// prolong the polling timeouts
					actualTimeout = DEFAULT_TIMEOUT_MINUTES;
				} else {
					throw new CLIStatusException(e,
							"service_installation_timed_out_on_client",
							serviceName);
				}
			}
		}

		// drop one line before printing the last message
		displayer.printEvent("");
		return getFormattedMessage("service_install_ended", Color.GREEN, serviceName);

	}

	private String waitForScaleIn(
			final String deploymentID,
			final int plannedNumberOfInstnaces,
			final int lastEventIndex,
			final int currentNumberOfInstances)
			throws InterruptedException, CLIException, IOException {
		final SetInstancesScaledownInstallationProcessInspector inspector =
				new SetInstancesScaledownInstallationProcessInspector(
						((RestAdminFacade) adminFacade).getNewRestClient(),
						deploymentID,
						verbose,
						serviceName,
						plannedNumberOfInstnaces,
						getCurrentApplicationName(),
						lastEventIndex,
						currentNumberOfInstances);

		int actualTimeout = this.timeout;
		boolean isDone = false;
		displayer.printEvent("installing_service", serviceName, plannedNumberOfInstnaces);
		displayer.printEvent("waiting_for_lifecycle_of_service", serviceName);
		while (!isDone) {
			try {

				inspector.waitForLifeCycleToEnd(actualTimeout);
				isDone = true;

			} catch (final TimeoutException e) {

				// if non interactive, throw exception
				if (!(Boolean) session.get(Constants.INTERACTIVE_MODE)) {
					throw new CLIException(e.getMessage(), e);
				}

				// ask the user whether to continue viewing the installation or to stop
				displayer.printEvent("");
				final boolean continueViewing = promptWouldYouLikeToContinueQuestion();
				if (continueViewing) {
					// prolong the polling timeouts
					actualTimeout = DEFAULT_TIMEOUT_MINUTES;
				} else {
					throw new CLIStatusException(e,
							"service_installation_timed_out_on_client",
							serviceName);
				}
			}
		}

		// drop one line before printing the last message
		displayer.printEvent("");
		return getFormattedMessage("service_install_ended", Color.GREEN, serviceName);

	}

}
