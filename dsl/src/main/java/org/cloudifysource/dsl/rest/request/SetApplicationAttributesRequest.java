package org.cloudifysource.dsl.rest.request;

import java.util.Map;

/**
 * A POJO representing a request to setApplicationAttributes command via the REST Gateway.
 * It holds the requested attributes names and values in a map.
 * 
 * A JSON serialization of this POJO may be used as the request body of the above mentioned REST command.
 * @author elip
 *
 */
public class SetApplicationAttributesRequest {
	
	private Map<String, Object> attributes;

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(final Map<String, Object> attributes) {
		this.attributes = attributes;
	}
}
