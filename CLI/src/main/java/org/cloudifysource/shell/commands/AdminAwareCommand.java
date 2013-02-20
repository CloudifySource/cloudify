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

import org.apache.felix.service.command.CommandSession;

/**
 * @author rafi, barakm
 * @since 2.0.0
 * 
 *        This Abstract class overrides {@link AbstractGSCommand}'s execute function. 
 *        Used by CLI commands that required an admin facade.
 */
public abstract class AdminAwareCommand extends AbstractGSCommand {

	/**
	 * This implementation of the execute method merely sets the flag "adminAware", to indicate for the super
	 * class that the admin facade should be used.
	 */
	@Override
	public Object execute(final CommandSession session) throws Exception {
		adminAware = true;
		return super.execute(session);
	}
}
