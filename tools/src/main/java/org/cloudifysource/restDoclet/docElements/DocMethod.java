package org.cloudifysource.restDoclet.docElements;

import java.util.LinkedList;
import java.util.List;

public class DocMethod {
	private String uri;
	private String description;
	private final List<DocHttpMethod> httpMethods;
	
	public DocMethod(DocHttpMethod httpMethod) {
		httpMethods = new LinkedList<DocHttpMethod>();
		httpMethods.add(httpMethod);
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public List<DocHttpMethod> getHttpMethods() {
		return httpMethods;
	}
	public void addHttpMethod(DocHttpMethod httpMethod) {
		this.httpMethods.add(httpMethod);
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String toString() {
		String str = "\nmapping: " + uri + "\n";
		if(description != null && !description.isEmpty()) {
			str += "description: \n";
			String[] split = description.trim().split("\n");
			for (String string : split) {
				str += "   " + string + "\n";				
			}
		}
		if(httpMethods != null) {
			str += "httpMethods:\n";
		for (DocHttpMethod httpMethod : httpMethods) {
			str += httpMethod + "\n";
		}
		}
		return str;
	}
}
