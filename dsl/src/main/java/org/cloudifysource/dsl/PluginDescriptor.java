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
package org.cloudifysource.dsl;

import java.io.Serializable;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


@CloudifyDSLEntity(name="plugin", clazz=PluginDescriptor.class, allowInternalNode = true, allowRootNode = false, parent = "service")
public class PluginDescriptor  implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String className;
    private Map<String, Object> config;
    private String name;
    
    
    public Map<String, Object> getConfig() {
        return config;
    }
    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    
    
}
