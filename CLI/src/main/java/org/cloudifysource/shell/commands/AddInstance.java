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

import java.util.Collection;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.fusesource.jansi.Ansi.Color;

/**
 * @deprecated
 * @author rafi, barakm
 * @since 2.0.0
 */
@Deprecated
@Command(scope = "cloudify", name = "add-instance", description = "adds an instance to a service")
public class AddInstance extends AdminAwareCommand {

	@Argument(index = 0, required = true, name = "service", description = "The service name to add instance to."
			+ "Press tab to see the list"
			+ " of currently running services")
	private String serviceName;

	@Argument(index = 1, required = false, name = "timeout", description = "Specifies the maximal timeout period"
			+ " for adding a new instance")
	private int timeout = 10;

	/**
	 * Returns the list of services of the current application.
	 * 
	 * @return All service of the current application.
	 */
	@CompleterValues(index = 0)
	public Collection<String> getComponentList() {
		try {
			return adminFacade.getServicesList(getCurrentApplicationName());
		} catch (final CLIException e) {
			logger.warning("Could not get list of services: " + e.toString());
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Object doExecute() throws CLIException {
		adminFacade.addInstance(getCurrentApplicationName(), serviceName, timeout);
		return getFormattedMessage("added_instance_successfully", Color.GREEN);
	}
}
