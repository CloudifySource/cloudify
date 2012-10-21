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

import java.util.List;

import org.cloudifysource.restDoclet.constants.RestDocConstants.DocAnnotationTypes;

import com.sun.javadoc.Type;

public class DocParameter {
	private final Type type;
	private final String name;
	private String description;
	private String location;
	
	private List<DocAnnotation> annotations;
	private DocRequestParamAnnotation requestParamAnnotation;


	public DocParameter(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	public Type getType() {
		return type;
	}
	public String getName() {
		return name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Boolean isRequired() {
		if(requestParamAnnotation != null)
			return requestParamAnnotation.isRequierd() == null ? Boolean.FALSE : requestParamAnnotation.isRequierd();
		return Boolean.TRUE;
	}
	public List<DocAnnotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<DocAnnotation> annotations) {
			this.annotations = annotations;
			setAnnotationsAttributes();
	}
	public String getLocation() {
		return location;
	}

	public String getDefaultValue() {
		if(requestParamAnnotation != null )
			return requestParamAnnotation.getDefaultValue();
		return null;
	}
	public DocRequestParamAnnotation getRequestParamAnnotation() {
		return requestParamAnnotation;
	}

	private void setAnnotationsAttributes() {
		if(annotations == null)
			return;
		String currLocation = "";
		for (DocAnnotation docAnnotation : annotations) {
			String annotationName = docAnnotation.getName();
			if(!currLocation.isEmpty())
				currLocation += " or ";
			currLocation += annotationName;
			DocAnnotationTypes docAnnotationType = DocAnnotationTypes.fromName(annotationName);
			if(docAnnotationType == DocAnnotationTypes.REQUEST_PARAM) {
				if(!(docAnnotation instanceof DocRequestParamAnnotation)) 
					throw new ClassCastException("Annotation type is " + DocAnnotationTypes.REQUEST_PARAM + ", expected class type to be " +  DocRequestParamAnnotation.class.getName());
					requestParamAnnotation = (DocRequestParamAnnotation) docAnnotation;
			}
			else if(docAnnotationType != DocAnnotationTypes.PATH_VARIABLE && docAnnotationType != DocAnnotationTypes.REQUEST_BODY)
				throw new IllegalArgumentException("Unsupported parameter annotation - " + annotationName);
		}
		this.location = currLocation;
	}

	@Override
	public String toString() {
		String str = "Parameter[";
		if(annotations !=null) {
			if(annotations.size() == 1)
				str += annotations.get(0) + ", "; 
			else
				str += annotations + ", ";
		}
		str += "type = " + type + ", name = " + name;
		if(description != null)
			str += description;
		return str + "]";
	}

}
