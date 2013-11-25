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

import org.openspaces.admin.gsc.GridServiceContainer;

/**
 * Created with IntelliJ IDEA.
 * User: elip
 * Date: 5/20/13
 * Time: 2:45 PM
 * <br></br>
 * Key for log entry matchers. <br></br>
 *
 * container - the GridServiceContainer this matcher will match logs from. <br></br>
 * deploymentId - the operation if this matcher was deidcated to. <br></br>
 */
public class ContainerLogEntryMatcherProviderKey {

    private GridServiceContainer container;
    private String deploymentId;

    public void setContainer(final GridServiceContainer container) {
        this.container = container;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(final String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContainerLogEntryMatcherProviderKey that = (ContainerLogEntryMatcherProviderKey) o;

        return container.getUid().equals(that.container.getUid())
                && deploymentId.equals(that.deploymentId);

    }

    @Override
    public int hashCode() {
        int result = container.hashCode();
        result = 31 * result + deploymentId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LogEntryMatcherProviderKey{" + "container=" + container.getUid() + container.getExactZones().getZones()
                + ", deploymentId='" + deploymentId + '\'' + '}';
    }
}
