package com.gigaspaces.cloudify.dsl.cloud;

import java.util.HashMap;
import java.util.Map;

import com.gigaspaces.cloudify.dsl.internal.CloudifyDSLEntity;

@CloudifyDSLEntity(name="cloud", clazz=Cloud2.class, allowInternalNode = false, allowRootNode = true)
public class Cloud2 {

	private String name;
	private CloudProvider provider;
	private CloudUser user = new CloudUser();
	private CloudConfiguration configuration = new CloudConfiguration();
	private Map<String, CloudTemplate> templates = new HashMap<String, CloudTemplate>();
	private Map<String, Object> custom = new HashMap<String, Object>();
	
	
	public Map<String, Object> getCustom() {
		return custom;
	}

	public void setCustom(Map<String, Object> custom) {
		this.custom = custom;
	}

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

	public CloudConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(CloudConfiguration configuration) {
		this.configuration = configuration;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Cloud [name=" + name + ", provider=" + provider + ", user=" + user + ", configuration="
				+ configuration + ", templates=" + templates + ", custom=" + custom + "]";
	}
	
	
	
}
