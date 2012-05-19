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

/********
 * A simple wrapper to the usm, passed to threads that can notify the USM of the failure of the service. Having one such
 * object makes it easier to handle concurrent notifications of such a failure..
 * 
 * @author barakme
 * 
 */
public class ProcessDeathNotifier {

	private boolean processDead = false;
	private final UniversalServiceManagerBean usm;

	public ProcessDeathNotifier(final UniversalServiceManagerBean usm) {
		super();
		this.usm = usm;
	}

	/*********
	 * Called by an object on a different thread, typically a stop detector, to notify the USM that the service is down.
	 * Only the first invocation by a thread that the service has died will be passed to the USM.
	 * This method blocks until the USM has finished handling the service death notification.
	 */
	public synchronized void processDeathDetected() {
		if (processDead) {
			return;
		}

		processDead = true;
		usm.onProcessDeath();
	}

}
