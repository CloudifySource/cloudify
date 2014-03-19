/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.rest.controllers;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.j_spaces.kernel.PlatformVersion;

/**
 * An interceptor for validating client request's REST API version. The client's version is stored in a designated
 * header for each request sent to the REST. For each request, the preHandle method validates that the client's version
 * is equal to the REST API version.
 * 
 * @author yael
 * @since 2.2.0
 * 
 */
public class VersionValidateInterceptor extends HandlerInterceptorAdapter {

	private static final Logger logger = Logger
			.getLogger(VersionValidateInterceptor.class.getName());

	public VersionValidateInterceptor() {
		logger.log(Level.FINE, "Initialize VersionValidateHandler");
	}

	@Override
	public boolean preHandle(final HttpServletRequest request,
			final HttpServletResponse response, final Object handler)
			throws Exception {
		final String version = request.getHeader(CloudifyConstants.REST_API_VERSION_HEADER);
		final String currentVersion = PlatformVersion.getVersion();
		final String currentVersionNumber = PlatformVersion.getVersionNumber();
		if (logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "pre handle request to " + request.getRequestURI()
					+ ". Validating the value of "
					+ CloudifyConstants.REST_API_VERSION_HEADER + " header, request URI = "
					+ request.getRequestURI() + ", request REST-API version = "
					+ version + " current REST-API version = " + currentVersion);
		}
		if (version == null) {
			logger.log(Level.FINE,
					"The " + CloudifyConstants.REST_API_VERSION_HEADER
							+ " header is missing, the request URI is "
							+ request.getRequestURI());
		} else if ((!version.equals(currentVersion)) && (!version.equals(currentVersionNumber))) {
			// For backward compatibility, we check both version (i.e. 2.7.0) and version number (2.7.0-rc)
			// you should use 'version'.
			throw new RestErrorException("version_mismatch", version, currentVersion);
		}
		return true;
	}
}
