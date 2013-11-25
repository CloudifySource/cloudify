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

import com.gigaspaces.log.ContinuousLogEntryMatcher;
import com.gigaspaces.log.LogEntryMatcher;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.cloudifysource.rest.events.cache.EventsCacheKey;

import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/18/13
 * Time: 8:52 PM
 * <br/><br/>
 *
 * A small cache implementation of matchers.
 * <br></br>
 *
 * We dont want to use a different matcher for every reloading operation.
 * Since this will lead in retrieving all logs every single time, even if some of them are already cache.
 * This provides us with a mechanism for retrieving just the last logs we haven't retrieved yet.
 *
 */
public class ContainerLogEntryMatcherProvider {

    private static final Logger logger = Logger.getLogger(ContainerLogEntryMatcherProvider.class.getName());

    private final LoadingCache<ContainerLogEntryMatcherProviderKey, ContinuousLogEntryMatcher> matcherCache;

    public ContainerLogEntryMatcherProvider() {

        this.matcherCache = CacheBuilder.newBuilder()
                .build(new CacheLoader<ContainerLogEntryMatcherProviderKey, ContinuousLogEntryMatcher>() {

                    @Override
                    public ContinuousLogEntryMatcher load(final ContainerLogEntryMatcherProviderKey key) throws Exception {

                        logger.fine(EventsUtils.getThreadId() + "Loading matcher cache with matcher for key " + key);

                        // continuousMatcher is always the one with the USM event logger prefix.
                        LogEntryMatcher continuousMatcher = EventsUtils.createUSMEventLoggerMatcher();
                        LogEntryMatcher initialMatcher = EventsUtils.createUSMEventLoggerMatcher();
                        return new ContinuousLogEntryMatcher(initialMatcher, continuousMatcher);
                    }
                });
    }

    /**
     * Removes all matchers that were dedicated to events with a curtain key.
     * @param key - the specified key.
     */
    public void removeAll(final EventsCacheKey key) {
        logger.fine(EventsUtils.getThreadId() + "Removing matcher for key " + key);

        for (ContainerLogEntryMatcherProviderKey logMatcherKey
                : new HashSet<ContainerLogEntryMatcherProviderKey>(matcherCache.asMap().keySet())) {
            if (logMatcherKey.getDeploymentId().equals(key.getDeploymentId())) {
                matcherCache.asMap().remove(logMatcherKey);
            }
        }
    }

    /**
     * Retrieves a matcher for the given key.
     * @param key The key.
     * @return The matching matcher.
     */
    public LogEntryMatcher get(final ContainerLogEntryMatcherProviderKey key) {
        return matcherCache.getUnchecked(key);
    }
}
