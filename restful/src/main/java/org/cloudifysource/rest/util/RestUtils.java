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
	
	private static final String VERBOSE = "verbose";

	private static final Logger logger = Logger.getLogger(RestUtils.class.getName());

	/**
	 *
	 */
	public static final int TIMEOUT_IN_SECOND = 5;

	private RestUtils() {

	}

	/**
	 * Creates a map to be converted to a Json map in the response's body.
	 * 
	 * @return A map contains "status"="success".
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> successStatus() {
		return newHashMap(mapEntry(CloudifyConstants.STATUS_KEY, (Object) CloudifyConstants.SUCCESS_STATUS));
	}

	/**
	 * 
	 * @param response
	 *            .
	 * @return A map contains "status"="success", "response"=response.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> successStatus(final Object response) {
		return newHashMap(mapEntry(CloudifyConstants.STATUS_KEY, (Object) CloudifyConstants.SUCCESS_STATUS), 
				mapEntry(CloudifyConstants.RESPONSE_KEY, response));
	}

	/**
	 * 
	 * @param responseKey
	 *            .
	 * @param response
	 *            .
	 * @return A map contains "status"="success", responseKey=response.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> successStatus(final String responseKey, final Object response) {
		return newHashMap(mapEntry(CloudifyConstants.STATUS_KEY, (Object) CloudifyConstants.SUCCESS_STATUS), 
				mapEntry(responseKey, response));
	}

	/**
	 * 
	 * @param errorDesc
	 *            .
	 * @return A map contains "status"="error", "error"=errorDesc.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> errorStatus(final String errorDesc) {
		return newHashMap(mapEntry(CloudifyConstants.STATUS_KEY, (Object) CloudifyConstants.ERROR_STATUS), 
				mapEntry(CloudifyConstants.ERROR_STATUS, (Object) errorDesc));
	}

	/**
	 * 
	 * @param errorDesc
	 *            .
	 * @param args
	 *            .
	 * @return A map contains "status"="error", "error"=errorDesc,
	 *         "error_args"=args.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> errorStatus(final String errorDesc, final String... args) {
		return newHashMap(mapEntry(CloudifyConstants.STATUS_KEY, (Object) CloudifyConstants.ERROR_STATUS), 
				mapEntry(CloudifyConstants.ERROR_STATUS, (Object) errorDesc),
				mapEntry(CloudifyConstants.ERROR_ARGS_KEY, (Object) args));
	}

	/************
	 * Creates a Rest error map with verbose data.
	 * @param errorDesc the error name.
	 * @param verboseData the verbose data.
	 * @param args the error description parameters.
	 * @return the rest error map.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> verboseErrorStatus(final String errorDesc, final String verboseData,
			final String... args) {
		return newHashMap(mapEntry(CloudifyConstants.STATUS_KEY, (Object) CloudifyConstants.ERROR_STATUS), 
				mapEntry(CloudifyConstants.ERROR_STATUS, (Object) errorDesc), 
				mapEntry(CloudifyConstants.ERROR_ARGS_KEY, (Object) args), mapEntry(VERBOSE, (Object) verboseData));
	}

	/************
	 * Creates a Rest error map.
	 * @param errorDesc .
	 * @param args .
	 * @return A map contains "status"="error", "error"=errorDesc, "error_args"=args.
	 */
	public static Map<String, Object> errorStatus(final String errorDesc, final Object... args) {
		return newHashMap(mapEntry(CloudifyConstants.STATUS_KEY, (Object) CloudifyConstants.ERROR_STATUS), 
				mapEntry(CloudifyConstants.ERROR_STATUS, (Object) errorDesc),
				mapEntry(CloudifyConstants.ERROR_ARGS_KEY, (Object) args));
	}

	/**
	 * 
	 * @param response .
	 * @param httpMethod .
	 * @return response's body.
	 * @throws IOException .
	 * @throws RestErrorException .
	 */
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

	/**
	 * 
	 * @param is .
	 * @return .
	 * @throws IOException .
	 */
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

	/**
	 * Executing HTTP method.
	 * @param httpMethod .
	 * @param httpClient .
	 * @return the result.
	 * @throws RestErrorException .
	 */
	public static Object executeHttpMethod(final HttpRequestBase httpMethod, final DefaultHttpClient httpClient)
			throws RestErrorException {
		logger.log(Level.FINE, "[executeHttpMethod] - executing http method: " + httpMethod.getMethod()
				+ " to URI: " + httpMethod.getURI());

		String responseBody;
		try {
			final HttpResponse response = httpClient.execute(httpMethod);
			logger.log(Level.FINE, "[executeHttpMethod]: got response from " + httpMethod.getURI()
					+ ". Response: " + response);
			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != CloudifyConstants.HTTP_STATUS_CODE_OK) {
				responseBody = getResponseBody(response, httpMethod);
				logger.log(Level.FINE, "[executeHttpMethod] - got response with status " + statusCode
						+ ". responseBody: " + responseBody);
				try {
					final Map<String, Object> errorMap = jsonToMap(responseBody);
					final String status = (String) errorMap.get(CloudifyConstants.STATUS_KEY);
					if (CloudifyConstants.ERROR_STATUS.equals(status)) {
						final String reason = (String) errorMap.get(CloudifyConstants.ERROR_STATUS);
						@SuppressWarnings("unchecked")
						final List<String> reasonsArgs = (List<String>) errorMap.get(CloudifyConstants.ERROR_ARGS_KEY);
						logger.log(Level.WARNING, "Failed to execute rest request to " + httpMethod.getURI()
								+ ", error: " + reason + ", error args: " + reasonsArgs);
						throw new RestErrorException(reason);
					}
				} catch (final IOException e) {
					throw new RestErrorException("Failed to send http request to " + httpMethod.getURI().toString()
							+ ", error: e.getMessage().");
				}
				throw new RestErrorException("Failed to send http request to " + httpMethod.getURI().toString()
						+ ", statusCode is is " + statusCode);
			}
			Map<String, Object> responseMap;
			try {
				responseBody = getResponseBody(response, httpMethod);
				logger.log(Level.FINEST, "response body " + responseBody);
				responseMap = jsonToMap(responseBody);
			} catch (final IOException e) {
				logger.log(Level.WARNING, "Failed to read response from " + httpMethod.getURI()
						+ ", error: " + e);
				throw new RestErrorException("Failed to read response from "
						+ httpMethod.getURI().toString() + ", got an IOException- " + e.getMessage());
			}
			return responseMap.get(CloudifyConstants.RESPONSE_KEY);

		} catch (final ClientProtocolException e) {
			logger.log(Level.WARNING, "Failed to execute rest request to " + httpMethod.getURI()
					+ ", error: " + e);
			throw new RestErrorException("Failed to send http request to "
					+ httpMethod.getURI().toString() + ", got ClientProtocolException- " + e.getMessage());
		} catch (IOException e1) {
			logger.log(Level.WARNING, "Failed to execute rest request to " + httpMethod.getURI()
					+ ", error: " + e1);
			throw new RestErrorException("Failed to send http request to "
					+ httpMethod.getURI().toString() + ", got IOException- " + e1.getMessage());
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
