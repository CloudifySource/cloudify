package org.cloudifysource.dsl.rest.response;

public class DeleteServiceInstanceAttributeResponse {
	
	private String attributeName;
	private Object attributeLastValue;
	
	public String getAttributeName() {
		return attributeName;
	}
	
	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}
	
	public Object getAttributeLastValue() {
		return attributeLastValue;
	}
	
	public void setAttributeLastValue(Object attributeLastValue) {
		this.attributeLastValue = attributeLastValue;
	}
}
