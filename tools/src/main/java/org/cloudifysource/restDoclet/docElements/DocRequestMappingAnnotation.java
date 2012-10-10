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
		String shortAttrValueStr = null;
		Class<? extends Object> class1 = shortAttrValue.getClass();
		if(class1.isArray() && String.class.equals(class1.getComponentType()))
			shortAttrValueStr = ((String[]) shortAttrValue)[0].trim();
		else if((shortAttrValue instanceof String))
			shortAttrValueStr = ((String) shortAttrValue).trim();
		String shortAttrName = getShortName(attrName);
		
		if(RestDocConstants.REQUEST_MAPPING_VALUE.equals(shortAttrName)) 
			value = shortAttrValueStr.startsWith("/") ? shortAttrValueStr : ("/" + shortAttrValueStr);
		if(RestDocConstants.REQUEST_MAPPING_METHOD.equals(shortAttrName))
			method = (((FieldDocImpl[]) attrValue)[0]).name();
		if(RestDocConstants.REQUEST_MAPPING_HEADERS.equals(shortAttrName))
			headers = shortAttrValueStr;
		if(RestDocConstants.REQUEST_MAPPING_PARAMS.equals(shortAttrName))
			params = shortAttrValueStr;
		if(RestDocConstants.REQUEST_MAPPING_PRODUCES.equals(shortAttrName))
			produces = shortAttrValueStr;
		if(RestDocConstants.REQUEST_MAPPING_CONSUMED.equals(shortAttrName))
			consumes = shortAttrValueStr;
		
		super.addAttribute(shortAttrName, shortAttrValue);
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
			int lastIndexOf = str.lastIndexOf(',');
			if(lastIndexOf != -1)
				str = str.substring(0, lastIndexOf);
			return str + "}";
	}
}
