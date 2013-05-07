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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.LifecycleEvents;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.internal.DSLErrorMessageException;


/*********
 * Debug related utilities.
 *
 * @author barakme
 * @since 2.5.0
 *
 */
public final class DebugUtils {

	/********
	 * Private constructor to prevent instantiation.
	 */
	private DebugUtils() {

	}

	/********
	 * validates the debug setting of an install-* command.
	 *
	 * @param debugAll
	 *            .
	 * @param debugEvents
	 *            .
	 * @param debugModeString
	 *            .
	 * @throws DSLErrorMessageException .
	 */
	public static void validateDebugSettings(final boolean debugAll, final String debugEvents,
			final String debugModeString) throws DSLErrorMessageException {

		if (!debugAll && StringUtils.isBlank(debugEvents)) {
			return; // nothing to validate
		}

		if (debugAll && !StringUtils.isBlank(debugEvents)) {
			throw new DSLErrorMessageException(CloudifyErrorMessages.DEBUG_EVENTS_AND_ALL_SET);
		}

		if (debugModeString != null) {
			DebugModes mode = DebugModes.nameOf(debugModeString);
			if (mode == null) {
				throw new DSLErrorMessageException(CloudifyErrorMessages.DEBUG_UNKNOWN_MODE, debugModeString,
						DebugModes.getNames().toString());
			}
		}
		
		if (debugEvents != null) {
			final String[] parts = debugEvents.split(",");
			final Set<LifecycleEvents> debugEventsSet = new HashSet<LifecycleEvents>();
			for (final String part : parts) {
				final String temp = part.trim();
				final LifecycleEvents event = LifecycleEvents.getEventByName(temp);
				if (event == null) {
					throw new DSLErrorMessageException(CloudifyErrorMessages.DEBUG_EVENT_UNKNOWN, part);
				} else if (debugEventsSet.contains(event)) {
					throw new DSLErrorMessageException(CloudifyErrorMessages.DEBUG_EVENT_REPEATS, part);
				} else {
					debugEventsSet.add(event);
				}
			}

		}

	}

}
