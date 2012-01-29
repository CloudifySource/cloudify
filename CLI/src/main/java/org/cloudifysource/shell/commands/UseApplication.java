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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        Sets the currently used application.
 *        
 *        Required arguments:
 *        name - The name of the application to use
 * 
 *        Command syntax: use-application name
 */
@Command(scope = "cloudify", name = "use-application", description = "Sets the currently used application")
public class UseApplication extends AdminAwareCommand {

	@Argument(required = true, name = "name", description = "The name of the application to use")
	private String applicationName;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		session.put(Constants.ACTIVE_APP, applicationName);
		GigaShellMain.getInstance().setCurrentApplicationName(applicationName);
		return getFormattedMessage("using_application", Color.GREEN, applicationName);
	}
}
