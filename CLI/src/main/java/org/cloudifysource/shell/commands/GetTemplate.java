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
package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.domain.cloud.compute.ComputeTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.dsl.rest.response.GetTemplateResponse;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.shell.rest.RestAdminFacade;

/**
 * Gets a cloud's template.
 * 
 * Required arguments: name - The name of the template to get.
 * 
 * Command syntax: get-template name.
 * 
 * @author yael
 * 
 * @since 2.3.0
 * 
 */
@Command(scope = "cloudify", name = "get-template", description = "Displayes the cloud template details")
public class GetTemplate extends AdminAwareCommand implements NewRestClientCommand {

	@Argument(required = true, name = "name", description = "The name of the template")
	private String templateName;

	@Override
	protected Object doExecute() throws Exception {

		ComputeTemplate template = adminFacade.getTemplate(templateName);
		return templateName + ":"
				+ CloudifyConstants.NEW_LINE
				+ template.toFormatedString();
	}

	@Override
	public Object doExecuteNewRestClient() throws Exception {
		final RestClient newRestClient = ((RestAdminFacade) getRestAdminFacade()).getNewRestClient();
		GetTemplateResponse response = newRestClient.getTemplate(templateName);
		return templateName + ":"
				+ CloudifyConstants.NEW_LINE
				+ response.getTemplate().toFormatedString();
	}

}
