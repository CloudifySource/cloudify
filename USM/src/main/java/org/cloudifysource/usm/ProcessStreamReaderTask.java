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
package org.cloudifysource.usm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class ProcessStreamReaderTask implements Runnable {

	private static java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(ProcessStreamReaderTask.class.getName());
	
	private java.util.logging.Logger streamLogger;
	private ProcessDeathNotifier notifier;

	private volatile boolean enabled = true;

	private Level logLevel;

	private InputStream inputStream;

	private String loggerName;
	public ProcessStreamReaderTask(InputStream inputStream, ProcessDeathNotifier notifier, Level logLevel, String loggerName) {
		super();
		this.inputStream = inputStream;
		this.notifier = notifier;
		this.logLevel = logLevel;
		this.streamLogger = java.util.logging.Logger.getLogger(loggerName);
		this.loggerName = loggerName;
	}

	
	public void run() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		try {
			while (enabled) {
				String line = reader.readLine();
				if(line != null) {
					streamLogger.log(this.logLevel, line);
				} else {
					logger.warning("Input Stream " + loggerName + " has shut down! The monitored process has stopped!");
					// process failure!
					if (notifier != null){
						notifier.processDeathDetected();
					}
					return;
				}

			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

	}

	public boolean isEnabled() {
		return enabled;
	}

	public void disable() {
		this.enabled = false;		
	}

}
