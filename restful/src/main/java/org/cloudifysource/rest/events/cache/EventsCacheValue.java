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

import org.cloudifysource.dsl.rest.response.DeploymentEvents;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/20/13
 * Time: 2:00 PM
 * <br/><br/>
 *
 * Value for the events cache. containing the actual events plus some implementation specific information.
 */
public class EventsCacheValue {

    private DeploymentEvents events = new DeploymentEvents();
    private long lastRefreshedTimestamp;
    private int lastEventIndex;
    private volatile Object mutex = new Object();
    private Set<ProcessingUnit> processingUnits = new HashSet<ProcessingUnit>();
    private Set<GridServiceContainer> containers = new HashSet<GridServiceContainer>();

    public ElasticServiceManager getElasticServiceManager() {
        return elasticServiceManager;
    }

    public void setElasticServiceManager(final ElasticServiceManager elasticServiceManager) {
        this.elasticServiceManager = elasticServiceManager;
    }

    private ElasticServiceManager elasticServiceManager;

    public Set<ProcessingUnit> getProcessingUnits() {
        return processingUnits;
    }

    public DeploymentEvents getEvents() {
        return events;
    }

    public void setEvents(final DeploymentEvents events) {
        this.events = events;
    }

    public long getLastRefreshedTimestamp() {
        return lastRefreshedTimestamp;
    }

    public void setLastRefreshedTimestamp(final long lastRefreshedTimestamp) {
        this.lastRefreshedTimestamp = lastRefreshedTimestamp;
    }

    public int getLastEventIndex() {
        return lastEventIndex;
    }

    public void setLastEventIndex(final int lastEventIndex) {
        this.lastEventIndex = lastEventIndex;
    }

    public Object getMutex() {
        return mutex;
    }

    @Override
    public String toString() {
        return "EventsCacheValue{" + "events=" + events
                + ", lastRefreshedTimestamp=" + lastRefreshedTimestamp
                + ", lastEventIndex=" + lastEventIndex + ", mutex=" + mutex + '}';
    }

	public Set<GridServiceContainer> getContainers() {
		return containers;
	}

	public void setContainers(Set<GridServiceContainer> containers) {
		this.containers = containers;
	}
}
