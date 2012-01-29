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

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.CompleterValues;
import org.apache.felix.gogo.commands.Option;
import org.cloudifysource.shell.ShellUtils;

/**
 * @deprecated
 * @author rafi, barakm
 * @since 2.0.0
 */
@Deprecated
@Command(scope = "cloudify", name = "start", description = "starts an application or service")
public class Start extends AdminAwareCommand {

	@Option(required = false, name = "component", description = "component type, press tab to see available options")
	private String applicationName;

	@Option(required = true, name = "name", description = "component name")
	private String name;

	/**
	 * Gets the acceptable component types.
	 * @return A collection of component types
	 */
	@CompleterValues
	public Collection<String> getCompleterValues() {
		return ShellUtils.getComponentTypesAsLowerCaseStringCollection();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		// switch (type){
		// case APPLICATION:
		// throw new UnsupportedOperationException("APPLICATION is not supported yet");
		// case SERVICE:
		// File service = ((Map<String, File>)session.get(Constants.RECIPES)).get(name);
		// if (service != null && service.exists()){
		// return adminFacade.startService(service);
		// }else{
		// return MessageFormat.format(messages.getString("service_not_found"), name);
		// }
		// default:
		// throw new IllegalArgumentException();
		// }
		return null;
	}

}
