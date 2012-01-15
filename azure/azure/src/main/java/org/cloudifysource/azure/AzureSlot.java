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

public enum AzureSlot {
    Staging("staging"), Production("production");
    
    private String slot;
    
    private AzureSlot(String slot) {
        this.slot = slot;
    }
    
    public String getSlot() {
        return slot;
    }
    
    public static AzureSlot fromString(String slot) {
        for (AzureSlot azureSlot : values()) {
            if (azureSlot.getSlot().equals(slot)) {
                return azureSlot;
            }
        }
        throw new IllegalArgumentException("Invalid azure slot: " + slot);
    }
    
}
