/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

import org.apache.commons.exec.ExecuteWatchdog;

/**********
 * Wraps the standard commons-exec watchdog and adds a timeout logging message.
 * 
 * @author barakme
 * 
 */
public class TestRecipeWatchdog extends ExecuteWatchdog {

	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TestRecipeWatchdog.class
			.getName());

	/*************
	 * Constructor.
	 * 
	 * @param timeout .
	 */
	public TestRecipeWatchdog(final long timeout) {
		super(timeout);
	}

	@Override
	public synchronized void timeoutOccured(final org.apache.commons.exec.Watchdog arg0) {
		logger.severe("The test recipe command has exceeded its timeout! "
				+ "The process watchdog is about to kill the test container.");
		super.timeoutOccured(arg0);

	}

}
