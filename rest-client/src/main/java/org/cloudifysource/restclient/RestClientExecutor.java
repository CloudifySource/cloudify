/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
 ******************************************************************************/
package org.cloudifysource.restclient;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.cloudifysource.restclient.exceptions.RestClientHttpException;
import org.cloudifysource.restclient.exceptions.RestClientIOException;
import org.cloudifysource.restclient.messages.MessagesUtils;
import org.cloudifysource.restclient.messages.RestClientMessageKeys;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

/**
 * Creates all the HTTP requests needed for the RestClient and handles the HTTP responses.
 * @author yael
 *
 */
public class RestClientExecutor {
	private static final Logger logger = Logger.getLogger(RestClientExecutor.class.getName());

    private static final String FORWARD_SLASH = "/";
    private static final int DEFAULT_TRIALS_NUM = 1;
    private static final int GET_TRIALS_NUM = 3;

    private final SystemDefaultHttpClient httpClient;
    private String urlStr;


    /**
     * C'tor.
     * @param httpClient .
     * @param url .
     */
	public RestClientExecutor(
			final SystemDefaultHttpClient httpClient,
			final URL url) {
		this.httpClient = httpClient;
		this.urlStr = url.toExternalForm();
		if (!this.urlStr.endsWith(FORWARD_SLASH)) {
			this.urlStr += FORWARD_SLASH;
		}
	}
	
	/**
	 * Executes HTTP post over REST on the given (relative) URL with the given postBody.
	 *
	 * @param url
	 *            The URL to post to.
	 * @param postBody
	 *            The content of the post.
	 * @param responseTypeReference
	 *            The type reference of the response.
	 * @param <T> The type of the response.
	 * @return The response object from the REST server.
	 * @throws org.cloudifysource.restclient.exceptions.RestClientException
	 *             Reporting failure to post the file.
	 */
	public <T> T postObject(
			final String url,
			final Object postBody,
			final TypeReference<Response<T>> responseTypeReference)
					throws RestClientException {
		final HttpEntity stringEntity;
		String jsonStr;
		try {
			jsonStr = new ObjectMapper().writeValueAsString(postBody);
			stringEntity = new StringEntity(jsonStr, "UTF-8");
		} catch (final IOException e) {
			throw  MessagesUtils.createRestClientIOException(
					RestClientMessageKeys.SERIALIZATION_ERROR.getName(),
					e,
					url);
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "executing post request to " + url 
					+ ", tring to post object " + jsonStr);
		}
		return post(url, responseTypeReference, stringEntity);
	}
	
	/**
	 *
	 * @param relativeUrl
	 *          The URL to post to.
	 * @param fileToPost
	 * 			The file to post.
	 * @param partName
	 * 			The name of the request parameter (the posted file) to bind to.
	 * @param responseTypeReference
	 *          The type reference of the response.
	 * @param <T> The type of the response.
	 * @return The response object from the REST server.
	 * @throws RestClientException
	 *             Reporting failure to post the file.
	 */
	public <T> T postFile(
			final String relativeUrl,
			final File fileToPost,
			final String partName,
			final TypeReference<Response<T>> responseTypeReference)
					throws RestClientException {
		final MultipartEntity multipartEntity = new MultipartEntity();
		final FileBody fileBody = new FileBody(fileToPost);
		multipartEntity.addPart(partName, fileBody);
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "executing post request to " + relativeUrl 
					+ ", tring to post file " + fileToPost.getName());
		}
		return post(relativeUrl, responseTypeReference, multipartEntity);
	}

	/**
	 *
	 * @param relativeUrl
     *          The URL to send the get request to.
     * @param responseTypeReference
     *          The type reference of the response.
     * @param <T> The type of the response.
     * @return The response object from the REST server.
     * @throws RestClientException .
     */
    public <T> T get(
    		final String relativeUrl,
    		final TypeReference<Response<T>> responseTypeReference)
    				throws RestClientException {
        String fullUrl = getFullUrl(relativeUrl);
		final HttpGet getRequest = new HttpGet(fullUrl);
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "execute get request to " + relativeUrl);
		}
		return executeRequest(getRequest, responseTypeReference);
    }

    /**
     *
     * @param relativeUrl
     *          The URL to send the delete request to.
     * @param responseTypeReference
     *          The type reference of the response.
     * @param params
     *          Request parameters
     * @param <T> The type of the response.
     * @return The response object from the REST server.
     * @throws RestClientException .
     */
    public <T> T delete(final String relativeUrl, final Map<String, String> params,
            final TypeReference<Response<T>> responseTypeReference) throws RestClientException {

    	URIBuilder builder;

    	try {
			builder = new URIBuilder(getFullUrl(relativeUrl));
		} catch (URISyntaxException e) {
			throw MessagesUtils.createRestClientException(RestClientMessageKeys.INVALID_URL.getName(), e,
					getFullUrl(relativeUrl));
		}

    	if (params != null) {
    		for (final Map.Entry<String, String> entry : params.entrySet()) {
    			builder.addParameter(entry.getKey(), entry.getValue());
    		}
    	}

    	final HttpDelete deleteRequest = new HttpDelete(builder.toString());
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "executing delete request to " + relativeUrl);
		}
    	return executeRequest(deleteRequest, responseTypeReference);
    }

    /**
    *
    * @param relativeUrl
    *          The URL to send the delete request to.
    * @param responseTypeReference
    *          The type reference of the response.
    * @param <T> The type of the response.
    * @return The response object from the REST server.
    * @throws RestClientException .
    */
   public <T> T delete(final String relativeUrl, final TypeReference<Response<T>> responseTypeReference)
		   throws RestClientException {

   	final HttpDelete deleteRequest = new HttpDelete(getFullUrl(relativeUrl));
	if (logger.isLoggable(Level.FINE)) {
		logger.log(Level.FINE, "executing delete request to " + relativeUrl);
	}
   	return executeRequest(deleteRequest, responseTypeReference);
   }

	/**
	*
	* @param relativeUrl
	*          The URL to send the delete request to.
	* @param responseTypeReference
	*          The type reference of the response.
	* @param <T> The type of the response.
	* @return The response object from the REST server.
	* @throws RestClientException .
	*/
	private <T> T post(final String relativeUrl,
                       final TypeReference<Response<T>> responseTypeReference,
			           final HttpEntity entity)
			        		   throws RestClientException {
		final HttpPost postRequest = new HttpPost(getFullUrl(relativeUrl));
        if (entity instanceof StringEntity) {
            postRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        }
		postRequest.setEntity(entity);
		return executeRequest(postRequest, responseTypeReference);
	}

	/**
	 * Return the response's body.
	 * @param response .
	 * @return the response's body.
	 * @throws RestClientIOException 
	 * 			if failed to transform the response into string.
	 */
    public static String getResponseBody(
    		final HttpResponse response)
    				throws RestClientIOException {

    	InputStream instream = null;
    	try {
    		final HttpEntity entity = response.getEntity();
    		if (entity == null) {
    			return null;
    		}
    		instream = entity.getContent();
    		return StringUtils.getStringFromStream(instream);
    	} catch (IOException e) {
    		// this means we couldn't transform the response into string, very unlikely
    		throw MessagesUtils.createRestClientIOException(
    				RestClientMessageKeys.READ_RESPONSE_BODY_FAILURE.getName(),
    				e);
    	} finally {
    		if (instream != null) {
    			try {
					instream.close();
				} catch (IOException e) {
    				if (logger.isLoggable(Level.WARNING)) {
    					logger.warning(e.getMessage());
    				}
				}
    		}
    	}
    }

    private <T> T executeRequest(final HttpRequestBase request,
    		                     final TypeReference<Response<T>> responseTypeReference) throws RestClientException {
    	HttpResponse httpResponse = null;
    	try {
    		IOException lastException = null;
    		int numOfTrials = DEFAULT_TRIALS_NUM;
    		if (HttpGet.METHOD_NAME.equals(request.getMethod())) {
    			numOfTrials = GET_TRIALS_NUM;
    		}
    		for (int i = 0; i < numOfTrials; i++) {
    			try {
    				httpResponse = httpClient.execute(request);
    				lastException = null;
    				break;
    			} catch (IOException e) {
    				if (logger.isLoggable(Level.FINER)) {
    					logger.finer("Execute get request to " + request.getURI()
    							+ ". try number " + (i + 1) + " out of " + GET_TRIALS_NUM
    							+ ", error is " + e.getMessage());
    				}
    				lastException = e;
    			}
    		}
    		if (lastException != null) {
    			if (logger.isLoggable(Level.WARNING)) {
					logger.warning("Failed executing " + request.getMethod() + " request to " + request.getURI()
                            + " : " + lastException.getMessage());
				}
    			throw MessagesUtils.createRestClientIOException(
    					RestClientMessageKeys.EXECUTION_FAILURE.getName(),
    					lastException,
    					request.getURI());
    		}
    		String url = request.getURI().toString();
			checkForError(httpResponse, url);
    		return getResponseObject(responseTypeReference, httpResponse, url);
    	} finally {
    		request.abort();
    	}
    }

	private void checkForError(final HttpResponse response, final String requestUri)
					throws RestClientException {
		StatusLine statusLine = response.getStatusLine();
		final int statusCode = statusLine.getStatusCode();
		String reasonPhrase = statusLine.getReasonPhrase();
		String responseBody;
		if (statusCode != HttpStatus.SC_OK) {
			responseBody = getResponseBody(response);
			if (logger.isLoggable(Level.FINE)) {
				logger.log(Level.FINE, "[checkForError] - REST request to " + requestUri 
						+ "  failed. status code is: " + statusCode + ", response body is: " + responseBody);
			}
			try {
				// this means we managed to read the response
				final Response<Void> entity =
						new ObjectMapper().readValue(responseBody, new TypeReference<Response<Void>>() { });
                // we also have the response in the proper format.
                // remember, we only got here because some sort of error happened on the server.
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, "[checkForError] - REST request to " + requestUri 
							+ "  failed. throwing RestClientException: [statusCode " 
							+ statusCode + " reasonPhrase " + reasonPhrase + " defaultMessage " + entity.getMessage() 
							+ " messageCode" + entity.getMessageId() + "]");
				}
				throw MessagesUtils.createRestClientResponseException(statusCode,
																	reasonPhrase,
																	entity.getVerbose(),
																	entity.getMessage(),
																	entity.getMessageId());
            } catch (final IOException e) {
            	
                // this means we got the response, but it is not in the correct format.
                // so some kind of error happened on the spring side.
            	if (logger.isLoggable(Level.WARNING)) {
            		logger.log(Level.WARNING, "[checkForError] - failed to read response. responseBody: " 
            				+ responseBody + ", reasonPhrase:" + reasonPhrase);
            	}

            	if (statusCode == CloudifyConstants.HTTP_STATUS_NOT_FOUND) {
            		 throw MessagesUtils.createRestClientHttpException(
                     		e,
                     		statusCode,
                     		reasonPhrase,
                     		responseBody,
                     		RestClientMessageKeys.URL_NOT_FOUND.getName(), requestUri);
				} else if (statusCode == CloudifyConstants.HTTP_STATUS_ACCESS_DENIED) {
					throw MessagesUtils.createRestClientHttpException(
                     		e,
                     		statusCode,
                     		reasonPhrase,
                     		responseBody,
                     		RestClientMessageKeys.NO_PERMISSION_ACCESS_DENIED.getName());
				} else if (statusCode == CloudifyConstants.HTTP_STATUS_UNAUTHORIZED) {
					throw MessagesUtils.createRestClientHttpException(
                     		e,
                     		statusCode,
                     		reasonPhrase,
                     		responseBody,
                     		CloudifyErrorMessages.UNAUTHORIZED.getName(),
                     		reasonPhrase,
                     		requestUri);
				} else {
	                throw MessagesUtils.createRestClientHttpException(
	                		e,
	                		statusCode,
	                		reasonPhrase,
	                		responseBody,
	                		RestClientMessageKeys.HTTP_FAILURE.getName(), reasonPhrase, requestUri);
				}
            }
        }
	}

	private <T> T getResponseObject(
			final TypeReference<Response<T>> typeReference,
			final HttpResponse httpResponse, final String url)
					throws RestClientIOException, RestClientHttpException {
		final String responseBody = getResponseBody(httpResponse);
		Response<T> response;
		try {
			response = new ObjectMapper().readValue(responseBody, typeReference);
			return response.getResponse();
		} catch (IOException e) {
			if (logger.isLoggable(Level.WARNING)) {
				logger.finer("failed to read the responseBody (of request to " + url + ")."
						+ ", error was " + e.getMessage());
			}
            // this means we got the response, but it is not in the correct format.
            // so some kind of error happened on the spring side.
			StatusLine statusLine = httpResponse.getStatusLine();
			String reasonPhrase = statusLine.getReasonPhrase();
			throw MessagesUtils.createRestClientHttpException(
            		e,
            		statusLine.getStatusCode(),
            		reasonPhrase,
            		responseBody,
            		RestClientMessageKeys.HTTP_FAILURE.getName(), reasonPhrase, url);
		}
	}

	/**
	 * Appends the given relative URL to the basic rest-service URL.
	 *
	 * @param relativeUrl
	 *            URL to add to the basic URL
	 * @return full URL as as String
	 */
	private String getFullUrl(final String relativeUrl) {
		String safeRelativeURL = relativeUrl;
		if (safeRelativeURL.startsWith(FORWARD_SLASH)) {
			safeRelativeURL = safeRelativeURL.substring(1);
		}
		return urlStr + safeRelativeURL;
	}

	/**
	 * 
	 * @param username .
	 * @param password .
	 */
    public void setCredentials(final String username, final String password) {
        if (StringUtils.notEmpty(username) && StringUtils.notEmpty(password)) {
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                    new UsernamePasswordCredentials(username, password));
        }
    }
}
