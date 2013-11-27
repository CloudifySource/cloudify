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
package org.cloudifysource.dsl.rest;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.AddTemplateResponse;
import org.cloudifysource.dsl.rest.response.AddTemplatesResponse;

/**
 * Indicating one or more templates failed to be added to one or more management machines.
 * 
 * @author yael
 * 
 */
public class AddTemplatesException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final AddTemplatesResponse addTemplatesResponse;

	public AddTemplatesException(final AddTemplatesResponse addTemplatesResponse) {
		this.addTemplatesResponse = addTemplatesResponse;
	}

	public AddTemplatesResponse getAddTemplatesResponse() {
		return addTemplatesResponse;
	}
	
	
	@Override
	public String toString() {
		Map<String, AddTemplateResponse> templates = addTemplatesResponse.getTemplates();
		Set<Entry<String, AddTemplateResponse>> entrySet = templates.entrySet();
		StringBuilder string = 
				new StringBuilder("AddTemplatesException[Status: " + addTemplatesResponse.getStatus() 
						+ ", Response per template:");
		for (Entry<String, AddTemplateResponse> entry : entrySet) {
			AddTemplateResponse templateResponse = entry.getValue();
			string.append(CloudifyConstants.NEW_LINE
					+ "template " + entry.getKey() 
					+ ": hosts that failed to add the template: " + templateResponse.getFailedToAddHosts() 
					+  ", hosts that added the tempalte successfully: " + templateResponse.getSuccessfullyAddedHosts());
		}
		return string.toString();
	}

}
