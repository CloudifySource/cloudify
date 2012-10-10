package org.cloudifysource.restDoclet.docElements;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.restDoclet.constants.RestDocConstants;
import org.cloudifysource.restDoclet.generation.Utils;
import org.codehaus.jackson.JsonParseException;

public class DocJsonRequestExample extends DocAnnotation {

	private String requestJsonBody;
	private String comments;

	public DocJsonRequestExample(String name) {
		super(name);
	}

	public String getComments() {
		return comments;
	}

	public String generateJsonRequestBody() throws JsonParseException, IOException {
		return Utils.getIndentJson(requestJsonBody);
	}

	@Override
	public void addAttribute(String attrName, Object attrValue) {
		String value = attrValue.toString().trim();

		String shortAttrName = getShortName(attrName);

		if(RestDocConstants.JSON_REQUEST_EXAMPLE_REQUEST_PARAMS.equals(shortAttrName))
			requestJsonBody = value;
		else if(RestDocConstants.JSON_REQUEST_EXAMPLE_COMMENTS.equals(shortAttrName))
			comments = value;

		super.addAttribute(shortAttrName, value);
	}

	@Override
	public String toString() {
		String str = "@" + RestDocConstants.JSON_REQUEST_EXAMPLE_ANNOTATION + "[";
		boolean isEmpty = true;
		if(!StringUtils.isBlank(requestJsonBody)) {
			str += "requestJsonBody = " + requestJsonBody;
			isEmpty = false;
		}
		if(!StringUtils.isBlank(comments)) {
			if(!isEmpty)
				str += ", ";
			str += "comments = " + comments;
			isEmpty = false;
		}
		if (isEmpty)
			str += "annotation has no attributes";
		return str + "]";
	}



}
