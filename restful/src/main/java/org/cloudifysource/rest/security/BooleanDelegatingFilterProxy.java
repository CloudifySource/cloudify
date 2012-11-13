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
package org.cloudifysource.rest.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.springframework.web.filter.DelegatingFilterProxy;

public class BooleanDelegatingFilterProxy extends DelegatingFilterProxy{
	
	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
				throws ServletException, IOException {
		final String springSecured = System.getenv(CloudifyConstants.SPRING_BEANS_PROFILE_ENV_VAR);

	    if (StringUtils.isNotBlank(springSecured) && springSecured.equalsIgnoreCase("true")) {
	    	// Call the delegate
		      super.doFilter(request, response, filterChain);
	    } else {
	    	// Ignore the DelegatingProxyFilter delegate
	    	filterChain.doFilter(request, response);
	    }
	  }
}
