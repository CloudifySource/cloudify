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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.rest.controllers.RestErrorException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * @author uri
 */
public final class RestUtils {
	private static final Logger logger = Logger.getLogger(RestUtils.class.getName());

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

	public static Object executeHttpMethod(final HttpRequestBase httpMethod, 
			final String responseJsonKey, final DefaultHttpClient httpClient) 
					throws RestErrorException {
		logger.log(Level.INFO, "executeHttpMethod: executing http method: " + httpMethod.getMethod() 
				+ " to URI: " + httpMethod.getURI());
		
		String responseBody;
		try {
			final HttpResponse response = httpClient.execute(httpMethod);
			logger.log(Level.INFO, "executeHttpMethod: got response from " + httpMethod.getURI() 
					+ ". Response: " +  response);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != CloudifyConstants.HTTP_STATUS_CODE_OK) {
				responseBody = getResponseBody(response, httpMethod);
				logger.log(Level.INFO, "executeHttpMethod: got response with status " + statusCode 
						+ ". responseBody: " +  responseBody);
				try {
					final Map<String, Object> errorMap = jsonToMap(responseBody);
					final String status = (String) errorMap.get(STATUS_KEY);
					if (ERROR.equals(status)) {
						final String reason = (String) errorMap.get(ERROR);
						@SuppressWarnings("unchecked")
						final List<String> reasonsArgs = (List<String>) errorMap.get(ERROR_ARGS);
						logger.log(Level.INFO, "failed to execute rest request to " + httpMethod.getURI() 
								+ ", error: " + reason + ", error args: " + reasonsArgs);
						throw new RestErrorException(reason, reasonsArgs.toArray(new String[]{}));
					}
				} catch (final IOException e) {
					throw new RestErrorException("failed_to_send_rest_request", 
							httpMethod.getURI().toString(), e.getMessage()); 
				}
				throw new RestErrorException("failed_to_send_rest_request", 
						httpMethod.getURI().toString(), "statusCode is not OK, it is " + statusCode); 
			}

			logger.log(Level.INFO, "executed rest request to " + httpMethod.getURI()); 
			Map<String, Object> responseMap;
			try {
				responseBody = getResponseBody(response, httpMethod);
				logger.log(Level.INFO, "response body " + responseBody); 
				responseMap = jsonToMap(responseBody);
			} catch (final IOException e) {
				logger.log(Level.INFO, "failed to read response from " + httpMethod.getURI() 
						+ ", error: " + e);
				throw new RestErrorException("failed_to_read_rest_response", 
						httpMethod.getURI().toString(), "IOException- " + e.getMessage());
			}
			return responseJsonKey != null ? responseMap.get(RESPONSE_KEY) : responseMap;

		} catch (final ClientProtocolException e) {
			logger.log(Level.INFO, "failed to execute rest request to " + httpMethod.getURI() 
					+ ", error: " + e);
			throw new RestErrorException("failed_to_send_rest_request", 
					httpMethod.getURI().toString(), "ClientProtocolException: " + e.getMessage());
		} catch (IOException e1) {
			logger.log(Level.INFO, "failed to execute rest request to " + httpMethod.getURI() 
					+ ", error: " + e1);
			throw new RestErrorException("failed_to_send_rest_request", 
					httpMethod.getURI().toString(), "IOException: " + e1.getMessage());
		} finally {
			httpMethod.abort();
		}
	}

	private static Map<String, Object> jsonToMap(final String response) throws IOException {
		final JavaType javaType = TypeFactory.type(Map.class);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(response, javaType);
	}

}
