package org.cloudifysource.dsl.rest.response;

/**
 * A POJO representing a response to deleteServiceInstanceAttribute command via the REST Gateway.
 * It holds the previous value of deleted attribute 
 * 
 * This POJO will be used when constructing the {@link Response} object by calling {@code Response#setResponse(Object)}
 * @author elip
 *
 */
public class DeleteServiceInstanceAttributeResponse {
	
	private Object previousValue;

	public Object getPreviousValue() {
		return previousValue;
	}

	public void setPreviousValue(Object previousValue) {
		this.previousValue = previousValue;
	}
	
	
}
