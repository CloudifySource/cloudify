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
 ******************************************************************************/
package org.cloudifysource.restclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.cloudifysource.dsl.internal.EventLogConstants;


/**
 * This class formats event lines as messages ready for print.
 */
public class EventLoggingTailer {

	/**
	 * A list of processed events, used to avoid duplicate prints.
	 */
	private Set<String> eventsSet;

	/**
	 * Empty Ctor.
	 */
	public EventLoggingTailer() {
		this.eventsSet = new HashSet<String>();
	}

	/**
	 * Create a list of messages from a given list of mapped event lines, while
	 * avoiding duplicate lines.
	 * 
	 * @param allLines
	 *            a list of mapped lines
	 * @return a list of formatted messages, based on the given lines
	 */
	public final List<String> getLinesToPrint(
			final List<Map<String, String>> allLines) {

		if (allLines == null || allLines.isEmpty()) {
			return null;
		}
		String outputMessage;
		List<String> outputList = new ArrayList<String>();
		for (Map<String, String> map : allLines) {
			Map<String, Object> sortedMap = new TreeMap<String, Object>(map);
			if (!eventsSet.contains(sortedMap.toString())) {
				eventsSet.add(sortedMap.toString());
				outputMessage = getMessageFromMap(sortedMap);
				outputList.add(outputMessage);
			}

		}
		return outputList;
	}

	/**
	 * Creates a formatted message based on a given map of details.
	 * 
	 * @param map a map of details
	 * @return formatted message
	 */
	private String getMessageFromMap(final Map<String, Object> map) {
		// TODO:Check nulls
		String cleanEventText = (map.get(EventLogConstants.getEventTextKey()))
				.toString().split(" - ")[1];
		String outputMessage = '['
				+ map.get(EventLogConstants.getMachineHostNameKey()).toString()
				+ '/' + map.get(EventLogConstants.getMachineHostAddressKey())
				+ "] " + cleanEventText;
		return outputMessage;
	}
}
