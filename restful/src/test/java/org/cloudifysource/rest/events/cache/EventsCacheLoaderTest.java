/*
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cloudifysource.rest.events.cache;

import com.gigaspaces.log.LogEntries;
import com.gigaspaces.log.LogEntry;
import com.gigaspaces.log.LogEntryMatcher;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.rest.events.EventsUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.admin.gsc.GridServiceContainer;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/8/13
 * Time: 5:56 PM
 */
public class EventsCacheLoaderTest {

    private final int LOG_ENTRIES_BATCH_SIZE = 10;

    @Test
    public void testLoad() throws Exception {

        EventsCacheLoader loader = new EventsCacheLoader(new MockOneGridServiceContainerProvider());

        EventsCacheKey eventsCacheKey = new EventsCacheKey("deploymentId");
        EventsCacheValue loadedValue = loader.load(eventsCacheKey);

        // test last event index
        Assert.assertEquals(LOG_ENTRIES_BATCH_SIZE, loadedValue.getLastEventIndex());

        // test all events are present and indexed correctly.
        List<DeploymentEvent> events = loadedValue.getEvents().getEvents();
        for (int i = 1; i < LOG_ENTRIES_BATCH_SIZE; i++) {
            DeploymentEvent event = EventsUtils.retrieveEventWithIndex(i, events);
            Assert.assertNotNull(event);
        }


    }

    @Test
    public void testReload() throws Exception {

        EventsCacheLoader loader = new EventsCacheLoader(new MockOneGridServiceContainerProvider());

        EventsCacheKey eventsCacheKey = new EventsCacheKey("deploymentId");
        EventsCacheValue oldValue = loader.load(eventsCacheKey);

        // reload the cache.
        ListenableFuture<EventsCacheValue> reloadedValue = loader.reload(eventsCacheKey, oldValue);

        // test old and new values are the same reference
        EventsCacheValue events = reloadedValue.get();
        Assert.assertSame(events, oldValue);

        // test last event index was updated correctly
        Assert.assertEquals(2 * LOG_ENTRIES_BATCH_SIZE, oldValue.getLastEventIndex());

        // test events were updated
        for (int i = 1; i < LOG_ENTRIES_BATCH_SIZE * 2; i++) {
            DeploymentEvent event = EventsUtils.retrieveEventWithIndex(i, events.getEvents().getEvents());
            Assert.assertNotNull(event);
        }

    }

    /**
     * This provider returns one container for each deployment id.
     * The returned container gives 10 different log line each time a call to
     * {@link GridServiceContainer#logEntries(com.gigaspaces.log.LogEntryMatcher)} is executed.
     */
    private class MockOneGridServiceContainerProvider implements GridServiceContainerProvider {

        private Map<String, Set<GridServiceContainer>> containersPerDeployment =
                new HashMap<String, Set<GridServiceContainer>>();

        @Override
        public Set<GridServiceContainer> getContainersForDeployment(String deploymentId) {
            Set<GridServiceContainer> containers = new HashSet<GridServiceContainer>();
            containers.add(createMockContainer());
            containersPerDeployment.put(deploymentId, containers);
            return containers;
        }

        private GridServiceContainer createMockContainer()  {

            GridServiceContainer mockContainer = Mockito.mock(GridServiceContainer.class);
            LogEntries mockLogEntries = createMockLogEntries();
            Mockito.when(mockContainer.logEntries(Mockito.any(LogEntryMatcher.class)))
                    .thenReturn(mockLogEntries);
            Mockito.when(mockContainer.getUid()).thenReturn(UUID.randomUUID().toString());
            return mockContainer;
        }

        private LogEntries createMockLogEntries() {

            List<LogEntry> logEntries = new ArrayList<LogEntry>();
            for (int i = 0; i < LOG_ENTRIES_BATCH_SIZE; i++) {
                LogEntry entry = createMockLogEntry();
                logEntries.add(entry);
            }

            LogEntries mockLogEntries = Mockito.mock(LogEntries.class);
            Mockito.when(mockLogEntries.iterator()).thenReturn(logEntries.iterator());
            Mockito.when(mockLogEntries.getHostAddress()).thenReturn("hostAddress");
            Mockito.when(mockLogEntries.getHostName()).thenReturn("hostName");
            return mockLogEntries;
        }

        private LogEntry createMockLogEntry() {
            LogEntry mockLogEntry = Mockito.mock(LogEntry.class);
            Mockito.when(mockLogEntry.isLog()).thenReturn(true);
            Mockito.when(mockLogEntry.getText()).thenReturn("USMLOGGER - Event" + System.currentTimeMillis());
            return mockLogEntry;
        }
    }
}
