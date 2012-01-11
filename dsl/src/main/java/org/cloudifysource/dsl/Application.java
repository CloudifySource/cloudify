package org.cloudifysource.dsl;

import java.util.LinkedList;
import java.util.List;

import org.cloudifysource.dsl.internal.CloudifyDSLEntity;


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


	// This is a hack, but it allows the application DSL to work with the existing DSL base script.
	public void setService(final Service service) {
		this.services.add(service);
	}
	public Service getService() {
		if(this.getServices().size() == 0) {
			return null;
		} else {
			return this.services.get(this.services.size() -1);
		}
		
	}

	
  

	

    
    
}
