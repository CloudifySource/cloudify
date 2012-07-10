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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.restclient.ErrorStatusException;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.shell.ConditionLatch;
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

	private String pollingID;

	private GSRestClient client;

	private long endTime;
	
	int cursor = 0;
	
	boolean isDone = false;
	
	boolean exceptionOnServer = false;
	
	String url;
	
	Map<String, Object> lifecycleEventLogs = null;

	private long remoteTaskLeaseExpiration;
	
	private static final Logger logger = Logger.getLogger(RestLifecycleEventsLatch.class.getName());
	
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
	 * @param timeout
	 * @param timeUnit
	 * @return
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws CLIException
	 */
	public boolean waitForLifecycleEvents(final int timeout, TimeUnit timeUnit) 
			throws InterruptedException, TimeoutException, CLIException {

		this.endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
		
		while (System.currentTimeMillis() < this.endTime) {
			url = "/service/lifecycleEventContainerID/" + pollingID
					+ "/cursor/" + cursor;
			try {
				lifecycleEventLogs = (Map<String, Object>) client.get(url);
			} catch (final ErrorStatusException e) {
				throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
			}

			List<String> events = (List<String>) lifecycleEventLogs.get(CloudifyConstants.LIFECYCLE_LOGS);
			this.cursor = (Integer) lifecycleEventLogs.get(CloudifyConstants.CURSOR_POS);
			this.isDone = (Boolean) lifecycleEventLogs.get(CloudifyConstants.IS_TASK_DONE);
			this.exceptionOnServer = (Boolean) lifecycleEventLogs.get(CloudifyConstants.POLLING_EXCEPTION);
			this.remoteTaskLeaseExpiration = Long.valueOf((String) lifecycleEventLogs.
					get(CloudifyConstants.SERVER_POLLING_TASK_EXPIRATION_MILLI)) + System.currentTimeMillis();

			if (events == null) {
				displayer.printNoChange();
			} else {
				displayer.printEvents(events);
			}

			if (isDone) {
				if (exceptionOnServer) {
					throw new CLIException("Event polling task failed on remote server." 
							+ "For more information regarding the installation, please refer to full logs");
				}
				displayer.eraseCurrentLine();
				return true;
			}
			Thread.sleep(pollingInterval);
		}
		return false;
	}
	
	/**
	 * Continue an already started polling task. Used for when polling was interrupted on the client side.
	 * 
	 * @param timeout
	 * @param timeUnit
	 * @return
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws CLIException if the polling task has expired on the remote server side
	 */
	public boolean continueWaitForLifecycleEvents(final int timeout, final TimeUnit timeUnit) 
			throws InterruptedException, TimeoutException, CLIException {
		if (System.currentTimeMillis() > this.remoteTaskLeaseExpiration) {
			throw new CLIException("Events polling task has expired on remote server side");
		}
		return waitForLifecycleEvents(timeout, timeUnit);
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
	public void setPollingInterval(final long pollingInterval, TimeUnit timeUnit) {
		long pollingIntervalInMillis = timeUnit.toMillis(pollingInterval);
		if (!(pollingIntervalInMillis < MIN_POLLING_INTERVAL)) {
			this.pollingInterval = pollingIntervalInMillis;
		} else {
			logger.log(Level.INFO, 
					"Polling interveal was set to the minimum polling" +
							" interval allowed: " + MIN_POLLING_INTERVAL + "seconds");
		}
	}
	
	public void setPollingId(String pollingID) {
		this.pollingID = pollingID;
	}
	
	public void setRestClient(final GSRestClient client) {
		this.client = client;
	}

//	/**
//	 * Sets the timeout exception message.
//	 * @param timeoutMessage The timeout exception message.
//	 */
//	public void setTimeoutMessage(final String timeoutMessage) {
//		this.timeoutMessage = timeoutMessage;
//
//	}
}
