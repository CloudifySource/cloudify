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
 ******************************************************************************/
package org.cloudifysource.restclient;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * @author rafi
 * @since 2.0.0
 */
public class GSRestClient {

    /**
     * constants.
     */
    public static final String REASON_CODE_COMM_ERR = "comm_error";

    private static final String STATUS_KEY = "status";
    private static final String ERROR = "error";

    // org.springframework.http.HttpStatus.OK.value();

    private static final Logger logger = Logger.getLogger(GSRestClient.class.getName());

    private static final String ERROR_ARGS = "error_args";
    private static final String VERBOSE = "verbose";
    private static final ObjectMapper PROJECT_MAPPER = new ObjectMapper();
    private static final String RESPONSE_KEY = "response";
    private static final String ADMIN_REFLECTION_URL = "/admin/";
    private static final String FORWARD_SLASH = "/";
    private static final String HTTPS = "https";
    private static final String MSG_PERFORMING_HTTP_GET = "performing http get to url: ";
    private static final String MSG_RESPONSE_CODE = " response code ";
    private static final String MSG_RESPONSE_ENTITY_NULL = " response entity is null";
    private static final String MSG_HTTP_GET_RESPONSE = " http get response: ";
    private static final String MSG_REST_API_ERR = " Rest api error";
    private static final String MIME_TYPE_APP_JSON = "application/json";

    // TODO change when legit certificate is available
    private final DefaultHttpClient httpClient;
    private final URL url;
    private final String urlStr;

    /**
     * Ctor.
     *
     * @param username Username for the HTTP client, optional.
     * @param password Password for the HTTP client, optional.
     * @param url      URL to the rest service.
     * @param version  cloudify api version of the client 
     * @throws RestException Reporting failure to create a SSL HTTP client.
     */
    public GSRestClient(final String username, final String password, final URL url, final String version) throws RestException {

        this.url = url;
        this.urlStr = createUrlStr();

        if (isSSL()) {
            httpClient = getSSLHttpClient();
        } else {
            httpClient = new DefaultHttpClient();
        }
        httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
			
			@Override
			public void process(HttpRequest request, HttpContext context)
					throws HttpException, IOException {
				request.addHeader(CloudifyConstants.REST_API_VERSION_HEADER, version);
			}
		});

        setCredentials(username, password);
    }
    
    /**
     * Sets username and password for the HTTP client
     * 
     * @param username Username for the HTTP client.
     * @param password Password for the HTTP client.
     */
    public void setCredentials(final String username, final String password) {
    	// TODO use userdetails instead of user/pass
        if (StringUtils.notEmpty(username) && StringUtils.notEmpty(password)) {
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                    new UsernamePasswordCredentials(username, password));
        }
    }

    /**
     * Creates the basic rest service URL. Relative URLs will be appended to this URL.
     *
     * @return the basic rest service URL
     */
    private String createUrlStr() {
        String urlStr = url.toExternalForm();
        if (!urlStr.endsWith(FORWARD_SLASH)) {
            urlStr += FORWARD_SLASH;
        }
        return urlStr;
    }

    /**
     * Checks if the Rest URL uses SSL (https instead of http).
     *
     * @return boolean indicating if ssl is used
     */
    private boolean isSSL() {
        return HTTPS.equals(url.getProtocol());
    }

    /**
     * Performs a REST GET operation on the given (relative) URL, using the Admin API.
     *
     * @param relativeUrl the Relative URL to the requested object. The rest server IP and port are not required.
     *                    <p/>
     *                    example: "processingUnits/Names/default.cassandra" will get the object named
     *                    "dafault.cassandra" from the list of all processing units.
     * @return An object, the response received from the Admin API.
     * @throws RestException Reporting errors of all types (IO, HTTP, rest etc.)
     */
    public final Map<String, Object> getAdminData(final String relativeUrl) throws RestException {
        final String url = getFullUrl("admin/" + relativeUrl);
        logger.finer(MSG_PERFORMING_HTTP_GET + url);
        final HttpGet httpMethod = new HttpGet(url);
        return readHttpAdminMethod(httpMethod);
    }

    /**
     * Performs a REST GET operation on the given (relative) URL.
     *
     * @param relativeUrl the Relative URL to the requested object. The rest server IP and port are not required.
     *                    <p/>
     *                    example: "/service/applications/travel/services/cassandra/ USMEventsLogs/" will get event
     *                    logs from the cassandra service of the travel application.
     * @return An object, the response received from the rest service
     * @throws ErrorStatusException Reporting errors of all types (IO, HTTP, rest etc.)
     */
    public final Object get(final String relativeUrl) throws ErrorStatusException {
        return get(relativeUrl, "response");
    }

    public final Object get(final String relativeUrl, String reponseJsonKey) throws ErrorStatusException {
        final String url = getFullUrl(relativeUrl);
        logger.finer(MSG_PERFORMING_HTTP_GET + url);
        final HttpGet httpMethod = new HttpGet(url);
        return executeHttpMethod(httpMethod, reponseJsonKey);
    }


    /**
     * Performs a REST GET operation on the given (relative) URL, using the Admin API.
     *
     * @param relativeUrl the Relative URL to the requested object. The rest server IP and port are not required.
     *                    <p/>
     *                    example: "applications/Names/travel/ProcessingUnits/Names/travel.tomcat
     *                    /Instances/0/GridServiceContainer/Uid" will get the UID of the first instance of the tomcat
     *                    service in the "travel" application.
     * @return An object, the response received from the Admin API.
     * @throws RestException Reporting errors of all types (IO, HTTP, rest etc.)
     */
    public final Map<String, Object> getAdmin(final String relativeUrl) throws RestException {
        final String url = getFullUrl(ADMIN_REFLECTION_URL + relativeUrl);
        logger.finer(MSG_PERFORMING_HTTP_GET + url);
        final HttpGet httpMethod = new HttpGet(url);
        InputStream instream = null;
        try {
            final HttpResponse response = httpClient.execute(httpMethod);
            if (response.getStatusLine().getStatusCode() != CloudifyConstants.HTTP_STATUS_CODE_OK) {
                logger.log(Level.FINE, httpMethod.getURI() + MSG_RESPONSE_CODE
                        + response.getStatusLine().getStatusCode());
                throw new RestException(response.getStatusLine().toString());
            }
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                final ErrorStatusException e = new ErrorStatusException(REASON_CODE_COMM_ERR, httpMethod.getURI(),
                        MSG_RESPONSE_ENTITY_NULL);
                logger.log(Level.FINE, httpMethod.getURI() + MSG_RESPONSE_ENTITY_NULL, e);
                throw e;
            }
            instream = entity.getContent();
            final String responseBody = StringUtils.getStringFromStream(instream);
            logger.finer(httpMethod.getURI() + MSG_HTTP_GET_RESPONSE + responseBody);
            final Map<String, Object> responseMap = GSRestClient.jsonToMap(responseBody);
            return responseMap;
        } catch (final ClientProtocolException e) {
            logger.log(Level.FINE, httpMethod.getURI() + MSG_REST_API_ERR, e);
            throw new ErrorStatusException(e, REASON_CODE_COMM_ERR, httpMethod.getURI(), MSG_REST_API_ERR);
        } catch (final IOException e) {
            logger.log(Level.FINE, httpMethod.getURI() + MSG_REST_API_ERR, e);
            throw new ErrorStatusException(e, REASON_CODE_COMM_ERR, httpMethod.getURI(), MSG_REST_API_ERR);
        } finally {
            if (instream != null) {
                try {
                    instream.close();
                } catch (final IOException e) {
                }
            }
            httpMethod.abort();
        }
    }

    /**
     * This method executes the given Http request and analyzes the response. Successful responses are
     * expected to be formatted as json strings, and are converted to a Map<String, Object> object. In this
     * map these keys can be expected: "status" (success/error), "error"(reason code), "error_args" and
     * "response".
     * <p/>
     * Errors of all types (IO, Http, rest etc.) are reported through an ErrorStatusException.
     *
     * @param httpMethod The http request to perform.
     * @return An object, the response body received from the rest service
     * @throws ErrorStatusException Reporting errors of all types (IO, HTTP, rest etc.)
     */
    private Object executeHttpMethod(final HttpRequestBase httpMethod) throws ErrorStatusException {
    	return executeHttpMethod(httpMethod, "response");
    }

    /**
     * This method executes the given Http request and analyzes the response. Successful responses are
     * expected to be formatted as json strings, and are converted to a Map<String, Object> object. In this
     * map these keys can be expected: "status" (success/error), "error"(reason code), "error_args" and
     * "response".
     * <p/>
     * Errors of all types (IO, Http, rest etc.) are reported through an ErrorStatusException.
     *
     * @param httpMethod      The http request to perform.
     * @param responseJsonKey specify a key for a response attribute to be returned, null means return the
     *                        entire response
     * @return An object, the response body received from the rest service
     * @throws ErrorStatusException Reporting errors of all types (IO, HTTP, rest etc.)
     */

    private Object executeHttpMethod(final HttpRequestBase httpMethod, String responseJsonKey) throws ErrorStatusException {
        String responseBody;
        try {
            final HttpResponse response = httpClient.execute(httpMethod);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != CloudifyConstants.HTTP_STATUS_CODE_OK) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, httpMethod.getURI() + MSG_RESPONSE_CODE + statusCode);
                }
                responseBody = getResponseBody(response, httpMethod);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, httpMethod.getURI() + " response body " + responseBody);
                }
                try {
                	final Map<String, Object> errorMap = GSRestClient.jsonToMap(responseBody);
                    final String status = (String) errorMap.get(STATUS_KEY);
                    if (ERROR.equals(status)) {
                        final String reason = (String) errorMap.get(ERROR);
                        @SuppressWarnings("unchecked")
                        final List<Object> reasonsArgs = (List<Object>) errorMap.get(ERROR_ARGS);
                        final ErrorStatusException e = new ErrorStatusException(reason,
                                reasonsArgs != null ? reasonsArgs.toArray() : null);
                        if(errorMap.containsKey(VERBOSE)) {
                        	e.setVerboseData((String) errorMap.get(VERBOSE));
                        }
                        logger.log(Level.FINE, reason, e);
                        throw e;
                    }
                } catch (final IOException e) {
                    if (statusCode == CloudifyConstants.HTTP_STATUS_NOT_FOUND) {
                        throw new ErrorStatusException(e, "URL_not_found", httpMethod.getURI());
                    } else if (statusCode == CloudifyConstants.HTTP_STATUS_ACCESS_DENIED) {
                        throw new ErrorStatusException(e, CloudifyErrorMessages.NO_PERMISSION_ACCESS_DENIED.getName(),
                        		httpMethod.getURI());
                    } else if (statusCode == CloudifyConstants.HTTP_STATUS_BAD_CREDENTIALS) {
                        throw new ErrorStatusException(e, CloudifyErrorMessages.BAD_CREDENTIALS.getName(),
                        		httpMethod.getURI());
                    }
                    
                    throw new ErrorStatusException(e, "CLI_unable_to_parse_to_JSON", responseBody);
                }
            }
            responseBody = getResponseBody(response, httpMethod);
            final Map<String, Object> responseMap = GSRestClient.jsonToMap(responseBody);
            return responseJsonKey != null ? responseMap.get(RESPONSE_KEY) : responseMap;
        } catch (final ClientProtocolException e) {
            logger.log(Level.FINE, httpMethod.getURI() + MSG_REST_API_ERR, e);
            throw new ErrorStatusException(e, REASON_CODE_COMM_ERR, httpMethod.getURI(), MSG_REST_API_ERR);
        } catch (final IOException e) {
            logger.log(Level.FINE, httpMethod.getURI() + MSG_REST_API_ERR, e);
            throw new ErrorStatusException(e, REASON_CODE_COMM_ERR, httpMethod.getURI(), MSG_REST_API_ERR);
        } finally {
            httpMethod.abort();
        }
    }

    /**
     * Gets the HTTP response's body as a String.
     *
     * @param response   The HttpResponse object to analyze
     * @param httpMethod The HTTP request that originated this response
     * @return the body of the given HttpResponse object, as a string
     * @throws ErrorStatusException Reporting a communication failure
     * @throws IOException          Reporting a failure to read the response's content
     */
    public static String getResponseBody(final HttpResponse response, final HttpRequestBase httpMethod)
            throws ErrorStatusException, IOException {

        InputStream instream = null;
        try {
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                final ErrorStatusException e = new ErrorStatusException(REASON_CODE_COMM_ERR, httpMethod.getURI(),
                        MSG_RESPONSE_ENTITY_NULL);
                logger.log(Level.FINE, MSG_RESPONSE_ENTITY_NULL, e);
                throw e;
            }
            instream = entity.getContent();
            return StringUtils.getStringFromStream(instream);
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
     * Executes the given HTTP request and analyzes the response. Successful responses are expected to be
     * formatted as json strings, and are converted to a Map<String, Object> object. The map can use these
     * keys: "status" (success/error), "error"(reason code), "error_args" and "response".
     * <p/>
     * Errors of all types (IO, HTTP, rest etc.) are reported through an ErrorStatusException (RestException).
     *
     * @param httpMethod The HTTP request to perform.
     * @return An object, the response body received from the rest service
     * @throws RestException Reporting errors of all types (IO, HTTP, rest etc.)
     */
    private Map<String, Object> readHttpAdminMethod(final HttpRequestBase httpMethod) throws RestException {
        InputStream instream = null;
        URI uri = httpMethod.getURI();
		try {
            final HttpResponse response = httpClient.execute(httpMethod);
            StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode != CloudifyConstants.HTTP_STATUS_CODE_OK) {
                final String message = uri + " response (code "
                        + statusCode + ") " + statusLine.toString();
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, message);
                }
                throw new RestException(message);
            }
            final HttpEntity entity = response.getEntity();
            if (entity == null) {
                final ErrorStatusException e = new ErrorStatusException(REASON_CODE_COMM_ERR, uri,
                        MSG_RESPONSE_ENTITY_NULL);
                logger.log(Level.FINE, uri + MSG_RESPONSE_ENTITY_NULL, e);
                throw e;
            }
            instream = entity.getContent();
            final String responseBody = StringUtils.getStringFromStream(instream);
            logger.finer(uri + MSG_HTTP_GET_RESPONSE + responseBody);
            final Map<String, Object> responseMap = GSRestClient.jsonToMap(responseBody);
            return responseMap;
        } catch (final ClientProtocolException e) {
            logger.log(Level.FINE, uri + MSG_REST_API_ERR, e);
            throw new ErrorStatusException(e, REASON_CODE_COMM_ERR, uri, MSG_REST_API_ERR);
        } catch (final IOException e) {
            logger.log(Level.FINE, uri + MSG_REST_API_ERR, e);
            throw new ErrorStatusException(e, REASON_CODE_COMM_ERR, uri, MSG_REST_API_ERR);
        } finally {
            if (instream != null) {
                try {
                    instream.close();
                } catch (final IOException e) {
                }
            }
            httpMethod.abort();
        }
    }

    /**
     * Appends the given relative URL to the basic rest-service URL.
     *
     * @param relativeUrl URL to add to the basic URL
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
     * Performs a REST DELETE operation on the given (relative) URL.
     *
     * @param relativeUrl the Relative URL to the object to be deleted. The rest server IP and port are not required.
     *                    <p/>
     *                    example: service/applications/default/services/cassandra/undeploy will delete (undeploy) the
     *                    service named "cassandra" from the list of all services on the default application.
     * @return Return object.
     * @throws ErrorStatusException Reporting errors of all types (IO, HTTP, rest etc.)
     */
    public final Object delete(final String relativeUrl) throws ErrorStatusException {
        return delete(relativeUrl, null);
    }

    /**
     * Performs a REST DELETE operation on the given (relative) URL.
     *
     * @param relativeUrl the Relative URL to the object to be deleted. The rest server IP and port are not required.
     *                    <p/>
     *                    example: service/applications/default/services/cassandra/undeploy will delete (undeploy) the
     *                    service named "cassandra" from the list of all services on the default application.
     * @param params      parameters set on the HttpDelete object
     * @return Return object.
     * @throws ErrorStatusException Reporting errors of all types (IO, HTTP, rest etc.)
     */
    public final Object delete(final String relativeUrl, final Map<String, String> params) throws ErrorStatusException {
        final HttpDelete httpdelete = new HttpDelete(getFullUrl(relativeUrl));
        if (params != null) {
            for (final Map.Entry<String, String> entry : params.entrySet()) {
                httpdelete.getParams().setParameter(entry.getKey(), entry.getValue());
            }
        }
        return executeHttpMethod(httpdelete);
    }

    /**
     * This methods executes HTTP post over REST on the given (relative) URL with the given parameters map.
     *
     * @param relativeUrl The URL to post to.
     * @return The response object from the REST server
     * @throws RestException Reporting failure to post the file.
     */
    public final Object post(final String relativeUrl) throws RestException {
        return post(relativeUrl, null);
    }

    /**
     * This methods executes HTTP post over REST on the given (relative) URL with the given parameters map.
     *
     * @param relativeUrl The URL to post to.
     * @param params parameters as a map of names and values.
     * @return The response object from the REST server
     * @throws RestException Reporting failure to post the file.
     */
    public final Object post(final String relativeUrl, final Map<String, String> params) throws RestException {
        final HttpPost httppost = new HttpPost(getFullUrl(relativeUrl));
        if (params != null) {
            HttpEntity entity;
            try {
                final String json = GSRestClient.mapToJson(params);
                entity = new StringEntity(json, MIME_TYPE_APP_JSON, "UTF-8");
                httppost.setEntity(entity);
                httppost.setHeader(HttpHeaders.CONTENT_TYPE, MIME_TYPE_APP_JSON);
            } catch (final IOException e) {
                throw new RestException(e);
            }
        }
        return executeHttpMethod(httppost);
    }

    /**
     * This methods executes HTTP post over REST on the given (relative) URL with the given file.
     *
     * @param relativeUrl The URL to post to.
     * @param file        The file to send (example: <SOME PATH>/tomcat.zip).
     * @return The response object from the REST server
     * @throws RestException Reporting failure to post the file.
     */
    public final Object postFile(final String relativeUrl, final File file) throws RestException {
        return postFile(relativeUrl, file, null/*props*/, null/*cloudOverrides*/, new HashMap<String, String>());
    }
    
    /**
     * This methods executes HTTP post over REST on the given (relative) URL with the given file.
     *
     * @param relativeUrl The URL to post to.
     * @param file        The file to send (example: <SOME PATH>/tomcat.zip).
     * @return The response object from the REST server
     * @throws RestException Reporting failure to post the file.
     */
    public final Object postFile(final String relativeUrl, final File file, final Properties props, 
    		final File cloudOverrides) throws RestException {
        return postFile(relativeUrl, file, props, cloudOverrides, new HashMap<String, String>());
    }
    
    public final Object postFiles(final String relativeUrl, Map<String, File> files) 
    		throws ErrorStatusException {
        final MultipartEntity reqEntity = new MultipartEntity();

        for (Entry<String, File> entry : files.entrySet()) {
        	final FileBody bin = new FileBody(entry.getValue());
        	reqEntity.addPart(entry.getKey(), bin);			
		}
        
        final HttpPost httppost = new HttpPost(getFullUrl(relativeUrl));
        httppost.setEntity(reqEntity);
        
        return executeHttpMethod(httppost);
    }

/**
     * This methods executes HTTP post over REST on the given (relative) URL with the given file and
     * properties (also sent as a separate file).
     *
     * @param relativeUrl The URL to post to.
     * @param file        The file to send (example: <SOME PATH>/tomcat.zip).
     * @param props      The properties of this POST action (example: com.gs.service.type=WEB_SERVER)
     * @param cloudOverrides A file containing override properties to be used by the cloud driver upon installation.
     * @param parameters as a map of names and values.
     * @return The response object from the REST server
     * @throws RestException Reporting failure to post the file.
     */
    public final Object postFile(final String relativeUrl, final File file, final Properties props,
    		final File cloudOverrides, final Map<String, String> params)
            throws RestException {

        // It should be possible to dump the properties into a String entity,
        // but I can't get it to work. So using a temp file instead.
        // Idiotic, but works.

        // dump map into file
        File tempFile;
        try {
            tempFile = writeMapToFile(props);
        } catch (final IOException e) {
            throw new RestException(e);
        }

        final MultipartEntity reqEntity = new MultipartEntity();
        
        FileBody cloudOverridesBody = null;
        if (cloudOverrides != null) {
        	cloudOverridesBody = new FileBody(cloudOverrides);
        	reqEntity.addPart("cloudOverridesFile", cloudOverridesBody);        	
        }
        final FileBody bin = new FileBody(file);
        final FileBody propsFile = new FileBody(tempFile);

        reqEntity.addPart("file", bin);
        reqEntity.addPart("props", propsFile);

        final HttpPost httppost = new HttpPost(getFullUrl(relativeUrl));
        httppost.setEntity(reqEntity);
        
        return executeHttpMethod(httppost);
    }
    
    /**
     * This methods executes HTTP post over REST on the given (relative) URL with the given file and
     * properties (also sent as a separate file).
     *
     * @param relativeUrl The URL to post to.
     * @param file        The file to send (example: <SOME PATH>/tomcat.zip).
     * @param props       An additional string parameter passed on this post.
     * @param params      parameters as a map of names and values.
     * @return The response object from the REST server
     * @throws RestException Reporting failure to post the file.
     */
    public final Object postFile(final String relativeUrl, final File file, final Properties props,
    		final Map<String, String> params) throws RestException {
    	return postFile(relativeUrl, file, props, null/*cloud overrides*/, params);
    }
    
    /**
     * This methods executes HTTP post over REST on the given (relative) URL with the given file and
     * properties (also sent as a separate file).
     *
     * @param relativeUrl The URL to post to.
     * @param file        The file to send (example: <SOME PATH>/tomcat.zip).
     * @param cloudOverrides A file containing override properties to be used by the cloud driver upon installation.
     * @param param parameters as a map of names and values.
     * @return The response object from the REST server
     * @throws RestException Reporting failure to post the file or the parameter.
     */
    public final Object postFile(final String relativeUrl, final File file, final File cloudOverrides, final Map<String, String> params)
            throws RestException {

        final MultipartEntity entity = new MultipartEntity();
        
        FileBody cloudOverridesBody = null;
        if (cloudOverrides != null) {
        	cloudOverridesBody = new FileBody(cloudOverrides);
        	entity.addPart("cloudOverridesFile", cloudOverridesBody);        	
        }

        entity.addPart("file", new FileBody(file));
        if (params != null) {
        	try {
        		for (Map.Entry<String, String> param : params.entrySet()) {
        			entity.addPart(param.getKey(), new StringBody(param.getValue(), Charset.forName("UTF-8")));        		
        		}
        	} catch (final IOException e) {
        		throw new RestException(e);
        	}
        }
        final HttpPost httppost = new HttpPost(getFullUrl(relativeUrl));
        httppost.setEntity(entity);
        
        return executeHttpMethod(httppost);
    }

    /**
     * Writes the given properties to a temporary text files and returns it. The text file will be deleted
     * automatically.
     *
     * @param props Properties to write to a temporary text (.tmp) file
     * @return File object - the properties file created.
     * @throws IOException Reporting failure to create the text file.
     */
    protected final File writeMapToFile(final Properties props) throws IOException {

        // then dump properties into file
        File tempFile = null;
        FileWriter writer = null;
        try {
            tempFile = File.createTempFile("uploadTemp", ".tmp");
            writer = new FileWriter(tempFile, true);
            if (props != null) {
                props.store(writer, "");
            }
            tempFile.deleteOnExit();

        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }

        return tempFile;
    }

    /**
     * Returns a HTTP client configured to use SSL.
     *
     * @return HTTP client configured to use SSL
     * @throws RestException Reporting different failures while creating the HTTP client
     */
    public final DefaultHttpClient getSSLHttpClient() throws RestException {
        try {
            final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            final SSLSocketFactory sf = new RestSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            final HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            final SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme(HTTPS, sf, url.getPort()));

            final ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (final KeyStoreException e) {
            throw new RestException(e);
        } catch (final NoSuchAlgorithmException e) {
            throw new RestException(e);
        } catch (final CertificateException e) {
            throw new RestException(e);
        } catch (final IOException e) {
            throw new RestException(e);
        } catch (final KeyManagementException e) {
            throw new RestException(e);
        } catch (final UnrecoverableKeyException e) {
            throw new RestException(e);
        }
    }

    /**
     * Converts a json String to a Map<String, Object>.
     *
     * @param response a json-format String to convert to a map
     * @return a Map<String, Object> based on the given String
     * @throws IOException Reporting failure to read or map the String
     */
    public static Map<String, Object> jsonToMap(final String response) throws IOException {
        final JavaType javaType = TypeFactory.type(Map.class);
        return PROJECT_MAPPER.readValue(response, javaType);
    }

    /**
     * Converts a Map<String, ?> to a json String.
     *
     * @param map a map to convert to String
     * @return a json-format String based on the given map
     * @throws IOException Reporting failure to read the map or convert it
     */
    public static String mapToJson(final Map<String, ?> map) throws IOException {
        return PROJECT_MAPPER.writeValueAsString(map);
    }

    /**
     * Convert a Map<String, Object> to an InvocationResult object.
     *
     * @param map a map to convert to an InvocationResult object
     * @return an InvocationResult object based on the given map
     * @throws IOException Reporting failure to read the map or convert it
     */
    public static InvocationResult mapToInvocationResult(final Map<String, Object> map) throws IOException {
        final String json = GSRestClient.mapToJson(map);
        final JavaType javaType = TypeFactory.type(InvocationResult.class);
        return PROJECT_MAPPER.readValue(json, javaType);
    }

}