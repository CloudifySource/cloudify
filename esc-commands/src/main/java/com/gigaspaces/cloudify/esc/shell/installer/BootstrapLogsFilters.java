package com.gigaspaces.cloudify.esc.shell.installer;

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.gigaspaces.cloudify.esc.installer.AgentlessInstaller;

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
