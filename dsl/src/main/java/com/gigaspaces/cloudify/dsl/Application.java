package com.gigaspaces.cloudify.dsl;

import java.util.LinkedList;
import java.util.List;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name="application", clazz=Application.class, allowInternalNode = false, allowRootNode = true)
public class Application {

    private String name;
    
    private List<Service> services = new LinkedList<Service>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

	@Override
	public String toString() {
		return "Application [name=" + name + ", services=" + services + "]";
	}

    

	
  

	

    
    
}
