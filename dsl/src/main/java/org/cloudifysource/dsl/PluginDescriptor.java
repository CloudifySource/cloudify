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
