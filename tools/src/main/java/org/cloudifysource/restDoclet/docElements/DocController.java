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

import java.util.Map.Entry;
import java.util.SortedMap;

public class DocController {
	private String name;
	private String uri;
	private String description;
	private SortedMap<String, DocMethod> methods;

	public DocController(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SortedMap<String, DocMethod> getMethods() {
		return methods;
	}

	public void setMethods(SortedMap<String, DocMethod> methods) {
		this.methods = methods;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("Controller " + name + " uri = " + uri + "\n");
		if (description != null && !description.isEmpty())
			builder.append(description + "\n");		
		if (methods != null) {
			for (Entry<String, DocMethod> entry : methods.entrySet()) {
				builder.append(entry.getValue().toString() + "\n");
			}
		}
		return builder.toString();
	}

}
