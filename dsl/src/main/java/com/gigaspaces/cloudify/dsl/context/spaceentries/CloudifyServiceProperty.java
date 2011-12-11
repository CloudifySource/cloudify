package com.gigaspaces.cloudify.dsl.context.spaceentries;

/**
 * Service level property
 * @author eitany
 * @since 2.0
 */
public class CloudifyServiceProperty extends AbstractCloudifyProperty {
	
	private String serviceName;

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
}
