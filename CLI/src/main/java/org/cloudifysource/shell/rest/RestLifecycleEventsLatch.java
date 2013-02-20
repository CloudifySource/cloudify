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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.restclient.ErrorStatusException;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.shell.ConditionLatch;
import org.cloudifysource.shell.ConditionLatch.Predicate;
import org.cloudifysource.shell.commands.CLIException;
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

	private static final Logger logger = Logger.getLogger(RestLifecycleEventsLatch.class.getName());
	private static final long MIN_POLLING_INTERVAL = 2000;
	private static final String DEFAULT_TIMEOUT_MESSAGE = "installation timed out";

	private long pollingInterval = MIN_POLLING_INTERVAL;
	private String timeoutMessage = DEFAULT_TIMEOUT_MESSAGE;
	private final CLIEventsDisplayer displayer;
	private String pollingID;
	private GSRestClient client;
	private int cursor = 0;
	private boolean isDone = false;
	private String url;
	private Map<String, Object> lifecycleEventLogs = null;
	private long remoteTaskLeaseExpiration;

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
	 * @param timeout units to wait.
	 * @param timeUnit unit type.
	 * @throws InterruptedException .
	 * @throws TimeoutException .
	 * @throws CLIException .
	 */
	public void waitForLifecycleEvents(final int timeout, final TimeUnit timeUnit) 
			throws InterruptedException, TimeoutException, CLIException {
		createConditionLatch(timeout, TimeUnit.MINUTES).waitFor(new Predicate() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean isDone() throws CLIException, InterruptedException {
				url = "/service/lifecycleEventContainerID/" + pollingID
						+ "/cursor/" + cursor;
				try {
					lifecycleEventLogs = (Map<String, Object>) client.get(url);
				} catch (final ErrorStatusException e) {
					if (e.getCause() instanceof IOException) {
						displayer.printEvent("Communication Error accessing "+ url); 
						return false;
					} 
					throw new CLIException("Operation failed. Reason: " + e.getMessage(), e);
				}

				List<String> events = (List<String>) lifecycleEventLogs.get(CloudifyConstants.LIFECYCLE_LOGS);
				cursor = (Integer) lifecycleEventLogs.get(CloudifyConstants.CURSOR_POS);
				isDone = (Boolean) lifecycleEventLogs.get(CloudifyConstants.IS_TASK_DONE);
				remoteTaskLeaseExpiration = Long.valueOf((String) lifecycleEventLogs.
						get(CloudifyConstants.SERVER_POLLING_TASK_EXPIRATION_MILLI)) + System.currentTimeMillis();
				
				if (System.currentTimeMillis() > remoteTaskLeaseExpiration) {
					throw new CLIException("Events polling task has expired on remote server side");
				}

				if (events == null) {
					displayer.printNoChange();
				} else {
					displayer.printEvents(events);
				}

				if (isDone) {
					displayer.eraseCurrentLine();
					return true;
				}
				return false;
			}
		});
	}

	/**
	 * Continue an already started polling task. Used for when polling was interrupted on the client side.
	 * 
	 * @param timeout .
	 * @param timeUnit .
	 * @throws InterruptedException .
	 * @throws TimeoutException .
	 * @throws CLIException if the polling task has expired on the remote server side
	 */
	public void continueWaitForLifecycleEvents(final int timeout, final TimeUnit timeUnit) 
			throws InterruptedException, TimeoutException, CLIException {
		if (System.currentTimeMillis() > this.remoteTaskLeaseExpiration) {
			throw new CLIException("Events polling task has expired on remote server side");
		}
		waitForLifecycleEvents(timeout, timeUnit);
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
	 * @param pollingInterval 
	 * 			Polling interval.
	 * @param timeUnit 
	 * 			The polling interval time unit.
	 */
	public void setPollingInterval(final long pollingInterval, final TimeUnit timeUnit) {
		long pollingIntervalInMillis = timeUnit.toMillis(pollingInterval);
		if (!(pollingIntervalInMillis < MIN_POLLING_INTERVAL)) {
			this.pollingInterval = pollingIntervalInMillis;
		} else {
			logger.log(Level.INFO, 
					"Polling interveal was set to the minimum polling" 
						+ " interval allowed: " + MIN_POLLING_INTERVAL + "seconds");
		}
	}

	public void setPollingId(final String pollingID) {
		this.pollingID = pollingID;
	}

	public void setRestClient(final GSRestClient client) {
		this.client = client;
	}

	/**
	 * Sets the timeout exception message.
	 * @param timeoutMessage The timeout exception message.
	 */
	public void setTimeoutMessage(final String timeoutMessage) {
		this.timeoutMessage = timeoutMessage;

	}
}
