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
package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.rest.events.EventsUtils;
import org.cloudifysource.rest.events.LogEntryMatcherProvider;
import org.cloudifysource.rest.events.LogEntryMatcherProviderKey;
import org.cloudifysource.rest.exceptions.ResourceNotFoundException;
import org.openspaces.admin.gsc.GridServiceContainer;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/20/13
 * Time: 2:09 PM
 * <br/><br/>
 * The cache loader used to load events to the events cache.
 * implements load, for initial load. and reload, for continues cache population.
 *
 * Load and reload operation will execute a remote call to fetch container logs.
 * These logs are then translated to events and saved inside the cache.
 *
 * @see org.cloudifysource.dsl.rest.response.DeploymentEvents
 *
 */
public class EventsCacheLoader extends CacheLoader<EventsCacheKey, EventsCacheValue> {

    private static final Logger logger = Logger.getLogger(EventsCacheLoader.class.getName());

    private final LogEntryMatcherProvider matcherProvider;
    private final GridServiceContainerProvider containerProvider;

    public EventsCacheLoader(final GridServiceContainerProvider containerProvider) {

        this.matcherProvider = new LogEntryMatcherProvider();
        this.containerProvider = containerProvider;
    }

    @Override
    public EventsCacheValue load(final EventsCacheKey key) throws Exception {

        logger.fine(EventsUtils.getThreadId() + "Could not find events for key " + key
                + " in cache. Loading from container logs...");

        DeploymentEvents events = new DeploymentEvents();

        // initial load. no events are present in the cache for this deployment.
        // iterate over all container and retrieve logs from logs cache.
        Set<GridServiceContainer> containersForDeployment = containerProvider.getContainersForDeployment(
                key.getDeploymentId());

        // no deployment with the given id was found
        if (containersForDeployment == null || containersForDeployment.isEmpty()) {
            throw new ResourceNotFoundException("Deployment with id " + key.getDeploymentId());
        }

        int index = -1;
        for (GridServiceContainer container : containersForDeployment) {

            LogEntryMatcherProviderKey logEntryMatcherProviderKey = createKey(container, key);

            LogEntries logEntries = container.logEntries(matcherProvider.get(logEntryMatcherProviderKey));
            for (LogEntry logEntry : logEntries) {
                if (logEntry.isLog()) {
                    DeploymentEvent event = EventsUtils.logToEvent(logEntry,
                            logEntries.getHostName(), logEntries.getHostAddress());
                    event.setIndex(++index);
                    events.getEvents().add(event);
                }
            }

        }

        EventsCacheValue value = new EventsCacheValue();
        value.setLastEventIndex(index);
        value.setEvents(events);
        value.setLastRefreshedTimestamp(System.currentTimeMillis());
        return value;
    }


    @Override
    public ListenableFuture<EventsCacheValue> reload(final EventsCacheKey key, final EventsCacheValue oldValue)
            throws Exception {

        logger.fine(EventsUtils.getThreadId() + "Reloading events cache entry for key " + key);

        // pickup any new containers along with the old ones
        Set<GridServiceContainer> containersForDeployment = new HashSet<GridServiceContainer>();

        containersForDeployment.addAll(containerProvider.getContainersForDeployment(key.getDeploymentId()));

        if (containersForDeployment.isEmpty()) {
        	// get containers by the deployment id
        	containersForDeployment = containerProvider.getContainersForDeployment(
                    key.getDeploymentId());
        }
        if (!containersForDeployment.isEmpty()) {
            int index = oldValue.getLastEventIndex() + 1;
            for (GridServiceContainer container : containersForDeployment) {

                // this will give us just the new logs.
                LogEntryMatcherProviderKey logEntryMatcherProviderKey = createKey(container, key);
                LogEntryMatcher matcher = matcherProvider.get(logEntryMatcherProviderKey);
                LogEntries logEntries = container.logEntries(matcher);

                for (LogEntry logEntry : logEntries) {
                    if (logEntry.isLog()) {
                        DeploymentEvent event = EventsUtils.logToEvent(
                                logEntry, logEntries.getHostName(), logEntries.getHostAddress());
                        event.setIndex(index++);
                        oldValue.getEvents().getEvents().add(event);
                    }
                }
            }

            // update refresh time.
            oldValue.setLastRefreshedTimestamp(System.currentTimeMillis());
            oldValue.setLastEventIndex(index - 1);
        }
        return Futures.immediateFuture(oldValue);
    }

    public LogEntryMatcherProvider getMatcherProvider() {
        return matcherProvider;
    }

    private LogEntryMatcherProviderKey createKey(final GridServiceContainer container,
                                                 final EventsCacheKey key) {
        LogEntryMatcherProviderKey logEntryMatcherProviderKey = new LogEntryMatcherProviderKey();
        logEntryMatcherProviderKey.setDeploymentId(key.getDeploymentId());
        logEntryMatcherProviderKey.setContainer(container);
        return logEntryMatcherProviderKey;
    }
}
