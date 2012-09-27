package org.cloudifysource.restDoclet.docElements;

import com.sun.javadoc.Type;

public class DocReturnDetails {
	private Type returnType;
	private String description;
	
	public Type getReturnType() {
		return returnType;
	}
	public void setReturnType(Type type) {
		this.returnType = type;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		String str = returnType.typeName();
		if(description != null)
		str += ": " + description;
		return str;
	}
}
