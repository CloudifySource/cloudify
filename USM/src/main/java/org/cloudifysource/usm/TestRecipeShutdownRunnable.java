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

import java.util.logging.Level;

import org.cloudifysource.dsl.internal.CloudifyConstants.USMState;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/********
 * Shuts down the test USM container when the timeout expires. 
 * @author barakme
 * @since 2.0
 *
 */
class TestRecipeShutdownRunnable implements Runnable {

	private static final int EXIT_CODE_SHUTDOWN_WHILE_INITIALIZING = 101;
	private static final int FAILED_SHUTDOWN_EXIT_CODE = 100;
	private final UniversalServiceManagerBean usm;
	private final ApplicationContext applicationContext;

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(TestRecipeShutdownRunnable.class.getName());

	public TestRecipeShutdownRunnable(final ApplicationContext applicationContext,
			final UniversalServiceManagerBean usm) {
		this.usm = usm;
		this.applicationContext = applicationContext;
	}

	@Override
	public void run() {
		logger.info("Test Recipe automatic shutdown has started");
		final USMState state = usm.getState();
		if (state != USMState.RUNNING) {
			logger.warning("Test Recipe automatic shutdown has started, but the USM is in state: " + state.toString()
					+ ". Is the test timeout too short? Process will be shut down forcefully, "
					+ "and the service stop lifecycle will not be executed.");
			System.exit(EXIT_CODE_SHUTDOWN_WHILE_INITIALIZING);
		}
		boolean shutdownSuccess = true;
		try {

			if (applicationContext instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) applicationContext).close();
			} else {
				logger.warning("Test Recipe is shutting down but the application context is of type: "
						+ this.applicationContext.getClass().getName()
						+ " and does not extend AbstractApplicationContext. "
						+ "The application context will not be closed, only the USM will be shut down");
				usm.shutdown();
			}
		} catch (final Exception e) {
			shutdownSuccess = false;

			logger.log(Level.SEVERE, "Test Recipe automatic shutdown was invoked, but the USM shutdown failed", e);
		}

		logger.info("Test-Recipe shutdown completing witn shutdownSuccess == " + shutdownSuccess);
		
		if (shutdownSuccess) {
			logger.info("Exiting with status 0!");
			System.exit(0);
		} else {
			logger.info("Exiting with status 100!");
			System.exit(FAILED_SHUTDOWN_EXIT_CODE);
		}

	}

}