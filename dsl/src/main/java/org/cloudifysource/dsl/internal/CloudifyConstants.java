package org.cloudifysource.dsl.internal;

/*********************
 * A shared location for all constants used by multiple cloudify constants.
 * 
 * @author barakme
 * 
 */
public class CloudifyConstants {

	/***
	 * USM service deployment States
	 */
	public static enum USMState {

		INITIALIZING,
		LAUNCHING,
		RUNNING,
		SHUTTING_DOWN
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
	public static final String CONTEXT_PROPERTY_APPLICATION_NAME = "com.gs.application";
	
	/**********
	 * Key names for invocation request and response parameters.
	 */
	public static final String INVOCATION_PARAMETER_COMMAND_NAME = "GS_USM_CommandName";
	public static final String INVOCATION_PARAMETERS_KEY = "GS_USM_Command_Parameters";
	public static final String INVOCATION_RESPONSE_STATUS = "Invocation_Success";
	public static final String INVOCATION_RESPONSE_EXCEPTION = "Invocation_Exception";
	public static final String INVOCATION_RESPONSE_RESULT = "Invocation_Result";
	public static final String INVOCATION_RESPONSE_COMMAND_NAME= "Invocation_Command_Name";
	public static final String INVOCATION_RESPONSE_INSTANCE_ID= "Invocation_Instance_ID";
	public static final String INVOCATION_RESPONSE_INSTANCE_NAME= "Invocation_Instance_Name";


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
	public static final String USM_MONITORS_CHILD_PROCESS_ID = "Child Process ID";
	public static final String USM_MONITORS_ACTUAL_PROCESS_ID = "Actual Process ID";
	
	/****************
	 * Key names for USM Details
	 */
	public static final String USM_DETAILS_PRIVATE_IP= "Cloud Private IP";
	public static final String USM_DETAILS_PUBLIC_IP= "Cloud Public IP";
	
	
	
	/****************
	 * Key names for USM Exposed Monitors and Details
	 */
	public static final String USM_MONITORS_SERVICE_ID = "USM";
	public static final String USM_DETAILS_SERVICE_ID = "USM";	
	
	
	/*********************
	 * Key names for USM parameters that can be configured using the service custom parameters.
	 * 
	 */
	public static final String USM_PARAMETERS_TAILER_INTERVAL = "TailerInterval";
	


	/*************************************
	 * Keys for Elastic Provisioning properties used with ESM machine provisioning.
	 * 
	 */
	public static final String ELASTIC_PROPERTIES_CLOUD_CONFIGURATION = "__CLOUDIFY__CLOUD_CONFIGURATION";
	public static final String ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME = "__CLOUDIFY__CLOUD_TEMPLATE_NAME";


	/********************
	 * Key names for lifecycle event results.
	 */
	public static final String USM_EVENT_EXEC_SUCCESSFULLY = " completed";
	public static final String USM_EVENT_EXEC_FAILED = " failed";
	public static final String USM_EVENT_EXEC_SUCCEED_MESSAGE = "[OK]";
	public static final String USM_EVENT_EXEC_FAILED_MESSAGE = "[ERROR]";

	
	/***********************************
	 * Key names for environment variables available to agent in a cloudify environment
	 */
	public static final String CLOUDIFY_AGENT_ENV_PRIVATE_IP = "CLOUDIFY_AGENT_ENV_PRIVATE_KEY";
	public static final String CLOUDIFY_AGENT_ENV_PUBLIC_IP = "CLOUDIFY_AGENT_ENV_PUBLIC_KEY";
	
	
	
	/***************
	 * Misc.
	 */
	public static final String DEFAULT_APPLICATION_NAME = "default";
	public static final String MANAGEMENT_SPACE_NAME = "cloudifyManagementSpace";
	public static final String MANAGEMENT_APPLICATION_NAME = "management";
	public static final String USM_LIB_DIR = "usmlib";
	// TODO - remove forward slashes
	public static final String SERVICE_EXTERNAL_FOLDER = "/ext/";
	
	
	private CloudifyConstants() {
		// private constructor to prevent initialization.
	}

}
