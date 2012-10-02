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
package org.cloudifysource.rest;

import org.cloudifysource.dsl.internal.CloudifyConstants;

/**
 * @author uri
 */
public interface ResponseConstants {
    String SERVICE_INSTANCE_UNAVAILABLE = "service_instance_unavailable";
    String FAILED_TO_LOCATE_LUS = "failed_to_locate_lookup_service";
    String FAILED_TO_LOCATE_GSM = "failed_to_locate_gsm";
    String FAILED_TO_LOCATE_ESM = "failed_to_locate_esm";
    String FAILED_TO_LOCATE_SERVICE_AFTER_DEPLOYMENT = "failed_to_locate_service_after_deployment";    
    String FAILED_TO_LOCATE_SERVICE = "failed_to_locate_service";
    String FAILED_TO_LOCATE_APP = CloudifyConstants.ERR_REASON_CODE_FAILED_TO_LOCATE_APP;
    String CANNOT_UNINSTALL_MANAGEMENT_APP = "cannot_uninstall_management_application";
    String FAILED_TO_ADD_INSTANCE = "failed_to_add_instance";
    String FAILED_TO_INVOKE_INSTANCE = "failed_to_invoke_instance";
    String SERVICE_NOT_ELASTIC = "service_not_elastic";
    String SET_INSTANCES_NOT_SUPPORTED_IN_EAGER = "set_instances_not_supported_on_eager";
    String NO_PROCESSING_UNIT_INSTANCES_FOUND_FOR_INVOCATION = "no_processing_unit_instances_found_for_invocation";

    
    
}
