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
package org.cloudifysource.shell;

import org.apache.felix.service.command.CommandSession;

/**
 * @author uri
 * since 2.0.0
 * A callback for the console ( {@link ConsoleWithProps} )
 */
class CloseCallback implements Runnable {

	private CommandSession session;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() {
		final AdminFacade adminFacade = (AdminFacade) session.get(Constants.ADMIN_FACADE);
		try {
			//disconnects from the server
			adminFacade.disconnect();

			if (session.get(Constants.INTERACTIVE_MODE) != null) {
				final boolean isInteractive = (Boolean) session.get(Constants.INTERACTIVE_MODE);
				if (!isInteractive) {
					//if an exception was thrown and this is not an interactive mode - exit with code "1"
					if (session.get(Constants.LAST_COMMAND_EXCEPTION) != null) {
						// Throwable t = (Throwable) session.get(Constants.LAST_COMMAND_EXCEPTION);
						System.exit(1);
					}
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void setSession(final CommandSession session) {
		this.session = session;
	}
}
