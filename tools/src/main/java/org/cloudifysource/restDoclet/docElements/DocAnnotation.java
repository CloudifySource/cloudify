package org.cloudifysource.restDoclet.docElements;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.javadoc.AnnotationValue;

public class DocAnnotation {
	private final String name;
	private final Map<String, Object> attributes;

	public DocAnnotation(String name) {
		this.name = name;
		attributes = new HashMap<String, Object>();
	}

	public String getName() {
		return name;
	}	

	protected static String getShortName(String name) {
		int beginIndex = name.lastIndexOf('.') + 1;
		int endIndex = name.lastIndexOf("()");
		if(endIndex == -1)
			endIndex = name.length();
		return name.substring(beginIndex, endIndex);
	}

	protected static Object getShortValue(Object value) {
		if(!(value instanceof String))
			return value;

		String strValue = (String)value;

		int beginIndex = 0;
		if(strValue.startsWith("\""))
			beginIndex = 1;

		int endIndex = strValue.length();
		if(strValue.endsWith("\""))
			endIndex--;

		return strValue.substring(beginIndex, endIndex);
	}

	public static Object constractAttrValue(Object value) {
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
		return value;
	}

	public void addAttribute(String attrName, Object attrValue) {
		attributes.put(getShortName(attrName), attrValue);
	}

	@Override
	public String toString() {
		String str = "@" + name;
		if(attributes != null && attributes.size() > 0) {
			StringBuilder attrStr = new StringBuilder();
			for (Entry<String, Object> entry : attributes.entrySet()) {
<<<<<<< Updated upstream
				str += entry.getKey() + "=" + entry.getValue() + ", ";
=======
				attrStr.append(entry.getKey())
				.append("=")
				.append(entry.getValue().toString())
				.append(", ");
>>>>>>> Stashed changes
			}
			if(attrStr.length() == 0)
				str += " {No attributes}";
			else
				str += " {" + attrStr.substring(0, str.lastIndexOf(',')) + "}";
		}
		return str;
	}

}
