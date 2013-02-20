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

import org.apache.felix.gogo.commands.Command;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.cloudifysource.shell.Constants;
import org.cloudifysource.shell.GigaShellMain;
import org.fusesource.jansi.Ansi.Color;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Disconnects from the REST server.
 * 
 *        Command syntax: disconnect
 */
@Command(scope = "cloudify", name = "disconnect", description = "disconnects the admin REST server")
public class Disconnect extends AdminAwareCommand {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		adminFacade.disconnect();
		session.put(Constants.ACTIVE_APP, CloudifyConstants.DEFAULT_APPLICATION_NAME);
		GigaShellMain.getInstance().setCurrentApplicationName(CloudifyConstants.DEFAULT_APPLICATION_NAME);
		return getFormattedMessage("disconnected_successfully", Color.GREEN);
	}
}
