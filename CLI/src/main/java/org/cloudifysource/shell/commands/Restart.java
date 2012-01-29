/*******************************************************************************
 * Copyright 2011 GigaSpaces Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.cloudifysource.shell.commands;

import static org.cloudifysource.shell.ShellUtils.componentTypeFromLowerCaseComponentName;
import static org.cloudifysource.shell.ShellUtils.delimitedStringToSet;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.cloudifysource.shell.ComponentType;
import org.cloudifysource.shell.ShellUtils;

/**
 * @deprecated
 * @author rafi, barakm
 * @since 2.0.0
 */
@Deprecated
@Command(scope = "cloudify", name = "restart", description = "restarts an application or a service")
public class Restart extends AdminAwareCommand {

	@Argument(index = 0, required = true, name = "component-type", description = "The component type to restart: press"
			+ " tab to see available options")
	private String componentTypeStr;

	@Argument(index = 1, name = "component-name", required = true, description = "The name of the service or "
			+ "application to restart. Use tab to list the currently running services or applications")
	private String componentName;

	@Argument(index = 2, required = false, name = "instance-ids", description = "If restarting a service, a "
			+ "optional comma separated list of instances IDs. "
			+ "Press tab to see a list of instance IDs. If not specified restarts all instances of the service")
	private String componentInstanceIDs;

	/**
	 * Gets the acceptable component types.
	 * @return A collection of component types
	 */
	@CompleterValues(index = 0)
	public Collection<String> getComponentTypes() {
		return ShellUtils.getComponentTypesAsLowerCaseStringCollection();
	}

	/**
	 * Gets all deployed applications/services.
	 * 
	 * @return Collection of all deployed applications/services
	 */
	@CompleterValues(index = 1)
	public Collection<String> getComponentList() {
		if (componentTypeStr != null) {
			try {
				final ComponentType componentType = componentTypeFromLowerCaseComponentName(componentTypeStr);
				switch (componentType) {
				case SERVICE:
					return adminFacade.getServicesList(getCurrentApplicationName());
				case APPLICATION:
					return adminFacade.getApplicationsList();
				default:
					logger.warning("Unhandled component type:" + componentType);
					return null;
				}
			} catch (final Exception e) {
				logger.warning("Cloud not get list of component of type: " + componentTypeStr + ": " + e.getMessage());
				return null;
			}
		}
		return null;
	}

	/**
	 * Gets all the instances of a specific service deployed on the current application.
	 * 
	 * @return Collection of all the deployed instances of this service
	 */
	@CompleterValues(index = 2)
	public Collection<String> getServiceInstanceList() {
		if (componentName != null) {
			try {
				final Map<String, Object> instanceIdToHost = adminFacade.getInstanceList(getCurrentApplicationName(),
						componentName);
				return instanceIdToHost.keySet();
			} catch (final Exception e) {
				logger.warning("Could not fetch instance list for service " + componentName + ":" + e.getMessage());
				return null;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		Set<Integer> componentInstances = null;
		if (componentInstanceIDs != null) {
			try {
				componentInstances = delimitedStringToSet(componentInstanceIDs);
			} catch (final NumberFormatException e) {
				return MessageFormat.format(messages.getString("id_array_illegal_format"), componentInstanceIDs);
			}
		}
		final ComponentType componentType = componentTypeFromLowerCaseComponentName(componentTypeStr);
		// return adminFacade.restart(componentType, componentName, componentInstances);
		return MessageFormat.format(messages.getString("command_not_implemented"), "restart");
	}

}
