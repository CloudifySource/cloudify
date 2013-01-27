package org.cloudifysource.dsl.rest.request;

import java.util.Map;

public class SetApplicationAttributesRequest {
	
	private Map<String, Object> attributes;

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
}
