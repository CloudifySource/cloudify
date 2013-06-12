/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ******************************************************************************/
package org.cloudifysource.shell.commands;

import java.io.IOException;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.CloudifyErrorMessages;
import org.cloudifysource.shell.exceptions.CLIException;
import org.cloudifysource.shell.exceptions.CLIStatusException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Sets attributes in the cloudify controller attribute store.
 *
 * Command syntax: set-attributes '{"att1":"value2","att2":"value2"}'
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
		Map<String, String> attributes = parseAttributes();
		getRestAdminFacade().updateAttributes(scope, getCurrentApplicationName(), attributes);
		return getFormattedMessage("attributes_updated_successfully");
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> parseAttributes()
			throws CLIException {

		final JavaType javaType = TypeFactory.type(Map.class);
		final ObjectMapper mapper = new ObjectMapper();
		try {
			return (Map<String, String>) mapper.readValue(attributes, javaType);
		} catch (IOException e) {
			throw new CLIStatusException(e, CloudifyErrorMessages.CLIENT_JSON_PARSE_ERROR.getName(), attributes,
					e.getMessage());
		}

	}
}
