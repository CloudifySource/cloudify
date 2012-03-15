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

public class RestLifecycleEventsLatch {

	private final long MIN_POLLING_INTERVAL = 2000;

	private long pollingInterval = MIN_POLLING_INTERVAL;

	private final String DEFAULT_TIMEOUT_MESSAGE = "installation timed out";

	private String timeoutMessage = DEFAULT_TIMEOUT_MESSAGE;

	private CLIEventsDisplayer displayer;

	public RestLifecycleEventsLatch(){
		this.displayer = new CLIEventsDisplayer();
	}

	public void waitForLifecycleEvents(final String serviceLifecycleEventContainerID,
			final GSRestClient client, int timeoutInMinutes) throws InterruptedException, TimeoutException, CLIException {
		createConditionLatch(timeoutInMinutes, TimeUnit.MINUTES).waitFor(new Predicate() {

			int cursor = 0;
			boolean isDone = false;
			boolean timedOutOnServer = false;
			String url;

			Map<String, Object> lifecycleEventLogs = null;

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

				if (events == null) {
					displayer.printNoChange();
				} else {
					displayer.printEvents(events);
			 	}
				
				if (isDone) {
					if (!timedOutOnServer) {
						displayer.eraseCurrentLine();
					} else {
						return false;
					}
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
		return new ConditionLatch().timeout(timeout, timeunit).pollingInterval(this.pollingInterval, TimeUnit.MILLISECONDS)
		.timeoutErrorMessage(this.timeoutMessage);
	}

	public void setPollingInterval(long pollingIntervalInMillis){

		if (!(pollingIntervalInMillis < MIN_POLLING_INTERVAL)){
			this.pollingInterval = pollingIntervalInMillis;
		}else{
			//TODO:logger: minimal interval set.
		}
	}

	public void setTimeoutMessage(String timeoutMessage) {
		this.timeoutMessage = timeoutMessage;

	}
}
