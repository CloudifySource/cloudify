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
package org.cloudifysource.dsl.rest.response;

import java.util.List;
import java.util.Map;

import org.cloudifysource.dsl.internal.CloudifyConstants;

/**
 * 
 * @author yael
 * @since 2.7.0
 */
public class AddTemplatesInternalResponse {
	private List<String> addedTempaltes;
	private Map<String, String> failedToAddTempaltesAndReasons;

	public List<String> getAddedTempaltes() {
		return addedTempaltes;
	}

	public void setAddedTempaltes(final List<String> addedTempaltes) {
		this.addedTempaltes = addedTempaltes;
	}

	public Map<String, String> getFailedToAddTempaltesAndReasons() {
		return failedToAddTempaltesAndReasons;
	}

	public void setFailedToAddTempaltesAndReasons(final Map<String, String> failedToAddTempaltesAndReasons) {
		this.failedToAddTempaltesAndReasons = failedToAddTempaltesAndReasons;
	}
	
	@Override
	public String toString() {
		String toString = "";
		if (addedTempaltes.isEmpty()) {
			toString += "no tempaltes was added.";
		} else {
			toString += "added the following templates:"
					+ addedTempaltes;
		}
		if (!failedToAddTempaltesAndReasons.isEmpty()) {
			toString += CloudifyConstants.NEW_LINE
					+ "failed to add the following tempaltes:"
					+ CloudifyConstants.NEW_LINE
					+ failedToAddTempaltesAndReasons;
		}
		return toString;
	}
}
