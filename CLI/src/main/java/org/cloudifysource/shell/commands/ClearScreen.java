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

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.service.command.CommandSession;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        clears the console.
 * 
 *        Command syntax: clear
 */
@Command(scope = "cloudify", name = "clear", description = "clears the console.")
public class ClearScreen implements Action {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object execute(final CommandSession arg0) throws Exception {
		System.out.print("\33[2J");
		System.out.flush();
		System.out.print("\33[1;1H");
		System.out.flush();
		return null;
	}
}
