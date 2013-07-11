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
package org.cloudifysource.domain;

import java.util.HashMap;
import java.util.Map;

/**********
 * enum for lifecycle events.
 * @author barakme
 * @since 2.0
 *
 */
public enum LifecycleEvents {

	// CHECKSTYLE:OFF
	PRE_SERVICE_START("preServiceStart"),
	INIT("init"),
	PRE_INSTALL("preInstall"),
	INSTALL("install"),
	POST_INSTALL("postInstall"),
	PRE_START("preStart"),
	START("start"),
	POST_START("postStart"),
	PRE_STOP("preStop"),
	STOP("Stop"),
	POST_STOP("postStop"),
	SHUTDOWN("shutdown"),
	PRE_SERVICE_STOP("preServiceStop"),
	CUSTOM_COMMAND("customCommand"),
	SERVICE_DETAILS("details"),
	SERVICE_MONITORS("monitors"),
	START_DETECTION("startDetection"),
	STOP_DETECTION("stopDetection"),
	PROCESS_LOCATOR("locator");

	// CHECKSTYLE:ON
	private LifecycleEvents(final String... names) {
		this.names = names;
	}

	private String[] names;

	private static Map<String, LifecycleEvents> eventByName = createEventsByFileName();

	private static Map<String, LifecycleEvents> createEventsByFileName() {
		final HashMap<String, LifecycleEvents> map = new HashMap<String, LifecycleEvents>();
		for (final LifecycleEvents event : LifecycleEvents.values()) {
			final String[] fileNames = event.names;
			for (final String name : fileNames) {
				map.put(name, event);
			}
		}
		return map;
	}

	/********
	 * Returns the lifecycle event by its DSL name.
	 * @param name the lifecycle's DSL name.
	 * @return the appropraite lifecycle enum value.
	 */
	public static LifecycleEvents getEventByName(final String name) {
		return eventByName.get(name);
	}
}
