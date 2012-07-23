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
package org.cloudifysource.usm.shutdown;

import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.USMUtils;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.gigaspaces.internal.sigar.SigarHolder;

/**********
 * The default process killer implementation, which uses SIGAR to kill processes.
 * 
 * @author barakme
 * 
 */
public class DefaultProcessKiller implements ProcessKiller {

	private static final int POST_KILL_SLEEP_INTERVAL = 100;
	private static final int PROCESS_STATUS_CHECK_INTERVAL = 200;
	private static final int PROCESS_STATUS_CHECK_ATTEMPTS = 5;

	private static java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(DefaultProcessKiller.class.getName());

	private int killRetries = 2;

	@Override
	public void killProcess(final long pid)
			throws USMException {
		if (pid == 0) {
			return; // this is possible in some end case situations, in the
					// IntegratedProcessingUnitContainer
		}
		final Sigar sigar = SigarHolder.getSigar();

		for (int retries = 0; retries <= killRetries; ++retries) {
			try {
				logger.info("Killing process: " + pid);
				sigar.kill(pid, "SIGTERM"); // (9 is the only signal used for
											// Kill on windows)
			} catch (final SigarException e) {
				logger.warning("Failed to shut down process: " + pid + ". Process may already be dead. Error was: "
						+ e.getMessage() + ".");
			}

			try {
				// sleep for a short period so process table get cleaned up
				Thread.sleep(POST_KILL_SLEEP_INTERVAL);
			} catch (final InterruptedException e1) {
				// ignore
			}

			// wait until process dies
			for (int i = 0; i < PROCESS_STATUS_CHECK_ATTEMPTS; ++i) {
				if (!USMUtils.isProcessAlive(pid)) {
					logger.info("Process " + pid + " is dead");
					return;
				}
				try {
					Thread.sleep(PROCESS_STATUS_CHECK_INTERVAL);
				} catch (final InterruptedException e) {
					// ignore
				}
			}
			logger.warning("Attempt number " + (retries + 1) + " to kill process " + pid + " failed.");
		}

		logger.severe("Process " + pid + " did not die as expected!");

		throw new USMException("Attempt to kill process " + pid + " failed!");

	}

	public int getKillRetries() {
		return killRetries;
	}

	public void setKillRetries(final int killRetries) {
		this.killRetries = killRetries;
	}
}
