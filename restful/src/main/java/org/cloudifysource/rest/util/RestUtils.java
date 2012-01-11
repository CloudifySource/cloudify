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
