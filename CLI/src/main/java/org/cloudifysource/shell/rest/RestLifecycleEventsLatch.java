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
package org.cloudifysource.shell.rest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.restclient.ErrorStatusException;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ConditionLatch.Predicate;
import org.cloudifysource.shell.commands.CLIException;
import org.cloudifysource.shell.commands.CLIStatusException;
import org.cloudifysource.shell.installer.CLIEventsDisplayer;
/**
 * The RestLifecycleEventsLatch will poll the rest for installation lifecycle events 
 * and print the new events to the CLI console. 
 * The polling latch will stop polling the rest for three reasons:
 * 				* The timeout period expired.
 * 				* Installation on the remote rest gateway ended.
 * 				* An exception was thrown in the polling thread on remote server
 * 
 * @author adaml
 *
 */
public class RestLifecycleEventsLatch {

	private final long MIN_POLLING_INTERVAL = 2000;

	private long pollingInterval = MIN_POLLING_INTERVAL;

	private final String DEFAULT_TIMEOUT_MESSAGE = "installation timed out";

	private String timeoutMessage = DEFAULT_TIMEOUT_MESSAGE;

	private CLIEventsDisplayer displayer;
	
	/**
	 * Constructor.
	 */
	public RestLifecycleEventsLatch() {
		this.displayer = new CLIEventsDisplayer();
	}

	/**
	 * Waits for lifecycle events. This method will poll the rest for installation lifecycle events 
	 * and print the new events to the CLI console.
	 * 
	 * @param serviceLifecycleEventContainerID The polling ID.
	 * @param client The rest client.
	 * @param timeoutInMinutes Timeout for task. 
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws CLIException
	 */
	public void waitForLifecycleEvents(final String serviceLifecycleEventContainerID,
			final GSRestClient client, final int timeoutInMinutes) 
			throws InterruptedException, TimeoutException, CLIException {
		createConditionLatch(timeoutInMinutes, TimeUnit.MINUTES).waitFor(new Predicate() {

			private int cursor = 0;
			private boolean isDone = false;
			private boolean timedOutOnServer = false;
			private boolean exceptionOnServer = false;
			private String url;
			private Map<String, Object> lifecycleEventLogs = null;

			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				url = "/service/lifecycleEventContainerID/" + serviceLifecycleEventContainerID
				+ "/cursor/" + cursor;
				try {
					lifecycleEventLogs = (Map<String, Object>) client.get(url);
				} catch (final ErrorStatusException e) {
					throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
				} 

				List<String> events = (List<String>) lifecycleEventLogs.get(CloudifyConstants.LIFECYCLE_LOGS);
				cursor = (Integer) lifecycleEventLogs.get(CloudifyConstants.CURSOR_POS);
				isDone = (Boolean) lifecycleEventLogs.get(CloudifyConstants.IS_TASK_DONE);
				timedOutOnServer = (Boolean) lifecycleEventLogs.get(CloudifyConstants.POLLING_TIMEOUT_EXCEPTION);
				exceptionOnServer = (Boolean) lifecycleEventLogs.get(CloudifyConstants.POLLING_EXCEPTION);
				

				if (events == null) {
					displayer.printNoChange();
				} else {
					displayer.printEvents(events);
			 	}
				
				if (isDone) {
					if (timedOutOnServer) { 
				           return false;
					}
					if (exceptionOnServer) {
							throw new CLIException("Event polling failed on remote server." 
									+ "For more information regarding the installation, please refer to full logs");
					}
					displayer.eraseCurrentLine();
				}
				
				return isDone;
			}
			
		});
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
		return new ConditionLatch().timeout(timeout, timeunit)
		.pollingInterval(this.pollingInterval, TimeUnit.MILLISECONDS)
		.timeoutErrorMessage(this.timeoutMessage);
	}

	/**
	 * Sets the polling interval.
	 * 
	 * @param pollingIntervalInMillis Polling interval in milliseconds.
	 */
	public void setPollingInterval(final long pollingIntervalInMillis) {

		if (!(pollingIntervalInMillis < MIN_POLLING_INTERVAL)) {
			this.pollingInterval = pollingIntervalInMillis;
		} else {
			//TODO:logger: minimal interval set.
		}
	}

	/**
	 * Sets the timeout exception message.
	 * @param timeoutMessage The timeout exception message.
	 */
	public void setTimeoutMessage(final String timeoutMessage) {
		this.timeoutMessage = timeoutMessage;

	}
}
