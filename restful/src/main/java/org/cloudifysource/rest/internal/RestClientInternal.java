/*
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * *****************************************************************************
 */
package org.cloudifysource.rest.internal;

import java.net.URL;

import org.cloudifysource.dsl.rest.request.AddTemplatesInternalRequest;
import org.cloudifysource.dsl.rest.response.AddTemplatesInternalResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.codehaus.jackson.type.TypeReference;

/**
 * Rest client for internal calls.
 * @author yael
 *
 */
public class RestClientInternal extends RestClient {
	private static final String ADD_TEMPALTES_INTERNAL_URL_FORMAT = "internal";
	private static final String REMOVE_TEMPALTE_INTERNAL_URL_FORMAT = "internal/%s";

	public RestClientInternal(final URL url, final String username, final String password, final String apiVersion)
			throws RestClientException {
		super(url, username, password, apiVersion);
	}

	/**
	 * Executes a rest API call to add templates to this instance only.
	 * 
	 * @param request
	 *            contains the templates folder.
	 * @return AddTemplatesResponse.
	 * @throws RestClientException .
	 */
	public AddTemplatesInternalResponse addTemplatesInternal(final AddTemplatesInternalRequest request)
			throws RestClientException {
		final String addTempaltesInternalUrl = getFormattedUrl(
				versionedTemplatesControllerUrl,
				ADD_TEMPALTES_INTERNAL_URL_FORMAT);
		return executor.postObject(
				addTempaltesInternalUrl,
				request,
				new TypeReference<Response<AddTemplatesInternalResponse>>() {
				});
	}
	
	/**
	 * Executes a rest API call to remove template from this instance only.
	 * 
	 * @param templateName the template's name to remove.
	 * @throws RestClientException .
	 */
	public void removeTemplateInternal(final String templateName) 
			throws RestClientException {
		final String removeTempalteUrl = getFormattedUrl(
				versionedTemplatesControllerUrl, 
				REMOVE_TEMPALTE_INTERNAL_URL_FORMAT,
				templateName);
		executor.delete(
				removeTempalteUrl, 
				new TypeReference<Response<Void>>() { });
	}

}
