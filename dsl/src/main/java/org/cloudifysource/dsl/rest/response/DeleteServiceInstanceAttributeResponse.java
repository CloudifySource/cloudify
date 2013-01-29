package org.cloudifysource.dsl.rest.response;

/**
 * A POJO representing a response to deleteServiceInstanceAttribute command via the REST Gateway.
 * It holds the requested attribute name and its last value in the attributes store.
 * 
 * This POJO will be used when constructing the {@link Response} object by calling {@code Response#setResponse(Object)}
 * @author elip
 *
 */
public class DeleteServiceInstanceAttributeResponse {
	
	private String attributeName;
	private Object attributeLastValue;
	
	public String getAttributeName() {
		return attributeName;
	}
	
	public void setAttributeName(final String attributeName) {
		this.attributeName = attributeName;
	}
	
	public Object getAttributeLastValue() {
		return attributeLastValue;
	}
	
	public void setAttributeLastValue(final Object attributeLastValue) {
		this.attributeLastValue = attributeLastValue;
	}
}
