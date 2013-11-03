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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyMessageKeys;
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
	
	/**
	 * Creates a folder a unique name based on the given basicFolderName, inside the specified parent folder.
	 * If the folder by that name already exists in the parent folder - a number is appended to that name, until
	 * a unique name is found. e.g.: "myfolder1", "myfolder2" ... "myfolder99" (max index is set by maxAppender).
	 * If all those names are already in use (meaning there are existing folders with these names) -
	 * we create a completely new random name using "File.createTempFile".
	 * 
	 * @param parentFolder The folder to contain the new folder created by this method.
	 * @param basicFolderName The base name to be used for the new folder. Numbers might be appended to this name to 
	 * reach uniqueness. 
	 * @param maxAppender The maximum number appended to the basic folder name to reach uniqueness. If a unique name
	 * is not found for the folder and the maximum is reached, a new random name using "File.createTempFile".
	 * @return The unique name
	 * @throws IOException Indicates a failure to generate a unique folder name
	 */
	public static String createUniqueFolderName(final File parentFolder, final String basicFolderName, 
			final int maxAppender) throws IOException {
				
    	int index = 0;
    	boolean uniqueNameFound = false;
    	String folderName = basicFolderName;
    	
    	while (!uniqueNameFound && index < maxAppender) {
			//create a new name (temp1, temp2... temp99)
    		folderName = basicFolderName + (++index);
    		
        	File restTempFolder = new File(parentFolder, folderName);
    		if (!restTempFolder.exists()) {
    			uniqueNameFound = true;
    		}
    	}    	
    	
    	if (!uniqueNameFound) {
    		//create folder with a new unique name
   			File tempFile = File.createTempFile(folderName, ".tmp");
   			tempFile.deleteOnExit();
   			folderName = StringUtils.substringBeforeLast(tempFile.getName(), ".tmp");
   			uniqueNameFound = true;
    	}
    	
    	if (uniqueNameFound) {
    		return folderName;
    	} else {
    		throw new IOException(CloudifyMessageKeys.UPLOAD_DIRECTORY_CREATION_FAILED.getName());
    	}
    }

}
