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
package org.cloudifysource.usm.events;

public class EventResult {

	
	
	private boolean success;
	private Exception exception;
	private Object result;
	
	public static final EventResult SUCCESS = new EventResult(true, null); 
	
	public EventResult(Exception exception) {
		this.success = false;
		this.exception = exception;
	}
	
	public EventResult(Object result) {
		this.success = true;
		this.result = result;
		this.exception = null;
		
	}
	public EventResult(boolean success, Exception exception) {
		super();
		this.success = success;
		this.exception = exception;
	}
	
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public Exception getException() {
		return exception;
	}
	public void setException(Exception exception) {
		this.exception = exception;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}
}
