/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package org.cloudifysource.rest.util;

import static org.cloudifysource.rest.util.CollectionUtils.mapEntry;
import static org.cloudifysource.rest.util.CollectionUtils.newHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.rest.controllers.RestErrorException;

/**
 * @author uri
 */
public final class RestUtils {

	private static final String VERBOSE = "verbose";

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
	 * @return A map contains "status"="error", "error"=errorDesc, "error_args"=args.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> errorStatus(final String errorDesc, final String... args) {
		return newHashMap(mapEntry(CloudifyConstants.STATUS_KEY, (Object) CloudifyConstants.ERROR_STATUS),
				mapEntry(CloudifyConstants.ERROR_STATUS, (Object) errorDesc),
				mapEntry(CloudifyConstants.ERROR_ARGS_KEY, (Object) args));
	}

	/************
	 * Creates a Rest error map with verbose data.
	 * 
	 * @param errorDesc
	 *            the error name.
	 * @param verboseData
	 *            the verbose data.
	 * @param args
	 *            the error description parameters.
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
	 * 
	 * @param errorDesc
	 *            .
	 * @param args
	 *            .
	 * @return A map contains "status"="error", "error"=errorDesc, "error_args"=args.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> errorStatus(final String errorDesc, final Object... args) {
		return newHashMap(mapEntry(CloudifyConstants.STATUS_KEY, (Object) CloudifyConstants.ERROR_STATUS),
				mapEntry(CloudifyConstants.ERROR_STATUS, (Object) errorDesc),
				mapEntry(CloudifyConstants.ERROR_ARGS_KEY, (Object) args));
	}

	/**
	 * 
	 * @param response
	 *            .
	 * @param httpMethod
	 *            .
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
	 * @param is
	 *            .
	 * @return .
	 * @throws IOException .
	 */
	public static String getStringFromStream(final InputStream is)
			throws IOException {
		final BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(is));
		final StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			sb.append(line);
		}
		return sb.toString();
	}

}
