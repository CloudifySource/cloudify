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
package org.cloudifysource.rest.util;

import static org.cloudifysource.rest.util.CollectionUtils.mapEntry;
import static org.cloudifysource.rest.util.CollectionUtils.newHashMap;

import java.util.Map;

/**
 * @author uri
 */
public class RestUtils {
    public static final String STATUS_KEY = "status";
    public static final String ERROR = "error";
    public static final String SUCCESS = "success";
    private static final String RESPONSE_KEY = "response";
    private static final String ERROR_ARGS = "error_args";

    public static Map<String, Object> successStatus() {
        return newHashMap(mapEntry(STATUS_KEY, (Object)SUCCESS));
    }

    public static Map<String, Object> successStatus(Object response) {
        return newHashMap(mapEntry(STATUS_KEY, (Object)SUCCESS), mapEntry(RESPONSE_KEY, response));
    }

    public static Map<String, Object> successStatus(String responseKey, Object response) {
        return newHashMap(mapEntry(STATUS_KEY, (Object)SUCCESS), mapEntry(responseKey, response));
    }

    public static Map<String, Object> errorStatus(String errorDesc) {
        return newHashMap(mapEntry(STATUS_KEY, (Object)ERROR), mapEntry(ERROR, (Object)errorDesc));
    }

    public static Map<String, Object> errorStatus(String errorDesc, String... args) {
        return newHashMap(mapEntry(STATUS_KEY, (Object)ERROR), mapEntry(ERROR, (Object)errorDesc), mapEntry(ERROR_ARGS, (Object)args));
    }

}
