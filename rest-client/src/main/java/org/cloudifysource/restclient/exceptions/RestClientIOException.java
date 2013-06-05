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

import java.io.IOException;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * 
 * @author yael
 * Severe exception when reading/writing response/request from/to server.
 * 
 */
public class RestClientIOException extends RestClientException {

	/**
	 * UID for serialization.
	 */
	private static final long serialVersionUID = 1L;

	private final IOException exception;

	public RestClientIOException(final String messageCode,
                                 final String messageFormattedText,
			                     final IOException exception) {
		super(messageCode, messageFormattedText, ExceptionUtils.getStackTrace(exception));
		this.exception = exception;
	}

	public IOException getException() {
		return exception;
	}

}
