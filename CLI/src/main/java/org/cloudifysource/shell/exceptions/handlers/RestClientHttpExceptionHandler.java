/*******************************************************************************
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
 *******************************************************************************/
package org.cloudifysource.shell.exceptions.handlers;

import java.util.logging.Level;

import org.cloudifysource.restclient.exceptions.RestClientHttpException;

/**
 * Exception handler for {@link RestClientHttpException}.
 * 
 * @author yael
 * @since 2.6.0
 */
public class RestClientHttpExceptionHandler extends AbstractClientSideExceptionHandler {

	private RestClientHttpException e;
	
	public RestClientHttpExceptionHandler(final RestClientHttpException e) {
		this.e = e;
	}

	@Override
	public Level getLoggingLevel() {
		return Level.WARNING;
	}

	@Override
	public String getFormattedMessage() {
		int statusCode = e.getStatusCode();
		String reasonPhrase = e.getReasonPhrase();
		String statusLine = (statusCode 
				+ " " + ((reasonPhrase == null) ? "" : reasonPhrase));
		String body = e.getResponseBody();
		return e.getMessageFormattedText() 
				+ " status line: " + statusLine
				+ (body == null ? "" : ", response body: " + body);
	}

	@Override
	public String getVerbose() {
		return e.getVerbose();
	}

}
