package org.cloudifysource.dsl.rest.response;

import java.util.Map;

public class ServiceDetails {

	private String name;
	private Map<String, org.openspaces.pu.service.ServiceDetails> instancesDetails;
	
	public ServiceDetails(final String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	public Map<String, org.openspaces.pu.service.ServiceDetails> getInstancesDetails() {
		return instancesDetails;
	}

	public void setInstancesDetails(
			Map<String, org.openspaces.pu.service.ServiceDetails> instancesDetails) {
		this.instancesDetails = instancesDetails;
	}
}
