package com.gigaspaces.cloudify.dsl.context.kvstorage;

import com.gigaspaces.cloudify.dsl.context.kvstorage.spaceentries.InstanceCloudifyAttribute;

public class InstanceAttributesAccessor extends AbstractAttributesAccessor {

	private final String serviceName;
	private final int instanceId;

	public InstanceAttributesAccessor(AttributesFacade attributesFacade,
			String applicationName, String serviceName, int instanceId) {
		super(attributesFacade, applicationName);
		this.serviceName = serviceName;
		this.instanceId = instanceId;
	}

	@Override
	protected InstanceCloudifyAttribute prepareAttributeTemplate() {
		InstanceCloudifyAttribute attribute = new InstanceCloudifyAttribute();
		attribute.setInstanceId(instanceId);
		attribute.setServiceName(serviceName);
		return attribute;
	}

}
