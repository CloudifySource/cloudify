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
package org.cloudifysource.usm.stopDetection;

import java.util.List;
import java.util.logging.Level;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.USMUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.dsl.DSLConfiguration;
import org.cloudifysource.usm.events.AbstractUSMEventListener;

/***************
 * A stop detection implementation that checks if the monitored processes are still alive by checking their state using
 * the SIGAR library, using their PIDs.
 * 
 * @author barakme
 * 
 */
public class ProcessStopDetector extends AbstractUSMEventListener implements StopDetector {

	private boolean stopOnAllProcessesDead = true;

	@Override
	public void init(final UniversalServiceManagerBean usm) {
		super.init(usm);
		final String setting =
				((DSLConfiguration) usm.getUsmLifecycleBean().getConfiguration()).getService().getCustomProperties()
						.get(CloudifyConstants.CUSTOM_PROPERTY_STOP_DETECTION_ON_ALL_PROCESSES);
		if (setting != null) {
			this.stopOnAllProcessesDead = Boolean.parseBoolean(setting);
		}
	}

	private static final java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(ProcessStopDetector.class.getName());

	@Override
	public boolean isServiceStopped()
			throws USMException {

		final List<Long> pids = usm.getServiceProcessesList();

		// TODO - change to FINE
		if (logger.isLoggable(Level.INFO)) {
			logger.info("Process based stop detection is running. Scanning processes: " + pids);
		}

		// special handling for the 'recipe about nothing' scenario.
		if (pids.size() == 0) {
			return false;
		}
		if (stopOnAllProcessesDead) {
			return checkForAllProcessesDead(pids);
		} else {
			return checkForOneProcessDead(pids);
		}

	}

	private boolean checkForOneProcessDead(final List<Long> pids)
			throws USMException {
		for (final Long pid : pids) {
			final boolean processAlive = USMUtils.isProcessAlive(pid);

			if (!processAlive) {
				return true;
			}
		}

		return false;

	}

	private boolean checkForAllProcessesDead(final List<Long> pids)
			throws USMException {
		for (final Long pid : pids) {
			final boolean processAlive = USMUtils.isProcessAlive(pid);

			if (processAlive) {
				return false;
			}
		}

		return true;
	}

}
