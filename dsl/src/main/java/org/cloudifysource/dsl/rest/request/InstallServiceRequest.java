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
package org.cloudifysource.dsl.rest.request;

import org.cloudifysource.dsl.internal.debug.DebugModes;

/**
 * Represents a request to install-service command via the REST Gateway. It holds all the needed parameters to install
 * the service.
 *
 * @author yael
 *
 */
public class InstallServiceRequest {

    private String serviceFolderUploadKey;
    private String cloudConfigurationUploadKey;
    private String cloudOverridesUploadKey;
    private String serviceOverridesUploadKey;
    private String serviceFileName;
    private String authGroups;
    private Boolean selfHealing = true;
    private boolean debugAll;
    private String debugEvents;
    
    private String debugMode = DebugModes.INSTEAD.getName();

    public String getServiceFolderUploadKey() {
        return serviceFolderUploadKey;
    }

    public void setServiceFolderUploadKey(final String serviceFolderUploadKey) {
        this.serviceFolderUploadKey = serviceFolderUploadKey;
    }

    public String getCloudConfigurationUploadKey() {
        return cloudConfigurationUploadKey;
    }

    public void setCloudConfigurationUploadKey(final String cloudConfigurationUploadKey) {
        this.cloudConfigurationUploadKey = cloudConfigurationUploadKey;
    }

    public String getCloudOverridesUploadKey() {
        return cloudOverridesUploadKey;
    }

    public void setCloudOverridesUploadKey(final String cloudOverridesUploadKey) {
        this.cloudOverridesUploadKey = cloudOverridesUploadKey;
    }

    public String getServiceOverridesUploadKey() {
        return serviceOverridesUploadKey;
    }

    public void setServiceOverridesUploadKey(final String serviceOverridesUploadKey) {
        this.serviceOverridesUploadKey = serviceOverridesUploadKey;
    }

    public String getServiceFileName() {
        return serviceFileName;
    }

    public void setServiceFileName(final String serviceFileName) {
        this.serviceFileName = serviceFileName;
    }

    public String getAuthGroups() {
        return authGroups;
    }

    public void setAuthGroups(final String authGroups) {
        this.authGroups = authGroups;
    }

    public Boolean getSelfHealing() {
        return selfHealing;
    }

    public void setSelfHealing(final Boolean selfHealing) {
        this.selfHealing = selfHealing;
    }

    public boolean isDebugAll() {
        return debugAll;
    }

    public void setDebugAll(final boolean debugAll) {
        this.debugAll = debugAll;
    }

    public String getDebugEvents() {
        return debugEvents;
    }

    public void setDebugEvents(final String debugEvents) {
        this.debugEvents = debugEvents;
    }

    public String getDebugMode() {
        return debugMode;
    }

    public void setDebugMode(final String debugMode) {
        this.debugMode = debugMode;
    }
}
