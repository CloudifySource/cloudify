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
 *******************************************************************************/
package org.cloudifysource.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author noak
 * @since 2.3.0
 * 
 *        Logs-in to the REST server with the given credentials
 * 
 *        Command syntax: login username password
 */
@Command(scope = "cloudify", name = "login", description = "reconnects to the admin REST server")
public class Login extends AdminAwareCommand {

	@Argument(required = true, name = "user", description = "The username for a secure connection to the rest "
			+ "server", index = 0)
	private String username;

	@Argument(required = true, name = "password", description = "The password for a secure connection to the rest "
			+ " server", index = 1)
	private String password;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		adminFacade.reconnect(username, password);
		session.put(Constants.ACTIVE_APP, CloudifyConstants.DEFAULT_APPLICATION_NAME);
		GigaShellMain.getInstance().setCurrentApplicationName(CloudifyConstants.DEFAULT_APPLICATION_NAME);
		return getFormattedMessage("logged_in_successfully", Color.GREEN);
	}
}
