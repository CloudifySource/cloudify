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
package org.cloudifysource.dsl.rest.request;

import java.util.List;

import org.cloudifysource.domain.ComputeTemplateHolder;

/**
 * 
 * @author yael
 * @since 2.7.0
 */
public class AddTemplatesInternalRequest {
	private List<ComputeTemplateHolder> cloudTemplates;
	private String uploadKey;
	private List<String> expectedTemplates;

	public List<ComputeTemplateHolder> getCloudTemplates() {
		return cloudTemplates;
	}

	public void setCloudTemplates(final List<ComputeTemplateHolder> cloudTemplates) {
		this.cloudTemplates = cloudTemplates;
	}

	public String getUploadKey() {
		return uploadKey;
	}

	public void setUploadKey(final String uploadKey) {
		this.uploadKey = uploadKey;
	}

	public List<String> getExpectedTemplates() {
		return expectedTemplates;
	}

	public void setExpectedTemplates(final List<String> expectedTemplates) {
		this.expectedTemplates = expectedTemplates;
	}

}
