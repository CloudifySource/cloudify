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
package org.cloudifysource.rest.events;

import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;

import java.text.MessageFormat;
import java.util.List;

import static com.gigaspaces.log.LogEntryMatchers.regex;

/**
 * Created with IntelliJ IDEA. User: elip Date: 5/16/13 Time: 10:47 AM <br/>
 * <br/>
 * 
 * Utility class for events related operations. mainly the translation of logs to events.
 */
public final class EventsUtils {

    private static final String ESM_EVENT_LOGGER_NAME = ".*.ESMEventLogger.*";;

    private static final String USM_EVENT_LOGGER_NAME = ".*.USMEventLogger.*";

    private EventsUtils() {

	}

	/**
	 * Given a log entry, translate to event.
	 * 
	 * @param logEntry
	 *            The log entry.
	 * @param hostName
	 *            The host name.
	 * @param hostAddress
	 *            The host address.
	 * @return The event.
	 */
	public static DeploymentEvent logToEvent(final LogEntry logEntry,
			final String hostName,
			final String hostAddress) {
		String text = logEntry.getText();
		String[] split = text.split(" - ");
		if (split.length < 2) {
			throw new IllegalArgumentException("Expected a separator (' - ') in USM log entry: " + text);
		}
		String textWithoutLogger = split[1];
		String actualEvent = textWithoutLogger.substring(textWithoutLogger.indexOf(".") + 1);
		DeploymentEvent event = new DeploymentEvent();
		event.setDescription("[" + hostName + "/" + hostAddress + "] - " + actualEvent);
		return event;
	}

	/**
	 * Creates a matcher for
	 * {@link org.openspaces.admin.gsc.GridServiceContainer#logEntries(com.gigaspaces.log.LogEntryMatcher)}. This
	 * matcher will find USM related event only using the {@code USM_EVENT_LOGGER_NAME} regex.
	 * 
	 * @return The log entry matcher.
	 */
	public static LogEntryMatcher createUSMEventLoggerMatcher() {
		final String regex = MessageFormat.format(USM_EVENT_LOGGER_NAME, new Object() {
		});
		return regex(regex);
	}

    /**
     * Creates a matcher for
     * {@link org.openspaces.admin.esm.ElasticServiceManager#logEntries(com.gigaspaces.log.LogEntryMatcher)}. This
     * matcher will find USM related event only using the {@code ESM_EVENT_LOGGER_NAME} regex.
     *
     * @return The log entry matcher.
     */
    public static LogEntryMatcher createESMEventLoggerMatcher() {
        final String regex = MessageFormat.format(ESM_EVENT_LOGGER_NAME, new Object() {
        });
        return regex(regex);
    }



    /**
	 * Given a set of events and indices, extract only events who's index is in range.
	 * 
	 * @param events
	 *            The events set.
	 * @param from
	 *            The start index.
	 * @param to
	 *            The end index.
	 * @return The requested events.
	 */
	public static DeploymentEvents extractDesiredEvents(final DeploymentEvents events,
			final int from,
			final int to) {

		DeploymentEvents desiredEvents = new DeploymentEvents();
		for (int i = from; i <= to; i++) {
			List<DeploymentEvent> serviceDeploymentEvents = events.getEvents();
			DeploymentEvent serviceDeploymentEvent = retrieveEventWithIndex(i, serviceDeploymentEvents);
			if (serviceDeploymentEvent != null) {
				desiredEvents.getEvents().add(serviceDeploymentEvent);
			}
		}
		return desiredEvents;
	}

	/**
	 * Retrieves an event with a curtain index from a list of events.
	 * 
	 * @param i
	 *            The index.
	 * @param serviceDeploymentEvents
	 *            The events.
	 * @return a {@link DeploymentEvent} if found, null otherwise.
	 */
	public static DeploymentEvent retrieveEventWithIndex(final int i,
			final List<DeploymentEvent> serviceDeploymentEvents) {
		for (DeploymentEvent event1 : serviceDeploymentEvents) {
			if (event1.getIndex() == i) {
				return event1;
			}
		}
		return null;
	}

	/**
	 * Given a set of events and indices, check if all index range of events is present. i.e if events contains 1,2,3
	 * and from=1 and to=4, this will return false. you get the idea.
	 * 
	 * @param events
	 *            The events set.
	 * @param from
	 *            The start index.
	 * @param to
	 *            The end index.
	 * @return true if all events in the range are present. false otherwise.
	 */
	public static boolean eventsPresent(final DeploymentEvents events,
			final int from,
			final int to) {

		for (int i = from; i <= to; i++) {
			DeploymentEvent event = retrieveEventWithIndex(i, events.getEvents());
			if (event == null) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 
	 * @return The id of the current thread.
	 */
	public static String getThreadId() {
		return "[" + Thread.currentThread().getName() + "][" + Thread.currentThread().getId() + "] - ";
	}

}
