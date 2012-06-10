/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.azure;

//CR: Where did you get the strings from ? according to http://msdn.microsoft.com/en-us/library/ee460804.aspx it is different
//CR: Document which Azure API version this enum conforms to
public enum AzureDeploymentStatus {
    RUNNING("Running"), 
    SUSPENDED("Suspended"), 
    RUNNING_TRANSITIONING("RunningTransitioning"),
    SUSPENDED_TRANSITIONING("SuspendedTransitioning"),
    STARTING("Starting"),
    SUSPENDING("Suspending"),
    DEPLOYING("Deploying"), 
    DELETING("Deleting"),
    
    // Not actual status, possible output of azureconfig
    NOT_FOUND("NotFound"), 
    INTERNAL_SERVER_ERROR("Intenal Server Error");
    
    private String status;
    
    private AzureDeploymentStatus(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public static AzureDeploymentStatus fromString(String status) {
        for (AzureDeploymentStatus deploymentStatus : values()) {
            if (deploymentStatus.getStatus().equals(status)) {
                return deploymentStatus;
            }
        }
        throw new IllegalArgumentException("No such status: " + status);
    }
}
