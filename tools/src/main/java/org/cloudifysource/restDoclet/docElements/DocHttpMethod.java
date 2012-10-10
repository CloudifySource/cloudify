package org.cloudifysource.restDoclet.docElements;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.restDoclet.constants.RestDocConstants.ResponseCodes;

public class DocHttpMethod {
	private String methodSignatureName;
	private String httpMethodName;
	private String description;
	
	List<DocParameter> params;
	List<DocParameter> annotatedParams;
	Map<String, String> requestParamAnnotationParamValues;
	
	private DocReturnDetails returnDetails;
	
	private DocJsonRequestExample jsonRequestExample;
	private DocJsonResponseExample jsonResponseExample;
	private List<DocResponseStatus> possibleResponseStatuses;
	
	
	public DocHttpMethod(String methodSignatureName, String requestMethod) {
		this.methodSignatureName = methodSignatureName;
		this.httpMethodName = requestMethod;
	}
	
	public String getMethodSignatureName() {
		return methodSignatureName;
	}
	
	public String getHttpMethodName() {
		return httpMethodName;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String commentText) {
		description = commentText;
		
	}
	
	public List<DocParameter> getParams() {
		return params;
	}
	public void setParams(List<DocParameter> params) {
		this.params = params;
		setAnnotatedParams();
	}
	public List<DocParameter> getAnnotatedParams() {
		return annotatedParams;
	}
	public void setAnnotatedParams() {
		if(params == null)
			return;
		for (DocParameter docParameter : params) {
			DocRequestParamAnnotation requestParamAnnotation = docParameter.getRequestParamAnnotation();
			if(requestParamAnnotation != null) {
				if(requestParamAnnotationParamValues == null)
					requestParamAnnotationParamValues = new HashMap<String, String>();
				requestParamAnnotationParamValues.put(docParameter.getName(), ("<" + docParameter.getType() + ">"));
			}
			List<DocAnnotation> annotations = docParameter.getAnnotations();
			if(annotations != null && !annotations.isEmpty()) {
				if(annotatedParams == null)
					annotatedParams = new LinkedList<DocParameter>();
				annotatedParams.add(docParameter);
			}
		}
	}
	
	public DocReturnDetails getReturnDetails() {
		return returnDetails;
	}
	public void setReturnDetails(DocReturnDetails returnDetails) {
		this.returnDetails = returnDetails;
	}
	
	public DocJsonResponseExample getJsonResponseExample() {
		return jsonResponseExample;
	}
	public void setJsonResponseExample(DocJsonResponseExample jsonResponseExample) {
		this.jsonResponseExample = jsonResponseExample;
	}
	public DocJsonRequestExample getJsonRequestExample() {
		return jsonRequestExample;
	}
	public void setJsonRequesteExample(DocJsonRequestExample request) {
		this.jsonRequestExample = request;
	}
	public List<DocResponseStatus> getPossibleResponseStatuses() {
		return possibleResponseStatuses;
	}
	public void setPossibleResponseStatuses(Integer[] codes, String[] descriptions) {
		if(this.possibleResponseStatuses == null)
			this.possibleResponseStatuses = new LinkedList<DocResponseStatus>();
		for (int i = 0; i < codes.length; i++) {
			int code = codes[i];
			DocResponseStatus possibleResponseStatus = new DocResponseStatus(code, ResponseCodes.fromCode(code).getReasonPhrase() , descriptions[i].trim());
			this.possibleResponseStatuses.add(possibleResponseStatus);
		}
	}

	@Override
	public String toString() {
		String httpMethodShort = httpMethodName.substring(httpMethodName.lastIndexOf('.')+1);
		String str = "http method: " + httpMethodShort + "\n";
		if(StringUtils.isBlank(description))
			str += "description: " + description + "\n";
		if(params != null && !params.isEmpty()) {
			StringBuilder paramsStr = new StringBuilder();
			for (DocParameter param : params) {
				paramsStr.append("   ").append(param).append("\n");
			}
			str += 	"parameters: \n" + paramsStr;		
		}
		str += "returns " + returnDetails;
		if(jsonResponseExample != null)
			str += "Response example: " + jsonResponseExample + "\n";
		if(jsonRequestExample != null)
			str += "Request example: " + jsonRequestExample + "\n";
		
		if(possibleResponseStatuses != null) {
			StringBuilder responseStatusStr = new StringBuilder();
			for (DocResponseStatus responseStatus : possibleResponseStatuses) {
				responseStatusStr.append("* ").append(responseStatus).append("\n");
			}
			str += "Returns: " + responseStatusStr;
		}
		return str;
	}
	
}
