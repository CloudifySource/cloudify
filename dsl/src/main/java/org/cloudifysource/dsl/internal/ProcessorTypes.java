/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * The types of processors that can be provided for generate dump file operations.
 * @author yael
 * @since 2.7.0
 */
public enum ProcessorTypes {

	/**
	 * General summary information of the process.
	 */
	SUMMARY("summary"),
	/**
	 * Information on the network layer of the process and the OS network stats.
	 */
	NETWORK("network"),
	/**
	 * Thread dump of the process.
	 */
	THREAD("thread"),
	/**
	 * Heap dump of the process. 
	 * <b>Note, this is a heavy operation and can produce very large dump files</b>
	 */
	HEAP("heap"),
	/**
	 * Adds all the log files of the process to the dump file.
	 */
	LOG("log"),
	/**
	 * Adds all the log files of the process to the dump file.
	 */
	PROCESSING_UNITS("processingUnits")
	
	// CHECKSTYLE:OFF
	;
	// CHECKSTYLE:ON

	private final String name;

	ProcessorTypes(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	
	private static Map<String, ProcessorTypes> eventByName = createEventsByFileName();

	private static Map<String, ProcessorTypes> createEventsByFileName() {
		final HashMap<String, ProcessorTypes> map = new HashMap<String, ProcessorTypes>();
		for (final ProcessorTypes type : ProcessorTypes.values()) {
			final String name = type.name;
			map.put(name, type);
		}
		return map;
	}

	/********
	 * Returns the lifecycle event by its DSL name.
	 * @param name the lifecycle's DSL name.
	 * @return the appropraite lifecycle enum value.
	 */
	public static ProcessorTypes fromString(final String name) {
		return eventByName.get(name);
	}
}
