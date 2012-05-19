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

import com.gigaspaces.internal.license.LicenseManagerVerifier;

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
	public static final String CONTEXT_PROPERTY_ELASTIC = "com.gs.service.elastic";
	public static final String CONTEXT_PROPERTY_TEMPLATE = "com.gs.service.template";
	
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
	public static final String USM_MONITORS_CHILD_PROCESS_ID = "USM_Child Process ID";
	public static final String USM_MONITORS_ACTUAL_PROCESS_ID = "USM_Actual Process ID";
	
	/****************
	 * Key names for USM Details
	 */
	public static final String USM_DETAILS_PRIVATE_IP= "Cloud Private IP";
	public static final String USM_DETAILS_PUBLIC_IP= "Cloud Public IP";
	public  static final String USM_DETAILS_URL = "url";
	public static final String USM_DETAILS_ICON = "icon";
	
	
	
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
	public static final String CLOUDIFY_AGENT_ENV_PRIVATE_IP = "CLOUDIFY_AGENT_ENV_PRIVATE_IP";
	public static final String CLOUDIFY_AGENT_ENV_PUBLIC_IP = "CLOUDIFY_AGENT_ENV_PUBLIC_IP";
	
	
	
	/***************
	 * Misc.
	 */
	public static final String DEFAULT_APPLICATION_NAME = "default";
	public static final String MANAGEMENT_SPACE_NAME = LicenseManagerVerifier.MANAGEMENT_SPACE_NAME;
	public static final String MANAGEMENT_APPLICATION_NAME = "management";
	public static final String USM_LIB_DIR = "usmlib";
	// TODO - remove forward slashes
	public static final String SERVICE_EXTERNAL_FOLDER = "/ext/";
	
	/***************
	 * Reason codes for rest exceptions
	 */
	public static final String ERR_REASON_CODE_FAILED_TO_LOCATE_APP = "failed_to_locate_app";

	
	/********************
	 * Custom properties for known cloudify settings.
	 * 
	 */
	public static final String CUSTOM_PROPERTY_ENABLE_PID_MONITOR = "org.cloudifysource.enable-pid-monitor";
	public static final String CUSTOM_PROPERTY_ENABLE_TCP_PORT_MONITOR = "org.cloudifysource.enable-port-monitor";
	public static final String CUSTOM_PROPERTY_ENABLE_START_PROCESS_MONITOR = "org.cloudifysource.enable-start-process-monitor";
	public static final String CUSTOM_PROPERTY_STOP_DETECTION_ON_ALL_PROCESSES = "org.cloudifysource.stop-detection-on-all-processes";
	
	/*******************
	 * event lifecycle polling parameters. 
	 * 
	 */
	public static final String CURSOR_POS = "curserPos";
	public static final String LIFECYCLE_LOGS = "lifecycleLogs";
	public static final String IS_TASK_DONE = "isDone";
	public static final String POLLING_TIMEOUT_EXCEPTION = "TimeoutException";
	public static final String POLLING_EXCEPTION = "PollingException";
	public static final String LIFECYCLE_EVENT_CONTAINER_ID = "lifecycleEventContainerID";
	public static final String SERVICE_ORDER = "srviceOrder";
	
	/*******************
	 * default ports (LUS and REST)
	 */
	// TODO : this port should be configurable
	public static final int DEFAULT_REST_PORT = 8100;
	public static final int SSH_PORT = 22;
	public static final int DEFAULT_LUS_PORT = net.jini.discovery.Constants.getDiscoveryPort();
	public static final int DEFAULT_LOCALCLOUD_LUS_PORT = DEFAULT_LUS_PORT + 2;

	
	/************************
	 * Keys used by Agentless Installer to modify default installer behavior.
	 */
	public static final String INSTALLER_CUSTOM_DATA_SFTP_PREFERRED_AUTHENTICATION_METHODS_KEY = "installer.sftp.preferredAuthentications";
	
	private CloudifyConstants() {
		// private constructor to prevent initialization.
	}

	// CHECKSTYLE:ON
}
