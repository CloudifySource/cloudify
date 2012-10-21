/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
			str += "description: \n" + description + "\n";
		}
		if(httpMethods != null) {
			str += "httpMethods:\n";
			StringBuilder httpMethodsStr = new StringBuilder();
			for (DocHttpMethod httpMethod : httpMethods) {
				httpMethodsStr.append(httpMethod);
				httpMethodsStr.append("\n");
			}
			str += httpMethodsStr;
		}
		return str;
	}
}
