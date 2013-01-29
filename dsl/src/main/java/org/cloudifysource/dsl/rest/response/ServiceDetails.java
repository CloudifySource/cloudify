package org.cloudifysource.dsl.rest.response;

import java.util.List;

/**
 * A POJO representing Metadata about a Service.
 * @author elip
 *
 */
public class ServiceDetails {

	private String name;
	private String applicationName;
	private int numberOfInstances;
	private List<String> instanceNames;
	
	public ServiceDetails() {
	}
	
	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(final String applicationName) {
		this.applicationName = applicationName;
	}

	public int getNumberOfInstances() {
		return numberOfInstances;
	}

	public void setNumberOfInstances(final int numberOfInstances) {
		this.numberOfInstances = numberOfInstances;
	}

	public List<String> getInstanceNames() {
		return instanceNames;
	}

	public void setInstanceNames(final List<String> instanceNames) {
		this.instanceNames = instanceNames;
	}

	public String getName() {
		return name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
}
