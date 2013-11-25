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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.rest.events.ContainerLogEntryMatcherProvider;
import org.openspaces.admin.Admin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 8:41 AM
 * <br/><br/>
 *
 * This class is the cache implementation for life cycle events.
 * Cache entries are deleted automatically in they haven't been accessed to in more than 5 minutes.
 *
 * Events are populated using a guava based {@link com.google.common.cache.CacheLoader}.
 *
 * @see EventsCacheLoader
 *
 */
public class EventsCache {

    private static final Logger logger = Logger.getLogger(EventsCache.class.getName());

    private static final int CACHE_EXPIRATION_MINUTES = 5;

    private final LoadingCache<EventsCacheKey, EventsCacheValue> eventsLoadingCache;
    private final ContainerLogEntryMatcherProvider matcherProvider;
    private int cacheExpirationPeriod = CACHE_EXPIRATION_MINUTES;
    private TimeUnit cacheExpirationTimeunit = TimeUnit.MINUTES;

    public void setCacheExpirationPeriod(int cacheExpirationPeriod) {
        this.cacheExpirationPeriod = cacheExpirationPeriod;
    }

    public void setCacheExpirationTimeunit(TimeUnit cacheExpirationTimeunit) {
        this.cacheExpirationTimeunit = cacheExpirationTimeunit;
    }

    public EventsCache(final Admin admin) {

        final EventsCacheLoader loader = new EventsCacheLoader(new AdminBasedGridServiceContainerProvider(admin));

        this.matcherProvider = loader.getContainerMatcherProvider();
        this.eventsLoadingCache = CacheBuilder.newBuilder()
                .expireAfterAccess(cacheExpirationPeriod, cacheExpirationTimeunit)
                .removalListener(new RemovalListener<Object, Object>() {

                    @Override
                    public void onRemoval(final RemovalNotification<Object, Object> notification) {

                        // the onRemoval will also be triggered on reload (RemovalCause.REPLACED)
                        // in that case we don't want to remove the matcher.
                        if (notification.wasEvicted()) {

                            logger.fine("Entry with key " + notification.getKey() + " was removed from cache.");
                            final EventsCacheKey key = (EventsCacheKey) notification.getKey();
                            matcherProvider.removeAll(key);
                        }

                    }
                })
                .build(loader);
    }

    /**
     * Refresh the cache. this results in a call to {@link EventsCacheLoader#reload(EventsCacheKey, EventsCacheValue)}.
     * @param key The key to refresh.
     */
    public void refresh(final EventsCacheKey key) {
        logger.finest("Refreshing events cache for entry " + key);
        eventsLoadingCache.refresh(key);
    }

    /**
     * Retrieve a cache entry. if the entry is not found, a cache loading is executed using
     *              {@link EventsCacheLoader#load(EventsCacheKey)}
     * @param key The key of the requested entry.
     * @return The cache value. containing also the events.
     * @throws java.util.concurrent.ExecutionException Thrown in case a failure happened while loading the cache with a new entry.
     */
    public EventsCacheValue get(final EventsCacheKey key) throws ExecutionException {
        logger.finest("Retrieving events cache value for entry " + key);
        return eventsLoadingCache.get(key);
    }

    /**
     * Retrieves a cache entry only if it exists. otherwise will return null.
     * @param key The key of the requested entry.
     * @return The cache value. containing also the events. null if doesnt exist.
     */
    public EventsCacheValue getIfExists(final EventsCacheKey key) {
        logger.finest("Retrieving events cache value for entry " + key + " if exists");
        return eventsLoadingCache.getIfPresent(key);
    }

    /**
     * Explicitly put a new entry to the cache. this method is used only when installing or uninstalling the service.
     * @param key The key of the requested entry.
     * @param value The value of the requested entry.
     */
    public void put(final EventsCacheKey key, final EventsCacheValue value) {
        if (!eventsLoadingCache.asMap().containsKey(key)) {
            logger.finest("Putting new value in events cache for key " + key + " : " + value);
            eventsLoadingCache.asMap().put(key, value);
        } else {
            logger.finest("Not putting new value in events cache for key " + key + " . it already exists.");
        }
    }
    
    /**
     * Adds the latest event to the events of the specified key.
     * @param key A key of ServiceDeploymentEvents
     * @param event The event to add
     */
    public void add(final EventsCacheKey key, final DeploymentEvent event) {
    	EventsCacheValue eventsCacheValue = eventsLoadingCache.asMap().get(key);
        synchronized (eventsCacheValue.getMutex()) {
            DeploymentEvents events = eventsCacheValue.getEvents();
            event.setIndex(eventsCacheValue.getLastEventIndex() + 1);
            logger.finest("Adding event " + event + " to cache value with key " + key);
            events.getEvents().add(event);
        }
    }
}
