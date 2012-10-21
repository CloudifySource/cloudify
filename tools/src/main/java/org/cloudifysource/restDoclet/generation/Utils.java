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
						RestDocConstants.ADMIN_API_CONTROLLER_CLASS_NAME.equals(classDoc.qualifiedTypeName()));
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

		StringWriter out = new StringWriter();
		JsonParser parser = null;
		JsonGenerator gen = null;
		try {
			JsonFactory fac = new JsonFactory();

			parser = fac.createJsonParser(new StringReader(body));
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(parser);
			// Create pretty printer:
			gen = fac.createJsonGenerator(out);
			gen.useDefaultPrettyPrinter();
			// Write:
			mapper.writeTree(gen, node);

			gen.close();
			parser.close();

			return out.toString();

		} finally {
			out.close();
			if(gen != null) {
				gen.close();
			}
			if(parser != null) {
				parser.close();
			}
		}

	}

//	protected static String generateHtml(List<DocController> controllers) throws JsonParseException, IOException {
//		StringBuilder html = new StringBuilder("<html>") 
//		.append("\n<head>") 
//		.append("\n<title>Cloudify REST API</title>")
//		.append("\n</head>\n<body>")
//		.append("\n<div class=\"panel\" style=\"border-color: #f7681a;border-width: 2px;\">")
//		.append("\n<div class=\"panelContent\">\n");	
//
//		// controllers
//		for (DocController docController : controllers) {
//			html.append("<h1>");
//			html.append("<a name=\"REST_API-ServiceController\"></a>");
//			html.append(docController.getName());
//			html.append("</h1>");
//			html.append("\n<p><font color=\"#f7681a\">Mapping of URIs that begin with ");
//			html.append(docController.getUri());
//			html.append("</font></p>");
//			html.append("\n<hr /></div>");
//			html.append("\n</div>\n");	
//
//			// methods
//			for (DocMethod docMethod : docController.getMethods().values()) {
//				String mathodUri = docController.getUri() + docMethod.getUri();
//				html.append("<h2><a name=\"REST_API-")
//				.append(mathodUri)
//				.append("\"></a>")
//				.append(mathodUri)
//				.append("</h2>\n");
//				if(docMethod.getDescription() != null)
//					html.append("<p><font color=\"#f7681a\">")
//					.append(docMethod.getDescription())
//					.append("</font></p>\n");
//				html.append("<h3><a name=\"REST_API-HTTPMethods\"></a>HTTP Methods</h3>\n");
//
//				// http methods
//				for (DocHttpMethod httpMethod : docMethod.getHttpMethods()) {
//					String httpMethodName = httpMethod.getHttpMethodName();
//					html.append("<h4><a name=\"REST_API-GET\"></a>")
//					.append(httpMethodName)
//					.append("</h4>\n");
//					if(httpMethod.getDescription() != null) {
//						html.append("<p>")
//						.append(httpMethod.getDescription())
//						.append("</p>\n");
//					}
//
//					// parameters
//					List<DocParameter> params = httpMethod.getAnnotatedParams();
//					if(params != null && !params.isEmpty()) {
//						html.append("<h3><a name=\"REST+API-Parameters\"></a>Parameters</h3>\n")
//						.append("<table border=\"1\">\n")
//						.append("<tr style=\"background-color:#D8D8D8;\">\n")
//						.append("<th>Type</th>\n")
//						.append("<th>Name</th>\n")
//						.append("<th>Decription</th>\n")
//						.append("<th>Mandatory</th>\n")
//						.append("<th>Location</th>\n")
//						.append("<th>Default value</th>\n")
//						.append("</tr>\n");
//						for (DocParameter docParameter : params) {
//							StringBuilder paramsTable = new StringBuilder();
//							paramsTable.append("<tr style=\"background-color:#FFFFCC;\">\n")
//							.append("<td>" + docParameter.getType() + "</td>\n")
//							.append("<td>" + docParameter.getName() + "</td>\n")
//							.append("<td>") 
//							.append(docParameter.getDescription() == null ? "" : docParameter.getDescription().replace(".", "")) 
//							.append("</td>\n")
//							.append("<td>" + docParameter.isRequired() + "</td>\n")							
//							.append("<td>" + docParameter.getLocation() + "</td>\n")
//							.append("<td>" + (docParameter.getDefaultValue() == null ? "" : docParameter.getDefaultValue()) + "</td>\n")
//							.append("</tr>\n");
//							html.append(paramsTable);
//						}
//						html.append("</table>\n");
//					}
//
//					// request
//					DocJsonRequestExample request = httpMethod.getJsonRequestExample();
//					if(request != null) {
//						html.append("<h5><a name=\"REST_API-Request\"></a>Request</h5>\n");
//						String requestBody = request.generateJsonRequestBody();
//						if(requestBody != null) {
//							html.append("<div class=\"codeHeader panelHeader\" style=\"border-bottom-width: 1px;\"><b>Request body for a ")
//							.append(httpMethodName)
//							.append(" on ")
//							.append(mathodUri)
//							.append(" </b></div>\n")
//							.append("<div class=\"codeContent panelContent\">\n")
//							.append("<pre><code>")
//							.append(request.generateJsonRequestBody())
//							.append("</code></pre>\n")
//							.append("</div>\n")
//							.append("</div>\n");
//						}
//						else
//							html.append("<p> Request has no body. </p>");
//						String comments = request.getComments();
//						if(comments != null)
//							html.append("<p>" + comments + "</p>");
//					}
//
//					// response
//					DocJsonResponseExample response = httpMethod.getJsonResponseExample();
//					if(response != null) {
//						html.append("<h5><a name=\"REST_API-Response\"></a>Response</h5>\n")
//						.append("<div class=\"code panel\" style=\"border-width: 1px;\">\n");
//						String generateJsonResponseBody = response.generateJsonResponseBody();
//						if(generateJsonResponseBody != null) {
//							html.append("<div class=\"codeHeader panelHeader\" style=\"border-bottom-width: 1px;\"><b>Response to a ") 
//							.append(httpMethodName)
//							.append(" on ")
//							.append(mathodUri)
//							.append(" </b></div>\n")
//							.append("<div class=\"codeContent panelContent\">\n")
//							.append("<code> ")
//							.append(generateJsonResponseBody) 
//							.append(" </code>\n")
//							.append("</div>\n")
//							.append("</div>\n");
//						}
//						else
//							html.append("<p> Response has no body. </p>");
//						String comments = response.getComments();
//						if(comments != null) {
//							html.append("<p>")
//							.append(comments)
//							.append("</p>");
//						}
//					}
//					List<DocResponseStatus> possibleResponses = httpMethod.getPossibleResponseStatuses();
//					if(possibleResponses != null && !possibleResponses.isEmpty()) {
//						html.append("<ul>\n");
//						for (DocResponseStatus docResponseStatus : possibleResponses) {
//							html.append("<li>Returns \"")
//							.append(docResponseStatus.getCode())
//							.append(" ")
//							.append(docResponseStatus.getCodeName())
//							.append("\"") 
//							.append(docResponseStatus.getDescription())
//							.append("</li>\n");
//						}
//						html.append("</ul>\n");
//					}
//				}
//			}
//		}                    				
//		return html.toString() + ("</body>\n") + ("</html>\n");
//	}


}
