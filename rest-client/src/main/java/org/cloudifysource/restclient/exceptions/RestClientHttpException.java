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
 ******************************************************************************/
package org.cloudifysource.restclient.exceptions;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * 
 * @author yael
 * Exception originating from errors on the server side, but not from our controllers.
 * 
 */
public class RestClientHttpException extends RestClientResponseException {

	/**
	 * UID for serialization.
	 */
	private static final long serialVersionUID = 1L;

	private final String responseBody;
	private final Exception exception;

	public RestClientHttpException(final String messageCode,
                                   final String messageFormattedText,
                                   final int statusCode,
			                       final String reasonPhrase,
                                   final String responseBody, final Exception exception) {
		super(messageCode, messageFormattedText, statusCode, reasonPhrase, ExceptionUtils.getFullStackTrace(exception));
		this.responseBody = responseBody;
		this.exception = exception;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public Exception getException() {
		return exception;
	}

}
