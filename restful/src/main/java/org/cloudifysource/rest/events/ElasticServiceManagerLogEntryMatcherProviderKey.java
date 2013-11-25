/*
 * ******************************************************************************
 *  * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  ******************************************************************************
 */

package org.cloudifysource.rest.events;

import org.openspaces.admin.esm.ElasticServiceManager;

/**
 * Key for log entry matchers. <br></br>
 *
 * manager - the ElasticServiceManager this matcher will match logs from. <br></br>
 * deploymentId - the operation of this matcher was dedicated to. <br></br>
 *
 * @author Eli Polonsky
 */
public class ElasticServiceManagerLogEntryMatcherProviderKey {

    private ElasticServiceManager manager;
    private String deploymentId;

    public ElasticServiceManager getManager() {
        return manager;
    }

    public void setManager(final ElasticServiceManager manager) {
        this.manager = manager;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(final String deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElasticServiceManagerLogEntryMatcherProviderKey that = (ElasticServiceManagerLogEntryMatcherProviderKey) o;

        if (deploymentId != null ? !deploymentId.equals(that.deploymentId) : that.deploymentId != null) return false;
        if (manager != null ? !manager.equals(that.manager) : that.manager != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = manager != null ? manager.hashCode() : 0;
        result = 31 * result + (deploymentId != null ? deploymentId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ElasticServiceManagerLogEntryMatcherProviderKey{" + "manager=" + manager
                + ", deploymentId='" + deploymentId + '\'' + '}';
    }
}
