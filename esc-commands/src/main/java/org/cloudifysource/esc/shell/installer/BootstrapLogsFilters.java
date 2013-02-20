/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
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
package org.cloudifysource.esc.shell.installer;

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.cloudifysource.esc.installer.AgentlessInstaller;

/**
 * The purpose of this class is to control logs output while the cloud is being bootstrapped or teared down.
 * @author itaif
 *
 */
public class BootstrapLogsFilters {

	private final Filter sshOutputFilter;
	private static final Logger SSH_OUTPUT_LOGGER = Logger.getLogger(AgentlessInstaller.SSH_OUTPUT_LOGGER_NAME);

	private final boolean verbose;

	public BootstrapLogsFilters(final boolean verbose) {
	    this.verbose = verbose;
	    sshOutputFilter = SSH_OUTPUT_LOGGER.getFilter();
	}

	// TODO filter unnecessary output from ssh

	/**
	 * Sets a filter that logs only on verbose mode.
	 */
	public void applyLogFilters() {
		Filter newFilter = new Filter() {
			@Override
			public boolean isLoggable(final LogRecord record) {
			    return verbose;
			}
		};

		SSH_OUTPUT_LOGGER.setFilter(newFilter);
	}

	/**
	 * Sets the default filter again.
	 */
	public void restoreLogFilters() {
		SSH_OUTPUT_LOGGER.setFilter(sshOutputFilter);
	}
}
