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
package org.cloudifysource.usm.liveness;

import java.util.concurrent.TimeoutException;

import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.events.USMEvent;

/***************
 * Interface for service health check implementations. The USM will run each of the liveness detector registered, and
 * will consider a service failed if any of the liveness detectors fails.
 * 
 * @author barakme
 * 
 */

public interface LivenessDetector extends USMEvent {

	/****************
	 * Returns true if the monitored service is alive, false if the service is down or unhealthy. It is sufficient for
	 * one liveness detector implementation to fail to mark a service as down.
	 * 
	 * @return true if the service is up, false otherwise.
	 * @throws USMException If the liveness detector failed to run. This will cause the start detection test to fail!
	 * @throws TimeoutException If the liveness detector failed to finish in time. This will cause the start detection
	 *         test to fail!
	 */
	boolean isProcessAlive()
			throws USMException, TimeoutException;
}
