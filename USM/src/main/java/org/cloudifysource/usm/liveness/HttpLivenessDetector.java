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
package org.cloudifysource.usm.liveness;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.cloudifysource.dsl.Plugin;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.utils.ServiceUtils;
import org.cloudifysource.usm.UniversalServiceManagerBean;
import org.cloudifysource.usm.events.AbstractUSMEventListener;


/**
 * The HttpLivenessDetector class is responsible for verifying that the process
 * has finished loading by checking whether an HTTP GET request is successfully
 * executed. A successful request is one where the HTTP response code is 200.
 * 
 * 
 * 
 * @author barakme
 * 
 */
public class HttpLivenessDetector extends AbstractUSMEventListener implements
		LivenessDetector, Plugin {
	
	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HttpLivenessDetector.class.getName());
	
	// constants
	private static final List<Integer> DEFAULT_SUCCESS_RESPONSE_CODES = Arrays
			.asList(200);
	
	private static final String URL_KEY = "url";
	private static final String RESPONSE_CODES_KEY = "responseCodes";

	// Injected values
	private Set<Integer> allowedResponseCodes = new HashSet<Integer>(
			DEFAULT_SUCCESS_RESPONSE_CODES);

	private String url = null;

	@Override
	public void setConfig(Map<String, Object> config) {
		this.url = config.get(URL_KEY).toString();

		if (this.url == null) {
			throw new IllegalArgumentException("Argument url in "
					+ this.getClass().getName() + " is mandatory");
		}
		if (config.containsKey(RESPONSE_CODES_KEY)) {
			Object codesValue = config.get(RESPONSE_CODES_KEY);
			if (List.class.isAssignableFrom(codesValue.getClass())) {
				@SuppressWarnings("unchecked")
				List<Integer> codes = (List<Integer>) codesValue;
				this.allowedResponseCodes = new HashSet<Integer>(codes);
			}
		}

	}

	/**
	 * Sends an HTTP GET request to the given URL and compares it to the allowedResponseCodes.
	 * 
	 * @return true if the HTTP response code is one of the allowed response codes.
	 * 
	 */
	@Override
	public boolean isProcessAlive() throws TimeoutException {
		final int responseCode = ServiceUtils.getHttpReturnCode(this.url);
		boolean isProcessAlive = this.allowedResponseCodes.contains(responseCode);
		if (logger.isLoggable(Level.FINE)) {
			logger.fine(this.url + " response code " + responseCode+". isProcessAlive="+isProcessAlive);
		}
		return isProcessAlive;
	}

	@Override
	public void init(UniversalServiceManagerBean usm) {

	}

	@Override
	public int getOrder() {
		return 5;
	}

	@Override
	public void setServiceContext(ServiceContext context) {
		// ignore
	}

}
