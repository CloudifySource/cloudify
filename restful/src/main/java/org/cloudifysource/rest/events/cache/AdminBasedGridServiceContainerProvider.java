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

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.Admin;
import org.openspaces.admin.esm.ElasticServiceManager;
import org.openspaces.admin.esm.ElasticServiceManagers;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.zone.Zone;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/8/13
 * Time: 5:42 PM
 *
 * Retrieves container using the {@link Admin} instance.
 * This is not a Cache, it performs admin lookup each time a call is made.
 *
 */
public class AdminBasedGridServiceContainerProvider implements GridServiceContainerProvider, ElasticServiceManagerProvider {

    private static final Logger logger = Logger.getLogger(AdminBasedGridServiceContainerProvider.class.getName());

    private final Admin admin;

    public AdminBasedGridServiceContainerProvider(final Admin admin) {
        this.admin = admin;
    }

    @Override
    public ElasticServiceManager getElasticServiceManager() {
        ElasticServiceManagers elasticServiceManagers = admin.getElasticServiceManagers();
        if (elasticServiceManagers == null || elasticServiceManagers.getSize() == 0) {
            throw new IllegalStateException("Could not find any elastic service managers");
        }
        return elasticServiceManagers.getManagers()[0];
    }

    @Override
    public Set<GridServiceContainer> getContainersForDeployment(final String deploymentId) {

        logger.fine("Searching for processing units with deployment id " + deploymentId);

        Set<ProcessingUnit> processingUnitsForDeploymentId = new HashSet<ProcessingUnit>();
        Set<GridServiceContainer> containers = new HashSet<GridServiceContainer>();
        for (ProcessingUnit pu : admin.getProcessingUnits()) {
            String puDeploymentId = (String) pu.getBeanLevelProperties().getContextProperties()
                    .get(CloudifyConstants.CONTEXT_PROPERTY_DEPLOYMENT_ID);
            if (deploymentId.equals(puDeploymentId)) {
                processingUnitsForDeploymentId.add(pu);
            }
        }

        for (ProcessingUnit pu : processingUnitsForDeploymentId) {
            logger.fine("Retrieving containers for processing unit " + pu.getName());
            containers.addAll(getContainersForProcessingUnit(pu));
        }

        logger.fine("Found containers " + toUidAndZone(containers) + " for deployment id " + deploymentId);
        return containers;
    }

    private Set<String> toUidAndZone(final Set<GridServiceContainer> containers) {
        Set<String> humanReadable = new HashSet<String>();
        for (GridServiceContainer container : containers) {
            humanReadable.add(container.getUid()  + container.getExactZones().getZones());
        }
        return humanReadable;
    }

    private Set<GridServiceContainer> getContainersForProcessingUnit(final ProcessingUnit pu) {
        Set<GridServiceContainer> containers = new HashSet<GridServiceContainer>();
        Zone zone = pu.getAdmin().getZones().getByName(pu.getName());
        if (zone != null) {
            for (GridServiceContainer container : zone.getGridServiceContainers()) {
                containers.add(container);
            }
        }

        return containers;
    }
}
