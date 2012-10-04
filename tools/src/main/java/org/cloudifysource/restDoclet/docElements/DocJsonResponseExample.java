package org.cloudifysource.restDoclet.docElements;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.restDoclet.constants.RestDocConstants;
import org.cloudifysource.restDoclet.generation.Utils;
import org.codehaus.jackson.JsonParseException;


public class DocJsonResponseExample extends DocAnnotation{
	private String status;
	private String response;
	private String comments;

	public DocJsonResponseExample(String name) {
		super(name);
	}
	
	public String getComments() {
		return comments;
	}
	
	public String generateJsonResponseBody() throws JsonParseException, IOException {
		String jsonResponseBody = "{\"status\": \"" + status + "\"";
		
		if (StringUtils.isBlank(response)) {
			jsonResponseBody += "}";
		}
		else {
			jsonResponseBody += ",\"response\": " + response + "}";
			jsonResponseBody = Utils.getIndentJson(jsonResponseBody);
		}
		
		return jsonResponseBody;
	}

	@Override
	public void addAttribute(String attrName, Object attrValue) {
		String value = attrValue.toString().replace("\\\"", "\"").trim();

//		if(value.startsWith("\"") && value.endsWith("\""))
//			value = value.substring(1,value.length()-1);
		
		String shortAttrName = getShortName(attrName);

		if(shortAttrName.equals(RestDocConstants.JSON_RESPONSE_EXAMPLE_STATUS))
			status = value;
		if(shortAttrName.equals(RestDocConstants.JSON_RESPONSE_EXAMPLE_RESPONSE)) {
			response = value;
		}
		else if(shortAttrName.equals(RestDocConstants.JSON_RESPONSE_EXAMPLE_COMMENTS))
			comments = value;

		attributes.put(shortAttrName, value);
	}

	@Override
	public String toString() {
		String str = "@" + RestDocConstants.JSON_RESPONSE_EXAMPLE_ANNOTATION + "[status = " + status;
		if(!StringUtils.isBlank(response))
			str += ", response = " + response;
		if(!StringUtils.isBlank(comments))
			str += ", comments = " + comments;
		return str + "]";
	}
}
