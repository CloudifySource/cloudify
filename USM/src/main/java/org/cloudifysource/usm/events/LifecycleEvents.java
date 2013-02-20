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
package org.cloudifysource.usm.events;

import java.util.HashMap;
import java.util.Map;

public enum LifecycleEvents {

	PRE_SERVICE_START("preServiceStart"),
	INIT("init"),
	PRE_INSTALL("preInstall"),
	INSTALL("install"),
	POST_INSTALL("postInstall"),
	PRE_START("preStart"),
	POST_START("postStart"),
	PRE_STOP("preStop"),
	STOP("Stop"),
	POST_STOP("postStop"),
	SHUTDOWN("shutdown"),
	PRE_SERVICE_STOP("preServiceStop");

	private LifecycleEvents(String... fileNames) {
		this.fileNames = fileNames;
	}

	private String[] fileNames;
	
	private static Map<String, LifecycleEvents> eventByFileName = createEventsByFileName();
	
	private static Map<String, LifecycleEvents> createEventsByFileName() {
		HashMap<String, LifecycleEvents> map = new HashMap<String, LifecycleEvents>();
		for (LifecycleEvents event : LifecycleEvents.values()) {
			String[] fileNames = event.fileNames;
			for (String name : fileNames) {
				map.put(name, event);
			}
		}
		return map;
	}
	public static LifecycleEvents getEventForFile(String fileName) {
		return eventByFileName.get(fileName);
	}
}
