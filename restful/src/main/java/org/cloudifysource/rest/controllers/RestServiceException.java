/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
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

package org.cloudifysource.rest.controllers;

/*****************
 * An exception class thrown by rest service calls, containing an error message names and message parameters.
 * @author barakme
 * @since 2.1.1
 *
 */
public class RestServiceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String messageName;
	
	public String getMessageName() {
		return messageName;
	}

	
	public String[] getParams() {
		return params;
	}

	private String[] params;
	
	public RestServiceException(final String messageName, final String... params) {
		this.messageName = messageName;
		this.params = params; 
	}
	
	
}
