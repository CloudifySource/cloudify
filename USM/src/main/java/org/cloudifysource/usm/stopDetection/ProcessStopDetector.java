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

import java.util.logging.Logger;

import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.USMUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;


public class ProcessStopDetector implements StopDetector {

	private UniversalServiceManagerBean usm;

	private final static Logger logger = Logger.getLogger(ProcessStopDetector.class.getName());
	@Override
	public boolean isServiceStopped() throws USMException {
		final long pid = usm.getActualProcessID();
		final boolean processStopped = !USMUtils.isProcessAlive(pid);
		if(processStopped) {
			logger.warning("Process Stop Detector execution has found that process: " + pid + " is not alive!");
		}
		return processStopped;
	}

	@Override
	public void init(UniversalServiceManagerBean usm) {
		this.usm = usm;
	}

	@Override
	public int getOrder() {
		return 5;
	}

}
