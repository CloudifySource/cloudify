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
package org.cloudifysource.esc.shell.listener;

import java.util.logging.Logger;

import org.cloudifysource.esc.installer.AgentlessInstallerListener;

/**
 * Event listener for events originated in the AgentlessInstaller.
 * 
 * @author adaml
 * 
 */
public class CliAgentlessInstallerListener extends AbstractEventListener implements AgentlessInstallerListener {

	private static final String VERBOSE_MARKER = "VERBOSE:";
	private static final Logger logger = Logger.getLogger(CliAgentlessInstallerListener.class.getName());
	private final boolean verbose;

	public CliAgentlessInstallerListener(final boolean verbose) {
		this.verbose = verbose;
	}

	@Override
	public void onInstallerEvent(final String eventName, final Object... args) {
		String formattedMessage = getFormattedMessage(eventName, args);
		if (formattedMessage.startsWith(VERBOSE_MARKER)) {
			// Message is marked to be printed only in Verbose mode
			if (this.verbose) {
				formattedMessage = formattedMessage.substring(VERBOSE_MARKER.length());
				logger.info(formattedMessage);
			}

		} else {
			logger.info(formattedMessage);
		}
	}

}
