/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.restclient.messages;

/**
 * Message codes for restClient usage.
 * @author yael
 *
 */
public enum RestClientMessageKeys {
	
	/**
	 * File to upload doesn't exists message.
	 */
	UPLOAD_FILE_DOESNT_EXIST("upload_file_doesnt_exist"),
	/**
	 * Exceeded size limit message.
	 */
	UPLOAD_FILE_SIZE_LIMIT_EXCEEDED("upload_file_size_limit_exceeded"), 
	/**
	 * Upload file is not a file message.
	 */
	UPLOAD_FILE_NOT_FILE("upload_file_not_file"), 
	/**
	 * Upload file missing message.
	 */
	UPLOAD_FILE_MISSING("upload_file_missing"), 
	/**
	 * Serialization error message.
	 */
	SERIALIZATION_ERROR("serialization_error"), 
	/**
	 * execute request failure message.
	 */
	EXECUTION_FAILURE("execute_request_failed"), 
	/**
	 * read response body failure message.
	 */
	READ_RESPONSE_BODY_FAILURE("read_response_body_failed"),
	/**
	 * HTTP failure message.
	 */
	HTTP_FAILURE("http_failure"),
	/**
	 * Invalid URL.
	 */
	INVALID_URL("invalid_url");
	
	private final String name;
	
	RestClientMessageKeys(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
