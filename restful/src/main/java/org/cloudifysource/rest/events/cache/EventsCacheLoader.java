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
import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.cloudifysource.dsl.rest.response.DeploymentEvent;
import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.cloudifysource.rest.events.ContainerLogEntryMatcherProvider;
import org.cloudifysource.rest.events.ContainerLogEntryMatcherProviderKey;
import org.cloudifysource.rest.events.ElasticServiceManagerLogEntryMatcherProvider;
import org.cloudifysource.rest.events.ElasticServiceManagerLogEntryMatcherProviderKey;
import org.cloudifysource.rest.events.EventsUtils;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

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

    private final ContainerLogEntryMatcherProvider containerMatcherProvider;
    private final GridServiceContainerProvider containerProvider;

    private final ElasticServiceManagerLogEntryMatcherProvider elasticServiceManagerMatcherProvider;
    private final ElasticServiceManagerProvider elasticServiceManagerProvider;

    public EventsCacheLoader(final GridServiceContainerProvider containerProvider,
                             final ElasticServiceManagerProvider elasticServiceManagerProvider) {

        this.containerMatcherProvider = new ContainerLogEntryMatcherProvider();
        this.elasticServiceManagerMatcherProvider = new ElasticServiceManagerLogEntryMatcherProvider();
        this.containerProvider = containerProvider;
        this.elasticServiceManagerProvider = elasticServiceManagerProvider;

    }

    @Override
    public EventsCacheValue load(final EventsCacheKey key) throws Exception {

        logger.fine(EventsUtils.getThreadId() + "Could not find events for key " + key
                + " in cache. Loading from container logs...");

        DeploymentEvents events = new DeploymentEvents();

        // initial load. no events are present in the cache for this deployment.
        // iterate over all container and retrieve logs from logs cache.
        Set<GridServiceContainer> containersForDeployment = containerProvider
                .getContainersForDeployment(key.getDeploymentId());

        Set<ProcessingUnit> processingUnitsForDeployment = new HashSet<ProcessingUnit>();

        int index = 0;
        for (GridServiceContainer container : containersForDeployment) {

            ProcessingUnitInstance[] processingUnitInstances = container.getProcessingUnitInstances();
            if (processingUnitInstances != null && processingUnitInstances.length > 0) {
                processingUnitsForDeployment.add(processingUnitInstances[0].getProcessingUnit());
            }

            index = addEventsFromContainer(index, container, key.getDeploymentId(), events);
        }

        // add logs from the esm.
        ElasticServiceManager elasticServiceManager = elasticServiceManagerProvider.getElasticServiceManager();
        index = addEventsFromElasticServiceManager(index, elasticServiceManager, key.getDeploymentId(), events);

        EventsCacheValue value = new EventsCacheValue();
        value.setLastEventIndex(index);
        value.setEvents(events);
        value.getProcessingUnits().addAll(processingUnitsForDeployment);
        value.setContainers(containersForDeployment);
        value.setElasticServiceManager(elasticServiceManager);
        value.setLastRefreshedTimestamp(System.currentTimeMillis());
        return value;
    }


    @Override
    public ListenableFuture<EventsCacheValue> reload(final EventsCacheKey key, final EventsCacheValue oldValue)
            throws Exception {

        logger.fine(EventsUtils.getThreadId() + "Reloading events cache entry for key " + key);

        // pickup any new containers along with the old ones
        oldValue.getContainers().addAll(containerProvider.getContainersForDeployment(key.getDeploymentId()));

        if (!oldValue.getContainers().isEmpty()) {
            int index = oldValue.getLastEventIndex();
            for (GridServiceContainer container : oldValue.getContainers()) {
                index = addEventsFromContainer(index, container, key.getDeploymentId(), oldValue.getEvents());
            }

            // update refresh time.
            oldValue.setLastRefreshedTimestamp(System.currentTimeMillis());
            oldValue.setLastEventIndex(index);
        }
        return Futures.immediateFuture(oldValue);
    }

    public ContainerLogEntryMatcherProvider getContainerMatcherProvider() {
        return containerMatcherProvider;
    }

    private ContainerLogEntryMatcherProviderKey createKey(final GridServiceContainer container,
                                                          final String deploymentId) {
        ContainerLogEntryMatcherProviderKey
                containerLogEntryMatcherProviderKey = new ContainerLogEntryMatcherProviderKey();
        containerLogEntryMatcherProviderKey.setDeploymentId(deploymentId);
        containerLogEntryMatcherProviderKey.setContainer(container);
        return containerLogEntryMatcherProviderKey;
    }

    private ElasticServiceManagerLogEntryMatcherProviderKey createKey(final ElasticServiceManager elasticServiceManager,
                                                                      final String deploymentId) {
        ElasticServiceManagerLogEntryMatcherProviderKey
                elasticServiceManagerLogEntryMatcherProviderKey = new ElasticServiceManagerLogEntryMatcherProviderKey
                ();
        elasticServiceManagerLogEntryMatcherProviderKey.setDeploymentId(deploymentId);
        elasticServiceManagerLogEntryMatcherProviderKey.setManager(elasticServiceManager);
        return elasticServiceManagerLogEntryMatcherProviderKey;

    }

    private int addEventsFromContainer(final int previousIndex,
                                        final GridServiceContainer container,
                                        final String deploymentId,
                                        final DeploymentEvents events) {
        int index = previousIndex;
        ContainerLogEntryMatcherProviderKey containerLogEntryMatcherProviderKey = createKey(container, deploymentId);

        if (container.isDiscovered()) {
            LogEntries logEntries = container.logEntries(containerMatcherProvider.get(
                    containerLogEntryMatcherProviderKey));
            for (LogEntry logEntry : logEntries) {
                if (logEntry.isLog()) {
                    DeploymentEvent event = EventsUtils.logToEvent(logEntry,
                            logEntries.getHostName(), logEntries.getHostAddress());
                    event.setIndex(++index);
                    events.getEvents().add(event);
                }
            }
        } else {
            logger.fine(EventsUtils.getThreadId() + "Not retrieving logs from container " + container.getUid()
                    + container.getExactZones().getZones() +  " since it is not discovered by the admin");
        }
        return index;
    }

    private int addEventsFromElasticServiceManager(final int previousIndex,
                                       final ElasticServiceManager manager,
                                       final String deploymentId,
                                       final DeploymentEvents events) {
        int index = previousIndex;
        ElasticServiceManagerLogEntryMatcherProviderKey elasticServiceManagerLogEntryMatcherProviderKey
                = createKey(manager, deploymentId);

        if (manager.isDiscovered()) {
            LogEntries logEntries = manager.logEntries(elasticServiceManagerMatcherProvider.get(
                    elasticServiceManagerLogEntryMatcherProviderKey));
            for (LogEntry logEntry : logEntries) {
                if (logEntry.isLog()) {
                    DeploymentEvent event = EventsUtils.logToEvent(logEntry,
                            logEntries.getHostName(), logEntries.getHostAddress());
                    event.setIndex(++index);
                    events.getEvents().add(event);
                }
            }
        } else {
            logger.fine(EventsUtils.getThreadId() + "Not retrieving logs from esm " + manager.getUid()
                    + " since it is not discovered by the admin");
        }
        return index;
    }

}
