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

import org.cloudifysource.usm.USMException;
import org.cloudifysource.usm.events.USMEvent;

/**************
 * Stop detection event interface. An implementation is expected to check if the monitored service is up or not.
 * 
 * @author barakme
 * 
 */
public interface StopDetector extends USMEvent {

	/********
	 * Called periodically to check if the service is up.
	 * 
	 * @return true if the service is stopped, false if it is functioning correctly.
	 * @throws USMException in case of an error while executing the detector. An exception will not cause the service to
	 *         be considered stopped.
	 */
	boolean isServiceStopped()
			throws USMException;

}
