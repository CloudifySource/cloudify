package org.cloudifysource.dsl.internal;

public enum CloudifyMessageKeys {
	
	OPERATION_SUCCESSFULL("operation_successfull"),
	
	ATTRIBUTE_DELETED_SUCCESSFULLY("deleted_instance_attribute"),
	
	API_VERSION_MISMATCH("version_mismatch"),
	
	SERVICE_WAIT_TIMEOUT("service_wait_timeout"),
	
	APPLICATION_WAIT_TIMEOUT("application_wait_timeout"),
	
	MISSING_SERVICE("missing_service"),

	MISSING_SERVICE_INSTANCE("missing_service_instance"),
	
	EMPTY_REQUEST_BODY_ERROR("empty_request_body_error"),
	
	EMPTY_ATTRIBUTE_NAME("empty_attribute_name"),
	
	NOT_EXIST_ATTRIBUTE("not_exist_attribute"),
	
	FILE_SIZE_LIMIT_EXCEEDED("file_size_limit_exceeded"),
	
	UPLOAD_FAILED("failed_to_upload_file"),
	
	WRONG_UPLOAD_KEY("wrong_upload_key"),
	
	UPLOAD_KEY_PARAMETER_MISSING("upload_key_is_missing"),
	
	ZONE_PARAMETER_MISSING("zone_parameter_is_missing"),
	
	SERVICE_OVERRIDES_LENGTH_LIMIT_EXCEEDED("service_overrides_string_length_limit_exceeded"),

	CLOUD_OVERRIDES_SIZE_LIMIT_EXCEEDED("cloud_overrides_size_limit_exceeded"),

	VALIDATOR_REQUEST_MISSING("validator_is_missing_install_service_request_instance"),
	
	VALIDATOR_CLOUD_MISSING("validator_is_missing_cloud_instance"),
	
	VALIDATOR_SERVICE_MISSING("validator_is_missing_service_instance"),
	
	VALIDATOR_TEMPLATE_NAME_MISSING("validator_is_missing_template_name"),

	UNSUPPORTED_SERVICE_TYPE("unsupported_service_type"),
	
	MISSING_TEMPLATE("missing_template"),
	
	NOT_ALL_GSM_INSTANCES_RUNNING("not_all_gsm_instances_running"), 
	
	INSUFFICIENT_MEMORY("insufficient_memory"),
	
	FAILED_TO_EXTRACT_PROJECT_FILE("failed_to_extract_service_project_file"),
	
	FAILED_TO_MERGE_OVERRIDES("failed_to_merge_overrides_with_properties"),
	
	FAILED_TO_READ_SERVICE("failed_to_read_service"),
	
	FAILED_TO_READ_SERVICE_CLOUD_CONFIGURATION("failed_to_read_service_cloud_configuration"), 
	
	WRONG_SERVICE_CLOUD_CONFIGURATION_UPLOAD_KEY("wrong_service_cloud_configuration_upload_key"),
	
	WRONG_CLOUD_OVERRIDES_UPLOAD_KEY("wrong_cloud_overrides_upload_key");
	
	private final String name;
	
	CloudifyMessageKeys(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
