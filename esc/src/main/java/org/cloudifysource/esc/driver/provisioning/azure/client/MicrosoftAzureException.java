/******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved		  *
 * 																			  *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at									  *
 *																			  *
 *       http://www.apache.org/licenses/LICENSE-2.0							  *
 *																			  *
 * Unless required by applicable law or agreed to in writing, software		  *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.											  *
 ******************************************************************************/

package org.cloudifysource.esc.driver.provisioning.azure.client;

/****************************************************************************************************
 * An exception wrapping all possible exceptions that could happen while calling azure REST API. 
 * @author elip																						
 *																									
 ****************************************************************************************************/
public class MicrosoftAzureException extends Exception {
	
	private String status;
	private String message;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public MicrosoftAzureException(final String status, final String message) {
		this.status = status;
		this.message = message;
	}
	
	
	
	/**
	 * 
	 */
	public MicrosoftAzureException() {
	}

	/**
	 * @param message .
	 */
	public MicrosoftAzureException(final String message) {
		super(message);
	}

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

	/**
	 * @param cause .
	 */
	public MicrosoftAzureException(final Throwable cause) {
		super(cause);
	}

	/**
	 * @param message .
	 * @param cause .
	 */
	public MicrosoftAzureException(final String message, final Throwable cause) {
		super(message, cause);

	}

}
