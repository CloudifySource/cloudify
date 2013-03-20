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
package org.cloudifysource.dsl.internal.debug;

import java.util.ArrayList;
import java.util.List;

/************
 * Enum for supported debug modes.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
public enum DebugModes {
	/**
	 * just create debug environment instead of running the target script.
	 */
	INSTEAD("instead"),
	/***
	 * run the target script and then let me debug the outcome state.
	 */
	AFTER("after"),
	/****
	 * like 'after', but only stop for debugging if the script fails.
	 */
	ON_ERROR("onError");

	// #onError - like 'after', but only stop for debugging if the script fails

	private final String name;

	DebugModes(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/*******
	 * Get the list of debug mode names.
	 *
	 * @return list of all debug mode names.
	 */
	public static List<String> getNames() {

		final ArrayList<String> result = new ArrayList<String>(DebugModes.values().length);

		for (DebugModes mode : DebugModes.values()) {
			result.add(mode.getName());
		}

		return result;
	}

	/*******
	 * Returns a debug mode by its name.
	 * @param name the debug mode name.
	 * @return the debug mode.
	 */
	public static DebugModes nameOf(final String name) {

		for (DebugModes mode : DebugModes.values()) {
			if (mode.getName().equalsIgnoreCase(name)) {
				return mode;
			}
		}

		return null;
	}

}
