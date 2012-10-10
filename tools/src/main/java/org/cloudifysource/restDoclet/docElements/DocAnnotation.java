package org.cloudifysource.restDoclet.docElements;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.javadoc.AnnotationValue;

public class DocAnnotation {
	String name;
	Map<String, Object> attributes;

	public DocAnnotation(String name) {
		this.name = name;
		attributes = new HashMap<String, Object>();
	}
	
	public String getName() {
		return name;
	}	
	
	protected String getShortName(String name) {
		int beginIndex = name.lastIndexOf(".") + 1;
		int endIndex = name.lastIndexOf("()");
		if(endIndex == -1)
			endIndex = name.length();
		return name.substring(beginIndex, endIndex);
	}

	protected Object getShortValue(Object value) {
		String strValue = value.toString();
		if(!(value instanceof String))
			return value;
		int beginIndex =0;
		int endIndex = strValue.length();
		if(strValue.startsWith("\""))
			beginIndex = 1;
		if(strValue.endsWith("\""))
			endIndex--;
		
		return strValue.substring(beginIndex, endIndex);
	}
	
	public Object constractAttrValue(Object value) {
		Class<? extends Object> class1 = value.getClass();
		if(class1.isArray()) {
			AnnotationValue[] values = (AnnotationValue[])value;
			Object firstValue = values[0].value();
			Object constractedValues = Array.newInstance(firstValue.getClass(), values.length);
			for (int i=0 ; i< values.length; i++) {
				Object currentValue = values[i].value();
				Array.set(constractedValues, i, currentValue);
			}
			//if(values.length == 1)
				//return firstValue;
			return constractedValues;
		}
		else
		{
			return value;
		}
	}

	public void addAttribute(String attrName, Object attrValue) {
		attributes.put(getShortName(attrName), attrValue);
	}

	@Override
	public String toString() {
		String str = "@" + name;
		if(attributes != null && attributes.size() > 0) {
			str += "{";
			for (Entry<String, Object> entry : attributes.entrySet()) {
				str += entry.getKey() + "=" + entry.getValue() + ", ";
			}
			str = str.substring(0, str.lastIndexOf(","));
			str += "}";
		}
		return str;
	}

}
