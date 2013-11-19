/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.internal;

import java.util.HashMap;
import java.util.Map;

/**
 * The types of processors that can be provided for generate dump file operations.
 * 
 * @author yael
 * @since 2.7.0
 */
public enum ProcessorTypes {	
	/**
	 * General summary information of the process.
	 */
	SUMMARY("summary", "summary.txt"),
	/**
	 * Information on the network layer of the process and the OS network stats.
	 */
	NETWORK("network", "network.txt"),
	/**
	 * Thread dump of the process.
	 */
	THREAD("thread", "threads.txt"),
	/**
	 * Heap dump of the process. <b>Note, this is a heavy operation and can produce very large dump files</b>
	 */
	HEAP("heap", "heap"),
	/**
	 * Adds all the log files of the process to the dump file.
	 */
	LOG("log", "logs"),
	/**
	 * Adds all the log files of the process to the dump file.
	 */
	PROCESSING_UNITS("processingUnits", "processing-units")

	// CHECKSTYLE:OFF
	;
	// CHECKSTYLE:ON

	/**
	 * 
	 */
	public static final String DEFAULT_PROCESSORS = "summary,network,thread,log";

	private final String name;
	private final String fileName;

	ProcessorTypes(final String name, final String fileName) {
		this.name = name;
		this.fileName = fileName;
	}

	public String getName() {
		return name;
	}

	public String getFileName() {
		return fileName;
	}

	private static Map<String, ProcessorTypes> processorByName = createProcessorsByName();

	private static Map<String, ProcessorTypes> createProcessorsByName() {
		final HashMap<String, ProcessorTypes> map = new HashMap<String, ProcessorTypes>();
		for (final ProcessorTypes type : ProcessorTypes.values()) {
			final String name = type.name;
			map.put(name, type);
		}
		return map;
	}

	/**
	 * 
	 * @param processor
	 *            The processor's name.
	 * @return the corresponding processor type.
	 */
	public static ProcessorTypes fromStringName(final String processor) {
		return processorByName.get(processor);
	}

	/**
	 * 
	 * @param processors
	 *            The processors list.
	 * @return an array of processor names.
	 */
	public static String[] fromStringList(final String processors) {
		final String[] parts = processors.split(",");
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}
		return parts;

	}

}
