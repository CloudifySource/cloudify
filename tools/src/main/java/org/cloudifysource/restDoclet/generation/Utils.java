package org.cloudifysource.restDoclet.generation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.restDoclet.constants.RestDocConstants;
import org.cloudifysource.restDoclet.docElements.DocAnnotation;
import org.cloudifysource.restDoclet.docElements.DocController;
import org.cloudifysource.restDoclet.docElements.DocHttpMethod;
import org.cloudifysource.restDoclet.docElements.DocJsonRequestExample;
import org.cloudifysource.restDoclet.docElements.DocJsonResponseExample;
import org.cloudifysource.restDoclet.docElements.DocMethod;
import org.cloudifysource.restDoclet.docElements.DocParameter;
import org.cloudifysource.restDoclet.docElements.DocPossibleResponseStatusesAnnotation;
import org.cloudifysource.restDoclet.docElements.DocRequestMappingAnnotation;
import org.cloudifysource.restDoclet.docElements.DocRequestParamAnnotation;
import org.cloudifysource.restDoclet.docElements.DocResponseStatus;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;

public class Utils {

	protected static DocAnnotation createNewAnnotation(String name) {
		switch (RestDocConstants.DocAnnotationTypes.fromName(name)) {
		case REQUEST_MAPPING: return new DocRequestMappingAnnotation(name);
		case REQUEST_PARAM: return new DocRequestParamAnnotation(name);
		case JSON_RESPONSE_EXAMPLE: return new DocJsonResponseExample(name);
		case JSON_REQUEST_EXAMPLE: return new DocJsonRequestExample(name);
		case POSSIBLE_RESPONSE_STATUSES: return new DocPossibleResponseStatusesAnnotation(name);
		case CONTROLLER: 
		default: return new DocAnnotation(name);
		}
	}

	protected static DocAnnotation getAnnotation(List<DocAnnotation> annotations, String annotationName) {
		DocAnnotation requestedAnnotation = null;
		if(annotations != null) {
			for (DocAnnotation annotation : annotations) {
				if(annotation.getName().equals(annotationName)) {
					requestedAnnotation = annotation;
					break;
				}
			}
		}
		return requestedAnnotation;
	}

	protected static DocRequestParamAnnotation getRequestParamAnnotation(List<DocAnnotation> annotations) {
		return  (DocRequestParamAnnotation) 
				getAnnotation(annotations, RestDocConstants.REQUEST_PARAMS_ANNOTATION);
	}

	protected static DocRequestMappingAnnotation getRequestMappingAnnotation(List<DocAnnotation> annotations) {
		return (DocRequestMappingAnnotation) 
				getAnnotation(annotations, RestDocConstants.REQUEST_MAPPING_ANNOTATION);
	}

	protected static DocJsonResponseExample getJsonResponseExampleAnnotation(List<DocAnnotation> annotations) {
		return (DocJsonResponseExample) 
				getAnnotation(annotations, RestDocConstants.JSON_RESPONSE_EXAMPLE_ANNOTATION);
	}

	protected static DocJsonRequestExample getJsonRequestExampleAnnotation(List<DocAnnotation> annotations) {
		return (DocJsonRequestExample) 
				getAnnotation(annotations, RestDocConstants.JSON_REQUEST_EXAMPLE_ANNOTATION);
	}

	protected static DocPossibleResponseStatusesAnnotation getPossibleResponseStatusesAnnotation(List<DocAnnotation> annotations) {
		return (DocPossibleResponseStatusesAnnotation) 
				getAnnotation(annotations, RestDocConstants.POSSIBLE_RESPONSE_STATUSES_ANNOTATION);
	}

	protected static Map<String, String> getParamTagsComments(MethodDoc methodDoc) {
		Map<String, String> paramComments = new HashMap<String, String>();
		for (ParamTag paramTag : methodDoc.paramTags()) {
			paramComments.put(paramTag.parameterName(), paramTag.parameterComment());
		}
		return paramComments;
	}

	protected static boolean filterOutControllerClass(ClassDoc classDoc, 
			List<DocAnnotation> annotations) {
		return 
				(Utils.getAnnotation(annotations, RestDocConstants.CONTROLLER_ANNOTATION) == null ||
				classDoc.qualifiedTypeName().equals(RestDocConstants.ADMIN_API_CONTROLLER_CLASS_NAME));
		//return !(classDoc.qualifiedTypeName().equals(RestDocConstants.SERVICE_CONTROLLER_CLASS_NAME));
	}

	protected static void printTypes(List<DocController> controllers) {
		Set<String> returnTypes = new HashSet<String>();
		Set<String> parameterTypes = new HashSet<String>();
		for (DocController docController : controllers) {
			Collection<DocMethod> methods = docController.getMethods().values();
			for (DocMethod docMethod : methods) {
				List<DocHttpMethod> httpMethods = docMethod.getHttpMethods();
				for (DocHttpMethod docHttpMethod : httpMethods) {
					returnTypes.add(docHttpMethod.getReturnDetails().getReturnType().typeName());
					List<DocParameter> params = docHttpMethod.getParams();
					for (DocParameter docParameter : params) {
						parameterTypes.add(docParameter.getType().typeName());
					}
				}
			}
		}
		System.out.println("Generated controllers.");
		System.out.println("Paramter types: " + parameterTypes);
		System.out.println("Return types: " + returnTypes);		
	}

	protected static void printMethodsToFile(List<DocController> controllers, String fileName) throws IOException {
		PrintStream print = null;
		try {
			print = new PrintStream(new File(fileName));
			for (DocController docController : controllers) {
				Collection<DocMethod> methods = docController.getMethods().values();
				print.println("*****************************************");
				print.println("Controller " + docController.getName());
				print.println("*****************************************");
				for (DocMethod docMethod : methods) {
					List<DocHttpMethod> httpMethods = docMethod.getHttpMethods();
					for (DocHttpMethod docHttpMethod : httpMethods) {
						print.println("method " + docHttpMethod.getMethodSignatureName());
						//print.println("				uri " + docMethod.getUri());
						//print.println("				request method " + docHttpMethod.getHttpMethodName());
					}				
				}
			}
		} finally {
			if(print != null) {
				print.flush();
				print.close();
			}
		}
	}

	public static String getIndentJson(String body) throws JsonParseException, IOException {
		if(StringUtils.isBlank(body))
			return null;

		JsonGenerator gen = null;
		StringWriter out = new StringWriter();
		try {
			JsonFactory fac = new JsonFactory();

			JsonParser parser = fac.createJsonParser(new StringReader(body));
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(parser);
			// Create pretty printer:
			gen = fac.createJsonGenerator(out);
			gen.useDefaultPrettyPrinter();
			// Write:
			mapper.writeTree(gen, node);

		} catch (JsonParseException e) {
			throw new JsonParseException("error occured while parsing " + body + ", error msg is " + e.getMessage(), null);
		} finally {
			if(gen != null) {
				gen.flush();
				gen.close();
			}
		}
		return out.toString();

	}

	protected static String generateHtml(List<DocController> controllers) throws JsonParseException, IOException {
		StringBuilder html = new StringBuilder();
		html.append("<html>\n");
		html.append("<head>\n");
		html.append("<title>Cloudify REST API</title>\n");
		html.append("</head>\n");
		html.append("<body>\n");	
		html.append("<div class=\"panel\" style=\"border-color: #f7681a;border-width: 2px;\">\n");		
		html.append("<div class=\"panelContent\">\n");	

		// controllers
		for (DocController docController : controllers) {
			html.append("<h1><a name=\"REST_API-ServiceController\"></a>" + docController.getName() + "</h1>\n");	
			html.append("<p><font color=\"#f7681a\">Mapping of URIs that begin with "+ docController.getUri()+"</font></p>\n");	
			html.append("<hr />");
			html.append("</div>\n");	
			html.append("</div>\n");	

			// methods
			for (DocMethod docMethod : docController.getMethods().values()) {
				String mathodUri = docController.getUri() + docMethod.getUri();
				html.append("<h2><a name=\"REST_API-" + mathodUri + "\"></a>" + mathodUri + "</h2>\n");
				if(docMethod.getDescription() != null)
					html.append("<p><font color=\"#f7681a\">" + docMethod.getDescription() + "</font></p>\n");
				html.append("<h3><a name=\"REST_API-HTTPMethods\"></a>HTTP Methods</h3>\n");

				// http methods
				for (DocHttpMethod httpMethod : docMethod.getHttpMethods()) {
					String httpMethodName = httpMethod.getHttpMethodName();
					html.append("<h4><a name=\"REST_API-GET\"></a>" + httpMethodName + "</h4>\n");
					if(httpMethod.getDescription() != null)
						html.append("<p>" + httpMethod.getDescription() + "</p>\n");	

					// parameters
					List<DocParameter> params = httpMethod.getAnnotatedParams();
					if(params != null && !params.isEmpty()) {
						html.append("<h3><a name=\"REST+API-Parameters\"></a>Parameters</h3>\n")
						.append("<table border=\"1\">\n")
						.append("<tr style=\"background-color:#D8D8D8;\">\n")
						.append("<th>Type</th>\n")
						.append("<th>Name</th>\n")
						.append("<th>Decription</th>\n")
						.append("<th>Mandatory</th>\n")
						.append("<th>Location</th>\n")
						.append("<th>Default value</th>\n")
						.append("</tr>\n");
						for (DocParameter docParameter : params) {
							html.append("<tr style=\"background-color:#FFFFCC;\">\n")
							.append("<td>" + docParameter.getType() + "</td>\n")
							.append("<td>" + docParameter.getName() + "</td>\n")
							.append("<td>" + (docParameter.getDescription() == null ? "" : docParameter.getDescription().replace(".", "")) + "</td>\n")
							.append("<td>" + docParameter.isRequired() + "</td>\n")							
							.append("<td>" + docParameter.getLocation() + "</td>\n")
							.append("<td>" + (docParameter.getDefaultValue() == null ? "" : docParameter.getDefaultValue()) + "</td>\n")
							.append("</tr>\n");
						}
						html.append("</table>\n");
					}

					// request
					DocJsonRequestExample request = httpMethod.getJsonRequestExample();
					if(request != null) {
						html.append("<h5><a name=\"REST_API-Request\"></a>Request</h5>\n");
						String requestBody = request.generateJsonRequestBody();
						if(requestBody != null) {
							html.append("<div class=\"codeHeader panelHeader\" style=\"border-bottom-width: 1px;\"><b>Request body for a "
									+ httpMethodName + " on " + mathodUri + " </b></div>\n")
									.append("<div class=\"codeContent panelContent\">\n")
									.append("<pre><code>" + request.generateJsonRequestBody() + "</code></pre>\n")
									.append("</div>\n")
									.append("</div>\n");
						}
						else
							html.append("<p> Request has no body. </p>");
						if(request.getComments() != null)
							html.append("<p>" + request.getComments() + "</p>");
					}

					// response
					DocJsonResponseExample response = httpMethod.getJsonResponseExample();
					if(response != null) {
						html.append("<h5><a name=\"REST_API-Response\"></a>Response</h5>\n")
						.append("<div class=\"code panel\" style=\"border-width: 1px;\">\n");
						if(response.generateJsonResponseBody() != null) {
							html.append("<div class=\"codeHeader panelHeader\" style=\"border-bottom-width: 1px;\"><b>Response to a " 
									+ httpMethodName + " on " + mathodUri + " </b></div>\n")
									.append("<div class=\"codeContent panelContent\">\n")
									.append("<code> " + response.generateJsonResponseBody() + " </code>\n")
									.append("</div>\n")
									.append("</div>\n");
						}
						else
							html.append("<p> Response has no body. </p>");
						if(response.getComments() != null)
							html.append("<p>" + response.getComments() + "</p>");
					}
					List<DocResponseStatus> possibleResponses = httpMethod.getPossibleResponseStatuses();
					if(possibleResponses != null && !possibleResponses.isEmpty()) {
						html.append("<ul>\n");
						for (DocResponseStatus docResponseStatus : possibleResponses) {
							html.append("<li>Returns \"" + docResponseStatus.getCode() + " "
									+ docResponseStatus.getCodeName() + "\"" 
									+ docResponseStatus.getDescription() + "</li>\n");
						}
						html.append("</ul>\n");
					}
				}
			}
		}                    				
		html.append("</body>\n")
		.append("</html>\n");
		return html.toString();
	}


}
