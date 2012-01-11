package org.cloudifysource.dsl.context.kvstorage.spaceentries;

/**
 * Instance level property
 * @author eitany
 * @since 2.0
 */
public class InstanceCloudifyAttribute extends AbstractCloudifyAttribute {

	private String serviceName;
	private Integer instanceId;

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	public String getServiceName() {
		return serviceName;
	}
	
	public void setInstanceId(Integer instanceId) {
		this.instanceId = instanceId;
	}
	
	public Integer getInstanceId() {
		return instanceId;
	}

}
