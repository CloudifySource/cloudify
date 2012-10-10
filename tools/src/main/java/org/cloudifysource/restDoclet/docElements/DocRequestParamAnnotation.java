package org.cloudifysource.restDoclet.docElements;

import org.cloudifysource.restDoclet.constants.RestDocConstants;

public class DocRequestParamAnnotation extends DocAnnotation {

	public DocRequestParamAnnotation(String name) {
		super(name);
	}
	
	private String value;
	private String defaultValue;
	private Boolean requierd;
	
	public String getValue() {
		return value;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public Boolean isRequierd() {
		return requierd;
	}
	
	@Override
	public void addAttribute(String attrName, Object attrValue) {
		Object shortAttrValue = getShortValue(attrValue);
		String shortAttrValueStr = (shortAttrValue instanceof String) ? ((String) shortAttrValue).trim() : null;
		String shortAttrName = getShortName(attrName);
		
		if(RestDocConstants.REQUEST_PARAMS_VALUE.equals(shortAttrName))
			value = shortAttrValueStr;
		if(RestDocConstants.REQUEST_PARAMS_DEFAULT_VALUE.equals(shortAttrName))
			defaultValue = shortAttrValueStr;
		if(RestDocConstants.REQUEST_PARAMS_REQUIRED.equals(shortAttrName))
			requierd = (Boolean) attrValue;
		
		super.addAttribute(shortAttrName, shortAttrValue);
	}
	
	@Override
	public String toString() {
		String str = "@" + RestDocConstants.REQUEST_PARAMS_ANNOTATION;
		if(value != null || defaultValue != null || requierd != null)
			str += "{";
		if(value != null)
			str	+= RestDocConstants.REQUEST_PARAMS_VALUE + " = \"" + value + "\"";
		if(defaultValue != null)
			str	+= RestDocConstants.REQUEST_PARAMS_DEFAULT_VALUE + " = \"" + defaultValue + "\"";
		if(requierd != null)
			str	+= RestDocConstants.REQUEST_PARAMS_REQUIRED + " = \"" + requierd + "\"";
		return str + "}";
	}
	

}
