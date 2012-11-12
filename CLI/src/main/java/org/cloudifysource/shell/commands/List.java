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
package org.cloudifysource.shell.commands;

import static org.cloudifysource.shell.ShellUtils.componentTypeFromLowerCaseComponentName;

import java.text.MessageFormat;
import java.util.Collection;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.cloudifysource.shell.ComponentType;

/**
 * @author uri, adaml, barakm
 * @since 2.0.0
 * 
 *        Lists all running component of a certain type (application/service).
 * 
 *        Required arguments:
 *         component-type - The component type to list (application/service)
 * 
 *        Command syntax: list component-type
 */
@Deprecated
@Command(scope = "cloudify", name = "list", description = "Lists all running component of a certain type")
public class List extends AdminAwareCommand {

	@Argument(index = 0, name = "component-type", required = true, description = "The component type to list. "
			+ "Press tab to see available options")
	private String componentType;

	/**
	 * Returns the possible values for component-type.
	 * @return A collection of component names
	 */
	@CompleterValues(index = 0)
	public Collection<String> getCompleterValues() {
		// return ShellUtils.getComponentTypesAsLowerCaseStringCollection();
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		final ComponentType componentTypeEnum = componentTypeFromLowerCaseComponentName(componentType);
		if (componentType == null) {
			throw new IllegalArgumentException(MessageFormat.format(messages.getString("unknown_service_type"),
					componentType));
		}
		switch (componentTypeEnum) {
		case SERVICE:
			return adminFacade.getServicesList(getCurrentApplicationName());
		case APPLICATION:
			return adminFacade.getApplicationNamesList();
		default:
			logger.warning("Unhandled component type");
			return null;
		}
	}

}
