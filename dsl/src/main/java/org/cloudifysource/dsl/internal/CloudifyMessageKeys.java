package org.cloudifysource.dsl.internal;

public enum CloudifyMessageKeys {

    OPERATION_SUCCESSFULL("operation_successfull"),

    ATTRIBUTE_DELETED_SUCCESSFULLY("deleted_instance_attribute"),

    API_VERSION_MISMATCH("version_mismatch"),

    MISSING_RESOURCE("missing_resource"),

    EMPTY_REQUEST_BODY_ERROR("empty_request_body_error"),

    EMPTY_ATTRIBUTE_NAME("empty_attribute_name"),

    NOT_EXIST_ATTRIBUTE("not_exist_attribute"),
    
    UPLOAD_DIRECTORY_CREATION_FAILED("failed_creating_upload_directory"),
    
    UPLOAD_FILE_SIZE_LIMIT_EXCEEDED("upload_file_size_limit_exceeded"),

    UPLOAD_FAILED("failed_to_upload_file"),

    WRONG_SERVICE_FOLDER_UPLOAD_KEY("wrong_service_upload_key"),

    WRONG_SERVICE_OVERRIDES_UPLOAD_KEY("wrong_service_overrides_upload_key"),

    WRONG_CLOUD_CONFIGURATION_UPLOAD_KEY("wrong_service_cloud_configuration_upload_key"),

    WRONG_CLOUD_OVERRIDES_UPLOAD_KEY("wrong_cloud_overrides_upload_key"),

    UPLOAD_KEY_PARAMETER_MISSING("upload_key_is_missing"),

    SERVICE_OVERRIDES_SIZE_LIMIT_EXCEEDED("service_overrides_file_size_limit_exceeded"),

    CLOUD_OVERRIDES_SIZE_LIMIT_EXCEEDED("cloud_overrides_file_size_limit_exceeded"),

    CLOUD_CONFIGURATION_SIZE_LIMIT_EXCEEDED("cloud_configuration_file_size_limit_exceeded"),

    MISSING_TEMPLATE("missing_template"),

    NOT_ALL_GSM_INSTANCES_RUNNING("not_all_gsm_instances_running"),

    INSUFFICIENT_MEMORY("insufficient_memory"),

    ESM_MISSING("esm_missing"),

    FAILED_TO_EXTRACT_PROJECT_FILE("failed_to_extract_service_project_file"),

    FAILED_TO_MERGE_OVERRIDES("failed_to_merge_overrides_with_properties"),

    FAILED_TO_READ_SERVICE("failed_to_read_service"),

    FAILED_TO_READ_SERVICE_CLOUD_CONFIGURATION("failed_to_read_service_cloud_configuration"),

    WRONG_APPLICTION_FILE_UPLOAD_KEY("wrong_application_file_upload_key"),

    WRONG_APPLICTION_OVERRIDES_FILE_UPLOAD_KEY("wrong_application_overrides_file_upload_key"),

    DEST_MERGE_FILE_MISSING("destination_merge_file_is_missing"),

    REPACKED_MERGE_FOLDER_MISSING("repacked_folder_is_missing"),
    
    APPLICATION_NAME_IS_ALREADY_IN_USE("application_name_is_alreay_in_use"),

    APPLICATION_NAME_CONTAINS_INVALID_CHARS("application_name_contains_invalid_chars"),

    WRONG_TEMPLATES_UPLOAD_KEY("wrong_templates_upload_key");

    
    private final String name;

    CloudifyMessageKeys(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
