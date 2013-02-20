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

package org.cloudifysource.usm.shutdown;

import java.util.List;
import java.util.logging.Level;

import org.cloudifysource.dsl.Service;
import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.USMUtils;
import org.cloudifysource.usm.events.AbstractUSMEventListener;
import org.cloudifysource.usm.events.EventResult;
import org.cloudifysource.usm.events.StopListener;
import org.cloudifysource.usm.events.StopReason;

/************
 * Default USM service stopper - responsible for shutting down service processes
 * using SIGAR kill commands.
 * 
 * @author barakme
 * @since 2.2
 * 
 */
public class DefaultStop extends AbstractUSMEventListener implements
		StopListener {

	private static final java.util.logging.Logger logger = java.util.logging.Logger
			.getLogger(DefaultStop.class.getName());

	@Override
	public EventResult onStop(final StopReason reason) {
		final List<Long> pids = this.usm.getServiceProcessesList();

		Service service = this.usm.getUsmLifecycleBean().getConfiguration()
				.getService();
		if (service.getLifecycle().getStart() == null) {
			logger.info("Service did not specify a 'start' element,"
					+ " so default stop implementation will not shutdown any processes. Current service process list: "
					+ pids);
		}

		USMException firstException = null;
		if (pids != null) {
			for (final Long pid : pids) {

				try {
					if (USMUtils.isProcessAlive(pid)) {
						usm.getUsmLifecycleBean().getProcessKiller()
								.killProcess(pid);
					}
				} catch (final USMException e) {
					firstException = e;
					logger.log(Level.SEVERE,
							"Failed to kill process with pid: " + pid, e);
				}
			}
		}

		if (firstException == null) {
			return EventResult.SUCCESS;
		} else {
			logger.log(
					Level.SEVERE,
					"Default stop implementation failed to stop at least one process. "
							+ "This process may be leaking. First exception was: "
							+ firstException.getMessage(), firstException);
			return new EventResult(firstException);
		}
	}

}
