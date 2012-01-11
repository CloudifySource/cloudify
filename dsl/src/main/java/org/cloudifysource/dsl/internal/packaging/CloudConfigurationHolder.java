package org.cloudifysource.dsl.internal.packaging;

import com.gigaspaces.annotation.pojo.SpaceId;


public class CloudConfigurationHolder {

	private String cloudConfiguration;
	private Long id = (long) 1;
	
	public CloudConfigurationHolder() {
		
	}
	
	public CloudConfigurationHolder(final String cloudConfigution) {
		this.cloudConfiguration = cloudConfigution;		
	}
	
	@SpaceId
	public Long getId() { 
		return id;
	}
	
	public void setId(final Long id) {
		this.id = id;
	}
	public String getCloudConfiguration() {
		return this.cloudConfiguration;
	}
	
	public void setCloudConfiguration(final String cloudConfiguration) { 
		this.cloudConfiguration = cloudConfiguration;
	}
	
}
