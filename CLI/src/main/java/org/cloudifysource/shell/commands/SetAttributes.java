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
 ******************************************************************************/
package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.restclient.ErrorStatusException;
import org.cloudifysource.restclient.GSRestClient;

import java.util.Map;


/**
 *        Sets attributes in the cloudify controller attribute store.
 * 
 *        Command syntax: set-attributes '{"att1":"value2","att2":"value2"}'
 *        
 * @author uri
 * @since 2.2.0
 */
@Command(scope = "cloudify", name = "set-attributes",
		description = "Sets attributes in the cloudify controller attribute store")
public class SetAttributes extends AbstractAttributesCommand {

	@Argument(required = true, name = "attributes", description = "A list of one or more attributes to store. List " 
			+ "should use a valid JSON format, e.g.:"
			+ " '{\"att1\":\"value1\",\"att2\":\"value2\"}' "
			+ "(make sure to use single quotes (') around this argument to make sure all JSON attributes are "
			+ "escaped properly.")
	protected String attributes;

	@Override
	protected Object doExecute()
			throws Exception {
		Map attributes = parseAttributes();
		getRestAdminFacade().updateAttributes(scope, getCurrentApplicationName(), attributes);
		return getFormattedMessage("attributes_updated_successfully");
	}

	private Map<String, Object> parseAttributes()
			throws CLIException {
		try {
			return GSRestClient.jsonToMap(attributes);
		} catch (ErrorStatusException e) {
			throw new CLIStatusException(e.getReasonCode(), e.getArgs());
		}
	}

}
