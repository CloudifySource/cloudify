package com.gigaspaces.cloudify.dsl;

import java.util.LinkedList;
import java.util.List;

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
