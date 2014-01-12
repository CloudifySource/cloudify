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
 *******************************************************************************/
package org.cloudifysource.rest.validators;

import java.util.List;

import org.openspaces.admin.Admin;

/**
 * 
 * @author yael
 * @since 2.7.0
 */
public class RemoveTemplatesValidationContext extends TemplatesValidationContext {
	private List<String> cloudDeclaredTemplates;
	private String templateName;
	private Admin admin;

	public List<String> getCloudDeclaredTemplates() {
		return cloudDeclaredTemplates;
	}

	public void setCloudDeclaredTemplates(final List<String> cloudDeclaredTemplates) {
		this.cloudDeclaredTemplates = cloudDeclaredTemplates;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(final String templateName) {
		this.templateName = templateName;
	}

	public Admin getAdmin() {
		return admin;
	}

	public void setAdmin(final Admin admin) {
		this.admin = admin;
	}

}
