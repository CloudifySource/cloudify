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

/**
 * 
 * @author yael
 * Exception originating from errors that were thrown by our controllers.
 * Meant to encapsulate all error data returned as strings from the rest.
 * 
 */
public class RestClientResponseException extends RestClientException {

	/**
	 * UID for serialization.
	 */
	private static final long serialVersionUID = 1L;

	private final int statusCode;
	private final String reasonPhrase;

	public RestClientResponseException(final String messageCode,
                                       final String messageFormattedText,
			                           final int statusCode,
                                       final String reasonPhrase,
                                       final String verbose) {
		super(messageCode, messageFormattedText, verbose);
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getReasonPhrase() {
		return reasonPhrase;
	}

}
