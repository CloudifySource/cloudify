/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.dsl.internal;

import java.io.File;

import org.apache.commons.io.FileUtils;

/*********************
 * A shared location for all constants used by multiple cloudify constants.
 *
 * @author barakme
 *
 */
public final class CloudifyConstants {

    // CHECKSTYLE:OFF

    /***
     * USM service deployment States.
     */
    public static enum USMState {

        INITIALIZING, LAUNCHING, RUNNING, SHUTTING_DOWN,
        /**
         * Indicates that the service instance has failed for some reason but
         * self-healing was disabled to the service instance will not shut down.
         */
        ERROR
    }

    public static enum DeploymentState {
        IN_PROGRESS,
        FAILED,
        STARTED

    }

    /***
     * Context properties for deployed services.
     */
    public static final String CONTEXT_PROPERTY_NETWORK_PROTOCOL_DESCRIPTION = "com.gs.service.network.protocolDescription";
    public static final String CONTEXT_PROPERTY_SERVICE_ICON = "com.gs.service.icon";
    public static final String CONTEXT_PROPERTY_SERVICE_TYPE = "com.gs.service.type";
    public static final String CONTEXT_PROPERTY_DEPENDS_ON = "com.gs.application.dependsOn";
    public static final String CONTEXT_PROPERTY_APPLICATION_FILE_NAME = "com.gs.cloudify.application-file-name";
    public static final String CONTEXT_PROPERTY_SERVICE_FILE_NAME = "com.gs.cloudify.service-file-name";
    public static final String CONTEXT_PROPERTY_CLOUD_FILE_NAME = "com.gs.cloudify.cloud-file-name";
    public static final String CONTEXT_PROPERTY_CLOUD_NAME = "com.gs.cloudify.cloud-name";
    public static final String CONTEXT_PROPERTY_PROPERTIES_FILE_NAME = "com.gs.cloudify.properties-file-name";
    public static final String CONTEXT_PROPERTY_ASYNC_INSTALL = "com.gs.cloudify.async-install";
    public static final String CONTEXT_PROPERTY_DEPLOYMENT_ID = "com.gs.cloudify.deployment-id";
    public static final String CONTEXT_PROPERTY_DISABLE_SELF_HEALING = "com.gs.cloudify.disable-self-healing";
    public static final String CONTEXT_PROPERTY_APPLICATION_NAME = "com.gs.application";
    public static final String CONTEXT_PROPERTY_ELASTIC = "com.gs.service.elastic";
    public static final String CONTEXT_PROPERTY_TEMPLATE = "com.gs.service.template";
    public static final String CONTEXT_PROPERTY_AUTH_GROUPS = "com.gs.deployment.auth.groups";
    public static final String CONTEXT_PROPERTY_DEBUG_ALL = "com.gs.service.debug.all";
    public static final String CONTEXT_PROPERTY_DEBUG_EVENTS = "com.gs.service.debug.events";
    public static final String CONTEXT_PROPERTY_DEBUG_MODE = "com.gs.service.debug.mode";

    /**********
     * Key names for invocation request and response parameters.
     */
    public static final String INVOCATION_PARAMETER_COMMAND_NAME = "GS_USM_CommandName";
    public static final String INVOCATION_PARAMETERS_KEY = "GS_USM_Command_Parameters";
    public static final String INVOCATION_RESPONSE_STATUS = "Invocation_Success";
    public static final String INVOCATION_RESPONSE_EXCEPTION = "Invocation_Exception";
    public static final String INVOCATION_RESPONSE_RESULT = "Invocation_Result";
    public static final String INVOCATION_RESPONSE_COMMAND_NAME = "Invocation_Command_Name";
    public static final String INVOCATION_RESPONSE_INSTANCE_ID = "Invocation_Instance_ID";
    public static final String INVOCATION_RESPONSE_INSTANCE_NAME = "Invocation_Instance_Name";
    public static final String INVOCATION_PARAMETER_BEAN_NAME_USM = "universalServiceManagerBean";

    /*************
     * Key names of environment variables passed to USM external scripts
     */
    public static final String USM_ENV_RUNNING_NUMBER = "USM_RUNNING_NUMBER";
    public static final String USM_ENV_NUMBER_OF_INSTANCES = "USM_NUMBER_OF_INSTANCES";
    public static final String USM_ENV_INSTANCE_ID = "USM_INSTANCE_ID";
    public static final String USM_ENV_PU_UNIQUE_NAME = "USM_PU_UNIQUE_NAME";
    public static final String USM_ENV_CLUSTER_NAME = "USM_CLUSTER_NAME";
    public static final String USM_ENV_SERVICE_FILE_NAME = "USM_SERVICE_FILE_NAME";
    public static final String USM_ENV_APPLICATION_NAME = "USM_APPLICATION_NAME";
    public static final String USM_ENV_SERVICE_NAME = "USM_SERVICE_NAME";

    /****************
     * Key names for USM Monitors
     */
    public static final String USM_MONITORS_STATE_ID = "USM_State";
    public static final String USM_MONITORS_CHILD_PROCESS_ID = "USM_Child Process ID";
    public static final String USM_MONITORS_ACTUAL_PROCESS_ID = "USM_Actual Process ID";

    /****************
     * Key names for USM Details
     */
    public static final String USM_DETAILS_PRIVATE_IP = "Cloud Private IP";
    public static final String USM_DETAILS_PUBLIC_IP = "Cloud Public IP";
    public static final String USM_DETAILS_URL = "url";
    public static final String USM_DETAILS_ICON = "icon";
    public static final String USM_DETAILS_IMAGE_ID = "Cloud Image ID";
    public static final String USM_DETAILS_HARDWARE_ID = "Cloud Hardware ID";
    public static final String USM_DETAILS_INSTANCE_ID = "Instance ID";
    public static final String USM_DETAILS_MACHINE_ID = "Machine ID";

    /****************
     * Key names for USM Exposed Monitors and Details
     */
    public static final String USM_MONITORS_SERVICE_ID = "USM";
    public static final String USM_DETAILS_SERVICE_ID = "USM";

    /*********************
     * Key names for USM parameters that can be configured using the service
     * custom parameters.
     *
     */
    public static final String USM_PARAMETERS_TAILER_INTERVAL = "TailerInterval";

    /*************************************
     * Keys for Elastic Provisioning properties used with ESM machine
     * provisioning.
     *
     */
    public static final String ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME = "__CLOUDIFY__CLOUD_TEMPLATE_NAME";
    public static final String ELASTIC_PROPERTIES_CLOUD_CONFIGURATION_DIRECTORY = "__CLOUDIFY__CLOUD_CONFIGURATION_DIRECTORY";
    public static final String ELASTIC_PROPERTIES_CLOUD_OVERRIDES_PER_SERVICE = "cloud-overrides-per-service";

    /********************
     * Key names for lifecycle event results.
     */
    public static final String USM_EVENT_EXEC_SUCCESSFULLY = " completed";
    public static final String USM_EVENT_EXEC_FAILED = " failed";
    public static final String USM_EVENT_EXEC_SUCCEED_MESSAGE = "[OK]";
    public static final String USM_EVENT_EXEC_FAILED_MESSAGE = "[ERROR]";

    /***********************************
     * Key names for environment variables available to agent in a cloudify
     * environment
     */
    public static final String GIGASPACES_AGENT_ENV_PRIVATE_IP = "GIGASPACES_AGENT_ENV_PRIVATE_IP";
    public static final String GIGASPACES_AGENT_ENV_PUBLIC_IP = "GIGASPACES_AGENT_ENV_PUBLIC_IP";
    public static final String GIGASPACES_AGENT_ENV_PRIVILEGED = "GIGASPACES_AGENT_ENV_PRIVILEGED";
    public static final String GIGASPACES_AGENT_ENV_INIT_COMMAND = "GIGASPACES_AGENT_ENV_INIT_COMMAND";
    public static final String GIGASPACES_CLOUD_IMAGE_ID = "GIGASPACES_CLOUD_IMAGE_ID";
    public static final String GIGASPACES_CLOUD_HARDWARE_ID = "GIGASPACES_CLOUD_HARDWARE_ID";
    public static final String GIGASPACES_CLOUD_TEMPLATE_NAME = "GIGASPACES_CLOUD_TEMPLATE_NAME";
    public static final String GIGASPACES_AGENT_ENV_JAVA_URL = "GIGASPACES_AGENT_ENV_JAVA_URL";
    public static final String GIGASPACES_ORIGINAL_JAVA_HOME = "GIGASPACES_ORIGINAL_JAVA_HOME";
    public static final String GIGASPACES_CLOUD_MACHINE_ID = "GIGASPACES_CLOUD_MACHINE_ID";
    public static final String GIGASPACES_AUTH_GROUPS = "GIGASPACES_AUTH_GROUPS";
    public static final String CLOUDIFY_CLOUD_MACHINE_IP_ADDRESS_ENV = "MACHINE_IP_ADDRESS";
    public static final String CLOUDIFY_OPEN_FILES_LIMIT = "CLOUDIFY_OPEN_FILES_LIMIT";

    /***********************************
     * Duplicate of Key names for environment variables available to agent in a cloudify
     * environment using the old names, to maintain backwards compatibility
     */

    public static final String CLOUDIFY_CLOUD_IMAGE_ID = "CLOUDIFY_CLOUD_IMAGE_ID";
    public static final String CLOUDIFY_CLOUD_HARDWARE_ID = "CLOUDIFY_CLOUD_HARDWARE_ID";
    public static final String CLOUDIFY_AGENT_ENV_PRIVATE_IP = "CLOUDIFY_AGENT_ENV_PRIVATE_IP";
    public static final String CLOUDIFY_AGENT_ENV_PUBLIC_IP = "CLOUDIFY_AGENT_ENV_PUBLIC_IP";
    public static final String CLOUDIFY_CLOUD_MACHINE_ID = "CLOUDIFY_CLOUD_MACHINE_ID";
    public static final String CLOUDIFY_CLOUD_LOCATION_ID = "CLOUDIFY_CLOUD_LOCATION_ID";
    public static final String CLOUDIFY_LINK_ENV = "GIGASPACES_LINK";
    public static final String CLOUDIFY_OVERRIDES_LINK_ENV = "GIGASPACES_OVERRIDES_LINK";


    /***************
     * Misc.
     */
    public static final String STOP_MANAGEMENT_TIMEOUT_IN_MINUTES = "org.cloudifysource" +
            ".stop-management-timeout-in-minutes";
    public static final String DEFAULT_APPLICATION_NAME = "default";
    public static final String MANAGEMENT_SPACE_NAME = "cloudifyManagementSpace";
    public static final String MANAGEMENT_WEBUI_SERVICE_NAME = "webui";
    public static final String MANAGEMENT_REST_SERVICE_NAME = "rest";
    public static final String MANAGEMENT_APPLICATION_NAME = "management";
    public static final String USM_LIB_DIR = "usmlib";
    public static final String SERVICE_EXTERNAL_FOLDER = "/ext/";
    public static final String SERVICE_CLOUD_CONFIGURATION_FILE_NAME = "__Cloud_Configuration.zip";
    public static final String TEMP_FOLDER = System.getProperty("java.io.tmpdir");
    public static final String REST_FOLDER = TEMP_FOLDER + File.separator + "Cloudify";


    /***************
     * Reason codes for rest exceptions
     */
    public static final String ERR_REASON_CODE_FAILED_TO_LOCATE_APP = "failed_to_locate_app";
    public static final String ERR_MESSAGE_CODE_MISSING_RESOURCE = "missing_resource";

    /********************
     * Custom properties for known cloudify settings.
     *
     */
    public static final String CUSTOM_PROPERTY_ENABLE_PID_MONITOR = "org.cloudifysource.enable-pid-monitor";
    public static final String CUSTOM_PROPERTY_ENABLE_TCP_PORT_MONITOR = "org.cloudifysource.enable-port-monitor";
    public static final String CUSTOM_PROPERTY_ENABLE_START_PROCESS_MONITOR = "org.cloudifysource.enable-start-process-monitor";
    public static final String CUSTOM_PROPERTY_STOP_DETECTION_ON_ALL_PROCESSES = "org.cloudifysource.stop-detection-on-all-processes";
    public static final String CUSTOM_PROPERTY_MONITORS_CACHE_EXPIRATION_TIMEOUT = "org.cloudifysource.monitors-cache-timeout";
    public static final String CUSTOM_PROPERTY_PIDS_SIZE_LIMIT = "org.cloudifysource.pids-size-limit";
    public static final String CUSTOM_CLOUD_PROPERTY_UNICAST_DISCOVERY_PORT = "org.cloudifysource.unicast-discovery-port";
    public static final String CUSTOM_PROPERTY_CLEAN_REMOTE_DIR_ON_START = "org.cloudifysource.clearRemoteDirectoryOnStart";
	public static final String NEW_REST_CLIENT_ENABLE_PROPERTY = "org.cloudifysource.rest-client.enable-new-rest-client";
	public static final String CUSTOM_PROPERTY_VERBOSE_VALIDATION = "org.cloudifysource.verboseValidation";


    /*******************
     * event lifecycle polling parameters.
     *
     */
    public static final String CURSOR_POS = "curserPos";
    public static final String LIFECYCLE_LOGS = "lifecycleLogs";
    public static final String IS_TASK_DONE = "isDone";
    public static final String LIFECYCLE_EVENT_CONTAINER_ID = "lifecycleEventContainerID";
    public static final String SERVICE_ORDER = "serviceOrder";
    public static final String SERVER_POLLING_TASK_EXPIRATION_MILLI = "PollingTaskExpirationTimeMillis";

    /************************
     * Keys used by Agentless Installer to modify default installer behavior.
     */
    public static final String INSTALLER_CUSTOM_DATA_SFTP_PREFERRED_AUTHENTICATION_METHODS_KEY = "installer.sftp.preferredAuthentications";

    private CloudifyConstants() {
        // private constructor to prevent initialization.
    }

    /*******************
     * USM process metrics
     */
    public static final String USM_METRIC_PROCESS_CPU_USAGE = "Process Cpu Usage";
    public static final String USM_METRIC_PROCESS_CPU_TIME = "Process Cpu Time";
    public static final String USM_METRIC_PROCESS_CPU_KERNEL_TIME = "Process Cpu Kernel Time";
    public static final String USM_METRIC_PROCESS_TOTAL_CPU_TIME = "Total Process Cpu Time";
    public static final String USM_METRIC_PROCESS_GROUP_ID = "Process GroupId";
    public static final String USM_METRIC_PROCESS_USER_ID = "Process User Id";
    public static final String USM_METRIC_PROCESS_TOTAL_PAGE_FAULTS = "Total Num Of PageFaults";
    public static final String USM_METRIC_PROCESS_TOTAL_RESIDENTAL_MEMORY = "Total Process Residental Memory";
    public static final String USM_METRIC_PROCESS_TOTAL_SHARED_MEMORY = "Total Process Shared Memory";
    public static final String USM_METRIC_PROCESS_CPU_TOTAL_VIRTUAL_MEMORY = "Total Process Virtual Memory";
    public static final String USM_METRIC_PROCESS_KERNEL_SCHEDULING_PRIORITY = "Kernel Scheduling Priority";
    public static final String USM_METRIC_PROCESS_ACTIVE_THREADS = "Num Of Active Threads";
    public static final String USM_METRIC_AVAILABLE_PROCESSORS = "Available Processors";
    public static final String USM_METRIC_COMMITTED_VIRTUAL_MEM_SIZE = "Committed Virtual Memory Size";
    public static final String USM_METRIC_THREAD_COUNT = "Thread Count";
    public static final String USM_METRIC_PEAK_THREAD_COUNT = "Peak Thread Count";

    public static final int SSH_PORT = 22;

    /*******************
     * REST Headers
     */
    public static final String REST_API_VERSION_HEADER = "cloudify-api-version";


    /*******************
     * HTTP status codes
     */
    public static final int HTTP_STATUS_CODE_OK = 200;
    public static final int HTTP_STATUS_INTERNAL_SERVER_ERROR = 500;
    public static final int HTTP_STATUS_NOT_FOUND = 404;
    public static final int HTTP_STATUS_ACCESS_DENIED = 403;
    public static final int HTTP_STATUS_UNAUTHORIZED = 401;


    /*******************
     * Spring security environment variable
     */
    public static final String SECURITY_FILE_NAME = "spring-security.xml";
    public static final String KEYSTORE_FILE_NAME = "keystore";
    public static final String SPRING_SECURITY_CONFIG_FILE_ENV_VAR = "SPRING_SECURITY_CONFIG_FILE";
    public static final String KEYSTORE_FILE_ENV_VAR = "KEYSTORE_FILE";
    public static final String KEYSTORE_PASSWORD_ENV_VAR = "KEYSTORE_KEY";
    public static final String SPRING_ACTIVE_PROFILE_ENV_VAR = "SPRING_PROFILES_ACTIVE";
    public static final String SPRING_PROFILE_NON_SECURE = "nonsecure";
    public static final String SPRING_PROFILE_SECURE = "secure";

    /*******************
     *
     */
    public static final String NEW_LINE = System.getProperty("line.separator");
    public static final String TAB_CHAR = "\t";

    /**
     * REST API parameter names.
     */
    public static final String TEMPLATES_DIR_PARAM_NAME = "templatesFolder";
    public static final String TEMPLATE_FOLDER_PREFIX = "templates_";
    public static final String SERVICE_OVERRIDES_FILE_PARAM = "serviceOverridesFile";
    public static final String APPLICATION_OVERRIDES_FILE_PARAM = "recipeOverridesFile";
    public static final String CLOUD_OVERRIDES_FILE_PARAM = "cloudOverridesFile";


    /**
     * Additional templates folder name.
     */
    public static final String ADDITIONAL_TEMPLATES_FOLDER_NAME = "additionalTemplates";

    /*******************
     *
     */
    public static String DYNAMIC_BYON_NAME = "dynamic-byon";
    public static String DYNAMIC_BYON_START_MACHINE_KEY = "startMachine";
    public static String DYNAMIC_BYON_STOP_MACHINE_KEY = "stopMachine";
    public static String DYNAMIC_BYON_START_MNG_MACHINES_KEY = "startManagementMachines";
    public static String DYNAMIC_BYON_STOP_MNG_MACHINES_KEY = "stopManagementMachines";

    /********************
     * Service grid components system props and environment variables
     */
    public static final String LUS_PORT_CONTEXT_PROPERTY = "com.sun.jini.reggie.initialUnicastDiscoveryPort";
    public static final String GSM_HTTP_PORT_CONTEXT_PROPERTY = "com.gigaspaces.start.httpPort";
    public static final String LRMI_BIND_PORT_CONTEXT_PROPERTY = "com.gs.transport_protocol.lrmi.bind-port";
    public static final String AGENT_PORT_CONTEXT_PROPERTY = "com.gigaspaces.system.registryPort";

    /********************
     * management service environment variable constants
     */
    public static final String REST_PORT_ENV_VAR = "REST_PORT_ENV_VAR";
    public static final String WEBUI_PORT_ENV_VAR = "WEBUI_PORT_ENV_VAR";
    public static final String WEBUI_MAX_MEMORY_ENVIRONMENT_VAR = "WEBUI_MAX_MEMORY_ENVIRONMENT_VAR";
    public static final String REST_MAX_MEMORY_ENVIRONMENT_VAR = "REST_MAX_MEMORY_ENVIRONMENT_VAR";
    public static final String GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR = "GSC_LRMI_PORT_RANGE_ENVIRONMENT_VAR";
    public static final String GSA_JAVA_OPTIONS_ENVIRONMENT_VAR = "GSA_JAVA_OPTIONS";
    public static final String LUS_JAVA_OPTIONS_ENVIRONMENT_VAR = "LUS_JAVA_OPTIONS";
    public static final String GSM_JAVA_OPTIONS_ENVIRONMENT_VAR = "GSM_JAVA_OPTIONS";
    public static final String ESM_JAVA_OPTIONS_ENVIRONMENT_VAR = "ESM_JAVA_OPTIONS";    
    public static final String LRMI_PORT_OR_RANGE_SYS_PROP = "com.gs.transport_protocol.lrmi.bind-port";
    public static final String LUS_IP_ADDRESS_ENV = "LUS_IP_ADDRESS";    

    /*********************
     * service grid components configuration.
     */
    public static final int DEFAULT_REST_PORT = 8100;
    public static final int SECURE_REST_PORT = 8100;
    public static final int DEFAULT_WEBUI_PORT = 8099;
    public static final int SECURE_WEBUI_PORT = 8099;

    public static final int MANAGEMENT_SPACE_MEMORY_IN_MB = 64;

    /*********************
     * localcloud grid component configuration
     */
    public static final int DEFAULT_LOCALCLOUD_REST_WEBUI_SPACE_MEMORY_IN_MB = 256;
    public static final int DEFAULT_LOCALCLOUD_GSA_GSM_ESM_LUS_MEMORY_IN_MB = 128;

    /**
     * Http timeouts
     */
    public static final int DEFAULT_HTTP_CONNECTION_TIMEOUT = 60 * 1000; // one minute
    public static final int DEFAULT_HTTP_READ_TIMEOUT = 60 * 1000 * 2; // two minutes per request


    /**
     * Rest response formatting keywords
     */
    public static final String STATUS_KEY = "status";
    public static final String RESPONSE_KEY = "response";
    public static final String ERROR_ARGS_KEY = "error_args";
    public static final String ERROR_STATUS = "error";
    public static final String SUCCESS_STATUS = "success";

    /*
     * Request and response keywords
     */
    public static final String REQ_PARAM_TIMEOUT_IN_MINUTES = "timeoutInMinutes";


    /**
     * CLI Printouts
     */
    public static final String TIMEOUT_ERROR_MESSAGE = "The operation timed out. "
            + "Try to increase the timeout using the -timeout flag";

    /*********
     * Persistent management
     */
    public static final String PERSISTENCE_PROFILE_PERSISTENT = "persistent";
    public static final String PERSISTENCE_PROFILE_TRANSIENT = "transient";
    public static final String PERSISTENCE_DIRECTORY_DEPLOY_RELATIVE_PATH = "deploy";
    public static final String PERSISTENCE_DIRECTORY_SPACE_RELATIVE_PATH = "management-space";
    public static final String PERSISTENCE_DIRECTORY_STATE_RELATIVE_PATH = "gsm";


    public static final String SYSTEM_PROPERTY_ESM_DISCOVERY_POLLING_INTERVAL_SECONDS =
            "com.gs.esm.discovery_polling_interval_seconds";

    /*******
     *  Upload file constants.
     */
    // load from property, default to tempdir
    public static final String UPLOAD_FILE_PARAM_NAME = "file";
    public static final String UPLOADS_FOLDER_NAME = "restUploads";
    public static final int DEFAULT_UPLOAD_TIMEOUT_MILLIS = 5 * 60 * 1000;
    public static final int DEFAULT_UPLOAD_SIZE_LIMIT_BYTES = 100 * 1000 * 1000;

    // install-service validators
    public static final long SERVICE_OVERRIDES_FILE_LENGTH_LIMIT_BYTES = 20 * FileUtils.ONE_KB;
    public static final long APPLICATION_OVERRIDES_FILE_LENGTH_LIMIT_BYTES = 20 * FileUtils.ONE_KB;
    public static final long CLOUD_OVERRIDES_FILE_LENGTH_LIMIT_BYTES = 10 * FileUtils.ONE_KB;
    public static final long CLOUD_CONFIGURATION_FILE_LENGTH_LIMIT_BYTES = 10 * FileUtils.ONE_KB;

    // install-service constants

    public static final String INSTALL_SERVICE_REQUEST_PARAM_NAME = "install-service-request";
    public static final String EXTRACTED_FILES_FOLDER_NAME = "extracted";
    public static final int LIFECYCLE_EVENT_POLLING_INTERVAL_SEC = 4;

    // uninstall-service constants
    public static final String UNDEPLOYED_SUCCESSFULLY_EVENT = "Internal event - undeployed successfully";

    // system property passed in test-recipe command
    public static final String TEST_RECIPE_TIMEOUT_SYSPROP = "com.gs.usm.RecipeShutdownTimeout";
    
    /**
     * service controller url
     */
	public static final String SERVICE_CONTROLLER_URL = "service";

	/**
	 * new rest client
	 */
	public static final boolean IS_NEW_REST_CLIENT_DEFAULT = true;
	/**
	 * built-in command prefix
	 */
	public static final String BUILT_IN_COMMAND_PREFIX = "cloudify:";
	
	/**
	 * service name
	 */
	public static final String ILlEGAL_SERVICE_NAME_PREFIX = ".";
	public static final String ILlEGAL_SERVICE_NAME_SUFFIX = ".";
	
	/**
	 * dump file
	 */
	public static final long DEFAULT_DUMP_FILE_SIZE_LIMIT = 10 * 1024 * 1024;
	public static final String[] DEFAULT_DUMP_PROCESSORS = { 
		ProcessorTypes.SUMMARY.getName(), ProcessorTypes.NETWORK.getName(), 
		ProcessorTypes.THREAD.getName(), ProcessorTypes.LOG.getName()};
	
	/*******
	 * Name of local cloud.
	 */
	public static final String LOCAL_CLOUD_NAME = "local-cloud";

	/*****
	 * Useful json mime type constant.
	 */
	public static final String MIME_TYPE_APPLICATION_JSON = "application/json";
   
	// CHECKSTYLE:ON

}


