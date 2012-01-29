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

import java.text.MessageFormat;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

/**
 * @deprecated
 * @author rafi, barakm
 * @since 2.0.0
 */
@Deprecated
@Command(scope = "cloudify", name = "remove-instance", description = "removes a service instance")
public class RemoveInstance extends AdminAwareCommand {

	@Argument(index = 0, name = "service-name", required = true, description = "The name of the service. Use tab to"
			+ " list the currently running services")
	private String serviceName;

	@Argument(index = 1, name = "instance-id", required = true, description = "The IDs of the service instance to "
			+ "remove. Press tab to see the complete list of IDs")
	private int instanceId;

	/*
	 * @CompleterValues(index = 0) public Collection<String> getServiceList() { try { return
	 * adminFacade.getServicesList(getCurrentApplicationName()); } catch (Exception e) {
	 * logger.warning("Could not get service list: " + e.getMessage()); return null; } }
	 * 
	 * @CompleterValues(index = 1) public Collection<String> getServiceInstanceList() { if (serviceName !=
	 * null) { try { Map<String, Object> instanceIdToHost =
	 * adminFacade.getInstanceList(getCurrentApplicationName(), serviceName); return
	 * instanceIdToHost.keySet(); } catch (Exception e) {
	 * logger.warning("Could not fetch instance list for service " + serviceName + ":" + e.getMessage());
	 * return null; } } return null; }
	 */
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		adminFacade.removeInstance(getCurrentApplicationName(), serviceName, instanceId);
		return MessageFormat.format(messages.getString("instance_removed_successfully"), instanceId);
	}

}
