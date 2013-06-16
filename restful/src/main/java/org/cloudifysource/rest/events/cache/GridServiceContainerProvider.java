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

import org.openspaces.admin.gsc.GridServiceContainer;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 6/8/13
 * Time: 5:37 PM
 *
 * Interface for providing {@link org.openspaces.admin.gsc.GridServiceContainers} to the events cache.
 */
public interface GridServiceContainerProvider {

    /**
     * Provides access to the currently discovered {@link org.openspaces.admin.gsc.GridServiceContainers}
     * fro a specific deployment.
     * @param deploymentId The deployment id.
     * @return a set of {@link GridServiceContainer} belonging to the requested deployment.
     */
    Set<GridServiceContainer> getContainersForDeployment(final String deploymentId);

}
