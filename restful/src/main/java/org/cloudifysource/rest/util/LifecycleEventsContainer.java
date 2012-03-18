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
package org.cloudifysource.rest.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudifysource.dsl.internal.EventLogConstants;

public class LifecycleEventsContainer {

	private final Logger logger = Logger.getLogger(getClass().getName());
	
	/**
     * A list of processed events, .
     */
    private List<String> eventsList;

    /**
     * A set containing all of the executed lifecycle events. used to avoid duplicate prints.
     */
    private Set<String> eventsSet;

    private UUID containerUUID;

    /**
     * future container polling task.
     */
    private Future<Boolean> futureTask;

    /**
     * LifecycleEventsContainer constructor.
     */
    public LifecycleEventsContainer() {
        this.eventsSet = new HashSet<String>();
        this.eventsList = new ArrayList<String>();
    }

    public synchronized List<String> getLifecycleEvents(int curser){
        if (curser >= this.eventsList.size()  
                || curser < 0){
            return null;
        }
        return eventsList.subList(curser, eventsList.size());
    }

    /**
     * Checks if the lifecycle event already exists in the set of events.
     * If not, adds the formatted event message into the eventsList.
     * 
     * @param allLifecycleEvents - All events logged.
     */
    public final synchronized void addLifecycleEvents(
            final List<Map<String, String>> allLifecycleEvents) {

        if (allLifecycleEvents == null || allLifecycleEvents.isEmpty()) {
            return;
        }
        String outputMessage;
        for (Map<String, String> map : allLifecycleEvents) {
            Map<String, Object> sortedMap = new TreeMap<String, Object>(map);
            if (this.eventsSet.contains(sortedMap.toString())) {
            	if (logger.isLoggable(Level.FINEST)) {
            		outputMessage = getParsedLifecyceEventMessageFromMap(sortedMap);
            		logger.finest("Ignoring Lifecycle Event: " + outputMessage);
                }
            }
            else {
                this.eventsSet.add(sortedMap.toString());
                outputMessage = getParsedLifecyceEventMessageFromMap(sortedMap);
                this.eventsList.add(outputMessage);
                if (logger.isLoggable(Level.FINE)) {
                	logger.fine("Lifecycle Event: " + outputMessage);
                }
            }
        }
    }

    /**
     * Adds a non-lifecycle event into the events list.
     * For example: Planned service instances: 1, Actual service instances: 1
     * 
     * @param event event to add
     */
    public final synchronized void addInstanceCountEvent(String event) {
        if (this.eventsSet.contains(event)){
        	if (logger.isLoggable(Level.FINEST)) {
        		logger.finest("Ignoring Instance Count Event: " + event);
            }
        }
        else {
            this.eventsSet.add(event);
            this.eventsList.add(event);
        	if (logger.isLoggable(Level.FINE)) {
        		logger.fine("Instance Count Event: " + event);
            }
        }
    }

    /**
     * Creates a formatted message based on a given map of details.
     * 
     * @param map a map of details
     * @return formatted message
     */
    private String getParsedLifecyceEventMessageFromMap(final Map<String, Object> map) {
        // TODO:Check nulls
        String cleanEventText = (map.get(EventLogConstants.getEventTextKey()))
        .toString().split(" - ")[1];
        String outputMessage = '['
            + map.get(EventLogConstants.getMachineHostNameKey()).toString()
            + '/' + map.get(EventLogConstants.getMachineHostAddressKey())
            + "] " + cleanEventText;
        return outputMessage;
    }

    public void setUUID(UUID lifecycleEventsContainerUUIID) {
        this.containerUUID = lifecycleEventsContainerUUIID;

    }

    public UUID getUUID() {
        return  this.containerUUID;
    }

    public void setFutureTask(Future<Boolean> future) {
        this.futureTask = future;
    }

    public Future<Boolean> getFutureTask() {
        return this.futureTask;
    }
}
