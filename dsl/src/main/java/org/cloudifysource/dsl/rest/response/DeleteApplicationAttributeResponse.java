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
package org.cloudifysource.dsl.rest.response;


/**
 * 
 * A POJO represent delete application attributes response used for making a REST API response .
 * contain the response attribute value ( previous value ) that will be sending into response 
 *  
 *  
 * 
 * @author ahmad
 * 
 */
public class DeleteApplicationAttributeResponse {

	private Object previousValue;


	public Object getPreviousValue() {
		return previousValue;
	}

	public void setPreviousValue(Object previousValue) {
		this.previousValue = previousValue;
	}

}
