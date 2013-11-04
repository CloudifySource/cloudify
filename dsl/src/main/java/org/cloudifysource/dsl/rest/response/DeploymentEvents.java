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

package org.cloudifysource.dsl.rest.response;

import java.util.List;

import org.cloudifysource.domain.MaxSizeList;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/8/13
 * Time: 4:27 PM
 * <br></br>
 *
 * Represents all deployment life cycle events. the deployment can either be a service/application installation/uninstallation.
 */
public class DeploymentEvents {

    private List<DeploymentEvent> events = new MaxSizeList<DeploymentEvent>(100);

    public List<DeploymentEvent> getEvents() {
        return events;
    }

    public void setEvents(final List<DeploymentEvent> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return "DeploymentEvents{" +
                "events=" + events +
                '}';
    }
}
