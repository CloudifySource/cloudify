package com.gigaspaces.cloudify.usm.dsl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.gigaspaces.cloudify.usm.events.EventResult;
import com.gigaspaces.cloudify.usm.launcher.ProcessLauncher;
import com.gigaspaces.cloudify.usm.launcher.USMException;

public class DSLEntryExecutor  {

	private Object entry;
	private ProcessLauncher launcher;
	private File workDir;
	private Map<String, Object> params;

	private static java.util.logging.Logger logger =
			java.util.logging.Logger.getLogger(DSLEntryExecutor.class.getName());
	public DSLEntryExecutor(Object entry, ProcessLauncher launcher, File workDir) {
		this(entry, launcher, workDir, new HashMap<String, Object>());
	}

	public DSLEntryExecutor(Object entry, ProcessLauncher launcher, File workDir, Map<String, Object> params) {
		this.entry = entry;
		this.launcher = launcher;
		this.workDir = workDir;
		this.params = params;
	}

	public EventResult run() {
		if (entry == null) {
			return EventResult.SUCCESS;
		} 
		try {
			Object result = launcher.launchProcess(entry, workDir, params);
			return new EventResult(result);
		} catch (USMException e) {
			logger.log(Level.SEVERE, "Failed to execute entry: " + entry, e);
			return new EventResult(e);
		}
		
		
	}

}
