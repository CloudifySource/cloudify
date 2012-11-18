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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * @author uri
 */
public final class RestUtils {
	
	/**
	 * 
	 */
    public static final String STATUS_KEY = "status";
    /**
     * 
     */
    public static final String ERROR = "error";
    /**
     * 
     */
    public static final String SUCCESS = "success";
    /**
     * 
     */
    public static final String RESPONSE_KEY = "response";
    
    private static final String ERROR_ARGS = "error_args";
    
	/**
	 * 
	 */
    public static final int TIMEOUT_IN_SECOND = 5;
    
    private RestUtils() {
    	
    }

    /**
     * Creates a map to be converted to a Json map in the response's body.
     * @return A map contains "status"="success".
     */
    @SuppressWarnings("unchecked")
	public static Map<String, Object> successStatus() {
        return newHashMap(mapEntry(STATUS_KEY, (Object) SUCCESS));
    }

    /**
     * 
     * @param response .
     * @return A map contains "status"="success", "response"=response.
     */
    @SuppressWarnings("unchecked")
	public static Map<String, Object> successStatus(final Object response) {
        return newHashMap(mapEntry(STATUS_KEY, (Object) SUCCESS), mapEntry(RESPONSE_KEY, response));
    }

    /**
     * 
     * @param responseKey .
     * @param response .
     * @return A map contains "status"="success", responseKey=response.
     */
    @SuppressWarnings("unchecked")
	public static Map<String, Object> successStatus(final String responseKey, final Object response) {
        return newHashMap(mapEntry(STATUS_KEY, (Object) SUCCESS), mapEntry(responseKey, response));
    }

    /**
     * 
     * @param errorDesc .
     * @return A map contains "status"="error", "error"=errorDesc.
     */
    @SuppressWarnings("unchecked")
	public static Map<String, Object> errorStatus(final String errorDesc) {
        return newHashMap(mapEntry(STATUS_KEY, (Object) ERROR), mapEntry(ERROR, (Object) errorDesc));
    }

    /**
     * 
     * @param errorDesc .
     * @param args .
     * @return A map contains "status"="error", "error"=errorDesc, "error_args"=args.
     */
    @SuppressWarnings("unchecked")
	public static Map<String, Object> errorStatus(final String errorDesc, final String... args) {
        return newHashMap(mapEntry(STATUS_KEY, (Object) ERROR), mapEntry(ERROR, (Object) errorDesc), 
        		mapEntry(ERROR_ARGS, (Object) args));
    }
    
	public static String getResponseBody(final HttpResponse response, final HttpRequestBase httpMethod)
			throws IOException, RestErrorException {

		InputStream instream = null;
		try {
			final HttpEntity entity = response.getEntity();
			if (entity == null) {
				final RestErrorException e = 
						new RestErrorException("comm_error", 
								httpMethod.getURI().toString(), " response entity is null");
				throw e;
			}
			instream = entity.getContent();
			return getStringFromStream(instream);
		} finally {
			if (instream != null) {
				try {
					instream.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static String getStringFromStream(final InputStream is)
			throws IOException {
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

	public static boolean isLocalHost(final InetAddress address) {
		 // If the address is local or loop back return true
	    if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
			return true;
		}

	    // If the address is defined on any interface return true, otherwise return false.
	    try {
	        return NetworkInterface.getByInetAddress(address) != null;
	    } catch (SocketException e) {
	        return false;
	    }
	}

}
