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


/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/13/13
 * Time: 8:41 AM
 * <br/><br/>
 *
 * Key for the events cache. containing just the deployment id for now.
 * The deployment id is a unique string identifying the deployment, retained at installation time.
 */
public class EventsCacheKey {

    private String deploymentId;

    public EventsCacheKey(final String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EventsCacheKey that = (EventsCacheKey) o;

        return deploymentId.equals(that.deploymentId);

    }

    @Override
    public int hashCode() {
        return deploymentId.hashCode();
    }

    @Override
    public String toString() {
        return "EventsCacheKey{" + "deploymentId='" + deploymentId + '\'' + '}';
    }
}
