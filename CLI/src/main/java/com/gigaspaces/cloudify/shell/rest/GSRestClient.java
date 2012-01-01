/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.gigaspaces.cloudify.shell.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.openspaces.pu.service.InvocationResult;

import com.gigaspaces.cloudify.shell.StringUtils;
import com.gigaspaces.cloudify.shell.commands.CLIException;

/**
 * @author rafi
 * @since 8.0.3
 */
public class GSRestClient {

	private static final int NOT_FOUND_404_ERROR_CODE = 404;

	private static final int HTTP_STATUS_OK = 200;//org.springframework.http.HttpStatus.OK.value();

	private static final Logger logger = Logger.getLogger(GSRestClient.class.getName());

	private static final ObjectMapper objectMapper = new ObjectMapper();
	public static final String STATUS_KEY = "status";
	public static final String ERROR = "error";
	private static final String ERROR_ARGS = "error_args";
	public static final String SUCCESS = "success";
	private static final String RESPONSE_KEY = "response";
	private static final String ADMIN_REFLECTION_URL = "/admin/";

	// TODO change when legit certificate is available
	private final DefaultHttpClient httpClient;
	private final URL url;
	private final String urlStr;

	public GSRestClient(final String username, final String password,
			final URL url) throws CLIException {

		this.url = url;
		this.urlStr = createUrlStr();

		if (isSSL()) {
			httpClient = getSSLHttpClient();
		} else {
			httpClient = new DefaultHttpClient();
		}

		// TODO use userdetails instead of user/pass
		if (StringUtils.notEmpty(username) && StringUtils.notEmpty(password)) {
			httpClient.getCredentialsProvider().setCredentials(
					new AuthScope(AuthScope.ANY),
					new UsernamePasswordCredentials(username, password));
		}

	}

	private String createUrlStr() {
		String urlStr = url.toExternalForm();
		if (!urlStr.endsWith("/")) {
			urlStr += "/";
		}
		return urlStr;
	}

	private boolean isSSL() {
		return "https".equals(url.getProtocol());
	}

	public Map<String, Object> getAdminData(final String relativeUrl)
	throws CLIException {
		final String url = getFullUrl("admin/" + relativeUrl);
		logger.finer("performing http get to url: " + url);
		final HttpGet httpMethod = new HttpGet(url);
		return readHttpAdminMethod(httpMethod);
	}

	public Object get(final String relativeUrl) throws CLIException {
		final String url = getFullUrl(relativeUrl);
		logger.finer("performing http get to url: " + url);
		final HttpGet httpMethod = new HttpGet(url);
		return executeHttpMethod(httpMethod);
	}

	public Map<String,Object> getAdmin(final String relativeUrl) throws CLIException {
		final String url = getFullUrl(ADMIN_REFLECTION_URL + relativeUrl);
		logger.finer("performing http get to url: " + url);
		final HttpGet httpMethod = new HttpGet(url);
		InputStream instream = null;
		try {
			final HttpResponse response = httpClient.execute(httpMethod);
			if (response.getStatusLine().getStatusCode() != HTTP_STATUS_OK) {
				logger.log(Level.FINE, httpMethod.getURI() + " response code " + response.getStatusLine().getStatusCode());
				throw new CLIException(response.getStatusLine().toString());
			}
			final HttpEntity entity = response.getEntity();
			if (entity == null) {
				final ErrorStatusException e = new ErrorStatusException("comm_error");
				logger.log(Level.FINE, httpMethod.getURI() + " response entity is null", e);
				throw e;
			}
			instream = entity.getContent();
			final String responseBody = StringUtils.getStringFromStream(instream);
			logger.finer(httpMethod.getURI() + " http get response: " + responseBody);
			final Map<String, Object> responseMap = GSRestClient.jsonToMap(responseBody);
			return responseMap;
		} catch (final ClientProtocolException e) {
			logger.log(Level.FINE, httpMethod.getURI() + " Rest api error", e);
			throw new ErrorStatusException("comm_error", e, e.getMessage());
		} catch (final IOException e) {
			logger.log(Level.FINE, httpMethod.getURI() + " Rest api error", e);
			throw new ErrorStatusException("comm_error", e, e.getMessage());
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

	private Object executeHttpMethod(final HttpRequestBase httpMethod)
	throws ErrorStatusException {
		String responseBody;
		try {
			final HttpResponse response = httpClient.execute(httpMethod);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HTTP_STATUS_OK) {
				logger.log(Level.FINE, httpMethod.getURI() + " response code " + statusCode);
				responseBody = getResponseBody(response);
				try{
					Map<String, Object> errorMap = GSRestClient.jsonToMap(responseBody);
					throw new ErrorStatusException("Remote_rest_gateway_exception", errorMap.get("error"));
				}catch(IOException e){
					if (statusCode == NOT_FOUND_404_ERROR_CODE) {
						throw new ErrorStatusException("URL_not_found", httpMethod.getURI());
					}
					throw new ErrorStatusException("CLI_unable_to_parse_to_JSON", responseBody);
				}
			}
			responseBody = getResponseBody(response);
			final Map<String, Object> responseMap = GSRestClient.jsonToMap(responseBody);
			final String status = (String) responseMap.get(STATUS_KEY);
			if (ERROR.equals(status)) {
				final String reason = (String) responseMap.get(ERROR);
				@SuppressWarnings("unchecked")
				final List<String> reasonsArgs = (ArrayList<String>) responseMap.get(ERROR_ARGS);
				final ErrorStatusException e =
					new ErrorStatusException(reason, reasonsArgs != null ? reasonsArgs.toArray() : null);
				logger.log(Level.FINE, reason, e);
				throw e;
			}
			return responseMap.get(RESPONSE_KEY);
		} catch (final ClientProtocolException e) {
			logger.log(Level.FINE, httpMethod.getURI() + " Rest api error", e);
			throw new ErrorStatusException("comm_error", e, e.getMessage());
		} catch (final IOException e) {
			logger.log(Level.FINE, httpMethod.getURI() + " Rest api error", e);
			throw new ErrorStatusException("comm_error", e, e.getMessage());
		} finally {
			httpMethod.abort();
		}
	}

	public static String getResponseBody(HttpResponse response) throws ErrorStatusException, IllegalStateException, IOException{

		InputStream instream = null;
		try{
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				final ErrorStatusException e = new ErrorStatusException("comm_error");
				logger.log(Level.FINE," response entity is null", e);
				throw e;
			}
			instream = entity.getContent();
			return StringUtils.getStringFromStream(instream);
		}finally {
			if (instream != null) {
				try {
					instream.close();
				} catch (final IOException e) {

				}
			}
		}
	}

	private Map<String, Object> readHttpAdminMethod(
			final HttpRequestBase httpMethod) throws CLIException {
		InputStream instream = null;
		try {
			final HttpResponse response = httpClient.execute(httpMethod);
			if (response.getStatusLine().getStatusCode() != HTTP_STATUS_OK) {
				String message = httpMethod.getURI() + " response (code " + response.getStatusLine().getStatusCode() + ") " + response.getStatusLine().toString();
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, message);
				}
				throw new CLIException(message);
			}
			final HttpEntity entity = response.getEntity();
			if (entity == null) {
				final ErrorStatusException e = new ErrorStatusException(
				"comm_error");
				logger.log(Level.FINE, httpMethod.getURI() + " response entity is null", e);
				throw e;
			}
			instream = entity.getContent();
			final String responseBody = StringUtils
			.getStringFromStream(instream);
			logger.finer(httpMethod.getURI() + " http get response: " + responseBody);
			final Map<String, Object> responseMap = GSRestClient
			.jsonToMap(responseBody);
			return responseMap;
		} catch (final ClientProtocolException e) {
			logger.log(Level.FINE, httpMethod.getURI() + " Rest api error", e);
			throw new ErrorStatusException("comm_error", e, e.getMessage());
		} catch (final IOException e) {
			logger.log(Level.FINE, httpMethod.getURI() + " Rest api error", e);
			throw new ErrorStatusException("comm_error", e, e.getMessage());
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

	private String getFullUrl(String relativeUrl) {

		if (relativeUrl.startsWith("/")) {
			relativeUrl = relativeUrl.substring(1);
		}
		return urlStr + relativeUrl;
	}

	public void delete(final String relativeUrl) throws CLIException {
		delete(relativeUrl, null);
	}

	public void delete(final String relativeUrl,
			final Map<String, String> params) throws CLIException {
		final HttpDelete httpdelete = new HttpDelete(getFullUrl(relativeUrl));
		if (params != null) {
			for (final Map.Entry<String, String> entry : params.entrySet()) {
				httpdelete.getParams().setParameter(entry.getKey(),
						entry.getValue());
			}
		}
		executeHttpMethod(httpdelete);
	}

	public Object post(final String relativeUrl) throws CLIException {
		return post(relativeUrl, null);
	}

	public Object post(final String relativeUrl,
			final Map<String, String> params) throws CLIException {
		final HttpPost httppost = new HttpPost(getFullUrl(relativeUrl));
		if (params != null) {
			HttpEntity entity;
			try {
				final String json = GSRestClient.mapToJson(params);
				entity = new StringEntity(json, "application/json", "UTF-8");
				httppost.setEntity(entity);
				httppost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			} catch (final IOException e) {
				throw new CLIException(e);
			}
		}
		return executeHttpMethod(httppost);
	}

	public String postFile(final String relativeUrl, final File file)
	throws CLIException {
		return postFile(relativeUrl, file, null);
	}

	public String postFile(final String relativeUrl, final File file,
			final Properties params) throws CLIException {

		// It should be possible to dump the properties into a String entity,
		// but I can't get it to work. So using a temp file instead.
		// Idiotic, but works.

		// dump map into file
		File tempFile;
		try {
			tempFile = writeMapToFile(params);
		} catch (final IOException e) {
			throw new CLIException(e);
		}

		final MultipartEntity reqEntity = new MultipartEntity();

		final FileBody bin = new FileBody(file);
		final FileBody propsFile = new FileBody(tempFile);

		reqEntity.addPart("file", bin);
		reqEntity.addPart("props", propsFile);

		final HttpPost httppost = new HttpPost(getFullUrl(relativeUrl));
		httppost.setEntity(reqEntity);

		return (String) executeHttpMethod(httppost);
	}

	protected File writeMapToFile(final Properties props) throws IOException {

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

	public DefaultHttpClient getSSLHttpClient() throws CLIException {
		try {
			final KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			final SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			final HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			final SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("https", sf, url.getPort()));

			final ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (KeyStoreException e) {
			throw new CLIException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new CLIException(e);
		} catch (CertificateException e) {
			throw new CLIException(e);
		} catch (IOException e) {
			throw new CLIException(e);
		} catch (KeyManagementException e) {
			throw new CLIException(e);
		} catch (UnrecoverableKeyException e) {
			throw new CLIException(e);
		}
	}

	public static Map<String, Object> jsonToMap(final String response)
	throws IOException {
		final JavaType javaType = TypeFactory.type(Map.class);
		return objectMapper.readValue(response, javaType);
	}

	private static String mapToJson(final Map<String, ?> map)
	throws IOException {
		return objectMapper.writeValueAsString(map);
	}

	public static InvocationResult mapToInvocationResult(
			final Map<String, Object> map) throws IOException {
		final String json = GSRestClient.mapToJson(map);
		final JavaType javaType = TypeFactory.type(InvocationResult.class);
		return objectMapper.readValue(json, javaType);
	}

}