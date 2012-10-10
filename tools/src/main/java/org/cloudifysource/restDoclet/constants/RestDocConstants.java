package org.cloudifysource.restDoclet.constants;



public class RestDocConstants {
	public static final String DOCLET_FLAG = "-doclet";
	public static final String SOURCE_PATH_FLAG = "-sourcepath";

	public static final String SOURCES_PATH = "../restful/src/main/java";
	public static final String CONTROLLERS_PACKAGE = "org.cloudifysource.rest.controllers";
	public static final String SERVICE_CONTROLLER_CLASS_NAME = "org.cloudifysource.rest.controllers.ServiceController";
	public static final String ADMIN_API_CONTROLLER_CLASS_NAME = "org.cloudifysource.rest.controllers.AdminAPIController";

	public static final String VELOCITY_TEMPLATE_PATH_FLAG = "-velocityTemplateFilePath";
	public static final String DOC_DEST_PATH_FLAG = "-docletDestdir";
	
	public static final String VERSION_FLAG = "-version";
	
	public static final String DEFAULT_VELOCITY_TEMPLATE_FILE_NAME = "restDocletVelocityTemplate.vm";
	public static final String DEFAULT_VELOCITY_TEMPLATE_PATH = "src/main/resources/" + DEFAULT_VELOCITY_TEMPLATE_FILE_NAME;
	public static final String DEFAULT_DOC_DEST_PATH = "restdoclet.html";
	
	public static final String CONTROLLER_ANNOTATION = "Controller";
	
	public static final String REQUEST_MAPPING_ANNOTATION = "RequestMapping";
	public static final String REQUEST_MAPPING_VALUE = "value";
	public static final String REQUEST_MAPPING_METHOD = "method";
	public static final String REQUEST_MAPPING_HEADERS = "headers";
	public static final String REQUEST_MAPPING_PARAMS = "params";
	public static final String REQUEST_MAPPING_PRODUCES = "produces";
	public static final String REQUEST_MAPPING_CONSUMED = "consumes";
	
	public static final String REQUEST_PARAMS_ANNOTATION = "RequestParam";
	public static final String REQUEST_PARAMS_REQUIRED = "required";
	public static final String REQUEST_PARAMS_VALUE = "value";
	public static final String REQUEST_PARAMS_DEFAULT_VALUE = "defaultValue";
	
	public static final String REQUEST_BODY_ANNOTATION = "RequestBody";
	
	public static final String RESPONSE_BODY_ANNOTATION = "ResponseBody";
	
	public static final String PATH_VARIABLE_ANNOTATION = "PathVariable";
	
	public static final String JSON_RESPONSE_EXAMPLE_ANNOTATION = "JsonResponseExample";
	public static final String JSON_RESPONSE_EXAMPLE_STATUS = "status";
	public static final String JSON_RESPONSE_EXAMPLE_RESPONSE = "responseBody";
	public static final String JSON_RESPONSE_EXAMPLE_COMMENTS = "comments";

	public static final String JSON_REQUEST_EXAMPLE_ANNOTATION = "JsonRequestExample";
	public static final String JSON_REQUEST_EXAMPLE_REQUEST_PARAMS = "requestBody";
	public static final String JSON_REQUEST_EXAMPLE_COMMENTS = "comments";
	
	public static final String POSSIBLE_RESPONSE_STATUSES_ANNOTATION = "PossibleResponseStatuses";
	public static final String POSSIBLE_RESPONSE_STATUSES_CODES = "codes";
	public static final String POSSIBLE_RESPONSE_STATUSES_DESCRIPTIONS = "descriptions";

	public static final String HTTP_MATHOD_GET = "GET";
	public static final String HTTP_MATHOD_POST = "POST";
	public static final String HTTP_MATHOD_DELETE = "DELETE";
	
	public enum DocAnnotationTypes {
		CONTROLLER,
		REQUEST_MAPPING,
		REQUEST_PARAM,
		REQUEST_BODY,
		RESPONSE_BODY,
		PATH_VARIABLE,
		JSON_RESPONSE_EXAMPLE,
		JSON_REQUEST_EXAMPLE,
		POSSIBLE_RESPONSE_STATUSES,
		DEFAULT;

		public static DocAnnotationTypes fromName(String annotationName) {
			if(CONTROLLER_ANNOTATION.equals(annotationName))
				return CONTROLLER;
			else if(REQUEST_MAPPING_ANNOTATION.equals(annotationName))
				return REQUEST_MAPPING;
			else if(REQUEST_PARAMS_ANNOTATION.equals(annotationName))
				return REQUEST_PARAM;
			else if(REQUEST_BODY_ANNOTATION.equals(annotationName))
				return REQUEST_BODY;
			else if(RESPONSE_BODY_ANNOTATION.equals(annotationName))
				return RESPONSE_BODY;
			else if(PATH_VARIABLE_ANNOTATION.equals(annotationName))
				return PATH_VARIABLE;
			else if(JSON_RESPONSE_EXAMPLE_ANNOTATION.equals(annotationName))
				return JSON_RESPONSE_EXAMPLE;
			else if(JSON_REQUEST_EXAMPLE_ANNOTATION.equals(annotationName))
				return JSON_REQUEST_EXAMPLE;
			else if(POSSIBLE_RESPONSE_STATUSES_ANNOTATION.endsWith(annotationName))
				return POSSIBLE_RESPONSE_STATUSES;
			else 
				return DEFAULT;
		}
		public String getAnnotationName()
		{
			switch (this)
			{
				case CONTROLLER: return CONTROLLER_ANNOTATION;
				case REQUEST_MAPPING: return REQUEST_MAPPING_ANNOTATION;
				case REQUEST_PARAM: return REQUEST_PARAMS_ANNOTATION;
				case REQUEST_BODY: return REQUEST_BODY_ANNOTATION;
				case RESPONSE_BODY: return RESPONSE_BODY_ANNOTATION;
				case PATH_VARIABLE: return PATH_VARIABLE_ANNOTATION;
				default: throw new IllegalArgumentException("Unsupported DocAnnotations: " + this);
			}
		}

	}
	
	public enum ResponseCodes {
		OK(200, "OK"),
		INTERNAL_SERVER_ERROR(500, "Internal Server Error");
		
		private final int value;
		private final String reasonPhrase;
		
		private ResponseCodes(int value, String reasonPhrase) {
			this.value = value;
			this.reasonPhrase = reasonPhrase;
		}
		
		public static ResponseCodes fromCode(int code) {
			switch (code) {
			case 200:
				return OK;
			case 500:
				return INTERNAL_SERVER_ERROR;
			default:
				throw new IllegalArgumentException("Unsupported ResponseCodes code: " + code);
			}
		}
		
		public int getValue() {
			return this.value;
		}
		
		public String getReasonPhrase() {
			return this.reasonPhrase;
		}
	}
}
