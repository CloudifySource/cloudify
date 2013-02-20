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
package org.cloudifysource.shell.installer;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The purpose of this class is to suppress communication error while the agent is being bootstrapped or teared down.
 * @author itaif
 * @since 2.0.0
 */
public class ConnectionLogsFilter {

	// overriding logger filters is very tricky, since the Logger.getLogger() may return a different instance
	// than the one used by XAP.
	// therefore Logger.getLogger must be called during static initialization for it to work.
	private final static Logger[] loggers = new Logger[] {
		Logger.getLogger("net.jini.discovery.LookupDiscovery"),
		Logger.getLogger("net.jini.discovery.LookupLocatorDiscovery"),
		Logger.getLogger("net.jini.lookup.ServiceDiscoveryManager"),
		Logger.getLogger("com.gigaspaces.lrmi.nio")
	};

	private final Filter[] filters;

	public ConnectionLogsFilter() {
		filters = new Filter[loggers.length];
		for (int i = 0; i < loggers.length; i++) {
			filters[i] = loggers[i].getFilter();
		}
	}

	public void supressConnectionErrors() {
		for (int i = 0 ; i < loggers.length ; i++) {
			supressConnectionErrors(loggers[i],filters[i]);
		}
	}
	
	private void supressConnectionErrors(final Logger logger, final Filter filter) {

		Filter newFilter = new Filter() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean isLoggable(final LogRecord record) {
				boolean isLoggable = true;
				
				Throwable t = record.getThrown();
				if ((filter != null 
						&& !filter.isLoggable(record)) 
						|| (t != null && isConnectExceptionOrCause(t) 
						&& record.getLevel().intValue() <= Level.WARNING.intValue())) {
					isLoggable = false;
				}
				
				return isLoggable;
			}

			private boolean isConnectExceptionOrCause(Throwable throwable) {
				while (throwable != null) {
					if (isConnectException(throwable)) {
						return true;
					}
					throwable = throwable.getCause();
				}
				
				return false;
			}
			
			private boolean isConnectException(final Throwable t) {
				return (t instanceof java.net.SocketException) 
						|| (t instanceof java.rmi.ConnectException);
			}
		};
		
		logger.setFilter(newFilter);
	}
	
	void restoreConnectionErrors() {
		for (int i = 0; i < loggers.length; i++) {
			loggers[i].setFilter(filters[i]);
		}
	}
	
}
