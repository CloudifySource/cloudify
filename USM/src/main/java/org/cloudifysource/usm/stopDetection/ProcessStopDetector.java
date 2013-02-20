/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.usm.stopDetection;

import java.util.List;
import java.util.logging.Level;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.events.AbstractUSMEventListener;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

/***************
 * A stop detection implementation that checks if the monitored processes are still alive by checking their state using
 * the SIGAR library, using their PIDs.
 *
 * @author barakme
 *
 */
public class ProcessStopDetector extends AbstractUSMEventListener implements StopDetector {

	private boolean stopOnAllProcessesDead = true;
	private Sigar sigarInstance = null;
	private long sigarCreationTime = System.currentTimeMillis();
	private static final long SIGAR_RECREATION_INTERVAL = 60 * 1000;

	@Override
	public void init(final UniversalServiceManagerBean usm) {
		super.init(usm);
		final String setting =
				usm.getUsmLifecycleBean().getConfiguration().getService().getCustomProperties()
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

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Process based stop detection is running. Scanning processes: " + pids);
		}

		// special handling for the 'recipe about nothing' scenario.
		if (pids.isEmpty()) {
			return false;
		}
		if (stopOnAllProcessesDead) {
			return checkForAllProcessesDead(pids);
		} else {
			return checkForOneProcessDead(pids);
		}

	}

	private ProcState getProcState(final long pid)
			throws USMException {

		if (sigarInstance == null) {
			sigarInstance = new Sigar();
			sigarCreationTime = System.currentTimeMillis();
		} else {

			if (sigarCreationTime + SIGAR_RECREATION_INTERVAL < System.currentTimeMillis()) {
				logger.log(Level.FINE, "recycling Sigar instance");
				sigarInstance.close();
				sigarInstance = null;

				sigarInstance = new Sigar();
				sigarCreationTime = System.currentTimeMillis();
				logger.log(Level.FINE, "a new sigar instance was created successfully");
			}
		}

		ProcState procState = null;
		try {
			procState = sigarInstance.getProcState(pid);
		} catch (final SigarException e) {
			if ("No such process".equals(e.getMessage())) {
				return null;
			}

			throw new USMException("Failed to check if process with PID: " + pid + " is alive. Error was: "
					+ e.getMessage(), e);
		}

		return procState;
	}

	// The sigar based process detection is problematic. When a process dies, sigar sometimes does not detect the death.
	// We solve this by creating a new sigar instance every predetermined time interval.
	/*********
	 * Checks, using Sigar, is a given process is alive.
	 *
	 * @param pid
	 *            the process pid.
	 * @return true if the process is alive (i.e. not stopped or zombie).
	 * @throws USMException
	 *             in case of an error.
	 */
	public boolean isProcessAlive(final long pid)
			throws USMException {

		final ProcState procState = getProcState(pid);
		return (procState != null && procState.getState() != ProcState.STOP
				&& procState.getState() != ProcState.ZOMBIE);
	}

	private boolean checkForOneProcessDead(final List<Long> pids)
			throws USMException {
		for (final Long pid : pids) {
			final boolean processAlive = isProcessAlive(pid);

			if (!processAlive) {
				return true;
			}
		}

		return false;

	}

	private boolean checkForAllProcessesDead(final List<Long> pids)
			throws USMException {
		for (final Long pid : pids) {
			final boolean processAlive = isProcessAlive(pid);

			if (processAlive) {
				return false;
			}
		}

		return true;
	}

}
