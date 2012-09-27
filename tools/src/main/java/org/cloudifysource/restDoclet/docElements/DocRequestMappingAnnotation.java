package org.cloudifysource.restDoclet.docElements;

import org.cloudifysource.restDoclet.constants.RestDocConstants;

import com.sun.tools.javadoc.FieldDocImpl;

public class DocRequestMappingAnnotation extends DocAnnotation {
	private String value;
	private String method;
	private String headers;
	private String params;
	private String produces;
	private String consumes;
	
	public DocRequestMappingAnnotation(String name) {
		super(name);
	}
	public String getValue() {
		return value;
	}
	public String getMethod() {
		return method;
	}
	public String getHeaders() {
		return headers;
	}
	public String getParams() {
		return params;
	}
	public String getProduces() {
		return produces;
	}
	public String getConsumes() {
		return consumes;
	}

	@Override
	public void addAttribute(String attrName, Object attrValue) {
		Object shortAttrValue = getShortValue(attrValue);
		String shortAttrValueStr = (shortAttrValue instanceof String[]) 
				? ((String[]) shortAttrValue)[0].trim() : 
					( (shortAttrValue instanceof String) 
							? ((String) shortAttrValue).trim() : null);
		String shortAttrName = getShortName(attrName);
		
		if(shortAttrName.equals(RestDocConstants.REQUEST_MAPPING_VALUE)) 
			value = shortAttrValueStr.startsWith("/") ? shortAttrValueStr : ("/" + shortAttrValueStr);
		if(shortAttrName.equals(RestDocConstants.REQUEST_MAPPING_METHOD))
			method = (((FieldDocImpl[]) attrValue)[0]).name();
		if(shortAttrName.equals(RestDocConstants.REQUEST_MAPPING_HEADERS))
			headers = shortAttrValueStr;
		if(shortAttrName.equals(RestDocConstants.REQUEST_MAPPING_PARAMS))
			params = shortAttrValueStr;
		if(shortAttrName.equals(RestDocConstants.REQUEST_MAPPING_PRODUCES))
			produces = shortAttrValueStr;
		if(shortAttrName.equals(RestDocConstants.REQUEST_MAPPING_CONSUMED))
			consumes = shortAttrValueStr;
		
		attributes.put(shortAttrName, shortAttrValue);
	}
	
	@Override
	public String toString() {
			String str = "@" + RestDocConstants.REQUEST_MAPPING_ANNOTATION + "{";
			if(value != null)
				str	+= RestDocConstants.REQUEST_MAPPING_VALUE + " = \"" + value + "\", ";
			if(method != null)
				str	+= RestDocConstants.REQUEST_MAPPING_METHOD + " = \"" + method + "\", ";
			if(headers != null)
				str	+= RestDocConstants.REQUEST_MAPPING_HEADERS + " = \"" + headers + "\", ";
			if(params != null)
				str	+= RestDocConstants.REQUEST_MAPPING_PARAMS + " = \"" + params + "\", ";
			if(produces != null)
				str	+= RestDocConstants.REQUEST_MAPPING_PRODUCES + " = \"" + produces + "\", ";
			if(consumes != null)
				str	+= RestDocConstants.REQUEST_MAPPING_CONSUMED + " = \"" + consumes + "\", ";
			if(str.lastIndexOf(",") != -1)
				str = str.substring(0, str.lastIndexOf(","));
			str += "}";
			return str;
	}
}
