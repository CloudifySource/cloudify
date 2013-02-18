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
package org.cloudifysource.esc.driver.provisioning.jclouds;

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.cloudifysource.esc.installer.AgentlessInstaller;


/**
 * The purpose of this class is to control logs ouput while the cloud is being bootstrapped or teared down.
 * @author itaif
 *
 */
public class BootstrapLogsFilters {

	final Filter sshOutputFilter;
	final static Logger sshOutputLogger = Logger.getLogger(AgentlessInstaller.SSH_OUTPUT_LOGGER_NAME);
	
	private final boolean verbose;
	
	public BootstrapLogsFilters(boolean verbose) {
	    this.verbose = verbose;
	    sshOutputFilter = sshOutputLogger.getFilter();
	}
	
	// TODO filter unnecessary output from ssh
	public void applyLogFilters() {
		Filter newFilter = new Filter() {
			@Override
			public boolean isLoggable(LogRecord record) {
			    return verbose;
			}
		};
		
		sshOutputLogger.setFilter(newFilter);
	}
	
	public void restoreLogFilters() {
	    sshOutputLogger.setFilter(sshOutputFilter);
	}
}
