package org.cloudifysource.restDoclet.docElements;

import org.cloudifysource.restDoclet.constants.RestDocConstants;

public class DocPossibleResponseStatusesAnnotation extends DocAnnotation {

	private Integer[] codes;
	private String[] descriptions;
	
	public DocPossibleResponseStatusesAnnotation(String name) {
		super(name);
	}

	/**
	 * @return the codes
	 */
	public Integer[] getCodes() {
		return codes;
	}

	/**
	 * @return the descriptions
	 */
	public String[] getDescriptions() {
		return descriptions;
	}
	
	@Override
	public void addAttribute(String attrName, Object attrValue) {
		String shortAttrName = getShortName(attrName);
		
		if(shortAttrName.endsWith(RestDocConstants.POSSIBLE_RESPONSE_STATUSES_CODES)) {
			codes = (Integer[])attrValue;
		}
		else if(shortAttrName.endsWith(RestDocConstants.POSSIBLE_RESPONSE_STATUSES_DESCRIPTIONS)) {
			descriptions = (String[])attrValue;
		}
		
		attributes.put(shortAttrName, attrValue);
	}
	
	@Override
	public String toString() {
		String str = "@" + RestDocConstants.POSSIBLE_RESPONSE_STATUSES_ANNOTATION + "[";
		str += "codes: {";
		for (int i = 0; i < codes.length; i++) {		
			if(i != 0)
				str += ", ";
			str += codes[i];
		}
		str += "}, descriptions: {";
		for (int i = 0; i < descriptions.length; i++) {
			if(i != 0)
				str += ", ";
			str += descriptions[i];
		}
		str += "}";
		return str + "]";
	}

}
