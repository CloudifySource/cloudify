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
import java.util.concurrent.TimeoutException;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.restclient.ErrorStatusException;
import org.cloudifysource.restclient.GSRestClient;
import org.cloudifysource.shell.ShellUtils;
import org.cloudifysource.shell.commands.CLIStatusException;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

public class RestLifecycleEventsLatch {

	private final long MIN_POLLING_INTERVAL = 2000;
	
	private long pollingInterval = MIN_POLLING_INTERVAL;

	private int progressCounter = 0;

	private final int PROGRESS_BAR_MAX_LENGTH = 6;
	
	private final String DEFAULT_TIMEOUT_MESSAGE = "installation timed out";

	private String timeoutMessage = DEFAULT_TIMEOUT_MESSAGE;


	public void waitForLifecycleEvents(String serviceLifecycleEventContainerID,
			GSRestClient client) throws InterruptedException, TimeoutException, CLIStatusException {

		int cursor = 0;
		boolean isDone = false;
		String url;
		Boolean timedOut = false;

		Map<String, Object> lifecycleEventLogs = null;
		while(!isDone){
			url = "/service/lifecycleEventContainerID/" + serviceLifecycleEventContainerID
			+ "/cursor/" + cursor;
			try {
				lifecycleEventLogs = (Map<String, Object>) client.get(url);
			} catch (final ErrorStatusException e) {
				throw new CLIStatusException(e, e.getReasonCode(), e.getArgs());
			} 

			List<String> events = (List<String>)lifecycleEventLogs.get(CloudifyConstants.LIFECYCLE_LOGS);
			cursor = (Integer)lifecycleEventLogs.get(CloudifyConstants.CURSOR_POS);
			isDone = (Boolean)lifecycleEventLogs.get(CloudifyConstants.IS_TASK_DONE);
			timedOut = (Boolean)lifecycleEventLogs.get(CloudifyConstants.POLLING_TIMEOUT_EXCEPTION);

			printProgressEventMessages(events);

			if (timedOut){
				throw new TimeoutException(this.timeoutMessage);
			}
			Thread.sleep(pollingInterval);
		}
		System.out.print(Ansi.ansi().cursorLeft(this.progressCounter).eraseLine());
	}
	private void printProgressEventMessages(final List<String> events) {
		if (events == null){
			System.out.print('.');
			System.out.flush();
			this.progressCounter++;
			if (progressCounter >= PROGRESS_BAR_MAX_LENGTH){
				System.out.print(Ansi.ansi()
						.cursorLeft(PROGRESS_BAR_MAX_LENGTH - 1)
						.eraseLine());
				System.out.flush();
				this.progressCounter = 1;
			}
			return;
		}
		if (events.size() != 0) {
			System.out.println('.');
			System.out.flush();
			this.progressCounter = 0;
		}
		for (final String eventString : events) {
			if (eventString.contains(CloudifyConstants.USM_EVENT_EXEC_SUCCESSFULLY)) {
				System.out.println(eventString + " "
						+ ShellUtils.getColorMessage(CloudifyConstants.USM_EVENT_EXEC_SUCCEED_MESSAGE, Color.GREEN));
			} else if (eventString.contains(CloudifyConstants.USM_EVENT_EXEC_FAILED)) {
				System.out.println(eventString + " "
						+ ShellUtils.getColorMessage(CloudifyConstants.USM_EVENT_EXEC_FAILED_MESSAGE, Color.RED));
			} else {
				System.out.println(eventString);
			}
		}
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
