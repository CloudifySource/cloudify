package org.cloudifysource.dsl.context.kvstorage.spaceentries;

/**
 * Service level property
 * @author eitany
 * @since 2.0
 */
public class ServiceCloudifyAttribute extends AbstractCloudifyAttribute {
	
	private String serviceName;

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
}
