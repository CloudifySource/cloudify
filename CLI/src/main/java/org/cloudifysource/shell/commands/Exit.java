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
import org.apache.karaf.shell.console.AbstractAction;
import org.apache.karaf.shell.console.CloseShellException;
import org.cloudifysource.shell.ShellUtils;

import java.util.ResourceBundle;

/**
 * @author rafi, adaml, barakm
 * @since 2.0.0
 * 
 *        Terminates the shell.
 * 
 *        Command syntax: exit
 */
@Command(scope = "cloudify", name = "exit", description = "Terminates the shell")
public class Exit extends AbstractAction {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Object doExecute() throws Exception {
		ResourceBundle messages = ShellUtils.getMessageBundle();
		session.getConsole().println(messages.getString("on_exit"));
		throw new CloseShellException();
	}
}
