package com.gigaspaces.cloudify.dsl;

import java.util.Map;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name="cloud2", clazz=Cloud2.class, allowInternalNode = false, allowRootNode = true)
public class Cloud2 {

	private CloudProvider provider;
	private CloudUser user;
	private Map<String, CloudTemplate> templates;
	
	public CloudProvider getProvider() {
		return provider;
	}

	public void setProvider(CloudProvider provider) {
		this.provider = provider;
	}

	public CloudUser getUser() {
		return user;
	}

	public void setUser(CloudUser user) {
		this.user = user;
	}

	public Map<String, CloudTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(Map<String, CloudTemplate> templates) {
		this.templates = templates;
	}
}
