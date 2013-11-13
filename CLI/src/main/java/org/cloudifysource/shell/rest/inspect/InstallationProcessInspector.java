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
package org.cloudifysource.shell.rest.inspect;

import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA. User: elip Date: 5/29/13 Time: 1:50 PM <br>
 * </br>
 *
 * Provides functionality for inspecting the installation process of services/application.
 *
 */
public abstract class InstallationProcessInspector {

    protected Logger logger = Logger.getLogger(InstallationProcessInspector.class.getName());

    private static final int POLLING_INTERVAL_MILLI_SECONDS = 500;
	protected static final int RESOURCE_NOT_FOUND_EXCEPTION_CODE = 404;

	protected RestClient restClient;
	private final boolean verbose;
	protected final String deploymentId;
	protected final String applicationName;
	protected final Map<String, Integer> plannedNumberOfInstancesPerService;
	protected final Map<String, Integer> currentRunningInstancesPerService;

	private int lastEventIndex = 0;
	private final CLIEventsDisplayer displayer = new CLIEventsDisplayer();

	public InstallationProcessInspector(final RestClient restClient,
			final String deploymentId,
			final String applicationName,
			final boolean verbose,
			final Map<String, Integer> plannedNumberOfInstancesPerService,
			final Map<String, Integer> currentRunningInstancesPerService) {
		this.restClient = restClient;
		this.deploymentId = deploymentId;
		this.applicationName = applicationName;
		this.verbose = verbose;
		this.plannedNumberOfInstancesPerService = plannedNumberOfInstancesPerService;
		this.currentRunningInstancesPerService = currentRunningInstancesPerService;
	}

	/**
	 * Waits until the application/service lifecycle ends. As long as the installation continues, it will print out the
	 * most recent events not yet printed.
	 *
	 * @param timeout
	 *            the timeout.
	 * @throws InterruptedException
	 *             Thrown in case the thread was interrupted.
	 * @throws CLIException
	 *             Thrown in case an error happened while trying to retrieve events.
	 * @throws TimeoutException
	 *             Thrown in case the timeout is reached.
	 */
	public void waitForLifeCycleToEnd(final long timeout) throws InterruptedException, CLIException, TimeoutException {
		ConditionLatch conditionLatch = createConditionLatch(timeout);

		conditionLatch.waitFor(new ConditionLatch.Predicate() {


			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				try {
					printInstalledInstances();
					boolean ended = lifeCycleEnded();

					List<String> latestEvents = getLatestEvents();
					if (!latestEvents.isEmpty()) {
						displayer.printEvents(latestEvents);
					} else {
						if (!ended) {
							displayer.printNoChange();
						}
					}

					if (ended) {
						printInstalledInstances();
					}
					return ended;
				} catch (final RestClientException e) {
					throw new CLIException(e.getMessage(), e, e.getVerbose());
				}
			}

			private void printInstalledInstances() throws RestClientException {
				for (Map.Entry<String, Integer> entry : plannedNumberOfInstancesPerService.entrySet()) {
					int runningInstances = getNumberOfRunningInstances(entry.getKey());
					if (runningInstances > currentRunningInstancesPerService.get(entry.getKey())) {
						// a new instance is now running
						displayer.printEvent("succesfully_installed_instances", runningInstances,
								entry.getValue(), entry.getKey());
						currentRunningInstancesPerService.put(entry.getKey(), runningInstances);
					}
				}
			}
		});
	}

	/**
	 * Determines whether or not the life cycle for this installation has ended.
	 *
	 * @return true if the service/application are fully running.
	 * @throws RestClientException
	 *             Thrown in case an error happened during a rest call.
     * @throws CLIException
     *             Thrown in case the CLI determined that some sort error happened.
	 */
	public abstract boolean lifeCycleEnded() throws RestClientException, CLIException;

	/**
	 * Query the number of running instances for a particular service.
	 *
	 * @param serviceName
	 *            The service name.
	 * @return how many instances are in running state.
	 * @throws RestClientException
	 *             Thrown in case an error happened during a rest call.
	 */
	public abstract int getNumberOfRunningInstances(final String serviceName) throws RestClientException;

	/**
	 *
	 * @return the error message presented upon timeout.
	 */
	public abstract String getTimeoutErrorMessage();

	/**
	 * Gets the latest events of this deployment id. Events are sorted by event index.
	 *
	 * @return A list of events. If this is the first time events are requested, all events are retrieved. Otherwise,
	 *         only new events (that were not reported earlier) are retrieved.
	 * @throws RestClientException
	 *             Indicates a failure to get events from the server.
	 */
	public List<String> getLatestEvents() throws RestClientException {

		List<String> eventsStrings = new ArrayList<String>();

		DeploymentEvents events = restClient.getDeploymentEvents(deploymentId, lastEventIndex + 1, -1);
		if (events == null || events.getEvents().isEmpty()) {
			return eventsStrings;
		}

		for (DeploymentEvent event : events.getEvents()) {
			eventsStrings.add(event.getDescription());
		}
		lastEventIndex = events.getEvents().get(events.getEvents().size() - 1).getIndex();
		return eventsStrings;
	}

	/**
	 * Creates a {@link ConditionLatch} object with the given timeout (in minutes), using a polling interval of 500 ms.
	 *
	 * @param timeout
	 *            Timeout, in minutes.
	 * @return a configured {@link ConditionLatch} object
	 */
	public ConditionLatch createConditionLatch(final long timeout) {
		return new ConditionLatch()
				.verbose(verbose)
				.pollingInterval(POLLING_INTERVAL_MILLI_SECONDS, TimeUnit.MILLISECONDS)
				.timeout(timeout, TimeUnit.MINUTES)
				.timeoutErrorMessage(getTimeoutErrorMessage());
	}

	public void setLastEventIndex(final int eventIndex) {
		this.lastEventIndex = eventIndex;
	}
}
