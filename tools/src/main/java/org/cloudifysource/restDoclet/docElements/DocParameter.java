package org.cloudifysource.restDoclet.docElements;

import java.util.List;

import org.cloudifysource.restDoclet.constants.RestDocConstants.DocAnnotationTypes;

import com.sun.javadoc.Type;

public class DocParameter {
<<<<<<< Updated upstream
	private final Type type;
	private final String name;
	private String description;
	private String location;
	
	private List<DocAnnotation> annotations;
	private DocRequestParamAnnotation requestParamAnnotation;
=======
	Type type;
	String name;
	String description;
	String location;

	List<DocAnnotation> annotations;
	DocRequestParamAnnotation requestParamAnnotation;
>>>>>>> Stashed changes

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
