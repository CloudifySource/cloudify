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
