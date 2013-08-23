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
package org.cloudifysource.rest.doclet;

/**
 * 
 * @author yael
 * 
 * @param <T>
 */
public class RESTExampleRespone<T> {
	private String status;
	private String message;
	private String messageId;
	private Object verbose;
	private T response;

	public String getStatus() {
		return status;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(final String messageId) {
		this.messageId = messageId;
	}

	public Object getVerbose() {
		return verbose;
	}

	public void setVerbose(final Object verbose) {
		this.verbose = verbose;
	}

	public T getResponse() {
		return response;
	}

	public void setResponse(final T response) {
		this.response = response;
	}

}
