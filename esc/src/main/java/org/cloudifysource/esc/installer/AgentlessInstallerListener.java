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
package org.cloudifysource.esc.installer;

/**********
 * Interface for event listener.
 * 
 * @author barakme
 * @since 2.0.1
 * 
 */
public interface AgentlessInstallerListener {

	/*****
	 * Callback method invoked for each event fired by the installer.
	 * IMPORTANT: Do not run long tasks on the thread that called this method,
	 * as this will block the installation process.
	 * 
	 * @param eventName
	 *            A string identifier for the event.
	 * @param args
	 *            optional event arguments.
	 */
	void onInstallerEvent(String eventName, Object... args);
}
